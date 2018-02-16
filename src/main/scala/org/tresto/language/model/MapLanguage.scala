package org.tresto.language.model

import net.liftweb.mapper._
import net.liftweb.common._

//case object LanguageConnectionIdentifier extends ConnectionIdentifier {
//  def jndiName: String = "languagedb"
//}

class LanguageVendor(driver: String, url: String, user: Box[String], password: Box[String]) extends StandardDBVendor(driver, url, user, password) {
  override protected def maxPoolSize = 1
  //override protected def doNotExpandBeyond = 100
}

object MapLanguage extends MapLanguage with LongKeyedMetaMapper[MapLanguage] {

//  override def dbDefaultConnectionIdentifier = LanguageConnectionIdentifier

  override def dbTableName = "languages" // define the DB table key

  override def fieldOrder = List(id, appId, langkey, enabled, lastmodified, modifiedby, en_us, hu_hu, ro_ro)

  val TYPE_TEXT = "text"
  val TYPE_XML = "xml"
  val TYPE_TEXTILE = "textile"
  val TYPE_MARKDOWN = "markdown"
}

class MapLanguage extends LongKeyedMapper[MapLanguage] with IdPK {
  def getSingleton = MapLanguage

  object appId extends MappedString(this, 16)
  object langkey extends MappedString(this, 96) {
    override def defaultValue = "";
  }
  object enabled extends MappedBoolean(this)
  object lastmodified extends MappedDateTime(this)
  object modifiedby extends MappedLong(this) //UserId last modified this entry
  object lastused extends MappedDateTime(this) //can be turned on in the Lang handler class to log all key access, so that we can filter out unused keys
  object textType extends MappedPoliteString(this, 10) {
    override def defaultValue = MapLanguage.TYPE_TEXT
  }
  object en_us extends MappedText(this) {
    override def defaultValue = "";
  }
  object hu_hu extends MappedText(this) {
    override def defaultValue = "";
  }
  object ro_ro extends MappedText(this) {
    override def defaultValue = "";
  }
  object pl_pl extends MappedText(this) {
    override def defaultValue = "";
  }

  /**
   * Search this object, all of its fields if they contain any of the incoming search term, which can contain more words, with any partial matched
   * We calculate how many of the searched words appear in how many time in all of the fields, and return the sum of those numbers.
   * E.g. search for 'time change' if 'time' appears 3 times, and 'change' appears 2 times, then we return 5
   * @param str the search string
   * @return the sum of the occurence of each of the searched words
   */
  def search(str: String): Int = {
    val sarray = str.toLowerCase().split("[ +]").map(_.trim()).toList //split search on space or +
    var foundCount = 0
    //log.debug("allFields: " + allFields)
    for (s <- sarray) { //all the entered search terms
      if (this.primaryKeyField.get.toString() == s) { //Check the id first
        //log.debug("Search for primary key match: " + this.primaryKeyField.get.toString() + " == " + s)
        foundCount += 1
      }
    }
    for (f <- allFields) { //All mapper fields
      //val f = x.asInstanceOf[MappedField[Any, _]]
      for (s <- sarray) { //all the entered search terms
        //        val mid = self.asInstanceOf[LongKeyedMapper[_]].fieldByName("id")
        //        log.debug("mid: " + mid  )
        //        if (mid.get != null && mid.get.toString.indexOf(s) > 0 ) {
        //          return true
        //        }
        if (f.get != null) { //A field can be null which will give us an exception
          val result = f.get.toString().toLowerCase().indexOf(s)
          //log.debug("result " + self.asInstanceOf[LongKeyedMapper[_]].fieldByName(f.name) + " " + f.name + ": " + f.get.toString().toLowerCase() + " == " + s)
          if (result >= 0) {
            //println("found: " + s+ " #in: " + f.get + " count: " + foundCount)
            foundCount += 1
          }
        }
      }
      //      if (foundCount == sarray.size) {  //If all the entered words were found ...
      //        println("sarray: " + sarray + " # foundCount: " + foundCount)
      //        return true
      //      }
    }
    return foundCount
  }

}