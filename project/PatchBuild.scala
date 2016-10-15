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

package moe.lymia.mppatch.build

import sbt._
import sbt.Keys._

import Config._
import Utils._

trait PatchBuild { this: Build =>
  object PatchBuildUtils {
    // Helper functions for compiling
    def mingw_gcc(p: Seq[Any]) = runProcess(config_mingw_gcc +: p)
    def gcc      (p: Seq[Any]) = runProcess(config_gcc +: p)
    def nasm     (p: Seq[Any]) = runProcess(config_nasm +: p)

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
    def cacheVersionHeader(cacheDirectory: File, tempTarget: File, finalTarget: File, version: String) = {
      val VersionRegex(major, minor, _, patch, _, suffix) = version
      cachedGeneration(cacheDirectory, tempTarget, finalTarget,
        "#ifndef VERSION_H\n"+
        "#define VERSION_H\n"+
        "#define patchMarkerString \"MPPatch by Lymia (lymia@lymiahugs.com)."+
        "Website: https://github.com/Lymia/MPPatch\"\n"+
        "#define patchVersionMajor "+tryParse(major, -1)+"\n"+
        "#define patchVersionMinor "+tryParse(minor, -1)+"\n"+
        "#define patchCompatVersion "+version_patchCompat+"\n"+
        "#define patchFullVersion \""+version+"\"\n"+
        "#endif /* VERSION_H */"
      )
    }
  }
  import PatchBuildUtils._

  object PatchBuildKeys {
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
  }
  import PatchBuildKeys._

  // Patch build script
  val patchBuildSettings = Seq(
    patchBuildDir  := crossTarget.value / "native-patch-build",
    patchCacheDir  := patchBuildDir.value / "cache",
    patchSourceDir := baseDirectory.value / "src" / "patch",

    // prepare common directories
    commonIncludes := prepareDirectory(patchBuildDir.value / "common") { dir =>
      cacheVersionHeader(patchCacheDir.value / "version_h", patchBuildDir.value / "tmp_version.h",
                         dir / "version.h", version.value)
    },
    win32Directory := prepareDirectory(patchBuildDir.value / "win32") { dir =>
      cachedTransform(patchCacheDir.value / "win23_lua_stub",
        patchSourceDir.value / "win32" / "lua51_Win32.c",
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

    resourceGenerators in Compile += Def.task {
      val patchDirectory = (resourceManaged in Compile).value / "moe" / "lymia" / "mppatch" / "data" / "patches"
      val logger         = streams.value.log

      IO.createDirectory(patchDirectory)

      val patches = for(versionDir <- (patchSourceDir.value / "versions").listFiles) yield {
        val version = versionDir.getName
        val Array(platform, sha1) = version.split("_")

        val (cc, nasmFormat, binaryExtension, sourcePath, sourceFiles, extraCDeps, gccFlags) =
          platform match {
            case "win32" => (mingw_gcc _, "win32", ".dll",
              Seq(patchSourceDir.value / "win32"), Seq(win32ExternDef.value),
              allFiles(win32Directory.value, ".dll"),
              Seq("-l", "lua51_Win32", "-Wl,-L,"+win32Directory.value, "-Wl,--enable-stdcall-fixup",
                  "-Wl,-Bstatic", "-lssp", "-Wl,--dynamicbase,--nxcompat",
                  "-DCV_CHECKSUM=\""+sha1+"\""))
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

        def buildVersion(debug: Boolean, target: File) = {
          val versionStr = "version_"+version + (if(debug) "_debug" else "")
          val logIsDebug = if(debug) " (debug version)" else ""
          val buildTmp = patchBuildDir.value / versionStr
          IO.createDirectory(buildTmp)

          val versionFlags = if(debug) Seq("-DDEBUG") else Seq()
          val nasm_o = trackDependencies(patchCacheDir.value / (versionStr + "_nasm_o"), sBuildDependencies.toSet) {
            logger.info("Compiling as_entry.o"+logIsDebug+" for version "+version)

            val output = buildTmp / "as_entry.o"
            nasm(versionFlags ++ includePaths("-i") ++ Seq("-Ox", "-f", nasmFormat, "-o", output,
                                                           patchSourceDir.value / "common" / "as_entry.s"))
            output
          }
          trackDependencies(patchCacheDir.value / (versionStr + "_c_out"), cBuildDependencies.toSet + nasm_o) {
            logger.info("Compiling binary patch"+logIsDebug+" for version "+version)

            cc(versionFlags ++ includePaths("-I") ++ Seq(
              "-m32", "-flto", "-g", "-shared", "-O2", "--std=gnu11", "-Wall", "-o", target,
              "-fstack-protector", "-fstack-protector-all", "-D_FORTIFY_SOURCE=2", nasm_o) ++
              gccFlags ++ fullSourcePath.flatMap(x => allFiles(x, ".c")) ++ sourceFiles)
            target
          }
        }

        val normalPath = buildVersion(debug = false, patchDirectory / (version+binaryExtension))
        val debugPath  = buildVersion(debug = true , patchDirectory / (version+"_debug"+binaryExtension))

        val mf = trackDependencies(patchCacheDir.value / ("version_manifest_"+version), Set(normalPath, debugPath)) {
          logger.info("Creating version manifest for version "+version)

          val propTarget = patchDirectory / (version+".properties")
          val properties = new java.util.Properties
          properties.put("normal.resname", version+binaryExtension)
          properties.put("normal.sha1"   , sha1_hex(IO.readBytes(normalPath)))
          properties.put("debug.resname" , version+"_debug"+binaryExtension)
          properties.put("debug.sha1"    , sha1_hex(IO.readBytes(debugPath)))
          properties.put("platform"      , platform)
          properties.put("target.sha1"   , sha1)
          IO.write(properties, "Patch information for version "+version, propTarget)
          propTarget
        }

        Seq(normalPath, debugPath, mf)
      }

      patches.toSeq.flatten
    }.taskValue
  )
}
