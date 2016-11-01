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

import java.util.Locale
import javax.swing.{JDialog, WindowConstants}

import com.github.rjeschke.txtmark.Processor
import moe.lymia.mppatch.util.{IOUtils, VersionInfo}

class AboutDialog(val locale: Locale, owner: MainFrame) extends FrameBase[JDialog] {
  private val variables = Seq(
    "VERSION"    -> VersionInfo.fromJar.versionString,
    "REVISION"   -> VersionInfo.fromJar.commit.substring(0, 8),
    "BUILD_DATE" -> VersionInfo.fromJar.buildDate,
    "BUILD_USER" -> VersionInfo.fromJar.buildUser
  )
  private def renderMarkdown(resource: String) = {
    var markdown = IOUtils.loadResource(resource)
    for((key, value) <- variables) markdown = markdown.replace(s"{$key}", value)
    Processor.process(markdown)
  }

  println(renderMarkdown("text/about.md"))

  override protected def buildForm(): Unit = {
    frame = new JDialog(owner.getFrame, i18n("title.about"), true)
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
  }
}