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

package ocelot.topology

import ocelot._

/**
  * Created by defago on 2017/03/17.
  */
class Clique(val processSet: Set[PID]) extends Topology
{
  def processes: Iterator[PID] = processSet.iterator
  def edges: Iterator[Edge] =
    for {
      a <- processes
      b <- processes if a < b
    } yield Edge(a,b)

  def neighborsOf(p: PID): Option[Set[PID]] = Some(processSet)

  def contains(process: PID): Boolean = processSet.contains(process)

  def isDirected: Boolean  = false
  def isConnected: Boolean = true
  def isWeighted: Boolean  = false

  def numberOfEdges    = processSet.size * (processSet.size - 1) / 2
  def numberOfVertices = processSet.size

  override def union(that: Topology): Topology = if (that.processSet.subsetOf(this.processSet)) this else super.union(that)
}

object Clique
{
  val empty = Clique(Set.empty)
  def apply(set: Set[PID]): Clique = new Clique(set)
  def apply(range: Range): Clique  = this(range.map(PID).toSet)
  def apply(n: Int): Clique        = this(0 until n)
  def apply(iter: Iterable[PID]): Clique = this(iter.toSet)
  def apply(pid: PID, pids: PID*): Clique = this(Set(pid +: pids:_*))
  def apply(): Clique = empty
}
