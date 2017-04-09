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

package ocelot.network

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

import com.typesafe.scalalogging.LazyLogging
import ocelot._
import ocelot.kernel._
import ocelot.time.Time
import ocelot.network.sim._
import ocelot.topology._
import org.scalatest.FlatSpec

import scala.concurrent.duration.Duration
import scala.reflect.classTag

/**
  * Created by defago on 2017/03/25.
  */
class NetworkSpec extends FlatSpec
  with LazyLogging
{
  val randomSeed = 1L
  val timeUnit   = Time.fromNanos(1)
  
  class TestSystem(val topology: Topology)(initializer0: OcelotProcess => Unit)
    extends OcelotSystem
  {
    import scala.util.Random

    val random    = new Random(randomSeed)
    val scheduler = Scheduler.Simulation()
    lazy val simNetwork   = new ConstantDelaySimNetwork(scheduler, topology, processMap, random)(Time(Duration.fromNanos(100)))
    def network: Network = simNetwork
    lazy val processMap: Map[PID, OcelotProcess] = {
      val initializer = ProcessInitializer.forProcess(initializer0)
      for {
        pid <- topology.processes
        out = new java.io.PrintStream(new util.LineHeadOutputStream(Console.out, s"$pid: "))
        process = new OcelotProcessImpl(this, pid, out)
        _ = initializer(process)
      } yield pid -> process
    }.toMap[PID, OcelotProcess]
  }
  
  object TestSystem
  {
    def apply(topo: Topology)(init0: OcelotProcess => Unit) = new TestSystem(topo)(init0)
  }
  
  behavior of "Network"
  
  it should "initialize correctly" in {
    var created = Seq.empty[PID]
    var protocolMap = Map.empty[PID, Set[Protocol]].withDefault(_=>Set.empty[Protocol])
    
    val num = 5
    val protoName = "testProto"
    val topology  = Clique(num)
    val system = TestSystem(topology) { p =>
      assertResult(num)(p.network.neighbors.size)
      created :+= p.pid
      val proto = ReactiveSenderProtocol(p, "protoName", p.network) { case _ => }
      val set: Set[Protocol] = protocolMap(p.pid)
      protocolMap = protocolMap.updated(p.pid,  protocolMap(p.pid) + proto)
    }
    
    assert(system.network != null)
    assert(system.processMap != null)
    assert(system.processMap.nonEmpty)
    assertResult(topology.numberOfVertices)(system.processMap.size)
    assertResult(topology.numberOfVertices)(created.size)
    assertResult(topology.processSet)(created.toSet)

    assertResult(num)(topology.processSet.size)
    assertResult(num)(topology.processes.size)
    
    for {
      pid <- topology.processSet
      process <- system.processMap.get(pid)
      protocols = process.protocols
    } {
      assertResult(1)(protocols.size)
      assertResult(protocolMap(pid))(protocols)
    }
  }
  
  it should "transmit broadcast messages" in {
    val p0 = PID(0)
    val dummyPayload = "payload"
    val numProcesses = 5
    val received = new AtomicReference(Set.empty[PID])
    
    case class TestMessage(from: PID, str: String) extends BroadcastMessage
    
    case class DummyReactive(p: OcelotProcess, sender: Sender)
      extends ReactiveSenderProtocol(p, "dummy")
    {
      val addMe : UnaryOperator[Set[PID]] = (s:Set[PID]) => s + me
      onInit {
        if (me == p0) {
          logger.trace(s"@$me sending TestMNessage to all others (broadcast)")
          sender.send(TestMessage(me, dummyPayload))
        }
      }
      onReceive[TestMessage] {
        case m @ TestMessage(`p0`, `dummyPayload`) =>
          logger.trace(s"@$me got $m")
          received.updateAndGet(addMe)

        case o =>
          logger.warn(s"@$me got unrecognized message $o")
          assert(false)
      }
      assertResult(Seq(this))(dispatcher.protocolsFor(classTag[TestMessage]))
    }

    val topology    = Clique(numProcesses)
    val initializer = ProcessInitializer.forProcess { p => DummyReactive(p, p.network) }
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    
    assertResult(0)(system.runningProcs.size)
    assertResult(topology.processSet - p0)(received.get())
    
    assert(system.now > 0)
    assert(system.currentTime > Time.Zero)
  }
  
  it should "transmit broadcast messages repeatedly" in {
    val p0 = PID(0)
    val dummyPayload = "payload"
    val numProcesses = 5
    val received = new AtomicReference(IndexedSeq.tabulate(numProcesses){ _ => Set.empty[PID] })
    
    case class TestMessage(from: PID, seq: Int, str: String) extends BroadcastMessage
    
    case class DummyReactive(p: OcelotProcess, sender: Sender)
      extends ReactiveSenderProtocol(p, "dummy")
    {
      def addMeAt(idx: Int): UnaryOperator[IndexedSeq[Set[PID]]] =
      {
        val op: UnaryOperator[IndexedSeq[Set[PID]]] =
          (seq: IndexedSeq[Set[PID]]) => seq.updated(idx, seq(idx) + me)
        op
      }
      onInit {
        if (me == p0) {
          logger.trace(s"@$me sending TestMNessage to all others (broadcast)")
          sender.send(TestMessage(me, 0, dummyPayload))
        }
      }
      onReceive[TestMessage] {
        case m @ TestMessage(from, seq, `dummyPayload`) if me.value == seq+1 =>
          logger.trace(s"@$me got $m and retransmit")
          sender.send(TestMessage(me, seq+1, dummyPayload))
          
        case m @ TestMessage(from, seq, `dummyPayload`) =>
          logger.trace(s"@$me got $m and register myself")
          received.updateAndGet(addMeAt(seq))
          
        case o =>
          logger.warn(s"@$me got unrecognized message $o")
          assert(false)
      }
      assertResult(Seq(this))(dispatcher.protocolsFor(classTag[TestMessage]))
    }

    val topology    = Clique(numProcesses)
    val initializer = ProcessInitializer.forProcess { p => DummyReactive(p, p.network) }
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    
    assertResult(0)(system.runningProcs.size)
    for ( (receivers, i) <- received.get().zipWithIndex ) {
      assertResult(topology.processSet - PID(i) - PID(i+1))(receivers)
    }
    
    assert(system.now > 0)
    assert(system.currentTime > Time.Zero)
  }
  
  
  it should "transmit unicast messages" in {
    val p0 = PID(0)
    val p1 = PID(1)
    val dummyPayload = "payload"
    val numProcesses = 5
    val received = new AtomicReference(Set.empty[PID])
    
    case class TestMessage(from: PID, to: PID, str: String) extends UnicastMessage
    
    case class DummyReactive(p: OcelotProcess, sender: Sender)
      extends ReactiveSenderProtocol(p, "dummy")
    {
      val addMe : UnaryOperator[Set[PID]] = (s:Set[PID]) => s + me
      onInit {
        if (me == p0) {
          logger.trace(s"@$me sending TestMNessage to all others (broadcast)")
          sender.send(TestMessage(me, p1, dummyPayload))
        }
      }
      onReceive[TestMessage] {
        case m @ TestMessage(`p0`, `p1`, `dummyPayload`) if me == p1 =>
          logger.trace(s"@$me got $m")
          received.updateAndGet(addMe)

        case o =>
          logger.warn(s"@$me got unrecognized message $o")
          assert(false)
      }
      assertResult(Seq(this))(dispatcher.protocolsFor(classTag[TestMessage]))
    }

    val topology    = Clique(numProcesses)
    val initializer = ProcessInitializer.forProcess { p => DummyReactive(p, p.network) }
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    
    assertResult(0)(system.runningProcs.size)
    assertResult(Set(p1))(received.get())
    
    assert(system.now > 0)
    assert(system.currentTime > Time.Zero)
  }
  
  it should "transmit unicast messages repeatedly" in {
    val p0 = PID(0)
    val p1 = PID(1)
    val dummyPayload = "payload"
    val numProcesses = 5
    val received = new AtomicReference(IndexedSeq.tabulate(numProcesses){ _ => Set.empty[PID] })
    
    case class TestMessage(from: PID, to: PID, seq: Int, str: String) extends UnicastMessage
    
    case class DummyReactive(p: OcelotProcess, sender: Sender)
      extends ReactiveSenderProtocol(p, "dummy")
    {
      val next = me.map(i => (i+1) % N)
      def addMeAt(idx: Int): UnaryOperator[IndexedSeq[Set[PID]]] =
      {
        val op: UnaryOperator[IndexedSeq[Set[PID]]] =
          (seq: IndexedSeq[Set[PID]]) => seq.updated(idx, seq(idx) + me)
        op
      }
      onInit {
        if (me == p0) {
          logger.trace(s"@$me sending TestMNessage to all others (broadcast)")
          sender.send(TestMessage(me, p1, 0, dummyPayload))
        }
      }
      onReceive[TestMessage] {
        case m @ TestMessage(from, to, seq, `dummyPayload`) if me.value == seq+1 =>
          logger.trace(s"@$me got $m and retransmit")
          sender.send(TestMessage(me, next, seq+1, dummyPayload))
          received.updateAndGet(addMeAt(seq))
          
        case m @ TestMessage(from, to, seq, `dummyPayload`) =>
          logger.trace(s"@$me got $m and register myself")
          received.updateAndGet(addMeAt(seq))
          
        case o =>
          logger.warn(s"@$me got unrecognized message $o")
          assert(false)
      }
      assertResult(Seq(this))(dispatcher.protocolsFor(classTag[TestMessage]))
    }

    val topology    = Clique(numProcesses)
    val initializer = ProcessInitializer.forProcess { p => DummyReactive(p, p.network) }
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    
    assertResult(0)(system.runningProcs.size)
    for ( (receivers, i) <- received.get().zipWithIndex ) {
      assertResult(Set(PID((i+1) % numProcesses)) )(receivers)
    }
    
    assert(system.now > 0)
    assert(system.currentTime > Time.Zero)
  }
  
  it should "transmit multicast messages" in {
    val p0 = PID(0)
    val dests = Set(p0, PID(1), PID(3))
    val dummyPayload = "payload"
    val numProcesses = 5
    val received = new AtomicReference(Set.empty[PID])
    
    case class TestMessage(from: PID, to: Set[PID], str: String) extends MulticastMessage
    
    case class DummyReactive(p: OcelotProcess, sender: Sender)
      extends ReactiveSenderProtocol(p, "dummy")
    {
      val addMe : UnaryOperator[Set[PID]] = (s:Set[PID]) => s + me
      onInit {
        if (me == p0) {
          logger.trace(s"@$me sending TestMNessage to all others (broadcast)")
          sender.send(TestMessage(me, dests, dummyPayload))
        }
      }
      onReceive[TestMessage] {
        case m @ TestMessage(`p0`, dests, `dummyPayload`) if dests.contains(me) =>
          logger.trace(s"@$me got $m")
          received.updateAndGet(addMe)

        case o =>
          logger.warn(s"@$me got unrecognized message $o")
          assert(false)
      }
      assertResult(Seq(this))(dispatcher.protocolsFor(classTag[TestMessage]))
    }

    val topology    = Clique(numProcesses)
    val initializer = ProcessInitializer.forProcess { p => DummyReactive(p, p.network) }
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    
    assertResult(0)(system.runningProcs.size)
    assertResult(dests)(received.get())
    
    assert(system.now > 0)
    assert(system.currentTime > Time.Zero)
  }

  it should "transmit multicast messages repeatedly" in {
    val p0 = PID(0)
    val dummyPayload = "payload"
    val numProcesses = 5
    val received = new AtomicReference(IndexedSeq.tabulate(numProcesses){ _ => Set.empty[PID] })
    
    case class TestMessage(from: PID, to: Set[PID], seq: Int, str: String) extends MulticastMessage
    
    case class DummyReactive(p: OcelotProcess, sender: Sender)
      extends ReactiveSenderProtocol(p, "dummy")
    {
      val next = Set(me, me.map(i => (i+1) % N))
      def addMeAt(idx: Int): UnaryOperator[IndexedSeq[Set[PID]]] =
      {
        val op: UnaryOperator[IndexedSeq[Set[PID]]] =
          (seq: IndexedSeq[Set[PID]]) => seq.updated(idx, seq(idx) + me)
        op
      }
      onInit {
        if (me == p0) {
          logger.trace(s"@$me sending TestMNessage to $next")
          sender.send(TestMessage(me, next, 0, dummyPayload))
        }
      }
      onReceive[TestMessage] {
        case m @ TestMessage(from, to, seq, `dummyPayload`) if me.value == seq + 1 =>
          logger.trace(s"@$me got $m and retransmit")
          sender.send(TestMessage(me, next, seq + 1, dummyPayload))
          received.updateAndGet(addMeAt(seq))
      }
      onReceive[TestMessage] {
        case m @ TestMessage(from, to, seq, `dummyPayload`) =>
          logger.trace(s"@$me got $m and register myself")
          received.updateAndGet(addMeAt(seq))
      }
      onReceive[TestMessage] {
        case o =>
          logger.warn(s"@$me got unrecognized message $o")
          assert(false)
      }
      assertResult(Seq(this))(dispatcher.protocolsFor(classTag[TestMessage]))
    }

    val topology    = Clique(numProcesses)
    val initializer = ProcessInitializer.forProcess { p => DummyReactive(p, p.network) }
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    
    assertResult(0)(system.runningProcs.size)
    for ( (receivers, i) <- received.get().zipWithIndex ) {
      assertResult(Set(PID(i), PID((i+1) % numProcesses)) )(receivers)
    }
    
    assert(system.now > 0)
    assert(system.currentTime > Time.Zero)
  }
}
