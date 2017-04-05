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

import com.typesafe.scalalogging.LazyLogging
import ocelot.kernel.{ Dispatcher, Protocol, Scheduler }

import scala.reflect.ClassTag


trait Receiver extends Function[Event,Unit]
{
  /**
   * delivers a given event (message or signal)
   */
  def deliver(e: Event)
  final def apply (e: Event) = deliver(e)
}


object Receiver
{
  def from (handler: (Event)=> Unit) = this(handler)
  
  def apply(handler: (Event)=> Unit): Receiver =
    new Receiver
    {
      def deliver(e: Event) = handler(e)
    }
}


trait ReceiverImpl
  extends Receiver
    with LazyLogging
{ this: Protocol =>
  protected[this] def dispatcher: Dispatcher
  
  /**
    * delivers a given event (message or signal)
    */
  final def deliver(e: Event): Unit = (_recvHooks orElse fallbackRecv)(e)

  /**
    * adds a set of rules to be evaluated each time an event is received.
    * Rules are evaluated according to the order they have been added.
    *
    * @param pf rules to apply when receiving an event.
    */
  protected[this] def onReceive[E<:Event](pf: PartialFunction[E,Unit])(implicit tag: ClassTag[E]): Unit =
  {
    listenTo[E]
    _recvHooks = _recvHooks orElse pf.asInstanceOf[PartialFunction[Event,Unit]]
  }
  
  protected[this] def listenTo[E <: Event](implicit tag: ClassTag[E]) =
  {
    logger.trace(s"protocol $name: listenTo $tag")
    dispatcher.registerFor(tag, this)
  }
  
  /**
    * returns a fallback handler called if no onReceive clause was able to catch the event.
    * This method does nothing by default and is meant to be overriden.
    *
    * @return the fallback handler
    */
  protected[this] def fallbackRecv: PartialFunction[Event, Unit] = PartialFunction.empty

  protected[this] def onInit(action: =>Unit): Unit =
  {
    _initHooks = _initHooks :+ ( (_:Unit) => action )
  }
  
  def preStart(): Unit = {
    _initHooks.foreach(hook => hook(Unit))
  }
  
  private var _recvHooks = PartialFunction.empty[Event,Unit]
  private var _initHooks: Seq[Unit=>Unit] = Seq.empty
}

import scala.language.reflectiveCalls


trait SendingProtocolOps
{ this: {
    //def process: OcelotProcess
    def dispatcher: Dispatcher
  } =>
  
  protected[this] def DELIVER(ev: Event) = dispatcher.deliver(ev)
  
}

trait ActiveProtocolOps
{ this: {
    def process: OcelotProcess
  } =>
  
}
