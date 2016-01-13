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

import java.security.MessageDigest

import sbt._

import MultiverseBuild._

import language.postfixOps

trait PatchBuild { this: Build =>
  // Helper functions for compiling
  def mingw_gcc(p: Seq[Any]) = runProcess(config_mingw_gcc +: p)
  def gcc      (p: Seq[Any]) = runProcess(config_gcc +: p)
  def nasm     (p: Seq[Any]) = runProcess(config_nasm +: p)

  def dir     (path: File) = path.toString + "/"
  def allFiles(path: File, extension: String) = path.listFiles.filter(_.getName.endsWith(extension)).toSeq
  val userHome = new File(System.getProperty("user.home"))

  // Codegen for the proxy files.
  def generateProxyDefine(file: File, target: File) {
    val lines = IO.readLines(file).filter(_.nonEmpty)
    val proxies = for(Array(t, name, attr, ret, signature, sym) <- lines.map(_.trim.split(":"))) yield {
      val paramNames = signature.split(",").map(_.trim.split(" ").last).mkString(", ")
      val rsymbol = if(sym == "*") name else sym
      val resolveBody =
        if(t == "offset") "resolveAddress(" + name + "_offset);"
        else if(t == "symbol") "resolveSymbol(\"" + rsymbol + "\");"
        else sys.error("Unknown proxy type "+t)
      ("// Proxy for " + name + "\n" +
        "typedef " + attr + " " + ret + " (*" + name + "_fn) (" + signature + ");\n" +
        "static " + name + "_fn " + name + "_ptr;\n" +
        ret + " " + name + "(" + signature + ") {\n" +
        "  return " + name + "_ptr(" + paramNames + ");\n" +
        "}\n", "  " + name + "_ptr = (" + name + "_fn) "+resolveBody)
    }

    IO.write(target, "#include \"c_rt.h\"\n"+
      "#include \"c_defines.h\"\n"+
      "#include \"extern_defines.h\"\n\n"+
      proxies.map(_._1).mkString("\n")+"\n\n"+
      "__attribute__((constructor(400))) static void loadGeneratedExternSymbols() {\n"+
      proxies.map(_._2).mkString("\n")+"\n"+
      "}\n")
  }
  def tryParse(s: String, default: Int) = try { s.toInt } catch { case _: Exception => default }

  // Crypto helper functions
  def digest(algorithm: String, data: Seq[Byte]) = {
    val md = MessageDigest.getInstance(algorithm)
    val hash = md.digest(data.toArray)
    hash
  }
  def hexdigest(algorithm: String, data: Seq[Byte]) =
    digest(algorithm, data).map(x => "%02x".format(x)).reduce(_ + _)
  def sha1_hex(data: Seq[Byte]) = hexdigest("SHA1", data)

  // Patch build script
  def buildPatch(basePath: File, baseDirectory: File, logger: Logger,
                 major: Int, minor: Int) = {
    val patchDirectory = basePath / "moe" / "lymia" / "multiverse" / "data" / "patches"
    IO.createDirectory(patchDirectory)
    val patches = IO.withTemporaryDirectory { temp =>
      val patch = baseDirectory / "src" / "patch"

      val win32Target  = temp / "win32"
      val linuxTarget  = temp / "linux"
      val commonTarget = temp / "common"
      IO.createDirectories(Seq(win32Target, linuxTarget, commonTarget))

      logger.info("Compiling lua51_Win32.dll linking stub")
      mingw_gcc(Seq("-shared", "-o", win32Target / "lua51_Win32.dll", patch / "win32" / "lua51_Win32.c"))

      logger.info("Generating extern_defines.c for all platforms.")
      generateProxyDefine(patch / "win32" / "extern_defines.gen", win32Target / "extern_defines.c")
      generateProxyDefine(patch / "linux" / "extern_defines.gen", linuxTarget / "extern_defines.c")

      logger.info("Generating version.h")
      IO.write(commonTarget / "version.h",
        "#ifndef VERSION_H\n"+
          "#define VERSION_H\n"+
          "#define patchMarkerString \"Multiverse Mod Manager CvGameDatabase patch by Lymia (lymia@lymiahugs.com)."+
          "Website: https://github.com/Lymia/MultiverseModManager\"\n"+
          "#define patchVersionMajor "+major+"\n"+
          "#define patchVersionMinor "+minor+"\n"+
          "#define patchCompatVersion "+version_patchCompat+"\n"+
          "#endif /* VERSION_H */"
      )

      for(versionDir <- (patch / "versions").listFiles) yield {
        val version = versionDir.getName
        val Array(platform, sha1) = version.split("_")

        logger.info("Compiling patch for version "+version)

        val (cc, nasmFormat, buildPath, includePath, binaryExtension, gccFlags) = platform match {
          case "win32" => (mingw_gcc _, "win32", win32Target, patch / "win32", ".dll",
            Seq("-l", "lua51_Win32", "-Wl,-L,"+win32Target, "-Wl,--enable-stdcall-fixup",
              "-Wl,-Bstatic", "-lssp", "-Wl,--dynamicbase,--nxcompat",
              "-DCV_CHECKSUM=\""+sha1+"\""))
          case "linux" => (gcc       _, "elf"  , linuxTarget, patch / "linux", ".so" ,
            Seq("-I", "/usr/include/SDL2/", userHome / ".steam/bin32/libSDL2-2.0.so.0", "-ldl"))
        }

        def buildVersion(debug: Boolean, target: File) {
          val buildTmp = temp / ("build_" + version + (if(debug) "_debug" else ""))
          IO.createDirectory(buildTmp)
          val versionFlags = if(debug) Seq("-DDEBUG") else Seq("-D_FORTIFY_SOURCE=2")

          nasm(versionFlags ++ Seq("-Ox", "-i", dir(versionDir), "-i", dir(patch / "common"), "-i", dir(includePath),
            "-f", nasmFormat, "-o", buildTmp / "as.o", patch / "common" / "as_entry.s"))
          cc(versionFlags ++ Seq("-m32", "-flto", "-g", "-shared", "-O2", "--std=gnu99", "-o", target,
            "-fstack-protector", "-fstack-protector-all",
            "-I", versionDir, "-I", patch / "common", "-I", includePath, "-I", commonTarget,
            buildPath / "extern_defines.c", buildTmp / "as.o") ++
            gccFlags ++ allFiles(patch / "common", ".c") ++ allFiles(includePath, ".c"))
        }

        val normalPath = patchDirectory / (version+binaryExtension)
        val debugPath  = patchDirectory / (version+"_debug"+binaryExtension)

        buildVersion(debug = false, normalPath)
        buildVersion(debug = true , debugPath)

        val properties = new java.util.Properties
        properties.put("normal.resname", version+binaryExtension)
        properties.put("normal.sha1"   , sha1_hex(IO.readBytes(normalPath)))
        properties.put("debug.resname" , version+"_debug"+binaryExtension)
        properties.put("debug.sha1"    , sha1_hex(IO.readBytes(debugPath)))
        properties.put("platform"      , platform)
        properties.put("target.sha1"   , sha1)
        IO.write(properties, "Patch information for version "+version, patchDirectory / (version+".properties"))

        Seq(normalPath, debugPath, patchDirectory / (version+".properties"))
      }
    }

    patches.toSeq.flatten
  }
}