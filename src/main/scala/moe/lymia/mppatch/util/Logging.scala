/*
 * Copyright (c) 2015-2016 Lymia Alusyia <lymia@lymiahugs.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package moe.lymia.mppatch.util

import java.text.DateFormat
import java.util.{Date, Locale}

object Logging {
  private val loggers = new scala.collection.mutable.ArrayBuffer[String => Unit]()
  def addLogger(l: String => Unit) = loggers += l
  addLogger(x => println(x))

  private val dateFormat   = DateFormat.getDateInstance(DateFormat.LONG, Locale.US)
  private val formatString = "[%s] %5s - %s"

  def logRaw(s: String) = loggers.foreach(_(s))
  def logFormat(format: String, vals: Any*) = logRaw(format.format(vals: _*))

  def log(format: String, level: String, vals: Any*) =
    logFormat(formatString.format(dateFormat.format(new Date()) +: level +: vals: _*))
  def info(format: String, vals: Any*) = log(format, "INFO", vals: _*)
  def warn(format: String, vals: Any*) = log(format, "WARN", vals: _*)
  def error(format: String, vals: Any*) = log(format, "ERROR", vals: _*)
}