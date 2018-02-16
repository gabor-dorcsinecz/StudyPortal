package org.tresto.cache

import scala.collection.mutable._
import org.tresto.traits.LogHelper

/**
 * A cache searchable by id storing any object you pass to it
 * You can also create searchfield by giving a search function which will be applied to the cached object to obtain the field
 * THIS IS NOT THREAD SAFE!. Put this into an actor or inside a class
 * example:
 *
 * case class LoUser(id:Long, name:String)
 *
 * val SFNAME = "name"
 * val tc = new TCache[Long,LoUser]()
 * def nameSearch(x:LoUser):String = x.name
 * tc.addSearchField(SFNAME,nameSearch)
 * val l1 = new LoUser(id = 1, name = "pista")
 * val l2 = new LoUser(id = 2, name = "joska")
 * val l3 = new LoUser(id = 3, name = "pista")
 *
 * tc.insert(l1)
 * tc.insert(l2)
 * tc.insert(l3)
 * println(tc)
 *
 * val res1 = tc.findAll(SFNAME,"pista")
 * println("\r\nres: " + res1)
 * println(tc)
 *
 * val l11 = l1.copy( name = "ujpista")
 * tc.update(l11)
 * val res2 = tc.findAll(SFNAME,"pista")
 * println("\r\nres: " + res2)
 * println(tc)
 *
 * tc.delete(l3)
 * val res3 = tc.findAll(SFNAME,"pista")
 * println("\r\nres: " + res3)
 * println(tc)
 *
 */
class TCache[K, V](idExtractor: V => K) extends LogHelper {

  /**
   * The original map containing object with their id's as keys
   */
  val idCache = new HashMap[K, V]

  /**
   * A map containing the extra searxh fields
   */
  val searchMap = new HashMap[String, TSearchField[Any, K, V]]

  /**
   * Add a search field to the original map
   * @param fieldName the name of the search field like username. This you must provide, because later, when you want to search you have to provide this name again
   * @param extractor a function that extracts from the cached objects the value of a search field this can be one field, or an aggregate of many fields
   */
  def addSearchField(fieldName: String, extractor: V => Any) {
    val sf = new TSearchField[Any, K, V](fieldName, extractor)
    searchMap += fieldName -> sf
  }

  def getById(id: K): Option[V] = {
    idCache.get(id)
  }

  def insert(v: V) {
    if (getById(idExtractor(v)).isEmpty) { //Do not insert twice
      idCache.put(idExtractor(v), v) //Put it into the main hashmap
      for (sm <- searchMap.values) { //for every Multimap
        sm.insert(idExtractor(v), v)
      }
    } else {
      //log.trace("Tried to insert twice: " + v)
    }
  }

  /**
   * In case of an update operation, we do not replace the object in the cache, we only update it,
   * so we must also update the search fields
   */
  def updateSearchFields(v: V) {
    for (sm <- searchMap.values) { //for every Multimap
      sm.update(idExtractor(v), v)
    }
  }

  def delete(v: V) {
    val prev = idCache.remove(idExtractor(v)).get
    for (sm <- searchMap.values) {
      sm.delete(idExtractor(prev), prev)
    }
  }

  def update(v: V) {
    delete(v)
    insert(v)
  }

  def getAll(): List[V] = {
    idCache.values.toList
  }

  def insertAll(lv: List[V]) {
    log.trace("insertAll: " + lv.length)
    lv.foreach(insert(_))
  }

  /**
   * Clear/delete all entries from this cache
   */
  def deleteAll() {
    idCache.clear
    for (sm <- searchMap.values) {
      sm.deleteAll
    }
  }

  /**
   * Find all objects having one fieldValue (like username == 'pista')
   * @param fieldName this you have provided when you added the searchField in addSearchField
   * @param fieldValue the value you are searching for
   */
  def findAll(fieldName: String, fieldValue: Any): List[V] = {
    //log.debug("findAll: " + fieldName + " -> " + fieldValue)
    val oids = searchMap(fieldName).searchMap.get(fieldValue)
    //log.debug("matching id's: " + oids)
    if (oids.isEmpty) {
      //log.debug("searchMap: " + searchMap(fieldName).searchMap)
      return Nil
    }
    var lb = new ListBuffer[V]
    for (i <- oids.get) {
      lb += idCache(i)
    }
    //log.debug("matching objects's: " + lb)
    return lb.toList
  }

  override def toString(): String = {
    var ret = idCache.toString + "\r\n"
    searchMap.foreach(a => ret += a.toString)
    return ret
  }

  def size(): Int = {
    idCache.size
  }
}

/**
 * A reverse multimap, lets say we have a TCache with long keys and conatining objects, which have a name field
 * We want to find those objects by name too, so we create a TSearchField.
 * TSearchField conatins a map where the name is the key, and the id's in TCache are the values.
 * So when we want to find something by name we just search here ,we find the id of the object,
 * then we search in TCache with that id and retrieve the object
 */
case class TSearchField[SF, K, V](fieldName: String, extractor: V => SF) extends LogHelper {

  val searchMap = new HashMap[SF, Set[K]]() with MultiMap[SF, K] //e.g. for a name map: Map("pista" -> Set(189), "joska" -> Set(187), "gabor" -> Set(5, 10, 25, 184, 2))

  def insert(k: K, v: V) {
    val ext = extractor(v)
    //log.debug("insert: " + fieldName + " k: " + k + " ext: " + ext + " v: " + v)
    searchMap.addBinding(ext, k)
  }

  def delete(k: K, v: V) {
    val ext = extractor(v)
    //log.debug("delete: " + fieldName + " k: " + k + " ext: " + ext + " v: " + v)
    searchMap.removeBinding(extractor(v), k)
  }

  def update(k: K, v: V) {
    val newValue = extractor(v)
    val test = searchMap.get(newValue)
    //log.debug("update: " + fieldName + " k: " + k + " newValue: " + newValue + " test: " + test + " v: " + v)
    for (sm <- searchMap) { //Remove the original value, browse through all Multipams
      //val old = sm._2.find(a => a == k)
      //log.debug("Old value: " + old)
      sm._2.remove(k)
    }
    searchMap.addBinding(newValue, k)
    //    if (test.isEmpty || (test.isDefined && test.find(a => a == k).isEmpty)) {
    //      //log.debug("Update Search Field: " + fieldName + " will be updated on key: " + newvalue)
    //      delete(k, v)
    //      insert(k, v)
    //    }
  }

  /**
   * Clear all the values from the searh field
   */
  def deleteAll() {
    searchMap.clear()
  }

  override def toString(): String = {
    "Searchfield's Name: " + fieldName + ": " + searchMap.toString
  }
} 