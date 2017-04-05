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
import ocelot.Receiver
import org.scalatest.FlatSpec
import scala.reflect.classTag
import scala.reflect.ClassTag

class DispatcherSpec extends FlatSpec
{
  val numIterations = 10

  case class DummyMessageA(from: PID, to: Set[PID]) extends MulticastMessage
  case class DummyMessageB(from: PID, to: PID)      extends UnicastMessage
  case class DummyMessageC(from: PID, to: Set[PID]) extends MulticastMessage

  class DummyReceiver(giveEvent: (Event)=>Unit) extends Receiver
  {
     override def deliver (m: Event): Unit = { giveEvent(m) }
  }
  object DummyReceiver
  {
    def apply(giveEvent: (Event)=>Unit) = new DummyReceiver(giveEvent)
    // Note: since 2.12, the compiler has become smarter with optimizing case classes and
    // trying to instantiate several DummyReceiver with the same hook would result in a single
    // instance.
  }

  behavior of "Dispatcher"

  it should "register and unregister Receivers properly" in {
    val orphan = Receiver { ev =>  }
    val disp   = Dispatcher.onOrphan(orphan)
    val receivers  = for (i <- 1 to numIterations) yield DummyReceiver{ ev => /* do nothing */ }
    val msgClasses = IndexedSeq(classTag[DummyMessageA], classTag[DummyMessageB], classTag[DummyMessageC])
    val expectedCount = IndexedSeq(4, 7, 10) // NB: not generic !!

    for (msgClass <- msgClasses) {
      assertResult(Seq(orphan))(disp.protocolsFor(msgClass))
    }

    for {
      i <- receivers.indices
      j <- msgClasses.indices
      if i <= receivers.size * (j+1) / msgClasses.size
      recv = receivers(i)
      msgClass = msgClasses(j)
    }{
      disp.registerFor(msgClass, recv)
    }

    for {
      j <- msgClasses.indices
      msgClass = msgClasses(j)
      expCount = expectedCount(j)
    }{
      val protocols = disp.protocolsFor(msgClass)
      assertResult(expCount)(protocols.size)
    }
  }

  it should "dispatch messages only to registered receivers" in {
    var result1: Option[Event] = None
    var result2: Option[Event] = None
    var result3: Option[Event] = None
    var resultOrphan: Option[Event] = None

    val receiver1 = DummyReceiver { ev => result1 = Some(ev) }
    val receiver2 = DummyReceiver { ev => result2 = Some(ev) }
    val receiver3 = DummyReceiver { ev => result3 = Some(ev) }
    val receiverOrphan = Receiver { ev => resultOrphan = Some(ev) }

    val eventA = DummyMessageA(PID(0), Set.empty)
    val eventB = DummyMessageB(PID(0), PID(1))

    val disp = Dispatcher.onOrphan(receiverOrphan)

    disp.deliver(eventA)
    assertResult(None)(result1)
    assertResult(None)(result2)
    assertResult(None)(result3)
    assertResult(Some(eventA))(resultOrphan)
    
    resultOrphan = None
    disp.registerFor(classTag[DummyMessageA], receiver1)
    disp.registerFor(classTag[DummyMessageB], receiver1)
    disp.registerFor(classTag[DummyMessageC], receiver1)
    disp.registerFor(classTag[DummyMessageB], receiver2)
    disp.registerFor(classTag[DummyMessageC], receiver2)
    disp.registerFor(classTag[DummyMessageC], receiver3)

    disp.deliver(eventA)
    assertResult(Some(eventA))(result1)
    assertResult(None)(result2)
    assertResult(None)(result3)
    assertResult(None)(resultOrphan)

    disp.deliver(eventB)
    assertResult(Some(eventB))(result1)
    assertResult(Some(eventB))(result2)
    assertResult(None)(result3)
    assertResult(None)(resultOrphan)

    result1 = None
    result2 = None
    result3 = None
    disp.unregisterFor(classTag[DummyMessageB], receiver1)

    disp.deliver(eventB)
    assertResult(None)(result1)
    assertResult(Some(eventB))(result2)
    assertResult(None)(result3)
    assertResult(None)(resultOrphan)

    disp.deliver(eventA)
    assertResult(Some(eventA))(result1)
    assertResult(Some(eventB))(result2)
    assertResult(None)(result3)
    assertResult(None)(resultOrphan)

    disp.registerFor(classTag[DummyMessageA], receiver1)
    disp.registerFor(classTag[DummyMessageA], receiver2)
    disp.registerFor(classTag[DummyMessageA], receiver3)

    disp.deliver(eventA)
    assertResult(Some(eventA))(result1)
    assertResult(Some(eventA))(result2)
    assertResult(Some(eventA))(result3)
    assertResult(None)(resultOrphan)

    result1 = None
    result2 = None
    result3 = None
    disp.unregisterAllFor(classTag[DummyMessageA])

    disp.deliver(eventA)
    assertResult(None)(result1)
    assertResult(None)(result2)
    assertResult(None)(result3)
    assertResult(Some(eventA))(resultOrphan)

    disp.deliver(eventB)
    assertResult(None)(result1)
    assertResult(Some(eventB))(result2)
    assertResult(None)(result3)
    assertResult(Some(eventA))(resultOrphan)

    assertResult(Seq(receiver2))(disp.protocolsFor(classTag[DummyMessageB]))
    assertResult(Seq(receiver3, receiver2, receiver1))(disp.protocolsFor(classTag[DummyMessageC]))
  }

  case object SignalA extends Signal
  case object SignalB extends Signal

  it should "dispatch case objects properly" in {
    var result1: Option[Event] = None
    var result2: Option[Event] = None
    var resultOrphan: Option[Event] = None

    val receiver1 = DummyReceiver{ ev => result1 = Some(ev) }
    val receiver2 = DummyReceiver{ ev => result2 = Some(ev) }

    val disp = Dispatcher.onOrphan{ ev => resultOrphan = Some(ev) }

    disp.registerFor(ClassTag(SignalA.getClass), receiver1)
    disp.registerFor(ClassTag(SignalB.getClass), receiver2)

    disp.deliver(SignalA)
    assertResult(Some(SignalA))(result1)
    assertResult(None)(result2)
    assertResult(None)(resultOrphan)

    disp.deliver(SignalB)
    assertResult(Some(SignalA))(result1)
    assertResult(Some(SignalB))(result2)
    assertResult(None)(resultOrphan)

    assertResult(Seq(receiver1))(disp.protocolsFor(ClassTag(SignalA.getClass)))
    assertResult(Seq(receiver2))(disp.protocolsFor(ClassTag(SignalB.getClass)))
  }

  it should "match subclasses of messages" in {
    abstract class DummyContentBase(from: PID, to: Set[PID]) extends MulticastMessage
    case class DummyContentSubclassA(from: PID, to: Set[PID], info: String)
      extends DummyContentBase(from,to)
    case class DummyContentSubclassB(from: PID, to: Set[PID], value: Int)
      extends DummyContentBase(from,to)

    var result1: Option[Event] = None
    var result2: Option[Event] = None
    var result3: Option[Event] = None
    var resultOrphan: Option[Event] = None
    
    val receiver1 = DummyReceiver{ ev => result1 = Some(ev) }
    val receiver2 = DummyReceiver{ ev => result2 = Some(ev) }
    val receiver3 = DummyReceiver{ ev => result3 = Some(ev) }

    val messageA = DummyContentSubclassA(PID(0), Set.empty, "msgA")
    val messageB = DummyContentSubclassB(PID(1), Set.empty, 10)
    val messageC = DummyMessageA(PID(2), Set.empty)

    val disp = Dispatcher.onOrphan{ ev => resultOrphan = Some(ev) }
    disp.registerFor(classTag[DummyContentBase], receiver1)
    disp.registerFor(classTag[DummyContentSubclassA], receiver2)
    disp.registerFor(classTag[DummyContentSubclassB], receiver3)

    disp.deliver(messageC)
    assertResult(None)(result1)
    assertResult(None)(result2)
    assertResult(None)(result3)
    assertResult(Some(messageC))(resultOrphan)

    disp.deliver(messageB)
    assertResult(Some(messageB))(result1)
    assertResult(None)(result2)
    assertResult(Some(messageB))(result3)
    assertResult(Some(messageC))(resultOrphan)

    disp.deliver(messageA)
    assertResult(Some(messageA))(result1)
    assertResult(Some(messageA))(result2)
    assertResult(Some(messageB))(result3)
    assertResult(Some(messageC))(resultOrphan)
  }
}
