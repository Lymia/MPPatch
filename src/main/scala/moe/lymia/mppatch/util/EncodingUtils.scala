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

package moe.lymia.mppatch.util

import play.api.libs.json.{JsError, JsResult, JsSuccess}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

object EncodingUtils {
  private def digest(algorithm: String, data: Array[Byte]) = {
    val md   = MessageDigest.getInstance(algorithm)
    val hash = md.digest(data)
    hash
  }
  private def hexdigest(algorithm: String, data: Array[Byte]) =
    digest(algorithm, data).map(x => "%02x".format(x)).reduce(_ + _)
  private def b64digest(algorithm: String, data: Array[Byte]) =
    new String(Base64.getEncoder.encode(digest(algorithm, data)), StandardCharsets.UTF_8).replace("=", "")

  def md5_hex(data: Array[Byte])    = hexdigest("MD5", data)
  def sha1_hex(data: Array[Byte])   = hexdigest("SHA-1", data)
  def sha256_hex(data: Array[Byte]) = hexdigest("SHA-256", data)
  def sha512_hex(data: Array[Byte]) = hexdigest("SHA-512", data)

  def md5_b64(data: Array[Byte])    = b64digest("MD5", data)
  def sha1_b64(data: Array[Byte])   = b64digest("SHA-1", data)
  def sha256_b64(data: Array[Byte]) = b64digest("SHA-256", data)
  def sha512_b64(data: Array[Byte]) = b64digest("SHA-512", data)

  def md5(data: Array[Byte])    = digest("MD5", data)
  def sha1(data: Array[Byte])   = digest("SHA-1", data)
  def sha256(data: Array[Byte]) = digest("SHA-256", data)
  def sha512(data: Array[Byte]) = digest("SHA-512", data)

  def unwrapJson[T](json: JsResult[T]) = json match {
    case JsSuccess(value, _) => value
    case JsError(errors) => {
      SimpleLogger.error(s"JSON error encountered: $errors")
      sys.error("JSON error encountered")
    }
  }
}
