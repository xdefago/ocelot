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

import kernel._

import ch.qos.logback.classic.{ Level, Logger => LogbackLogger }
import ocelot.kernel.SimScheduler
import ocelot.time.Time
import ocelot.topology.Topology
import org.slf4j.{ Logger, LoggerFactory }
import ocelot.network.sim.ConstantDelaySimNetwork

import scala.concurrent.duration.Duration


/**
  * Created by defago on 2017/03/25.
  */
class OcelotSimMain(
  topology:   Topology,
  logLevel:    Level = Level.ERROR,
  logFile:     Option[String] = None,
  withTrace:   Boolean = false,
  randomSeed:  Long = scala.util.Random.nextLong
)(
    processInit: OcelotProcess => Unit
) extends App
{
  final val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[LogbackLogger]
  val initializer = ProcessInitializer.forProcess(processInit)
  logger.setLevel(logLevel)

//  if (withTrace) {
//    trace.Tracing.defaultTracer_=(trace.Tracer.consoleOnly)
//  }
  
  val system = new OcelotSimSystem(topology, initializer, randomSeed)
}
