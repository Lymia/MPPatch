package moe.lymia.multiverse.ui

import moe.lymia.multiverse.data.VersionInfo

case class CLIArguments(command: CLIArguments => Int)

object CLI {
  val parser = new scopt.OptionParser[CLIArguments]("mvmm") {
    head("Multiverse Mod Manager", "v"+VersionInfo.versionString)
  }

  def executeCommand(args: Seq[String]) = {
    parser.parse(args, CLIArguments(_ => { println("No command given!"); -1 })) match {
      case Some(command) =>
        val ret = command.command(command)
        if(ret != 0) System.exit(ret)
      case None => System.exit(-1)
    }
  }
}