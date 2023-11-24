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

import Config.*
import Utils.*
import sbt.*
import sbt.Keys.*

import java.util.UUID

object NativePatchBuild {
  // Patch build script
  val settings = Seq(
    Keys.nativeVersions := {
      val crateDir = baseDirectory.value / "src" / "patch" / "mppatch-core"
      val logger   = streams.value.log

      for (
        platform <- Seq[PlatformType](PlatformType.Linux)
        if PlatformType.currentPlatform.shouldBuildNative(platform)
      ) yield {
        val (rustTarget, outName, targetName) = platform match {
          case PlatformType.Win32 => ("i686-pc-windows-gnu", "mppatch_core.dll", "mppatch_core.dll")
          case PlatformType.Linux => ("i686-unknown-linux-gnu", "mppatch_core.so", "libmppatch_core.so")
          case _                  => sys.error("unreachable")
        }

        // clean build the package
        IO.delete(crateDir / "target" / rustTarget)

        // run Cargo
        val buildId = UUID.randomUUID()
        runProcess(
          Seq("cargo", "build", "--target", rustTarget, "--release"),
          crateDir,
          Map(
            "MPPATCH_VERSION" -> version.value,
            "MPPATCH_BUILDID" -> buildId.toString
          )
        )

        // make the patches list
        PatchFile(outName, crateDir / "target" / rustTarget / "release" / targetName, buildId.toString)
      }
    }
  )

  case class PatchFile(name: String, file: File, buildId: String)
  object Keys {
    val nativeVersions = TaskKey[Seq[PatchFile]]("native-patch-files")
  }
}
