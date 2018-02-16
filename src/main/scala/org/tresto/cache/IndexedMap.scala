package org.tresto.cache

import scala.concurrent.stm._

/**
 * http://nbronson.github.io/scala-stm/indexed_map.html
 * https://github.com/nbronson/scala-stm/blob/master/src/test/scala/scala/concurrent/stm/examples/IndexedMap.scala
 */
class IndexedMap[A, B](idExtractor: B => A) {

  private class Index[C](view: (A, B) => Iterable[C]) extends (C => Map[A, B]) {
    val mapping = TMap.empty[C, Map[A, B]]

    /**
     * Search by the given key, e.g. byName("alice") which will return  Map[Int,User]
     */
    def apply(derived: C) = mapping.single.getOrElse(derived, Map.empty[A, B])

    def +=(kv: (A, B))(implicit txn: InTxn) {
      for (c <- view(kv._1, kv._2)) {
        mapping(c) = apply(c) + kv
      }
    }

    def -=(kv: (A, B))(implicit txn: InTxn) {
      for (c <- view(kv._1, kv._2)) {
        val after = mapping(c) - kv._1
        if (after.isEmpty) {
          mapping -= c
        } else {
          mapping(c) = after
        }
      }
    }
  }

  private val contents = TMap.empty[A, B] //[Key,Object] here are the objects actually stored
  private val indices = Ref(List.empty[Index[_]]) //Store the indicies of the objects here

  /**
   * Create a new index on a ceratin field of the stored object
   * val byName = users.addIndex { (id, u) => Some(u.name) } will return  Map[Int,User]
   */
  def addIndex[C](view: (A, B) => Iterable[C]): (C => Map[A, B]) = atomic { implicit txn =>
    val index = new Index(view)
    indices() = index :: indices()
    contents foreach { index += _ }
    index
  }

  /**
   * Get object by it's key
   */
  def get(key: A): Option[B] = contents.single.get(key)

  def getAll(): List[B] = {
    contents.single.values.toList
  }
  
  def removeAll() {
    contents.single.foreach( a=> remove(a._1))
  }

  /**
   * Add a new object, and also create the indexes
   */
  def put(value: B): Option[B] = atomic { implicit txn =>
    val key: A = idExtractor(value)
    val prev = contents.put(key, value)
    for (p <- prev; i <- indices())
      i -= (key -> p)
    for (i <- indices())
      i += (key -> value)
    prev
  }
  
  /**
   * An object already in the cache has been updated
   */
  def update(value: B): Option[B] = atomic { implicit txn =>
    val key: A = idExtractor(value)
    contents.remove(key)
    val prev = contents.put(key, value)
    for (p <- prev; i <- indices())
      i -= (key -> p)
    for (i <- indices())
      i += (key -> value)
    prev
  }

  /**
   * Add an object, and also remove from the indexes
   */
  def remove(key: A): Option[B] = atomic { implicit txn =>
    val prev = contents.remove(key)
    for (p <- prev; i <- indices())
      i -= (key -> p)
    prev
  }
  
  def size():Int = {
    contents.single.size
  }
}

