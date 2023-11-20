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

import java.io.{DataOutputStream, FileOutputStream}
import java.nio.charset.StandardCharsets
import scala.xml.*

import moe.lymia.mppatch.util.common.*

object PatchBuild {
  val settings = Seq(
    Keys.patchFiles := {
      def loadFromDir(dir: File) =
        Path
          .allSubpaths(dir)
          .filter(_._1.isFile)
          .map(x => PatchFile(dir.getName + "/" + x._2, IO.readBytes(x._1)))
          .toSeq

      val patchPath   = baseDirectory.value / "src" / "patch"
      val copiedFiles = loadFromDir(patchPath / "install") ++ loadFromDir(patchPath / "ui")

      val versions = NativePatchBuild.Keys.nativeVersions.value
      val patchFiles =
        for (version <- versions)
          yield PatchFile("native/" + version.file.getName, IO.readBytes(version.file))
      val xmlWriter = new PrettyPrinter(Int.MaxValue, 4)
      val output = <PatchManifest ManifestVersion="0" PatchVersion={version.value}
                                  Timestamp={System.currentTimeMillis().toString}>
        {XML.loadString(IO.read(patchPath / "manifest.xml")).child}{
        versions.map(x =>
          <NativePatch Platform={x.platform.name} Version={x.version} Source={s"native/${x.file.getName}"}/>
        )
      }
      </PatchManifest>

      val luajitFiles =
        for (platform <- LuaJITBuild.Keys.luajitFiles.value)
          yield PatchFile("native/" + platform.file.getName, IO.readBytes(platform.file))

      val buildIdInfo = PatchFile(
        "ui/lib/mppatch_version.lua",
        """-- Generated from PatchBuild.scala
          |_mpPatch.version = {}
          |_mpPatch.version.buildId = {}
        """.stripMargin + versions
          .map(x => s"_mpPatch.version.buildId.${x.platform.name}_${x.version} = ${LuaUtils.quote(x.buildId)}")
          .mkString("\n") + "\n" +
          "_mpPatch.version.info = {}\n" + InstallerResourceBuild.Keys.versionData.value
            .map(x => s"_mpPatch.version.info[${LuaUtils.quote(x._1)}] = ${LuaUtils.quote(x._2)}")
            .mkString("\n")
      )
      val manifestFile =
        PatchFile("manifest.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + xmlWriter.format(output))
      val versionFile = PatchFile("version.properties", IO.readBytes(InstallerResourceBuild.Keys.versionFile.value))

      // Final generated files list
      (buildIdInfo +: versionFile +: manifestFile +: (patchFiles ++ copiedFiles ++ luajitFiles)).toMap
    },
    Compile / resourceGenerators += Def.task {
      val basePath    = (Compile / resourceManaged).value
      val packagePath = basePath / "moe" / "lymia" / "mppatch" / s"mppatch.mppak"

      val debugOut = crossTarget.value / "patch-package-debug"
      streams.value.log.info(s"Writing patch package files to $debugOut")
      if (debugOut.exists) IO.delete(debugOut)
      for ((name, data) <- Keys.patchFiles.value) {
        val target = debugOut / name
        IO.createDirectory(target.getParentFile)
        IO.write(target, data)
      }

      IOWrappers.writePatchPackage(
        new DataOutputStream(new FileOutputStream(packagePath)),
        PatchPackage(Keys.patchFiles.value)
      )

      // Final generated files list
      Seq(packagePath)
    }.taskValue
  )

  object PatchFile {
    def apply(name: String, data: Array[Byte]) = (name, data)

    def apply(name: String, data: String) = (name, data.getBytes(StandardCharsets.UTF_8))
  }

  object Keys {
    val patchFiles = TaskKey[Map[String, Array[Byte]]]("patch-build-files")
  }
}
