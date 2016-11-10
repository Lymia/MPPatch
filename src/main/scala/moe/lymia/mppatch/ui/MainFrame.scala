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

package moe.lymia.mppatch.ui

import java.awt.{GridBagConstraints, GridBagLayout}
import java.nio.file.{Files, Path, Paths}
import java.util.Locale
import javax.swing._

import moe.lymia.mppatch.core._
import moe.lymia.mppatch.util.io.{DataSource, MppakDataSource}

class MainFrame(val locale: Locale) extends FrameBase[JFrame] {
  private var installButton  : ActionButton = _
  private var uninstallButton: ActionButton = _
  private var currentVersion : JTextField   = _
  private var targetVersion  : JTextField   = _
  private var currentStatus  : JTextField   = _

  private def packages = {
    val debug       = if(Preferences.enableDebug.value           ) Set("debug")       else Set[String]()
    val multiplayer = if(Preferences.enableMultiplayerPatch.value) Set("multiplayer") else Set[String]()
    val luajit      = if(Preferences.enableLuaJIT.value          ) Set("luajit")      else Set[String]()
    debug ++ multiplayer ++ luajit
  }

  private val platform  = Platform.currentPlatform.getOrElse(error(i18n("error.unknownplatform")))
  private def checkPath(path: Path) =
    Files.exists(path) && Files.isDirectory(path) &&
      patchPackage.script.checkFor.forall(x => Files.exists(path.resolve(x)))
  private def resolvePaths(paths: Seq[Path]) = paths.find(checkPath)

  private val syncLock = new Object
  private var patchPackage: PatchLoader = _
  private var isUserChange = false
  private var isValid = false
  private var installer: PatchInstaller = _
  def getPatch = patchPackage
  def getInstaller = installer
  def changeInstaller(path: Path, changeByUser: Boolean = true): Unit = syncLock synchronized {
    val instance = new PatchInstaller(path, patchPackage, platform)
    isUserChange = changeByUser
    if(Files.exists(path)) {
      isValid = checkPath(path)
      if(installer != null) installer.releaseLock()
      if(isValid) {
        instance.acquireLock()
        if(changeByUser) Preferences.installationDirectory.value = path.toFile.toString
      }
      installer = instance
    } else installer = null
  }
  def reloadInstaller() = syncLock synchronized {
    if(installer != null) changeInstaller(installer.basePath)
  }
  def changePatchPackage(pack: DataSource) = syncLock synchronized {
    patchPackage = new PatchLoader(pack, platform)
    reloadInstaller()
  }

  changePatchPackage(MppakDataSource("mppatch.mppak"))

  private def pathFromRegistry() = resolvePaths(platform.defaultSystemPaths) match {
    case Some(x) => changeInstaller(x, false)
    case None =>
  }
  if(Preferences.installationDirectory.hasValue) {
    val configPath = Paths.get(Preferences.installationDirectory.value)
    if(checkPath(configPath)) changeInstaller(configPath, false)
    else {
      Preferences.installationDirectory.clear()
      pathFromRegistry()
    }
  } else pathFromRegistry()

  private val actionUpdate = () => {
    installer.safeUpdate(packages)
  }
  private val actionUninstall = () => {
    installer.safeUninstall()
  }
  private val actionCleanup = () => {
    installer.cleanupPatch()
  }

  protected def buildForm() {
    frame = new JFrame(titleString)
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    frame.setLayout(new GridBagLayout())

    // Status section
    frame.subFrame(constraints(gridwidth = 4, fill = GridBagConstraints.BOTH)) { statusPane =>
      targetVersion = statusPane.gridTextRow(0, "target")
      currentVersion = statusPane.gridTextRow(1, "installed")
      currentStatus = statusPane.gridTextRow(2, "status")
    }

    // Button section
    installButton   = new ActionButton()
    frame.add(installButton  , constraints(gridx = 0, gridy = 1, weightx = 0.5, fill = GridBagConstraints.BOTH))

    uninstallButton = new ActionButton()
    frame.add(uninstallButton, constraints(gridx = 1, gridy = 1, weightx = 0.5, fill = GridBagConstraints.BOTH))

    frame.iconButton(2, 1, "settings") { new SettingsDialog(locale, this).showForm() }
    frame.iconButton(3, 1, "about"   ) { new AboutDialog   (locale, this).showForm() }
  }

  private def setStatus(text: String) = currentStatus.setText(i18n(text))
  override def update() = {
    installButton.setEnabled(false)
    installButton.setAction("action.install", actionUpdate)

    uninstallButton.setEnabled(false)
    uninstallButton.setAction("action.uninstall", actionUninstall)

    targetVersion.setText(patchPackage.data.patchVersion)

    installer match {
      case null =>
        currentVersion.setText(i18n("status.dir.noversion"))
        setStatus(if(isUserChange) "status.doesnotexist" else "status.cannotfind")
      case _ =>
        currentVersion.setText(installer.installedVersion.fold(i18n("status.dir.noversion"))(identity))

        if(!isValid) setStatus("status.noprogram")
        else if(!installer.isLockAcquired) setStatus("status.inuse")
        else installer.checkPatchStatus(packages) match {
          case PatchStatus.Installed =>
            setStatus("status.ready")
            installButton.setActionText("action.reinstall")
            installButton.setEnabled(true)
            uninstallButton.setEnabled(true)
          case PatchStatus.PackageChange =>
            setStatus("status.settingchange")
            installButton.setActionText("action.reinstall")
            installButton.setEnabled(true)
            uninstallButton.setEnabled(true)
          case PatchStatus.NeedsUpdate =>
            setStatus(if(installer.isDowngrade) "status.candowngrade" else "status.needsupdate")
            installButton.setActionText(if(installer.isDowngrade) "action.downgrade" else "action.update")
            installButton.setEnabled(true)
            uninstallButton.setEnabled(true)
          case PatchStatus.FilesCorrupted =>
            setStatus("status.filescorrupted")
            installButton.setActionText("action.repair")
            installButton.setEnabled(true)
            uninstallButton.setEnabled(true)
          case PatchStatus.NotInstalled(true) =>
            setStatus("status.notinstalled")
            installButton.setEnabled(true)
          case PatchStatus.NotInstalled(false) =>
            setStatus("status.unknownversion")
          case x => setStatus("unknown state: "+x)
        }
    }
  }
}