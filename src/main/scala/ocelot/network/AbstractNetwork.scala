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

package ocelot.network

import ocelot._

import scala.collection.mutable

/**
  * Base class for all network implementations.
  *
  * Created by defago on 2017/03/25.
  */
abstract class AbstractNetwork extends Network
{
  // def send(m: Event)(implicit pid: PID) = senderFor(pid).send(m)

  /**
    * hook called before the network is started.
    * Does nothing by default.
    */
  def preStart(): Unit = {}

  /**
    * hook called upon starting the network.
    * Does nothing by default.
    */
  def start(): Unit = {}

  def senderFor(pid: PID): Sender = // senders.get(pid)
    senders.getOrElseUpdate(pid, createSenderFor(pid))
  
  private val senders = mutable.Map.empty[PID, Sender]
  
  protected[this] def createSenderFor(p: PID): Sender
}
