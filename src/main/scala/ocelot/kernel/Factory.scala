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
import ocelot.network.Network
import ocelot.topology.Topology
import scala.util.Random

/**
  * Created by defago on 2017/03/20.
  */
sealed trait Factory[T] extends Function[OcelotSystem,T]
{
  val underlying: Function[OcelotSystem,T]
  def apply(system: OcelotSystem):T = underlying(system)
}

case class ProcessFactory  (underlying: OcelotSystem => PID => OcelotProcess) extends Factory[PID => OcelotProcess]
case class NetworkFactory  (underlying: OcelotSystem => Network)              extends Factory[Network]
case class TopologyFactory (underlying: OcelotSystem => Topology)             extends Factory[Topology]
case class SchedulerFactory(underlying: OcelotSystem => Scheduler)            extends Factory[Scheduler]
case class RandomGenFactory(underlying: OcelotSystem => Random)               extends Factory[Random]
