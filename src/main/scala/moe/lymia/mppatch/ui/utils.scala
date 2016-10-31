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
import java.awt.{Frame, GridBagConstraints, Insets}
import java.util.Locale
import javax.swing._

import moe.lymia.mppatch.util.VersionInfo

import scala.language.implicitConversions

trait FrameUtils {
  def constraints(gridx: Int = GridBagConstraints.RELATIVE, gridy: Int = GridBagConstraints.RELATIVE,
                  gridwidth: Int = 1, gridheight: Int = 1, weightx: Double = 0, weighty: Double = 0,
                  anchor: Int = GridBagConstraints.CENTER, fill: Int = GridBagConstraints.NONE,
                  insets: Insets = new Insets(0, 0, 0, 0), ipadx: Int = 0, ipady: Int = 0) =
    new GridBagConstraints(gridx, gridy, gridwidth, gridheight, weightx, weighty, anchor, fill, insets, ipadx, ipady)

  implicit def action(f: ActionEvent => Unit): Action = new AbstractAction() {
    override def actionPerformed(e: ActionEvent): Unit = f(e)
  }
}

trait I18NTrait {
  protected def locale: Locale
  protected val i18n = I18N(locale)
}

trait FrameError[F <: Frame] {
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

trait FrameBase[F <: Frame] extends FrameError[F] with FrameUtils with I18NTrait {
  protected var frame: F = _
  protected def parent: F = null.asInstanceOf[F]

  protected def buildForm()
  protected def update() { }

  def showForm() = {
    buildForm()
    update()
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.setVisible(true)
  }
}