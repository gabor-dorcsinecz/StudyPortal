package org.tresto.language.snippet

import net.liftweb._
import common._
import http._
import mapper._
import http.S
import org.tresto.traits.LogHelper;
import org.tresto.language.model._;
import scala.xml._
import java.util.Locale
import scala.collection.mutable.HashMap
import java.util.Date;

/**
 * Retrieve language string by keys from a cached map.
 * Use it from any snippet
 * For speed we sacrifice thread safety!
 */
object Lang extends LogHelper {
  val markUsed = false //when this is true, all language file get operations will update the language database, so that we can trace all unused keys
  //val markdownParser = new eu.henkelmann.actuarius.ActuariusTransformer()
  val recursiveLanguageKeyPattern = java.util.regex.Pattern.compile("__(.*?)__", java.util.regex.Pattern.DOTALL)

  val LOC_HU = "hu"
  val LOC_EN = "en"
  val LOC_RO = "ro"
  val LOC_PL = "pl"

  /**
   * Depending on the given locale(language) return the appropriate field of the database object (which should be in the same language)
   */
  def getFieldByLocale(ml: MapLanguage, loc: String): String = {
    if (loc.toLowerCase().indexOf(LOC_HU) == 0) {
      return ml.hu_hu.get
    } else if (loc.toLowerCase().indexOf(LOC_RO) == 0) {
      return ml.ro_ro.get
    } else if (loc.toLowerCase().indexOf(LOC_PL) == 0) {
      return ml.pl_pl.get
    } else {
      return ml.en_us.get
    }
  }

  /**
   * This is the only method you MUST CALL at setup!
   */
  def init(cfg: LangConfig) {
    this.config = cfg
    LiftRules.addToPackages("org.tresto.language")
//    if (!DB.jndiJdbcConnAvailable_?) {
//      val languageVendor = new LanguageVendor(config.langsourceDriver.open_!, config.langsourceDriverUrl.open_!, config.langsourceUser, config.langsourcePassword)
//      DB.defineConnectionManager(LanguageConnectionIdentifier, languageVendor)
//      LiftRules.unloadHooks.append(languageVendor.closeAllConnections_! _)
//    } else {
//      log.error("ERROR initializing Language database connection: " + config.langsourceDriver.open_! + " " + config.langsourceDriverUrl.open_! + " " + config.langsourceUser)
//    }//    Schemifier.schemify(true, Schemifier.infoF _, LanguageConnectionIdentifier, MapLanguage)

    deduplicate()

    val langRunnable = new java.lang.Runnable() { //Load language keys async, so it does not slows down the main thread
      def run() {
        cacheAndDbReloadAll()
      }
    }
    val langThread = new java.lang.Thread(langRunnable)
    langThread.start()
    //cacheAndDbReloadAll()

  }

  def getLocale() = config.getLocale()
  def isDebugMode() = config.isDebugMode
  def setDebugMode(mode: Boolean) {
    config.isDebugMode = mode
  }
  def showHints() = config.showHints
  def setShowHints(mode: Boolean) {
    config.showHints = mode
  }

  val pLANGID = "lid"
  val editorLinkList = List("ent", "system", "language")
  var editorLink = editorLinkList.mkString("/", "/", "/")
  private var config: LangConfig = _
  private val cache = new scala.collection.mutable.HashMap[String, MapLanguage]()

  //===================================================================================================================================================  
  def cacheGetByKey(iid: String): Option[MapLanguage] = {
    cache.get(iid)
  }
  def cacheGetAll(): List[MapLanguage] = {
    cache.values.toList
  }
  def cacheAndDbInsert(v: MapLanguage): MapLanguage = {
    if (config.canInsert2Database == true) { //maybe we are at startup time and the cache is still empty (can happen after restart)
      v.save
      log.debug("cacheAndDbInsert: " + v)
      cache.put(v.langkey.get, v)
    }
    return v
  }
  def cacheAndDbUpdate(v: MapLanguage) {
    //log.trace("cacheAndDbUpdate1: " + v)
    v.save()
    log.trace("cacheAndDbUpdate2: " + v)
    //val local = cache.getById(v.langkey.get)
    val local = cache.put(v.langkey.get, v)
    //log.trace("cacheAndDbUpdate3: " + local)
    //cache.delete(v)
    //cache.insert(v)
  }
  def cacheAndDbDelete(v: MapLanguage) {
    v.delete_! //delete from db
    cache.remove(v.langkey.get) //delete from cache
  }
  def cacheAndDbReloadAll() {
    config.canInsert2Database = false //While loading data we cannot insert any new data
    val nlist = config.appname.isDefined match {
      case true => MapLanguage.findAll(By(MapLanguage.appId, config.appname.get)) //Load language only for this app (if you store more than one apps languages in the same db)
      case false => MapLanguage.findAll() //Load all language rows from the database
    }

    log.trace("Lang cacheAndDbReloadAll: " + nlist.length)
    println("Lang cacheAndDbReloadAll: " + nlist.length) //Sometimes the logger framework is not yet ready ...
    if (nlist.length > 0) { //Only replace the language keys if we find any!
      cache.clear
      nlist.foreach(u => cache.put(u.langkey.get, u))
    }

    config.canInsert2Database = true
  }

  def getHtmlLink(key: String, args: List[Any]): NodeSeq = {
    <a href={ Lang.editorLink + SnLanguage.aEDIT + "?" + Lang.pLANGID + "=" + key }>{ Lang.getKeyWithArgs(key, args) }</a>
  }

  /**
   * Use it like this form a snippet:  <lift:Lang.g>edit</lift:Lang.g>
   */
  def g(lkey: NodeSeq): NodeSeq = {
    g(lkey.toString)
  }
  def g(lkey: String): NodeSeq = {
    g(lkey, config.getLocale(), Nil)
  }
  def g(lkey: String, locale: String): NodeSeq = {
    g(lkey, locale, Nil)
  }
  /**
   * Provide an argument list that will be included in the language key:
   *   Lang.g("payment.sellercompany", Lang.getLocale(), List("firstParam", "secondParam")) 
   * Then in the language key, place the string parameters:
   *   payment.sellercompany = "Please have %1$s or another (%2$s)"
   */
  def g(lkey: String, locale: String, args: List[Any]): NodeSeq = {
    if (config.isDebugMode) {
      return Text(getKeyWithArgs(lkey, args))
    }

    def getHintKey(key: String): String = key + ".hint"

    //    def getHintTranslation(key: String): String = {
    //      //log.trace("getHintTranslation: " + key )
    //      val translation4language = gs(getHintKey(key))
    //      (translation4language == "#") match {
    //        case true => ""
    //        case false => translation4language
    //      }
    //    }

    /**
     * @param translation4language is the translation of lkey in the locale given by locale
     * @param lkey the language key
     * @param keyType is it text/markdown/textile/html ?
     * @param the locale like hu_hu, or en_us
     */
    def getLanguageTranslation(translation4language: String, lkey: String, keyType: String, locale: String): NodeSeq = {
      if (translation4language.length == 0) { //If it is empty (there is no transaltion for the key), we offer a link!
        getHtmlLink(lkey, args)
      } else { //We have a value for this key in the given language, show the translation
        val oht = cacheGetByKey(getHintKey(lkey))
        //log.debug("hint: " + lkey + " -> " + oht)
        (oht.isDefined) match {
          case true => //There is a on object for this hint already
            val field = getFieldByLocale(oht.get, locale)
            (field != null && field.length > 0) match {
              case true =>
                <span title={ field }>{ makeOrLeaveAsXml(getValueWithArgs(translation4language, args), keyType) }</span>
              case false =>
                makeOrLeaveAsXml(getValueWithArgs(translation4language, args), keyType)
            }
          case false => //There is a NO object for this hint 
            makeOrLeaveAsXml(getValueWithArgs(translation4language, args), keyType)
        }
      }
    }

    //val lres = langMap.get(lkey)
    val lres = cacheGetByKey(lkey)
    //    if (didStartup == false) { //We load the language keys assync, so at the beginning we do not know if a key does not exist because it was not loaded, or loaded but does not exist
    //      if (System.currentTimeMillis() - timeOfStart  > 300000) {
    //        didStartup = true
    //      }
    //    }
    //log.debug("lres: " + lkey + " " + lres)
    if (lres.isEmpty) { //No such language key, we create it
      //      if (didStartup) {
      createNewLanguageEntry(lkey)
      if (config.showHints) {
        createNewLanguageEntry(getHintKey(lkey))
      } else {
        log.debug("Language data is not yet loaded into the system for key: " + lkey)
      }
      //      }
      getLanguageTranslation("", lkey, MapLanguage.TYPE_TEXT, locale)
    } else { //There is a language key
      val textType = if (lres.get.textType.get == null || lres.get.textType.get.length == 0) MapLanguage.TYPE_TEXT else lres.get.textType.get //To be compatible if there is no textType
      val field = getFieldByLocale(lres.get, locale)
      getLanguageTranslation(field, lkey, textType, locale)
      //      if (locale == "hu_HU" || locale == "hu") { //Hungarian case
      //        getLanguageTranslation(lres.get.hu_hu.get, lkey, textType)
      //      } else if (locale == "ro_ro" || locale == "ro") { //Romanian case
      //        getLanguageTranslation(lres.get.ro_ro.get, lkey, textType)
      //      } else if (locale == "pl_pl" || locale == "pl") { //Polish case
      //        getLanguageTranslation(lres.get.ro_ro.get, lkey, textType)
      //      } else { //This is english, the default language/case
      //        getLanguageTranslation(lres.get.en_us.get, lkey, textType)
      //      }
    }

  }

  def gs(lkey: String): String = {
    gs(lkey, config.getLocale(), Nil)
  }
  def gs(lkey: String, locale: String): String = {
    gs(lkey, locale, Nil)
  }
  /**
   * Used for submit forms, where lift requires string and not xml, returns only a string, no links, use for e.g.: save, no , stb
   */
  def gs(lkey: String, locale: String, args: List[Any]): String = {
    if (config.isDebugMode) {
      return getKeyWithArgs(lkey, args)
    }
    val lres = cacheGetByKey(lkey)

    if (lres.isEmpty) { //No such language key, we create it
      createNewLanguageEntry(lkey)
      getKeyWithArgs(lkey, args)
    } else { //There is a language key
      if (locale == "hu_HU" || locale == "hu") { //Hungarian case
        if (lres.get.hu_hu.get.length == 0) { //If it is empty, we offer a link!
          getKeyWithArgs(lkey, args)
        } else {
          getValueWithArgs(lres.get.hu_hu.get, args)
        }
      } else {
        if (lres.get.en_us.get.length == 0) { //If it is empty, we offer a link!
          getKeyWithArgs(lkey, args)
        } else {
          getValueWithArgs(lres.get.en_us.get, args)
        }
      }
    }

  }

  /**
   * Get the language key itself back as a string, it it has arguments include them too in the string
   * @param lkey the language key
   * @param args the arguments supplied to this language key
   */
  def getKeyWithArgs(lkey: String, args: List[Any]): String = {
    (args.length > 0) match {
      case true => "[" + lkey + "] with " + args.size + " parametes: " + args.mkString("(", ",", ")")
      case false => "[" + lkey + "]"
    }
  }

  /**
   * For a given translation (so not the key, but it's translation in some language) and the parameters provided, fill the parameters into the translation
   * For example translation= "hello %s, you have %d emails", and args = List("Mr Nice",5) it will produce: "hello Mr Nice, you have 5 emails"
   * There may be arguments for the language key, we add them here
   * @param translation e.g.:"hello %s, you have %d emails"
   * @param args List("Mr Nice",5)
   * @returns "hello Mr Nice, you have 5 emails"
   */
  def getValueWithArgs(translation: String, args: List[Any]): String = {
    val unq = removeQuote(translation)
    val argsinlang = unq.split("%").length - 1
    //log.trace("translation: " + translation + " @ " + args + " # " + args.length + " ? " + argsinlang)
    (args.length > 0) match {
      case true => //We have some arguments
        (args.size == argsinlang) match { //We 
          case true => unq.format(args: _*) //Format the args
          case false => unq + " with " + args.size + " parametes: " + args.mkString("(", ",", ")") //Print the args anyway!
        }
      case false => unq
      //        (unq.indexOf("__") >= 0) match { //Is this a recursive language key
      //          case true =>
      //            val sb = new StringBuffer()
      //            val m = recursiveLanguageKeyPattern.matcher(unq)
      //            while (m.find()) {
      //              val g1 = m.group(1)
      //              m.appendReplacement(sb, gs(g1));
      //            }
      //            m.appendTail(sb)
      //            log.debug("recursive language key: " + translation + " -> " + sb)
      //            sb.toString
      //          case false =>
      //            unq
      //        }

    }
  }

  /**
   * Postgre cannot save a single quote, we have to escape ith with another quote
   */
  def addQuote(in: String): String = {
    (in.indexOf("'") > 0) match {
      case true => in.replaceAll("'", "''")
      case false => in
    }
  }
  def removeQuote(in: String): String = {
    (in.indexOf("'") > 0) match {
      case true => in.replaceAll("''", "'")
      case false => in
    }
  }

  /**
   * When we find a new language entry (not in the database) we create a new empty language key in the database
   */
  def createNewLanguageEntry(pkey: String): MapLanguage = {
    val existing = MapLanguage.find(By(MapLanguage.langkey, pkey), By(MapLanguage.appId, config.appname.get)) //Try to find it first to avoid inserting it at startup time
    if (existing.isEmpty) {
      val ll = MapLanguage.create.langkey(pkey).lastmodified(new java.util.Date()).enabled(true).hu_hu("").en_us("").ro_ro("").pl_pl("") //Create a language entry with the missing key
      if (config.appname.isDefined) {
        ll.appId(config.appname.get) //If we started with an appname, we will use it!
      }
      (config.canInsert2Database == true) match {
        case true =>
          log.trace("Creating new language entry: " + ll)
          return cacheAndDbInsert(ll)
        case false =>
          return ll
      }
    } else { //This key already exists in the database
      return existing.head
    }
  }

  /**
   * ================================================================================================================================================
   * At init we check if there may be any duplicated language keys
   */
  def deduplicate() {
    val langDuplicates = "select appid, langkey, count(*) c from languages GROUP BY appid, langkey HAVING count(*) > 1 ORDER BY c desc"
    //val langKey = "select * from languages where langkey = ?"
    val duplicates = listList2ListHashMap(DB.performQuery(langDuplicates, Nil))
    for (drow <- duplicates) {
      val dkey = drow("langkey").toString()
      //log.trace("Duplicate key: " + dkey)
      val dups4key = MapLanguage.findAll(By(MapLanguage.langkey, dkey), OrderBy(MapLanguage.id, Ascending))
      //log.trace("Duplicate rows: " + dups4key)
      val deletables = dups4key.sortWith((a, b) => a.en_us.get.length() > b.en_us.get.length()).drop(1)
      //log.trace("Deletable rows: " + deletables)
      log.error("Duplicates for language key: " + dkey + " will delete: " + deletables.length + " out of: " + dups4key.length)
      println("Duplicates for language key: " + dkey + " will delete: " + deletables.length + " out of: " + dups4key.length)
      for (d <- deletables) {
        d.delete_!
      }
    }

  }

  /**
   * If there is only one xhtml part in it we will consider it as html
   */
  def isHtml(inc: String): Boolean = {
    if ((inc.indexOf("<") >= 0) && (inc.indexOf("/>") >= 0 || inc.indexOf("</") >= 0)) {
      return true
    } else {
      return false
    }
  }
  /**
   * If the incoming string is html, parse it as xml, if it is string, make html Text node from it
   */
  def makeOrLeaveAsXml(inc: String, keyType: String): scala.xml.NodeSeq = {
    keyType match {
      case MapLanguage.TYPE_XML => Unparsed(inc)
//      case MapLanguage.TYPE_TEXTILE => org.tresto.util.parser.textile.TextileParser.toHtml(inc)
//      case MapLanguage.TYPE_MARKDOWN => Unparsed(markdownParser(inc)) //This can be dangerous ... html5 parser from lift would be better
      case _ => //Includes the simple TYPE_TEXT case too
        isHtml(inc) match { //This is mainly for compatibility!
          case true => Unparsed(inc)
          case false => new scala.xml.Text(inc)
        }
    }
  }

  /**
   * DB.performQuery returns this result
   */
  def listList2ListHashMap(in: (List[String], List[List[Any]])): List[Map[String, Any]] = {
    val lb = new scala.collection.mutable.ListBuffer[Map[String, Any]]()
    for (i <- 0 until in._2.length) { //All rows
      val mp = new scala.collection.mutable.HashMap[String, Any]()
      for (j <- 0 until in._1.length) {
        mp += in._1(j) -> in._2(i)(j)
      }
      lb += mp.toMap
    }
    return lb.toList
  }

  //===================================================================================================================================================  
  def cancel(): NodeSeq = g("gen.cancel")
  def back(): NodeSeq = g("gen.back")
  def save(): NodeSeq = g("gen.save")
  def no(): NodeSeq = g("gen.no")
  def yes(): NodeSeq = g("gen.yes")
  def edit(): NodeSeq = g("gen.edit")
  def delete(): NodeSeq = g("gen.delete")
  def id(): NodeSeq = g("gen.id")
  def filter(): NodeSeq = g("gen.filter")
  def view(): NodeSeq = g("gen.view")
  def failed(): NodeSeq = g("gen.failed")
  def success(): NodeSeq = g("gen.success")

  def edits(): String = gs("gen.edit")
  def deletes(): String = gs("gen.delete")
  def saves(): String = gs("gen.save")
  def nos(): String = gs("gen.no")
  def yess(): String = gs("gen.yes")
  def filters(): String = gs("gen.filter")
  def views(): String = gs("gen.view")
  def successs(): String = gs("gen.success")
  def faileds(): String = gs("gen.failed")

}








