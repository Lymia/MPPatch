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

package moe.lymia.mppatch.ui

import java.io.File
import java.nio.file.{Files, Path}
import java.util.Locale

import moe.lymia.mppatch.core.PatchInstaller
import moe.lymia.mppatch.util.res.{I18N, VersionInfo}
import moe.lymia.mppatch.platform.Platform

case class CLIArguments(command: (CLIArguments, Platform, PatchInstaller) => Unit,
                        systemPath: Option[Path],
                        // patch options
                        debug: Boolean = false)

class CLI(locale: Locale) {
  private val i18n = I18N(locale)
  private val parser = new scopt.OptionParser[CLIArguments]("mppatch") {
    head(i18n("common.title"), "v"+VersionInfo.versionString)

    help("help").text(i18n(s"cli.param.help"))

    private def cmd2(name: String) = {
      note("")
      cmd(name).text(i18n(s"cli.cmd.$name"))
    }

    opt[File]("system-path").action((f, args) => args.copy(systemPath = Some(f.toPath)))
      .valueName(i18n("cli.param.args.directory")).text(i18n("cli.param.system-path"))

    cmd2("status").action((_, args) => args.copy(command = cmd_status))
    cmd2("updatePatch").action((_, args) => args.copy(command = cmd_update)).children(
      opt[Unit]('d', "debug").action((_, args) => args.copy(debug = true))
        .text(i18n("cli.cmd.updatePatch.param.debug"))
    )
    cmd2("uninstallPatch").action((_, args) => args.copy(command = cmd_uninstall))
  }

  private def fatal(err: String) = {
    System.err.println(err)
    System.exit(-2)
    sys.error("System.exit returned!!!")
  }

  private def loadInstaller(args: CLIArguments, platform: Platform) =
    new PatchInstaller(args.systemPath.get, platform)

  private def cmd_unknown(args: CLIArguments, platform: Platform, installer: PatchInstaller) = {
    println(i18n("cli.cmd.unknown"))
  }

  private def cmd_status(args: CLIArguments, platform: Platform, installer: PatchInstaller) = {
    val status = installer.checkPatchStatus()
    println(i18n("cli.cmd.status.patchStatus", status))
  }

  private def cmd_update(args: CLIArguments, platform: Platform, installer: PatchInstaller) = {
    installer.safeUpdate(args.debug)
  }
  private def cmd_uninstall(args: CLIArguments, platform: Platform, installer: PatchInstaller) = {
    installer.safeUninstall()
  }

  def executeCommand(args: Seq[String]) = {
    val platform = Platform.currentPlatform.getOrElse(sys.error("Unknown platform."))
    val systemPath = resolvePaths(platform.defaultSystemPaths)
    parser.parse(args, CLIArguments(cmd_unknown, systemPath)) match {
      case Some(command) => command.command(command, platform, loadInstaller(command, platform))
      case None => System.exit(-1)
    }
  }
}
