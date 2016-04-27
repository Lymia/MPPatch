/*
 * Copyright (c) 2015 Lymia Alusyia <lymia@lymiahugs.com>
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

import java.util.UUID

import scala.xml.Node

case class ModAuthorshipInformation(authors: String, specialThanks: String, homepage: String)

sealed trait ModReference
case class ModToModReference(id: UUID, minVersion: Int, maxVersion: Int, title: String) extends ModReference
case class ModGameVersion(minVersion: String, maxVersion: String) extends ModReference
case class ModDlcDependency(id: UUID, minVersion: Int, maxVersion: Int) extends ModReference

sealed trait ModDataSource
case class ModSqlSource(sql: String) extends ModDataSource
case class ModXmlSource(xml: Node) extends ModDataSource

sealed trait ModAction
case class ModUpdateDatabaseAction(data: ModDataSource) extends ModAction
case class ModUpdateUserDataAction(data: ModDataSource) extends ModAction
case class ModExecuteScriptAction (data: String) extends ModAction

case class ModEntryPoint(event: String, name: String, description: String, file: String)
case class ModManifest(uuid: UUID, version: Int, name: String, teaser: String, description: String,
                       authorship: ModAuthorshipInformation, rawProperties: Map[String, String],
                       dependencies: Seq[ModReference], references: Seq[ModReference], blocks: Seq[ModReference])
  extends ManifestCommon
case class ModGameplay(fileList: Map[String, Array[Byte]], entryPoints: Seq[ModEntryPoint],
                       onModActivated: Seq[ModAction], onCreateUserData: Seq[ModAction],
                       dllOverride: Option[Array[Byte]])
case class ModData(manifest: ModManifest, data: ModGameplay)

