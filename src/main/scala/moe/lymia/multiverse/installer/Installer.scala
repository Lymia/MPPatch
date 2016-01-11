package moe.lymia.multiverse.installer

import java.nio.file.Path

import moe.lymia.multiverse.platform.Platform

class Installer(systemPath: Path, userPath: Path, platform: Platform) {
  val patchInstaller = new PatchInstaller(systemPath, platform)
}
