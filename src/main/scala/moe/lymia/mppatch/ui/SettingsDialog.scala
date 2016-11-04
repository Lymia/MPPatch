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
import java.nio.file.Paths
import java.util.Locale
import javax.swing._

class SettingsDialog(val locale: Locale, main: MainFrame) extends FrameBase[JDialog] {
  private var installPath: JTextField = _

  private var enableDebug: JCheckBox = _

  private def applySettings(): Unit = {
    main.changeInstaller(Paths.get(installPath.getText))

    Preferences.enableDebug.value = enableDebug.isSelected

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
        val installer = main.getInstaller
        val chooser = new JFileChooser()
        if(installer != null) chooser.setCurrentDirectory(installer.basePath.toFile)
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        if(chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
          installPath.setText(chooser.getSelectedFile.toString)
      }
      installPath.setEditable(true)
      main.getInstaller match {
        case null => installPath.setText("")
        case installer => installPath.setText(installer.basePath.toString)
      }

      enableDebug = options.gridCheckRow(1, "debug")
      enableDebug.setSelected(Preferences.enableDebug.value)

      equalHeight(enableDebug, installPath)
    }

    frame.add(new JSeparator(), constraints(gridy = 1, weighty = 1, fill = GridBagConstraints.BOTH,
                                            insets = insets(top = 2, bottom = 2)))

    frame.subFrame(constraints(gridy = 2, fill = GridBagConstraints.BOTH)) { frameButtons =>
      frameButtons.add(new JPanel, constraints(weightx = 1))

      val apply = new ActionButton(false)
      apply.setAction("action.apply", () => applySettings())
      frameButtons.add(apply, constraints(gridx = 1))

      val cancel = new ActionButton(false)
      cancel.setAction("action.cancel", () => closeWindow())
      frameButtons.add(cancel, constraints(gridx = 2))

      val ok = new ActionButton(false)
      ok.setAction("action.ok", () => {
        applySettings()
        closeWindow()
      })
      frameButtons.add(ok, constraints(gridx = 3))

      equalButtonWidth(apply, cancel, ok)
    }
  }
}