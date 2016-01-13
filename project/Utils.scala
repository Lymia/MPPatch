/*
 * Copyright (C) 2015-2016 Lymia Aluysia <lymiahugs@gmail.com>
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

package moe.lymia.multiverse.build

import sbt._

import java.security.MessageDigest

import language.postfixOps

object Utils {
  // Various helper functions
  val VersionRegex = "([0-9]+)\\.([0-9]+)(\\.([0-9]+))?(-(.*))?".r // major.minor.patch-suffix
  def assertProcess(i: Int) = if(i != 0) sys.error("Process returned non-zero return value! (ret: "+i+")")
  def runProcess   (p: Seq[Any]) = assertProcess(Process(p.map(_.toString)) !)
  def runProcess   (p: Seq[Any], cd: File) = assertProcess(Process(p.map(_.toString), cd) !)
  def tryParse(s: String, default: Int) = try { s.toInt } catch { case _: Exception => default }

  def dir     (path: File) = path.toString + "/"
  def allFiles(path: File, extension: String) = path.listFiles.filter(_.getName.endsWith(extension)).toSeq

  def prepareDirectory(path: File)(fn: File => Unit) = {
    IO.createDirectory(path)
    fn(path)
    path
  }
  def simplePrepareDirectory(path: File) = prepareDirectory(path){ dir => }

  // Steam runtime helper functions
  def downloadSteamRuntime[T](url: String, target: File, beforeLog: => T = null)(fn: (File, File) => Unit) =
    if(target.exists) target else {
      beforeLog
      IO.withTemporaryDirectory { temp =>
        IO.download(new URL(url), temp / "steam_runtime_package.deb")
        runProcess(Seq("ar", "xv", temp / "steam_runtime_package.deb"), temp)
        runProcess(Seq("tar", "xvf", temp / "data.tar.gz"), temp)
        fn(temp, target)
      }
      target
    }

  // Code generation functions
  def cached(cacheDirectory: File, inStyle: FilesInfo.Style = FilesInfo.lastModified, // hack to fix ambigous overload
             outStyle: FilesInfo.Style = FilesInfo.exists)
            (fn: Set[File] => Set[File]) = FileFunction.cached(cacheDirectory, inStyle, outStyle)(fn)
  def trackDependencies(cacheDirectory: File, deps: Set[File],
                        inStyle: FilesInfo.Style = FilesInfo.lastModified,
                        outStyle: FilesInfo.Style = FilesInfo.exists)(fn: => File) = {
    val cache = cached(cacheDirectory, inStyle, outStyle) { _ =>
      Set(fn)
    }
    cache(deps).head
  }
  def cachedTransform(cacheDirectory: File, input: File, output: File,
                      inStyle: FilesInfo.Style = FilesInfo.lastModified,
                      outStyle: FilesInfo.Style = FilesInfo.exists)(fn: (File, File) => Unit) = {
    val cache = cached(cacheDirectory, inStyle, outStyle){ in =>
      fn(in.head, output)
      Set(output)
    }
    cache(Set(input))
    output
  }
  def cachedGeneration(cacheDirectory: File, tempTarget: File, finalTarget: File, data: String) = {
    IO.write(tempTarget, data)
    cachedTransform(cacheDirectory, tempTarget, finalTarget, inStyle = FilesInfo.hash)((in, out) =>
      IO.copyFile(in, out))
  }

  // Crypto helper functions
  def digest(algorithm: String, data: Seq[Byte]) = {
    val md = MessageDigest.getInstance(algorithm)
    val hash = md.digest(data.toArray)
    hash
  }
  def hexdigest(algorithm: String, data: Seq[Byte]) =
    digest(algorithm, data).map(x => "%02x".format(x)).reduce(_ + _)
  def sha1_hex(data: Seq[Byte]) = hexdigest("SHA1", data)
}