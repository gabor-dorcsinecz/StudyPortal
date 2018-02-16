package bootstrap.liftweb

import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._

import scala.xml._
import code.model.MapUser
import code.model.cache.AcsUser
import code.snippet._
import org.tresto.language.snippet.TemplateLanguage

object MenuInfo {

  val MUST_LOG_IN = "You Must Log In"
  val IfLoggedIn = If(() => MapUser.curUserId.isDefined, MUST_LOG_IN) //For productions environment
  val IfLoggedOut = If(() => !MapUser.curUserId.isDefined, "") //For productions environment
  val IfLoggedInStudent = If(() => (MapUser.curUserId.isDefined && MapUser.curUserType.get.head == MapUser.USERTYPE_STUDENT), MUST_LOG_IN)
  val IfLoggedInStaff = If(() => (MapUser.curUserId.isDefined && MapUser.curUserType.get.head == MapUser.USERTYPE_STAFF), MUST_LOG_IN)
  val IfLoggedInSiteVisor = If(() => (MapUser.curUserId.isDefined && MapUser.curUserType.get.head == MapUser.USERTYPE_SITEVISOR), MUST_LOG_IN)
  val IfLoggedInAdmin = If(() => (MapUser.curUserId.isDefined && ( MapUser.curUserType.get.head == MapUser.USERTYPE_SITEVISOR || MapUser.curUserType.get.head == MapUser.USERTYPE_STAFF)), MUST_LOG_IN)


  val LEFT = LocGroup("Left") //Menus in this group will be laid out on the left hand side
  val RIGHT = LocGroup("Right") //Menus in this group will be laid out on the right hand side

  val mIndex = Menu(Loc("Index", new Link(List("index"), false), <span><span class="glyphicon glyphicon-home"></span>Home</span>, Hidden, LEFT))
  val mLogin = Menu(Loc("mLogin", new Link(List("login"), false), Text("Login") , getTemplateBootstrap(LoginSnippet.getStartScreen), IfLoggedOut, Hidden, RIGHT))
  val mLoggedInDashboard = Menu(Loc("mLoggedInDashboard", new Link(List(WebLinks.DASHBOARD_NAME), false), Text("Dashboard"), getTemplateBootstrap(LoginSnippet.render), IfLoggedIn, LEFT))
  val mCreateUser = Menu(Loc("mCreateUser", new Link(MapUser.signUpPath, true), Text("Signup"), getTemplateBootstrap(MapUser.signup), IfLoggedOut, Hidden, RIGHT))

  val mTopicList = Menu(Loc("mTopicList", WebLinks.TOPIC.getIndexLink, Text("Topics List"), getTemplateBootstrap(SnTopic.list), IfLoggedIn))
  val mTopicEdit = Menu(Loc("mTopicEdit", WebLinks.TOPIC.getEditLink, Text("Topic Edit"), getTemplateBootstrap(SnTopic.edit), IfLoggedIn, Hidden))
  val mTopicAdd = Menu(Loc("mTopicAdd", WebLinks.TOPIC.getAddLink, Text("Topic New"), getTemplateBootstrap(SnTopic.add), IfLoggedIn, Hidden))
  val mTopicDelete = Menu(Loc("mTopicDelete", WebLinks.TOPIC.getDeleteLink, Text("Topic Delete"), getTemplateBootstrap(SnTopic.delete), IfLoggedIn, Hidden))

  val mAssignementList = Menu(Loc("mAssignementList", WebLinks.ASSIGNEMENT.getIndexLink, Text("Assignements List"), getTemplateBootstrap(SnAssignement.list), IfLoggedIn))
  val mAssignementEdit = Menu(Loc("mAssignementEdit", WebLinks.ASSIGNEMENT.getEditLink, Text("Assignement Edit"), getTemplateBootstrap(SnAssignement.edit), IfLoggedIn, Hidden))
  val mAssignementAdd = Menu(Loc("mAssignementAdd", WebLinks.ASSIGNEMENT.getAddLink, Text("Assignement New"), getTemplateBootstrap(SnAssignement.add), IfLoggedIn, Hidden))
  val mAssignementDelete = Menu(Loc("mAssignementDelete", WebLinks.ASSIGNEMENT.getDeleteLink, Text("Assignement Delete"), getTemplateBootstrap(SnAssignement.delete), IfLoggedIn, Hidden))

  val mSolutionList = Menu(Loc("mSolutionList", WebLinks.SOLUTION.getIndexLink, Text("Solutions List"), getTemplateBootstrap(SnSolution.list), IfLoggedIn))
  val mSolutionEdit = Menu(Loc("mSolutionEdit", WebLinks.SOLUTION.getEditLink, Text("Solution Edit"), getTemplateBootstrap(SnSolution.edit), IfLoggedIn, Hidden))
  val mSolutionAdd = Menu(Loc("mSolutionAdd", WebLinks.SOLUTION.getAddLink, Text("Solution New"), getTemplateBootstrap(SnSolution.add), IfLoggedIn, Hidden))
  val mSolutionDelete = Menu(Loc("mSolutionDelete", WebLinks.SOLUTION.getDeleteLink, Text("Solution Delete"), getTemplateBootstrap(SnSolution.delete), IfLoggedIn, Hidden))

  val mUserStaffList = Menu(Loc("mUserStaffList", WebLinks.USER.getIndexLink, "Users", getTemplateBootstrap(SnUser.list), IfLoggedIn))
  val mUserStaffEdit = Menu(Loc("mUserStaffEdit", WebLinks.USER.getEditLink, "Users", getTemplateBootstrap(SnUser.edit), IfLoggedIn, Hidden))
  val mUserStaffAdd = Menu(Loc("mUserStaffAdd", WebLinks.USER.getAddLink, "Users", getTemplateBootstrap(SnUser.add), IfLoggedIn, Hidden))
  val mUserStaffDelete = Menu(Loc("mUserStaffDelete", WebLinks.USER.getDeleteLink, "Users", getTemplateBootstrap(SnUser.delete), IfLoggedIn, Hidden))

  val menuLanguageList = TemplateLanguage.menuLanguageList(IfLoggedInAdmin)
  val menuLanguageDelete = TemplateLanguage.menuLanguageDelete(Hidden)
  val menuLanguageAdd = TemplateLanguage.menuLanguageAdd(Hidden)
  val menuLanguageEdit = TemplateLanguage.menuLanguageEdit(Hidden)

  def menu(): List[Menu] = {
    List[Menu](
      mIndex,mLoggedInDashboard,
      menuLanguageDelete, menuLanguageAdd, menuLanguageEdit,
      Menu(Loc("mServiceProvider", getMenuLink("/menu/student/"), Text("Student"), getMainMenu("Student"),IfLoggedIn, PlaceHolder, LEFT),
        mAssignementList,mAssignementEdit,mAssignementAdd,mAssignementDelete,
        mSolutionList,mSolutionAdd,mSolutionEdit,mSolutionDelete
      ),
      Menu(Loc("MenuSaSystem", getMenuLink("/menu/sa/system/"), Text("System"), getMainMenu("System"), IfLoggedInAdmin, PlaceHolder, LEFT),
        mTopicList,mTopicAdd,mTopicEdit,mTopicDelete,
        mUserStaffList,mUserStaffEdit,mUserStaffAdd,mUserStaffDelete,
        menuLanguageList
      ),

      mLogin,
      mCreateUser,
      MapUser.lostPasswordMenuLoc.head,
      MapUser.validateUserMenuLoc.head,
      MapUser.resetPasswordMenuLoc.head,

      Menu(Loc("mUserInside", new Link(List("manageUser"), false), <span><span class="glyphicon glyphicon-user"></span> { AcsUser.getCurrentUserOption.map(_.email.get).getOrElse("unknown") }</span>, getTemplateBootstrap(LoginSnippet.render), IfLoggedIn, PlaceHolder, RIGHT),
        MapUser.editUserMenuLoc.head,
        MapUser.changePasswordMenuLoc.head,
        MapUser.logoutMenuLoc.head),
    )
  }

  def getTemplateBootstrap(f: () => scala.xml.NodeSeq) = Template({ () =>
  {
    <lift:surround with="default" at="content">
      { f() }
    </lift:surround>
  }
  })

  /**
    * A logged in menu which is visible among on the top menu bar, the entire content is a language key
    */
  def getMainMenu(languageKey: String) = Template({ () =>
  {
    <lift:surround with="default" at="content">{languageKey}</lift:surround>
  }
  })
  /**
    * Creates a menu link
    * @param link input string can be: a/b/c/  or a/b/c/something.html
    * @param allowdir should we allow the whole directory to access or just the file given ?
    */
  def getMenuLink(link: String, allowdir: Boolean = true): Link[Unit] = {
    val mg = (link.lastIndexOf("/") + 1 == link.length) match { //Maybe its not a directory but a contcrete file like /ent/item/index.html
      case true => link
      case false => link.substring(0, link.lastIndexOf("/"))
    }
    //println("getMenuLink: " + i + " # " +  mg )
    Link(getMenuAsList(mg), allowdir, link)
  }

  /**
    * Breaks up a link like this: /ent/invoice/ and returns List("ent","invoice")
    */
  def getMenuAsList(i: String): List[String] = {
    var tmp = i.indexOf("/") match { //remove first slash
      case 0 => i.substring(1)
      case _ => i
    }
    val res = tmp.split("/").toList
    //println("getMenuList: " + res)
    return res
  }
}
