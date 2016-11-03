/*
 * Copyright (C) 2015-2016 Lymia Aluysia <lymiahugs@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import sbt._
import sbt.Keys._
import Config._
import Utils._

object NativePatchBuild {
  def mingw_gcc(p: Seq[Any]) = runProcess(config_mingw_gcc +: p)
  def gcc      (p: Seq[Any]) = runProcess(config_linux_gcc +: p)
  def nasm     (p: Seq[Any]) = runProcess(config_nasm +: p)

  // Steam runtime
  def extractSteamRuntime[T](source: File, target: File, beforeLog: => T = null)(fn: (File, File) => Unit) =
    if(target.exists) target else {
      beforeLog
      IO.withTemporaryDirectory { temp =>
        runProcess(Seq("ar", "xv", source), temp)
        runProcess(Seq("tar", "xvf", temp / "data.tar.gz"), temp)
        fn(temp, target)
      }
      target
    }

  // Codegen for the proxy files.
  def generateProxyDefine(file: File, target: File) {
    val lines = IO.readLines(file).filter(_.nonEmpty)
    val proxies = for(Array(t, name, attr, ret, signature, domain, sym) <- lines.map(_.trim.split(":"))) yield {
      val functionDef =
        s"""// Proxy for $name
           |typedef $attr $ret (*${name}_fn) ($signature);
           |static ${name}_fn ${name}_ptr;
           |$ret $name($signature) {
           |  return ${name}_ptr(${signature.split(",").map(_.trim.split(" ").last).mkString(", ")});
           |}
        """.stripMargin
      val initString = s"  ${name}_ptr = (${name}_fn) ${
        if(t == "offset") s"resolveAddress($domain, ${name}_offset);"
        else if(t == "symbol") s"""resolveSymbol($domain, "${if(sym == "*") name else sym}");"""
        else sys.error(s"Unknown proxy type $t")
      }"
      (functionDef, initString)
    }

    IO.write(target, "#include \"c_rt.h\"\n"+
      "#include \"c_defines.h\"\n"+
      "#include \"extern_defines.h\"\n\n"+
      proxies.map(_._1).mkString("\n")+"\n\n"+
      "__attribute__((constructor(400))) static void loadGeneratedExternSymbols() {\n"+
      proxies.map(_._2).mkString("\n")+"\n"+
      "}\n")
  }

  // Codegen for version header
  def tryParse(s: String, default: Int) = try { s.toInt } catch { case _: Exception => default }
  def cacheVersionHeader(cacheDirectory: File, tempTarget: File, finalTarget: File, version: String) = {
    val VersionRegex(major, minor, _, _, _, _) = version
    cachedGeneration(cacheDirectory, tempTarget, finalTarget,
      "#ifndef VERSION_H\n"+
      "#define VERSION_H\n"+
      "#define patchVersionMajor "+tryParse(major, -1)+"\n"+
      "#define patchVersionMinor "+tryParse(minor, -1)+"\n"+
      "#define patchFullVersion \""+version+"\"\n"+
      "#endif /* VERSION_H */"
    )
  }

  case class PatchFile(platform: String, version: String, file: File)
  object Keys {
    val patchBuildDir  = SettingKey[File]("native-patch-build-directory")
    val patchCacheDir  = SettingKey[File]("native-patch-cache-directory")
    val patchSourceDir = SettingKey[File]("native-patch-source-directory")

    val win32Directory = TaskKey[File]("native-patch-win32-directory")
    val linuxDirectory = TaskKey[File]("native-patch-linux-directory")

    val steamrtSDL     = TaskKey[File]("native-patch-download-steam-runtime-sdl")
    val steamrtSDLDev  = TaskKey[File]("native-patch-download-steam-runtime-sdl-dev")

    val commonIncludes = TaskKey[File]("native-patch-common-includes")

    val win32ExternDef = TaskKey[File]("native-patch-win32-extern-defines")
    val linuxExternDef = TaskKey[File]("native-patch-linux-extern-defines")

    val nativeVersions = TaskKey[Seq[PatchFile]]("native-patch-files")
  }
  import Keys._

  // Patch build script
  val settings = Seq(
    patchBuildDir  := crossTarget.value / "native-patch-build",
    patchCacheDir  := patchBuildDir.value / "cache",
    patchSourceDir := baseDirectory.value / "src" / "patch" / "native",

    // prepare common directories
    commonIncludes := prepareDirectory(patchBuildDir.value / "common") { dir =>
      cacheVersionHeader(patchCacheDir.value / "version_h", patchBuildDir.value / "tmp_version.h",
                         dir / "version.h", version.value)
    },
    win32Directory := prepareDirectory(patchBuildDir.value / "win32") { dir =>
      cachedTransform(patchCacheDir.value / "win23_lua_stub",
        patchSourceDir.value / "win32" / "stub" / "lua51_Win32.c",
        dir / "lua51_Win32.dll")((in, out) => mingw_gcc(Seq("-shared", "-o", out, in)))
    },
    linuxDirectory := simplePrepareDirectory(patchBuildDir.value / "linux"),

    // Extract Steam runtime libSDL files.
    steamrtSDL :=
      extractSteamRuntime(baseDirectory.value / "project" / "contrib_bin" / config_steam_sdlbin_path,
                          patchBuildDir.value / config_steam_sdlbin_name,
                          streams.value.log.info("Extracting "+config_steam_sdlbin_name+"...")) { (dir, target) =>
        IO.copyFile(dir / "usr" / "lib" / "i386-linux-gnu" / config_steam_sdlbin_name, target)
      },
    steamrtSDLDev :=
      extractSteamRuntime(baseDirectory.value / "project" / "contrib_bin" / config_steam_sdldev_path,
                          patchBuildDir.value / "SDL2_include",
                          streams.value.log.info("Extracting SDL2 headers...")) { (dir, target) =>
        IO.copyDirectory(dir / "usr" / "include" / "SDL2", target)
      },

    // prepare generated source
    win32ExternDef := cachedTransform(patchCacheDir.value / "win32_extern",
      patchSourceDir.value / "win32" / "extern_defines.gen",
      win32Directory.value / "extern_defines.c")(generateProxyDefine),
    linuxExternDef := cachedTransform(patchCacheDir.value / "linux_extern",
      patchSourceDir.value / "linux" / "extern_defines.gen",
      linuxDirectory.value / "extern_defines.c")(generateProxyDefine),

    nativeVersions := {
      val patchDirectory = patchBuildDir.value / "output"
      val progVersion    = version.value
      val logger         = streams.value.log

      IO.createDirectory(patchDirectory)

      val patches = for(versionDir <- (patchSourceDir.value / "versions").listFiles) yield {
        val version = versionDir.getName
        val Array(platform, sha256) = version.split("_")

        val (cc, nasmFormat, binaryExtension, sourcePath, sourceFiles, extraCDeps, gccFlags) =
          platform match {
            case "win32" => (mingw_gcc _, "win32", ".dll",
              Seq(patchSourceDir.value / "win32"), Seq(win32ExternDef.value),
              allFiles(win32Directory.value, ".dll"),
              Seq("-l", "lua51_Win32", "-Wl,-L,"+win32Directory.value, "-Wl,--enable-stdcall-fixup",
                  "-Wl,-Bstatic", "-lssp", "-Wl,--dynamicbase,--nxcompat"))
            case "linux" => (gcc       _, "elf"  , ".so" ,
              Seq(patchSourceDir.value / "linux", steamrtSDLDev.value), Seq(linuxExternDef.value),
              Seq(steamrtSDL.value),
              Seq("-ldl"))
          }
        val fullSourcePath = Seq(patchSourceDir.value / "common", commonIncludes.value, versionDir) ++ sourcePath
        val cBuildDependencies =
          fullSourcePath.flatMap(x => allFiles(x, ".c") ++ allFiles(x, ".h")) ++ sourceFiles ++ extraCDeps
        val sBuildDependencies = fullSourcePath.flatMap(x => allFiles(x, ".s"))
        def includePaths(flag: String) = fullSourcePath.flatMap(x => Seq(flag, dir(x)))

        val target = patchDirectory / (version+"_"+progVersion+binaryExtension)
        val versionStr = "version_"+version
        val buildTmp = patchBuildDir.value / versionStr
        IO.createDirectory(buildTmp)

        val nasm_o = trackDependencies(patchCacheDir.value / (versionStr + "_nasm_o"), sBuildDependencies.toSet) {
          logger.info("Compiling as_entry.o for version "+version)

          val output = buildTmp / "as_entry.o"
          nasm(includePaths("-i") ++ Seq("-Ox", "-f", nasmFormat, "-o", output,
                                         patchSourceDir.value / "common" / "as_entry.s"))
          output
        }
        val outputPath =
          trackDependencies(patchCacheDir.value / (versionStr + "_c_out"), cBuildDependencies.toSet + nasm_o) {
            logger.info("Compiling binary patch for version "+version)

            cc(includePaths("-I") ++ Seq(
              "-m32", "-flto", "-g", "-shared", "-O2", "--std=gnu11", "-Wall", "-o", target,
              "-fstack-protector", "-fstack-protector-all", "-D_FORTIFY_SOURCE=2", nasm_o) ++
              gccFlags ++ fullSourcePath.flatMap(x => allFiles(x, ".c")) ++ sourceFiles)
            target
          }

        PatchFile(platform, sha256, outputPath)
      }
      patches.toSeq
    }
  )
}
