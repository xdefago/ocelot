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

/**
 * Basic trait for implementing the process initialization.
 *
 * A process needs an initializer class to instantiate its protocols, register them, and connect
 * them together.
 * Such a class must be a subclass of [[ProcessInitializer]] and contain the code for
 * initialization as shown in the example below.
 *
 * {{{
 * class LamportMutexInitializer extends ProcessInitializer
 * {
 *   forProcess { p =>
 *     // create protocols
 *     val app   = p.register(new MutexApplication(p))
 *     val clock = p.register(new protocol.LamportClock(p))
 *     val mutex = p.register(new LamportMutex(p, clock))
 *     val fifo  = p.register(new protocol.FIFOChannel(p))
 *
 *     // connect protocols
 *     app   --> mutex
 *     mutex --> clock
 *     clock --> fifo
 *   }
 * }
 * }}}
 *
 * Without a call to [[ProcessInitializer.forProcess]], the initializer does nothing by default, thus resulting in
 * an "empty" process.
 */
trait ProcessInitializer extends Function[OcelotProcess, Unit]
{
  private var init : Function[OcelotProcess, Unit] = { _ => /* nothing */ }
  final def apply(p: OcelotProcess)     { init(p) }

  /**
   * Initialization code for a process.
   *
   * The argument is an initializer function which, given a process configuration as argument,
   * creates the protocols for that process, registers them, and connects them. For instance,
   * {{{
   * class LamportMutexInitializer extends ProcessInitializer
   * {
   *   forProcess { p =>
   *     // create protocols
   *     val app   = p.register(new MutexApplication(p))
   *     val clock = p.register(new protocol.LamportClock(p))
   *     val mutex = p.register(new LamportMutex(p, clock))
   *     val fifo  = p.register(new protocol.FIFOChannel(p))
   *
   *     // connect protocols
   *     app   --> mutex
   *     mutex --> clock
   *     clock --> fifo
   *   }
   * }
   * }}}
   *
   * @param initializer  the block which, given a process configuration as argument, creates and
   * connects the protocols.
   */
  protected[this] def forProcess (initializer: OcelotProcess => Unit) { init = initializer }
}


object ProcessInitializer
{
  
  
  import scala.util.Try
  
  
  /**
    * creates a new anonymous instance of [[ProcessInitializer]] from the given function.
    *
    * @param initializer initialization function of the initializer. Takes a process system and
    *                    initializes its protocols.
    *
    * @return the new instance of [[ProcessInitializer]].
    */
  def apply (initializer: OcelotProcess => Unit): ProcessInitializer =
    forProcess(initializer)
  
  /**
    * creates a new anonymous instance of [[ProcessInitializer]] from the given function.
    *
    * @param initializer initialization function of the initializer. Takes a process system and
    *                    initializes its protocols.
    *
    * @return the new instance of [[ProcessInitializer]].
    */
  def forProcess (initializer: OcelotProcess => Unit): ProcessInitializer =
    new ProcessInitializer
    {forProcess(initializer)}
  
  /**
    * creates a new instance of [[ProcessInitializer]] from the given class name.
    *
    * @param className name of the subclass of [[ProcessInitializer]] to instantiate.
    *
    * @return `Success` with the new instance if successful, or `Failure` otherwise.
    */
  def forName (className: String): Try[ProcessInitializer] =
    Try {
      Class
        .forName(className)
        .asSubclass(classOf[ProcessInitializer])
        .newInstance()
    }
}
