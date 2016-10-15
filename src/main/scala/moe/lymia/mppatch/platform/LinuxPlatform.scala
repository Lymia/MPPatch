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

package moe.lymia.mppatch.platform

import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}
import java.util.Locale

import moe.lymia.mppatch.core.{PatchInstalledFile, PatchPlatformInfo}
import moe.lymia.mppatch.util.Steam

object LinuxPlatform extends Platform {
  val platformName = "linux"

  private val home = Paths.get(System.getProperty("user.home"))
  def defaultSystemPaths: Seq[Path] =
    Steam.loadLibraryFolders(home.resolve(".steam/steam")).map(_.resolve("steamapps/common/Sid Meier's Civilization V"))

  def assetsPath = "steamassets/assets"
  override def mapPath(name: String): String = name.replace("\\", "/").toLowerCase(Locale.ENGLISH)

  def normalizeLineEndings(name: String) = name.replace("\r\n", "\n").replace("\r", "\n")

  private def shellScript(name: String, text: String) =
    PatchInstalledFile(name, ("#!/bin/bash\n"+text).getBytes(StandardCharsets.UTF_8), executable = true)

  def patchInfo = new PatchPlatformInfo {
    val replacementTarget = "Civ5XP"
    def replacementNewName(versionName: String) = s"Civ5XP.orig.$versionName"
    def patchInstallTarget(versionName: String) = s"mppatch_patch_$versionName.so"

    def patchReplacementFile(versionName: String) =
      Some(shellScript(replacementTarget,
        "version=\""+versionName+"\"\n"+
        """path="`dirname "$0"`"
          |export LD_LIBRARY_PATH="$path:$LD_LIBRARY_PATH"
          |export LD_PRELOAD="mppatch_patch_$version.so"
          |exec -a Civ5XP "$path/Civ5XP.orig.$version" $*
          |""".stripMargin))

    def additionalFiles(versionName: String) = Seq(
      shellScript("Civ5XP.launch",
        "version=\""+versionName+"\"\n"+
        """path="`dirname "$0"`"
          |export LD_PRELOAD="$path/mppatch_patch_$version.so"
          |export SteamAppId=8930
          |export LD_LIBRARY_PATH="$HOME/.steam/bin32/steam-runtime/i386/lib/i386-linux-gnu/:$HOME/.steam/bin32/steam-runtime/i386/usr/lib/i386-linux-gnu/"
          |exec -a Civ5XP "$path/Civ5XP.orig.$version" $*
          |""".stripMargin),
      shellScript("Civ5XP.launch.dbg",
        "version=\""+versionName+"\"\n"+
        """path="`dirname "$0"`"
          |export SteamAppId=8930
          |export LD_LIBRARY_PATH="$HOME/.steam/bin32/steam-runtime/i386/lib/i386-linux-gnu/:$HOME/.steam/bin32/steam-runtime/i386/usr/lib/i386-linux-gnu/"
          |gdb --init-eval-command="set env LD_PRELOAD=$path/mppatch_patch_$version.so" --args "$path/Civ5XP.orig.$version" $*
          |""".stripMargin)
    )

    def findInstalledFiles(list: Seq[String]) = list.filter(x =>
      x == "Civ5XP.launch" || x == "Civ5XP.launch.dbg" ||
      x.matches("Civ5XP\\.orig\\.[0-9a-f]+") ||
      x.matches("mppatch_patch_[0-9a-f]+\\.so")
    )
  }
}
