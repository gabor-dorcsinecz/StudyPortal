package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._
import common._
import http._
import js.jquery.JQueryArtifacts
import sitemap._
import Loc._
import mapper._
import code.model._
import code.model.cache.AcsUser
import net.liftmodules.JQueryModule
import org.tresto.language.model.MapLanguage
import org.tresto.traits.LogHelper


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    if (!DB.jndiJdbcConnAvailable_?) {
      sys.props.put("h2.implicitRelativePath", "true")
      val vendor = new StandardDBVendor("org.h2.Driver", "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE", Empty, Empty )
      //val vendor = new StandardDBVendor("org.h2.Driver", "jdbc:h2:portaldata;AUTO_SERVER=TRUE", Full("scala") , Full("123456") )
//      val vendor = new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
//			     Props.get("db.url") openOr
//			     "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
//			     Props.get("db.user"), Props.get("db.password"))
      //val vendor = new StandardDBVendor(GlobalConfig.sqlDriver.head, GlobalConfig.sqlDriverUrl.head, GlobalConfig.sqlUser, GlobalConfig.sqlPassword)


      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(util.DefaultConnectionIdentifier, vendor)
      TrestoDbLogger.setDbDebug()
    }

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    Schemifier.schemify(true, Schemifier.infoF _, MapLanguage, MapUser, MapTopic, MapAssignement, MapSolution)

    val wcp = new BillingLangConfig()
    org.tresto.language.snippet.Lang.init(wcp)
    LiftRules.localeCalculator = wcp.localeCalculator _   //Override lift's locale calculator with my own, when the user is logged in

    // where to search snippet
    LiftRules.addToPackages("code")

//    // Build SiteMap
//    def sitemap = SiteMap(
//      Menu.i("Home") / "index" >> MapUser.AddUserMenusAfter, // the simple way to declare a menu
//      Menu(Loc("Static", Link(List("static"), true, "/static/index"),"Static Content"))
//    )

    def sitemapMutators = MapUser.sitemapMutator

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    //LiftRules.setSiteMapFunc(() => sitemapMutators(sitemap))
    LiftRules.setSiteMap(SiteMap(MenuInfo.menu: _*))


    //Init the jQuery module, see http://liftweb.net/jquery for more information.
    //LiftRules.jsArtifacts = JQueryArtifacts
    JQueryModule.InitParam.JQuery=JQueryModule.JQuery1113
    JQueryModule.init()

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => MapUser.loggedIn_?)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    //Lift CSP settings see http://content-security-policy.com/ and 
    //Lift API for more information.  
    LiftRules.securityRules = () => {
      SecurityRules(content = Some(ContentSecurityPolicy(
        scriptSources = List(ContentSourceRestriction.All),
        styleSources = List(ContentSourceRestriction.All)
            )))
    }
    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)

    InitDatabase.initDefaults() //Insert default fields

  }
}


import org.tresto.language.snippet._
import net.liftweb.http.S
import java.util.Locale


class BillingLangConfig extends LangConfig {
  appname = Some("tbilling")

  /**
    * How should we find the current users locale? We differentiate wether the user is logged in or out
    */
  override def getLocale(): String = {
    val cuser = MapUser.curUserId
    cuser.is.isEmpty match { //determine the locale based on if there is a logged in user or not
      case true => S.locale.toString //The session's locale (from the users browser settings)
      case false => AcsUser.cacheGetById(cuser.get.head).get.locale.get //user is logged in
    }
  }


  def localeCalculator(request : Box[net.liftweb.http.provider.HTTPRequest]): Locale = {
    MapUser.curUserId.is.toOption match {
      case None => request.flatMap(_.locale).openOr(Locale.getDefault())
      case Some(uid) => new Locale(AcsUser.cacheGetById(uid).get.locale.get)
    }

  }
}


object TrestoDbLogger extends LogHelper{

  def setDbDebug() {
    DB.addLogFunc{  //Log all mapper database operations
      case (query, time) => {
        //log.debug("Mapper queries took: " + time + "ms total")
        query.allEntries.foreach({
          case net.liftweb.db.DBLogEntry(stmt, duration) => {
            if (stmt.toString.indexOf("Closed Statement") < 0) {
              log.debug(stmt + " => took " + duration + "ms")
            }
          }
        })
      }
    }
  }

}
