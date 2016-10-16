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

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.SbtProguard._
import Config._

import sbtassembly._
import AssemblyKeys._

object MPPatchBuild extends Build with PatchBuild with ResourceGenerators {
  // Additional keys
  val proguardMapping = TaskKey[File]("proguard-mapping")
  val buildDist       = TaskKey[File]("build-dist")
  val dist            = InputKey[Unit]("dist")

  lazy val project = Project("mppatch", file(".")) settings (versionWithGit ++ proguardSettings ++ Seq(
    GitKeys.baseVersion in ThisBuild := version_baseVersion,

    organization := "moe.lymia",
    scalaVersion := config_scalaVersion,
    scalacOptions ++= ("-Xlint -Yclosure-elim -target:jvm-1.8 -optimize -deprecation -unchecked "+
                       "-Ydead-code -Yinline -Yinline-handlers").split(" ").toSeq,
    crossPaths := false,

    homepage := Some(url("https://github.com/Lymia/MPPatch")),
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),

    // Dependencies
    resolvers += Resolver.sonatypeRepo("public"),
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.4.0",

    // Build distribution file
    assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("scala.**" -> "moe.lymia.mppatch.externlibs.scala.@1").inAll,
      ShadeRule.rename("scopt.**" -> "moe.lymia.mppatch.externlibs.scopt.@1").inAll
    ),
    assemblyMergeStrategy in assembly := {
      case PathList(x) if Set("library.properties", "rootdoc.txt", "scala-xml.properties").contains(x) =>
        MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
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
    ProguardKeys.proguard in Proguard <<= (ProguardKeys.proguard in Proguard).dependsOn(assembly),
    dist := streams.value.log.info("Final binary output to: "+buildDist.value)
  ) ++ patchBuildSettings ++ resourceGeneratorSettings ++ inConfig(Proguard)(Seq(
    // Package whole project into a single .jar file with Proguard.
    ProguardKeys.proguardVersion := "5.3",
    ProguardKeys.options ++= Seq("-verbose", "@"+(baseDirectory.value / "project" / "proguard.pro").getCanonicalPath),

    // Print mapping to file
    proguardMapping := ProguardKeys.proguardDirectory.value / ("mppatch_symbols-"+version.value+".map"),
    ProguardKeys.options ++= Seq("-printmapping", proguardMapping.value.toString),

    // Proguard filter configuration
    ProguardKeys.inputs := Seq(),
    ProguardKeys.filteredInputs ++= ProguardOptions.noFilter((assemblyOutputPath in assembly).value)
  )))
}
