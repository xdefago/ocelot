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

import ocelot.PID

/**
  * Created by defago on 2017/03/17.
  */
case class Union(left: Topology, right: Topology) extends Topology
{
  def processSet: Set[PID]     = left.processSet union right.processSet

  def processes: Iterator[PID] = left.processes ++ right.processes.filterNot(left.contains)
  def edges: Iterator[Edge]    = edgeSet.toIterator
  private lazy val edgeSet = (left.edges ++ right.edges).toSet
  
  def neighborsOf(p: PID): Option[Set[PID]] =
    left.neighborsOf(p)
      .map(_ union right.neighborsOf(p).getOrElse(Set.empty))

  def contains(process: PID): Boolean = processSet.contains(process)
  
  def isDirected: Boolean  = left.isDirected || right.isDirected
  lazy val isConnected: Boolean =
    (left.isConnected && right.isConnected && left.processes.exists(right.processSet.contains)) ||
    ???

  def isWeighted: Boolean  = left.isWeighted || right.isWeighted

  def numberOfEdges    = edges.size
  def numberOfVertices = processSet.size
}
