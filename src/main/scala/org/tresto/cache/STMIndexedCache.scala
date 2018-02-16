package org.tresto.cache

import scala.collection.mutable._
import scala.concurrent.stm._
import org.tresto.traits.LogHelper
import net.liftweb.mapper._
import scala.beans.BeanProperty;

class STMIndexedCache[V <: LongKeyedMapper[V] with IdPKP](myType: V, idExtractor: V => Long) extends LogHelper {

  //  lazy val stopWatchGroup = new StopwatchGroup(mediatorName)
  //  stopWatchGroup.enabled = true
  //  stopWatchGroup.range = StopwatchRange(0 seconds, 2 seconds, 50 millis)
  var isTesting = false //Is this a testcase, when we do not need to read and write to the database or is it real application runing?

  //protected val cache = Ref(new TCache[Long, V](idExtractor)) //No access to anyone, WE CANNOT ALLOW TO RETRIEVE THE ORIGINAL CACHED OBJECTS, just clones!
  val cache = new IndexedMap[Long, V](idExtractor)
  @BeanProperty var useClones: Boolean = true //Should we return a clone or should we return the original objects from cache (this is VERY dangerous)

  def cacheGetById(iid: Long, returnOriginals: Boolean = false): Option[V] = {
    atomic { implicit txn =>
      //log.trace("getById: " + iid)
      val olocal = cache.get(iid)
      val local: Option[V] = olocal.isDefined match {
        case true => //We have it in the cache
          olocal
        case false => //Cache does not contain object with this id, try from the database
          if (isTesting == false) {
            val odbobj = myType.getSingleton.find(iid)
            odbobj.isDefined match {
              case true => //We have it in the database
                cache.put(odbobj.head) //Cache it first
                //org.tresto.billing.model.CacheLoader.loadCache4SuperCompany(iid) //Cache this whole supercompany
                odbobj
              case false =>
                None
            }
          } else { //This is a testcase
            None
          }
      }
      if (local.isEmpty) {
        return None
      }
      //log.trace("getById: " + local.get)
      //log.trace("getById: " + cloned)
      val res = returnOriginals match {
        case false => Some(cloneObject(local.get))
        case true => Some(local.get)
      }
      //log.debug("cacheGetById: " + iid + " originals: " + returnOriginals)
      return res
    }
  }

  /**
   * Insert into the cache only
   * Used to fill the cache at startup
   */
  def cacheInsert(v: V): V = {
    cache.put(v)
    v
    //    atomic { implicit txn =>
    //      //log.trace("cacheInsert: " + v)
    //      //val local = cloneObject(v) //We do not touch the original object, it may be modified again after calling this method in the caller code, which would fuck up the cache
    //      //log.debug("cacheInsert cached: " + local + "\r\nIncoming           : " + v)
    //      cache.insert(v)
    //      return v
    //    }
  }

  /**
   * Get all objects i the cache
   */
  def cacheGetAll(returnOriginals: Boolean = false): List[V] = {
    //    atomic { implicit txn =>
    //log.trace("cacheGetAll ")
    val fn = cache.getAll()
    returnOriginals match {
      case false => fn.map(cloneObject(_))
      case true => fn
    }
    //    }
  }

  /**
   * Insert a list of objects into the cache only (no db operation)
   * Used to fill the cache at startup
   */
  def cacheInsertAll(lv: List[V]): List[V] = {
    //atomic { implicit txn =>
    //log.trace("cacheInsertAll: " + lv.length)
    lv.foreach(cacheInsert(_))
    //lv.map(cloneObject(_))
    return lv
    //}
  }

  def cacheDeleteAll() {
    cache.removeAll()
    //atomic { implicit txn =>
    //log.trace("cacheDeleteAll: ")
    //  cache.deleteAll
    //}
  }

  //  /**
  //   * Add a search field.
  //   * @param fieldName when later you want to search based on this field, you have to provide it again in the find functions
  //   * @param extractor an extractor function which will extract the search field from the object
  //   */
  //  def addSearchField(fieldName: String, extractor: V => Any) {
  //    atomic { implicit txn =>
  //      cache.addSearchField(fieldName, extractor)
  //    }
  //  }

  //  def findAll(fieldName: String, fieldValue: Any, returnOriginals: Boolean = false): List[V] = {
  //    atomic { implicit txn =>
  //      //log.trace("findAll: " + fieldName + " = " +  fieldValue)
  //      val fn = cache.findAll(fieldName, fieldValue)
  //      returnOriginals match {
  //        case false => fn.map(cloneObject(_))
  //        case true => fn
  //      }
  //    }
  //
  //  }

  /**
   * Insert a new object into the cache and the database too
   */
  def cacheAndDbInsert(v: V): V = {
    //    atomic { implicit txn =>
    //    //val local = cloneObject(v) //We do not touch the original object, it may be modified again after calling this method in the caller code, which would fuck up the cache
    //    val local = cacheInsert(v) //insert into the cacheF
    //    if (isTesting == false) { //If this is not a test save it
    //      local.save() //Save it to have an id field!
    //    }
    //    //logActivity(local,CacheOperations.CACHEANDDB_INSERT)
    //    //return cloneObject(local) //Do not return the original!
    //    log.debug("cacheAndDbInsert cached: " + local + "\r\nIncoming                : " + v)
    //    //v.id(local.id.get)
    //    return v
    val local = cloneObject(v) //We do not touch the original object, it may be modified again after calling this method in the caller code, which would fuck up the cache
    local.save() //Save it to have an id field!
    cacheInsert(local) //insert into the cacheF
    //logActivity(local,CacheOperations.CACHEANDDB_INSERT)
    return cloneObject(local) //Do not return the original!
    //    }
  }

  /**
   * Update an object in the cache and the database too
   */
  def cacheAndDbUpdate(v: V) {
    //    atomic { implicit txn =>
    val local = updateLocalFromRemote(v) //Read the changed fields and update our local object
    cache.update(local)
    //log.debug("cacheAndDbUpdate cached: " + local + "\r\nIncoming                : " + v)
    //logActivity(local,CacheOperations.CACHEANDDB_UPDATE)
    if (isTesting == false) { //If this is not a test save it
      local.save() //Save it to the database
    }
    //    }
  }

  def cacheUpdate(v: V) {
    atomic { implicit txn =>
      val local = updateLocalFromRemote(v) //Read the changed fields and update our local object
      //log.trace("cacheAndDbUpdate: " + local)
    }
  }

  /**
   * Delete an object from the cache and the database too
   */
  def cacheAndDbDelete(v: V) {
    atomic { implicit txn =>
      //log.trace("cacheAndDbDelete: " + v)
      val local = cache.get(v.id.get).get
      //logActivity(local,CacheOperations.CACHEANDDB_DELETE)
      if (isTesting == false) { //If this is not a test delte it
        local.delete_! //delete from db
      }
      cache.remove(v.id.get) //delete from cache
    }
  }

  /**
   * Used at cache startup, it will load all avaiable objects from the db and inserts them into the cache
   */
  def cacheAndDbReloadAll() {
    atomic { implicit txn =>
      //log.trace("cacheAndDbReloadAll before: " + cache.idCache.size)
      cacheDeleteAll()
      //log.trace("After cache clean: " + cache.idCache.size)
      if (isTesting == false) { //If this is not a test save it
        val fromdb = myType.getSingleton.findAll()
        //log.trace("Loaded rows from db: " + fromdb.size)
        cacheInsertAll(fromdb)
      }
    }
  }

  /**
   * Based on an incoming (not cached) object's id we retrieve the cached object
   * Then all the changed fields of the incoming object will be copied into the cached object
   */
  protected def updateLocalFromRemote(remote: V): V = {
    atomic { implicit txn =>
      if (useClones == true) { //If we use clones, we must copy all the data from the cloned objects to the original objects
        val local = cache.get(remote.id.get).get
        //traceObject(local)
        //log.trace("updateLocalFromRemote: " + local)
        for (f <- remote.allFields) {
          val remoteField = f.asInstanceOf[MappedField[Any, V]]
          //log.trace("copyFromRemote: " + remoteField.name +  ": " + remoteField.get + " dirty: " + remoteField.dirty_?)
          if (remoteField.dirty_?) { //Did the value of this field changed ?
            val localField = local.fieldByName(f.name).head
            //log.trace("updateLocalFromRemote local: " + localField.name + ": " + localField.get + " dirty: " + localField.dirty_?)
            //log.trace("updateLocalFromRemote remote: " + remoteField.name + ": " + remoteField.get + " dirty: " + remoteField.dirty_?)
            localField.setFromAny(f.get);
            //          if (remoteField.isInstanceOf[MappedPasswordReadable[_]]) {  
            //            val pwvalue = f.asInstanceOf[MappedPasswordReadable[_]].readablePassword //once a password was set into the password field it can not be read from there, we must use this passwordclass
            //            //log.trace("Password field: " + pwvalue)
            //            if (pwvalue.length >= 3 ) {
            //              localField.asInstanceOf[MappedPasswordReadable[_]].setList(List(pwvalue,pwvalue))
            //            }
            //          }
          }
        }
        //traceObject(local)
        return local
      } else { //As we returned previously the original objects and they were changed, we do not have to do anything
        cache.update(remote) //I DO NOT understand why but if we do not use clones this should be done, even though another thread updated the cached object directly
        return remote
      }
    }
  }

  /**
   * When you retrieve an object from the cache you do not get that object but a clone from it
   * This way you can't fuck up the cache. If you would get the cached objects and woud modify them
   * then others retrieving the same object could get a half updated version of the same object
   * So this clones a cached object and returns a new one which can be safely given to other threads to paly with
   */
  protected def cloneObject(existing: V): V = {
    if (useClones == true) {
      //val cloned = existing.getClass.newInstance.asInstanceOf[V]  
      val cloned = existing.getSingleton.create
      //log.trace("cloneObject: " + existing)
      for (localField <- existing.allFields) {
        val clonedField = cloned.fieldByName(localField.name).head.asInstanceOf[MappedField[Any, V]]
        //val origField = f.asInstanceOf[MappedField[Any, V]]
        //log.trace("existing: " + f.name + " = " +  f.get + " - " + f.getClass.toString  )
        //log.trace("cloned: " + clonedField.name + " = " +  clonedField.get + " - " + clonedField.actualField(cloned))
        clonedField.setFromAny(localField.get);
        clonedField.resetDirty
        //log.trace("cloneObject local: " + localField.name + ": " + localField.get + " dirty: " + localField.asInstanceOf[MappedField[Any, V]].dirty_?)
        //log.trace("cloneObject cloned: " + clonedField.name + ": " + clonedField.get + " dirty: " + clonedField.dirty_?)
        //        if (clonedField.isInstanceOf[MappedPasswordReadable[_]]) {  
        //          val pwvalue = f.asInstanceOf[MappedPasswordReadable[_]].readablePassword //once a password was set into the password field it can not be read from there, we must use this passwordclass
        //          //log.trace("Password field: " + pwvalue)
        //          clonedField.asInstanceOf[MappedPasswordReadable[_]].setList(List(pwvalue,pwvalue))
        //        }
      }
      cloned.id.set(existing.id.get)
      return cloned
    } else {
      return existing
    }
  }

  protected def traceObject(obj: V) {
    log.trace("=== traceObject ===: " + obj)
    for (l <- obj.allFields) {
      val localField = obj.fieldByName(l.name).head
      log.trace("field: " + localField.name + ": " + localField.get + " dirty: " + localField.dirty_?)
    }
  }

  protected def reloadAtStart() {
    //val note = new Notification(CacheOperations.CACHEANDDB_RELOAD); //Schedule for later loading (needed for jetty)
    //net.liftweb.actor.LAPinger.schedule(this, note, math.round((math.random * 1000 + 4000)));
    //this.bangMe(CacheOperations.CACHEANDDB_RELOAD)
    cacheAndDbReloadAll()
  }

  def size(): Int = {
    cache.size()
  }

  //  /**
  //   * Just an empty method to override
  //   */
  //  protected def logActivity(obj: V, etype: String) {
  //    org.tresto.billing.model.accessor.AcsActivityLog.addLog(obj,etype)
  //  }

}
