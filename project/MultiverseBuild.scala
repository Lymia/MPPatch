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

import com.typesafe.sbt.SbtProguard._
import com.typesafe.sbt.SbtGit._

import language.postfixOps

object MultiverseBuild extends Build {
  val config_scalaVersion = "2.11.7"

  val config_mingw_gcc    = "i686-w64-mingw32-gcc"
  val config_gcc          = "gcc"
  val config_nasm         = "nasm"

  val version_baseVersion = "0.5.0"
  val version_patchCompat = 1

  // Helper functions for working with external programs
  def assertProcess(i: Int) = if(i != 0) sys.error("Process returned non-zero return value! (ret: "+i+")")
  def runProcess   (p: Seq[Any]) = assertProcess(Process(p.map(_.toString)) !)

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
      "// Proxy for " + name + "\n" +
        "typedef " + attr + " " + ret + " (*" + name + "_fn) (" + signature + ");\n" +
        "static " + name + "_fn " + name + "_ptr;\n" +
        ret + " " + name + "(" + signature + ") {\n" +
        "  return " + name + "_ptr(" + paramNames + ");\n" +
        "}\n" +
        "__attribute__((constructor(400))) static void " + name + "_loader() {\n" +
        "  " + name + "_ptr = (" + name + "_fn) "+resolveBody+"\n" +
        "}\n"
    }

    IO.write(target, "#include \"c_rt.h\"\n"+
                     "#include \"c_defines.h\"\n"+
                     "#include \"extern_defines.h\"\n\n"+
                     proxies.mkString("\n"))
  }
  def tryParse(s: String, default: Int) = try { s.toInt } catch { case _: Exception => default }

  lazy val project = Project("multiverse-mod-manager", file(".")) settings (versionWithGit ++ proguardSettings ++ Seq(
    GitKeys.baseVersion in ThisBuild := version_baseVersion,

    organization := "moe.lymia",
    scalaVersion := config_scalaVersion,
    scalacOptions ++= ("-Xlint -Yclosure-elim -target:jvm-1.7 -optimize -deprecation -unchecked "+
                       "-Ydead-code -Yinline -Yinline-handlers").split(" ").toSeq,

    // Dependencies
    resolvers += Resolver.sonatypeRepo("public"),
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0",

    // Package whole project into a single .jar file with Proguard.
    ProguardKeys.options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings"),
    ProguardKeys.options in Proguard += ProguardOptions.keepMain("moe.lymia.multiverse.MultiverseModManager"),

    resourceGenerators in Compile += Def.task {
      val basePath = (resourceManaged in Compile).value
      val logger   = streams.value.log

      // Generate version information file
      val properties = new java.util.Properties
      properties.put("mvmm.version.string", version.value)
      val Version = "([0-9]+)\\.([0-9]+)(\\.([0-9]+))?(-(.*))?".r // major.minor.patch-suffix
      val Version(major, minor, _, patch, _, suffix) = version.value
      properties.put("mvmm.version.major" , major)
      properties.put("mvmm.version.minor" , minor)
      properties.put("mvmm.version.patch" , patch)
      properties.put("mvmm.version.suffix", suffix)
      properties.put("mvmm.version.commit", git.gitHeadCommit.value getOrElse "<unknown>")

      properties.put("mvmm.patch.compat"  , version_patchCompat.toString)

      properties.put("mvmm.build.time"    , new java.util.Date().toString)
      properties.put("mvmm.build.path"    , baseDirectory.value.getAbsolutePath)

      properties.put("mvmm.build.treestatus", try {
        IO.withTemporaryFile[String]("git-status", ".txt") { file =>
          assertProcess("git status --porcelain" #> file !)
          IO.read(file)
        }
      } catch {
        case _: Throwable => "<unknown>"
      })

      val versionPropertiesPath = basePath / "moe" / "lymia" / "multiverse" / "data" / "version.properties"
      IO.write(properties, "Multiverse Mod Manager build information", versionPropertiesPath)

      // Compile patches
      val patchDirectory = basePath / "moe" / "lymia" / "multiverse" / "data" / "patches"
      IO.createDirectory(patchDirectory)
      val patches = IO.withTemporaryDirectory { temp =>
        val patch = baseDirectory.value / "src" / "patch"

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
          "#define patchVersionMajor "+tryParse(major, -1)+"\n"+
          "#define patchVersionMinor "+tryParse(minor, -1)+"\n"+
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

          buildVersion(debug = false, patchDirectory / (version+binaryExtension))
          buildVersion(debug = true , patchDirectory / (version+"_debug"+binaryExtension))

          val properties = new java.util.Properties
          properties.put("normal.resname", version+binaryExtension)
          properties.put("debug.resname" , version+"_debug"+binaryExtension)
          properties.put("platform"      , platform)
          properties.put("sha1"          , sha1)
          IO.write(properties, "Patch information for version "+version, patchDirectory / (version+".properties"))

          Seq(patchDirectory / (version+binaryExtension), patchDirectory / (version+"_debug"+binaryExtension),
              patchDirectory / (version+".properties"))
        }
      }

      // Final generated files list
      Seq(versionPropertiesPath) ++ patches.toSeq.flatten
    }.taskValue
  ))
}