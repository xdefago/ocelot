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

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

import com.typesafe.scalalogging.LazyLogging

import scala.reflect.classTag
import ocelot.topology.Clique
import org.scalatest.FlatSpec


/**
  * Created by defago on 2017/04/01.
  */
class ReactiveProtocolSpec
  extends FlatSpec
    with LazyLogging
{
  val randomSeed = 1L
  
  case class WrappingMessage(m: Message) extends Wrapper(m)
  case class WrappingProtocol(p: OcelotProcess, sender: Sender)
    extends ReactiveSenderProtocol(p, "wrapping")
  {
    onSend {
      case m: Message =>
        logger.trace(s"$me wrap-sending $m")
        sender.send(WrappingMessage(m))

      case _ => assert(false)
    }
    onReceive[WrappingMessage] {
      case m =>
        logger.trace(s"$me got wrapped $m")
        DELIVER(m.m)
    }
  }
  
  case class Token(from: PID, to: PID, sn: Int) extends UnicastMessage
  case class TokenRound(p: OcelotProcess, sender: Sender)(act: (Token)=>Unit)
    extends ReactiveSenderProtocol(p, "token")
  {
    val next = me.map(i => (i+1) % sender.neighbors.size)
    onInit {
      if (me == PID(0)) {
        logger.trace(s"$me initialized and sending token")
        sender.send(Token(me, next, 0))
      }
      else {
        logger.trace(s"$me initialized")
      }
    }
    onReceive[Token] {
      case tok @ Token(_,_,sn) if sn+1 == me.value =>
        logger.trace(s"$me will resend token $tok")
        sender.send(Token(me, next, sn+1))
        act(tok)
    }
    onReceive[Token] {
      case tok: Token =>
        logger.trace(s"$me got token $tok")
        act(tok)
    }
  }
  
  
  behavior of "ReactiveSenderProtocol"
  
  it should "stack work appropriately when stacked" in {
    val numProcesses = 5
    val received = new AtomicReference(IndexedSeq.tabulate(numProcesses){ _ => Set.empty[PID] })
    val topology    = Clique(numProcesses)
    
    val initializer = ProcessInitializer.forProcess { p =>
      def addMeAt(idx: Int): UnaryOperator[IndexedSeq[Set[PID]]] =
      {
        val op: UnaryOperator[IndexedSeq[Set[PID]]] =
          (seq: IndexedSeq[Set[PID]]) => seq.updated(idx, seq(idx) + p.pid)
        op
      }
      
      val wrap  = WrappingProtocol(p, p.network)
      val token = TokenRound      (p, wrap) { tok =>
        received.updateAndGet(addMeAt(tok.sn))
      }
      assertResult(2)(p.protocols.size)
      assertResult(1)(p.dispatcher.protocolsFor(classTag[WrappingMessage]).size)
      assertResult(1)(p.dispatcher.protocolsFor(classTag[Token]).size)
    }
    
    val system = new OcelotSimSystem(topology, initializer, randomSeed)
    
    assertResult(0)(system.runningProcs.size)
    for ( (receivers, i) <- received.get().zipWithIndex ) {
      assertResult(Set(PID((i+1) % numProcesses)) )(receivers)
    }
  }
}
