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

import Utils.*
import sbt.*
import sbt.Keys.*

import java.util.UUID

object NativePatchBuild {
  // Patch build script
  val settings = Seq(
    Keys.nativeVersions := {
      val logger   = streams.value.log

      for (
        platform <- Config.config_native_platforms
        if PlatformType.currentPlatform.shouldBuildNative(platform)
      ) yield {
        val (rustTarget, outName, targetName, extraEnv) = platform match {
          case PlatformType.Win32 => (
            "i686-win7-windows-gnu", "mpp_hooklib.dll", "mpp_hooklib.dll",
            Map("LUA_LIB_NAME" -> "lua51_Win32")
          )
          case PlatformType.Linux => (
            "i686-unknown-linux-gnu", "mpp_hooklib.so", "libmpp_hooklib.so",
            Map()
          )
          case _                  => ???
        }

        // run Cargo
        val buildId = UUID.randomUUID()
        runProcess(
          Seq("cargo", "build", "--target", rustTarget, "--release", "-p", "mpp_hooklib"),
          baseDirectory.value,
          Map(
            "MPPATCH_VERSION" -> version.value,
            "MPPATCH_BUILDID" -> buildId.toString
          ) ++ extraEnv
        )

        // make the patches list
        PatchFile(outName, baseDirectory.value / "target" / rustTarget / "release" / targetName, buildId.toString)
      }
    },
    Keys.win32Wrapper := {
      if (PlatformType.currentPlatform.shouldBuildNative(PlatformType.Win32)) {
        val __ = Keys.nativeVersions.value // make an artifical dependency
        runProcess(Seq("scripts/ci/build-win32-wrapper.sh"))
        Some(baseDirectory.value / "target" / "i686-win7-windows-gnu" / "release" / "mpp_hooklib_wrapper.dll")
      } else None
    }
  )

  case class PatchFile(name: String, file: File, buildId: String)
  object Keys {
    val nativeVersions = TaskKey[Seq[PatchFile]]("mppatch-native-versions")
    val win32Wrapper   = TaskKey[Option[File]]("mppatch-native-win32-wrapper")
  }
}
