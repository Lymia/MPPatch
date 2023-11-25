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

import java.nio.charset.StandardCharsets
import scala.xml.*

object PatchBuild {
  val settings = Seq(
    Keys.nativesDir := crossTarget.value / "native-bin",
    Keys.buildDylibDir := {
      // create the native-patch directory
      val dir = Keys.nativesDir.value
      IO.delete(dir)
      IO.createDirectory(dir)

      // copy native-patch files to the directory
      val log = streams.value.log
      for (luajitBin <- LuaJITBuild.Keys.luajitFiles.value) {
        log.log(Level.Info, s"Copying $luajitBin to output directory.")
        IO.copyFile(luajitBin.file, dir / luajitBin.file.getName)
      }
      for (nativeBin <- NativePatchBuild.Keys.nativeVersions.value) {
        log.log(Level.Info, s"Copying $nativeBin to output directory.")
        IO.copyFile(nativeBin.file, dir / nativeBin.name)
        IO.write(dir / s"${nativeBin.name}.build-id", nativeBin.buildId)
      }

      // return directory
      dir
    },
    Keys.patchFiles := {
      val log = streams.value.log

      def loadFromDir(dir: File) =
        Path
          .allSubpaths(dir)
          .filter(_._1.isFile)
          .map(x => PatchFile(dir.getName + "/" + x._2, IO.readBytes(x._1)))
          .toSeq

      val patchPath   = baseDirectory.value / "src" / "patch"
      val copiedFiles = loadFromDir(patchPath / "install") ++ loadFromDir(patchPath / "ui")

      val nativeDirFiles = Keys.nativesDir.value.listFiles()
      if (nativeDirFiles == null) sys.error("native-bin does not exist!")
      val patchFiles =
        for (binary <- nativeDirFiles if !binary.getName.endsWith(".build-id")) yield {
          log.info(s"Found native binary file: $binary")
          PatchFile(s"native/${binary.getName}", IO.readBytes(binary))
        }

      val versionDataInfo = InstallerResourceBuild.Keys.versionData.value.toSeq.sorted
        .map(x => s"_mpPatch.version.info[${LuaUtils.quote(x._1)}] = ${LuaUtils.quote(x._2)}")
        .mkString("\n")
      val buildIdInfo = nativeDirFiles
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

      val versionFile = PatchFile("version.properties", IO.readBytes(InstallerResourceBuild.Keys.versionFile.value))

      val xmlWriter = new PrettyPrinter(Int.MaxValue, 4)
      val output = <PatchManifest ManifestVersion="1" PatchVersion={version.value}
                                  Timestamp={System.currentTimeMillis().toString}>
        {XML.loadString(IO.read(patchPath / "manifest.xml")).child}
      </PatchManifest>
      val manifestFile =
        PatchFile("manifest.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + xmlWriter.format(output))

      // Final generated files list
      (versionInfo +: versionFile +: manifestFile +: (patchFiles ++ copiedFiles)).toMap
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
    val nativesDir    = TaskKey[File]("patch-natives-dir")
    val patchFiles    = TaskKey[Map[String, Array[Byte]]]("patch-build-files")
    val buildDylibDir = TaskKey[File]("build-dylib-dir")
  }
}
