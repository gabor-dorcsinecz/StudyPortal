package org.tresto.helper

import org.tresto.traits.LogHelper;
import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util._

object LoGeneralCRUD {
  val UPDATE = "U";
  val DELETE = "D";
  val INSERT = "I";
}

trait MapperFieldSearch extends LogHelper { self: BaseLongKeyedMapper =>
  /**
   * Search this object, all of its fields if they contain any of the incoming search term, which can contain more words, with any partial matched
   */
  def search(str: String): Boolean = {
    val sarray = str.toLowerCase().split("[ +]") //split search on space or +
    //log.debug("allFields: " + allFields)
    for (s <- sarray) { //all the entered search terms
      if (this.primaryKeyField.get.toString() == s) { //Check the id first
        //log.debug("Search for primary key match: " + this.primaryKeyField.get.toString() + " == " + s)
        return true
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
            return true
          }
        }
      }
    }
    return false
  }

  /**
   * Search in a list of fields
   */
  def searchInFields(str: String, fieldName: List[String]): Boolean = {
    val sarray = str.toLowerCase().split("[ +]") //split search on space or +
    val fields = this.allFields.filter(a => fieldName.find(b => b == a.name).isDefined)
    for (f <- fields) {
      for (s <- sarray) { //all the entered search terms
        if (f.get != null) { //A field can be null which will give us an exception
          val result = f.get.toString().toLowerCase().indexOf(s)
          //log.debug("result " + self.asInstanceOf[LongKeyedMapper[_]].fieldByName(f.name) + " " + f.name + ": " + f.get.toString().toLowerCase() + " == " + s)
          if (result >= 0) {
            return true
          }
        }
      }
    }
    return false
  }

}


