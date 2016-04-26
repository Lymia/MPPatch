package moe.lymia.multiverse.core

import java.nio.file.Path

import moe.lymia.multiverse.platform.Platform

class Installer(systemPath: Path, userPath: Path, platform: Platform) {
  val patchInstaller = new PatchInstaller(systemPath, platform)
}

object PathNames {
  val PATCH_STATE_FILENAME = "mvmm_patch_state.json"
  val PATCH_LOCK_FILENAME  = ".mvmm_patch_dirty"
}
