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

package moe.lymia.mppatch.ui

import java.util.Locale
import javax.swing.JFrame

object Preferences extends LaunchFrameError {
  trait PreferenceSerializer[T] {
    def encode(t: T): String
    def decode(s: String): T
  }
  case class SimplePreferenceSerializer[T](encodeF: T => String, decodeF: String => T) extends PreferenceSerializer[T] {
    def encode(t: T): String = encodeF(t)
    def decode(s: String): T = decodeF(s)
  }

  private implicit val StringPreference: SimplePreferenceSerializer[String] =
    SimplePreferenceSerializer[String](identity, identity)
  private implicit val BooleanPreference: SimplePreferenceSerializer[Boolean] =
    SimplePreferenceSerializer[Boolean](_.toString, _.toBoolean)
  private implicit val IntPreference: SimplePreferenceSerializer[Int] =
    SimplePreferenceSerializer[Int](_.toString, _.toInt)

  private case class SeqPreferenceImpl[T: PreferenceSerializer]() extends PreferenceSerializer[Seq[T]] {
    override def encode(t: Seq[T]): String =
      t.map(x => implicitly[PreferenceSerializer[T]].encode(x).replace("|", "|X").replace("_", "__").replace("|", "_"))
        .mkString("|")
    override def decode(s: String): Seq[T] = s
      .split("\\|")
      .toSeq
      .map(x => x.replace("_X", "|").replace("__", "_"))
      .map(x => implicitly[PreferenceSerializer[T]].decode(x))
  }
  private implicit def SeqPreference[T](implicit root: PreferenceSerializer[T]): SeqPreferenceImpl[T] =
    SeqPreferenceImpl()

  private val prefs = java.util.prefs.Preferences.userNodeForPackage(getClass)
  class PreferenceKey[T: PreferenceSerializer](val name: String, default: => T) {
    def this(name: String) = this(name, sys.error("preference " + name + " not set"))

    private val encoder = implicitly[PreferenceSerializer[T]]

    def hasValue = prefs.get(name, null) != null
    def clear()  = prefs.remove(name)
    def valueOption = try {
      val pref = prefs.get(name, null)
      if (pref == null) None else Some(encoder.decode(pref))
    } catch {
      case _: Exception => None
    }
    def value         = valueOption.fold(default)(identity)
    def value_=(t: T) = prefs.put(name, encoder.encode(t))
  }

  private val preferencesVersion = new PreferenceKey[Int]("preferenceVersion")
  val installationDirs           = new PreferenceKey[Seq[String]]("installationDirs")

  val legacyInstallationDirectory  = new PreferenceKey[String]("installationDirectory")
  val legacyEnableDebug            = new PreferenceKey[Boolean]("enableDebug", false)
  val legacyEnableLogging          = new PreferenceKey[Boolean]("enableLogging", true)
  val legacyEnableMultiplayerPatch = new PreferenceKey[Boolean]("enableMultiplayerPatch", true)
  val legacyEnableLuaJIT           = new PreferenceKey[Boolean]("enableLuaJIT", true)

  private def hasLegacyValues: Boolean =
    legacyInstallationDirectory.hasValue || legacyEnableDebug.hasValue || legacyEnableLogging.hasValue ||
      legacyEnableMultiplayerPatch.hasValue || legacyEnableLuaJIT.hasValue
  def updatePreferences(): Unit =
    if (!preferencesVersion.hasValue && hasLegacyValues) {
      preferencesVersion.value = 1
    } else if (preferencesVersion.hasValue && preferencesVersion.value != 1) {
      if (confirmDialog("error.configVersionTooNew")) {
        preferencesVersion.value = 1
      } else {
        throw new InstallerException("Cancelling configuration downgrade", null)
      }
    } else {
      // do nothing
    }
}
