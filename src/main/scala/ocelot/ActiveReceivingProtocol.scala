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

import ocelot.kernel.ReceivingProtocol
import ocelot.time.Time

import scala.collection.immutable
import scala.reflect.ClassTag

/**
  * Created by defago on 2017/03/25.
  */
abstract class ActiveReceivingProtocol(p: OcelotProcess, nickname: String)
  extends ActiveProtocol(p, nickname)
    with ReceivingProtocol
{
  override def hasPendingMessage = receiveQueue.nonEmpty
  
  private var receiveQueue = immutable.Queue.empty[Event]
  protected[this] def enqueueEvent(ev: Event): Unit =
  {
    receiveQueue = receiveQueue :+ ev
  }
  
  override def preStart () = { onInit() }
  
  protected[this] def takeNextEvent(): Option[Event] =
  {
    if ( receiveQueue.nonEmpty ) {
      val (ev, tail) = receiveQueue.dequeue
      receiveQueue = tail
      Some (ev)
    }
    else None
  }
  
  protected[this] final override def fallbackRecv: PartialFunction[Event, Unit] =
  {
    case e: Event =>
        logger.trace(s"${id.name} : ENQUEUE ${e.toPrettyString}")
        enqueueEvent(e)
  }
  
  protected[this] final override
  def onReceive[E <: Event] (pf: PartialFunction[E, Unit]) (implicit tag: ClassTag[E]): Unit =
  {
    logger.warn("Calling onReceive() on an ActiveProtocol is EXPERIMENTAL and may yield unexpected results!")
    super.onReceive(pf)(tag)
  }
  
  def RECEIVE[E <: Event](implicit tag: ClassTag[E]): E = {
    var res  = Option.empty[Event]
    scheduler.when { hasPendingMessage } {
      res  = takeNextEvent()
    }
    scheduler.waitUntil { res.nonEmpty }
    res match {
      case Some(e: E) => e
      case Some(e) =>
        logger.error(s"expecting subtype of $tag but got $e")
        throw new ClassCastException(s"expecting subtype of $tag but got $e")
      case e =>
        logger.error(s"unexpected behavior happened when expecting a message of type $tag (got $e)")
        throw new RuntimeException(s"unexpected behavior happened when expecting a message of type $tag (got $e)")
    }
  }

  def RECEIVE(withTimeout: Time): Event = {
    import ocelot.event.Timeout
    val deadline   = scheduler.currentTime + withTimeout
    val wakeupTask = scheduler.scheduleAt(deadline){ /* wake up */ }
    var res  = Option.empty[Event]
    scheduler.when { hasPendingMessage || (scheduler.currentTime >= deadline) } {
      if (hasPendingMessage)
        res = takeNextEvent()
      else
        res = Some(Timeout)
    }
    scheduler.waitUntil { res.nonEmpty }
    res match {
      case Some(e) => e
      case None =>
        logger.error(s"unexpected behavior happened when expecting a message")
        throw new RuntimeException(s"unexpected behavior happened when expecting a message")
    }
  }
  
  /**
   * Basic functionality for receiving together with pattern matching.
   *
   * Receiving is done either through [[Receive.apply]] or with [[Receive.withTimeout]].
   * Typical example:
   * {{{
   *   Receive {
   *     case Request(from, to, req) if (to-me).nonEmpty =>
   *       SEND(Request(me, to-me, req))
   *       SEND(Ack(me, from))
   *     case Ack(from, _) => countAcks += 1
   *   }
   * }}}
   *
   * or with a timeout:
   * {{{
   *   Receive.withTimeout(10.0) {
   *     case Request(from, to, req) if (to-me).nonEmpty =>
   *       SEND(Request(me, to-me, req))
   *       SEND(Ack(me, from))
   *     case Ack(from, _) => countAcks += 1
   *     case Timeout => SEND(Suspect(me, ALL-me))
   *   }
   * }}}
   */
  object Receive
  {
    import ocelot.event.Timeout
    /**
     * Receives and handles the next message in the queue, if any.
     *
     * If the receive queue is empty, the call blocks until a message arrives. When a message
     * is available, it is removed from the queue, and passed to a handler function (can also be
     * a partial function).
     *
     * Typical example:
     * {{{
     *   Receive {
     *     case Request(from, to, req, _) if (to-me).nonEmpty =>
     *       SEND(Request(me, to-me, req))
     *       SEND(Ack(me, from))
     *     case Ack(from, _, _) => countAcks += 1
     *   }
     * }}}
     *
     * Another use is to pass an anonymous function as the handler:
     * {{{
     *   Receive { m =>
     *     println(m)
     *     // other code pertaining to m
     *   }
     * }}}
     *
     * @param handler  function applied upon receiving a message. It can be a partial function.
     * @return the value returned by the handler
     */
    def apply(handler: Function[Event, Unit]) : Unit =
      handler(RECEIVE[Event])

    /**
     * Receives and handles the next message in the queue, if any, or times out.
     *
     * If the receive queue is empty, the call blocks until a message arrives or the call times out,
     * whichever occurs first. If a message is available before the timeout, it is removed from the
     * queue, and passed as argument to a handler function (which can also be a partial function).
     * If the timeout occurs before a message is received, the internal event [[Timeout]] is passed
     * instead.
     *
     * Typical example:
     * {{{
     *   Receive.withTimeout(Time.second) {
     *     case Request(from, to, req, _) if (to-me).nonEmpty =>
     *       SEND(Request(me, to-me, req))
     *       SEND(Ack(me, from))
     *     case Ack(from, _, _) => countAcks += 1
     *     case Timeout => SEND(Suspect(me, neighbors))
     *   }
     * }}}
     * @param timeout duration after which a [[Timeout]] is issued
     * @param handler function applied upon receiving a message. It can be a partial function.
     */
    def withTimeout(timeout: Time)(handler: Function[Event, Unit]) : Unit =
      handler(RECEIVE(timeout))
  }
}


abstract class Program(p: OcelotProcess, nickname: String)
  extends ActiveReceivingProtocol(p, nickname)

abstract class SendingProgram(p: OcelotProcess, nickname: String)
  extends Program(p, nickname)
    with SendingProtocolOps
{
  def sender: Sender
}
