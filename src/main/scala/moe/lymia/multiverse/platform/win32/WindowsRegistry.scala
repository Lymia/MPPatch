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

package moe.lymia.multiverse.platform.win32

import java.util.prefs.Preferences

object WindowsRegistry {
  private val KEY_ALL_ACCESS = 0xf003f
  private val KEY_READ       = 0x20019

  private type Dynamic = DynamicReflectiveProxy
  private def toCString(s: String) = (s.getBytes :+ 0.toByte).toArray
  private def fromCString(s: Array[Byte]) = new String(s).replaceAll("\u0000.*", "")
  private def withKey[A](hive: Hive, key: String, access: Int)(f: Int => A) = {
    val Array(handle, success) = hive.h.WindowsRegOpenKey(hive.hiveId, toCString(key), access).get[Array[Int]]
    if(success == 0) try {
      Some(f(handle))
    } catch {
      case t: Throwable => None
    } finally {
      hive.h.WindowsRegCloseKey(handle)
    } else None
  }

  sealed trait RegistryKeyType[T] {
    private[WindowsRegistry] def readKey(hive: Hive, key: String, value: String): Option[T]
    private[WindowsRegistry] def writeKey(hive: Hive, key: String, value: String, data: T)
  }
  implicit case object RegistryStringKey extends RegistryKeyType[String] {
    private[WindowsRegistry] def readKey(hive: Hive, key: String, value: String) =
      withKey(hive, key, KEY_READ) { handle =>
        fromCString(hive.h.WindowsRegQueryValueEx(handle, toCString(value)).get[Array[Byte]])
      }
    private[WindowsRegistry] def writeKey(hive: Hive, key: String, value: String, data: String) =
      withKey(hive, key, KEY_ALL_ACCESS) { handle =>
        hive.h.WindowsRegSetValueEx(handle, toCString(value), toCString(data))
      }
  }

  case class Hive private[WindowsRegistry] (
      private[WindowsRegistry] val hiveId: Int,
      private[WindowsRegistry] val hrName: String,
      private[WindowsRegistry] val h: Dynamic) {

    assert(System.getProperty("os.name").toLowerCase.contains("windows"), "Windows registry not available.")

    def readKey[T: RegistryKeyType](key: String, value: String) =
      implicitly[RegistryKeyType[T]].readKey(this, key, value)
    def apply[T: RegistryKeyType](key: String, value: String) = readKey[T](key, value)

    def writeKey[T: RegistryKeyType](key: String, value: String, data: T) =
      implicitly[RegistryKeyType[T]].writeKey(this, key, value, data)
    def update[T: RegistryKeyType](key: String, value: String, data: T) = writeKey[T](key, value, data)
  }
  val HKEY_CURRENT_USER  = Hive(0x80000001, "HKEY_CURRENT_USER",
                                DynamicReflectiveProxy(Preferences.systemRoot()))
  val HKEY_LOCAL_MACHINE = Hive(0x80000002, "HKEY_LOCAL_MACHINE",
                                DynamicReflectiveProxy(Preferences.userRoot()))

}