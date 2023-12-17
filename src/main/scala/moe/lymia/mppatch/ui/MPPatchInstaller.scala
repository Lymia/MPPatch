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

package moe.lymia.mppatch.ui

import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont
import com.formdev.flatlaf.{FlatIntelliJLaf, FlatLaf}
import moe.lymia.mppatch.core.PlatformType
import moe.lymia.mppatch.util.{Logger, SimpleLogger, VersionInfo}

import java.io.{File, FileOutputStream, OutputStreamWriter, PrintWriter}
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.Locale
import javax.swing.{JFrame, JOptionPane, UIManager}

object MPPatchInstaller extends LaunchFrameError {
  private val nsisMarker = "018c6bba-54e0-7cf2-b16a-7b6abb9215e0"
  private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)

  private lazy val isAppImage =
    PlatformType.currentPlatform == PlatformType.Linux && System.getenv("APPIMAGE") != null
  private lazy val isNsis =
    PlatformType.currentPlatform == PlatformType.Win32 && System.getenv("NSIS_LAUNCH_MARKER") == nsisMarker

  private[this] def fileForEnv(test: Boolean, env: String) = if (test) {
    Some(new File(System.getenv(env)).getCanonicalFile)
  } else {
    None
  }

  private lazy val appImageFileLocation = fileForEnv(isAppImage, "APPIMAGE")
  private lazy val appImageContents     = fileForEnv(isAppImage, "APPDIR")

  private lazy val nsisFileLocation = fileForEnv(isNsis, "NSIS_LAUNCH_EXE")
  private lazy val nsisContents     = fileForEnv(isNsis, "NSIS_LAUNCH_TEMPDIR")

  private lazy val baseDirectory = if (isAppImage) {
    appImageFileLocation.get.getParentFile
  } else if (isNsis) {
    nsisFileLocation.get.getParentFile
  } else {
    new File(".").getCanonicalFile
  }

  lazy val logFile = baseDirectory.toPath.resolve("mppatch_installer.log").toFile

  def main(args: Array[String]): Unit =
    try {
      // enable anti-aliasing
      System.setProperty("awt.useSystemAAFontSettings", "on")
      System.setProperty("swing.aatext", "true")

      // set properties for appimage
      if (isAppImage) {
        System.setProperty("java.library.path", s"${appImageContents.get.toString}/usr/lib/")
      }

      // enable logging
      SimpleLogger.addLogger(
        new PrintWriter(
          new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8)
        )
      )

      // print version information to console
      val line = Seq.fill(80)("=").mkString("")
      log.logRaw("")
      log.logRaw(line)
      log.logRaw(s"MPPatch version ${VersionInfo.versionString}")
      log.logRaw(
        s"Revision ${VersionInfo.commit.substring(0, 8)}${if (VersionInfo.isDirty) " (dirty)" else ""}, " +
          s"built on ${dateFormat.format(VersionInfo.buildDate)} " +
          s"by ${VersionInfo.buildUser}@${VersionInfo.buildHostname}" +
          (if (VersionInfo.isCi) " (Github Actions)" else "")
      )
      log.logRaw("")

      val versionData = Seq(
        "Build ID"    -> VersionInfo.buildID,
        "Build Date"  -> Logger.dateFormat.format(VersionInfo.buildDate),
        "Revision"    -> VersionInfo.commit,
        "Tree Status" -> VersionInfo.treeStatus
      )
      val headerLength = versionData.map(_._1.length).max
      val indent       = Seq.fill(headerLength + 2)(" ").mkString("")
      val formatStr    = s"%-${headerLength}s: %s"
      for ((k, v) <- versionData) {
        val lines = v.split("\n")
        log.logRaw(formatStr.format(k, lines.head))
        for (l <- lines.tail) log.logRaw(indent + l)
      }
      log.logRaw(line)
      log.logRaw("")

      // check runtime environment
      if (isAppImage) {
        log.info("Running from appimage")
        log.info(f"AppImage binary: ${appImageFileLocation.get}")
        log.info(f"AppImage directory: ${appImageContents.get}")
      } else if (isNsis) {
        log.info("Running from NSIS wrapper")
        log.info(f"NSIS binary: ${nsisFileLocation.get}")
        log.info(f"NSIS temporary directory: ${nsisContents.get}")
      } else {
        log.info("Running from .jar")
      }
      log.info(f"Platform: ${PlatformType.currentPlatform}")
      log.info(f"Base directory: ${baseDirectory}")
      log.logRaw("")

      // install look-and-feel
      FlatRobotoFont.install()
      FlatLaf.setPreferredFontFamily(FlatRobotoFont.FAMILY)
      FlatLaf.setPreferredLightFontFamily(FlatRobotoFont.FAMILY_LIGHT)
      FlatLaf.setPreferredSemiboldFontFamily(FlatRobotoFont.FAMILY_SEMIBOLD)
      FlatIntelliJLaf.setup()

      // check command line arguments
      val checkUuid = "9e3c6db9-2a2f-4a22-9eb5-fba1a710449c"
      if (args.length == 2 && args(0) == "@nativeImageGenerateConfig" && args(1) == checkUuid) {
        // generate native image configs
        log.warn("Generating native image configs...")
        log.warn("If you did not intend this, I don't know what to say.")
        Preferences.updatePreferences()
        NativeImageGenConfig.run()
      } else {
        // start main frame
        Preferences.updatePreferences()
        new MainFrame(locale).showForm()
      }
    } catch {
      case _: InstallerException => // ignored
      case e: Exception =>
        try reportException("error.genericerror", e)
        catch {
          case _: InstallerException => // ignored
        }
    }
}
