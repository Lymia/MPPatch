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
name := "mppatch"
homepage := Some(url("https://github.com/Lymia/MPPatch"))
licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

// Plugins for the project.
InstallerResourceBuild.settings
NativeImagePlugin.projectSettings

// Git versioning
versionWithGit
git.baseVersion := "0.1.3"
git.uncommittedSignifier := Some("DIRTY")
git.formattedShaVersion := {
  val base   = git.baseVersion.?.value
  val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)
  git.gitHeadCommit.value map { rawSha =>
    val sha = "dev_" + rawSha.substring(0, 8)
    git.defaultFormatShaVersion(base, sha, suffix)
  }
}

// Scala configuration
scalaVersion := "2.13.12"
scalacOptions ++= "-Xlint -target:jvm-1.8 -opt:l:method,inline -deprecation -unchecked".split(" ").toSeq
crossPaths := false

// Native Image configuration
nativeImageInstalled := true
nativeImageGraalHome := {
  val platform   = PlatformType.currentPlatform
  val graalvmDir = target.value / s"graalvm_${platform.name}"
  val (dl_url, dl_dir) = platform match {
    case PlatformType.Linux => (Config.config_linux_graalvm_url, Config.config_linux_graalvm_dir)
    case _                  => sys.error("platform not supported for graalvm")
  }

  val log = streams.value.log;
  if (!(graalvmDir / dl_dir / "jmods" / "java.base.jmod").exists) {
    // recreate directory
    log.info("Preparing directories for GraalVM installation...")
    IO.delete(graalvmDir)
    IO.createDirectory(graalvmDir)

    // download graalvm
    log.info("Downloading GraalVM...")
    url(dl_url) #> (graalvmDir / "graalvm.tar.gz") !!;
    Utils.runProcess(Seq("tar", "-xv", "-f", "graalvm.tar.gz"), graalvmDir)
  }

  (graalvmDir / dl_dir).toPath
}

nativeImageOptions += "--no-fallback"
nativeImageOptions += "-H:+UnlockExperimentalVMOptions"
nativeImageOptions += "-Djava.awt.headless=false"

nativeImageOptions += s"-H:ReflectionConfigurationFiles=${target.value / "native-image-configs" / "reflect-config.json"}"
nativeImageOptions += s"-H:ConfigurationFileDirectories=${target.value / "native-image-configs"}"

// Dependencies
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
libraryDependencies += "com.formdev"             % "flatlaf"   % "3.2.5"

// custom tasks
val buildNativeTask = TaskKey[Unit]("buildNativeTask")

// New dependencies for tasks
(Compile / run) := (Compile / run).dependsOn(PatchBuild.Keys.buildDylibDir).evaluated

// Build distribution file
InputKey[Unit]("dist") := {
  val path = crossTarget.value / "dist"
  def copy(source: File) = {
    val output = path / source.getName.replace("-assembly", "")
    IO.copyFile(source, output)
    output
  }
  streams.value.log.info(s"Output packed to: ${copy((Compile / assembly).value)}")
}

// Build native binaries
InputKey[Unit]("buildNative") := PatchBuild.Keys.buildDylibDir.value
