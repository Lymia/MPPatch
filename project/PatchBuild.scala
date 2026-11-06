/*
 * Copyright (c) 2015-2026 Rimi Kanokawa <rimi@rimin.moe>
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

import java.nio.charset.StandardCharsets
import scala.xml.*

object PatchBuild {
  val settings = Seq(
    Keys.nativesDir := crossTarget.value / "native-bin",
    Keys.patchFiles := {
      val log = streams.value.log

      def loadFromDir(dir: File) =
        Path.allSubpaths(dir).filter(_._1.isFile).map(x => PatchFile(x._2, IO.readBytes(x._1))).toSeq
      val copiedFiles = loadFromDir(baseDirectory.value / "src" / "patch")

      val nativePlatforms = Keys.nativesDir.value.listFiles()
      if (nativePlatforms == null) sys.error("native-bin does not exist!")
      val patchFiles =
        for (platform <- nativePlatforms; binary <- platform.listFiles()) yield {
          log.info(s"Found native binary file: $binary")
          PatchFile(s"${platform.getName}/${binary.getName}", IO.readBytes(binary))
        }

      val versionDataInfo = InstallerResourceBuild.Keys.versionData.value.toSeq.sorted
        .map(x => s"_mpPatch.version.info[${LuaUtils.quote(x._1)}] = ${LuaUtils.quote(x._2)}")
        .mkString("\n")
      val versionInfo = PatchFile(
        "ui/lib/mppatch_version.lua",
        s"""-- Generated from PatchBuild.scala
           |_mpPatch.version = {}
           |_mpPatch.version.info = {}
           |$versionDataInfo
           |_mpPatch.version.loaded = true
        """.stripMargin.trim
      )

      val versionFile = PatchFile("version.properties", IO.readBytes(InstallerResourceBuild.Keys.versionFile.value))

      // Final generated files list
      (versionInfo +: versionFile +: (patchFiles ++ copiedFiles)).toMap
    },
    Compile / resourceGenerators += Def.task {
      val basePath    = (Compile / resourceManaged).value
      val packagePath = basePath / "moe" / "lymia" / "mppatch" / s"builtin_patch"

      streams.value.log.info(s"Writing patch package files to $packagePath")
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
