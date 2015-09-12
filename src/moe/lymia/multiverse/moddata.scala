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

package moe.lymia.multiverse

import java.nio.file.{Files, Path}
import java.util.UUID
import moe.lymia.multiverse.platform.Platform

import scala.xml.{XML, PrettyPrinter, NodeSeq, Node}

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
case class ModGameplayFlags(hideSetupGame: Boolean, affectsSavegame: Option[Int],
                            supportsSingleplayer: Boolean, supportsMultiplayer: Boolean,
                            supportsHotseat: Boolean, supportsMac: Boolean,
                            reloadAudioSystem: Boolean, reloadLandmarkSystem: Boolean,
                            reloadStrategicViewSystem: Boolean, reloadUnitSystem: Boolean)
case class ModGameplay(fileList: Map[String, Array[Byte]], entryPoints: Seq[ModEntryPoint],
                       onModActivated: Seq[ModAction], onCreateUserData: Seq[ModAction],
                       dllOverride: Option[Array[Byte]])
case class ModData(manifest: ModManifest, flags: ModGameplayFlags, data: ModGameplay)

object ModDataReader {
  private def getAttribute(node: Node, attribute: String) = (node \ ("@" + attribute)).text
  private def getNodeText (node: Node, tag: String) = (node \ tag).text.trim
  private def readFlag    (node: Node, tag: String) = getNodeText(node, tag) == "1"
  private def readFile(modBasePath: Path, path: String, platform: Platform) =
    Files.readAllBytes(modBasePath.resolve(platform.mapFileName(path)))
  private def loadScriptFile(string: String) = try {
    ModXmlSource(XML.loadString(string))
  } catch {
    case _: Throwable => ModSqlSource(string)
  }
  private def readModActionList(modBasePath: Path, n: NodeSeq, p: Platform) = n.flatMap(_.child).collect {
    case <UpdateDatabase>{str}</UpdateDatabase> =>
      ModUpdateDatabaseAction(loadScriptFile(new String(readFile(modBasePath, str.text.trim, p))))
    case <UpdateUserData>{str}</UpdateUserData> =>
      ModUpdateDatabaseAction(loadScriptFile(new String(readFile(modBasePath, str.text.trim, p))))
    case <ExecuteScript> {str}</ExecuteScript>  =>
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
  private def readModEntryPoints(n: NodeSeq) = n.flatMap(_.child).collect {
    case ep @ <EntryPoint/> =>
      ModEntryPoint(getAttribute(ep, "type"), getNodeText(ep, "Name"), getNodeText(ep, "Description"),
                    getAttribute(ep, "file"))
  }
  private def loadFiles(modBasePath: Path, n: NodeSeq, platform: Platform) = {
    val files = n.flatMap(_.child).collect {
      case file @ <File>{name}</File> =>
        (name.toString(), getAttribute(file, "import") == "1", getAttribute(file, "md5"))
    }
    (for((name, doImport, md5) <- files if doImport) yield {
      val data = readFile(modBasePath, name, platform)
      assert(Crypto.md5_hex(data).equalsIgnoreCase(md5))
      (name, data)
    }).toMap
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
  def readModGameplayFlags(modData: Node) = {
    val properties = (modData \ "Properties").head

    ModGameplayFlags(readFlag(properties, "HideSetupGame"),
                     if(readFlag(properties, "AffectsSavedGames"))
                       Some(getNodeText(properties, "MinCompatibleSaveVersion").toInt)
                     else None,
                     readFlag(properties, "SupportsSinglePlayer"), readFlag(properties, "SupportsMultiplayer"),
                     readFlag(properties, "SupportsHotSeat"), readFlag(properties, "SupportsMac"),
                     readFlag(properties, "ReloadAudioSystem"), readFlag(properties, "ReloadLandmarkSystem"),
                     readFlag(properties, "ReloadStrategicViewSystem"), readFlag(properties, "ReloadUnitSystem"))
  }
  def readModGameplay(modBasePath: Path, modData: Node, platform: Platform) =
    ModGameplay(loadFiles(modBasePath, modData \ "Files", platform),
                readModEntryPoints(modData \ "EntryPoints"),
                readModActionList(modBasePath, modData \ "Actions" \ "OnModActivated"     , platform),
                readModActionList(modBasePath, modData \ "Actions" \ "OnCreateModUserData", platform),
                (modData \ "Actions" \ "OnGetDLLPath" \ "SetDLLPath").headOption map { pathElem =>
                  readFile(modBasePath, pathElem.text.trim, platform)})
  def loadMod(modBasePath: Path, modData: Node, platform: Platform) =
    ModData(readModManifest(modData), readModGameplayFlags(modData), readModGameplay(modBasePath, modData, platform))
}