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

package moe.lymia.mppatch.util.io

import scala.xml.{Node, NodeSeq}

object XMLUtils {
  def getOptional(nodes: NodeSeq) =
    if (nodes.isEmpty) None else Some(nodes.text)

  def getAttributeNodes(node: Node, attribute: String)    = node \ s"@$attribute"
  def getBoolAttribute(node: Node, attribute: String)     = getAttributeNodes(node, attribute).nonEmpty
  def getOptionalAttribute(node: Node, attribute: String) = getOptional(getAttributeNodes(node, attribute))
  def getAttribute(node: Node, attribute: String) =
    getOptionalAttribute(node, attribute).getOrElse(sys.error(s"No such attribute '$attribute'"))

  def getNodeText(node: Node, tag: String)         = (node \ tag).text.trim
  def getOptionalNodeText(node: Node, tag: String) = getOptional(node \ tag).map(_.trim)

  def loadPath(node: Node)   = getAttribute(node, "Path")
  def loadHash(node: Node)   = getAttribute(node, "Hash")
  def loadSource(node: Node) = getAttribute(node, "Source")
}
