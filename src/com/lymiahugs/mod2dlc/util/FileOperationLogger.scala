/*
 * Copyright (C) 2014 Lymia Aluysia <lymiahugs@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.lymiahugs.mod2dlc.util

import java.io._
import java.nio.file.Files
import com.lymiahugs.util.StreamUtils

class FileOperationLogger(log_callback: String => Unit) {
  private def assurePresence(f: File) =
    if(!f.exists) {
      f.mkdirs()
      f.delete()
    }

  def log(format: String, args: Any*) =
    log_callback(format.format(args: _*))

  def readFile(file: File) =
    scala.io.Source.fromFile(file).mkString
  def writeFile(file: File, data: String, source: String = "") {
    log("Writing%s to %s", source, file.getCanonicalPath)
    assurePresence(file)
    val s = new PrintWriter(new FileWriter(file))
    s.print(data)
    s.close()
  }
  def writeFileFromStream(file: File, source: String = "")(p: PrintWriter => Any) {
    writeFile(file, StreamUtils.writeToString(p), source)
  }

  def copy(source: File, target: File) = {
    log("Copying %s to %s", source.getCanonicalPath, target.getCanonicalPath)
    assurePresence(target)
    Files.copy(source.toPath, target.toPath)
  }

  def logException[T](body: => T, fini: => Unit = Unit) =
    try {
      body
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        log("Error encountered: "+t.getClass.getCanonicalName+": "+t.getMessage)
        throw t
    } finally {
      fini
    }
}
object FileOperationLogger extends FileOperationLogger(println)