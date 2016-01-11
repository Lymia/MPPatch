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

package moe.lymia.multiverse.core

import java.nio.file.{Files, Path}
import java.util.UUID

import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.Crypto
import moe.lymia.multiverse.util.XMLUtils._

import scala.xml.{Node, NodeSeq, XML}

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
                       authorship: ModAuthorshipInformation,
                       dependencies: Seq[ModReference], references: Seq[ModReference], blocks: Seq[ModReference])
case class ModGameplay(fileList: Map[String, Array[Byte]], entryPoints: Seq[ModEntryPoint],
                       onModActivated: Seq[ModAction], onCreateUserData: Seq[ModAction],
                       dllOverride: Option[Array[Byte]])
case class ModData(manifest: ModManifest, rawProperties: Map[String, String], data: ModGameplay)

object ModDataReader {
  private def readFlag    (node: Node, tag: String) = getNodeText(node, tag) == "1"
  private def readFile(modBasePath: Path, path: String, platform: Platform) =
    Files.readAllBytes(platform.resolve(modBasePath, path))
  private def loadScriptFile(string: String) = try {
    ModXmlSource(XML.loadString(string))
  } catch {
    case _: Exception => ModSqlSource(string)
  }
  private def readModActionList(modBasePath: Path, n: NodeSeq, p: Platform) = n.flatMap(_.child).collect {
    case <UpdateDatabase>{str}</UpdateDatabase> =>
      ModUpdateDatabaseAction(loadScriptFile(new String(readFile(modBasePath, str.text.trim, p))))
    case <UpdateUserData>{str}</UpdateUserData> =>
      ModUpdateDatabaseAction(loadScriptFile(new String(readFile(modBasePath, str.text.trim, p))))
    case <ExecuteScript>{str}</ExecuteScript>   =>
      ModExecuteScriptAction (new String(readFile(modBasePath, str.text.trim, p)))
  }
  private def readModReferenceList(n: NodeSeq) = n.flatMap(_.child).collect {
    case game @ <Game/> => ModGameVersion(getAttribute(game, "minversion"), getAttribute(game, "maxversion"))
    case mod @ <Mod/> =>
      ModToModReference(UUID.fromString(getAttribute(mod, "id")),
                        getAttribute(mod, "minversion").toInt, getAttribute(mod, "maxversion").toInt,
                        getAttribute(mod, "title"))
    case dlc @ <DLC/> =>
      ModDlcDependency(UUID.fromString(getAttribute(dlc, "id")),
                       getAttribute(dlc, "minversion").toInt, getAttribute(dlc, "maxversion").toInt)
  }
  private def readModEntryPoints(uuid: UUID, modBasePath: Path, n: NodeSeq,
                                 files: Map[String, (String, Boolean, String)], platform: Platform) = {
    val isImported = files.mapValues(_._2)
    val fileMD5    = files.mapValues(_._3)
    val uuidStr    = uuid.toString.toLowerCase.replace("-", "")
    val out        = n.flatMap(_.child).collect {
      case ep if ep.label == "EntryPoint" =>
        val fileName = getAttribute(ep, "file")
        val isFileImported = isImported.getOrElse(fileName.toLowerCase(), true)
        val md5 = fileMD5.getOrElse(fileName.toLowerCase(), "")
        val filteredFileName = fileName.split("[/\\\\\\\\]").last
        val outputName = if(isFileImported) fileName
                         else "mvmm_entrypoint_noexport_"+uuidStr+"_"+md5+"_"+filteredFileName

        val entryPoint = ModEntryPoint(getAttribute(ep, "type"), getNodeText(ep, "Name"),
                                       getNodeText(ep, "Description"), outputName)
        val fileListEntry = if(!isFileImported) {
          val data = readFile(modBasePath, fileName, platform)
          assert(Crypto.md5_hex(data).equalsIgnoreCase(md5))
          Some((outputName, data))
        } else None
        (entryPoint, fileListEntry)
    }
    (out.map(_._1), out.flatMap(_._2.toSeq))
  }
  private def readFileList(n: NodeSeq): Map[String, (String, Boolean, String)] =
    n.flatMap(_.child).collect {
      case file @ <File>{name}</File> =>
        name.toString().toLowerCase() ->
          ((name.toString(), getAttribute(file, "import") == "1", getAttribute(file, "md5")))
    }.toMap
  private def loadFiles(modBasePath: Path, files: Map[String, (String, Boolean, String)], platform: Platform) =
    for((_, (name, doImport, md5)) <- files if doImport) yield {
      val data = readFile(modBasePath, name, platform)
      assert(Crypto.md5_hex(data).equalsIgnoreCase(md5))
      (name, data)
    }
  def readModManifest(modData: Node) = {
    val properties = (modData \ "Properties").head
    val authorship = ModAuthorshipInformation(getNodeText(properties, "Authors"),
                                              getNodeText(properties, "SpecialThanks"),
                                              getNodeText(properties, "Homepage"))

    ModManifest(UUID.fromString(getAttribute(modData, "id")), getAttribute(modData, "version").toInt,
                getNodeText(properties, "Name"),
                getNodeText(properties, "Teaser"),
                getNodeText(properties, "Description"),
                authorship,
                readModReferenceList(modData \ "Dependencies"),
                readModReferenceList(modData \ "References"),
                readModReferenceList(modData \ "Blocks"))
  }

  def readModGameplay(modBasePath: Path, modData: Node, platform: Platform) = {
    val uuid = UUID.fromString(getAttribute(modData, "id"))
    val fileList = readFileList(modData \ "Files")
    val (entryPoints, entryFiles) = readModEntryPoints(uuid, modBasePath, modData \ "EntryPoints", fileList, platform)
    ModGameplay(loadFiles(modBasePath, fileList, platform) ++ entryFiles,
                entryPoints,
                readModActionList(modBasePath, modData \ "Actions" \ "OnModActivated"     , platform),
                readModActionList(modBasePath, modData \ "Actions" \ "OnCreateModUserData", platform),
                (modData \ "Actions" \ "OnGetDLLPath" \ "SetDLLPath").headOption map { pathElem =>
                  readFile(modBasePath, pathElem.text.trim, platform)})
  }

  def readModRawProperties(modData: Node) = (modData \ "Properties").flatMap(_.child).collect {
    case n: Node => (n.label, n.text)
  }.toMap
  def loadMod(modBasePath: Path, modData: Node, platform: Platform) =
    ModData(readModManifest(modData), readModRawProperties(modData), readModGameplay(modBasePath, modData, platform))
}