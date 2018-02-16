package code.model

import net.liftweb.mapper._
import org.tresto.cache._
import org.tresto.helper._

object MapAssignement extends MapAssignement with LongKeyedMetaMapper[MapAssignement] { //with PxGeneric[MapItem]{
  val LANG_SCALA = "scala"
}

class MapAssignement extends LongKeyedMapper[MapAssignement] with IdPKP with MapperFieldSearch {
  def getSingleton = MapAssignement

  object topicId extends MappedLongForeignKey(this, MapTopic)
  object name extends MappedPoliteString(this, 32)

  object description extends MappedText(this) //
  object tests extends MappedText(this)

  object language extends MappedPoliteString(this, 16) { //LANG_ the scrips programming language
    override def defaultValue = MapAssignement.LANG_SCALA
  }
  object solutionType extends MappedString(this,16)
  object modifiedBy extends MappedLong(this)
  object modifiedDate extends MappedDateTime(this)
}
