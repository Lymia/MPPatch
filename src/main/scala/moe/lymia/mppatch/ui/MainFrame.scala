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

import java.awt.{GridBagConstraints, GridBagLayout}
import java.nio.file.{Files, Path, Paths}
import java.util.Locale
import javax.swing.*
import moe.lymia.mppatch.core.*
import moe.lymia.mppatch.util.Steam
import moe.lymia.mppatch.util.io.{DataSource, ResourceDataSource}

class MainFrame(val locale: Locale) extends FrameBase[JFrame] {
  protected var installButton: ActionButton   = _
  protected var uninstallButton: ActionButton = _
  protected var currentVersion: JTextField    = _
  protected var targetVersion: JTextField     = _
  protected var currentStatus: JTextField     = _

  private val installationManager = new InstallationManager()

  private val syncLock                          = new Object
  private var patchPackage: PatchPackage        = _
  private var isUserChange                      = false
  private var isValid                           = false
  private var installer: Option[PatchInstaller] = None

  private def getInstallerUnsafe = installer.getOrElse(sys.error("installer does not exist"))
  private def installation       = new Installation(getInstallerUnsafe.basePath)
  private def packages           = installation.config.packages

  def getPatch     = patchPackage
  def getInstaller = installer
  def changeInstaller(path: Path, changeByUser: Boolean = true): Unit = syncLock synchronized {
    log.info(s"Changing installer path to ${path.toString}")
    val installPlatform = patchPackage.detectInstallationPlatform(path).get
    val instance        = new PatchInstaller(path, installPlatform, MPPatchInstaller.platform)
    isUserChange = changeByUser
    if (Files.exists(path)) {
      isValid = new Installation(path).isValid(patchPackage)
      installer.foreach(_.releaseLock())
      if (isValid) {
        instance.acquireLock()
        if (changeByUser) installationManager.addDirectory(path)
      }
      installer = Some(instance)
    } else {
      log.warn("- Directory does not exist!")
      installer = None
    }
  }
  def reloadInstaller() = syncLock synchronized {
    installer.foreach(i => changeInstaller(i.basePath))
  }
  def changePatchPackage(pack: DataSource) = syncLock synchronized {
    log.info("Changing patch package...")
    patchPackage = new PatchPackage(pack)
    reloadInstaller()
    installationManager.patchPackage = patchPackage
  }

  changePatchPackage(ResourceDataSource("builtin_patch"))
  changeInstaller(installationManager.installations.head.rootDir, changeByUser = false)

  private var lastPatchStatus: Option[PatchStatus] = _
  private def checkPatchStatus() = {
    val flag = lastPatchStatus match {
      case Some(x) => installer.fold(false)(_.checkPatchStatus(packages) == x)
      case None    => false
    }
    if (!flag) warn("error.statuschanged")
    flag
  }
  private val actionUpdate = () =>
    if (checkPatchStatus()) {
      getInstallerUnsafe.safeUpdate(packages)
      true
    } else false
  private val actionUninstall = () =>
    if (checkPatchStatus()) {
      getInstallerUnsafe.safeUninstall()
      true
    } else false
  private def actionValidate0() = {
    val ret = JOptionPane.showConfirmDialog(frame, i18n("validate.confirm"), titleString, JOptionPane.YES_NO_OPTION)
    if (ret == JOptionPane.OK_OPTION) {
      getInstallerUnsafe.cleanupPatch()
      Steam.validateGameFiles(getInstallerUnsafe.install.script.steamId)
      JOptionPane.showMessageDialog(frame, i18n("validate.wait"), titleString, JOptionPane.INFORMATION_MESSAGE)
      reloadInstaller()
    }
  }
  private val actionValidate = () =>
    if (checkPatchStatus()) {
      actionValidate0()
      true
    } else false
  private val actionCleanup = () =>
    if (checkPatchStatus()) {
      getInstallerUnsafe.cleanupPatch()
      getInstallerUnsafe.checkPatchStatus(packages) match {
        case PatchStatus.NeedsValidation | PatchStatus.NotInstalled(false) =>
          actionValidate0()
        case _ =>
        // ignored
      }
      true
    } else false

  protected def buildForm() = {
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
    installButton = new ActionButton()
    frame.add(installButton, constraints(gridx = 0, gridy = 1, weightx = 0.5, fill = GridBagConstraints.BOTH))

    uninstallButton = new ActionButton()
    frame.add(uninstallButton, constraints(gridx = 1, gridy = 1, weightx = 0.5, fill = GridBagConstraints.BOTH))

    frame.iconButton(2, 1, "settings")(new SettingsDialog(locale, this).showForm())
    frame.iconButton(3, 1, "about")(new AboutDialog(locale, this).showForm())
  }

  private def setStatus(text: String) = currentStatus.setText(i18n(text))
  override def update() = {
    installButton.setEnabled(false)
    installButton.setStatusAction("action.install", actionUpdate)

    uninstallButton.setEnabled(false)
    uninstallButton.setStatusAction("action.uninstall", actionUninstall)

    targetVersion.setText(patchPackage.patchManifest.patchVersion)

    lastPatchStatus = None

    installer match {
      case None =>
        currentVersion.setText(i18n("status.dir.noversion"))
        setStatus(if (isUserChange) "status.doesnotexist" else "status.cannotfind")
      case Some(installer) =>
        currentVersion.setText(installer.installedVersion.fold(i18n("status.dir.noversion"))(identity))

        if (!isValid) {
          setStatus("status.noprogram")
          uninstallButton.setAction("action.validate", () => actionValidate0())
          uninstallButton.setEnabled(true)
        } else if (!installer.isLockAcquired) setStatus("status.inuse")
        else {
          val status = installer.checkPatchStatus(packages)
          lastPatchStatus = Some(status)
          status match {
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
              setStatus(if (installer.isDowngrade) "status.candowngrade" else "status.needsupdate")
              installButton.setActionText(if (installer.isDowngrade) "action.downgrade" else "action.update")
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
            case PatchStatus.FilesValidated =>
              setStatus("status.filesvalidated")
              installButton.setActionText("action.reinstall")
              installButton.setEnabled(true)
              uninstallButton.setEnabled(true)
            case PatchStatus.TargetUpdated =>
              setStatus("status.targetupdated")
              installButton.setActionText("action.update")
              installButton.setEnabled(true)
              uninstallButton.setEnabled(true)
            case PatchStatus.UnknownUpdate =>
              setStatus("status.unknownupdate")
              uninstallButton.setActionText("action.cleanup")
              uninstallButton.setEnabled(true)
            case PatchStatus.NeedsCleanup =>
              setStatus("status.needscleanup")
              uninstallButton.setStatusAction("action.cleanup", actionCleanup)
              uninstallButton.setEnabled(true)
            case PatchStatus.NeedsValidation | PatchStatus.NotInstalled(false) =>
              setStatus("status.needsvalidation")
              uninstallButton.setStatusAction("action.validate", actionValidate)
              uninstallButton.setEnabled(true)
            case x => setStatus("unknown state: " + x)
          }
        }
    }
  }
}
