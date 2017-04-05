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

import java.io.PrintStream

import ocelot.kernel.{ Dispatcher, Protocol, Scheduler }


trait OcelotProcess extends Receiver
{
  def system: OcelotSystem
  def scheduler: Scheduler
  def pid: PID
  def out: PrintStream
  def dispatcher: Dispatcher

  def name: String = pid.name
  def deliver(e: Event) = dispatcher.deliver(e)
  def preStart(): Unit = {}
  
  def start(): Unit
  def isRunning: Boolean
  def hasPendingMessage: Boolean
  
  def network: Sender

  def program: Option[ActiveProtocol]
  def protocols: Set[Protocol]
  protected[ocelot] def register(proto: ReactiveProtocol)
  protected[ocelot] def register(active: ActiveProtocol)
}
