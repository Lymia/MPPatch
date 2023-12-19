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

import moe.lymia.mppatch.ui.InstallationConfiguration.*
import moe.lymia.mppatch.util.Crypto
import play.api.libs.json.*
import play.api.libs.json.Reads.*
import play.api.libs.json.Writes.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}
import java.util.Locale
import javax.swing.JFrame
import scala.collection.mutable

object ConfigurationStore extends LaunchFrameError {
  private val prefs = java.util.prefs.Preferences.userNodeForPackage(getClass)
  class ConfigKey[T: Reads: Format](val name: String, default: => T) {
    def this(name: String) = this(name, sys.error("preference " + name + " not set"))

    protected def encode(v: T): String = Json.stringify(Json.toJson(v))
    protected def decode(s: String): T = Json.fromJson(Json.parse(s)).get

    def hasValue = prefs.get(name, null) != null
    def clear()  = prefs.remove(name)
    def valueOption = try {
      val pref = prefs.get(name, null)
      if (pref == null) None else Some(decode(pref))
    } catch {
      case _: Exception => None
    }
    def value         = valueOption.fold(default)(identity)
    def value_=(t: T) = prefs.put(name, encode(t))
  }
  private class RawStringConfigKey(name: String) extends ConfigKey[String](name) {
    override protected def encode(v: String): String = v
    override protected def decode(s: String): String = s
  }

  private val configVersion = new ConfigKey[Int]("installer.config_ver")
  val installationDirs      = new ConfigKey[Set[String]]("installer.v1.installation_dirs", Set())
  val suppressedDirs        = new ConfigKey[Set[String]]("installer.v1.suppressed_dirs", Set())
  def installationConf(path: Path) = {
    val canonical = Crypto.sha256_b64(path.toRealPath().toString.getBytes(StandardCharsets.UTF_8))
    new ConfigKey[InstallationConfiguration](s"installer.v1.conf|$canonical", InstallationConfiguration.default)
  }

  private val legacyInstallationDirectory  = new RawStringConfigKey("installationDirectory")
  private val legacyEnableDebug            = new ConfigKey("enableDebug", false)
  private val legacyEnableLogging          = new ConfigKey("enableLogging", true)
  private val legacyEnableMultiplayerPatch = new ConfigKey("enableMultiplayerPatch", true)
  private val legacyEnableLuaJIT           = new ConfigKey("enableLuaJIT", true)

  private def hasLegacyValues: Boolean =
    legacyInstallationDirectory.hasValue || legacyEnableDebug.hasValue || legacyEnableLogging.hasValue ||
      legacyEnableMultiplayerPatch.hasValue || legacyEnableLuaJIT.hasValue
  def updatePreferences(findDefaultDirectory: => Option[Path]): Unit =
    if (!configVersion.hasValue && hasLegacyValues) {
      configVersion.value = 1
      val defaultDirectory = legacyInstallationDirectory.valueOption match {
        case Some(x) =>
          val realPath = Paths.get(x).toRealPath().toString
          installationDirs.value = installationDirs.value + realPath
          Some(Paths.get(x))
        case None =>
          val _ = installationDirs.value // make sure the key exists, but don't do anything else
          findDefaultDirectory
      }
      defaultDirectory match {
        case Some(defaultDirectory) =>
          val seq = mutable.HashSet.empty[String]
          if (legacyEnableDebug.value) seq.add("debug")
          if (legacyEnableLogging.value) seq.add("logging")
          if (legacyEnableMultiplayerPatch.value) seq.add("multiplayer")
          if (legacyEnableLuaJIT.value) seq.add("luajit")
          installationConf(defaultDirectory).value = InstallationConfiguration(
            isManuallyAdded = legacyInstallationDirectory.hasValue,
            packages = seq.toSet
          )

          legacyInstallationDirectory.clear()
          legacyEnableDebug.clear()
          legacyEnableLogging.clear()
          legacyEnableMultiplayerPatch.clear()
          legacyEnableLuaJIT.clear()
        case _ =>
      }
    } else if (configVersion.hasValue && configVersion.value != 1) {
      if (confirmDialog("error.configVersionTooNew")) {
        configVersion.value = 1
      } else {
        throw new InstallerException("Cancelling configuration downgrade", null)
      }
    } else {
      configVersion.value = 1
    }
}
