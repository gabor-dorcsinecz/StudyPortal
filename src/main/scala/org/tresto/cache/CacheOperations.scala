package org.tresto.cache

//import org.puremvc.scala.multicore.patterns.observer.Notification
//import org.puremvc.scala.multicore.patterns.mediator.Mediator
//import org.puremvc.scala.multicore.interfaces.IFacade
//import org.puremvc.scala.multicore.interfaces.IMediator
import scala.collection.mutable.HashMap
import org.tresto.traits.LogHelper
import net.liftweb.mapper._
import net.liftweb.util.FieldError

object CacheOperations {

  val CACHE_GET_BYID = "CACHE_GET_BYID"
  val CACHE_INSERT = "CACHE_INSERT"
  val CACHE_UPDATE = "CACHE_UPDATE"
  val CACHE_DELETE = "CACHE_DELETE"
  val CACHE_GET_ALL = "CACHE_GET_ALL"
  val CACHE_INSERTALL = "CACHE_INSERTALL"
  val CACHE_DELETEALL = "CACHE_DELETEALL"

  val CACHEANDDB_INSERT = "CACHEANDDB_INSERT"
  val CACHEANDDB_UPDATE = "CACHEANDDB_UPDATE"
  val CACHEANDDB_DELETE = "CACHEANDDB_DELETE"
  //val CACHEANDDB_INSERTGET = "CACHEANDDB_INSERTGET"
  //val CACHEANDDB_CRUD = "CACHEANDDB_CRUD"
  val CACHEANDDB_RELOAD = "CACHEANDDB_RELOAD"
  //val CACHE_SIZE = "CACHE_SIZE"

}

case class PaginatedSearch[V <: LongKeyedMapper[V]](searchExpression: Any, sorter: (V, V) => Boolean, startAt: Int, count: Int)

class MappedPasswordReadable[T <: Mapper[T]](val fOwner: T) extends MappedPassword[T](fOwner) with LogHelper {
  var readablePassword = "NotYetSet"

  override protected def real_i_set_!(value: String): String = {
    readablePassword = value
    log.trace("readablePassword: " + readablePassword + " -> " + value)
    super.real_i_set_!(value)
  }

//  override protected def i_is_! = {
//    if (validate.isEmpty) MappedPassword.blankPw
//    else ""
//  }

}

trait IdPKP /* extends BaseLongKeyedMapper */ {
  self: BaseLongKeyedMapper =>
  object id extends MappedLongIndex[MapperType](this.asInstanceOf[MapperType]) {
    override def writePermission_? = true
  }
  def primaryKeyField = id
}

trait AccessorsCommon[V] {
  def cacheGetById(iid: Long, returnOriginal: Boolean = false): Option[V]
  def cacheInsert(v: V): V
  def cacheUpdate(v: V)
  //def cacheDelete(v: V)
  def cacheGetAll(returnOriginals: Boolean = false): List[V]
  def cacheInsertAll(lv: List[V]): List[V]
  def cacheDeleteAll()
}

//trait AccessorBase[V] extends AccessorsCommon[V]{
//
//  val mediatorName: String
//  //  var myName: String = java.util.UUID.randomUUID().toString  //Just to have some initial value
//  //  def mediatorName = myName
//  //  def mediatorName_(n:String){myName = n}  
//
//  def getMediator(): IMediator;
//  private lazy val mediator = getMediator()
//
//  def cacheGetById(iid: Long, returnOriginal: Boolean = false): Option[V] = {
//    mediator.awaitFuture(CacheOperations.CACHE_GET_BYID, (iid, returnOriginal)).asInstanceOf[Option[V]]
//  }
//
//  def cacheInsert(v: V): V = {
//    mediator.awaitFuture(CacheOperations.CACHE_INSERT, v).asInstanceOf[V]
//  }
//
//  def cacheGetAll(returnOriginals: Boolean = false): List[V] = {
//    mediator.awaitFuture(CacheOperations.CACHE_GET_ALL, returnOriginals).asInstanceOf[List[V]]
//  }
//
//  def cacheInsertAll(lv: List[V]): List[V] = {
//    mediator.awaitFuture(CacheOperations.CACHE_INSERTALL, lv).asInstanceOf[List[V]]
//  }
//
//  def cacheDeleteAll() {
//    mediator.bangMe(CacheOperations.CACHE_DELETEALL)
//  }
//
//  def cacheAndDbInsert(v: V): V = {
//    mediator.awaitFuture(CacheOperations.CACHEANDDB_INSERT, v).asInstanceOf[V]
//  }
//
//  def cacheAndDbUpdate(v: V) {
//    mediator.bangMe(CacheOperations.CACHEANDDB_UPDATE, v)
//  }
//
//  /** Update only the cache but do not save to the db*/
//  def cacheUpdate(v: V) {
//    mediator.bangMe(CacheOperations.CACHE_UPDATE, v)
//  }
//
//  def cacheAndDbDelete(v: V) {
//    mediator.bangMe(CacheOperations.CACHEANDDB_DELETE, v)
//  }
//
//  def cacheAndDbReloadAll() {
//    //stopWatchGroup("cacheAndDbReloadAll")(getMediator.bangMe(CacheOperations.CACHEANDDB_RELOAD))
//    mediator.bangMe(CacheOperations.CACHEANDDB_RELOAD)
//  }
//}
//
///**
// * Extend this if you have an in memory cache only, no persistent storage
// */
//trait AccessorInMemory[V] extends AccessorsCommon[V]{
//
//  val mediatorName: String
//  private lazy val mediator = getMediator()
//
//  def getMediator(): IMediator;
//
//  def cacheGetById(iid: Long): Option[V] = {
//    mediator.awaitFuture(CacheOperations.CACHE_GET_BYID, iid).asInstanceOf[Option[V]]
//  }
//
//  def cacheInsert(v: V): V = {
//    mediator.awaitFuture(CacheOperations.CACHE_INSERT, v).asInstanceOf[V]
//  }
//
//  /** Update only the cache but do not save to the db*/
//  def cacheUpdate(v: V) {
//    mediator.bangMe(CacheOperations.CACHE_UPDATE, v)
//  }
//
//  def cacheDelete(v: V) {
//    mediator.bangMe(CacheOperations.CACHE_DELETE, v)
//  }
//
//  def cacheGetAll(returnOriginals: Boolean = false): List[V] = {
//    mediator.awaitFuture(CacheOperations.CACHE_GET_ALL, returnOriginals).asInstanceOf[List[V]]
//  }
//
//  def cacheInsertAll(lv: List[V]): List[V] = {
//    mediator.awaitFuture(CacheOperations.CACHE_INSERTALL, lv).asInstanceOf[List[V]]
//  }
//
//  def cacheDeleteAll() {
//    mediator.bangMe(CacheOperations.CACHE_DELETEALL)
//  }
//
//}

/**
 * An accessor for objects that are not cached but accessed from the database directly
 */
class AccessorInDB[V <: LongKeyedMapper[V]](myType: V) {

  def cacheGetById(iid: Long): Option[V] = {
    myType.getSingleton.find(iid)
    //V.find(iid)
  }

  def cacheAndDbInsert(v: V): V = {
    v.save()
    v
  }

  /** Update only the cache but do not save to the db*/
  def cacheAndDbUpdate(v: V) {
    v.save()
  }

  def cacheAndDbDelete(v: V) {
    v.delete_!
  }

  def cacheGetAll(returnOriginals: Boolean = false): List[V] = {
    myType.getSingleton.findAll()
    //V.find()
  }

  //  def cacheInsertAll/(lv: List[V]): List[V] = {
  //    lv.foreach(a => a.save())
  //    lv
  //  }
  //
  //  def cacheDeleteAll() {
  //  }

}
