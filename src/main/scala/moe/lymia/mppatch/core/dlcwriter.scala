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

package moe.lymia.mppatch.core

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}

import moe.lymia.mppatch.util.IOUtils
import moe.lymia.mppatch.util.common.Crypto

import scala.xml.{Node, NodeSeq}

case class DLCUISkin(name: String, set: String, platform: String)
case class DLCManifest(uuid: UUID, version: Int, priority: Int, shortName: String, name: String)
case class DLCGameplay(textData: Map[String, Node] = Map(),
                       uiFiles: Map[String, Map[String, Array[Byte]]] = Map(), uiSkins: Seq[DLCUISkin] = Nil)
case class DLCData(manifest: DLCManifest, data: DLCGameplay)

private object DLCKey {
  private val staticInterlace =
    Seq(0x1f, 0x33, 0x93, 0xfb, 0x35, 0x0f, 0x42, 0xc7,
        0xbd, 0x50, 0xbe, 0x7a, 0xa5, 0xc2, 0x61, 0x81) map (_.toByte)
  private val staticInterlaceStream = Stream.from(0) map (x => staticInterlace(x%staticInterlace.length))
  private def interlaceData(data: Array[Byte]) =
    data.zip(staticInterlaceStream).flatMap(x => Seq(x._1, x._2))

  private def encodeLe32(i: Int) =
    Array(i&0xFF, (i>>8)&0xFF, (i>>16)&0xFF, (i>>24)&0xFF)
  private def encodeBe32(i: Int) = encodeLe32(i).reverse
  private def encodeLe16(i: Int) =
    Array(i&0xFF, (i>>8)&0xFF)
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

  private def populateDirectory(dlcBasePath: String, dirName: String, data: Map[String, Array[Byte]],
                                platform: Platform) =
    (data.map(x => (s"$dlcBasePath/$dirName/${x._1}", x._2)), <Directory>{dirName}</Directory>)

  def writeDLC(dlcBasePath: String, languageDirPath: Option[String], dlcData: DLCData, platform: Platform) = {
    var id = 0
    def newId() = {
      id = id + 1
      id
    }

    val nameString = s"${dlcData.manifest.uuid.toString.replace("-", "")}_v${dlcData.manifest.version}"

    val uiFiles = dlcData.data.uiFiles.map(y => y._2.map(x => (s"$dlcBasePath/${y._1}/${x._1}", x._2))).reduce(_ ++ _)

    def writeUISkin(skin: DLCUISkin) = {
      val DLCUISkin(name, set, skinPlatform) = skin
      val dirName = s"UISkin_${name}_${set}_$skinPlatform"
      <UISkin name={name} set={set} platform={skinPlatform}>
        <Skin>
          { dlcData.data.uiFiles.map(x => <Directory>{x._1}</Directory>) }
        </Skin>
      </UISkin>
    }
    val newFiles = Map(s"$dlcBasePath/$nameString.Civ5Pkg" -> IOUtils.writeXMLBytes(
      <Civ5Package>
        <GUID>{s"{${dlcData.manifest.uuid}}"}</GUID>
        <Version>{dlcData.manifest.version.toString}</Version>
        <Name>{languageValues(s"${dlcData.manifest.uuid.toString.replace("-", "")}_v${dlcData.manifest.version}")}</Name>
        <Description>{languageValues(dlcData.manifest.name)}</Description>
        <Priority>{dlcData.manifest.priority.toString}</Priority>

        <SteamApp>99999</SteamApp>
        <Ownership>FREE</Ownership>
        <PTags>
          <Tag>Version</Tag>
          <Tag>Ownership</Tag>
        </PTags>
        <Key>{DLCKey.key(dlcData.manifest.uuid, Seq(99999), dlcData.manifest.version.toString, "FREE")}</Key>
        { for(skin <- dlcData.data.uiSkins) yield writeUISkin(skin) }
      </Civ5Package>
    ))

    val uuid_string = dlcData.manifest.uuid.toString.replace("-", "").toUpperCase(Locale.ENGLISH)
    val languageFiles = languageDirPath.fold(Map[String, Array[Byte]]()) { languagePath =>
      Map(s"$languagePath/${nameString}_DlcName.xml", IOUtils.writeXMLBytes(
        <GameData> {
          languageList.flatMap(x =>
            <NODE> {
              <Row Tag={s"TXT_KEY_${uuid_string}_NAME"}>
                <Text>{dlcData.manifest.shortName}</Text>
              </Row>
              <Row Tag={s"TXT_KEY_${uuid_string}_DESCRIPTION"}>
                <Text>{dlcData.manifest.name}</Text>
              </Row>
            } </NODE>.copy(label = s"Language_$x")
          )
        } </GameData>
      )) ++ dlcData.data.textData.map(x =>
        s"$languagePath/${nameString}_TextData_${x._1}.xml" -> IOUtils.writeXMLBytes(x._2))
    }
  }
}
