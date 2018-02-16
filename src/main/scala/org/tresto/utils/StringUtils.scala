package org.tresto.utils

object StringUtils {

  def null2EmptyString(x: String): String = {
    (x == null) match {
      case true => ""
      case false => x
    }
  }

  def isNullOrEmpty(x: String): Boolean = {
    if (x == null) {
      return true
    }
    if (x.length == 0) {
      return true
    }
    return false
  }

  def getFirstPartOfString(txt: String, length: Int): String = {
    (txt.length > length) match {
      case true => txt.substring(0, length)
      case false => txt
    }
  }

  def limitString(pin: String, len: Int = 50): String = {
    if (pin == null) {
      return ""
    }
    if (pin.length > len) pin.substring(0, len) + "..." else pin
  }

  def removeXMLTags(in:String):String = {
    in.replaceAll("<[^>]+>","")
  }

  val validIpAddressRegex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"""

  def isIpAddressValid(ip: String): Boolean = {
    if (ip.length < 7) { // 1.1.1.1
      return false
    }
    ip.trim().matches(validIpAddressRegex)
  }

  val validHostnameRegex = """^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"""
  def isHostNameValid(ip: String): Boolean = {
    ip.trim().matches(validHostnameRegex)
  }

  val validInteger = "^\\d+$"
  def isNumberValid(number: String): Boolean = {
    number.matches(validInteger)
  }
  def isPortValid(number: String): Boolean = {
    val m = number.trim().matches(validInteger)
    if (m == false) {
      return false
    }
    val n = number.toInt
    return (n > 0 && n < 65536)

  }

  val validFloat = "-?\\d+(\\.\\d+)?"
  def isFloatValid(number: String): Boolean = {
    number.matches(validInteger)
  }

  val validEmailRegex = """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$"""
  def isEmailValid(email: String): Boolean = {
    if (email == null) {
      return false
    }
    email.matches(validEmailRegex)
  }

  val phoneNumberCrapRegex = "[\\\\/\\+\\-\\s]"   //Replace \ / + - space
  /**
   * Many times people write phone numbers like this: +36-20 / 111 - 222
   */
  def phoneNumberUncrapper(str: String): String = {
    str.replaceAll(phoneNumberCrapRegex, "")
  }

  /**
   * @param KEY1:value1,KEY2:value2 SEE? , or ; devides key-value pairs
   * @returns lmap Map(KEY1 -> value1, KEY2 -> value2)
   */
  def string2SimpleHashMap(str: String): Map[String, String] = {
    val map = new scala.collection.mutable.HashMap[String, String]
    val kvs = str.split("[,;]")
    for (kv <- kvs) {
      val x = kv.split("[:=]")
      x.length match {
        case 1 => map.put(x(0), x(0))
        case 2 => map.put(x(0), unescapeHashMapSeparators(x(1)))
        case _ =>
      }
    }
    return map.toMap
  }

  /**
   * @param KEY1:value1,KEY2:value2;KEY1:value3,KEY2:value4;....    SEE? , devides key-value pairs  ; devides maps
   * @returns lmap List(Map(KEY1 -> value1, KEY2 -> value2), Map(KEY1 -> value3, KEY2 -> value4))
   */
  def string2HashMap(str: String): List[Map[String, String]] = {
    val lb = new scala.collection.mutable.ListBuffer[Map[String, String]]
    val list = str.split(";")
    for (l <- list) { //List[KEY1:value1,KEY2:value2]
      val map = new scala.collection.mutable.HashMap[String, String]
      val kvs = l.split(",")
      for (kv <- kvs) { //List[]
        val x = kv.split("[:=]")
        x.length match {
          case 1 => map.put(x(0), x(0))
          case 2 => map.put(x(0), unescapeHashMapSeparators(x(1)))
          case _ =>
        }
      }
      lb += map.toMap
    }
    lb.toList
  }

  /**
   * @param lmap List(Map(KEY1 -> value1, KEY2 -> value2), Map(KEY1 -> value3, KEY2 -> value4))
   * @returns KEY1:value1,KEY2:value2;KEY1:value3,KEY2:value4;....   SEE? , devides key-value pairs  ; devides maps
   *
   */
  def hashMap2String(lmap: List[Map[String, String]]): String = {
    if (lmap.length == 0) {
      return ""
    }
    if (lmap.head.size == 0) {
      return ""
    }
    var res = ""
    for (l <- lmap) {
      for (m <- l) {
        res = res + m._1 + ":" + escapeHashMapSeparators(m._2) + ","
      }
      res = res.substring(0, res.length() - 1) + ";" //Replace last comma with semicolon 
    }
    res = res.substring(0, res.length() - 1) //Remove last semicolon
    return res
  }

  def string2List(str: String): List[String] = {
    if (str == null) {
      return Nil
    }
    if (str.length() == 0) {
      return Nil
    }
    val list = str.split(",")
    list.toList
  }

  def list2String(list: List[String]): String = {
    if (list.length == 0) {
      return ""
    }
    if (list.head.size == 0) {
      return ""
    }
    val res = list.mkString(",")
    return res
  }

  def escapeHashMapSeparators(pin: String): String = {
    var res = pin.replace(",", "~C~")
    res = res.replace(";", "~S~")
    res = res.replace(":", "~L~")
    res = res.replace("=", "~E~")
    res
  }
  def unescapeHashMapSeparators(pin: String): String = {
    var res = pin.replace("~C~", ",")
    res = res.replace("~S~", ";")
    res = res.replace("~L~", ":")
    res = res.replace("~E~", "=")
    res
  }

}
