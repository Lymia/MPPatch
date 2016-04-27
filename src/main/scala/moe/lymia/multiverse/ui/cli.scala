package moe.lymia.multiverse.ui

import java.io.File
import java.nio.file.{Files, Path}

import moe.lymia.multiverse.core.Installer
import moe.lymia.multiverse.util.res.VersionInfo
import moe.lymia.multiverse.platform.Platform

case class CLIArguments(command: (CLIArguments, Platform) => Unit,
                        systemPath: Option[Path], userPath: Option[Path],
                        // common options
                        force: Boolean = false,
                        // patch options
                        debug: Boolean = false)

object CLI {
  val parser = new scopt.OptionParser[CLIArguments]("mvmm") {
    head("Multiverse Mod Manager", "v"+VersionInfo.versionString)

    help("help") text "Shows usage information."

    opt[File]("system-path").action((f, args) => args.copy(systemPath = Some(f.toPath)))
      .valueName("<directory>").text("Path for the Civilization V installation directory.")
    opt[File]("user-path"  ).action((f, args) => args.copy(userPath   = Some(f.toPath)))
      .valueName("<directory>").text("Path for the Civilization v user directory")

    note("")
    cmd("status").action((_, args) => args.copy(command = cmd_status))
      .text("Displays installation status for mods/the patch.")

    note("")
    cmd("updatePatch").action((_, args) => args.copy(command = cmd_update))
      .text("Updates the Multiverse Mod Manager patch.")
      .children(
        opt[Unit]('f', "force").action((_, args) => args.copy(force = true)).text("Force update patch."),
        opt[Unit]('d', "debug").action((_, args) => args.copy(debug = true)).text("Install debug version of patch.")
      )
  }

  private def resolvePaths(paths: Seq[Path]) = paths.find(x => Files.exists(x) && Files.isDirectory(x))
  private def loadInstaller(args: CLIArguments, platform: Platform) =
    new Installer(args.systemPath.get, args.userPath.get, platform)

  private def cmd_unknown(args: CLIArguments, platform: Platform) = sys.error("No command given!")
  private def cmd_status(args: CLIArguments, platform: Platform) = {
    val installer = loadInstaller(args, platform)
    System.out.println("Patch status: "+installer.patchInstaller.checkPatchStatus())
  }
  private def cmd_update(args: CLIArguments, platform: Platform) = {
    val installer = loadInstaller(args, platform)
    if(!args.force) installer.patchInstaller.safeUpdate(args.debug)
    else sys.error("force update not yet implemented")
  }

  def executeCommand(args: Seq[String]) = {
    val platform = Platform.currentPlatform.getOrElse(sys.error("Unknown platform."))
    val systemPath = resolvePaths(platform.defaultSystemPaths)
    val userPath   = resolvePaths(platform.defaultUserPaths)
    parser.parse(args, CLIArguments(cmd_unknown, systemPath, userPath)) match {
      case Some(command) => command.command(command, platform)
      case None => System.exit(-1)
    }
  }
}