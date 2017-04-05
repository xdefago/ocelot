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

import ocelot.{ OcelotProcess, ProtoID }


/**
  * Created by defago on 2017/03/25.
  */
trait Protocol
  // extends Receiver // with Sender
{
  /**
   * returns the identifier of the process on which this instance of the protocol is running.
   * @return interface of the process
   */
  def process: OcelotProcess

  /**
   * returns the identifier of the protocol
   * @return identifier of the protocol
   */
  def id : ProtoID

  /**
   * returns the name of the protocol
   * @return name of the protocol
   */
  def name = s"${process.name}:${this.id.name}"

  /**
   * returns the output target for this protocol
   */
  protected[this] def out = process.out

  /**
   * override this method to perform initializations just before the process begins to run.
   */
  def onInit() = {}

  
  /**
   * override this method to perform take downs after all processes have finished and before the
   * process shuts down.
   */
  def onShutdown() = {}
  
  /**
   * identifier of the process in which the protocol is running.
   */
  protected[this] lazy val me  = process.pid

  def preStart()
  
  def start()
}
