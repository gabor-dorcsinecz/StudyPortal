package code.model.cache

import scala.collection.mutable.HashMap
//import org.puremvc.scala.multicore.patterns.mediator.Mediator;
import org.tresto.traits.LogHelper;
import org.tresto.cache._
import net.liftweb.mapper.MappedField
import code.model.MapUser

case class UserFilter(partnerCompanyId: String = "0", anyfield: String = "", isDeleted: Boolean = false)

object AcsUser extends STMIndexedCache[MapUser](MapUser, (v: MapUser) => v.id.get) with LogHelper {

  val byEmail = cache.addIndex{(id,u) => Some(u.email.get)}
  this.useClones = false

  def getUserByEmail(email: String): Option[MapUser] = {
    byEmail(email).values.headOption
  }

  /**  Get all or for only one callcenter a dropdown list    */
  def asList(list: List[MapUser]): List[(String, String)] = {
    list.sortWith(emailSorter).map(u => (u.id.get.toString, u.email.get));
  }

  /** Get all or only for one callcenter a map of userId, userName */
  def asMap(list: List[MapUser]): Map[Long, String] = {
    list.map(u => (u.id.get, u.email.get)).toMap
  }


  def getCurrentUserOption(): Option[MapUser] = {
    val u = MapUser.curUserId.get
    u.isEmpty match {
      case true => None
      case false => cacheGetById(u.head)
    }
  }
  def getCurrentUser(): MapUser = getCurrentUserOption().get

  def getFullName(u: MapUser): String = {
    if (u.lastName.get.length() > 0 && u.firstName.get.length() > 0) {
      return u.lastName.get + ", " + u.firstName.get
    } else if (u.lastName.get.length() > 0 && u.firstName.get.length() == 0) {
      return u.lastName.get
    } else if (u.lastName.get.length() == 0 && u.firstName.get.length() > 0) {
      return u.firstName.get
    } else if (u.email.get.indexOf("@") > 0) {
      u.email.get.substring(0, u.email.get.indexOf("@"))
    } else {
      u.email.get
    }

  }
  def getFullNameWithEmail(u: MapUser): String = {
    if (u.lastName.get.length() > 0 && u.firstName.get.length() > 0) {
      return u.lastName.get + ", " + u.firstName.get + " (" + u.email.get + ")"
    } else if (u.lastName.get.length() > 0 && u.firstName.get.length() == 0) {
      return u.lastName.get + " (" + u.email.get + ")"
    } else if (u.lastName.get.length() == 0 && u.firstName.get.length() > 0) {
      return u.firstName.get + " (" + u.email.get + ")"
    } else if (u.email.get.indexOf("@") > 0) {
      u.email.get.substring(0, u.email.get.indexOf("@")) + " (" + u.email.get + ")"
    } else {
      u.email.get
    }
  }

  def getUsersWithEmail(email: String): List[MapUser] = {
    val allusers = cacheGetAll()
    allusers.filter(a => a.email.get.toLowerCase().trim() == email.toLowerCase().trim())
  }

  /**
   * See if there is another user with the same email address as the user passed in as parameter
   */
  def isThereUserWithEmailBesidesMe(email: String, userId: Long): Boolean = {
    val users = getUsersWithEmail(email.toLowerCase().trim())
    users.size match {
      case 0 => false //No users with email
      case 1 => !(users(0).id == userId) //One user, but maybe it's the one requesting 
      case _ => true
    }
  }

  def getCurrUsersEmail(): String = {
    AcsUser.cacheGetById(MapUser.curUserId.get.head).get.email.get
  }

  def sorter(a: MapUser, b: MapUser): Boolean = {
    val af = a.lastName.get.toLowerCase + a.firstName.get.toLowerCase
    val bf = b.lastName.get.toLowerCase + b.firstName.get.toLowerCase
    af < bf
  }
  def emailSorter(a: MapUser, b: MapUser) = a.email.get.toLowerCase < b.email.get.toLowerCase

  override def cacheAndDbInsert(v: MapUser): MapUser = {
    val ret = super.cacheAndDbInsert(v)
    return ret
  }

  override def cacheAndDbUpdate(v: MapUser) {
    super.cacheAndDbUpdate(v)
  }

  override def cacheAndDbDelete(v: MapUser) {
    super.cacheAndDbDelete(v)
  }

  
  def cloneUser(original: MapUser): MapUser = {
    val clone = MapUser.create
    for (localField <- original.allFields) {
      val clonedField = clone.fieldByName(localField.name).get.asInstanceOf[MappedField[Any, MapUser]]
      clonedField.setFromAny(localField.get);
      clonedField.resetDirty
    }
    //clone.id.set(0)
    clone
  }   
  
  def generateApiKey():String = {
    java.util.UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16)
  }
}


