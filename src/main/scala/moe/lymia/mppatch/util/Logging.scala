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

import java.io.{OutputStreamWriter, PrintWriter, Writer}
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.{Date, Locale}

trait Logger {
  protected def dateFormat   = DateFormat.getDateInstance(DateFormat.LONG, Locale.US)
  protected def formatString = "[%s] %5s - %s"

  def logRaw(s: String)
  def logFormat(format: String, vals: Any*)
  def logException(t: Throwable)

  def log(format: String, level: String, vals: Any*) =
    logFormat(formatString.format(dateFormat.format(new Date()) +: level +: vals: _*))
  def info (format: String, vals: Any*) = log(format, "INFO" , vals: _*)
  def warn (format: String, vals: Any*) = log(format, "WARN" , vals: _*)
  def error(format: String, vals: Any*) = log(format, "ERROR", vals: _*)

  def info(format: String, t: Throwable, vals: Any*) = {
    info(format, vals: _*)
    logException(t)
  }
  def warn(format: String, t: Throwable, vals: Any*) = {
    warn(format, vals: _*)
    logException(t)
  }
  def error(format: String, t: Throwable, vals: Any*) = {
    error(format, vals: _*)
    logException(t)
  }
}

class SimpleLogger(writers: Writer*) extends Logger {
  private val loggers = new scala.collection.mutable.ArrayBuffer[PrintWriter]()
  def addLogger(w: Writer) = loggers += (w match {
    case p: PrintWriter => p
    case _ => new PrintWriter(w)
  })
  writers.map(addLogger)

  def logRaw(s: String) = loggers.foreach(_.println(s))
  def logFormat(format: String, vals: Any*) = logRaw(format.format(vals: _*))
  def logException(t: Throwable) = loggers.foreach(t.printStackTrace)
}

object Logging extends SimpleLogger(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))