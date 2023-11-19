/*
 * Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
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

import sbt.{FileFunction, FileInfo, IO, *}

import scala.sys.process.*

object Utils {
  val VersionRegex = "([0-9]+)\\.([0-9]+)(\\.([0-9]+))?(-(.*))?".r // major.minor.patch-suffix

  // Process helper functions
  def assertProcess(i: Int) = if(i != 0) sys.error("Process returned non-zero return value! (ret: "+i+")")
  def runProcess   (p: Seq[Any]) = {
    println("Running process: "+p.map(_.toString).mkString(" "))
    assertProcess(Process(p.map(_.toString)) !)
  }
  def runProcess   (p: Seq[Any], cd: File) = {
    println("Running process in "+cd+": "+p.map(_.toString).mkString(" "))
    assertProcess(Process(p.map(_.toString), cd) !)
  }

  // Directory helpers
  def dir     (path: File) = path.toString + "/"
  def allFiles(path: File, extension: String) = path.listFiles.filter(_.getName.endsWith(extension)).toSeq

  def prepareDirectory(path: File)(fn: File => Unit) = {
    IO.createDirectory(path)
    fn(path)
    path
  }
  def simplePrepareDirectory(path: File) = prepareDirectory(path){ dir => }

  // Code generation helpers
  def cached(cacheDirectory: File, inStyle: FileInfo.Style = FileInfo.lastModified, // hack to fix ambigous overload
             outStyle: FileInfo.Style = FileInfo.exists)
            (fn: Set[File] => Set[File]) = FileFunction.cached(cacheDirectory, inStyle, outStyle)(fn)
  def trackDependencySet(cacheDirectory: File, deps: Set[File],
                         inStyle: FileInfo.Style = FileInfo.lastModified,
                         outStyle: FileInfo.Style = FileInfo.exists)(fn: => Set[File]) = {
    val cache = cached(cacheDirectory, inStyle, outStyle) { _ => fn }
    cache(deps)
  }
  def trackDependencies(cacheDirectory: File, deps: Set[File],
                        inStyle: FileInfo.Style = FileInfo.lastModified,
                        outStyle: FileInfo.Style = FileInfo.exists)(fn: => File) = {
    val cache = cached(cacheDirectory, inStyle, outStyle) { _ => Set(fn) }
    cache(deps).head
  }
  def cachedTransform(cacheDirectory: File, input: File, output: File,
                      inStyle: FileInfo.Style = FileInfo.lastModified,
                      outStyle: FileInfo.Style = FileInfo.exists)(fn: (File, File) => Unit) = {
    val cache = cached(cacheDirectory, inStyle, outStyle){ in =>
      fn(in.head, output)
      Set(output)
    }
    cache(Set(input))
    output
  }
  def cachedGeneration(cacheDirectory: File, tempTarget: File, finalTarget: File, data: String) = {
    IO.write(tempTarget, data)
    cachedTransform(cacheDirectory, tempTarget, finalTarget, inStyle = FileInfo.hash)((in, out) =>
      IO.copyFile(in, out))
  }
}