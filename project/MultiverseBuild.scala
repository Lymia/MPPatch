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

import com.typesafe.sbt.SbtProguard._
import com.typesafe.sbt.SbtGit._

import java.security.MessageDigest

import language.postfixOps

object MultiverseBuild extends Build with PatchBuild {
  val config_scalaVersion = "2.11.7"

  val config_mingw_gcc    = "i686-w64-mingw32-gcc"
  val config_gcc          = "gcc"
  val config_nasm         = "nasm"

  val version_baseVersion = "0.5.0"
  val version_patchCompat = 1

  // Helper functions for working with external programs
  def assertProcess(i: Int) = if(i != 0) sys.error("Process returned non-zero return value! (ret: "+i+")")
  def runProcess   (p: Seq[Any]) = assertProcess(Process(p.map(_.toString)) !)

  // Additional keys
  val proguardMapping = TaskKey[File]("proguard-mapping")

  lazy val project = Project("multiverse-mod-manager", file(".")) settings (versionWithGit ++ proguardSettings ++ Seq(
    GitKeys.baseVersion in ThisBuild := version_baseVersion,

    organization := "moe.lymia",
    scalaVersion := config_scalaVersion,
    scalacOptions ++= ("-Xlint -Yclosure-elim -target:jvm-1.7 -optimize -deprecation -unchecked "+
                       "-Ydead-code -Yinline -Yinline-handlers").split(" ").toSeq,

    // Dependencies
    resolvers += Resolver.sonatypeRepo("public"),
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0",

    resourceGenerators in Compile += Def.task {
      val basePath = (resourceManaged in Compile).value
      val logger   = streams.value.log

      // Generate version information file
      val properties = new java.util.Properties
      properties.put("mvmm.version.string", version.value)
      val Version = "([0-9]+)\\.([0-9]+)(\\.([0-9]+))?(-(.*))?".r // major.minor.patch-suffix
      val Version(major, minor, _, patch, _, suffix) = version.value
      properties.put("mvmm.version.major" , major)
      properties.put("mvmm.version.minor" , minor)
      properties.put("mvmm.version.patch" , patch)
      properties.put("mvmm.version.suffix", suffix)
      properties.put("mvmm.version.commit", git.gitHeadCommit.value getOrElse "<unknown>")

      properties.put("mvmm.patch.compat"  , version_patchCompat.toString)

      properties.put("mvmm.build.time"    , new java.util.Date().toString)
      properties.put("mvmm.build.path"    , baseDirectory.value.getAbsolutePath)

      properties.put("mvmm.build.treestatus", try {
        IO.withTemporaryFile[String]("git-status", ".txt") { file =>
          assertProcess("git status --porcelain" #> file !)
          IO.read(file)
        }
      } catch {
        case _: Throwable => "<unknown>"
      })

      val versionPropertiesPath = basePath / "moe" / "lymia" / "multiverse" / "data" / "version.properties"
      IO.write(properties, "Multiverse Mod Manager build information", versionPropertiesPath)

      // Compile patches
      val patchFiles = buildPatch(basePath, baseDirectory.value, logger,
                                  tryParse(major, -1), tryParse(minor, -1))

      // Final generated files list
      Seq(versionPropertiesPath) ++ patchFiles
    }.taskValue
  ) ++ inConfig(Proguard)(Seq(
    // Package whole project into a single .jar file with Proguard.
    ProguardKeys.proguardVersion := "5.2.1",
    ProguardKeys.options ++= Seq("-verbose", "-ignorewarnings"),
    ProguardKeys.options ++= Seq("-optimizationpasses", "3", "-allowaccessmodification"),
    ProguardKeys.options ++= Seq( // Obfuscation options
      "-keeppackagenames" ,"moe.lymia.**", "-flattenpackagehierarchy", "moe.lymia.multiverse.contrib",
      "-keepattributes", "SourceFile,LineNumberTable", "-overloadaggressively"),
    ProguardKeys.options += ProguardOptions.keepMain("moe.lymia.multiverse.MultiverseModManager"),

    // Print mapping to file
    proguardMapping := ProguardKeys.proguardDirectory.value / ("multiverse-mod-manager_symbols-"+version.value+".map"),
    ProguardKeys.options ++= Seq("-printmapping", proguardMapping.value.toString),

    // Proguard filter configuration
    ProguardKeys.outputFilter := (_ => Some("!library.properties,!scala-xml.properties,!rootdoc.txt")),
    ProguardKeys.inputs := (dependencyClasspath in Compile).value.files,
    ProguardKeys.filteredInputs ++= ProguardOptions.noFilter((packageBin in Compile).value)
  )))
}