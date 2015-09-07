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

package moe.lymia.mod2dlc

import java.nio.file.{Files, Path}
import java.util.UUID
import moe.lymia.mod2dlc.platform.Platform

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
      ModUpdateDatabaseAction(loadScriptFile(new String(readFile(modBasePath, str.text, p))))
    case <UpdateUserData>{str}</UpdateUserData> =>
      ModUpdateDatabaseAction(loadScriptFile(new String(readFile(modBasePath, str.text, p))))
    case <ExecuteScript> {str}</ExecuteScript>  =>
      ModExecuteScriptAction (new String(readFile(modBasePath, str.text, p)))
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
      assert(Crypto.md5(data).equalsIgnoreCase(md5))
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
                  readFile(modBasePath, pathElem.text, platform)})
  def loadMod(modBasePath: Path, modData: Node, platform: Platform) =
    ModData(readModManifest(modData), readModGameplayFlags(modData), readModGameplay(modBasePath, modData, platform))
}
object ModDataWriter {
  private def outputFlag(b: Boolean) = if(b) "1" else "0"
  private def generateReferencesBlock(ref: Seq[ModReference]) = ref map {
    case ModToModReference(id, minVersion, maxVersion, title) =>
      <Mod id={id.toString} minversion={minVersion.toString} maxversion={maxVersion.toString} title={title}/>
    case ModGameVersion(minVersion, maxVersion) =>
      <Game minversion={minVersion} maxversion={maxVersion}/>
    case ModDlcDependency(id, minVersion, maxVersion) =>
      <DLC id={id.toString} minversion={minVersion.toString} maxversion={maxVersion.toString}/>
  }
  private def generateEntryPointsBlock(points: Seq[ModEntryPoint]) = points.map { point =>
    <EntryPoint type={point.event} file={point.file}>
      <Name>{point.name}</Name>
      <Description>{point.description}</Description>
    </EntryPoint>
  }
  private def writeFile(path: Path, data: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, data)
  }
  def writeMod(path: Path, modData: ModData, platform: Platform) = {
    val modString = modData.manifest.name+" (v"+modData.manifest.version+")"

    def writeData(data: String, extension: String) = {
      val bytes = data.getBytes("UTF8")
      val fileName = "noimport/"+modString+"_noimport_"+Crypto.sha1(bytes)+"."+extension
      val filePath = path.resolve(platform.mapFileName(fileName))
      writeFile(filePath, bytes)
      (filePath, fileName, bytes)
    }
    def writeDataSourceFile(dataSource: ModDataSource) = dataSource match {
      case ModSqlSource(scriptData) => writeData(scriptData, "sql")
      case ModXmlSource(xml) => writeData(new PrettyPrinter(Int.MaxValue, 4).format(xml), "xml")
    }
    val generatedMap = (for(a <- modData.data.onCreateUserData ++ modData.data.onModActivated) yield (a, a match {
      case ModUpdateDatabaseAction(dataSource) => writeDataSourceFile(dataSource)
      case ModUpdateUserDataAction(dataSource) => writeDataSourceFile(dataSource)
      case ModExecuteScriptAction (scriptData) => writeData(scriptData, "lua")
    })).toMap

    def generateActionsBlock(act: Seq[ModAction]) = act map {
      case v: ModUpdateDatabaseAction => <UpdateDatabase>{generatedMap(v)._2}</UpdateDatabase>
      case v: ModUpdateUserDataAction => <UpdateUserData>{generatedMap(v)._2}</UpdateUserData>
      case v: ModExecuteScriptAction  => <ExecuteScript> {generatedMap(v)._2}</ExecuteScript>
    }

    val modInfo =
      <Mod id={modData.manifest.uuid.toString} version={modData.manifest.version.toString}>
        <!-- Mod data file generated by Mod2DLC -->
        <Properties>
          <Name>{modData.manifest.name}</Name>
          <Teaser>{modData.manifest.teaser}</Teaser>
          <Description>{modData.manifest.description}</Description>
          <Authors>{modData.manifest.authorship.authors}</Authors>
          <SpecialThanks>{modData.manifest.authorship.specialThanks}</SpecialThanks>
          <Homepage>{modData.manifest.authorship.homepage}</Homepage>
          {modData.flags.affectsSavegame match {
          case Some(x) =>
            Seq(<AffectsSavedGames>1</AffectsSavedGames>,
              <MinCompatibleSaveVersion>{x}</MinCompatibleSaveVersion>)
          case None => Seq(<AffectsSavedGames>0</AffectsSavedGames>)
        }}
          <HideSetupGame>{outputFlag(modData.flags.hideSetupGame)}</HideSetupGame>
          <SupportsSinglePlayer>{outputFlag(modData.flags.supportsSingleplayer)}</SupportsSinglePlayer>
          <SupportsMultiplayer>{outputFlag(modData.flags.supportsMultiplayer)}</SupportsMultiplayer>
          <SupportsHotSeat>{outputFlag(modData.flags.supportsHotseat)}</SupportsHotSeat>
          <SupportsMac>{outputFlag(modData.flags.supportsMac)}</SupportsMac>
          <ReloadAudioSystem>{outputFlag(modData.flags.reloadAudioSystem)}</ReloadAudioSystem>
          <ReloadLandmarkSystem>{outputFlag(modData.flags.reloadLandmarkSystem)}</ReloadLandmarkSystem>
          <ReloadStrategicViewSystem>{outputFlag(modData.flags.reloadStrategicViewSystem)}</ReloadStrategicViewSystem>
          <ReloadUnitSystem>{outputFlag(modData.flags.reloadUnitSystem)}</ReloadUnitSystem>
        </Properties>
        <Dependencies>{generateReferencesBlock(modData.manifest.dependencies)}</Dependencies>
        <References>{generateReferencesBlock(modData.manifest.references)}</References>
        <Blocks>{generateReferencesBlock(modData.manifest.blocks)}</Blocks>
        <Files> {
          for((name, data) <- modData.data.fileList) yield {
            writeFile(path.resolve(platform.mapFileName(name)), data)
            <File md5={Crypto.md5(data)} import="1">{name}</File>
          }
        } {
          for((_, (_, name, data)) <- generatedMap) yield
            <File md5={Crypto.md5(data)} import="0">{name}</File>
        } </Files>
        <Actions>
          <OnModActivated>{generateActionsBlock(modData.data.onModActivated)}</OnModActivated>
          <OnCreateModUserData>{generateActionsBlock(modData.data.onCreateUserData)}</OnCreateModUserData>
          {
            modData.data.dllOverride match {
              case Some(x) =>
                val dllPath = path.resolve(platform.mapFileName("CvGameCore_"+modString+".dll"))
                writeFile(dllPath, x)
                Seq(<OnGetDLLPath><SetDLLPath>{dllPath.toString}</SetDLLPath></OnGetDLLPath>)
              case None => Seq()
            }
          }
        </Actions>
        <EntryPoints>{generateEntryPointsBlock(modData.data.entryPoints)}</EntryPoints>
      </Mod>
    val xmlString = new PrettyPrinter(Int.MaxValue, 4).format(modInfo)
    val xmlPath = path.resolve(platform.mapFileName(modString+".modinfo"))
    writeFile(xmlPath, xmlString.getBytes("UTF8"))
  }
}