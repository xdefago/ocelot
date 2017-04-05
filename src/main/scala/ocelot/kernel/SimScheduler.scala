/*
 * Copyright (c) 2017 Xavier Defago (Tokyo Institute of Technology)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ocelot.kernel

/**
  * Created by defago on 01/03/2017.
  */

import ocelot._
import ocelot.time.Time

import java.io.{PrintWriter, StringWriter}
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import com.typesafe.scalalogging.LazyLogging

import scala.collection.immutable.SortedSet
import scala.concurrent.{ExecutionContext, Future, Promise, blocking}


abstract class AbstractScheduler extends Scheduler with LazyLogging
{
  protected type Action    = ()=>Unit
  protected type Condition = ()=>Boolean

  case object PoisonPill extends RuntimeException
  

  private val runningActivities = new AtomicInteger(0)
  protected def incrementRunning() = runningActivities.incrementAndGet()
  protected def decrementRunning() = runningActivities.decrementAndGet()
  protected def getRunning = runningActivities.get()
  protected def hasRunning = getRunning > 0
  
  protected trait TaskInfo { def id: TaskID }
  protected trait ActionInfo extends TaskInfo { def action: Action }
  protected case class ActivityInfo(id: TaskID, run: Runnable)              extends TaskInfo
  protected case class ConditionalTaskInfo(id: TaskID, cond: Condition, action: Action) extends ActionInfo
  protected case class TimedTaskInfo(id: TaskID, time: Time, action: Action)            extends ActionInfo
  protected val chronologicalOrder =
    Ordering.fromLessThan[TimedTaskInfo] (
      (t1, t2) =>
        t1.time < t2.time
        || (t1.time == t2.time) && (t1.hashCode() < t2.hashCode())
    )
  
  private val NoTimedActions = SortedSet.empty[TimedTaskInfo](chronologicalOrder)
  private var timedActions   = NoTimedActions
  protected def timedActionsCount = timedActions.size
  protected def hasTimed = timedActions.nonEmpty
  protected def timedActionNext: Option[Time] = timedActions.headOption.map(_.time)
  private def addTimedAction(task: TimedTaskInfo) = synchronized { timedActions += task }
  private def delTimedAction(task: TimedTaskInfo) = synchronized { timedActions = timedActions.filterNot(_.id == task.id ) }
  
  def isEmpty: Boolean  = ! hasTimed && ! hasRunning
  def nonEmpty: Boolean = hasTimed || hasRunning

  def scheduleAt(at: Time)(action: =>Unit): Task =
    synchronized {
      val task = TimedTaskInfo(TaskID.auto(), at, ()=>action)
      timedActions += task
      new Task {
        def id = task.id
        def cancel()             = { timedActions -= task }
        def rescheduleAt(t:Time) = addTimedAction(task.copy(time=t))
      }
    }

  def when(when: =>Boolean)(action: =>Unit) =
    synchronized {
      val task = ConditionalTaskInfo(TaskID.auto(), ()=>when, ()=>action)
      conditionalActions += task
      new Task {
        def id = task.id
        def cancel() = { conditionalActions -= task }
        def rescheduleAt(time: Time) = ???
      }
    }
  
  protected def checkTimed(): Int = {
    var actionsExecuted = 0
    
    do {
      val current = currentTime
      val (batch, rest) = timedActions.partition { (entry) => entry.time <= current }
      timedActions = rest
      
      if (batch.isEmpty) return actionsExecuted
      actionsExecuted += batch.size
      
      for (task <- batch) {
        runAction(task)
      }
    } while (true)
    throw new AssertionError("This code must never be reached")
  }
  
  private val NoActivities  = Seq.empty[ActivityInfo]
  private var newActivities = NoActivities // new AtomicReference(NoActivities)
  protected def hasNewActivities   = newActivities.nonEmpty
  protected def newActivitiesCount = newActivities.size
  protected def flushActivities(): Seq[ActivityInfo]  =
    synchronized {
      val tmp = newActivities
      newActivities = NoActivities
      tmp
    }
  protected def addNewActivity(run: Runnable)   =
    synchronized {
      val activity = ActivityInfo(TaskID.auto(), run)
      newActivities :+= activity
      activity
    }
  
  private val NoConditionals = Set.empty[ConditionalTaskInfo]
  private var conditionalActions = NoConditionals
  protected def hasConditionals   = conditionalActions.nonEmpty
  protected def conditionalsCount = conditionalActions.size

  protected def checkConditionals(): Int = {
    var conditionsExecuted = 0
    
    do {
      val (batch, rest) = conditionalActions.partition { (entry) => entry.cond() }
      conditionalActions = rest
      
      if (batch.isEmpty) return conditionsExecuted
      conditionsExecuted += batch.size
      
      for (action <- batch) {
        runAction(action)
      }
    } while(true)
    throw new AssertionError("This code must never be reached")
  }
  
  protected def runAction(action: ActionInfo): Unit
}



class SimScheduler(startTime: Time = Time.Zero)
  extends AbstractScheduler
{
  
  def currentTime : Time = _currentTime
  
  // Register activity
  def execute(activity: Runnable): Unit =
    synchronized {
      incrementRunning()
      addNewActivity(activity)
    }
  
  def exit(): Unit = {
    throw PoisonPill
  }
  
  // Conditional actions
  def waitUntil(cond: => Boolean) = {
    if (! cond) {
      var release = false
      when { cond } { release = true }
      while (! release) {
        blocking {
          phaser.awaitAdvance(phaser.arrive())
        }
        if (hasStashedException) {
          throw PoisonPill
        }
      }
    }
  }
  
  // Time actions
  def waitUntil(time: Time) = {
    logger.trace(s"waitUntil($time)")
    if (time > _currentTime) {
      var release = false
      scheduleAt (time) { release = true }
      while (!release) {
        logger.trace(s"waitUntil($time): continue waiting (now=$now)")
        blocking {
          phaser.awaitAdvance(phaser.arrive())
        }
        if (hasStashedException) {
          throw PoisonPill
        }
      }
    }
    logger.trace(s"waitUntil($time): finished waiting (now=$now)")
  }
  
  
  // PRIVATE STUFF
  
  private var _currentTime = startTime
  
  private val executor      = java.util.concurrent.Executors.newCachedThreadPool()
  private val execContext   = ExecutionContext.fromExecutor(executor)
  private val terminated    = Promise[Boolean]()
  
  private var advanceHooks  = Seq.empty[()=>Boolean]
  
  private def startNewActivities(): Int =
  {
    var activitiesStarted = 0
    do {
      val batch = flushActivities()
      
      if (batch.isEmpty) return activitiesStarted
      activitiesStarted = batch.size
      
      logger.debug(s"Will start ${batch.size} new activities")
      phaser.bulkRegister(batch.size)
      for (activity <- batch) {
        logger.trace(s"startActivity($activity)")
        startActivity(activity)
      }
    } while (true)
    throw new AssertionError("This code must never be reached")
  }
  
  private def startActivity(activity: ActivityInfo): Future[Unit] =
  {
    Future [ Unit ] {
      logger.trace (s"startActivity($activity): registered at $currentTime ($now)")
      // runActivity (activity)
      try {
        activity.run.run ()
        logger.trace (s"startActivity($activity): completed activity at $currentTime ($now)")
      }
      catch {
        case PoisonPill   => /* Do nothing: interrupted due to another exception or exit */
        case e: Exception => stashException(e)
      }
      finally {
        phaser.awaitAdvance(phaser.arriveAndDeregister())
        decrementRunning()
        logger.trace (s"startActivity($activity): deregistered at $currentTime ($now)")
      }
    }(execContext)
  }
  

  private def stackTrace(e: Throwable): String = {
    val sw = new StringWriter ()
    e.printStackTrace (new PrintWriter (sw))
    sw.toString
  }
  
  
  private def advanceTime(): Unit =
    synchronized {
      timedActionNext.foreach{ t =>
        _currentTime = _currentTime max t
      }
    }
  
  
  private val phaser =
    new Phaser() {
      override protected def onAdvance (phase: Int, registeredParties: Int): Boolean =
        try {
          val time = currentTime
          val pendingActivities   = newActivitiesCount
          val running             = getRunning
          logger
            .trace (s"PHASER($phase, $registeredParties): running=$running, new=$pendingActivities, timed=$timedActionsCount, at=$time ($now)")
          if (hasStashedException) {
            stashedException.foreach { e =>
              logger.error ("Phaser stopping with pending exception:")
              logger.error (stackTrace(e))
            }
            return false /* let the activities and the main loop deregister themselves */
          }
          val conditionsExecuted = checkConditionals()
          if (conditionsExecuted > 0) {
            logger.trace (s"Executed $conditionsExecuted conditional actions at $time ($now)")
          }
          
          val activitiesStarted = startNewActivities ()
          if (activitiesStarted > 0) {
            logger.trace (s"Started $activitiesStarted new activities")
          }
          else {
            val actionsExecuted    = checkTimed ()
            if (actionsExecuted > 0) {
              logger.trace (s"Executed $actionsExecuted timed actions at $time ($now)")
              val conditionsExecuted = checkConditionals()
              if (conditionsExecuted > 0) {
                logger.trace (s"Executed $conditionsExecuted conditional actions at time $time ($now)")
              }
            }
            else {
              val running = getRunning
              if (running > 0) {
                logger.trace (s"Go for another round ($running running activities)")
                advanceTime ()
                if (time != currentTime) {
                  logger.trace (s"Advancing time: $time -> $currentTime")
                }
              }
              else {
                val timeToFinish = getRegisteredParties == 1
                logger.trace (s"No running tasks: time to finish? $timeToFinish")
                return timeToFinish
              }
            }
          }
          false
        }
        catch {
          case e: Throwable =>
            logger.error(s"Phaser: caught exception $e in advance method:")
            logger.error(stackTrace(e))
            true
        }
    }
  
  private val _exceptionStash    = new AtomicReference(Option.empty[Throwable])
  private def hasStashedException = _exceptionStash.get().nonEmpty
  private def stashedException = _exceptionStash.get()
  private def stashException(e: Throwable): Unit =
  {
    logger.warn(s"Stashing exception: $e")
    _exceptionStash.weakCompareAndSet(None, Some(e))
  }
  
  private def enterPhaserMode(): Unit =
    try {
      startNewActivities ()
      phaser.register()
      while ((hasRunning || hasTimed || hasNewActivities) && ! hasStashedException ) {
        phaser.arriveAndAwaitAdvance()
      }
      logger
        .debug (
          s"MAIN LEAVING: running = $getRunning timed = $timedActionsCount new = $newActivitiesCount"
        )
      
      stashedException.foreach { e => throw SchedulerException(e) }
    }
    finally {
      while (phaser.getRegisteredParties > 1) {
        logger.trace(s"Mainloop: letting other activities complete (${phaser.getRegisteredParties-1} left)")
        phaser.arriveAndAwaitAdvance()
      }
      phaser.awaitAdvance(phaser.arriveAndDeregister())
      logger.trace(s"Deregistered mainloop at time $currentTime ($now)")
    }

  def runMainloop (): Unit =
  {
    try {
      checkConditionals()
      while (hasTimed && ! hasNewActivities) {
        advanceTime()
        checkTimed()
        checkConditionals()
        stashedException.foreach { e => throw SchedulerException(e) }
      }
      
      if (hasNewActivities) {
        logger.trace(s"Entering phaser mode with $newActivitiesCount activities")
        enterPhaserMode()
      }
    }
    catch {
      case SchedulerException(PoisonPill) => /* ignore and leave */
      case e: java.lang.IllegalStateException =>
        e.printStackTrace()
    }
    finally {
      executor.shutdown ()
    }
  }

  protected def runAction(action: ActionInfo): Unit =
    try {
      action.action()
    } catch { case e: Exception => stashException(e) }
}
