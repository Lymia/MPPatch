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

package moe.lymia.multiverse.core.generator

import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}
import javax.xml.bind.DatatypeConverter

import moe.lymia.multiverse.core.mods._
import moe.lymia.multiverse.core.dlc._
import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.Crypto
import moe.lymia.multiverse.util.res.LuaCode.quoteLuaString
import moe.lymia.multiverse.util.res.{LuaCode, VersionInfo}

import scala.xml.{Elem, Node}

object DlcUUID {
  val BASE_DLC_UUID           = UUID.fromString("aa75946c-7fca-4166-874c-de18ecd39162")
  val MOD_UUID_NAMESPACE      = UUID.fromString("28b620c3-93a8-4b4d-9cee-58bc71640a58")
  val MOD_PACK_UUID_NAMESPACE = UUID.fromString("0b3e3322-2dee-45c1-9f4c-091136c7cf29")
}

object BaseDLC {
  val patchList = Seq(
    "InstanceManager.lua"   -> "UI/InstanceManager.lua",
    "GameplayUtilities.lua" -> "Gameplay/Lua/GameplayUtilities.lua")
  def generateBaseDLC(civBaseDirectory: Path, platform: Platform) = {
    val patchedFileList = (for((file, realPath) <- patchList) yield {
      val targetPath = platform.resolve(civBaseDirectory, platform.assetsPath, realPath)
      (file, LuaCode.core_entrypoint_hook.getBytes("UTF8") ++ Files.readAllBytes(targetPath))
    }).toMap
    DLCData(DlcUUID.BASE_DLC_UUID, 1, 250,
            "Multiverse - Base DLC", "Base DLC for Multiverse",
            Nil, Nil, Nil, patchedFileList ++ LuaCode.core_library, Nil)
  }
}

object ModTranslator {
  val audioTables = Set("Audio_SoundScapeElementScripts", "Audio_SoundScapeElements", "Audio_SoundScapes")

  def translateModUUID(modData: ModData) =
    Crypto.sha1_uuid(DlcUUID.MOD_UUID_NAMESPACE, Crypto.uuidToBytes(modData.manifest.uuid))
  def translatedModPackUUID(modData: Seq[ModData]) = {
    val fields = modData.map(x => x.manifest.uuid.toString+":"+x.manifest.version)
    fields.sorted.reduce(_ + "\n" + _)
  }

  val UISkin_BaseGame      = DLCUISkin("BaseGame"         , "BaseGame"  , "Common", includeImports = true, Map())
  val UISkin_GodsAndKings  = DLCUISkin("Expansion1Primary", "Expansion1", "Common", includeImports = true, Map())
  val UISkin_BraveNewWorld = DLCUISkin("Expansion2Primary", "Expansion2", "Common", includeImports = true, Map())

  private def prepareCData(s: String) =
    scala.xml.Unparsed("<![CDATA["+s.replace("]]>", "]]]]><![CDATA[>")+"]]>")
  private def translateDataSource(d: ModDataSource, platform: Platform) = d match {
    case ModSqlSource(sql) => Seq(
      <__MVMM_PATCH_IGNORE>
        <!-- This (hopefully) triggers an error for players who have an unmodded CvGameDatabase file -->
        <Please_install_the_CvGameDatabase_patch_for_Multiverse_Mod_Manager>
        </Please_install_the_CvGameDatabase_patch_for_Multiverse_Mod_Manager>
      </__MVMM_PATCH_IGNORE>,
      <__MVMM_PATCH_RAWSQL>{
        DatatypeConverter.printBase64Binary(sql.replace("\r\n","\n").getBytes("UTF-8"))
      }</__MVMM_PATCH_RAWSQL>
    )
    case ModXmlSource(xml) => xml.child.filter(_.isInstanceOf[Elem])
  }

  private val mapBoolean = (s: String) => {
    val i = try { s.toInt } catch { case _: NumberFormatException => 1 }
    val isTrue = s.equalsIgnoreCase("true") || i != 0
    if(isTrue) "True" else "False"
  }
  val audioScriptMap = {
    val stringFields  = Seq("ScriptID", "SoundID", "SoundType", "StartPosition", "EndPosition", "Channel")
    val booleanFields = Seq("Looping", "StartFromRandomPosition", "OnlyTriggerOnUnitRuns", "DontPlay",
                            "DontTriggerDuplicates", "DontTriggerDuplicatesOnUnits", "IsMusic")
    val integerFields = Seq("MaxVolume", "MinVolume", "DontPlayMoreThan", "PercentChanceOfPlaying",
                            "MinTimeMustNotPlayAgain", "MaxTimeMustNotPlayAgain", "MinTimeDelay", "MaxTimeDelay",
                            "PitchChangeDown", "PitchChangeUp", "Priority", "MinRightPan", "MaxRightPan",
                            "MinLeftPan", "MaxLeftPan", "MinVelocity", "MaxVelocity")
    val floatFields   = Seq("DryLevel", "WetLevel", "TaperSoundtrackVolume", "DistanceFromListener", "MinDistance",
                            "CutoffDistance")

    ((stringFields.map(x => (x, x)) ++ booleanFields.map(x => (x, "b"+x)) ++
        integerFields.map(x => (x, "i"+x)) ++ floatFields.map(x => (x, "f"+x))).toMap,
      booleanFields.map(x => (x, mapBoolean)).toMap)
  }
  val audioDefineMap = (Map(
    "SoundID"                      -> "SoundID",
    "Filename"                     -> "Filename",
    "LoadType"                     -> "LoadType",
    "OnlyLoadOneVariationEachTime" -> "bOnlyLoadOneVariationEachTime",
    "DontCache"                    -> "bDontCache"
  ), Map(
    "OnlyLoadOneVariationEachTime" -> mapBoolean,
    "DontCache"                    -> mapBoolean,
    "LoadType"                     -> ((s: String) => {
      // I'm pretty sure DynamicResident doesn't actually exist, and modders are just being stupid.
      // Probably ends up defaulting to STREAMED or something. I'd need to investigate and find out.
      if(s.equalsIgnoreCase("DynamicResident"))  "DYNAMIC_RES"
      else if(s.equalsIgnoreCase("Dynamic_Res")) "DYNAMIC_RES"
      else if(s.equalsIgnoreCase("Resident"))    "RESIDENT"
      else if(s.equalsIgnoreCase("Streamed"))    "STREAMED"
      else                                       "STREAMED"
    })
  ))

  private def baseName(name: String) = name.split("[/\\\\]").last
  private def normalizeName(name: String) = baseName(name).toLowerCase(Locale.ENGLISH)

  def generateAudioScriptData(event: String, fileTagName: String, tagName: String, data: Seq[Node],
                              definedData: (Map[String, String], Map[String, String => String])) = {
    val (defineMap, mapValue) = definedData
    DLCInclude(event, <NODE> {
      <NODE> {
        data.flatMap(_ \ "Row").map { elem =>
          <NODE> {
            elem.child.flatMap{ child =>
              val data = mapValue.get(child.label).fold(child.text)(_(child.text))
              defineMap.get(child.label).map(x => <NODE>{data}</NODE>.copy(label = x))
            }
          } </NODE>.copy(label = tagName)
        }
      } </NODE>.copy(label = tagName+"s")
    } </NODE>.copy(label = fileTagName))
  }

  def ifNonEmpty[T](s: Seq[Node], t: T) = if(s.nonEmpty) Some(t) else None

  def translateModToDLC(modData: ModData, priority: Int, targetUISkin: DLCUISkin, platform: Platform) = {
    val translatedUUID = translateModUUID(modData)
    val fullDescription = modData.manifest.teaser+"\n\n-----\n\n"+modData.manifest.description

    val data = for(action <- modData.data.onCreateUserData ++ modData.data.onModActivated) yield action match {
      case ModUpdateUserDataAction(source) =>
        val data = translateDataSource(source, platform)
        (Seq(ifNonEmpty(data, DLCInclude("GameData", <GameData>{data}</GameData>))), Seq(), Seq())
      case ModUpdateDatabaseAction(source) =>
        val xmlBody = translateDataSource(source, platform)
        // FIXME This can't handle language data updated via SQL, if someone does that for some reason
        val (languageData  , remaining0) = xmlBody.partition(_.label.startsWith("Language_"))
        val (audio2DScripts, remaining1) = remaining0.partition(_.label == "Audio_2DSounds")
        val (audio3DScripts, remaining2) = remaining1.partition(_.label == "Audio_3DSounds")
        val (audioDefines  , remaining3) = remaining2.partition(_.label == "Audio_Sounds")
        val (audioData     , gameData) = remaining3.partition(x => audioTables.contains(x.label))

        val audio2DFile =
          generateAudioScriptData("AudioScripts2D", "Script2DFile", "Script2DSound", audio2DScripts, audioScriptMap)
        val audio3DFile =
          generateAudioScriptData("AudioScripts3D", "Script3DFile", "Script3DSound", audio2DScripts, audioScriptMap)
        val audioDefineFile =
          generateAudioScriptData("AudioDefines", "AudioDefinesFile", "SoundData", audioDefines, audioDefineMap)

        (Seq(ifNonEmpty(languageData  , DLCInclude("TextData", <GameData>{languageData}</GameData>)),
             ifNonEmpty(gameData      , DLCInclude("GameData", <GameData>{gameData}</GameData>))),
         Seq(ifNonEmpty(audioDefines  , audioDefineFile),
             ifNonEmpty(audio2DScripts, audio2DFile),
             ifNonEmpty(audio3DScripts, audio3DFile)), Seq())
      case ModExecuteScriptAction(script) => (Seq(), Seq(), Seq(script))
    }

    val mapEntryPoints =
      modData.data.entryPoints.filter(_.event.equalsIgnoreCase("MapScript")).map(x => normalizeName(x.file))
    val mapScripts = mapEntryPoints.map(x => DLCMap("lua", ("include "+quoteLuaString(x)).getBytes("UTF8")))

    val manifest: Map[String, Array[Byte]] = {
      val out = new StringBuilder()
      out.append("-- Generated by Multiverse Mod Manager\n")
      out.append("local uuid        = "+quoteLuaString(translatedUUID.toString)+"\n")
      out.append("local version     = "+modData.manifest.version+"\n")
      out.append("local name        = "+quoteLuaString(modData.manifest.name.toString)+"\n")
      out.append("\n")
      out.append("local assetPrefix = _mvmm.getAssetPrefix(function() end)")
      out.append("\n")
      out.append("local rawProperties = {}\n")
      for((name, value) <- modData.rawProperties) {
        out.append("rawProperties[ "+quoteLuaString(name)+" ] = "+quoteLuaString(value)+"\n")
      }
      out.append("\n")
      out.append("local entryPoints = {}\n")
      for(event <- modData.data.entryPoints.map(_.event).toSet : Set[String]) {
        out.append("entryPoints[ " + quoteLuaString(event) + " ] = {\n")
        for (ModEntryPoint(_, name, description, path) <- modData.data.entryPoints.filter(_.event == event)) {
          out.append("  {\n")
          out.append("    name        = "+quoteLuaString(name       )+",\n")
          out.append("    description = "+quoteLuaString(description)+",\n")
          out.append("    path        = "+quoteLuaString(path       )+",\n")
          out.append("  },\n")
        }
        out.append("}\n")
      }
      out.append("\n")
      out.append("if _mvmm and not _mvmm.disabled then\n")
      out.append("  _mvmm.registerMod("+VersionInfo.patchCompat+", uuid, version, name, " +
                                     "{ properties = rawProperties,"+
                                      " assetPrefix = assetPrefix,"+
                                      " entryPoints = entryPoints })\n")
      out.append("end\n")

      Map("mvmm_modmanifest_"+translatedUUID.toString.replace("-", "")+".lua" -> out.toString().getBytes("UTF8"))
    }

    // Weird consideration... could we write values into ModEntryPoints/etc in an XML file, instead of the manifest?
    // If that works, it would obviate our need for a core mod and manifest files. But I doubt it'd be very clean
    // considering that the code would /not/ be built around someone poking in those tables from game data files.

    val fileList = modData.data.fileList.map(x => x.copy(_1 = baseName(x._1), _2 =
      if(x._1.toLowerCase(Locale.ENGLISH).endsWith(".lua")) {
        LuaCode.core_entrypoint_hook.getBytes("UTF8") ++ x._2
      } else x._2)) ++ manifest

    DLCData(translatedUUID, modData.manifest.version, 300 + priority, modData.manifest.name, fullDescription,
            data.flatMap(_._1.flatMap(_.toSeq)), data.flatMap(_._2.flatMap(_.toSeq)),
            mapScripts, fileList, Seq(targetUISkin))
  }
}
