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

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}

import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.{Crypto, IOUtils}

import scala.xml.{Node, NodeSeq}

case class DLCUISkin(name: String, set: String, platform: String, includeImports: Boolean,
                     skinSpecificDirectory: Map[String, Array[Byte]],
                     standalone: Option[DLCManifest])
case class DLCInclude(event: String, fileData: Node)
case class DLCMap(extension: String, data: Array[Byte])

case class DLCGameplay(gameplayIncludes: Seq[DLCInclude] = Nil, globalIncludes: Seq[DLCInclude] = Nil,
                       mapIncludes: Seq[DLCMap] = Nil, globalTextData: NodeSeq = Nil,
                       importFileList: Map[String, Array[Byte]] = Map(),
                       uiOnlyFiles: Map[String, Array[Byte]] = Map(), uiSkins: Seq[DLCUISkin] = Nil)
case class DLCManifest(uuid: UUID, version: Int, priority: Int, shortName: String, name: String)

case class DLCData(manifest: DLCManifest, data: DLCGameplay)

object DLCKey {
  private val staticInterlace =
    Seq(0x1f, 0x33, 0x93, 0xfb, 0x35, 0x0f, 0x42, 0xc7,
        0xbd, 0x50, 0xbe, 0x7a, 0xa5, 0xc2, 0x61, 0x81) map (_.toByte)
  private val staticInterlaceStream = Stream.from(0) map (x => staticInterlace(x%staticInterlace.length))
  private def interlaceData(data: Seq[Byte]) =
    data.zip(staticInterlaceStream).flatMap(x => Seq(x._1, x._2))

  private def encodeLe32(i: Int) =
    Seq(i&0xFF, (i>>8)&0xFF, (i>>16)&0xFF, (i>>24)&0xFF)
  private def encodeBe32(i: Int) = encodeLe32(i).reverse
  private def encodeLe16(i: Int) =
    Seq(i&0xFF, (i>>8)&0xFF)
  private def encodeUUID(u: UUID) =
    (encodeLe32(((u.getMostSignificantBits >>32) & 0xFFFFFFFF).toInt) ++
     encodeLe16(((u.getMostSignificantBits >>16) &     0xFFFF).toInt) ++
     encodeLe16(((u.getMostSignificantBits >> 0) &     0xFFFF).toInt) ++
     encodeBe32(((u.getLeastSignificantBits>>32) & 0xFFFFFFFF).toInt) ++
     encodeBe32(((u.getLeastSignificantBits>> 0) & 0xFFFFFFFF).toInt)).map(_.toByte)
  private def encodeNumber(i: Int) = i.toString.getBytes(StandardCharsets.US_ASCII).toSeq

  def key(u: UUID, sid: Seq[Int], ptags: String*) = {
    val data = sid.map(encodeNumber) ++ ptags.map(_.getBytes(StandardCharsets.UTF_8).toSeq)
    Crypto.md5_hex(interlaceData(encodeUUID(u) ++ data.fold(Seq())(_ ++ _)))
  }
}

object DLCDataWriter {
  private val languageList = Seq("en_US","fr_FR","de_DE","es_ES","it_IT","ru_RU","ja_JP","pl_PL","ko_KR","zh_Hant_HK")
  private def languageValues(string: String) = languageList.map { x =>
    <Value language={x}>{string}</Value>
  }

  private def commonHeader(manifest: DLCManifest, includeValidDlcKey: Boolean = true) =
    <NODE>
      <GUID>{s"{${manifest.uuid}}"}</GUID>
      <Version>{manifest.version.toString}</Version>
      <Name>{languageValues(s"${manifest.uuid.toString.replace("-", "")}_v${manifest.version}")}</Name>
      <Description>{languageValues(manifest.name)}</Description>
      <Priority>{manifest.priority.toString}</Priority>

      <SteamApp>99999</SteamApp>
      <Ownership>FREE</Ownership>
      <PTags>
        <Tag>Version</Tag>
        <Tag>Ownership</Tag>
      </PTags>
      {if(includeValidDlcKey)
         Seq(<Key>{DLCKey.key(manifest.uuid, Seq(99999), manifest.version.toString, "FREE")}</Key>)
       else Seq()}
    </NODE>.child

  private def nameStringFromManifest(manifest: DLCManifest) =
    s"${manifest.uuid.toString.replace("-", "")}_v${manifest.version}"

  private def populateDirectory(dlcBasePath: Path, dirName: String, data: Map[String, Array[Byte]],
                                tagName: String, platform: Platform): NodeSeq = {
    if(data.nonEmpty) {
      val filesDirectory = platform.resolve(dlcBasePath, dirName)
      Files.createDirectories(filesDirectory)
      for((name, file) <- data) IOUtils.writeFile(platform.resolve(filesDirectory, name), file)

      Seq(<NODE>{dirName}</NODE>.copy(label = tagName))
    } else Seq()
  }

  def writeDLC(dlcBasePath: Path, languageFilePath: Option[Path], dlcData: DLCData, platform: Platform) = {
    var id = 0
    def newId() = {
      id = id + 1
      id
    }

    val nameString = nameStringFromManifest(dlcData.manifest)

    val filesInclude  = populateDirectory(dlcBasePath, "Files", dlcData.data.importFileList, "Directory", platform)
    val uiFileInclude = populateDirectory(dlcBasePath, "UI_Files", dlcData.data.uiOnlyFiles, "Directory", platform)
    val mapIncludes   = populateDirectory(dlcBasePath, "Maps", dlcData.data.mapIncludes.map(map =>
      s"mvmm_map_${nameString}_${newId()}.${map.extension}" -> map.data
    ).toMap, "MapDirectory", platform)

    def writeIncludes(pathName: String, includes: Seq[DLCInclude]) = {
      val path = platform.resolve(dlcBasePath, pathName)
      if(includes.nonEmpty) Files.createDirectories(path)
      (for(DLCInclude(event, fileData) <- includes) yield {
        val fileName = s"mvmm_include_${nameString}_${event}_${newId()}.xml"
        IOUtils.writeXML(platform.resolve(path, fileName), fileData)
        <NODE>{fileName}</NODE>.copy(label = event)
      }, if(includes.nonEmpty) <Directory>{pathName}</Directory> else Seq())
    }
    def writeUISkin(skin: DLCUISkin) = {
      val DLCUISkin(name, set, skinPlatform, includeImports, files, standalone) = skin
      val dirName = s"UISkin_${name}_${set}_$skinPlatform"
      <UISkin name={name} set={set} platform={skinPlatform}>
        <Skin>
          { uiFileInclude }
          { populateDirectory(dlcBasePath, dirName, files, "Directory", platform) }
          { if(includeImports) filesInclude else Seq() }
        </Skin>
      </UISkin>
    }

    if(dlcData.data.uiOnlyFiles.nonEmpty) {
      val uiFilesDirectory = platform.resolve(dlcBasePath, "UI_Files")
      Files.createDirectories(uiFilesDirectory)
      for((name, file) <- dlcData.data.uiOnlyFiles) IOUtils.writeFile(platform.resolve(uiFilesDirectory, name), file)
    }

    val (globalIncludes  , globalDirectory  ) = writeIncludes("GlobalImports"  , dlcData.data.globalIncludes  )
    val (gameplayIncludes, gameplayDirectory) = writeIncludes("GameplayImports", dlcData.data.gameplayIncludes)
    IOUtils.writeXML(platform.resolve(dlcBasePath, s"$nameString.Civ5Pkg"), <Civ5Package>
      {commonHeader(dlcData.manifest)}

      {globalIncludes}
      <Gameplay>
        {gameplayIncludes}
        {filesInclude}
        {mapIncludes}
        {globalDirectory}
        {gameplayDirectory}
      </Gameplay>

      { for(skin <- dlcData.data.uiSkins if skin.standalone.isEmpty) yield writeUISkin(skin) }
    </Civ5Package>)

    for(skin <- dlcData.data.uiSkins if skin.standalone.nonEmpty) {
      val manifest = skin.standalone.get
      val nameString = nameStringFromManifest(manifest)
      IOUtils.writeXML(platform.resolve(dlcBasePath, s"$nameString.Civ5Pkg"), <Civ5Package>
        {commonHeader(manifest, includeValidDlcKey = false)}
        {writeUISkin(skin)}
      </Civ5Package>)
    }

    val uuid_string = dlcData.manifest.uuid.toString.replace("-", "").toUpperCase(Locale.ENGLISH)
    languageFilePath.foreach(languagePath =>
      IOUtils.writeXML(languagePath, <GameData>
        {
          languageList.flatMap(x =>
            <NODE>
              {
                (dlcData.manifest +: dlcData.data.uiSkins.flatMap(_.standalone.toSeq)).flatMap(manifest =>
                  <Row Tag={s"TXT_KEY_${uuid_string}_NAME"}>
                    <Text>{manifest.shortName}</Text>
                  </Row>
                  <Row Tag={s"TXT_KEY_${uuid_string}_DESCRIPTION"}>
                    <Text>{manifest.name}</Text>
                  </Row>
                )
              }
            </NODE>.copy(label = s"Language_$x")
          )
        }
        {dlcData.data.globalTextData}
      </GameData>)
    )
  }
}
