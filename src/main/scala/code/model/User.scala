//package code
//package model
//
//import net.liftweb.mapper._
//import net.liftweb.util._
//import net.liftweb.common._
//import net.liftweb.http.SessionVar
//import code.snippet._
//
//import scala.xml._
//import org.tresto.traits.LogHelper
//import net.liftweb.common.Full
//import net.liftweb.mapper._
//import net.liftweb.http.SessionVar;
//import net.liftweb.common._;
//import java.math.MathContext
//import net.liftweb.http.S;
//import net.liftweb.sitemap.{Loc, Menu, SiteMap}
//import net.liftweb.sitemap.Loc._
//import _root_.net.liftweb._
//import http._
//import mapper._
//import S._
//import SHtml._
//import common._
//import net.liftweb.util.BaseField
//import net.liftweb.util.FieldError
//import net.liftweb.util.Helpers._
//import js._
//import JsCmds._
//
//
////object Lang {
////  def g(in: String): NodeSeq = Text(in)
////}
//
///**
//  * The singleton that has methods for accessing the database
//  */
//object User extends User with MetaMegaProtoUser[User] with LogHelper {
//  override def dbTableName = "users" // define the DB table name
//  override def screenWrap = Full(<lift:surround with="default" at="content">
//    <lift:bind/>
//  </lift:surround>)
//
//  // define the order fields will appear in forms and output
//  override def fieldOrder = List(id, email, firstName, lastName, locale, timezone, password)
//
//  val PASSWORD_LENGTH_MIN = 8 //Minimum number of characters in the password
//  //
//  // comment this line out to require email validations
//  override def skipEmailValidation = true
//
//  object curUserId extends SessionVar[Box[Long]](Empty) //Will never change inside a session, no need to update ever
//
//
//  override def signup(): Elem = {
//    val theUser: TheUserType = mutateUserOnSignup(createNewUserInstance())
//    val theName = signUpPath.mkString("")
//    val app = S.param("app")
//    val passwordGuiHelper = new PasswordGuiHelper(theUser.locale.get)
//    log.debug("signup app? " + app)
//    var country2 = Countries(Countries.USA.id)
//
//    def testSignup() {
//      //log.debug("passwords: " + passwd1 + " # " + passwd2)
//      theUser.setPasswordFromListString(List(passwordGuiHelper.passwd1, passwordGuiHelper.passwd2))
//      (checkTheEmail(theUser.email.get).toList ::: validateSignup(theUser) ::: passwordGuiHelper.checkThePasswords) match {
//        case Nil =>
//          doAfterSignup(theUser, country2)
//        case xs =>
//          S.error(xs)
//          signupFunc(Full(innerSignup _))
//      }
//    }
//
//    def innerSignup = {
//      <div class="row">
//        <div class="col-lg-6 col-md-8">
//          <h3>Signup</h3>
//          <form method="post" action={S.uri} role="form" class="form-horizontal">
//            <div class="form-group">
//              <label>Email</label>{SHtml.text("", theUser.email(_)) % ("class" -> "form-control")}
//            </div>{passwordGuiHelper.gui1}{passwordGuiHelper.gui2}<div class="form-group">
//            <label>First Name</label>{SHtml.text("", theUser.firstName(_)) % ("class" -> "form-control")}
//          </div>
//            <div class="form-group">
//              <label>Last Name</label>{SHtml.text("", theUser.lastName(_)) % ("class" -> "form-control")}
//            </div>{SHtml.button("Signup", testSignup _) % ("class" -> "btn btn-success")}
//          </form>
//        </div>
//      </div>
//    }
//
//    innerSignup
//  }
//
//  def doAfterSignup(theUser: User, country2: Countries.Value): Nothing = {
//    theUser.email(theUser.email.get.trim) //Do not allow spaces before and after
//    logUserIn(theUser, () => {
//      S.notice("Welcome")
//      S.redirectTo(WebLinks.DASHBOARD)
//    })
//    S.redirectTo(WebLinks.DASHBOARD)
//  }
//
//  def checkTheEmail(email: String): Option[FieldError] = {
//    //      if (AcsUser.isThereUserWithEmailBesidesMe(email, 0)) {
//    //        log.debug("Trying to register with the same email address again: " + email)
//    //        return Some(FieldError(this.email, Lang.g("user.emailexists")))
//    //      }
//    //      if (email.length() < 1) { //Do not allow too short email addresses
//    //        log.debug("Trying to register with too short email address: " + email)
//    //        return Some(FieldError(this.email, Lang.g("user.emailtooshort")))
//    //      }
//    //      if (email.matches(BillingUtil.digitsOnlyRegex) == true) { //Do not allow phone numbers
//    //        log.debug("Trying to register with a phone number: " + email)
//    //        return Some(FieldError(this.email, Lang.g("user.numbers.are.invalid")))
//    //      }
//    return None
//  }
//
//    override def login = {
//      val app = S.param("app")
//
//      def dothelogin(user: User) { //This functino will actually log the user into the system
//        logUserIn(user, () => {
//          S.redirectTo(WebLinks.DASHBOARD)
//        })
//      }
//
//      if (S.post_?) { //Testing the given username and password combination here!
//        val usersIp = S.containerRequest.map(_.remoteAddress) //We will add weblogins to the abuse check
//        val usersName = S.param("username")
//        val usersPassword = S.param("password")
//        val ubun = usersName.flatMap(username => findUserByUserName(username.trim()))
//        ubun match {
//          case Full(user) if user.validated_? && user.testPassword(S.param("password")) => dothelogin(user)
//          //case Full(user) if !user.validated_? => S.error(Lang.gs("account.validation.error"))
//          case _ =>
//            log.debug("Login failed for: " + usersName + " from ip: " + usersIp)
//            S.error("invalid.credentials")
//        }
//      }
//      <form method="post" action={ S.uri }>
//        <table class="table formtable">
//          <tr><td colspan="2"><p class="smsHeader nomargin" style="margin-bottom: 10px;overflow: visible">Log In</p></td></tr>
//          <tr><td>Email Address</td><td><p class="nomargin">{FocusOnLoad(<input type="text" name="username" style="width: 160px" class="biginput"/>)}</p></td></tr>
//          <tr><td>Password</td><td><p class="nomargin"><input type="password" name="password" style="width: 160px" class="biginput"/></p></td></tr>
//          <tr><td><a href={ lostPasswordPath.mkString("/", "/", "") }>Recover Password</a></td><td><p><input type="submit" class="bbutton" style="width: 170px" value="main.log.in"/></p></td></tr>
//        </table>
//      </form>
//    }
//
//    override def lostPasswordXhtml = {
//      (<form method="post" action={ S.uri }>
//         <table id="logintable" class="table formtable">
//           <tr><td colspan="2"><p class="smsHeader" style="margin-bottom: 0px;">Enter Email</p></td></tr>
//           <tr><td>Email Address</td><td><user:email/></td></tr>
//           <tr><td><a href="/">Cancel</a></td><td><user:submit/></td></tr>
//         </table>
//       </form>)
//    }
////    override def lostPassword = {
////      bind("user", lostPasswordXhtml,
////        "email" -> SHtml.text("", sendPasswordReset _) % ("class" -> "biginput"),
////        "submit" -> <input type="submit" value={ Lang.gs("main.send.it") } class="bbutton"/>)
////    }
//
//}
//
///**
//  * An O-R mapped "User" class that includes first name, last name, password and we add a "Personal Essay" to it
//  */
//class User extends MegaProtoUser[User] {
//  def getSingleton = User // what's the "meta" server
//
//}
//
//
