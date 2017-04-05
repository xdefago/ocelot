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


trait Topology
{
  def processSet : Set[PID]

  def processes: Iterator[PID]
  def edges:     Iterator[Edge]

  def isDirected: Boolean
  def isConnected: Boolean
  def isWeighted: Boolean

  def numberOfEdges: Int
  def numberOfVertices: Int

  def neighborsOf(process: PID): Option[Set[PID]]

  def contains(process: PID): Boolean

  def isEmpty  = processSet.isEmpty
  def nonEmpty = processSet.nonEmpty

  def size: Int = numberOfVertices

  def union    (that: Topology): Topology = Union(this, that)
  def diff     (that: Topology): Topology = if (that.processSet.intersect(this.processSet).isEmpty) Topology.empty else ???
  def intersect(that: Topology): Topology = ??? // if (that.processSet.intersect(this.processSet).isEmpty) this else ???
}


object Topology
{
  def empty: Topology = Empty

  case object Empty extends Topology
  {
    def processSet : Set[PID]    = Set.empty

    def processes: Iterator[PID] = Iterator.empty
    def edges: Iterator[Edge]    = Iterator.empty

    def isDirected: Boolean  = false
    def isConnected: Boolean = true
    def isWeighted: Boolean  = false

    def numberOfEdges: Int    = 0
    def numberOfVertices: Int = 0

    def neighborsOf(process: PID): Option[Set[PID]] = None

    def contains(process: PID): Boolean = false

    override def union    (that: Topology): Topology = that
    override def diff     (that: Topology): Topology = this
    override def intersect(that: Topology): Topology = this
  }
}










