//package org.tresto.cache
//
////import org.puremvc.scala.multicore.patterns.observer.Notification;
////import org.puremvc.scala.multicore.patterns.mediator.Mediator;
////import org.puremvc.scala.multicore.interfaces.IFacade;
////import org.puremvc.scala.multicore.interfaces.IMediator;
//import scala.collection.mutable.HashMap
//import org.tresto.traits.LogHelper;
//import net.liftweb.mapper._
//import scala.beans.BeanProperty;
//import stopwatch._
//import stopwatch.TimeUnit._
//
///**
// * An in memory thread safe proxy implementation, using TCache
// */
//class CacheBase[K, V](name: String, pfacade: IFacade, idExtractor: V => K) extends Mediator(name) with LogHelper {
//  val cache = new TCache[K, V](idExtractor)
//  pfacade.registerMediator(this);
//  log.info("Registered")
//
//  override def listNotificationInterests(): Array[String] = {
//    return Array(
//      CacheOperations.CACHE_GET_BYID,
//      CacheOperations.CACHE_INSERT,
//      CacheOperations.CACHE_UPDATE,
//      CacheOperations.CACHE_DELETE,
//      CacheOperations.CACHE_GET_ALL,
//      CacheOperations.CACHE_INSERTALL,
//      CacheOperations.CACHE_DELETEALL)
//  }
//
//  override def handleMessage: PartialFunction[Any, Unit] = {
//    case note: Notification if note.getName == CacheOperations.CACHE_GET_BYID =>
//      val tup = note.getBody.asInstanceOf[(K, Boolean)]
//      reply(cache.getById(tup._1))
//    case note: Notification if note.getName == CacheOperations.CACHE_INSERT => reply(cacheInsert(note.getBody.asInstanceOf[V]))
//    case note: Notification if note.getName == CacheOperations.CACHE_UPDATE => cache.update(note.getBody.asInstanceOf[V])
//    case note: Notification if note.getName == CacheOperations.CACHE_DELETE => cache.delete(note.getBody.asInstanceOf[V])
//    case note: Notification if note.getName == CacheOperations.CACHE_GET_ALL => reply(cache.getAll())
//    case note: Notification if note.getName == CacheOperations.CACHE_INSERTALL => cache.insertAll(note.getBody.asInstanceOf[List[V]])
//    case note: Notification if note.getName == CacheOperations.CACHE_DELETEALL => cache.deleteAll
//
//  }
// 
//  /**
//   * Insert into the cache only
//   * Used to fill the cache at startup
//   */
//  protected def cacheInsert(v: V): V = {
//    cache.insert(v)
//    v
//  }
//
//}
//
////====================================================================================================================================================
////====================================================================================================================================================
////====================================================================================================================================================
///**
// * There is one thing you need to keep in mind: NEVER RETURN THE CACHED OBJECTS (THEY ARE MUTABLE), ALLWAYS CLONE THEM AND RETURN THE CLONES
// * This class sits on top of an in memory cache, and
// * - Makes the in memory cache thread safe by wrapping an actor around it, so noone can directly access the cache
// * - Will takes care of the database operations (besides caching objects)
// * When you create this class, you can initialize it by loading objects from the database cacheAndDbReloadAll
// * You can add any number of search fields (by default you can search by id, given by the idExtractor) like this:
// *    def superCompanyIdExtractor(v:MapScript) = v.superCompanyId.is
// *    addSearchField("superCompany",superCompanyIdExtractor)
// * There are some MapperFields that are not usable here, currenlty:
// * - IdPK cannot be used because the id parameter is not writable, use IdPKP instead, which allows to override the id
// * - MappedPassword cannot be used because once you set the password there is no way you can read that out for cloning, use MappedPasswordReadable instead
// *
// */
//class LongKeyedMapperProxyBase[V <: LongKeyedMapper[V] with IdPKP](mediatorName: String, pfacade: IFacade, myType: V, idExtractor: V => Long) extends Mediator(mediatorName) with LogHelper {
//
//  lazy val stopWatchGroup = new StopwatchGroup(mediatorName)
//  stopWatchGroup.enabled = true
//  stopWatchGroup.range = StopwatchRange(0 seconds, 2 seconds, 50 millis)
//  var isTesting = false //Is this a testcase, when we do not need to read and write to the database or is it real application runing?
//
//  protected val cache = new TCache[Long, V](idExtractor) //No access to anyone, WE CANNOT ALLOW TO RETRIEVE THE ORIGINAL CACHED OBJECTS, just clones!
//  pfacade.registerMediator(this);
//  log.info("Registered")
//
//  @BeanProperty var useClones: Boolean = true //Should we return a clone or should we return the original objects from cache (this is VERY dangerous)
//
//  override def listNotificationInterests(): Array[String] = {
//    return Array(
//      CacheOperations.CACHE_GET_BYID,
//      CacheOperations.CACHE_INSERT,
//      CacheOperations.CACHE_GET_ALL,
//      CacheOperations.CACHE_INSERTALL,
//      CacheOperations.CACHE_DELETEALL,
//      CacheOperations.CACHEANDDB_INSERT,
//      CacheOperations.CACHEANDDB_UPDATE,
//      CacheOperations.CACHE_UPDATE,
//      CacheOperations.CACHEANDDB_DELETE,
//      CacheOperations.CACHEANDDB_RELOAD)
//  }
//
//  override def handleMessage: PartialFunction[Any, Unit] = {
//    case note: Notification if note.getName == CacheOperations.CACHE_GET_BYID =>
//      val tup = note.getBody.asInstanceOf[(Long, Boolean)]
//      reply(stopWatchGroup("cacheGetById")(cacheGetById(tup._1, tup._2)))
//    case note: Notification if note.getName == CacheOperations.CACHE_INSERT => reply(stopWatchGroup("cacheInsert")(cacheInsert(note.getBody.asInstanceOf[V])))
//    case note: Notification if note.getName == CacheOperations.CACHE_GET_ALL => reply(stopWatchGroup("cacheGetAll")(cacheGetAll(note.getBody().asInstanceOf[Boolean])))
//    case note: Notification if note.getName == CacheOperations.CACHE_INSERTALL => reply(stopWatchGroup("cacheInsertAll")(cacheInsertAll(note.getBody.asInstanceOf[List[V]])))
//    case note: Notification if note.getName == CacheOperations.CACHE_DELETEALL => stopWatchGroup("cacheDeleteAll")(cacheDeleteAll())
//    case note: Notification if note.getName == CacheOperations.CACHEANDDB_INSERT => reply(stopWatchGroup("cacheAndDbInsert")(cacheAndDbInsert(note.getBody.asInstanceOf[V])))
//    case note: Notification if note.getName == CacheOperations.CACHEANDDB_UPDATE => stopWatchGroup("cacheAndDbUpdate")(cacheAndDbUpdate(note.getBody.asInstanceOf[V]))
//    case note: Notification if note.getName == CacheOperations.CACHE_UPDATE => stopWatchGroup("cacheUpdate")(cacheUpdate(note.getBody.asInstanceOf[V]))
//    case note: Notification if note.getName == CacheOperations.CACHEANDDB_DELETE => stopWatchGroup("cacheAndDbDelete")(cacheAndDbDelete(note.getBody.asInstanceOf[V]))
//    case note: Notification if note.getName == CacheOperations.CACHEANDDB_RELOAD => stopWatchGroup("cacheAndDbReloadAll")(cacheAndDbReloadAll())
//  }
//
//  //  val notficationInterests = new HashMap[String,Any=>Any]
//  //  def addNotificationInterests(noteName:String,func:Any=>Any) {
//  //  }
//
//  protected def cacheGetById(iid: Long, returnOriginals: Boolean = false): Option[V] = {
//    //log.trace("getById: " + iid)
//    val olocal = cache.getById(iid)
//    val local: Option[V] = olocal.isDefined match {
//      case true => //We have it in the cache
//        olocal
//      case false => //Cache does not contain object with this id, try from the database
//        if (isTesting == false) {
//          val odbobj = myType.getSingleton.find(iid)
//          odbobj.isDefined match {
//            case true => //We have it in the database
//              cache.insert(odbobj.get) //Cache it first
//              //org.tresto.billing.model.CacheLoader.loadCache4SuperCompany(iid) //Cache this whole supercompany
//              odbobj
//            case false =>
//              None
//          }
//        } else { //This is a testcase
//          None
//        }
//    }
//    if (local.isEmpty) {
//      return None
//    }
//    //log.trace("getById: " + local.get)
//    //log.trace("getById: " + cloned)
//    val res = returnOriginals match {
//      case false => Some(cloneObject(local.get))
//      case true => Some(local.get)
//    }
//    //log.debug("cacheGetById: " + iid + " originals: " + returnOriginals)
//    return res
//  }
//
//  /**
//   * Insert into the cache only
//   * Used to fill the cache at startup
//   */
//  protected def cacheInsert(v: V): V = {
//    //log.trace("cacheInsert: " + v)
//    //val local = cloneObject(v) //We do not touch the original object, it may be modified again after calling this method in the caller code, which would fuck up the cache
//    //log.debug("cacheInsert cached: " + local + "\r\nIncoming           : " + v)
//    cache.insert(v)
//    v
//  }
//
//  /**
//   * Get all objects i the cache
//   */
//  protected def cacheGetAll(returnOriginals: Boolean = false): List[V] = {
//    //log.trace("cacheGetAll ")
//    val fn = cache.getAll()
//    returnOriginals match {
//      case false => fn.map(cloneObject(_))
//      case true => fn
//    }
//
//  }
//
//  /**
//   * Insert a list of objects into the cache only (no db operation)
//   * Used to fill the cache at startup
//   */
//  protected def cacheInsertAll(lv: List[V]): List[V] = {
//    //log.trace("cacheInsertAll: " + lv.length)
//    lv.foreach(cacheInsert(_))
//    //lv.map(cloneObject(_))
//    lv
//  }
//
//  protected def cacheDeleteAll() {
//    //log.trace("cacheDeleteAll: ")
//    cache.deleteAll
//  }
//
//  /**
//   * Add a search field.
//   * @param fieldName when later you want to search based on this field, you have to provide it again in the find functions
//   * @param extractor an extractor function which will extract the search field from the object
//   */
//  def addSearchField(fieldName: String, extractor: V => Any) {
//    cache.addSearchField(fieldName, extractor)
//  }
//
//  protected def findAll(fieldName: String, fieldValue: Any, returnOriginals: Boolean = false): List[V] = {
//    //log.trace("findAll: " + fieldName + " = " +  fieldValue)
//    val fn = cache.findAll(fieldName, fieldValue)
//    returnOriginals match {
//      case false => fn.map(cloneObject(_))
//      case true => fn
//    }
//
//  }
//
//  //  protected def findAllOriginal(fieldName: String, fieldValue: Any): List[V] = {
//  //    //log.trace("findAll: " + fieldName + " = " +  fieldValue)
//  //    cache.findAll(fieldName, fieldValue)
//  //  }
//  //
//  //  protected def findAll4Paginator(fieldName: String, fieldValue: Any, sorter: (V,V) => Boolean, startAt:Int, count: Int): (List[V],Int) = {
//  //    val obj = cache.findAll(fieldName, fieldValue).sortWith(sorter)
//  //    val res1 = obj.slice(startAt,startAt + count).map(cloneObject(_))
//  //    val res2 = obj.size
//  //    return (res1,res2)
//  //  }
//
//  //protected def getCahceSize():Int = cache.idCache.size
//
//  /**
//   * Insert a new object into the cache and the database too
//   */
//  protected def cacheAndDbInsert(v: V): V = {
//    //    //val local = cloneObject(v) //We do not touch the original object, it may be modified again after calling this method in the caller code, which would fuck up the cache
//    //    val local = cacheInsert(v) //insert into the cacheF
//    //    if (isTesting == false) { //If this is not a test save it
//    //      local.save() //Save it to have an id field!
//    //    }
//    //    //logActivity(local,CacheOperations.CACHEANDDB_INSERT)
//    //    //return cloneObject(local) //Do not return the original!
//    //    log.debug("cacheAndDbInsert cached: " + local + "\r\nIncoming                : " + v)
//    //    //v.id(local.id.get)
//    //    return v
//    val local = cloneObject(v) //We do not touch the original object, it may be modified again after calling this method in the caller code, which would fuck up the cache
//    local.save() //Save it to have an id field!
//    cacheInsert(local) //insert into the cacheF
//    //logActivity(local,CacheOperations.CACHEANDDB_INSERT)
//    return cloneObject(local) //Do not return the original!    
//  }
//
//  /**
//   * Update an object in the cache and the database too
//   */
//  protected def cacheAndDbUpdate(v: V) {
//    val local = updateLocalFromRemote(v) //Read the changed fields and update our local object
//    cache.updateSearchFields(local)
//    //log.debug("cacheAndDbUpdate cached: " + local + "\r\nIncoming                : " + v)
//    //logActivity(local,CacheOperations.CACHEANDDB_UPDATE)
//    if (isTesting == false) { //If this is not a test save it
//      local.save() //Save it to the database
//    }
//  }
//
//  protected def cacheUpdate(v: V) {
//    val local = updateLocalFromRemote(v) //Read the changed fields and update our local object
//    //log.trace("cacheAndDbUpdate: " + local)
//  }
//
//  /**
//   * Delete an object from the cache and the database too
//   */
//  protected def cacheAndDbDelete(v: V) {
//    //log.trace("cacheAndDbDelete: " + v)
//    val local = cache.getById(v.id.is).get
//    //logActivity(local,CacheOperations.CACHEANDDB_DELETE)
//    if (isTesting == false) { //If this is not a test delte it
//      local.delete_! //delete from db
//    }
//    cache.delete(local) //delete from cache
//  }
//
//  /**
//   * Used at cache startup, it will load all avaiable objects from the db and inserts them into the cache
//   */
//  protected def cacheAndDbReloadAll() {
//    //log.trace("cacheAndDbReloadAll before: " + cache.idCache.size)
//    cacheDeleteAll()
//    //log.trace("After cache clean: " + cache.idCache.size)
//    if (isTesting == false) { //If this is not a test save it
//      val fromdb = myType.getSingleton.findAll()
//      //log.trace("Loaded rows from db: " + fromdb.size)
//      cacheInsertAll(fromdb)
//    }
//  }
//
//  /**
//   * Based on an incoming (not cached) object's id we retrieve the cached object
//   * Then all the changed fields of the incoming object will be copied into the cached object
//   */
//  protected def updateLocalFromRemote(remote: V): V = {
//    if (useClones == true) { //If we use clones, we must copy all the data from the cloned objects to the original objects
//      val local = cache.getById(remote.id.is).get
//      //traceObject(local)
//      //log.trace("updateLocalFromRemote: " + local)
//      for (f <- remote.allFields) {
//        val remoteField = f.asInstanceOf[MappedField[Any, V]]
//        //log.trace("copyFromRemote: " + remoteField.name +  ": " + remoteField.get + " dirty: " + remoteField.dirty_?)
//        if (remoteField.dirty_?) { //Did the value of this field changed ?
//          val localField = local.fieldByName(f.name).open_!
//          //log.trace("updateLocalFromRemote local: " + localField.name + ": " + localField.get + " dirty: " + localField.dirty_?)
//          //log.trace("updateLocalFromRemote remote: " + remoteField.name + ": " + remoteField.get + " dirty: " + remoteField.dirty_?)
//          localField.setFromAny(f.get);
//          //          if (remoteField.isInstanceOf[MappedPasswordReadable[_]]) {  
//          //            val pwvalue = f.asInstanceOf[MappedPasswordReadable[_]].readablePassword //once a password was set into the password field it can not be read from there, we must use this passwordclass
//          //            //log.trace("Password field: " + pwvalue)
//          //            if (pwvalue.length >= 3 ) {
//          //              localField.asInstanceOf[MappedPasswordReadable[_]].setList(List(pwvalue,pwvalue))
//          //            }
//          //          }
//        }
//      }
//      //traceObject(local)
//      return local
//    } else { //As we returned previously the original objects and they were changed, we do not have to do anything
//      cache.update(remote) //I DO NOT understand why but if we do not use clones this should be done, even though another thread updated the cached object directly
//      return remote
//    }
//  }
//
//  /**
//   * When you retrieve an object from the cache you do not get that object but a clone from it
//   * This way you can't fuck up the cache. If you would get the cached objects and woud modify them
//   * then others retrieving the same object could get a half updated version of the same object
//   * So this clones a cached object and returns a new one which can be safely given to other threads to paly with
//   */
//  protected def cloneObject(existing: V): V = {
//    if (useClones == true) {
//      //val cloned = existing.getClass.newInstance.asInstanceOf[V]  
//      val cloned = existing.getSingleton.create
//      //log.trace("cloneObject: " + existing)
//      for (localField <- existing.allFields) {
//        val clonedField = cloned.fieldByName(localField.name).open_!.asInstanceOf[MappedField[Any, V]]
//        //val origField = f.asInstanceOf[MappedField[Any, V]]
//        //log.trace("existing: " + f.name + " = " +  f.get + " - " + f.getClass.toString  )
//        //log.trace("cloned: " + clonedField.name + " = " +  clonedField.get + " - " + clonedField.actualField(cloned))
//        clonedField.setFromAny(localField.get);
//        clonedField.resetDirty
//        //log.trace("cloneObject local: " + localField.name + ": " + localField.get + " dirty: " + localField.asInstanceOf[MappedField[Any, V]].dirty_?)
//        //log.trace("cloneObject cloned: " + clonedField.name + ": " + clonedField.get + " dirty: " + clonedField.dirty_?)
//        //        if (clonedField.isInstanceOf[MappedPasswordReadable[_]]) {  
//        //          val pwvalue = f.asInstanceOf[MappedPasswordReadable[_]].readablePassword //once a password was set into the password field it can not be read from there, we must use this passwordclass
//        //          //log.trace("Password field: " + pwvalue)
//        //          clonedField.asInstanceOf[MappedPasswordReadable[_]].setList(List(pwvalue,pwvalue))
//        //        }
//      }
//      cloned.id.set(existing.id.is)
//      return cloned
//    } else {
//      return existing
//    }
//  }
//
//  protected def traceObject(obj: V) {
//    log.trace("=== traceObject ===: " + obj)
//    for (l <- obj.allFields) {
//      val localField = obj.fieldByName(l.name).open_!
//      log.trace("field: " + localField.name + ": " + localField.get + " dirty: " + localField.dirty_?)
//    }
//  }
//
//  protected def reloadAtStart() {
//    //val note = new Notification(CacheOperations.CACHEANDDB_RELOAD); //Schedule for later loading (needed for jetty)
//    //net.liftweb.actor.LAPinger.schedule(this, note, math.round((math.random * 1000 + 4000)));
//    //this.bangMe(CacheOperations.CACHEANDDB_RELOAD)
//    cacheAndDbReloadAll()
//  }
//
//  //  /**
//  //   * Just an empty method to override
//  //   */
//  //  protected def logActivity(obj: V, etype: String) {
//  //    org.tresto.billing.model.accessor.AcsActivityLog.addLog(obj,etype)
//  //  }
//
//}
//
////trait ActivityLogger[V <: LongKeyedMapper[V]] { self : LongKeyedMapperProxyBase[V] =>
////
////  protected def logActivity(obj: V, etype: String) {
////    org.tresto.billing.model.accessor.AcsActivityLog.addLog(obj,etype)
////  }
////  
////}
////
//
