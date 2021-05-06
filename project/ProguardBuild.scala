/*
 * Copyright (c) 2015-2017 Lymia Alusyia <lymia@lymiahugs.com>
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

import sbt._
import sbt.Keys._
import com.lightbend.sbt.SbtProguard._
import sbtassembly.AssemblyPlugin.autoImport.ShadeRule

import sbtassembly._
import AssemblyKeys._
import autoImport._

object ProguardBuild {
  object Keys {
    val shadeMappings     = SettingKey[Seq[(String, String)]]("proguard-wrapper-shade-mappings")
    val excludeFiles      = SettingKey[Set[String]]("proguard-wrapper-exclude-files")
    val proguardConfig    = SettingKey[String]("proguard-wrapper-config")
    val proguardMapping   = TaskKey[File]("proguard-wrapper-mapping")
  }
  import Keys._

  val settings = baseSettings ++ Seq(
    shadeMappings := Seq(),
    excludeFiles := Set(),
    assemblyShadeRules in assembly := shadeMappings.value.map(x => ShadeRule.rename(x).inAll),
    assemblyMergeStrategy in assembly := {
      case PathList(x) if excludeFiles.value.contains(x) =>
        MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    proguard in Proguard := (proguard in Proguard).dependsOn(assembly).value
  ) ++ inConfig(Proguard)(Seq(
    proguardVersion := "5.3.1",
    proguardOptions ++= Seq("-verbose", "-include",
                                 (file(".") / "project" / proguardConfig.value).getCanonicalPath),
    proguardOptions +=
      ProguardOptions.keepMain((mainClass in Compile).value.getOrElse(sys.error("No main class!"))),

    // Print mapping to file
    proguardMapping := proguardDirectory.value / ("symbols-"+version.value+".map"),
    proguardOptions ++= Seq("-printmapping", proguardMapping.value.toString),

    // Proguard filter configuration
    proguardInputs := Seq(),
    proguardFilteredInputs ++= ProguardOptions.noFilter((assemblyOutputPath in assembly).value)
  ))
}
