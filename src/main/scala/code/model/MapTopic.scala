package code.model

import net.liftweb.mapper._
import org.tresto.cache._
import org.tresto.helper._

object MapTopic extends MapTopic with LongKeyedMetaMapper[MapTopic] { //with PxGeneric[MapItem]{
}

class MapTopic extends LongKeyedMapper[MapTopic] with IdPKP with MapperFieldSearch {
  def getSingleton = MapTopic

  object name extends MappedPoliteString(this, 128)
  object sequenceNumber extends MappedInt(this)
  //object description extends MappedText(this)
  object docsUrl extends MappedString(this,256)
  object modifiedBy extends MappedLong(this)
  object modifiedDate extends MappedDateTime(this)
}
