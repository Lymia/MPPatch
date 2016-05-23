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

// Common components
trait ManifestCommon {
  val uuid   : UUID
  val version: Int
  val name   : String
}

sealed trait ImportedFile {
  def data: Array[Byte]
}
case class ImportFromPath(path: Path) extends ImportedFile {
  lazy val data = IOUtils.readFileAsBytes(path)
}
case class ImportFromMemory(data: Array[Byte]) extends ImportedFile

// DLC data structures
case class DLCUISkin(name: String, set: String, platform: String, includeImports: Boolean,
                     skinSpecificDirectory: Map[String, ImportedFile])
case class DLCInclude(event: String, fileData: Node)
case class DLCMap(extension: String, data: ImportedFile)

case class DLCGameplay(gameplayIncludes: Seq[DLCInclude], globalIncludes: Seq[DLCInclude], mapEntries: Seq[DLCMap],
                       importFileList: Map[String, ImportedFile], uiSkins: Seq[DLCUISkin])
case class DLCManifest(uuid: UUID, version: Int, priority: Int, shortName: String, name: String)
  extends ManifestCommon

case class DLCData(manifest: DLCManifest, data: DLCGameplay)

// Mod manifest data structures
sealed trait ModReference
case class ModToModReference(id: UUID, minVersion: Int, maxVersion: Int, title: String) extends ModReference
case class ModGameVersion(minVersion: String, maxVersion: String) extends ModReference
case class ModDlcDependency(id: UUID, minVersion: Int, maxVersion: Int) extends ModReference

case class ModAuthorshipInformation(authors: String, specialThanks: String, homepage: String)
case class ModManifest(uuid: UUID, version: Int, name: String, teaser: String, description: String,
                       authorship: ModAuthorshipInformation, rawProperties: Map[String, String],
                       dependencies: Seq[ModReference], references: Seq[ModReference], blocks: Seq[ModReference],
                       manifestChecksum: String, manifestData: Node,
                       modDataPath: Option[Path] = None)
  extends ManifestCommon

// Mod gameplay data structures
sealed trait ModDataSource
case class ModSqlSource(sql: String) extends ModDataSource
case class ModXmlSource(xml: Node) extends ModDataSource

sealed trait ModAction
case class ModUpdateDatabaseAction(data: ModDataSource) extends ModAction
case class ModUpdateUserDataAction(data: ModDataSource) extends ModAction
case class ModExecuteScriptAction (data: String) extends ModAction

case class ModEntryPoint(event: String, name: String, description: String, file: String)
case class ModGameplay(importedFiles: Map[String, ImportedFile], entryPoints: Seq[ModEntryPoint],
                       onModActivated: Seq[ModAction], onCreateUserData: Seq[ModAction],
                       dllOverride: Option[ImportedFile])

// Combined data structure
case class ModData(manifest: ModManifest, data: ModGameplay)
