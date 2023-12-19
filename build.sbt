/*
 * Copyright (C) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
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

import sbt.*
import sbt.Keys.*
import scala.sys.process.*

// Package metainfo
organization := "moe.lymia"
name         := "mppatch-installer"
homepage     := Some(url("https://github.com/Lymia/MPPatch"))
licenses     := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

// Plugins for the project.
InstallerResourceBuild.settings
NativeImagePlugin.projectSettings

// Git versioning
versionWithGit
git.baseVersion          := "0.2.0"
git.uncommittedSignifier := Some("DIRTY")
git.formattedShaVersion := {
  val base   = git.baseVersion.?.value
  val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)
  git.gitHeadCommit.value map { rawSha =>
    val sha = "r" + rawSha.substring(0, 7)
    git.defaultFormatShaVersion(base, sha, suffix)
  }
}

// Scala configuration
scalaVersion := "3.3.1"
scalacOptions ++= "-release:21 -deprecation -unchecked".split(" ").toSeq
crossPaths := false

// Dependencies
libraryDependencies += "org.scala-lang.modules" %% "scala-xml"            % "2.1.0"
libraryDependencies += "org.playframework"      %% "play-json"            % "3.0.1"
libraryDependencies += "com.intellij"            % "forms_rt"             % "7.0.3"
libraryDependencies += "com.formdev"             % "flatlaf"              % "3.2.5"
libraryDependencies += "com.formdev"             % "flatlaf-fonts-roboto" % "2.137"

// Build assembled jar
ThisBuild / assemblyMergeStrategy := {
  case x if x.startsWith("moe/lymia")       => MergeStrategy.first
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case x if x.startsWith("com/intellij")    => MergeStrategy.first // for debug builds
  case x if x.endsWith(".tasty")            => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

// Build native binaries
nativeImageInstalled := true
nativeImageOutput := {
  IO.createDirectory(target.value / s"native-image-${PlatformType.currentPlatform.name}")
  target.value / s"native-image-${PlatformType.currentPlatform.name}" / name.value
}
nativeImageGraalHome := (target.value / "deps" / f"graalvm-${PlatformType.currentPlatform.name}").toPath

nativeImageOptions += "--no-fallback"
nativeImageOptions += "-Djava.awt.headless=false"
nativeImageOptions += "--strict-image-heap"
nativeImageOptions += s"-H:ConfigurationFileDirectories=${baseDirectory.value / "scripts" / "native-image-config" / PlatformType.currentPlatform.name}"

Global / excludeLintKeys += nativeImageJvm
Global / excludeLintKeys += nativeImageJvmIndex
Global / excludeLintKeys += nativeImageVersion
Global / excludeLintKeys += NativeImage / name

InputKey[Unit]("buildNative") := PatchBuild.Keys.buildDylibDir.value

// Runtime configuration
run / fork    := true
run / envVars := Map("MPPATCH_SBT_LAUNCH" -> "018c7e5e-473e-7273-b818-e677c7fe1439")
