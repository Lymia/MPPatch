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

import moe.lymia.mppatch.util.{Logger, SimpleLogger, VersionInfo}

import java.io.{File, FileOutputStream, OutputStreamWriter, PrintWriter}
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.Locale
import javax.swing.JFrame

object InstallerMain {
  val logFile = new File("mppatch_installer.log")
}
class InstallerMain extends FrameError[JFrame] with I18NTrait {
  private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US)

  protected def locale = Locale.getDefault
  override protected def frame: JFrame = null

  def main(args: Array[String]): Unit =
      try {
      System.setProperty("awt.useSystemAAFontSettings","on")
      System.setProperty("swing.aatext", "true")

      SimpleLogger.addLogger(new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(InstallerMain.logFile, true), StandardCharsets.UTF_8)))

      val line = Seq.fill(80)("=").mkString("")
      log.logRaw("")
      log.logRaw(line)
      log.logRaw(s"MPPatch version ${VersionInfo.versionString}")
      log.logRaw(s"Revision ${VersionInfo.commit.substring(0, 8)}${if(VersionInfo.isDirty) " (dirty)" else ""}, "+
                     s"built on ${dateFormat.format(VersionInfo.buildDate)} " +
                     s"by ${VersionInfo.buildUser}@${VersionInfo.buildHostname}")
      log.logRaw("")

      com.formdev.flatlaf.FlatIntelliJLaf.setup()

      val versionData = Seq(
        "Build ID"    -> VersionInfo.buildID,
        "Build Date"  -> Logger.dateFormat.format(VersionInfo.buildDate),
        "Revision"    -> VersionInfo.commit,
        "Tree Status" -> VersionInfo.treeStatus
      )
      val headerLength = versionData.map(_._1.length).max
      val indent = Seq.fill(headerLength + 2)(" ").mkString("")
      val formatStr = s"%-${headerLength}s: %s"
      for((k, v) <- versionData) {
        val lines = v.split("\n")
        log.logRaw(formatStr.format(k, lines.head))
        for(l <- lines.tail) log.logRaw(indent + l)
      }
      log.logRaw(line)
      log.logRaw("")

      new MainFrame(locale).showForm()
    } catch {
      case _: InstallerException => // ignored
      case e: Exception => try {
        dumpException("error.genericerror", e)
      } catch {
        case _: InstallerException => // ignored
      }
    }
}

object Installer {
  def main(args: Array[String]): Unit = new InstallerMain().main(args)
}