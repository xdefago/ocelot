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
import ocelot.time.Time
import ocelot.kernel.Scheduler
import ocelot.topology.Topology

import scala.util.Random

/**
  * Created by defago on 2017/03/25.
  */
class ConstantDelaySimNetwork(
    scheduler0: Scheduler,
    topology0: Topology,
    receivers0: => Map[PID, Receiver],
    random0: Random = Random
)(delay: Time)
  extends AbstractSimNetwork(scheduler0, topology0, receivers0, random0)
{
  protected def delayForMessage (m: Message, to: PID): Time = delay
}
