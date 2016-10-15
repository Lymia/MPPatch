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

import java.awt.event.ActionEvent
import java.awt.{Dimension, GridBagConstraints, GridBagLayout}
import java.util.Locale
import javax.swing._

import moe.lymia.mppatch.core.{PatchInstaller, PatchStatus}
import moe.lymia.mppatch.platform.Platform
import moe.lymia.mppatch.util.IOUtils
import moe.lymia.mppatch.util.res.I18N

class GUI(locale: Locale) {
  val i18n = I18N(locale)
  val frame = new JFrame()

  def error[T](string: String): T = {
    JOptionPane.showMessageDialog(if(frame.isVisible) frame else null, string,
                                  i18n("common.title"), JOptionPane.ERROR_MESSAGE)
    frame.setVisible(false)
    frame.dispose()
    sys.error(string)
  }
  def dumpException[T](errorString: String, e: Exception, exArgs: Object*): T = {
    e.printStackTrace()
    error(i18n(errorString, (e.getClass+": "+e.getMessage) +: exArgs : _*))
  }

  val platform  = Platform.currentPlatform.getOrElse(error(i18n("gui.unknownplatform")))
  val installer = resolvePaths(platform.defaultSystemPaths) match {
    case Some(x) => new PatchInstaller(x, platform)
    case None    =>
      // TODO: Allow user selection
      error("system path could not be found")
  }

  def actionUpdate(): Unit = {
    installer.safeUpdate(true)
  }
  def actionUninstall(): Unit = {
    installer.safeUninstall()
  }
  def actionCleanup(): Unit = {
    installer.cleanupPatch()
  }

  // initialize frame
  frame.setTitle(i18n("common.title"))
  frame.setLayout(new GridBagLayout())

  val statusPane = new JPanel()
  statusPane.setLayout(new GridBagLayout)

  var constraints = new GridBagConstraints
  val label = new JLabel()
  label.setText(i18n("gui.status"))
  constraints.ipadx = 3
  constraints.ipady = 3
  statusPane.add(label, constraints)

  constraints = new GridBagConstraints()
  val currentStatus = new JTextField()
  def setStatus(text: String) = currentStatus.setText(i18n(text))
  currentStatus.setEditable(false)
  currentStatus.setPreferredSize(new Dimension(450, currentStatus.getPreferredSize.getHeight.toInt))
  constraints.gridx = 1
  constraints.weightx = 1
  constraints.ipadx = 3
  constraints.ipady = 3
  constraints.fill = GridBagConstraints.BOTH
  statusPane.add(currentStatus, constraints)

  constraints = new GridBagConstraints()
  constraints.gridwidth = 2
  frame.add(statusPane, constraints)

  class ActionButton() extends JButton {
    var action: () => Unit = () => error("no action registered")
    var text  : String     = "<no action>"

    setAction(new AbstractAction() {
      override def actionPerformed(e: ActionEvent): Unit = {
        val cont = i18n(text+".continuous")
        try {
          action()
          JOptionPane.showMessageDialog(frame, i18n(text+".completed"))
        } catch {
          case e: Exception => dumpException("gui.commandfailed", e, i18n(text+".continuous"))
        }
        updateFn()
      }
    })

    def setActionText(name: String): Unit = {
      text = name
      setText(i18n(name))
    }
    def setAction(name: String, action: () => Unit): Unit = {
      this.action = action
      setActionText(name)
    }
  }

  constraints = new GridBagConstraints()
  constraints.gridy = 1
  constraints.weightx = 0.5
  constraints.fill = GridBagConstraints.BOTH

  val installButton = new ActionButton()
  constraints.gridx = 0
  frame.add(installButton, constraints)

  val uninstallButton = new ActionButton()
  constraints.gridx = 1
  frame.add(uninstallButton, constraints)

  def updateFn() = {
    installButton.setEnabled(false)
    installButton.setAction("gui.action.install", actionUpdate)

    uninstallButton.setEnabled(false)
    uninstallButton.setAction("gui.action.uninstall", actionUninstall)

    installer.checkPatchStatus() match {
      case PatchStatus.Installed(_) =>
        setStatus("gui.status.ready")
        installButton.setActionText("gui.action.reinstall")
        installButton.setEnabled(true)
        uninstallButton.setEnabled(true)
      case PatchStatus.NeedsUpdate =>
        setStatus("gui.status.needsupdate")
        installButton.setActionText("gui.action.update")
        installButton.setEnabled(true)
        uninstallButton.setEnabled(true)
      case PatchStatus.NotInstalled(true) =>
        setStatus("gui.status.notinstalled")
        installButton.setEnabled(true)
      case PatchStatus.NotInstalled(false) =>
        setStatus("gui.status.unknownversion")
      case x => setStatus("unknown state: "+x)
    }
  }
  updateFn()

  frame.pack()
  frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
  frame.setLocationRelativeTo(null)

  // show loop
  def show(): Unit = try {
    val lockFile = installer.resolve(".mppatch_installer_gui_lock")
    var continueLoop = true
    def lockLoop() =
      IOUtils.withLock(lockFile, error = {
        val doOverride = JOptionPane.showConfirmDialog(frame, i18n("gui.overridelock"),
                                                     i18n("common.title"), JOptionPane.YES_NO_OPTION)
        if(doOverride == JOptionPane.OK_OPTION) continueLoop = true
      }) {
        frame.setVisible(true)
        while(frame.isVisible) try {
          Thread.sleep(1000)
        } catch {
          case _: InterruptedException => // ignored
        }
      }
    while(continueLoop) {
      continueLoop = false
      lockLoop()
    }
    if(frame.isDisplayable) frame.dispose()
  } catch {
    case e: Exception => dumpException("gui.genericerror", e)
  }
}