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

import ocelot.kernel.SendingProtocol

/**
  * Created by defago on 2017/03/25.
  */
abstract class ReactiveSenderProtocol(p: OcelotProcess, nickname: String, sender: Sender)
  extends ReactiveProtocol(p, nickname)
    with SendingProtocol
    with SendingProtocolOps
{
  /**
    * Returns the set of neighbors as logically seen by the above protocol.
    *
    * For instance, if the protocol implements routing, the neighbors become all other processes
    * in the same connected component. Likewise, if the protocol implements an overlay network,
    * the neighbors are only the processes in direct contact with the sending process.
    *
    * By default, the method returns the same set as its underlying sender.
    *
    * @return the set of neighbors as logically seen by the above
    *         protocol.
    */
  def neighbors: Set[PID] = sender.neighbors
}


object ReactiveSenderProtocol
{
  def apply(p: OcelotProcess, nickname: String, sender: Sender)(handler: PartialFunction[Event, Unit]): ReactiveSenderProtocol
    = new ReactiveSenderProtocol(p, nickname, sender) { onReceive(handler) }
}
