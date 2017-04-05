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

import ocelot._
import ocelot.time.Time

trait Task
{
  def id: TaskID
  def cancel()
  def rescheduleAt(time: Time)
}


trait Scheduler
{
  def currentTime: Time
  def now = currentTime.toNanos
  
  def when(when: =>Boolean)(action: =>Unit): Task
  def scheduleAt(at: Time)(action: =>Unit): Task
  def scheduleAfter(after: Time)(action: =>Unit): Task = scheduleAt(currentTime + after) { action }
  def scheduleNow(action: =>Unit): Task                = scheduleAt(currentTime) { action }


  // Blocking
  def waitUntil(time: Time)
  def waitUntil(cond: => Boolean)
  def waitFor(delay: Time): Unit                       = waitUntil(currentTime + delay)
  
  def execute(activity: Runnable)
  def exit()
  
  def isEmpty: Boolean
  
  def nonEmpty: Boolean
  
  def runMainloop ()
  
//  def wait(until: =>Boolean): Unit = wait( { ()=> until })
  
  // --------- earlier interface
  
  
//  def when(cond: => Boolean)(act: => Unit): TaskID = ???
//  def scheduleAt(time: Time)(action: => Unit): TaskID = ???
//  def scheduleAfter(delay: Time)(action: => Unit): TaskID = ???
  def rescheduleAt(id: TaskID, time: Time)(action: => Unit): Unit = ???
  def rescheduleAfter(id: TaskID, delay: Time)(action: => Unit): Unit = ???

  def cancel(task: TaskID) = ???

  def continue(): Unit = continueOrElse { throw new InterruptedException("Waiting but no more events") }
  def continueOrElse(whenFail: => Unit): Unit = ???
  
  protected[ocelot] def debugPrint(): Unit = ???
}


object Scheduler
{
  def Simulation(startTime: Time = Time.Zero): Scheduler = new SimScheduler(startTime)
}
