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
import java.awt._
import java.util.Locale
import javax.swing._

import moe.lymia.mppatch.util.{IOUtils, VersionInfo}

import scala.language.implicitConversions

object FrameUtils {
  lazy val symbolFont = Font.createFont(Font.TRUETYPE_FONT, IOUtils.getResource("text/Symbola_hint_subset.ttf"))
}
import FrameUtils._

trait FrameUtils {
  protected def insets(top: Int = 0, left: Int = 0, bottom: Int = 0, right: Int = 0) =
    new Insets(top, left, bottom, right)
  protected def constraints(gridx: Int = GridBagConstraints.RELATIVE, gridy: Int = GridBagConstraints.RELATIVE,
                            gridwidth: Int = 1, gridheight: Int = 1, weightx: Double = 0, weighty: Double = 0,
                            anchor: Int = GridBagConstraints.CENTER, fill: Int = GridBagConstraints.NONE,
                            insets: Insets = this.insets(), ipadx: Int = 0, ipady: Int = 0) =
    new GridBagConstraints(gridx, gridy, gridwidth, gridheight, weightx, weighty, anchor, fill, insets, ipadx, ipady)

  protected implicit def action(f: ActionEvent => Unit): Action = new AbstractAction() {
    override def actionPerformed(e: ActionEvent): Unit = f(e)
  }

  protected class FontLabel(style: Int, string: String) extends JLabel(string) {
    setFont(getFont.deriveFont(style))
  }

  protected def symbolButton(button: JButton) = {
    val size = button.getMinimumSize
    if(size.getWidth < size.getHeight) {
      button.setMinimumSize  (new Dimension(size.getHeight.toInt, size.getHeight.toInt))
      button.setPreferredSize(new Dimension(size.getHeight.toInt, size.getHeight.toInt))
    }

    val font = button.getFont
    button.setFont(symbolFont.deriveFont(Font.PLAIN, font.getSize))

    button
  }
  protected def equalWidth(component: JComponent*) = {
    val maxWidth = component.map(_.getMinimumSize.getWidth).max.toInt + 20
    for(c <- component) {
      val height = c.getMinimumSize.getHeight.toInt
      c.setMinimumSize  (new Dimension(maxWidth, height))
      c.setPreferredSize(new Dimension(maxWidth, height))
    }
  }

  protected implicit class ContainerExtension(c: Container) {
    def subFrame(constraints: GridBagConstraints)(f: JPanel => Unit) = {
      val panel = new JPanel()
      panel.setLayout(new GridBagLayout())
      f(panel)
      c.add(panel, constraints)
      panel
    }
  }
}

trait I18NFrameUtils extends FrameUtils { this: I18NTrait =>
  protected implicit class I18NContainerExtension(c: Container) {
    def gridLabel(row: Int, labelStr: String) = {
      val label = new JLabel()
      label.setText(i18n(s"label.$labelStr"))
      c.add(label, constraints(gridy = row, insets = insets(left = 3, right = 4),
                               anchor = GridBagConstraints.LINE_START))
    }
    def gridTextField(row: Int, width: Int = 2) = {
      val textField = new JTextField()
      textField.setEditable(false)
      textField.setPreferredSize(new Dimension(450, textField.getPreferredSize.getHeight.toInt))
      c.add(textField, constraints(gridx = 1, gridy = row, gridwidth = width, weightx = 1,
                                   fill = GridBagConstraints.BOTH))
      textField
    }
    def iconButton(col: Int, row: Int, str: String)(f: => Unit) = {
      val button = new JButton()
      button.setAction(action { e => f })
      button.setText(i18n("icon."+str))
      button.setToolTipText(i18n("tooltip."+str))
      symbolButton(button)
      c.add(button, constraints(gridx = col, gridy = row, fill = GridBagConstraints.BOTH))
    }

    def gridTextRow(row: Int, labelStr: String) = {
      gridLabel(row, labelStr)
      gridTextField(row)
    }
    def gridButtonTextRow(row: Int, labelStr: String, icon: String)(f: => Unit) = {
      gridLabel(row, labelStr)
      val field = gridTextField(row, 1)
      iconButton(2, row, icon)(f)
      field
    }
  }
}

trait I18NTrait {
  protected def locale: Locale
  protected lazy val i18n = I18N(locale)
}

trait FrameError[F <: Window] {
  protected def frame: F
  protected def i18n: I18N

  protected def titleString = i18n("title", VersionInfo.fromJar.versionString)

  protected def warn(string: String) = {
    JOptionPane.showMessageDialog(if(frame != null && frame.isVisible) frame else null, string,
                                  titleString, JOptionPane.ERROR_MESSAGE)
  }
  protected def error[T](string: String): T = {
    warn(string)
    if(frame != null) {
      frame.setVisible(false)
      frame.dispose()
    }
    sys.error(string)
  }
  protected def dumpException[T](errorString: String, e: Exception, exArgs: Object*): T = {
    e.printStackTrace()
    error(i18n(errorString, (e.getClass+": "+e.getMessage) +: exArgs : _*))
  }
}

trait FrameBase[F <: Window] extends FrameError[F] with I18NFrameUtils with I18NTrait {
  protected var frame: F = _
  def getFrame = frame

  protected def buildForm()
  def update() { }

  def showForm() = {
    buildForm()
    update()
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.setVisible(true)
  }

  protected class ActionButton(showCompleteMessage: Boolean = true) extends JButton {
    var action: () => Unit = () => error("no action registered")
    var text  : String     = "<no action>"

    setAction(FrameBase.this.action { e =>
      try {
        action()
        if(showCompleteMessage && i18n.hasKey(text+".completed"))
          JOptionPane.showMessageDialog(frame, i18n(text+".completed"))
      } catch {
        case e: Exception =>
          dumpException(if(i18n.hasKey(text+".continuous")) "error.commandfailed" else "error.commandfailed.generic",
                        e, i18n(text+".continuous"))
      }
      FrameBase.this.update()
    })

    def setActionText(name: String): Unit = {
      text = name
      setText(i18n(name))
      if(i18n.hasKey(name+".tooltip")) setToolTipText(i18n(name+".tooltip"))
    }
    def setAction(name: String, action: () => Unit): Unit = {
      this.action = action
      setActionText(name)
    }
  }
}