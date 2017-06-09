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

import java.io.FileOutputStream
import java.util.jar.{JarFile, Pack200}
import java.util.jar.Pack200.Packer

import org.tukaani.xz.{LZMA2Options, XZOutputStream}
import sbt._
import sbt.Keys._

object LoaderBuild {
  object Keys {
    val loaderSourceJar  = TaskKey[File]("loader-source-jar")
    val loaderTargetPath = TaskKey[String]("loader-target-path")

    val loaderExclude    = TaskKey[Set[String]]("loader-exclude")

    val loaderOutputFile = TaskKey[File]("loader-output-file")
    val loaderOutputRaw  = TaskKey[(File, Seq[File])]("loader-output-raw")
    val loaderOutput     = TaskKey[File]("loader-output")
  }
  import Keys._

  val settings = Seq(
    loaderExclude := Set(),
    loaderOutputFile := crossTarget.value / "loaderTarget.pack.xz",
    loaderOutputRaw := {
      val packer = Pack200.newPacker()
      val p      = packer.properties()
      p.put(Packer.EFFORT, "9")
      p.put(Packer.SEGMENT_LIMIT, "-1")
      p.put(Packer.KEEP_FILE_ORDER, Packer.FALSE)
      p.put(Packer.MODIFICATION_TIME, Packer.LATEST)
      p.put(Packer.UNKNOWN_ATTRIBUTE, Packer.ERROR)

      val exclude = loaderExclude.value
      val copiedResources = IO.withTemporaryFile("loaderTarget", ".jar") { tempFile =>
        val copiedResources = IO.withTemporaryDirectory { dir =>
          IO.unzip(loaderSourceJar.value, dir)
          val (classFiles, resources) =
            Path.allSubpaths(dir).partition { x => !exclude.contains(x._2) }
          IO.zip(classFiles, tempFile)

          for((file, path) <- resources if file.isFile) yield {
            val target = (resourceManaged in Compile).value / path
            IO.createDirectory(target.getParentFile)
            IO.copyFile(file, target)
            target
          }
        }

        packer.pack(new JarFile(tempFile), new FileOutputStream(loaderOutputFile.value))

        copiedResources
      }

      (loaderOutputFile.value, copiedResources.toSeq)
    },
    loaderOutput := loaderOutputRaw.value._1,
    resourceGenerators in Compile += Def.task {
      val target = (resourceManaged in Compile).value / loaderTargetPath.value
      IO.copyFile(loaderOutput.value, target)
      Seq(target)
    }.taskValue,
    resourceGenerators in Compile += Def.task { loaderOutputRaw.value._2 }.taskValue
  )
}