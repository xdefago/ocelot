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

import com.typesafe.scalalogging.LazyLogging


/**
  * Created by defago on 2017/03/25.
  */
trait RunningProtocol
  extends Protocol
    with Runnable
    with LazyLogging
{
  /**
    * SUBCLASS MUST IMPLEMENT
    * @return
    */
  def run(): Unit
  
  def onStart(): Unit = {
    logger.trace(s"[$me] onStart()")
  }
  def onFinish(): Unit = {
    logger.trace(s"[$me] onFinish()")
  }
  
  def isRunning: Boolean  = _running
  def isFinished: Boolean = _finished
  def hasStarted: Boolean = _started
  def isActive = hasStarted || ! isFinished
  
  private var _running = false
  private var _started = false
  private var _finished = false
  
  protected[this] def scheduler = process.scheduler
  
  final def start (): Unit =
  {
    _started = true
    logger.trace(s"    start $me")
    scheduler.execute { () =>
      logger.trace(s"    run $me")
      _running = true
      onStart()
      run()
      onFinish()
      _finished = true
      _running  = false
    }
  }
}
