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

package moe.lymia.multiverse.core.data

import java.nio.file.Path
import java.util.UUID

import moe.lymia.multiverse.util.IOUtils

import scala.xml.Node
import moe.lymia.multiverse.util.XMLUtils._

object DLCDataReader {
  private def readDlcNameField(uuid: UUID, dlcData: Node, nodeName: String) = {
    val node = dlcData \ nodeName \ "Value"
    if(node.isEmpty) uuid.toString
    else {
      val englishList = node.filter(x => getAttribute(x, "language") == "en_US")
      if(englishList.nonEmpty) englishList.head.text
      else                     node.head.text
    }
  }
  def readDlcManifest(dlcData: Node): DLCManifest = {
    val uuid = UUID.fromString(getNodeText(dlcData, "GUID").replaceAll("[{}]", ""))
    DLCManifest(uuid,
                getNodeText(dlcData, "Version").toInt,
                getOptionalAttribute(dlcData, "Priority").map(_.toInt).getOrElse(0),
                readDlcNameField(uuid, dlcData, "Name"),
                readDlcNameField(uuid, dlcData, "Description"))
  }
  def readDlcManifest(dlcData: Path) = readDlcManifest(IOUtils.readXML(dlcData))
}
