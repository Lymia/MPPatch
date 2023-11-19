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

import Config.*
import Utils.*
import com.github.rjeschke.txtmark.Processor
import com.typesafe.sbt.SbtGit.*
import sbt.*
import sbt.Keys.*

import java.net.InetAddress
import java.text.DateFormat
import java.util.{Locale, UUID}
import scala.collection.mutable.ArrayBuffer
import scala.sys.process.*

object InstallerResourceBuild {
  val settings = PatchBuild.settings ++ NativePatchBuild.settings ++ LuaJITBuild.settings ++ Seq(
    Keys.versionData := {
      val VersionRegex(major, minor, _, patch, _, suffix) = version.value
      val dateFormat                                      = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)
      Map(
        "mppatch.version.string" -> version.value,
        "mppatch.version.major"  -> major,
        "mppatch.version.minor"  -> minor,
        "mppatch.version.patch"  -> patch,
        "mppatch.version.suffix" -> Option(suffix).getOrElse(""),
        "mppatch.version.commit" -> git.gitHeadCommit.value.getOrElse("<unknown>"),
        "mppatch.version.clean"  -> (!git.gitUncommittedChanges.value).toString,
        "mppatch.website"        -> homepage.value.fold("<unknown>")(_.toString),
        "build.id"               -> UUID.randomUUID().toString,
        "build.os" -> tryProperty {
          System.getProperty("os.name")
        },
        "build.user" -> tryProperty {
          System.getProperty("user.name") + "@" +
            InetAddress.getLocalHost.getHostName
        },
        "build.time"          -> new java.util.Date().getTime.toString,
        "build.timestr"       -> dateFormat.format(new java.util.Date()),
        "build.path"          -> baseDirectory.value.getAbsolutePath,
        "build.treestatus"    -> propertyFromProcess("git", "status", "--porcelain"),
        "build.version.uname" -> propertyFromProcess("uname", "-a"),
        "build.version.distro" -> tryProperty {
          IO.read(file("/etc/os-release"))
        },
        "build.version.sbt"      -> sbtVersion.value,
        "build.version.nasm"     -> propertyFromProcess(config_nasm, "-v"),
        "build.version.cc.win32" -> "n/a", //propertyFromProcess(config_win32_cc, "-v"),
        "build.version.cc.macos" -> "n/a", //propertyFromProcess(config_macos_cc, "-v"),
        "build.version.cc.linux" -> propertyFromProcess(config_linux_cc, "-v")
      )
    },
    Keys.versionFile := {
      val path = crossTarget.value / "version-resource-cache.properties"

      val properties = new java.util.Properties
      for ((k, v) <- Keys.versionData.value) properties.put(k, v)
      IO.write(properties, "MPPatch build information", path)

      path
    },
    Compile / resourceGenerators += Def.task {
      val versionPropertiesPath =
        (Compile / resourceManaged).value / "moe" / "lymia" / "mppatch" / "version.properties"
      IO.copyFile(Keys.versionFile.value, versionPropertiesPath)
      Seq(versionPropertiesPath)
    }.taskValue,
    // Render about information
    resourceGenerators in Compile += Def.task {
      val outPath = (Compile / resourceManaged).value / "moe" / "lymia" / "mppatch" / "text"

      (for (file <- IO.listFiles(baseDirectory.value / "project" / "about") if file.getName.endsWith(".md")) yield {
        val markdown = IO.read(file)
        val html =
          s"""<html>
             |  <body>
             |    ${Processor.process(markdown)}
             |  </body>
             |</html>
                    """.stripMargin

        val outFile = outPath / file.getName.replaceAll("\\.md$", ".html")
        IO.write(outFile, html)
        outFile
      }).toSeq
    }.taskValue
  )

  private def propertyFromProcess(proc: String*) = tryProperty {
    val output = new ArrayBuffer[String]()
    val logger = new ProcessLogger {
      override def buffer[T](f: => T): T = f

      override def err(s: => String): Unit = output += s

      override def out(s: => String): Unit = output += s
    }
    assertProcess(proc ! logger)
    output.mkString("\n")
  }

  private def tryProperty(s: => String) = try {
    val str = s
    if (str == null) "<null>" else str
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      s"<unknown>"
  }

  object Keys {
    val versionData = TaskKey[Map[String, String]]("resource-version-data")
    val versionFile = TaskKey[File]("resource-version-file")
  }
}
