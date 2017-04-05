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

package ocelot.sim

/*
 * Copyright 2017 Xavier Defago
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

import org.scalatest._

import ocelot._
import ocelot.time._
import ocelot.kernel._


class SimSchedulerSpec extends FlatSpec
{
  import scala.concurrent.duration._
  
  behavior of "A SimScheduler"
  

  it should "be initially empty and terminate right away" in {
    val ssys = new SimScheduler()
    
    assert(ssys.isEmpty)
    
    ssys.runMainloop()
    assertResult(Time.Zero)(ssys.currentTime)
  }

  
  it should "be empty if only with conditional actions" in {
    val ssys = new SimScheduler()
    val num = 10
    val startTime = Time.fromNanos(10)
    val trigger  = Array.fill(num)(false)
    val executed = Array.fill(num)(false)
    
    for (i <- executed.indices ) {
      ssys.when { trigger(i) } { executed(i) = true }
    }
    
    assert(ssys.isEmpty)
    
    ssys.runMainloop()
    
    assert(executed.forall(_ == false))
  }
  
  
  it should "be able to start from enabled conditionals" in {
    val ssys = new SimScheduler()
    val num = 10
    val startTime = Time.fromNanos(10)
    val trigger  = Array.fill(num)(false)
    val executed = Array.fill(num)(false)
    
    for (i <- executed.indices ) {
      ssys.when { trigger(i) } { executed(i) = true }
    }
    
    ssys.when { true } {
      ssys.scheduleAt (startTime) {
        for (i <- executed.indices if i < num / 2) {
          trigger (i) = true
        }
      }
    }
    
    assert(ssys.isEmpty)
    
    ssys.runMainloop()

    assert(ssys.isEmpty)
    
    for (i <- executed.indices) {
      assertResult(trigger(i))(executed(i))
      assertResult(i < num/2)(executed(i))
    }
  }
  
  
  // ------- timed actions -------
 
  
  it should "execute initial timed actions" in {
    val ssys = new SimScheduler()
    val num = 10
    val executed = Array.fill(num)(false)
    
    for (i <- executed.indices) {
      ssys.scheduleNow {
        executed(i) = true
      }
    }
    
    assert(ssys.nonEmpty)
    
    ssys.runMainloop()
    assert(executed.forall(Predef.identity))
    assertResult(Time.Zero)(ssys.currentTime)
  }

  
  it should "execute successive timed actions" in {
    val ssys = new SimScheduler()
    val num = 10
    val executed = Array.fill(num)(false)
    
    for (i <- executed.indices) {
      ssys.scheduleAt(Time.fromNanos(i)) { executed(i) = true }
    }
    ssys.runMainloop()
    assert(executed.forall(Predef.identity))
    assertResult(Time.fromNanos(num-1))(ssys.currentTime)
  }

  
  it should "cancel timed actions iff not already executed" in {
    val ssys = new SimScheduler()
    val num = 5
    val threshold = 25
    val executed = Array.fill(num)(false)
    val tasks =
      for (i <- executed.indices) yield {
        ssys.scheduleAt(Time.fromNanos(10 * (i+1))) {
          executed(i) = true
        }
      }
    
    ssys.scheduleAt(Time.fromNanos(threshold)) {
      for (i <- executed.indices) {
        assertResult (10*(i+1) < threshold)(executed (i))
      }
      tasks(0).cancel()
      tasks(num-1).cancel()
    }
    
    ssys.runMainloop()
    
    for (i <- executed.indices)
      assertResult(i != num-1)(executed(i))
    assertResult(Time.fromNanos(10*(num-1)))(ssys.currentTime)
  }
  
  
  it should "report exceptions occurring in timed actions" in {
    case class MyException(i: Int) extends RuntimeException
    val ssys = new SimScheduler()
    val num  = 5
    for (i <- 0 until num) {
      ssys.scheduleNow {
        if (i == num / 2) throw MyException(i)
      }
    }
    val caught =
      intercept[SchedulerException[MyException]] {
        ssys.runMainloop()
      }
    assertResult(num / 2)(caught.exception.i)
  }
  
  
  // ------- conditional actions -------
  
  it should "execute conditional actions" in {
    val ssys = new SimScheduler()
    var a = false
    var b = false
    var c = false
    
    ssys.when { ! a } { a = true }
    ssys.when { a }   { b = true }
    ssys.when { c }   { c = true }
    
    ssys.runMainloop()
    
    assert(a)
    assert(b)
    assert(!c)
  }
  
  
  it should "execute conditional and timed actions" in {
    val ssys = new SimScheduler()
    val delay = Time.fromNanos(500)
    var a = false
    var b = false
    var c = false
    var d = false
    
    ssys.when { a } { b = true }
    ssys.when { b } { c = true }
    ssys.scheduleAt(delay) {
      a = true
      ssys.when { c } { d = true }
    }
    
    ssys.runMainloop()
    
    assertResult(delay)(ssys.currentTime)
    assert(a)
    assert(b)
    assert(c)
    assert(d)
  }
  
  
  // ------- activities -------
  
  
  it should "start activities" in {
    val ssys = new SimScheduler()
    val num  = 10
    val started = Array.fill(num)(false)
    for (i <- started.indices) {
      ssys.execute { ()=>
        started(i) = true
      }
    }
    ssys.runMainloop()
    for (i <- started.indices) {
      assert(started(i))
    }
  }
  
  
  it should "handle a single waiting activity" in {
    val ssys = new SimScheduler()
    val num  = 5
    val times = Array.fill(num)(Option.empty[Time])
    ssys.execute { () =>
      times(0) = Some(ssys.currentTime)
      for (i <- 1 until num) {
        ssys.waitFor(Time.fromNanos(1))
        times(i) = Some(ssys.currentTime)
      }
    }
    ssys.runMainloop()
    for {
      i <- times.indices
      t <- times(i)
    } {
      assertResult(Time.fromNanos(i))(t)
    }
  }
  

  it should "report exceptions occurring in timed actions with running activities" in {
    case class MyException(time: Time) extends RuntimeException
    val ssys = new SimScheduler()
    val num  = 5
    val wakeupTime = Time.fromNanos(10000)
    val throwTime  = Time.fromNanos(500)
    for (i <- 0 until num) {
      ssys.execute { ()=>
        ssys.waitUntil(wakeupTime)
      }
    }
    ssys.scheduleAt(throwTime) {
      throw MyException(ssys.currentTime)
    }
    
    val caught =
      intercept[SchedulerException[MyException]] {
        ssys.runMainloop()
      }
    
    assertResult(ssys.currentTime)(caught.exception.time)
    assertResult(throwTime)(caught.exception.time)
  }

  
  it should "report exceptions occurring in activities" in {
    case class MyException(i: Int) extends RuntimeException
    val ssys = new SimScheduler()
    val num  = 5
    for (i <- 0 until num) {
      ssys.scheduleNow {
        if (i == num / 2) throw MyException(i)
      }
    }
    val caught =
      intercept[SchedulerException[MyException]] {
        ssys.runMainloop()
      }
    assertResult(num / 2)(caught.exception.i)
  }
  
  
  it should "let activities exit properly" in {
    val ssys = new SimScheduler()
    val delay = Time.fromNanos(500)
    val num = 5
    val completed = Array.fill(num)(false)
    for (i <- 0 until num) {
      ssys.execute { ()=>
        ssys.waitUntil(delay * (i+1) )
        if (i == 0) ssys.exit()
        completed(i) = true
      }
    }
    ssys.runMainloop()
    for (i <- completed.indices) {
      // should be false for i=0 and true otherwise
      assertResult(i != 0)(completed(i))
    }
    assertResult(delay * num)(ssys.currentTime)
  }
  
  
  it should "handle multiple waiting activities" in {
    val ssys = new SimScheduler()
    val numAct = 2
    val num  = 5
    val times = Array.fill(numAct,num)(Option.empty[Time])
    for (k <- 0 until numAct) {
      ssys.execute { () =>
        times (k)(0) = Some (ssys.currentTime)
        for (i <- 1 until num) {
          ssys.waitFor (Time.fromNanos(k))
          times (k)(i) = Some (ssys.currentTime)
        }
      }
    }
    ssys.runMainloop()
    for {
      k <- times.indices
      i <- times(k).indices
      t <- times(k)(i)
    } {
      assertResult(Time.fromNanos(k*i))(t)
    }
  }
  
  
  it should "alternate timed actions and activities" in {
    val ssys = new SimScheduler()
    val iterations = 10
    val timesActivities = Array.fill(iterations)(Time.fromNanos(-1))
    val timesTimed      = Array.fill(iterations)(Time.fromNanos(-1))
    
    for (i <- timesTimed.indices) {
      ssys.scheduleAt(Time.fromNanos(i)) {
        timesTimed(i) = ssys.currentTime
      }
    }
    ssys.execute { ()=>
      timesActivities(0) = ssys.currentTime
      for (i <- 1 until iterations) {
        ssys.waitFor(Time.fromNanos(1))
        timesActivities(i) = ssys.currentTime
      }
    }
    ssys.runMainloop()
    
    for (i <- timesTimed.indices) {
      assertResult (Time.fromNanos(i))(timesTimed(i))
      assert (Time.fromNanos(i) <= timesActivities(i))
    }
  }
  
  
  it should "alternate conditional actions and activities" in {
    val ssys = new SimScheduler()
    val iterations = 10
    val timesActivities = Array.fill(iterations)(Time.fromNanos(-1))
    val timesTimed      = Array.fill(iterations)(Time.fromNanos(-1))
    val timesCond       = Array.fill(iterations)(Time.fromNanos(-1))
    
    for (i <- timesTimed.indices) {
      ssys.scheduleAt(Time.fromNanos(i)) {
        timesTimed(i) = ssys.currentTime
      }
      ssys.when { ssys.now >= i } {
        timesCond(i) = ssys.currentTime
      }
    }
    ssys.execute { ()=>
      timesActivities(0) = ssys.currentTime
      for (i <- 1 until iterations) {
        ssys.waitFor(Time.fromNanos(1))
        timesActivities(i) = ssys.currentTime
      }
    }
    ssys.runMainloop()
    
    for (i <- timesTimed.indices) {
      assertResult (Time.fromNanos(i))(timesTimed(i))
      assertResult (Time.fromNanos(i))(timesCond(i))
      assert (Time.fromNanos(i) <= timesActivities(i))
    }
  }
  
}
