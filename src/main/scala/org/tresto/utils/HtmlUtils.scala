package code.utils

import scala.xml._
import scala.xml.transform._
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.util._
import org.tresto.traits.LogHelper
import java.net._
import java.io._
import java.util

import net.liftweb.common._

object HtmlUtils extends LogHelper {

  /** From a list of nodeseqs creates xml1, xml2, xml3  (just like mkString but for nodeseqs)*/
  def createXMLList(ls: List[NodeSeq]): NodeSeq = {
    var res = NodeSeq.Empty
    for (i <- 0 until ls.size - 1) {
      res = res ++ ls(i) ++ Text(", ")
    }
    res = res ++ ls(ls.size - 1)
    res
  }

  /**
   * Returns an alternating background coloor
   */
  def getBackgroudColor(i: Int): String = {
    if (i % 2 == 0) {
      return "bg0"
    } else {
      return "bg1"
    }
  }

  /**
   * we have an url which may or may not have the last character as /  (like http://tbilling.com does not have one)
   * This function will put the slash there if necessary
   */
  def putLastSlash2Url(url: String): String = {
    if (url == null) {
      return "/"
    }
    (url.charAt(url.length - 1) == '/') match {
      case true => url
      case false => url + "/"
    }
  }

  // If there is only one xhtml part in it we will consider it as html
  def isHtml(inc: String): Boolean = {
    if ((inc.indexOf("<") >= 0) && (inc.indexOf("/>") >= 0 || inc.indexOf("</") >= 0)) {
      return true
    } else {
      return false
    }
  }

  def getEmailBody(inc: String): Mailer.MailBodyType = {
    isHtml(inc) match {
      case true => Mailer.xmlToMailBodyType(scala.xml.XML.loadString(inc))
      case false => Mailer.PlainPlusBodyType(inc, "UTF-8")
    }
  }

  val IDFIELD = "id"
  /**
   * From e.g. an edit link and an object id, get a link
   * @param link the link we want to e.g.:edit our object e.g.: "/ent/items/edit"
   * @param id the id of the object we want to edit, this is the number we want to hide, by encripting it
   * @returns the link like: "/ent/items/edit?id=SASO23JHKKHKJH"
   */
  def getLink(link: String, id: String): String = {
    link + "?" + IDFIELD + "=" + URLEncoder.encode(id)
  }
//  def getLinkEncrypted(link: String, id: Long): String = {
//    link + "?" + IDFIELD + "=" + URLEncoder.encode(SecurityUtils.getParamEncripted(id.toString()))
//  }
//  def getAnyLinkEncrypted(link: String, id: Long, linkText: NodeSeq): NodeSeq = {
//    <a href={ getLinkEncrypted(link, id) }>{ linkText }</a>
//  }
  def getEditLink(link: String, id: Long): NodeSeq = {
    <a href={ getLink(link, id.toString) }>Edit</a>
  }
//  def getEditLinkEncrypted(link: String, id: Long): NodeSeq = {
//    <a href={ getLinkEncrypted(link, id) }>{ Lang.edit }</a>
//  }
  def getDeleteLink(link: String, id: Long): NodeSeq = {
    <a href={ getLink(link, id.toString) }>Delete</a>
  }
//  def getDeleteLinkEncrypted(link: String, id: Long): NodeSeq = {
//    <a href={ getLinkEncrypted(link, id) }>{ Lang.delete }</a>
//  }

  def getAnyLink(link: String, id: String, linkText: NodeSeq): NodeSeq = {
    <a href={ link + "?" + IDFIELD + "=" + URLEncoder.encode(id) }>{ linkText }</a>
  }

  /**
   * The opposite of the previous function, from the encrypted number return the decrypted one
   */
//  def getIdDecrypted(): Long = {
//    SecurityUtils.getParamDecripted(getIdParam.open_!).toLong
//  }
  def getIdLong(): Long = {
    getIdParam.head.toLong
  }
  def getIdParam(): Box[String] = {
    net.liftweb.http.S.param(IDFIELD).map(URLDecoder.decode(_))
  }

  def space(): Unparsed = scala.xml.Unparsed("&nbsp;")
  def space2(): Unparsed = scala.xml.Unparsed("&nbsp;&nbsp;")
  def space3(): Unparsed = scala.xml.Unparsed("&nbsp;&nbsp;&nbsp;")

  /**
   * Read data from the specified url
   */
  def getFromUrl(apiurl: URL): Box[String] = {
    try {
      val connection = apiurl.openConnection.asInstanceOf[HttpURLConnection];
      connection.setRequestMethod("GET");
      //connection.setRequestProperty("Cookie", sessionId);
      connection.connect();
      val rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
      val sb = new StringBuilder();

      var line = rd.readLine()
      while (line != null) {
        sb.append(line + '\n');
        line = rd.readLine()
      }
      connection.disconnect();
      return Full(sb.toString)
    } catch {
      case e: Exception =>
        log.error(e.getMessage)
        //log.error(org.tresto.utils.Statics.stack2string(e))
        return Failure(e.getMessage)
    }
  }

  /**
   * send Http Post message with aparameters in the map
   */
  def postToUrl(apiurl: URL, p_params: scala.collection.mutable.HashMap[String, String]): String = {
    val urlParameters = p_params.map(x => x._1 + "=" + URLEncoder.encode(x._2)).mkString("?", "&", "")
    log.debug("postToUrl=" + apiurl.toString + "" + urlParameters)
    val connection = apiurl.openConnection.asInstanceOf[HttpURLConnection];
    connection.setRequestMethod("POST");
    //connection.setRequestProperty("Cookie", sessionId);
    connection.setDoInput(true);
    connection.setDoOutput(true);
    connection.setReadTimeout(5000) // set timeout for 5 sec

    val wr = new DataOutputStream(connection.getOutputStream());
    wr.writeBytes(urlParameters);
    wr.flush();
    wr.close();

    val rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    val sb = new StringBuilder();
    var line = rd.readLine()
    while (line != null) {
      sb.append(line + '\n');
      line = rd.readLine()
    }
    connection.disconnect();
    log.debug("answer=" + sb.toString.replaceAll("\n", ""))
    return sb.toString
    //return postToFile(p_url, p_params)
  }

  /**
   * This will reload the website at reloadTime interwalls, should be used if ajax/comet is not avaiable
   */
  def getPageReloaderJavascript(reloadTime: Int): Unparsed = {
    Unparsed(""" <head>
	<script type="text/javascript"> 
		$(window).focus(function(){
			window.setTimeout('location.reload()', """ + reloadTime + """); //reloads after x seconds
		});
    </script>
 </head>
""")
  }

  /**
   * Date picker for bootstrap (use with bootstrap-datepicker.min.css and bootstrap-datepicker.min.js)
   * https://github.com/eternicode/bootstrap-datepicker
   * http://bootstrap-datepicker.readthedocs.org/en/latest/
   */
  def getFromToDatePickers(fromName: String, toName: String): Unparsed = {
    Unparsed("""
        <script type="text/javascript">
        $(function() {
            $('#""" + fromName + """').datepicker({format:'yyyy-mm-dd 00:00:00'});
            $('#""" + toName + """').datepicker({format:'yyyy-mm-dd 23:59:59'});
        });
    </script>
         """)
  }

  /**
   * Date picker for bootstrap (use with bootstrap-datepicker.min.css and bootstrap-datepicker.min.js)
   * This works with bootstrap css and bootstrap-datepicker.min.js NOT working with jquery-ui!
   */
  def getAtDatePicker(atName: String): Unparsed = {
    val df = new java.text.SimpleDateFormat("HH:mm:ss")
    val nowhms = df.format(new java.util.Date())
    Unparsed("""
        <script type="text/javascript">
        $(function() {
            $('#""" + atName + """').datepicker({format:'yyyy-mm-dd """ + nowhms + """'});
        });
    </script>
         """)
  }

  /**
   *  jquery datepicker (not bootstrap, will not work with the bootstrap datepicker)
   *  Datepicker for jquery-ui use with jquery-ui.css and jquery-ui.min.js 
   */
  def getAtDatePickerJqery(atName: String): Unparsed = {
    val df = new java.text.SimpleDateFormat("HH:mm:ss")
    val nowhms = df.format(new java.util.Date())
    Unparsed("""
        <script type="text/javascript">
        $(function() {
            $('#""" + atName + """').datepicker({dateFormat:'yy-mm-dd """ + nowhms + """'});
        });
    </script>
         """)
  }

  /**
   *  jquery datepicker (not bootstrap, will not work with the bootstrap datepicker)
   *  Datepicker for jquery-ui use with jquery-ui.css and jquery-ui.min.js 
   */
  def getAtDatePickerJqeryDay(atName: String): Unparsed = {
    Unparsed("""
        <script type="text/javascript">
        $(function() {
            $('#""" + atName + """').datepicker({dateFormat:'yy-mm-dd'});
        });
    </script>
         """)
  }
  
  /**
   *  jquery datepicker (not bootstrap, will not work with the bootstrap datepicker)
   *  Datepicker for jquery-ui use with jquery-ui.css and jquery-ui.min.js 
   */
  def getFromToDatePickersJquery(fromName: String, toName: String): Unparsed = {
    Unparsed("""
        <script type="text/javascript">
        $(function() {
            $('#""" + fromName + """').datepicker({dateFormat:'yy-mm-dd 00:00:00'});
        });
    </script>
    <script type="text/javascript">
        $(function() {
            $('#""" + toName + """').datepicker({dateFormat:'yy-mm-dd 23:59:59'});
        });
    </script>  
         """)
  }

  /*
  // Datepicker for jquery-ui use with jquery-ui.css and jquery-ui.min.js
  def getAtDatePicker(atName: String): Unparsed = {
    val df = new java.text.SimpleDateFormat("HH:mm:ss")
    val nowhms = df.format(new java.util.Date())
    Unparsed("""
        <script type="text/javascript">
        $(function() {
            $('#""" + atName + """').datepicker({dateFormat:'yy-mm-dd """ + nowhms + """'});
        });
    </script>
         """)
  }
  
    /**
   * All javascript is loaded at the end of the page, so we cannot use selectors before those are loaded (jQuery is one of them)
   * So we create a domReadyQueue and add up the functions we want to call after jQuery is loaded
   * http://stackoverflow.com/questions/1220956/move-jquery-to-the-end-of-body-tag
   */
  def getFromToDatePickersBs(fromName: String, toName: String): Unparsed = {
    Unparsed("""
        <script type="text/javascript">
        domReadyQueue.push(function() {
            $('#""" + fromName + """').datepicker({dateFormat:'yy-mm-dd 00:00:00'});
            $('#""" + toName + """').datepicker({dateFormat:'yy-mm-dd 23:59:59'});
        });
    </script>
         """)
  }
  
  
  */

  /**
   * Take a table and insert color into every second row. Used before html5
   */
  def stripeTableRows(xml: NodeSeq): NodeSeq = {
    val rr = new TableStriper()
    val newDoc = new RuleTransformer(rr).transform(xml)
    newDoc
  }

  //  def getBackLink(defaultLink:String):NodeSeq = {
  //    <a href={ S.referer.openOr(defaultLink) }>{ Lang.cancel }</a>
  //  }

}

/**
 * Take a html table's rows and color every second row. Used before html5 did it automatically
 */
class TableStriper extends RewriteRule with LogHelper {
  var i = 0

  override def transform(n: Node): NodeSeq = n match {
    case Elem(prefix, "tr", att, scope, ch @ _*) => //For some reason every row runs 4 times here ...
      i = i + 1
      val res = Elem(prefix, "tr", att, scope, ch: _*) % Attribute(None, "class", Text(HtmlUtils.getBackgroudColor(i / 4)), Null)
      //log.debug("transform: " + i + " res: " + res)
      res
    //case Elem(prefix, label, attribs, scope, child @ _*) =>
    //case e : Elem(_, "tr", attributes, _, _*) => e % Attribute(None, "name", Text("value"), Null)
    //case e @ Elem(_, "tr", attributes, _, _*) => attributes.append(updates, scope) <tr class={ i = i + 1; HtmlUtils.getBackgroudColor(i); }>
    case n => n
  }
}
