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


trait Sender
{
  def neighbors: Set[PID]
  
  /**
    * sends a given event (message or signal).
    */
  def send(e: Event)
}


trait SenderImpl extends Sender
{
  /**
    * sends a given event (message or signal).
    */
  final def send(e: Event): Unit = (_sendHooks orElse fallbackSend)(e)
  
  /**
    * adds a set of rules to be evaluated each time an event is sent.
    * Rules are evaluated according to the order they have been added.
    *
    * @param pf rules to apply when sending an event.
    */
  protected[this] def onSend(pf: PartialFunction[Event,Unit]): Unit =
  {
    _sendHooks = _sendHooks orElse pf
  }

  /**
    * returns a fallback handler called if no onSend clause was able to catch the event.
    * This method does nothing by default and is meant to be overriden.
    *
    * @return the fallback handler
    */
  protected[this] def fallbackSend: PartialFunction[Event, Unit] = PartialFunction.empty
  
  private var _sendHooks = PartialFunction.empty[Event,Unit]
}
