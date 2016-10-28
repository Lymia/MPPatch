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

package moe.lymia.mppatch.build

import java.net.InetAddress

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtGit._
import Config._
import Utils._

import language.postfixOps
import scala.collection.mutable.ArrayBuffer

trait ResourceGenerators { this: Build =>
  private def tryProperty(s: => String) = try {
    val str = s
    if(str == null) "<null>" else str
  } catch {
    case t: Throwable => s"<unknown (${t.getClass.getName}: ${t.getMessage})>"
  }
  private def propertyFromProcess(proc: String*) = tryProperty {
    val output = new ArrayBuffer[String]()
    val logger = new ProcessLogger {
      override def buffer[T](f: => T): T = f
      override def error(s: => String): Unit = output += s
      override def info(s: => String): Unit = output += s
    }
    assertProcess(proc ! logger)
    output.mkString("\n")
  }
  val resourceGeneratorSettings = Seq(
    resourceGenerators in Compile += Def.task {
      val basePath = (resourceManaged in Compile).value

      // Generate version information file
      val properties = new java.util.Properties
      properties.put("mppatch.version.string", version.value)
      val VersionRegex(major, minor, _, patch, _, suffix) = version.value
      properties.put("mppatch.version.major" , major)
      properties.put("mppatch.version.minor" , minor)
      properties.put("mppatch.version.patch" , patch)
      properties.put("mppatch.version.suffix", suffix)
      properties.put("mppatch.version.commit", git.gitHeadCommit.value getOrElse "<unknown>")

      properties.put("mppatch.patch.compat"  , version_patchCompat.toString)

      properties.put("mppatch.build.sbt"       , sbtVersion.value)
      properties.put("mppatch.build.nasm"      , propertyFromProcess(config_nasm, "-v"))
      properties.put("mppatch.build.gcc"       , propertyFromProcess(config_linux_gcc, "-v"))
      properties.put("mppatch.build.mingw.gcc" , propertyFromProcess(config_mingw_gcc, "-v"))
      properties.put("mppatch.build.user"      , tryProperty { System.getProperty("user.name") })
      properties.put("mppatch.build.system"    , tryProperty { InetAddress.getLocalHost.toString })
      properties.put("mppatch.build.time"      , new java.util.Date().toString)
      properties.put("mppatch.build.path"      , baseDirectory.value.getAbsolutePath)
      properties.put("mppatch.build.treestatus", propertyFromProcess("git", "status", "--porcelain"))

      val versionPropertiesPath = basePath / "moe" / "lymia" / "mppatch" / "version.properties"
      IO.write(properties, "MPPatch build information", versionPropertiesPath)

      // Final generated files list
      Seq(versionPropertiesPath)
    }.taskValue
  )
}
