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

import moe.lymia.mppatch.core.{PatchPackage, Platform, PlatformType}
import moe.lymia.mppatch.util.io.DataSource
import play.api.libs.json.{Json, OFormat}

import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable

case class InstallationConfiguration(
    isManuallyAdded: Boolean,
    packages: Set[String]
)
object InstallationConfiguration {
  val default = InstallationConfiguration(
    isManuallyAdded = false,
    packages = Set("logging", "luajit", "multiplayer")
  )
  implicit val jsonFormat: OFormat[InstallationConfiguration] = Json.format[InstallationConfiguration]
}

class Installation(val rootDir: Path) {
  def config                                      = ConfigurationStore.installationConf(rootDir).value
  def config_=(config: InstallationConfiguration) = ConfigurationStore.installationConf(rootDir).value = config

  def isValid(pkg: PatchPackage) =
    Files.exists(rootDir) && Files.isDirectory(rootDir) && pkg.detectInstallationPlatform(rootDir).isDefined
}

class InstallationManager {
  private var installationDirList = Seq[Installation]()
  private var patchPackage0       = new PatchPackage(MPPatchInstaller.defaultPackageSource)

  def installations = installationDirList
  def patchPackage  = patchPackage0
  def patchPackage_=(v: PatchPackage) = {
    patchPackage0 = v
    reset()
  }

  private def reset(): Unit = {
    val detectedPaths = MPPatchInstaller.platform.defaultSystemPaths
    val paths         = ConfigurationStore.installationDirs.value.map(x => Paths.get(x))

    val installationDirs = mutable.HashMap[String, Installation]()
    for (path <- detectedPaths if patchPackage0.detectInstallationPlatform(path).isDefined)
      installationDirs(path.toRealPath().toString) = new Installation(path)
    for (path <- paths) installationDirs(path.toRealPath().toString) = new Installation(path)

    for (dir <- ConfigurationStore.suppressedDirs.value) installationDirs.remove(dir)

    ConfigurationStore.installationDirs.value = installationDirs.filter(x => x._2.config.isManuallyAdded).keys.toSet
    installationDirList = installationDirs.keys.toSeq.sorted.map(x => installationDirs(x))
  }
  def setPatchPackage(source: DataSource): Unit = {
    patchPackage0 = new PatchPackage(source)
    reset()
  }
  reset()

  def addDirectory(dir: Path) = {
    val canonical = dir.toRealPath().toString

    ConfigurationStore.suppressedDirs.value = ConfigurationStore.suppressedDirs.value - canonical
    ConfigurationStore.installationDirs.value = ConfigurationStore.installationDirs.value + canonical

    val key = ConfigurationStore.installationConf(dir)
    key.value = key.value.copy(isManuallyAdded = true)

    reset()
  }
  def removeDirectory(dir: Path) = {
    val canonical = dir.toRealPath().toString

    ConfigurationStore.suppressedDirs.value = ConfigurationStore.suppressedDirs.value + canonical
    ConfigurationStore.installationDirs.value = ConfigurationStore.installationDirs.value - canonical

    val key = ConfigurationStore.installationConf(dir)
    key.value = key.value.copy(isManuallyAdded = false)

    reset()
  }
}
