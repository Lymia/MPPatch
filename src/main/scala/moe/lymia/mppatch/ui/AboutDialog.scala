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

import java.awt._
import java.text.DateFormat
import java.util.Locale
import javax.swing.text.html.HTMLEditorKit
import javax.swing._
import javax.swing.event.{HyperlinkEvent, HyperlinkListener}

import moe.lymia.mppatch.util.VersionInfo
import moe.lymia.mppatch.util.io.IOUtils

class AboutDialog(val locale: Locale, main: MainFrame) extends FrameBase[JDialog] {
  private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
  private val desktop = Desktop.getDesktop
  override protected def buildForm(): Unit = {
    frame = new JDialog(main.getFrame, i18n("title.about"), true)
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    frame.setLayout(new GridBagLayout())

    val editor = new JEditorPane()
    editor.setEditable(false)

    val scroll = new JScrollPane(editor)
    scroll.setPreferredSize(new Dimension(500, 300))

    val htmlEditor = new HTMLEditorKit
    editor.setEditorKit(htmlEditor)

    val systemFont = new JLabel().getFont
    val stylesheet = htmlEditor.getStyleSheet
    stylesheet.addRule(IOUtils.loadResource("text/style.css"))
    stylesheet.addRule(
      s"""p, li, h1, h2, h3, h4, h5, h6 {
         |  font-family: ${systemFont.getFontName},sans-serif;
         |  font-size: ${systemFont.getSize};
         |}
       """.stripMargin)

    val document = htmlEditor.createDefaultDocument()
    editor.setDocument(document)

    def setPage(pageParam: String) = {
      val page = pageParam.replaceAll("^/+", "")
      editor.setText(IOUtils.loadResource(page))
      editor.setCaretPosition(0)
    }
    setPage("text/about.html")

    editor.addHyperlinkListener(new HyperlinkListener {
      override def hyperlinkUpdate(e: HyperlinkEvent): Unit = {
        if(e.getEventType == HyperlinkEvent.EventType.ACTIVATED) {
          val url = e.getURL
          if(url.getProtocol == "http" && url.getHost == "fromres") setPage(url.getPath)
          else if(url.getProtocol == "http" || url.getProtocol == "https") desktop.browse(e.getURL.toURI)
          else warn("Unknown protocol "+url.getProtocol)
        }
      }
    })

    frame.add(new FontLabel(Font.BOLD,
                            i18n("about.0", VersionInfo.versionString)),
              constraints(gridy = 0, anchor = GridBagConstraints.LINE_START,
                          insets = insets(left = 3, right = 3, top = 3)))
    frame.add(new FontLabel(Font.PLAIN,
                            i18n("about.1", VersionInfo.commit.substring(0, 8),
                                            if(VersionInfo.isDirty) i18n("about.dirty") else "",
                                            dateFormat.format(VersionInfo.buildDate), VersionInfo.buildUser)),
              constraints(gridy = 1, anchor = GridBagConstraints.LINE_START,
                          insets = insets(left = 3, right = 3, bottom = 3)))
    frame.add(scroll, constraints(gridy = 2, weightx = 1, weighty = 1,
                                  fill = GridBagConstraints.BOTH))
  }
}