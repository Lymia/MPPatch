/*
 * Copyright (c) 2015-2016 Lymia Alusyia <lymia@lymiahugs.com>
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

import java.io.{DataOutputStream, FileOutputStream}
import java.nio.charset.StandardCharsets

import sbt._
import sbt.Keys._
import Config._
import Utils._

import scala.xml._

object PatchBuild {
  object PatchFile {
    def apply(name: String, data: Array[Byte]) = (name, data)
    def apply(name: String, data: String) = (name, data.getBytes(StandardCharsets.UTF_8))
  }

  object Keys {
    val patchFiles = TaskKey[Map[String, Array[Byte]]]("patch-build-files")
  }
  import Keys._

  val settings = Seq(
    patchFiles := {
      val patchPath = baseDirectory.value / "src" / "patch"
      val copiedFiles = for(directory <- (patchPath / "install") +: (patchPath / "ui").listFiles
                                         if directory.isDirectory;
                            file      <- directory.listFiles if file.isFile)
        yield PatchFile(file.getName, IO.readBytes(file))

      val versions = NativePatchBuild.Keys.nativeVersions.value
      val patchFiles = for(version <- versions)
        yield PatchFile(version.file.getName, IO.readBytes(version.file))

      val xmlWriter = new PrettyPrinter(Int.MaxValue, 4)
      val output = <PatchManifest ManifestVersion="0" PatchVersion={version.value}
                                  Timestamp={System.currentTimeMillis().toString}>
        {XML.loadString(IO.read(patchPath / "manifest.xml")).child}
        {versions.map(x => <Version Platform={x.platform} Version={x.version} Filename={x.file.getName}/>)}
      </PatchManifest>

      val manifestFile = PatchFile("manifest.xml", xmlWriter.format(output))
      val versionFile  = PatchFile("version.properties", IO.readBytes(ResourceGenerators.Keys.versionFile.value))

      // Final generated files list
      (versionFile +: manifestFile +: (patchFiles ++ copiedFiles)).toMap
    },
    resourceGenerators in Compile += Def.task {
      val basePath = (resourceManaged in Compile).value
      val packagePath = basePath / "moe" / "lymia" / "mppatch" / s"patch_${version.value}.mppak"

      import moe.lymia.mppatch.util.common._
      IOWrappers.writePatchPackage(new DataOutputStream(new FileOutputStream(packagePath)),
                                   PatchPackage(patchFiles.value))

      // Final generated files list
      Seq(packagePath)
    }.taskValue
  )
}