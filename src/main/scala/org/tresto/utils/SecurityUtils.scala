//package org.tresto.callcenter.util
//
//import org.tresto.utils.security.SecHelpers
//
//object SecurityUtils {
//  val secKey = SecHelpers.makeTripleDESKey //This will bu unique, and will be constant for every run of the server (will not change until server restart)
//
//  val crapRemoverRegex = "[^\\p{L}\\p{N}]" //Leave only characters and numbers, no special characters
//
//  def normalizeClassName(pin: String): String = {
//    pin.substring(pin.lastIndexOf(".") + 1).replaceAll("$", "")
//  }
//  /**
//   * We want to use url params sometimes like ?id=1&name=pista
//   * But passing around id's is extreamly dangerous, so we will encode them like: id=ZXRSNFFuYTlWWE9LOUpXcU9ocms4dz09
//   * So pass in the value, and we will return the encrypted string
//   */
//  def getParamEncripted(in: String, key: Array[Byte] = secKey): String = {
//    val x1 = SecHelpers.tripleDESEncrypt(in, key)
//    val ret = java.net.URLEncoder.encode(x1, "UTF-8") //Make it browsery
//    //log.debug("getParamEncripted: " + in + " => " + x1 + " => " + ret)
//    return ret
//  }
//  def getParamDecripted(in: String, key: Array[Byte] = secKey): String = {
//    //val x1 = java.net.URLDecoder.decode(in,"UTF-8")  //Lift will automatically URL Decode the link
//    val ret = SecHelpers.tripleDESDecrypt(in, key)
//    //log.debug("getParamDecripted: " + in + " => " + ret)
//    return ret
//  }
//
//}