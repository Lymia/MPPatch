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

import java.awt.{Dimension, Font, GridBagConstraints, GridBagLayout}
import java.nio.file.{Files, Path, Paths}
import java.util.Locale
import javax.swing._

import moe.lymia.mppatch.core._
import moe.lymia.mppatch.util.{IOUtils, VersionInfo}

class MainFrame(val locale: Locale) extends FrameBase[JFrame] {
  private var installButton  : ActionButton = _
  private var uninstallButton: ActionButton = _
  private var installPath    : JTextField   = _
  private var currentVersion : JTextField   = _
  private var targetVersion  : JTextField   = _
  private var currentStatus  : JTextField   = _

  private val platform  = Platform.currentPlatform.getOrElse(error(i18n("error.unknownplatform")))
  private def checkPath(path: Path) =
    Files.exists(path) && Files.isDirectory(path) && platform.checkPaths.forall(x => Files.exists(path.resolve(x)))
  private def resolvePaths(paths: Seq[Path]) = paths.find(checkPath)

  private val syncLock = new Object
  private var patchPackage: PatchLoader = _
  private var isValid = false
  private var installer: PatchInstaller = _
  private def changeInstaller(path: Path, changeByUser: Boolean = true): Unit = syncLock synchronized {
    val instance = new PatchInstaller(path, patchPackage, platform)
    isValid = checkPath(path)
    if(installer != null) installer.releaseLock()
    if(isValid) {
      instance.acquireLock()
      if(changeByUser) Preferences.installationDirectory.value = path.toFile.toString
    }
    installer = instance
  }
  private def reloadInstaller() = syncLock synchronized {
    if(installer != null) changeInstaller(installer.basePath)
  }
  private def changePatchPackage(pack: PatchFileSource) = syncLock synchronized {
    patchPackage = new PatchLoader(pack)
    reloadInstaller()
  }

  changePatchPackage(PatchPackageLoader("mppatch.mppak"))

  private def pathFromRegistry() = resolvePaths(platform.defaultSystemPaths) match {
    case Some(x) => changeInstaller(x, false)
    case None =>
  }
  if(Preferences.installationDirectory.hasValue) {
    val configPath = Paths.get(Preferences.installationDirectory.value)
    println(configPath)
    if(checkPath(configPath)) changeInstaller(configPath)
    else {
      Preferences.installationDirectory.clear()
      pathFromRegistry()
    }
  } else pathFromRegistry()

  private def actionUpdate(): Unit = {
    installer.safeUpdate()
  }
  private def actionUninstall(): Unit = {
    installer.safeUninstall()
  }
  private def actionCleanup(): Unit = {
    installer.cleanupPatch()
  }

  private val symbolFont = Font.createFont(Font.TRUETYPE_FONT, IOUtils.getResource("text/Symbola_hint_subset.ttf"))
  private def symbolButton(button: JButton) = {
    val size = button.getMinimumSize
    if(size.getWidth < size.getHeight) {
      button.setMinimumSize  (new Dimension(size.getHeight.toInt, size.getHeight.toInt))
      button.setPreferredSize(new Dimension(size.getHeight.toInt, size.getHeight.toInt))
    }

    val font = button.getFont
    button.setFont(symbolFont.deriveFont(Font.PLAIN, font.getSize))

    button
  }

  protected def buildForm() {
    frame = new JFrame()
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)

    frame.setTitle(titleString)
    frame.setLayout(new GridBagLayout())

    // Status seciton
    val statusPane = new JPanel()
    statusPane.setLayout(new GridBagLayout())

    def gridLabel(row: Int, labelStr: String) = {
      val label = new JLabel()
      label.setText(i18n(s"label.$labelStr"))
      statusPane.add(label, constraints(gridy = row, ipadx = 3, ipady = 3,
                     insets = insets(left = 3, right = 4),
                     anchor = GridBagConstraints.LINE_START))
    }
    def gridTextField(row: Int, width: Int = 2) = {
      val textField = new JTextField()
      textField.setEditable(false)
      textField.setPreferredSize(new Dimension(450, textField.getPreferredSize.getHeight.toInt))
      statusPane.add(textField, constraints(gridx = 1, gridy = row, gridwidth = width, weightx = 1,
                                            ipadx = 3, ipady = 3, fill = GridBagConstraints.BOTH))
      textField
    }

    gridLabel(0, "path")
    installPath = gridTextField(0, 1)

    val browseButton = new JButton()
    browseButton.setAction(action { e =>
      val chooser = new JFileChooser()
      if(installer != null) chooser.setCurrentDirectory(installer.basePath.toFile)
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
      if(chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
        changeInstaller(chooser.getSelectedFile.toPath)
        update()
      }
    })
    browseButton.setText(i18n("icon.browse"))
    browseButton.setToolTipText(i18n("tooltip.browse"))
    symbolButton(browseButton)
    statusPane.add(browseButton, constraints(gridx = 2, gridy = 0, fill = GridBagConstraints.BOTH))

    gridLabel(1, "target")
    targetVersion = gridTextField(1)

    gridLabel(2, "installed")
    currentVersion = gridTextField(2)

    gridLabel(3, "status")
    currentStatus = gridTextField(3)

    frame.add(statusPane, constraints(gridwidth = 4, fill = GridBagConstraints.BOTH))

    // Button section
    installButton = new ActionButton()
    frame.add(installButton  , constraints(gridx = 0, gridy = 1, weightx = 0.5,
                                           fill = GridBagConstraints.BOTH))

    uninstallButton = new ActionButton()
    frame.add(uninstallButton, constraints(gridx = 1, gridy = 1, weightx = 0.5,
                                           fill = GridBagConstraints.BOTH))

    val settingsButton = new JButton()
    settingsButton.setAction(action { e => new SettingsDialog(locale, this).showForm() })
    settingsButton.setText(i18n("icon.settings"))
    settingsButton.setToolTipText(i18n("tooltip.settings"))
    symbolButton(settingsButton)
    frame.add(settingsButton, constraints(gridx = 2, gridy = 1,
                                          fill = GridBagConstraints.BOTH))

    val aboutButton = new JButton()
    aboutButton.setAction(action { e => new AboutDialog(locale, this).showForm() })
    aboutButton.setText(i18n("icon.about"))
    aboutButton.setToolTipText(i18n("tooltip.about"))
    symbolButton(aboutButton)
    frame.add(aboutButton, constraints(gridx = 3, gridy = 1,
                                          fill = GridBagConstraints.BOTH))
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
        installPath   .setText("")
        currentVersion.setText(i18n("status.dir.noversion"))
        setStatus("status.cannotfind")
      case _ =>
        installPath   .setText(installer.basePath.toString)
        currentVersion.setText(installer.installedVersion.fold(i18n("status.dir.noversion"))(identity))

        if(!isValid) setStatus("status.noprogram")
        else if(!installer.isLockAcquired) setStatus("status.inuse")
        else installer.checkPatchStatus() match {
          case PatchStatus.Installed =>
            setStatus("status.ready")
            installButton.setActionText("action.reinstall")
            installButton.setEnabled(true)
            uninstallButton.setEnabled(true)
          case PatchStatus.NeedsUpdate =>
            setStatus(if(installer.isDowngrade) "status.candowngrade" else "status.needsupdate")
            installButton.setActionText(if(installer.isDowngrade) "action.downgrade" else "action.update")
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