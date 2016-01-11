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

package moe.lymia.multiverse.platform

import java.nio.file.{Files, Path, Paths}
import javax.swing.JFileChooser

import moe.lymia.multiverse.installer.PatchPlatformInfo
import moe.lymia.multiverse.util.{Steam, WindowsRegistry}

object Win32Platform extends Platform {
  val platformName = "win32"

  override def defaultSystemPaths: Seq[Path] =
    WindowsRegistry.HKEY_CURRENT_USER("Software\\Valve\\Steam", "SteamPath").toSeq.flatMap(
      x => Steam.loadLibraryFolders(Paths.get(x))).map(_.resolve("SteamApps\\common\\Sid Meier's Civilization V")) ++
    WindowsRegistry.HKEY_CURRENT_USER("Software\\Firaxis\\Civilization5", "LastKnownPath").map(x => Paths.get(x)).toSeq
  override def defaultUserPaths  : Seq[Path] = {
    val regPath =
      WindowsRegistry.HKEY_CURRENT_USER("Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders",
        "Personal").map(x => Paths.get(x, "My Games", "Sid Meier's Civilization 5")).filter(x => Files.exists(x))
    Seq(new JFileChooser().getFileSystemView.getDefaultDirectory.toPath.
      resolve("\"My Games\\\\Sid Meier's Civilization 5\"")) ++ regPath
  }

  override def assetsPath = "Assets"

  def normalizeLineEndings(name: String) = name.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n")

  def patchInfo = ???
}
