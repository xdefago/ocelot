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
class DirectedAdjacencyMap(val map: Map[PID,Set[PID]]) extends Topology
{
  def processes: Iterator[PID] = map.keysIterator
  def edges: Iterator[Edge]    = map.iterator.flatMap{kv => val (k,v) = kv ; v.map(Edge(k,_)) }

  def processSet: Set[PID] = map.keySet
  def neighborsOf(p: PID): Option[Set[PID]] = map.get(p)

  def contains(process: PID): Boolean = map.contains(process)

  def numberOfEdges    = map.map(_._2.size).sum
  def numberOfVertices = map.size

  def isDirected: Boolean  = true
  def isConnected: Boolean = ???
  def isWeighted: Boolean  = false
}
