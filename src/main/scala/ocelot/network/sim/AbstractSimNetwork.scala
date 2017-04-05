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

package ocelot.network.sim

import com.typesafe.scalalogging.LazyLogging
import ocelot._
import ocelot.kernel.Scheduler
import ocelot.time.Time
import ocelot.topology.Topology
import ocelot.network.AbstractNetwork

import scala.util.Random

/**
  * Created by defago on 2017/03/18.
  */
abstract class AbstractSimNetwork (
    scheduler0: Scheduler,
    topology0: Topology,
    receivers0: => Map[PID, Receiver],
    random0: Random = Random
)
  extends AbstractNetwork
    with SimNetwork
    with LazyLogging
{
  protected def delayForMessage(m: Message, to: PID): Time
  
  protected[this] def scheduler: Scheduler = scheduler0
  protected[this] def topology: Topology   = topology0
  protected[this] def random: Random       = random0
  
  /**
    * returns a mapping of process identifiers to their corresponding receiver.
    *
    * @return a mapping of process identifiers to their corresponding receiver.
    */
  private lazy val receivers: Map[PID, Receiver] = receivers0

  def receiverFor(pid: PID): Option[Receiver] = receivers.get(pid)

  def doAtReceiver[T](pid: PID)(action: Receiver => T): Option[T] = receiverFor(pid).map(action)
  
  /**
    * returns a mapping of process identifiers to their corresponding sender.
    *
    * @return a mapping of process identifiers to their corresponding sender.
    */
  //def senders: Map[PID, Sender] = _senders
  
  //private lazy val _senders = topology.processes.map(p => p -> createSenderFor(p)).toMap

  //protected[this] def createSenderFor(p: PID): Sender
  protected[this] def createSenderFor(p: PID): Sender = new SenderFor(p)
  private class SenderFor(p: PID) extends Sender
  {
    def neighbors = topology.neighborsOf(p).getOrElse(Set.empty) + p
    override def send(event: Event): Unit =
    {
      logger.trace(s"network.Sender($p) send($event)")
      event match {
        case m: BroadcastMessage =>
          for (to <- neighbors if to != m.from) {
            scheduleMessage (m, to)
          }
        case m: Message =>
          // Schedule message for delivery
          for (to <- m.destinations intersect neighbors) {
            scheduleMessage (m, to)
          }
          
        case _: Signal =>
          // drop signals
      }
    }
    protected[this] def scheduleMessage(m: Message, to: PID) =
    {
      for (receiver <- receiverFor(to)) {
        scheduler.scheduleAfter(delayForMessage(m, to)) {
          logger.trace(s"at ${scheduler.now} deliver message to $to: $m")
          receiver.deliver(m)
        }
      }
    }
  }
}
