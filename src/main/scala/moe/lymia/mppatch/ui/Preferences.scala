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

object Preferences {
  trait PreferenceSerializer[T] {
    def encode(t: T): String
    def decode(s: String): T
  }
  case class SimplePreferenceSerializer[T](encodeF: T => String, decodeF: String => T) extends PreferenceSerializer[T] {
    def encode(t: T): String = encodeF(t)
    def decode(s: String): T = decodeF(s)
  }

  implicit val StringPreference  = SimplePreferenceSerializer[String](identity, identity)
  implicit val BooleanPreference = SimplePreferenceSerializer[Boolean](_.toString, _.toBoolean)

  val prefs = java.util.prefs.Preferences.userNodeForPackage(getClass)
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

  val installationDirectory  = new PreferenceKey[String]("installationDirectory")
  val enableDebug            = new PreferenceKey[Boolean]("enableDebug", false)
  val enableLogging          = new PreferenceKey[Boolean]("enableLogging", true)
  val enableMultiplayerPatch = new PreferenceKey[Boolean]("enableMultiplayerPatch", true)
  val enableLuaJIT           = new PreferenceKey[Boolean]("enableLuaJIT", true)
}
