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

import java.awt.{Desktop, GridBagConstraints, GridBagLayout}
import java.io.File
import java.nio.file.Paths
import java.util.Locale
import javax.swing._

class SettingsDialog(val locale: Locale, main: LegacyMainFrame) extends FrameBase[JDialog] {
  private var installPath: JTextField = _

  private var enableDebug: JCheckBox            = _
  private var enableLogging: JCheckBox          = _
  private var enableMultiplayerPatch: JCheckBox = _
  private var enableLuaJIT: JCheckBox           = _

  private val desktop = Desktop.getDesktop

  private def validateSettings() =
    if (!enableMultiplayerPatch.isSelected && !enableLuaJIT.isSelected) {
      warn("error.nothingenabled")
      false
    } else true
  private def applySettings(): Unit = {
    main.changeInstaller(Paths.get(installPath.getText))

    // TODO ConfigurationStore.legacyEnableDebug.value = enableDebug.isSelected
    // TODO ConfigurationStore.legacyEnableLogging.value = enableLogging.isSelected
    // TODO ConfigurationStore.legacyEnableMultiplayerPatch.value = enableMultiplayerPatch.isSelected
    // TODO ConfigurationStore.legacyEnableLuaJIT.value = enableLuaJIT.isSelected

    main.update()
  }
  private def closeWindow() = {
    frame.setVisible(false)
    frame.dispose()
  }

  override protected def buildForm(): Unit = {
    frame = new JDialog(main.getFrame, i18n("title.settings"), true)
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    frame.setLayout(new GridBagLayout())

    frame.subFrame(constraints(fill = GridBagConstraints.BOTH)) { options =>
      installPath = options.gridButtonTextRow(0, "path", "browse") {
        val chooser         = new JFileChooser()
        val installPathFile = new File(installPath.getText)
        if (installPath.getText.trim.nonEmpty && installPathFile.exists) chooser.setCurrentDirectory(installPathFile)
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
          installPath.setText(chooser.getSelectedFile.toString)
      }
      installPath.setEditable(true)
      main.getInstaller match {
        case None            => installPath.setText("")
        case Some(installer) => installPath.setText(installer.basePath.toString)
      }

      enableLogging = options.gridCheckRow(1, "logging")
      // TODO enableLogging.setSelected(ConfigurationStore.legacyEnableLogging.value)

      enableMultiplayerPatch = options.gridCheckRow(2, "modding")
      // TODO enableMultiplayerPatch.setSelected(ConfigurationStore.legacyEnableMultiplayerPatch.value)

      enableLuaJIT = options.gridCheckRow(3, "luajit")
      // TODO enableLuaJIT.setSelected(ConfigurationStore.legacyEnableLuaJIT.value)

      enableDebug = options.gridCheckRow(4, "debug")
      // TODO enableDebug.setSelected(ConfigurationStore.legacyEnableDebug.value)
    }

    frame.add(
      new JSeparator(),
      constraints(gridy = 1, weighty = 1, fill = GridBagConstraints.BOTH, insets = insets(top = 2, bottom = 2))
    )

    frame.subFrame(constraints(gridy = 2, fill = GridBagConstraints.BOTH)) { frameButtons =>
      val viewLog = new ActionButton(false)
      viewLog.setAction("action.viewlog", () => desktop.open(MPPatchInstaller.logFile))
      frameButtons.add(viewLog, constraints())

      frameButtons.add(new JPanel, constraints(gridx = 1, weightx = 1))

      val apply = new ActionButton(false)
      apply.setAction("action.apply", () => if (validateSettings()) applySettings())
      frameButtons.add(apply, constraints(gridx = 2))

      val cancel = new ActionButton(false)
      cancel.setAction("action.cancel", () => closeWindow())
      frameButtons.add(cancel, constraints(gridx = 3))

      val ok = new ActionButton(false)
      ok.setAction(
        "action.ok",
        () =>
          if (validateSettings()) {
            applySettings()
            closeWindow()
          }
      )
      frameButtons.add(ok, constraints(gridx = 4))

      sizeButtons(viewLog)
      sizeButtons(apply, cancel, ok)
    }
  }
}
