/*
 * Copyright (c) 2015-2017 Lymia Alusyia <lymia@lymiahugs.com>
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
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

object Logger {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
}

trait Logger {
  protected def dateFormat = Logger.dateFormat
  protected def formatString = "[%s/%5s] %s"

  def logRaw(s: String)
  def logException(t: Throwable)
  def flush()

  def log(format: String, level: String, vals: Any*) =
    logRaw(formatString.format(dateFormat.format(new Date()), level, format.format(vals: _*)))

  def info (format: String, vals: Any*): Unit = log(format, "INFO" , vals: _*)
  def warn (format: String, vals: Any*): Unit = log(format, "WARN" , vals: _*)
  def error(format: String, vals: Any*): Unit = log(format, "ERROR", vals: _*)

  def info (str: String): Unit = info ("%s", str)
  def warn (str: String): Unit = warn ("%s", str)
  def error(str: String): Unit = error("%s", str)

  def info(format: String, t: Throwable, vals: Any*): Unit = {
    info(format, vals: _*)
    logException(t)
  }
  def warn(format: String, t: Throwable, vals: Any*): Unit = {
    warn(format, vals: _*)
    logException(t)
  }
  def error(format: String, t: Throwable, vals: Any*): Unit = {
    error(format, vals: _*)
    logException(t)
  }

  def info (str: String, t: Throwable): Unit = info ("%s", t, str)
  def warn (str: String, t: Throwable): Unit = warn ("%s", t, str)
  def error(str: String, t: Throwable): Unit = error("%s", t, str)
}

class SimpleLogger(writers: Writer*) extends Logger {
  private val loggers = new scala.collection.mutable.ArrayBuffer[PrintWriter]()
  def addLogger(w: Writer) = loggers.append(w match {
    case p: PrintWriter => p
    case _ => new PrintWriter(w)
  })
  writers.foreach(addLogger)

  def flush() = loggers.foreach(_.flush())
  def logRaw(s: String) = {
    loggers.foreach(_.println(s))
    flush()
  }
  def logException(t: Throwable) = {
    loggers.foreach(t.printStackTrace)
    flush()
  }
}
object SimpleLogger extends SimpleLogger(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))