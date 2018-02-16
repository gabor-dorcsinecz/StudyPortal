package org.tresto.cache

import scala.collection.mutable._
import scala.concurrent.stm._
import org.tresto.traits.LogHelper

class STMCache[K, V](idExtractor: V => K) extends LogHelper {

  /**
   * The original map containing object with their id's as keys
   */
  val idCache = Ref(new HashMap[K, V])

  /**
   * A map containing the extra searxh fields
   */
  val searchMap = Ref(new HashMap[String, TSearchField[Any, K, V]])

  /**
   * Add a search field to the original map
   * @param fieldName the name of the search field like username. This you must provide, because later, when you want to search you have to provide this name again
   * @param extractor a function that extracts from the cached objects the value of a search field this can be one field, or an aggregate of many fields
   */
  def addSearchField(fieldName: String, extractor: V => Any) {
    val sf = new TSearchField[Any, K, V](fieldName, extractor)
    atomic { implicit txn =>
      searchMap.get += fieldName -> sf
    }

  }

  def getById(id: K): Option[V] = {
    atomic { implicit txn =>
      idCache.get.get(id)
    }
  }

  def insert(v: V) {
    atomic { implicit txn =>
      if (getById(idExtractor(v)).isEmpty) { //Do not insert twice
        idCache.get.put(idExtractor(v), v) //Put it into the main hashmap
        for (sm <- searchMap.get.values) { //for every Multimap
          sm.insert(idExtractor(v), v)
        }
      } else {
        log.trace("Tried to insert twice: " + v)
      }
    }
  }

  /**
   * In case of an update operation, we do not replace the object in the cache, we only update it,
   * so we must also update the search fields
   */
  def updateSearchFields(v: V) {
    atomic { implicit txn =>
      for (sm <- searchMap.get.values) { //for every Multimap
        sm.update(idExtractor(v), v)
      }
    }
  }

  def delete(v: V) {
    atomic { implicit txn =>
      val prev = idCache.get.remove(idExtractor(v)).get
      for (sm <- searchMap.get.values) {
        sm.delete(idExtractor(prev), prev)
      }
    }
  }

  def update(v: V) {
    delete(v)
    insert(v)
  }

  def getAll(): List[V] = {
    atomic { implicit txn =>
      idCache.get.values.toList
    }
  }

  def insertAll(lv: List[V]) {
    log.trace("insertAll: " + lv.length)
    lv.foreach(insert(_))
  }

  /**
   * Clear/delete all entries from this cache
   */
  def deleteAll() {
    atomic { implicit txn =>
      idCache.get.clear
      for (sm <- searchMap.get.values) {
        sm.deleteAll
      }
    }
  }

  /**
   * Find all objects having one fieldValue (like username == 'pista')
   * @param fieldName this you have provided when you added the searchField in addSearchField
   * @param fieldValue the value you are searching for
   */
  def findAll(fieldName: String, fieldValue: Any): List[V] = {
    atomic { implicit txn =>
      //log.debug("findAll: " + fieldName + " -> " + fieldValue)
      val oids = searchMap.get.get(fieldName).get.searchMap.get(fieldValue)
      //log.debug("matching id's: " + oids)
      if (oids.isEmpty) {
        //log.debug("searchMap: " + searchMap(fieldName).searchMap)
        return Nil
      }
      var lb = new ListBuffer[V]
      for (i <- oids.get) {
        lb += idCache.get.get(i).get
      }
      //log.debug("matching objects's: " + lb)
      return lb.toList
    }
  }

  override def toString(): String = {
    atomic { implicit txn =>
      var ret = idCache.toString + "\r\n"
      searchMap.get.foreach(a => ret += a.toString)
      return ret
    }
  }
}

//========================================================================================================================================================================================
//========================================================================================================================================================================================
