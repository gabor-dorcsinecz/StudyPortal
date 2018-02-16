package bootstrap.liftweb

import java.io.File

import net.liftweb.util.Props

object GlobalConfig {

  val WEBAPP_NAME = "study"
  val DIR_WEBAPP = "webapps" + File.separator + WEBAPP_NAME + File.separator;
  val DIR_WEBINF = "WEB-INF" + File.separator

  /**
    * Cut the . after a path and add / or \ after it
    */
  def dirize(p: String): String = {
    var oloc = p
    if (oloc.endsWith(".")) { //Cut the trailing .
      oloc = oloc.substring(0, oloc.length() - 1)
    }
    if (oloc.endsWith(File.separator) == false) { //Add the separator at the end if it's not there
      oloc = oloc + File.separator
    }
    oloc
  }

  /**
    * Try to calculate where are we running from
    */
  def getBasePath(): String = {
    val ofile = new File(".") //will be like: C:\programs\Red5_0.8.0\.
    var oloc = dirize(ofile.getAbsolutePath)
    if (oloc.length < 3) {
      println("jetty.home is not defined, going for . file!")
      oloc = dirize((new File(".")).getAbsolutePath) //This will work only if you run jetty as an application, but fails when you run as service on linux
    }
    oloc
  }

  println("jetty.home: " + System.getProperty("jetty.home"))
  println("jetty.base: " + System.getProperty("jetty.base"))

  lazy val oloc = getBasePath()
  lazy val webServerPath = oloc //will be like: C:\programs\Red5_0.8.0\
  lazy val webServerResourcesPath = webServerPath + "resources" + File.separator //C:\programs\jetty\resources\
  lazy val webappPath = oloc + DIR_WEBAPP; // C:\programs\jetty\webapps\tbilling
  lazy val webinfPath = oloc + DIR_WEBAPP + DIR_WEBINF // callcenter/WEB-INF/
  lazy val webClassPath = webinfPath + "classes" + File.separator // callcenter/WEB-INF/classes/
  lazy val webConfigPath = webClassPath + File.separator + "props" // callcenter/WEB-INF/classes/props
  lazy val webLibPath = webinfPath + "lib" + File.separator // callcenter/WEB-INF/lib/
  lazy val webResourcesPath = webinfPath + "resources" + File.separator // callcenter/WEB-INF/resources/
  lazy val voiceRecordDirectory = webappPath + "voicerecordings"  //Where to store recorded voices

  val sqlDriver = Props.get("datasource.driver")
  val sqlDriverUrl = Props.get("datasource.driverUrl")
  val sqlUser = Props.get("datasource.user")
  val sqlPassword = Props.get("datasource.password")


}
