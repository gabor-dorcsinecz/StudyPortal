package code.model

import net.liftweb.common.Full
import net.liftweb.mapper._
import org.tresto.cache._
import org.tresto.helper._
import org.tresto.language.snippet.Lang

/**
  * Solution for a given assignement by a student
  */
object MapSolution extends MapSolution with LongKeyedMetaMapper[MapSolution] { //with PxGeneric[MapItem]{

  val SOLUTION_TYPE_TEXT = "text"
  val SOLUTION_TYPE_CODE = "code"
  val SOLUTION_TYPE_COMPILED = "compile"

  def solutionTypes = List(SOLUTION_TYPE_TEXT,SOLUTION_TYPE_CODE,SOLUTION_TYPE_COMPILED)
  def solutionTypeList():List[(String,String)] = solutionTypes.map(a => (a, Lang.gs("assignement.type." + a)))

}

class MapSolution extends LongKeyedMapper[MapSolution] with IdPKP with MapperFieldSearch {
  def getSingleton = MapSolution

  object userId extends MappedLongForeignKey(this, MapUser)
  object assignementId extends MappedLongForeignKey(this, MapAssignement)

  object solution extends MappedText(this)  //Submitted by the user

  object result extends MappedInt(this)

  object modifiedDate extends MappedDateTime(this)
}
