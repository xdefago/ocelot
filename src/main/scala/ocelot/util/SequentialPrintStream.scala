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

package ocelot.util

import java.io.PrintStream
import java.util.Locale


/**
  * Created by defago on 06/04/2017.
  */
class SequentialPrintStream(out: PrintStream) extends PrintStream(out)
{
  override def printf (format: String, args: AnyRef*) =
    SequentialPrintStream.synchronized { super.printf(format, args: _*) }
  override def printf (
      l: Locale,
      format: String,
      args: AnyRef*
  ) = SequentialPrintStream.synchronized { super.printf(l, format, args: _*) }
  override def println () = SequentialPrintStream.synchronized { super.println() }
  override def println (x: Boolean) = SequentialPrintStream.synchronized { super.println(x) }
  override def println (x: Char) = SequentialPrintStream.synchronized { super.println(x) }
  override def println (x: Int) = SequentialPrintStream.synchronized { super.println(x) }
  override def println (x: Long) = SequentialPrintStream.synchronized { super.println(x) }
  override def println (x: Float) = SequentialPrintStream.synchronized { super.println(x) }
  override def println (x: Double) = SequentialPrintStream.synchronized { super.println(x) }
  override def println (x: Array[Char]) = SequentialPrintStream.synchronized { super.println(x) }
  override def println (x: String) = SequentialPrintStream.synchronized { super.println(x) }
  override def println (x: scala.Any) = SequentialPrintStream.synchronized { super.println(x) }
  override def flush () = SequentialPrintStream.synchronized { super.flush() }
  override def write (b: Int) = SequentialPrintStream.synchronized { super.write(b) }
  override def write (buf: Array[Byte], off: Int, len: Int) =
    SequentialPrintStream.synchronized { super.write(buf, off, len) }
  override def format (format: String, args: AnyRef*) =
    SequentialPrintStream.synchronized { super.format(format, args: _*) }
  override def format (
      l: Locale,
      format: String,
      args: AnyRef*
  ) = SequentialPrintStream.synchronized { super.format(l, format, args: _*) }
  override def print (b: Boolean) = SequentialPrintStream.synchronized { super.print(b) }
  override def print (c: Char) = SequentialPrintStream.synchronized { super.print(c) }
  override def print (i: Int) = SequentialPrintStream.synchronized { super.print(i) }
  override def print (l: Long) = SequentialPrintStream.synchronized { super.print(l) }
  override def print (f: Float) = SequentialPrintStream.synchronized { super.print(f) }
  override def print (d: Double) = SequentialPrintStream.synchronized { super.print(d) }
  override def print (s: Array[Char]) = SequentialPrintStream.synchronized { super.print(s) }
  override def print (s: String) = SequentialPrintStream.synchronized { super.print(s) }
  override def print (obj: scala.Any) = SequentialPrintStream.synchronized { super.print(obj) }
  override def append (csq: CharSequence) = SequentialPrintStream.synchronized { super.append(csq) }
  override def append (csq: CharSequence, start: Int, end: Int) =
    SequentialPrintStream.synchronized { super.append(csq, start, end) }
  override def append (c: Char) = SequentialPrintStream.synchronized { super.append(c) }
  override def write (b: Array[Byte]) = SequentialPrintStream.synchronized { super.write(b) }
}


object SequentialPrintStream
