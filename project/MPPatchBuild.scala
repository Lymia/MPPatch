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

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.SbtProguard._

import sbtassembly._
import AssemblyKeys._

import Config._
import ProguardBuild.Keys._

object MPPatchBuild extends Build {
  // Additional keys
  val buildDist       = TaskKey[File]("build-dist")
  val dist            = InputKey[Unit]("dist")

  val commonSettings = versionWithGit ++ Seq(
    // Organization configuration
    organization := "moe.lymia",
    homepage := Some(url("https://github.com/Lymia/MPPatch")),
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
    GitKeys.baseVersion in ThisBuild := version_baseVersion,

    // Scala configuration
    scalaVersion := config_scalaVersion,
    scalacOptions ++= ("-Xlint -Yclosure-elim -target:jvm-1.8 -optimize -deprecation -unchecked "+
                       "-Ydead-code -Yinline -Yinline-handlers").split(" ").toSeq,
    crossPaths := false
  )

  lazy val mppatch = project in file(".") settings (commonSettings ++ ProguardBuild.settings ++ Seq(
    name := "MPPatch",

    // Dependencies
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
    libraryDependencies += "org.tukaani" % "xz" % "1.5",
    libraryDependencies += "org.whispersystems" % "curve25519-java" % "0.3.0",

    // Shade rules
    shadeMappings += "scala.**" -> "moe.lymia.mppatch.externlibs.scala.@1",
    shadeMappings += "org.tukaani.xz.**" -> "moe.lymia.mppatch.externlibs.xz.@1",
    excludeFiles  := Set("library.properties", "rootdoc.txt", "scala-xml.properties"),
    proguardMainClass := "moe.lymia.mppatch.MPPatchInstaller",

    // Build distribution file
    buildDist := {
      val path   = crossTarget.value / "dist"
      val source = (ProguardKeys.proguard in Proguard).value.head
      val target = path / source.getName

      IO.createDirectory(path)
      IO.withTemporaryDirectory { dir =>
        IO.unzip(source, dir)
        val f = Path.allSubpaths(dir) ++ Seq(((proguardMapping in Proguard).value, "moe/lymia/mppatch/symbols.map"))
        IO.zip(f, target)
      }
      target
    },
    dist := streams.value.log.info("Final binary output to: "+buildDist.value)
  ) ++ PatchBuild.settings ++ ResourceGenerators.settings ++ NativePatchBuild.settings)
}
