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

package ocelot

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

import com.typesafe.scalalogging.LazyLogging
import ocelot.event.Timeout
import ocelot.time.Time
import ocelot.topology.Clique
import org.scalatest.FlatSpec

import scala.concurrent.duration._


/**
  * Created by defago on 03/04/2017.
  */
class ActiveProtocolSpec
  extends FlatSpec
    with LazyLogging
{
  val randomSeed = 1L

  def addMe(me: PID): UnaryOperator[Set[PID]] =
  {
    val op: UnaryOperator[Set[PID]] =
      (set: Set[PID]) => set + me
    op
  }

  
  behavior of "ActiveProtocol"
  
  it should "run correctly in simple settings" in {
    val numProcesses = 5
    val executed = new AtomicReference(Set.empty[PID])
    val topology    = Clique(numProcesses)

    
    class MyActiveProtocol(p: OcelotProcess) extends ActiveProtocol(p, "my active")
    {
      override def run () =
      {
        executed.updateAndGet(addMe(me))
      }
    }
    
    val initializer = ProcessInitializer.forProcess { p =>

      val active = new MyActiveProtocol(p)
      
      assert(p.protocols.isEmpty)
      assertResult(Some(active))(p.program)
    }
    
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    
    assertResult(0)(system.runningProcs.size)
    assertResult(topology.processSet)(executed.get())
  }
  
  
  
  it should "raise IllegalStateException if registered multiple times" in {
    val numProcesses = 1
    val topology    = Clique(numProcesses)
    
    class MyActiveProtocol(p: OcelotProcess) extends ActiveProtocol(p, "my active")
    {
      override def run () = {}
    }
    
    val initializer = ProcessInitializer.forProcess { p =>

      val active1 = new MyActiveProtocol(p)
      val active2 =
        assertThrows[IllegalStateException] {
          new MyActiveProtocol(p)
        }
      
      assert(p.protocols.isEmpty)
      assertResult(Some(active1))(p.program)
    }
    
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
  }
  
  
  
  it should "raise IllegalStateException if registered after start" in {
    val numProcesses = 1
    val topology    = Clique(numProcesses)
    
    class MyActiveProtocol(p: OcelotProcess) extends ActiveProtocol(p, "my active")
    {
      override def run () = {
        assertThrows[IllegalStateException] {
          new ActiveProtocol(p, "inner")
          {
            def run () = {}
          }
        }
      }
    }
    
    val initializer = ProcessInitializer.forProcess { p =>

      val active = new MyActiveProtocol(p)
      
      assert(p.protocols.isEmpty)
      assertResult(Some(active))(p.program)
    }
    
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
  }
  
  
  
  it should "propagate exceptions to the system" in {
    val numProcesses = 1
    val topology = Clique(numProcesses)
  
    class MyException extends RuntimeException
    class MyActiveProtocol (p: OcelotProcess) extends ActiveProtocol(p, "my active")
    {
      override def run () = throw new MyException
    }
  
    val initializer = ProcessInitializer.forProcess { p =>
      val active = new MyActiveProtocol(p)
    }
  
    assertThrows[MyException] {
      val system =
        new OcelotSimSystem(topology, initializer, randomSeed)
    }
  }
  
  
  
  it should "exchange unicast messages" in {
    val numProcesses = 2
    val p0 = PID(0)
    val p1 = PID(1)
    val topology = Clique(numProcesses)
  
    case class MyMessage(from: PID, to: PID) extends UnicastMessage
    class MySender (p: OcelotProcess, sender: Sender) extends ActiveProtocol(p, "sender")
    {
      override def run () = {
        logger.trace(s"[$me] sending message")
        sender.send(MyMessage(me, p1))
      }
    }
    class MyReceiver (p: OcelotProcess) extends Program(p, "receiver")
    {
      listenTo[MyMessage]
      override def run () = {
        logger.trace(s"[$me] started and waiting for message")
        
        RECEIVE[Event] match {
          case m : MyMessage =>
            logger.trace(s"[$me] received message: $m")
            assert(true)
            
          case m =>
            logger.error(s"[$me] received unexpected message: $m")
            assert(false)
        }
      }
    }
  
    val initializer = ProcessInitializer.forProcess { p =>
      if (p.pid == p0)
        new MySender(p, p.network)
      else
        new MyReceiver(p)
    }
  
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
  }
  

  
  it should "exchange broadcast messages" in {
    val numProcesses = 5
    val p0 = PID(0)
    val topology = Clique(numProcesses)
    val received = new AtomicReference(Set.empty[PID])
    
  
    case class MyMessage(from: PID) extends BroadcastMessage
    class MySender (p: OcelotProcess, sender: Sender) extends ActiveProtocol(p, "sender")
    {
      override def run () = {
        logger.trace(s"[$me] sending message")
        sender.send(MyMessage(me))
      }
    }
    class MyReceiver (p: OcelotProcess) extends Program(p, "receiver")
    {
      listenTo[MyMessage]
      override def run () = {
        logger.trace(s"[$me] started and waiting for message")
        
        RECEIVE[Event] match {
          case m : MyMessage =>
            logger.trace(s"[$me] received message: $m")
            received.updateAndGet(addMe(me))
            
          case m =>
            logger.error(s"[$me] received unexpected message: $m")
            assert(false)
        }
      }
    }
  
    val initializer = ProcessInitializer.forProcess { p =>
      if (p.pid == p0)
        new MySender(p, p.network)
      else
        new MyReceiver(p)
    }
  
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    assertResult(topology.processSet - p0)(received.get())
  }
  
  
  it should "exchange multicast messages" in {
    val numProcesses = 8
    val p0 = PID(0)
    val destinations = Set(1,3,5).map(PID)
    val others = (0 until numProcesses).toSet.map(PID) diff destinations
    val timeout = Time(Duration(10000, SECONDS))
    
    val topology = Clique(numProcesses)
    val received = new AtomicReference(Set.empty[PID])
    val timedout = new AtomicReference(Set.empty[PID])
    
  
    case class MyMessage(from: PID, to: Set[PID]) extends MulticastMessage
    class MySender (p: OcelotProcess, sender: Sender) extends ActiveProtocol(p, "sender")
    {
      override def run () = {
        logger.trace(s"[$me] sending message")
        sender.send(MyMessage(me, destinations))
      }
    }
    class MyReceiver (p: OcelotProcess) extends Program(p, "receiver")
    {
      listenTo[MyMessage]
      override def run () = {
        logger.trace(s"[$me] started and waiting for message")
        
        Receive.withTimeout(timeout) {
          case m : MyMessage =>
            logger.trace(s"[$me] received message: $m")
            received.updateAndGet(addMe(me))

          case Timeout =>
            logger.trace(s"[$me] timed out")
            timedout.updateAndGet(addMe(me))
            
          case m =>
            logger.error(s"[$me] received unexpected message: $m")
            assert(false)
        }
      }
    }
  
    val initializer = ProcessInitializer.forProcess { p =>
      if (p.pid == p0)
        new MySender(p, p.network)
      else
        new MyReceiver(p)
    }
  
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    assertResult(destinations)(received.get())
    assertResult(others - p0)(timedout.get())
    assertResult(timeout)(system.currentTime)
  }
}
