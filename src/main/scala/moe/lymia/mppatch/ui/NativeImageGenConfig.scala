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
import java.awt.Desktop
import java.util.Locale
import javax.swing.{JFileChooser, JOptionPane}

object NativeImageGenConfig {
  private val sleepDur = 1000

  private def forLocale(locale: Locale): Unit = {
    class TestFrame(locale: Locale) extends LegacyMainFrame(locale) {
      override protected val neverShowMessage: Boolean = true
      def doInstallUninstall(): Unit = {
        if (uninstallButton.isEnabled) uninstallButton.doClick()
        Thread.sleep(sleepDur)
        installButton.doClick()
        Thread.sleep(sleepDur)
        installButton.doClick()
        Thread.sleep(sleepDur)
        uninstallButton.doClick()
        Thread.sleep(sleepDur)
      }
    }

    val mainFrame = new TestFrame(locale)
    new Thread(() => mainFrame.showForm()).start()
    Thread.sleep(sleepDur)

    // about dialog
    val aboutDialog = new AboutDialog(locale, mainFrame)
    new Thread(() => aboutDialog.showForm()).start()
    Thread.sleep(sleepDur)
    aboutDialog.forceClose()

    // settings dialog
    val settingsDialog = new SettingsDialog(locale, mainFrame)
    new Thread(() => settingsDialog.showForm()).start()
    Thread.sleep(sleepDur)
    settingsDialog.forceClose()

    // test install
    mainFrame.doInstallUninstall()

    // clean up
    mainFrame.forceClose()
  }

  def genericFeatures(): Unit = {
    // show a file chooser
    val chooser = new JFileChooser()
    new Thread(() => chooser.setVisible(true))
    Thread.sleep(sleepDur)
    chooser.setVisible(false)

    // desktop open
    Desktop.getDesktop.open(MPPatchInstaller.logFile)
  }

  def run(): Unit = {
    forLocale(Locale.US)
    genericFeatures()
  }
}
