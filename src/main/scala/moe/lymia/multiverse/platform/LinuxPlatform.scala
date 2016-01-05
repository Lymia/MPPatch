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

import java.nio.file.{Paths, Path}
import java.util.Locale

object LinuxPlatform extends Platform {
  private val home = Paths.get(System.getProperty("user.home"))
  def defaultSystemPaths: Seq[Path] =
    loadSteamLibraryFolders(home.resolve(".steam/steam")).map(_.resolve("steamapps/common/Sid Meier's Civilization V"))
  def defaultUserPaths  : Seq[Path] =
    Seq(home.resolve(".local/share/Aspyr/Sid Meier's Civilization 5"))

  def assetsPath = "steamassets/assets"
  override def mapPath(name: String): String = name.replace("\\", "/").toLowerCase(Locale.ENGLISH)

  def normalizeLineEndings(name: String) = name.replace("\r\n", "\n").replace("\r", "\n")
}
