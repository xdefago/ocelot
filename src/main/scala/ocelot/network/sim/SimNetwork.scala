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

import ocelot._
import ocelot.kernel.Scheduler
import ocelot.network._
import ocelot.topology.Topology

/**
  * Created by defago on 2017/03/18.
  */
trait SimNetwork extends Network
{
  protected[this] def scheduler: Scheduler
  protected[this] def topology: Topology
  
  /**
   * returns a mapping of process identifiers to their corresponding receiver.
   *
   * @return a mapping of process identifiers to their corresponding receiver.
   */
  //def receivers: Map[PID, Receiver]

  /**
   * returns the receiver corresponding to the process identifier given in argument, if any.
   * @param pid identifier of a process
   * @return receiver of the given process if found, or `None`.
   */
  def receiverFor(pid: PID): Option[Receiver]

  /**
   * perform an action on the receiver of a given process, if found.
   *
   * @param pid    identifier of a process
   * @param action action to execute on the receiver of the process
   * @tparam T     return type of the action
   * @return       return value of the action, or `None` if no receiver was found for the process
   */
  def doAtReceiver[T](pid: PID)(action: Receiver => T): Option[T]
  
}
