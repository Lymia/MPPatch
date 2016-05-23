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

package moe.lymia.multiverse.core.generator

import java.util.Locale
import javax.xml.bind.DatatypeConverter

import moe.lymia.multiverse.core.data._
import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.Crypto
import moe.lymia.multiverse.util.res.LuaCode.quoteLuaString
import moe.lymia.multiverse.util.res.{LuaCode, VersionInfo}

import scala.xml.{Elem, Node}

private object AudioScriptTranslator {
  private val mapBoolean = (s: String) => {
    // TODO: Figure out how Civ V actually normalizes booleans inserted into the database
    if(s.trim.toInt != 0) "True" else "False"
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
      // I'm pretty sure DynamicResident doesn't actually exist, and modders are copying other modders' mistakes.
      // Probably ends up defaulting to STREAMED or something. I'd need to investigate and find out.
      if(s.equalsIgnoreCase("DynamicResident"))  "DYNAMIC_RES"
      else if(s.equalsIgnoreCase("Dynamic_Res")) "DYNAMIC_RES"
      else if(s.equalsIgnoreCase("Resident"))    "RESIDENT"
      else if(s.equalsIgnoreCase("Streamed"))    "STREAMED"
      else                                       "STREAMED" // TODO: Confirm that this is, indeed, the default.
    })
  ))

  def translateAudioScript(event: String, fileTagName: String, tagName: String, data: Seq[Node],
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
}

private object ActionProcessor {
  private val audioTables = Set("Audio_SoundScapeElementScripts", "Audio_SoundScapeElements", "Audio_SoundScapes")

  private def ifNonEmpty[T](s: Seq[Node], t: T) = if(s.nonEmpty) Some(t) else None
  private def translateDataSource(d: ModDataSource) = d match {
    case ModSqlSource(sql) => Seq(
      <__MVMM_PATCH_IGNORE>
        <!-- This (hopefully) triggers an error for players who have an unpatched game binary -->
        <Please_install_the_CvGameDatabase_patch_for_Multiverse_Mod_Manager>
        </Please_install_the_CvGameDatabase_patch_for_Multiverse_Mod_Manager>
      </__MVMM_PATCH_IGNORE>,
      <__MVMM_PATCH_RAWSQL> {
        DatatypeConverter.printBase64Binary(sql.replace("\r\n","\n").getBytes("UTF-8"))
      } </__MVMM_PATCH_RAWSQL>
    )
    case ModXmlSource(xml) => xml.child.filter(_.isInstanceOf[Elem])
  }

  case class ProcessedActions(gameplayIncludes: Seq[DLCInclude], globalIncludes: Seq[DLCInclude],
                              scripts: Seq[String])
  def translateActions(actions: Seq[ModAction]) = {
    // _1 = gameplay include, _2 = global include, _3 = script
    val data = for(action <- actions) yield action match {
      case ModUpdateUserDataAction(source) =>
        val data = translateDataSource(source)
        (Seq(ifNonEmpty(data, DLCInclude("GameData", <GameData>{data}</GameData>))), Seq(), Seq())
      case ModUpdateDatabaseAction(source) =>
        val xmlBody = translateDataSource(source)
        // FIXME This can't handle language data updated via SQL, if someone does that for some reason
        val (languageData  , remaining0) = xmlBody.partition(_.label.startsWith("Language_"))
        val (audio2DScripts, remaining1) = remaining0.partition(_.label == "Audio_2DSounds")
        val (audio3DScripts, remaining2) = remaining1.partition(_.label == "Audio_3DSounds")
        val (audioDefines  , remaining3) = remaining2.partition(_.label == "Audio_Sounds")
        val (audioData     , gameData)   = remaining3.partition(x => audioTables.contains(x.label))

        val audio2DFile = AudioScriptTranslator.translateAudioScript(
          "AudioScripts2D", "Script2DFile", "Script2DSound", audio2DScripts, AudioScriptTranslator.audioScriptMap)
        val audio3DFile = AudioScriptTranslator.translateAudioScript(
          "AudioScripts3D", "Script3DFile", "Script3DSound", audio2DScripts, AudioScriptTranslator.audioScriptMap)
        val audioDefineFile = AudioScriptTranslator.translateAudioScript(
          "AudioDefines", "AudioDefinesFile", "SoundData", audioDefines, AudioScriptTranslator.audioDefineMap)

        (Seq(ifNonEmpty(languageData  , DLCInclude("TextData", <GameData>{languageData}</GameData>)),
             ifNonEmpty(gameData      , DLCInclude("GameData", <GameData>{gameData}</GameData>))),
         Seq(ifNonEmpty(audioDefines  , audioDefineFile),
             ifNonEmpty(audio2DScripts, audio2DFile),
             ifNonEmpty(audio3DScripts, audio3DFile)), Seq())
      case ModExecuteScriptAction(script) => (Seq(), Seq(), Seq(script))
    }
    ProcessedActions(data.flatMap(_._1.flatMap(_.toSeq)), data.flatMap(_._2.flatMap(_.toSeq)), data.flatMap(_._3))
  }
}

object ModTranslator {
  def translateModUUID(modData: ModData) =
    Crypto.sha1_uuid(DlcUUID.MOD_UUID_NAMESPACE, Crypto.uuidToBytes(modData.manifest.uuid))
  def translatedModPackUUID(modData: Seq[ModData]) = {
    val fields = modData.map(x => x.manifest.uuid.toString+":"+x.manifest.version)
    fields.sorted.reduce(_ + "\n" + _)
  }

  val UISkin_BaseGame      = DLCUISkin("BaseGame"         , "BaseGame"  , "Common", includeImports = true, Map())
  val UISkin_GodsAndKings  = DLCUISkin("Expansion1Primary", "Expansion1", "Common", includeImports = true, Map())
  val UISkin_BraveNewWorld = DLCUISkin("Expansion2Primary", "Expansion2", "Common", includeImports = true, Map())

  private def baseName(name: String) = name.split("[/\\\\]").last
  private def normalizeName(name: String) = baseName(name).toLowerCase(Locale.ENGLISH)

  def translateModToDLC(modData: ModData, priority: Int, targetUISkin: DLCUISkin, platform: Platform) = {
    val translatedUUID = translateModUUID(modData)

    val data = ActionProcessor.translateActions(modData.data.onCreateUserData ++ modData.data.onModActivated)

    val mapEntryPoints =
      modData.data.entryPoints.filter(_.event.equalsIgnoreCase("MapScript")).map(x => normalizeName(x.file))
    val mapScripts = mapEntryPoints.map(x =>
      DLCMap("lua", ImportFromMemory(("include "+quoteLuaString(x)).getBytes("UTF8"))))

    val manifest = {
      val out = new StringBuilder()
      out.append("-- Generated by Multiverse Mod Manager\n")
      out.append("local uuid        = "+quoteLuaString(translatedUUID.toString)+"\n")
      out.append("local version     = "+modData.manifest.version+"\n")
      out.append("local name        = "+quoteLuaString(modData.manifest.name.toString)+"\n")
      out.append("\n")
      out.append("local assetPrefix = _mvmm.getAssetPrefix(function() end)")
      out.append("\n")
      out.append("local rawProperties = {}\n")
      for((name, value) <- modData.manifest.rawProperties) {
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
                 "                    { properties = rawProperties,"+
                 "                      assetPrefix = assetPrefix,"+
                 "                      entryPoints = entryPoints })\n")
      out.append("end\n")

      Map("mvmm_modmanifest_"+translatedUUID.toString.replace("-", "")+".lua" ->
          ImportFromMemory(out.toString().getBytes("UTF8")))
    }

    // Weird consideration... could we write values into ModEntryPoints/etc in an XML file, instead of the manifest?
    // If that works, it would obviate our need for a core mod and manifest files. But I doubt it'd be very clean
    // considering that the code would /not/ be built around someone poking in those tables from game data files.

    val fileList = modData.data.importedFiles.map(x => x.copy(_1 = baseName(x._1), _2 =
      if(x._1.toLowerCase(Locale.ENGLISH).endsWith(".lua")) {
        ImportFromMemory(LuaCode.core_entrypoint_hook.getBytes("UTF8") ++ x._2.data)
      } else x._2)) ++ manifest

    DLCData(DLCManifest(translatedUUID, modData.manifest.version, 350 + priority,
                        modData.manifest.name, modData.manifest.name),
            DLCGameplay(data.gameplayIncludes, data.globalIncludes,
                        mapScripts, fileList, Seq(targetUISkin)))
  }
}
