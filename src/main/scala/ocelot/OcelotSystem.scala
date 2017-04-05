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

import kernel.Scheduler
import network.Network
import time.Time
import topology.Topology

import org.slf4j.{ Logger, LoggerFactory }
import scala.util.Random


/**
  * Created by defago on 2017/03/20.
  */


trait OcelotSystem
{
  /**
    * returns the number generator used in the system.
    *
    * @note
    * The intended purpose of using this generator throughout the code rather than the normal
    * procedure is so that the code can potentially be made deterministic, thus allowing for
    * reproducibility of executions. Note that this is a necessary condition for reproducibility,
    * but by no means sufficient since there are many other sources of non-determinism (time,
    * race conditions, etc). In fact, the current settings is not thread-safe and hence will not
    * provide deterministic results in the presence of [[ocelot.kernel.RunningProtocol]]
    *
    * @return random number generator.
    */
  def random    : Random
  def scheduler : Scheduler
  def topology  : Topology
  def network   : Network
  def processMap: Map[PID, OcelotProcess]
  
  /**
    * returns the elapsed time in nanoseconds.
    *
    * @return nanoseconds since starting the program
    *
    * @note
    * With 64-bits signed long, this leaves about 290 years until the counter overflows.
    */
  def now: Long         = scheduler.now
  def currentTime: Time = scheduler.currentTime
  
  def processNum = topology.numberOfVertices
  def processSet = topology.processSet
  def processes = processMap.values
}
