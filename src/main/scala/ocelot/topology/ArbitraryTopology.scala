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
import scalax.collection.Graph
import scalax.collection.GraphPredef._

/**
  * Created by defago on 2017/03/17.
  */
class ArbitraryTopology(var graph: Graph[PID,EdgeLikeIn]) extends Topology
{
  def processSet = graph.nodes.toOuter
  def processes  = graph.nodes.iterator.map(_.toOuter)
  def edges      = graph.edges.iterator.map(_.toOuter).map(e => Edge(e._1, e._2))
  def isDirected  = graph.isDirected
  def isConnected = graph.isConnected
  def isWeighted  = false
  def numberOfEdges    = graph.graphSize
  def numberOfVertices = graph.order
  def neighborsOf (process: PID) =
    graph
      .find (process)
      .map { _.outNeighbors.map { _.toOuter } }
  
  def contains (process: PID) = graph.contains(process)
}
