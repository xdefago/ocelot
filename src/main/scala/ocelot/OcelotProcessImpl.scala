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
import ocelot.util.SequentialPrintStream


/**
  * Created by defago on 2017/03/28.
  */
class OcelotProcessImpl(
    val system: OcelotSystem,
    val pid: PID,
    out0: PrintStream = Console.out,
    val dispatcher: Dispatcher = Dispatcher.withMessage(Console.err)
) extends OcelotProcess
{
  private var _program   = Option.empty[ActiveProtocol]
  private var _protocols = Set.empty[ReactiveProtocol]
  private var _locked    = false
 
  val out: PrintStream = new SequentialPrintStream(out0)
  
  def scheduler: Scheduler = system.scheduler
  
  def program: Option[ActiveProtocol] = _program
  
  private lazy val _networkSender = system.network.senderFor(pid)
  def network = _networkSender
  
  def protocols = _protocols.toSet[Protocol]
  
  final def start () =
  {
    _protocols.foreach(_.preStart())
    _program.foreach(_.preStart())
    _locked = true
    _protocols.foreach(_.start() )
    _program.foreach( _.start() )
  }
  
  def isRunning         = _program.fold(false)(_.isRunning)
  def hasPendingMessage = _program.fold(false)(_.hasPendingMessage)
  
  protected[ocelot] def register (proto: ReactiveProtocol) =
  {
    if ( _locked ) throw new IllegalStateException(s"Attempt to add protocol ${proto.id} to process $name after it was locked")
    
    _protocols += proto
  }
  
  protected[ocelot] def register (active: ActiveProtocol) =
  {
    if ( _locked ) throw new IllegalStateException(s"Attempt to add program ${active.id} to process $name after it was locked")
    _program.foreach { other =>
      throw new IllegalStateException(s"Attempt to add program ${active.id} to process $name after ${other.id}")
    }
    
    _program = Some(active)
  }
}
