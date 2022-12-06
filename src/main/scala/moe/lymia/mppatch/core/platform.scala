/*
 * Copyright (c) 2015-2017 Lymia Alusyia <lymia@lymiahugs.com>
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

package moe.lymia.mppatch.core

import java.nio.file.{Path, Paths}
import java.util.Locale

import moe.lymia.mppatch.util.{SimpleLogger, Steam}
import moe.lymia.mppatch.util.win32.WindowsRegistry

import scala.annotation.tailrec

sealed trait PlatformType
object PlatformType {
  case object Win32 extends PlatformType
  case object MacOS extends PlatformType
  case object Linux extends PlatformType
  case object Other extends PlatformType

  lazy val currentPlatform = {
    val os = System.getProperty("os.name", "-").toLowerCase(Locale.ENGLISH)
         if(os.contains("windows")) Win32
    else if(os.contains("linux"  )) Linux
    else if(os.contains("mac"    ) ||
            os.contains("darwin" )) MacOS
    else                            Other
  }
}

trait Platform {
  val platformName: String

  def defaultSystemPaths: Seq[Path]
  def mapPath(name: String): String = name

  @tailrec final def resolve(path: Path, name: String*): Path =
    if(name.length == 1) path.resolve(mapPath(name.head))
    else resolve(path.resolve(mapPath(name.head)), name.tail: _*)
}
object Platform {
  def apply(t: PlatformType) = t match {
    case PlatformType.Win32 => Some(Win32Platform)
    case PlatformType.MacOS => Some(MacOSPlatform)
    case PlatformType.Linux => Some(LinuxPlatform)
    case _                  => None
  }

  lazy val currentPlatform = apply(PlatformType.currentPlatform)
}

object Win32Platform extends Platform {
  override val platformName = "win32"

  override def defaultSystemPaths: Seq[Path] = try {
    WindowsRegistry.HKLM("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 8930", "InstallLocation").toSeq.map(x => Paths.get(x))
  } catch {
    case e: Exception =>
      SimpleLogger.error("Could not load default system paths from registery.", e)
      Seq()
  }
}

object MacOSPlatform extends Platform {
  override val platformName = "macos"

  private val home = Paths.get(System.getProperty("user.home"))
  override def defaultSystemPaths: Seq[Path] =
    Steam.loadLibraryFolders(home.resolve("Library/Application Support/Steam")).map(
      _.resolve("steamapps/common/Sid Meier's Civilization V"))
}

object LinuxPlatform extends Platform {
  override val platformName = "linux"

  private val home = Paths.get(System.getProperty("user.home"))
  override def defaultSystemPaths: Seq[Path] =
    Steam.loadLibraryFolders(home.resolve(".steam/steam")).map(_.resolve("steamapps/common/Sid Meier's Civilization V"))
  override def mapPath(name: String): String = name.replace("\\", "/").toLowerCase(Locale.ENGLISH)
}

