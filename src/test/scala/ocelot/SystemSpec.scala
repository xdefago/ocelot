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

import ocelot.kernel.{ Protocol, Scheduler }
import ocelot.network.Network
import ocelot.network.sim.ConstantDelaySimNetwork
import ocelot.time.Time
import ocelot.topology.{ Clique, Topology }
import org.scalatest.FlatSpec

import scala.concurrent.duration.Duration

/**
  * Created by defago on 29/03/2017.
  */
class SystemSpec extends FlatSpec
{
  val randomSeed = 1L
  val timeUnit   = Time.fromNanos(1)
  
  behavior of "OcelotSimSystem"
  
  it should "initialize correctly" in {
    val numProcesses = 3
    val protoName    = "protoName"

    var created = Seq.empty[PID]
    var protocolMap = Map.empty[PID, Set[Protocol]].withDefault(_=>Set.empty[Protocol])
    var processes = Set.empty[OcelotProcess]

    val topology = Clique(numProcesses)
    val initializer = ProcessInitializer.forProcess { p =>
      created :+= p.pid
      processes += p
      val proto = ReactiveSenderProtocol(p, protoName, p.network) { case _ => /* no nothing */ }
      val set: Set[Protocol] = protocolMap(p.pid)
      protocolMap = protocolMap.updated(p.pid,  protocolMap(p.pid) + proto)
    }
    val system = new OcelotSimSystem(topology, initializer, randomSeed)

    assert(system.network != null)
    assert(system.processMap != null)
    assert(system.processMap.nonEmpty)
    assertResult(topology.numberOfVertices)(system.processMap.size)
    assertResult(topology.numberOfVertices)(created.size)
    assertResult(topology.processSet)(created.toSet)
    assertResult(numProcesses)(system.processNum)
    assertResult(processes)(system.processes.toSet)

    for {
      pid <- topology.processSet
      process <- system.processMap.get(pid)
      protocols = process.protocols
    } {
      assertResult(1)(protocols.size)
      assertResult(protocolMap(pid))(protocols)
      assertResult(protoName)(protocols.head.id.value)
    }
    
    assertResult(Time.Zero.toNanos)(system.now)
    assertResult(Time.Zero)(system.currentTime)
  }
  
  it should "execute active processes" in {
    val numProcesses = 3

    val executed = new AtomicReference(Set.empty[PID])
    
    case class DummyProgram(p: OcelotProcess) extends ActiveProtocol(p, "dummy") {
      override def run () = {
        val addMe : UnaryOperator[Set[PID]] = (s:Set[PID]) => s + me
        executed.updateAndGet( addMe )
      }
    }
    
    val topology    = Clique(numProcesses)
    val initializer = ProcessInitializer.forProcess { p => DummyProgram(p) }
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    
    assertResult(0)(system.runningProcs.size)
    assertResult(topology.processSet)(executed.get())
    
    assertResult(Time.Zero.toNanos)(system.now)
    assertResult(Time.Zero)(system.currentTime)
  }
  
  it should "execute reactive processes" in {
    val numProcesses = 3

    val executed = new AtomicReference(Set.empty[PID])

    case class DummyReactive(p: OcelotProcess) extends ReactiveProtocol(p, "dummy") {
      onInit {
        val addMe : UnaryOperator[Set[PID]] = (s:Set[PID]) => s + me
        executed.updateAndGet( addMe )
      }
    }

    val topology    = Clique(numProcesses)
    val initializer = ProcessInitializer.forProcess { p => DummyReactive(p) }
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    
    assertResult(0)(system.runningProcs.size)
    assertResult(topology.processSet)(executed.get())
    
    assertResult(Time.Zero.toNanos)(system.now)
    assertResult(Time.Zero)(system.currentTime)
  }
}
