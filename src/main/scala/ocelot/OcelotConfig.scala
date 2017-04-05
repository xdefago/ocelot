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

import ocelot.kernel._
import ocelot.topology.Topology

import scala.util.Random

case class OcelotConfig(
    random: Random,
    scheduler: Scheduler,
    topology: Topology,
              //  randomGenFactory:   RandomGenFactory,
              //  schedulerFactory:   SchedulerFactory,
              //  topologyFactory:    TopologyFactory,
    networkFactory:     NetworkFactory,
    processFactory:     ProcessFactory,
    processInitializer: ProcessInitializer
)
