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
package ocelot.time

import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

/**
  * Created by defago on 01/03/2017.
  */

case class Time(value: Duration) extends Ordered[Time]
{
  def compare (that: Time) = this.value compare that.value
  
  def +(that: Time): Time      = map2(that)(_ + _)
  def -(that: Time): Time      = map2(that)(_ - _)
  def *(factor: Double): Time  = copy(this.value * factor)
  def /(that: Time): Double    = this.value / that.value
  def /(divisor: Double): Time = copy(this.value / divisor)
  def unary_-                  = copy(-this.value)
  def unit                     = value.unit
  
  def toNanos   = value.toNanos
  def toMicros  = value.toMicros
  def toMillis  = value.toMillis
  def toSeconds = value.toSeconds
  def toUnit(unit: TimeUnit): Double = value.toUnit(unit)
  
  def before (that: Time) = this < that
  def after  (that: Time) = this > that
  
  def max(that: Time):Time = if (this < that)  that else this
  def min(that: Time):Time = if (this <= that) this else that
  
  def map2(that: Time)(f: (Duration,Duration)=>Duration) : Time = copy(f(this.value, that.value))
  
  def toDuration : Duration = value

  // OUTPUT

  /**
   * returns a string representation of this time expressed in nanoseconds.
   *
   * @return string representation
   */
  def asNanoseconds: String = s"${toNanos}ns"

  /**
   * returns a string representation of this time expressed in seconds with precision to the microsecond.
   *
   * @return string representation
   */
  def asSeconds: String   = Time.formatTimeSeconds(this) + "s"
}


object Time
{
  val Zero = Time(Duration.Zero)

  val nanosecond  = Time(Duration(1,TimeUnit.NANOSECONDS))
  val microsecond = nanosecond * 1000
  val millisecond = microsecond * 1000
  val second      = millisecond * 1000

  val NANOSECOND  = 1L
  val MICROSECOND = 1000L
  val MILLISECOND = 1000000L
  val SECOND      = 1000000000L
  
  def fromNanos(nanos: Long): Time   = Time(Duration.fromNanos(nanos))
  def fromNanos(nanos: Double): Time = Time(Duration.fromNanos(nanos))

  private val decimalFormat = new DecimalFormat("0.##################")

  def formatTime(time: Time, unit: TimeUnit=TimeUnit.NANOSECONDS, decimals: Int=0): String =
  {
    assume(decimals >= 0)
    val decimal = BigDecimal(time.toUnit(unit)).setScale(decimals, BigDecimal.RoundingMode.HALF_UP).toString()
    if (decimals <= 3) {
      decimal.toString
    } else {
      decimalFormat.format(decimal)
    }
  }

  /**
   * formats time as a string expressed in seconds, with precision to the millisecond.
   *
   * @param time    time to format
   * @return        string expressed in seconds, with precision to the millisecond
   */
  def formatTimeSeconds(time: Time): String =
  {
    val seconds = time.toSeconds
    val millis  = time.toMillis % 1000
    f"$seconds%3d.$millis%03d"
  }
}
