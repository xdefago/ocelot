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

import ocelot.kernel.{ SchedulerException, SimScheduler }
import ocelot.time.Time
import ocelot.topology.Topology
import ocelot.network.sim._
import org.slf4j.LoggerFactory

/**
  * Created by defago on 29/03/2017.
  */
class OcelotSimSystem(
  topology0:   Topology,
  initializer: ProcessInitializer,
  randomSeed:  Long = scala.util.Random.nextLong
) extends OcelotSystem
{
  val logger = LoggerFactory.getLogger(GlobalConstants.OCELOT_LOGGER_NAME)
  
  val random    = new scala.util.Random(randomSeed)
  val scheduler = new SimScheduler()
  val topology  = topology0
  lazy val network   = new RandomDelaySimNetwork(scheduler, topology, processMap, random)(Time.fromNanos(1000000))
  lazy val processMap: Map[PID, OcelotProcess] = {
    for {
      pid <- topology.processes
      out = new java.io.PrintStream(new util.LineHeadOutputStream(Console.out, s"$pid: "))
      process = new OcelotProcessImpl(this, pid, out)
      _ = initializer(process)
    } yield pid -> process
  }.toMap[PID, OcelotProcess]
  
  /*
   * Enter run loop
   */
  
  // prestart network
  logger.trace("[INIT] prestarting network")
  network.preStart()

  logger.trace("[INIT] prestarting processes")
  var started = false
  processMap.values.foreach { p =>
    logger.trace(s"[INIT]      - prestarting ${p.pid }")
    p.preStart()
  }
  
  logger.trace("[INIT] starting network")
  network.start()

  logger.trace("[INIT] starting processes")
  processMap.values.foreach { p =>
    logger.trace(s"[INIT]      - starting ${p.pid}")
    p.start()
  }
  
  // enter main loop
  logger.trace("[INIT] entering mainloop")
  
  try {
    scheduler.runMainloop()

    logger.trace(s"running processes = ${processes.count(_.isRunning)}")
    logger.info (s"Simulation DONE at ${currentTime.asSeconds} ($now ns)")
  }
  catch {
    case SchedulerException(e) =>
      logger.error(s"reporting exception: $e")
      throw e
      
    case e : InterruptedException =>
      logger.error(s"SIMULATION ABORTED at ${currentTime.asSeconds} ($now ns): $e")
  }

  val runningProcs = processMap.values.filter( _.isRunning )
  if (runningProcs.nonEmpty) {
    logger.error(s"... with unfinished processes: ${runningProcs.map(_.pid).mkString(" ")}")
  } else {
    logger.info(s"... with all processes reaching completion")
  }
}
