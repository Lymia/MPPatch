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

import sbt.*
import sbt.Keys.*
import sbt.util.Logger

import java.nio.charset.StandardCharsets
import scala.xml.*

object PatchBuild {
  // Helper function to build the patch files map from native files
  private def buildPatchFilesMap(
    log: Logger,
    nativeFiles: Array[File],
    baseDir: File,
    versionData: Map[String, String],
    versionFile: File
  ): Map[String, Array[Byte]] = {
    def loadFromDir(dir: File) =
      Path.allSubpaths(dir).filter(_._1.isFile).map(x => PatchFile(x._2, IO.readBytes(x._1))).toSeq
    val copiedFiles = loadFromDir(baseDir / "src" / "patch" / "static")

    val patchFiles =
      for (binary <- nativeFiles if !binary.getName.endsWith(".build-id")) yield {
        log.info(s"[MPPatch] Including native binary: ${binary.getName}")
        PatchFile(s"native/${binary.getName}", IO.readBytes(binary))
      }

    val versionDataInfo = versionData.toSeq.sorted
      .map(x => s"_mpPatch.version.info[${LuaUtils.quote(x._1)}] = ${LuaUtils.quote(x._2)}")
      .mkString("\n")
    val buildIdInfo = nativeFiles
      .filter(x => x.getName.endsWith(".build-id"))
      .sorted
      .map { x =>
        val platform = x.getName match {
          case "mppatch_core.dll.build-id"   => "win32"
          case "mppatch_core.dylib.build-id" => "macos"
          case "mppatch_core.so.build-id"    => "linux"
        }
        s"_mpPatch.version.buildId[${LuaUtils.quote(platform)}] = ${LuaUtils.quote(IO.read(x))}"
      }
      .mkString("\n")
    val versionInfo = PatchFile(
      "ui/lib/mppatch_version.lua",
      s"""-- Generated from PatchBuild.scala
         |_mpPatch.version = {}
         |
         |_mpPatch.version.buildId = {}
         |$buildIdInfo
         |
         |_mpPatch.version.info = {}
         |$versionDataInfo
         |
         |_mpPatch.version.loaded = true
      """.stripMargin.trim
    )

    val versionFileEntry = PatchFile("version.properties", IO.readBytes(versionFile))

    // Final generated files list
    (versionInfo +: versionFileEntry +: (patchFiles ++ copiedFiles)).toMap
  }

  val settings = Seq(
    Keys.nativesDir := crossTarget.value / "native-bin",

    // buildDylibDir: Only used when building from source (not in CI with pre-built natives)
    Keys.buildDylibDir := {
      val dir = Keys.nativesDir.value
      val log = streams.value.log
      IO.delete(dir)
      IO.createDirectory(dir)
      log.log(Level.Info, "[MPPatch] Building natives from source...")
      for (luajitBin <- LuaJITBuild.Keys.luajitFiles.value) {
        log.log(Level.Info, s"[MPPatch] Copying $luajitBin to output directory.")
        IO.copyFile(luajitBin.file, dir / luajitBin.file.getName)
      }
      for (nativeBin <- NativePatchBuild.Keys.nativeVersions.value) {
        log.log(Level.Info, s"[MPPatch] Copying $nativeBin to output directory.")
        IO.copyFile(nativeBin.file, dir / nativeBin.name)
        IO.write(dir / s"${nativeBin.name}.build-id", nativeBin.buildId)
      }
      for (wrapperBin <- NativePatchBuild.Keys.win32Wrapper.value) {
        log.log(Level.Info, s"[MPPatch] Copying $wrapperBin to output directory.")
        IO.copyFile(wrapperBin, dir / wrapperBin.getName)
      }
      dir
    },

    // patchFiles: Use Def.taskDyn to conditionally choose between pre-built and from-source
    // This is critical - sbt only resolves dependencies for the RETURNED task, not all code paths
    Keys.patchFiles := Def.taskDyn {
      val log = streams.value.log
      val prebuiltDir = target.value / "native-bin"

      // Check at task-selection time (before returning the task)
      if (prebuiltDir.exists() && prebuiltDir.listFiles() != null && prebuiltDir.listFiles().nonEmpty) {
        log.log(Level.Info, s"[MPPatch] Found pre-built natives in $prebuiltDir")
        // Return a task that uses pre-built natives - NO dependency on buildDylibDir
        Def.task {
          val nativeFiles = prebuiltDir.listFiles()
          log.log(Level.Info, s"[MPPatch] Using ${nativeFiles.length} pre-built native files")
          buildPatchFilesMap(
            log,
            nativeFiles,
            baseDirectory.value,
            InstallerResourceBuild.Keys.versionData.value,
            InstallerResourceBuild.Keys.versionFile.value
          )
        }
      } else {
        log.log(Level.Info, "[MPPatch] No pre-built natives found, will build from source")
        // Return a task that builds from source - HAS dependency on buildDylibDir
        Def.task {
          val nativesDir = Keys.buildDylibDir.value
          val nativeFiles = nativesDir.listFiles()
          if (nativeFiles == null || nativeFiles.isEmpty) {
            sys.error(s"[MPPatch] native-bin directory is empty after build!")
          }
          log.log(Level.Info, s"[MPPatch] Built ${nativeFiles.length} native files")
          buildPatchFilesMap(
            log,
            nativeFiles,
            baseDirectory.value,
            InstallerResourceBuild.Keys.versionData.value,
            InstallerResourceBuild.Keys.versionFile.value
          )
        }
      }
    }.value,

    Compile / resourceGenerators += Def.task {
      val basePath    = (Compile / resourceManaged).value
      val packagePath = basePath / "moe" / "lymia" / "mppatch" / s"builtin_patch"

      streams.value.log.info(s"[MPPatch] Writing patch package files to $packagePath")
      if (packagePath.exists) IO.delete(packagePath)

      for ((name, data) <- Keys.patchFiles.value.toSeq) yield {
        val target = packagePath / name
        IO.createDirectory(target.getParentFile)
        IO.write(target, data)
        target
      }
    }.taskValue
  )

  object PatchFile {
    def apply(name: String, data: Array[Byte]) = (name, data)
    def apply(name: String, data: String) = {
      val fullData = s"${data.trim}\n"
      (name, fullData.getBytes(StandardCharsets.UTF_8))
    }
  }

  object Keys {
    val nativesDir        = TaskKey[File]("patch-natives-dir")
    val patchFiles        = TaskKey[Map[String, Array[Byte]]]("patch-build-files")
    val buildDylibDir     = TaskKey[File]("build-dylib-dir")
    val buildPatchPackage = TaskKey[Unit]("mppatch-build-patch-package")
  }
}
