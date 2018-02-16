package org.tresto.language.snippet

import net.liftweb._
import mapper._
import http.S
import common._
import util._
import org.tresto.traits.LogHelper;
import scala.xml._
import java.util.Locale
import java.util.Date;

trait LangConfig {

  def langsourceDriver = Props.get("langsource.driver")
  def langsourceDriverUrl = Props.get("langsource.driverUrl")
  def langsourceUser = Props.get("langsource.user")
  def langsourcePassword = Props.get("langsource.password") 
  
  var isDebugMode: Boolean = false //In debug mode you can see the language keys and not their translation
  var showHints: Boolean = false  //Besides the translation itself it will generate a hint, which will pop up when hoovered over
  
  @volatile
  var canInsert2Database = false //This decides whether we can insert new language keys into the database, at setup or cache reload we cannot!
  
  var appname:Option[String] = None  //If there is an appname, we will use keys only with this appname (if you store more than one applications languages in the same db)
  //===================================================================================================================================================  
  /**
   * Get the locale for the current user, currently the browsers settings
   * Override this with something like this for example:
   *
   * val cuser = MapUser.curUserId
   * cuser.is.isEmpty match { //determine the locale based on if there is a logged in user or not
   *   case true => S.locale.toString //The session's locale (from the users browser settings)
   *   case false => AcsUser.cacheGetById(cuser.is.open_!).get.locale.get //user is logged in
   * }
   */
  def getLocale(): String = {
    //log.trace("locale1: " + S.locale)
    //log.trace("locale2: " + S.request.open_!.request.locale)
    //log.trace("locale3: " + Locale.getDefault)
    S.locale.toString //The session's locale (from the users browser settings)
  }
  

} 