package code.snippet

import scala.xml._
import org.tresto.traits.LogHelper
import _root_.net.liftweb._
import http._
import mapper._
import S._
import SHtml._
import js._
import common._
import util._
import Helpers._
import code.model.MapUser
import code.model.cache.AcsUser
import net.liftweb.http.js.JsCmds.{CmdPair, SetHtml}
import org.tresto.language.snippet.Lang
import org.tresto.utils.BillingConstants


object DBOperation {
  val UPDATE = "U"
  val INSERT = "I"
}

///**
//  * Can be used as html head title attribute, generate a language key from the link of the page
//  */
//object PageTitle {
//  def render(): NodeSeq = {
//    //log.debug("aa " + S.uri)
//    var title = "title" + S.uri.replaceAll("/", ".")
//    //log.debug(title.lastIndexOf(".") + "==" + title.length())
//    if (title.lastIndexOf(".") == title.length() - 1) { //The default page in a directory, sometimes just the link is given without index.html
//      title = title + "index"
//    }
//    val pwdReset = "reset_password"
//    if (title.indexOf(pwdReset) > 0) {
//      title = title.substring(0, title.indexOf(pwdReset) + pwdReset.length())
//    }
//    //val res = Lang.gs(title)
//    //log.debug("PageTitle: " + title + " res: " + res)
//    //For some reason if I just return a Text(res) and the in the template I do <title data-lift="PageTitle.render"></title> it will not reder as a title, neither this way: <title><span data-lift=""....
//    <title>{ title }</title>
//  }
//}


object LoginSnippet extends LogHelper {
  val className = this.getClass().getSimpleName()

  def render(): NodeSeq = {

    val curUserId = MapUser.curUserId.is
    if (curUserId.isDefined == true) { //did the MapUser log in ?
//      val us = AcsUser.cacheGetById(MapUser.curUserId.is.open_!).get
//      us.userType.get match {
//        case MapUser.USERTYPE_XYTONVISOR => getHyperadminScreen(us)
//        case MapUser.USERTYPE_STAFF => getDashboard(us)
//        case MapUser.USERTYPE_VOIP => getDashboard(us)
//        case MapUser.USERTYPE_AGENT => S.redirectTo(WebLinks.AGENT_MAIN.getIndex)
//        case _ => <b>WTF</b>
//      }
      val user = AcsUser.cacheGetById(curUserId.head).get
      <h3>You are logged in as {BillingConstants.accessRightsMap(user.userType.get)}</h3>
    } else {
      getStartScreen()
    }
  }

  /**
    * The login page
    */
  def getStartScreen(): NodeSeq = {
    if (MapUser.curUserId.is.isDefined) { //If he is already logged in we should redirect him to the dashboard, to see the loggen in menus
      S.redirectTo(WebLinks.DASHBOARD)
      return NodeSeq.Empty
    }

    <div class="row">
      <div class="col-sm-7">
        <div class="panel panel-default">
          <div class="panel-body">
            Please Log in to read and submit assignements
          </div>
        </div>
      </div>
      <div class="col-sm-5 well">
        { MapUser.login }
        <hr/>
      </div>
    </div>
  }

}



//===================================================================================================
/**
  * Helper class to display (xml snippet) and to check the strength (length, difficulty) of passwords
  * Also to display and check the second password input, if it matches the first instance
  *
  * Usage: include gui1 and gui2 fields in your snippet.
  * They are ajax snippets so they will post back, and update themselfs with green/red colors and messages (password too short/too weak/ string, not matching passwords etc)
  * Before commit/submit call checkThePasswords() function which will return FieldErrors
  */
class PasswordGuiHelper(locale: String) extends LogHelper {

  val MINIMUM_PASSWORD_SCORE = 24 //<= 24 mediocre , <= 34 Strong, <= 44 very strong

  var passwd1 = "" //This variable holds the password entered in the snippet rendered by gui1
  var passwd2 = "" //This variable holds the password entered in the snippet rendered by gui2

  val gui1 = //Use this in a form to display a password entry field, when ajax postback, the password strength will be checked, and with ajax, it will be displayed
    <div id="pwdBar1" class="form-group">
      <label>Password</label>
      <div class="input-group">
        {SHtml.ajaxText("", x => checkPassword1(x)) % ("type" -> "password") % ("class" -> "form-control")}<label class="input-group-addon">
        <strong id="strengthTextId"></strong>
      </label>
      </div>
    </div>

  val gui2 = //Use this in a form to display a password entry field, when ajax postback, the password strength will be checked, and with ajax, it will be displayed
    <div id="pwdBar2" class="form-group">
      <label>Repeat Password</label>
      <div class="input-group">
        {SHtml.ajaxText("", x => checkPassword2(x)) % ("type" -> "password") % ("class" -> "form-control")}<label id="passwordMatch" class="input-group-addon"></label>
      </div>
    </div>

  /**
    * Check the length and strength of a password
    * This will be called by ajax, so during editing the password field, and not at submit.
    * It will update the html fields (sets the color and a message) in the password field
    */
  protected def checkPassword1(pwd: String): JsCmd = {
    log.debug("checkPassword1: " + pwd)
    passwd1 = pwd.trim
    if (passwd1 == null || passwd1.length() < MapUser.PASSWORD_LENGTH_MIN) {
      return CmdPair(JsCmds.SetElemById("pwdBar1", JE.Str("form-group has-error"), "className"), SetHtml("strengthTextId", Lang.g("MapUser.password.too.short")))
    }
    val pwdStrength = org.tresto.utils.security.PasswordCheck.CheckPasswordStrength(passwd1, locale)
    if (pwdStrength.score <= MINIMUM_PASSWORD_SCORE) { //Password too weak, make the input red, and display the verdict
      return CmdPair(JsCmds.SetElemById("pwdBar1", JE.Str("form-group has-error"), "className"), SetHtml("strengthTextId", Text(pwdStrength.verdict)))
    }
    //If the password is OK then return the verdict and make it green
    return CmdPair(JsCmds.SetElemById("pwdBar1", JE.Str("form-group has-success has-feedback"), "className"), SetHtml("strengthTextId", Text(pwdStrength.verdict)))
  }

  /*
   * Check if the 2 passwords entered match
   * This will be called by ajax, so during editing the password field, and not at submit.
   * It will update the html fields (sets the color and a message) in the password field
   */
  protected def checkPassword2(pwd: String): JsCmd = {
    passwd2 = pwd.trim
    log.debug("checkPassword2: " + passwd2 + " == " + passwd1)
    (passwd2 == passwd1) match {
      case true => CmdPair(JsCmds.SetElemById("pwdBar2", JE.Str("form-group has-success has-feedback"), "className"), SetHtml("passwordMatch", Lang.g("MapUser.password.match.ok")))
      case false => CmdPair(JsCmds.SetElemById("pwdBar2", JE.Str("form-group has-error"), "className"), SetHtml("passwordMatch", Lang.g("MapUser.password.not.match")))
    }
  }

  /**
    * Check password length and strength. This should be called at submit time, and it will return an error we can display
    *
    * @returns None if all is ok
    */
  def checkThePasswordLonely(): List[FieldError] = {
    if (passwd1 == null || passwd1.length() < MapUser.PASSWORD_LENGTH_MIN) {
      return List(FieldError(MapUser.password, Lang.g("MapUser.password.too.short")))
    }
    val pwdStrength = org.tresto.utils.security.PasswordCheck.CheckPasswordStrength(passwd1, "en")
    if (pwdStrength.score <= MINIMUM_PASSWORD_SCORE) { //<= 24 mediocre , <= 34 Strong, <= 44 very strong
      return List(FieldError(MapUser.password, Lang.g("MapUser.password.too.weak")))
    }
    return Nil
  }

  def checkThePasswords(): List[FieldError] = {
    if (passwd1 == null || passwd1.length() < MapUser.PASSWORD_LENGTH_MIN) {
      return List(FieldError(MapUser.password, Lang.g("MapUser.password.too.short")))
    }
    val pwdStrength = org.tresto.utils.security.PasswordCheck.CheckPasswordStrength(passwd1, "en")
    if (pwdStrength.score <= MINIMUM_PASSWORD_SCORE) { //<= 24 mediocre , <= 34 Strong, <= 44 very strong
      return List(FieldError(MapUser.password, Lang.g("MapUser.password.too.weak")))
    }
    if (passwd1 != passwd2) {
      return List(FieldError(MapUser.password, Lang.g("MapUser.password.not.match")))
    }
    return Nil
  }

}
