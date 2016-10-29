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

package moe.lymia.mppatch.ui

object Preferences {
  trait PreferenceSerializer[T] {
    def encode(t: T): String
    def decode(s: String): T
  }
  case class SimplePreferenceSerializer[T](encodeF: T => String, decodeF: String => T) {
    def encode(t: T): String = encodeF(t)
    def decode(s: String): T = decodeF(s)
  }

  implicit val StringPreference = SimplePreferenceSerializer[String](identity, identity)

  val prefs = java.util.prefs.Preferences.userNodeForPackage(getClass)
  case class PreferenceKey[T : PreferenceSerializer](name: String, default: T) {
    private val encoder = implicitly[PreferenceSerializer[T]]
    private val defaultString = encoder.encode(default)

    def value = try {
      encoder.decode(prefs.get(name, defaultString))
    } catch {
      case _: Exception => default
    }
    def value_=(t: T) = prefs.put(name, encoder.encode(t))
  }
}
