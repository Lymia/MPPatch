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

import java.security.MessageDigest
import sbt.*
import sbt.Keys.*
import Config.*
import Utils.*

object Launch4JBuild {
  // Launch4J File
  def extractLaunch4J[T](source: File, target: File) =
    if(target.exists) target else {
      target.mkdirs()
      runProcess(Seq("tar", "xvf", source), target)
      target
    }

  object Keys {
    val launch4jDir          = TaskKey[File]("launch4j-dir")
    val launch4jDownloadPath = TaskKey[File]("launch4j-download-path")
    val launch4jDownloadTgz  = TaskKey[File]("launch4j-download-tgz")
    val launch4jBinary       = TaskKey[File]("launch4j-binary")

    val launch4jFinalConfig  = TaskKey[(File, File)]("launch4j-final-config")
    val launch4jConfig       = TaskKey[File]("launch4j-config")
    val launch4jManifest     = TaskKey[File]("launch4j-manifest")
    val launch4jSourceJar    = TaskKey[File]("launch4j-source-jar")

    val launch4jOutput       = TaskKey[File]("launch4j-output")
  }
  import Keys.*

  // Patch build script
  val settings = Seq(
    launch4jDir    := {
      val dir = crossTarget.value / "launch4j"
      if(!dir.exists) dir.mkdirs()
      dir
    },
    launch4jConfig := baseDirectory.value / "project" / "launch4j.xml",
    launch4jManifest := baseDirectory.value / "project" / "launch4j.manifest",
    launch4jFinalConfig := {
      val outDir = launch4jDir.value / "out"
      if(!outDir.exists) outDir.mkdirs()

      val path = launch4jDir.value / "config.xml"
      val jarFile = launch4jSourceJar.value
      val jarFileShortName = jarFile.getName.replaceAll("\\.jar$", "")

      val configPersister = net.sf.launch4j.config.ConfigPersister.getInstance()
      configPersister.load(launch4jConfig.value)
      val config = configPersister.getConfig

      val VersionRegex(major, minor, _, patch, _, suffix) = version.value
      val versionString = s"$major.$minor.$patch.0"

      config.setJar     (jarFile)
      config.setOutfile (outDir / s"$jarFileShortName.exe")
      config.setManifest(launch4jManifest.value)

      config.getVersionInfo.setFileVersion      (versionString)
      config.getVersionInfo.setTxtFileVersion   (version.value)
      config.getVersionInfo.setProductVersion   (versionString)
      config.getVersionInfo.setTxtProductVersion(version.value)

      config.getVersionInfo.setOriginalFilename (s"$jarFileShortName.exe")
      config.getVersionInfo.setInternalName     (jarFileShortName)

      configPersister.save(path)

      (config.getOutfile, path)
    },
    launch4jDownloadPath := launch4jDir.value / "launch4j.tgz",
    launch4jDownloadTgz := {
      if(!launch4jDownloadPath.value.exists)
        IO.transfer(new URL(config_launch4j_url).openStream(), launch4jDownloadPath.value)

      def sha256_hex(data: Array[Byte]) = {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(data)
        hash.map(x => "%02x".format(x)).reduce(_ + _)
      }

      val gotChecksum = sha256_hex(IO.readBytes(launch4jDownloadPath.value))
      if(gotChecksum != config_launch4j_checksum)
        sys.error(s"Launch4J checksum error: got $gotChecksum, expected $config_launch4j_checksum")

      launch4jDownloadPath.value
    },
    launch4jBinary := extractLaunch4J(launch4jDownloadTgz.value, launch4jDir.value / "bin") / "launch4j" / "launch4j",
    launch4jOutput := {
      val (outFile, config) = launch4jFinalConfig.value
      runProcess(Seq(launch4jBinary.value, config), baseDirectory.value)
      if(!outFile.exists) sys.error("outfile not found!")
      outFile
    }
  )
}
