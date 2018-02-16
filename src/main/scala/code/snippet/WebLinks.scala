package code.snippet

import org.tresto.utils.TLink

object WebLinks {
  val HOME = "/"
  val PUB = "/pub/" //Public web
  val DASHBOARD_NAME = "dashboard"
  val DASHBOARD = "/" + DASHBOARD_NAME

  object TOPIC extends TLink(List("ent","topic"))
  object ASSIGNEMENT extends TLink(List("ent","assignements"))
  object SOLUTION extends TLink(List("ent","solution"))

  object USER extends TLink(List("ent", "users"))


    //def asLink(ls: List[String]): String = ls.mkString("/", "/", "/") //Points to a directory
  //def asLinkDirect(ls: List[String]): String = ls.mkString("/", "/", "") //Points to a file

}
