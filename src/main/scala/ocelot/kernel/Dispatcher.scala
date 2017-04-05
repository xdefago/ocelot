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

import java.io.{ OutputStream, PrintStream }

import com.typesafe.scalalogging.LazyLogging
import ocelot.Receiver
import ocelot.Event

import scala.reflect.ClassTag

trait Dispatcher extends Receiver with LazyLogging
{
  def unregister(proto: Receiver): Unit
  
  def registerFor(tag: ClassTag[_<:Event], proto: Receiver): Unit
  def unregisterFor(clazz: ClassTag[_<:Event], proto: Receiver): Unit
  def unregisterAllFor(clazz: ClassTag[_<:Event]): Unit
  def protocolsFor(clazz: ClassTag[_<:Event]) : Seq[Receiver]
  
  override def deliver(ev: Event): Unit =
  {
    val tag = ClassTag(ev.getClass)
    logger.trace(s"deliver (tag=$tag): m = $ev")
    val protocols = this.protocolsFor(tag)
    logger.trace(s"deliver (tag=$tag): protos = ${protocols.mkString(", ")}")
    for (proto <- protocols) {
      logger.trace(s"deliver $ev to $proto")
      proto.deliver(ev)
    }
  }
}

object Dispatcher
{
  def apply(): Dispatcher = new InheritanceDispatcher()
  
  def apply(handler: (Event)=>Unit): Dispatcher = new TrivialDispatcher(handler)
  
  def onOrphan(hook: Event=>Unit): Dispatcher =
    hook match {
      case r : Receiver => new InheritanceDispatcher(Some(r))
      case _            => new InheritanceDispatcher(Some(Receiver(hook)))
    }
  
  def withMessage(output: OutputStream = Console.err): Dispatcher =
    {
      val out = new PrintStream(output)
      Dispatcher.onOrphan { ev =>
        out.println (s"WARNING: orphan message (did you forget listenTo?): $ev(${ev.getClass.getName})")
      }
    }
}


class TrivialDispatcher(handler: (Event)=>Unit) extends Dispatcher
{
  def unregister(proto: Receiver) = {}
  
  def registerFor(tag: ClassTag[_<:Event], proto: Receiver) = {}
  def unregisterFor(clazz: ClassTag[_<:Event], proto: Receiver) = {}
  def unregisterAllFor(clazz: ClassTag[_<:Event]) = {}
  def protocolsFor(clazz: ClassTag[_<:Event]) : Seq[Receiver] = Seq(this)
  override def deliver(ev: Event): Unit = handler(ev)
}


class InheritanceDispatcher(orphanHook: Option[Receiver] = None) extends Dispatcher
{
  private val orphanEventHooks = orphanHook.toSeq
  
  private var protocolsForMessage =
    Map
      .empty[ClassTag[_<:Event], List[Receiver]]
      .withDefaultValue(Nil)
  
  def protocolsFor(tag: ClassTag[_<:Event]) : Seq[Receiver] =
  {
    val protocolList : Seq[Receiver] =
      for {
        key <- ancestryOf(tag)
        protocol <- protocolsForMessage(key)
      } yield protocol
    val protocols = protocolList.distinct
    if (protocols.nonEmpty) protocols else orphanEventHooks
  }
  
  def registerFor(tag: ClassTag[_<:Event], proto: Receiver): Unit =
  {
    val protocols = protocolsForMessage(tag)
    if (! protocols.contains(proto)) {
      protocolsForMessage = protocolsForMessage.updated(tag, proto :: protocols)
    }
  }
  
  def unregister(proto: Receiver): Unit =
  {
    protocolsForMessage = protocolsForMessage.mapValues(recvs => recvs.filterNot(_ == proto))
  }
  
  def unregisterFor(tag: ClassTag[_<:Event], proto: Receiver): Unit =
  {
    val protocols = protocolsForMessage(tag)
    if (protocols.contains(proto)) {
      val excluding = protocols.filterNot(_ eq proto)
      protocolsForMessage = protocolsForMessage.updated(tag, excluding)
    }
  }
  
  def unregisterAllFor(tag: ClassTag[_<:Event]): Unit =
    protocolsForMessage = protocolsForMessage.updated(tag, Nil)

  private def ancestryOf[T<:Event](tag: ClassTag[T]): Seq[ClassTag[T]] =
  {
    val maxDepth = 25 // TODO: should be parametrized somewhere
    var seq = Seq.empty[ClassTag[T]]
    var clazz = tag.runtimeClass
    var depth = 1
    do {
      seq :+= ClassTag(clazz)
      if (clazz == classOf[Event]) return seq
      if (depth > maxDepth) throw new RuntimeException(s"Event class hierarchy running too deep ($depth): $seq")
      clazz = clazz.getSuperclass
      depth += 1
    } while (true)
    ???
  }
}
