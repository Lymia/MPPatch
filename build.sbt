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
import com.typesafe.sbt.SbtGit.GitKeys
import com.typesafe.sbt.SbtProguard._

import Config._

import ProguardBuild.Keys._
import LoaderBuild.Keys._

// Additional keys

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
  name := "mppatch-nopack",

  libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
  shadeMappings       += "scala.**" -> "moe.lymia.mppatch.lib.scala.@1",

  libraryDependencies += "org.tukaani" % "xz" % "1.5",
  shadeMappings       += "org.tukaani.xz.**" -> "moe.lymia.mppatch.lib.xz.@1",

  libraryDependencies += "com.github.rjeschke" % "txtmark" % "0.13",
  shadeMappings       += "com.github.rjeschke.txtmark.**" -> "moe.lymia.mppatch.lib.txtmark.@1",

  excludeFiles   := Set("library.properties", "rootdoc.txt", "scala-xml.properties"),
  proguardConfig := "installer.pro"
) ++ PatchBuild.settings ++ ResourceBuild.settings ++ NativePatchBuild.settings)

lazy val loader = project in file("loader") settings (commonSettings ++ LoaderBuild.settings ++ Seq(
  name := "mppatch",
  autoScalaLibrary := false,

  loaderSourceJar := (ProguardKeys.proguard in Proguard in mppatch).value.head,
  loaderTargetPath := "moe/lymia/mppatch/installer.pack"
))

// Build distribution file
InputKey[Unit]("dist") := {
  val path   = crossTarget.value / "dist"
  def copy(source: File) = {
    val output = path / source.getName
    IO.copyFile(source, output)
    output
  }
  streams.value.log.info(s"Output packed to: ${copy((Keys.`package` in Compile in loader).value)}")
}