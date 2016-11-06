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

import sbt._
import sbt.Keys._
import Config._
import Utils._

object LuaJITBuild {
  val coreCount = java.lang.Runtime.getRuntime.availableProcessors
  def make(dir: File, actions: Seq[String], env: Map[String, String]) =
    runProcess(config_make +: "--trace" +: "-C" +: dir.toString +: "-j" +: coreCount.toString +:
               (actions ++ env.map(x => s"${x._1}=${x._2}")))

  case class LuaJITPatchFile(platform: String, file: File)
  object Keys {
    val luajitCacheDir  = SettingKey[File]("luajit-cache-dir")
    val luajitSourceDir = SettingKey[File]("luajit-source-dir")
    val luajitIncludes  = SettingKey[File]("luajit-includes")

    val luajitFiles = TaskKey[Seq[LuaJITPatchFile]]("luajit-files")
  }
  import Keys._

  // Patch build script
  val settings = Seq(
    luajitCacheDir  := crossTarget.value / "luajit-cache",
    luajitSourceDir := baseDirectory.value / "src" / "patch" / "native" / "luajit",
    luajitIncludes  := luajitSourceDir.value / "src",

    luajitFiles := {
      val patchDirectory = luajitCacheDir.value / "output"
      val logger         = streams.value.log
      IO.createDirectory(patchDirectory)

      for (platform <- Seq("win32", "linux")) yield {
        val (platformEnv, outputFile, extension, flags) =
          platform match {
            case "win32" =>
              (Map("HOST_CC" -> (config_linux_gcc+" -m32"), "CROSS" -> config_mingw_prefix,
                   "TARGET_SYS" -> "Windows"), "src/lua51.dll", ".dll",
               Seq("-static-libgcc") ++ config_win32_secureFlags)
            case "linux" =>
              (Map("CC" -> (config_linux_gcc+" -m32")), "src/libluajit.so", ".so", config_linux_secureFlags)
          }
        val allFlags = ("-O2" +: "-flto" +: (config_common_secureFlags ++ flags)).mkString(" ")
        val env = Map("TARGET_FLAGS" -> allFlags) ++ platformEnv
        val excludeDeps = Set(
          "lj_bcdef.h", "lj_ffdef.h", "lj_libdef.h", "lj_recdef.h", "lj_folddef.h", "buildvm_arch.h", "vmdef.lua"
        )
        val dependencies = Path.allSubpaths(luajitSourceDir.value).filter { case (_, x) =>
          (x.endsWith(".c") || x.endsWith(".h") || x.endsWith("Makefile") || x.endsWith(".lua")) &&
          !excludeDeps.contains(x.split("/").last)
        }.map(_._1)

        val outTarget = patchDirectory / s"luajit_$platform$extension"
        val outputPath =
          trackDependencies(luajitCacheDir.value / (platform + "_c_out"), dependencies.toSet) {
            logger.info("Compiling Luajit for "+platform)
            make(luajitSourceDir.value, Seq("clean"), env)
            make(luajitSourceDir.value, Seq("default"), env)
            IO.copyFile(luajitSourceDir.value / outputFile, outTarget)
            outTarget
          }

        LuaJITPatchFile(platform, outTarget)
      }
    }
  )
}
