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

import java.util.Locale
import scala.sys.process.*

object Utils {
  val VersionRegex = "([0-9]+)\\.([0-9]+)(\\.([0-9]+))?(-(.*))?".r // major.minor.patch-suffix

  def runProcess(p: Seq[Any]) = {
    println("Running process: " + p.map(_.toString).mkString(" "))
    assertProcess(Process(p.map(_.toString)) !)
  }

  def runProcess(p: Seq[Any], cd: File) = {
    println("Running process in " + cd + ": " + p.map(_.toString).mkString(" "))
    assertProcess(Process(p.map(_.toString), cd) !)
  }

  def runProcess(p: Seq[Any], cd: File, env: Map[String, String]) = {
    println("Running process in " + cd + ": " + p.map(_.toString).mkString(" "))
    for ((k, v) <- env) println(s"  - $k = $v")
    assertProcess(Process(p.map(_.toString), cd, env.toSeq*) !)
  }

  // Process helper functions
  def assertProcess(i: Int) = if (i != 0) sys.error("Process returned non-zero return value! (ret: " + i + ")")

  // Directory helpers
  def dir(path: File) = path.toString + "/"

  def allFiles(path: File, extension: String) = path.listFiles.filter(_.getName.endsWith(extension)).toSeq

  def simplePrepareDirectory(path: File) = prepareDirectory(path) { dir => }

  def prepareDirectory(path: File)(fn: File => Unit) = {
    IO.createDirectory(path)
    fn(path)
    path
  }

  def trackDependencySet(
      cacheDirectory: File,
      deps: Set[File],
      inStyle: FileInfo.Style = FileInfo.lastModified,
      outStyle: FileInfo.Style = FileInfo.exists
  )(fn: => Set[File]) = {
    val cache = cached(cacheDirectory, inStyle, outStyle)(_ => fn)
    cache(deps)
  }

  def trackDependencies(
      cacheDirectory: File,
      deps: Set[File],
      inStyle: FileInfo.Style = FileInfo.lastModified,
      outStyle: FileInfo.Style = FileInfo.exists
  )(fn: => File) = {
    val cache = cached(cacheDirectory, inStyle, outStyle)(_ => Set(fn))
    cache(deps).head
  }

  def cachedGeneration(cacheDirectory: File, tempTarget: File, finalTarget: File, data: String) = {
    IO.write(tempTarget, data)
    cachedTransform(cacheDirectory, tempTarget, finalTarget, inStyle = FileInfo.hash)((in, out) => IO.copyFile(in, out))
  }

  def cachedTransform(
      cacheDirectory: File,
      input: File,
      output: File,
      inStyle: FileInfo.Style = FileInfo.lastModified,
      outStyle: FileInfo.Style = FileInfo.exists
  )(fn: (File, File) => Unit) = {
    val cache = cached(cacheDirectory, inStyle, outStyle) { in =>
      fn(in.head, output)
      Set(output)
    }
    cache(Set(input))
    output
  }

  // Code generation helpers
  def cached(
      cacheDirectory: File,
      inStyle: FileInfo.Style = FileInfo.lastModified, // hack to fix ambigous overload
      outStyle: FileInfo.Style = FileInfo.exists
  )(fn: Set[File] => Set[File]) = FileFunction.cached(cacheDirectory, inStyle, outStyle)(fn)
}

object LuaUtils {
  def quote(s: String) = {
    val buffer = new StringBuilder
    for (c <- s) c match {
      case '\n'             => buffer.append("\\n")
      case '\r'             => buffer.append("\\r")
      case '"'              => buffer.append("\\\"")
      case _ if c.isControl => buffer.append("\\%03d".format(c.toInt))
      case _                => buffer.append(c)
    }
    '"' + buffer.toString() + '"'
  }
}

sealed trait PlatformType {
  def shouldBuildNative(other: PlatformType) = (this, other) match {
    case (PlatformType.Win32, PlatformType.Win32)                      => true
    case (PlatformType.Linux, PlatformType.Win32 | PlatformType.Linux) => true
    case _                                                             => false
  }

  def name = this match {
    case PlatformType.Win32 => "win32"
    case PlatformType.MacOS => "macos"
    case PlatformType.Linux => "linux"
  }

  def extension = this match {
    case PlatformType.Win32 => ".dll"
    case PlatformType.MacOS => ".dylib"
    case PlatformType.Linux => ".so"
  }
}
object PlatformType {
  case object Win32 extends PlatformType
  case object MacOS extends PlatformType
  case object Linux extends PlatformType
  case object Other extends PlatformType

  def forString(name: String): PlatformType = name match {
    case "win32" => Win32
    case "macos" => MacOS
    case "linux" => Linux
  }

  lazy val currentPlatform = {
    val os = System.getProperty("os.name", "-").toLowerCase(Locale.ENGLISH)
    if (os.contains("windows")) Win32
    else if (os.contains("linux")) Linux
    else if (
      os.contains("mac") ||
      os.contains("darwin")
    ) MacOS
    else Other
  }
}
