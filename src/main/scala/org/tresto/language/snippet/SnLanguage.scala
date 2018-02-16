package org.tresto.language.snippet

import _root_.net.liftweb._
import http._
import mapper._
import S._
import SHtml._
import common._
import util._
import js.JsCmds._
import js.jquery._
import js._
import JE._
import Helpers._
import _root_.scala.xml.{ NodeSeq, Text, Group }
import _root_.java.util.Locale
import org.tresto.traits.LogHelper;
import java.util.Date
import org.tresto.language.model._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._

/**
 * Use these templates in menus so you do not have to include the html templates, just this jar file will do it.
 * Use it like this
 *
 * val menuLanguageList = org.tresto.language.snippet.TemplateLanguage.menuLanguageList(IfLoggedInXytonVisor)
 * val menuLanguageDelete = org.tresto.language.snippet.TemplateLanguage.menuLanguageDelete(Hidden)
 * val menuLanguageAdd = org.tresto.language.snippet.TemplateLanguage.menuLanguageAdd(Hidden)
 * val menuLanguageList = org.tresto.language.snippet.TemplateLanguage.menuLanguageList(Hidden)
 *
 * or:
 *
 * val menuLanguageList = Menu(Loc("menuLanguageList",  "" :: Nil,  Lang.gs("menu.sa.system.language.title"), org.tresto.language.snippet.TemplateLanguage.list))
 * val menuLanguageDelete = Menu(Loc("menuLanguageDelete",  "" :: Nil,  Lang.gs("menu.sa.system.language.delete.title"), org.tresto.language.snippet.TemplateLanguage.delete, Hidden))
 * val menuLanguageAdd = Menu(Loc("menuLanguageAdd",  "" :: Nil,  Lang.gs("menu.sa.system.language.add.title"), org.tresto.language.snippet.TemplateLanguage.add, Hidden))
 * val menuLanguageEdit = Menu(Loc("menuLanguageEdit",  "" :: Nil,  Lang.gs("menu.sa.system.language.edit.title"), org.tresto.language.snippet.TemplateLanguage.edit, Hidden))
 *
 *
 */
object TemplateLanguage extends LogHelper {

  def menuLanguageList(lp: LocParam[Any]*) = {
    Menu(Loc("menuLanguageList", new Link(Lang.editorLinkList :+ SnLanguage.aINDEX, false), Lang.gs("menu.sa.system.language.title"), list :: lp.toList))
  }
  def menuLanguageDelete(lp: LocParam[Any]*) = {
    Menu(Loc("menuLanguageDelete", new Link(Lang.editorLinkList :+ SnLanguage.aDELETE, false), Lang.gs("menu.sa.system.language.delete.title"), delete :: lp.toList))
  }
  def menuLanguageAdd(lp: LocParam[Any]*) = {
    Menu(Loc("menuLanguageAdd", new Link(Lang.editorLinkList :+ SnLanguage.aADD, false), Lang.gs("menu.sa.system.language.add.title"), add :: lp.toList))
  }
  def menuLanguageEdit(lp: LocParam[Any]*) = {
    //Menu(Loc("menuLanguageEdit", getMenuLink(Lang.editorLink + SnLanguage.aEDIT), Lang.gs("menu.sa.system.language.edit.title"), edit:: lp.toList))
    Menu(Loc("menuLanguageEdit", new Link(Lang.editorLinkList :+ SnLanguage.aEDIT, false), Lang.gs("menu.sa.system.language.edit.title"), edit :: lp.toList))
  }

  //===============================================================================================================================================================================

  val list = Template({ () =>
    {
      log.trace("Template list is running")
      <lift:surround with="default" at="content">
        <lift:SnLanguage.list/>
      </lift:surround>
    }
  })

  val delete = Template({ () =>
    <lift:surround with="default" at="content">
      <lift:SnLanguage.confirmDelete form="post">
        <xmp:lang_reallydelete/>
        &nbsp;<xmp:dispname/>
        ? &nbsp;&nbsp;
        <xmp:cancel/>
        &nbsp;&nbsp;<xmp:delete/>
      </lift:SnLanguage.confirmDelete>
    </lift:surround>
  })

  val add = Template({ () =>
    <lift:surround with="default" at="content">
      <lift:SnLanguage.add form="post"/>
    </lift:surround>
  })

  val edit = Template({ () =>
    <lift:surround with="default" at="content">
      <lift:SnLanguage.edit form="post"/>
    </lift:surround>
  })

  //  /**
  //   * Creates a menuling
  //   * @param link input string can be: a/b/c/  or a/b/c/something.html
  //   * @param allowdir should we allow the whole directory to access or just the file given ?
  //   */
  //  def getMenuLink(link: String, allowdir: Boolean = true): Link[Unit] = {
  //    val mg = (link.lastIndexOf("/") + 1 == link.length) match { //Maybe its not a directory but a contcrete file like /ent/item/index.html
  //      case true => link
  //      case false => link.substring(0, link.lastIndexOf("/"))
  //    }
  //    println("getMenuLink: " + link + " # " + mg)
  //    Link(getMenuAsList(mg), allowdir, link)
  //  }
  //
  //  def getMenuAsList(i: String): List[String] = {
  //    var tmp = i.indexOf("/") match { //remove first slash
  //      case 0 => i.substring(1)
  //      case _ => i
  //    }
  //    val res = tmp.split("/").toList
  //    println("getMenuList: " + i + " -> " + res)
  //    return res
  //  }

}

class LangFilter() {
  var key: Box[String] = Empty
  var all: Box[String] = Empty
}

object SnLanguage extends LanguagePaginator with LogHelper {

  val UPDATE = "U"
  val INSERT = "I"
  val aINDEX = "index"
  val aEDIT = "edit"
  val aADD = "add"
  val aDELETE = "delete"
  val sanePeoplesFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  private object selectedRowData extends RequestVar[Box[MapLanguage]](Empty)
  private object selectedFilterData extends RequestVar[Box[LangFilter]](Full(new LangFilter()))
  /**
   * List all data in a TABLE
   */
  def list(): NodeSeq = {
    //val cid = MapUser.curSuperCampanyId.is.open_!
    val fl = selectedFilterData.is.head
    var langs = Lang.cacheGetAll().sortWith((a, b) => a.langkey.get < b.langkey.get);
    fl.all.map(x => langs = langs.map(f => (f.search(x), f)).filter(a => a._1 > 0).sortWith((a, b) => a._1 > b._1).map(a => a._2)) //Order by found count
    fl.key.map(a => langs = langs.filter(f => f.langkey.get.contains(a)))  //Return partial matches too
    val totalLength = langs.length
    langs = langs.slice(objectStartOnPage(), objectsEndOnPage())
    //langs = langs.sortWith((a,b) => a.langkey.get < b.langkey.get)

    var i = 0
    val res = langs.flatMap { u =>
      <tr class={ i = i + 1; getBackgroudColor(i); }>
        <!--<td>{u.id}</td>--> 
        <td>{ u.langkey.get }</td>
        <td>{ Lang.removeQuote(u.en_us.get) }</td>
        <td>{ Lang.removeQuote(u.hu_hu.get) }</td>
        <td>{ sanePeoplesFormat.format(u.lastmodified.get) }</td>
        <td>{ link(Lang.editorLink + aEDIT + "?" + Lang.pLANGID + "=" + u.langkey.get, () => selectedRowData(Full(u)), Lang.edit) }</td>
        <td>{ link(Lang.editorLink + aDELETE + "?" + Lang.pLANGID + "=" + u.langkey.get, () => selectedRowData(Full(u)), Lang.delete) }</td>
      </tr>
    }
    //<!--<th>{Lang.g("language.id")}</th>-->
    <div>
      { renderFilter }
      <span class="column span-6 actionbarstyle"><a href="add">{ Lang.g("language.add") }</a></span>
      <span class="column span-18 last paginator"> { getPaginator(totalLength, () => selectedFilterData(Full(fl))) }</span>
      <table>
        <colgroup>
          <col span="1" style="width: 15%;"/>
          <col span="1" style="width: 25%;"/>
          <col span="1" style="width: 25%;"/>
          <col span="1" style="width: 15%;"/>
          <col span="1" style="width: 10%;"/>
          <col span="1" style="width: 10%;"/>
        </colgroup>
        <tr><th>{ Lang.g("language.key") }</th><th>{ Lang.g("language.english") }</th><th>{ Lang.g("language.hungarian") }</th><th>{ Lang.g("language.lastmodified") }</th><th>{ Lang.edits }</th><th>{ Lang.deletes }</th></tr>
        { res }
      </table>
    </div>

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

  //  def shortenString(pin:String):String = {
  //    (pin.length > 25 ) match {
  //      case true => pin.substring(0, 25) + "..."
  //      case false => pin
  //    }
  //  }

  /**
   */
  def confirmDelete(xhtml: NodeSeq): NodeSeq = {
    val data = selectedRowData.get.head
    val dispname = data.langkey
    log.trace("Language ConfirmDelete: " + dispname)

    def deleteMe() {
      notice(Lang.gs("language.deleted") + ": " + dispname)
      Lang.cacheAndDbDelete(data)
      //val lgc = new LoGeneralCRUD(LoGeneralCRUD.DELETE,data.id,data)
      //FacadeTBilling.getInstance().bang(TrestoConstants.LANGUAGE_CACHEDB_LOGENERALCRUDE,lgc)
      redirectTo(Lang.editorLink + aINDEX)
    }

//    bind("xmp", xhtml,
//      "dispname" -> dispname,
//      "lang_reallydelete" -> Lang.g("language.reallydelete"),
//      "cancel" -> SHtml.link(Lang.editorLink + aINDEX, () => (), Lang.no),
//      "delete" -> submit(Lang.gs("language.delete"), deleteMe))
    <h1>Coming soon</h1>
  }

  /**
   */
  def edit(): NodeSeq = {
    if (S.param(Lang.pLANGID).isDefined) {
      val lid = S.param(Lang.pLANGID).head //We are being called from elwhere, and somebody wants to edit, which was not selected from the list here
      change(Lang.cacheGetByKey(lid).get, UPDATE)
    } else if (selectedRowData.is.isEmpty == false) {
      change(selectedRowData.is.head, UPDATE)
    } else {
      notice(Lang.gs("language.error.tryagain"))
      log.error("Language key edit, neither linked, nor local")
      S.redirectTo(S.referer.openOr(Lang.editorLink + aINDEX))
    }
  }

  /**
   * Add a row in a FORM
   */
  def add(): NodeSeq = {
    var litem = new MapLanguage();
    litem.enabled(true).lastmodified(new Date()).lastused(new Date())
    litem.hu_hu("").en_us("").ro_ro("").pl_pl("")
    change(litem, INSERT)
  }

  /**
   * toLink is the link we will forward after editing finished
   */
  def change(litem: MapLanguage, operation: String): NodeSeq = {
    val backLink = S.referer.openOr(Lang.editorLink + aINDEX)
    var langkey = litem.langkey.get
    var textType = litem.textType.get
    var en_us = Lang.removeQuote(litem.en_us.get);
    var hu_hu = Lang.removeQuote(litem.hu_hu.get);
    var ro_ro = Lang.removeQuote(litem.ro_ro.get);
    var lastmodified = sanePeoplesFormat.format(litem.lastmodified.get)
    var enabled = litem.enabled.get.toString;

    def submit() = { //Save the data
      litem.appId(litem.appId.get)
      litem.langkey(langkey)
      litem.enabled(true)
      litem.lastmodified(new java.util.Date())
      litem.modifiedby(0)
      litem.en_us(Lang.addQuote(en_us))
      litem.hu_hu(Lang.addQuote(hu_hu))
      litem.ro_ro(Lang.addQuote(ro_ro))
      if (Lang.isHtml(en_us) && Lang.isHtml(hu_hu) && Lang.isHtml(ro_ro)) { //Even if he did not set xml, if all of the languages are xml, we set it automatically
        textType = MapLanguage.TYPE_XML
      }
      litem.textType(textType)

      operation match {
        case INSERT => Lang.cacheAndDbInsert(litem)
        case UPDATE => Lang.cacheAndDbUpdate(litem)
      }

      //val lgc = new LoGeneralCRUD(operation,finalItem.id, finalItem)
      //crudLanguageLo(lgc)
      S.redirectTo(backLink)
    }

    val textTypeSelector = List((MapLanguage.TYPE_TEXT, Lang.gs("language.type.string")), (MapLanguage.TYPE_XML, Lang.gs("language.type.xml")), (MapLanguage.TYPE_TEXTILE, Lang.gs("language.type.textile")), (MapLanguage.TYPE_MARKDOWN, Lang.gs("language.type.markdown")))

    <div>
      <p>
        <a href="http://en.wikipedia.org/wiki/Markdown" target="_blank">{ Lang.g("language.markdown.link") }</a><br/>
        <a href="http://redcloth.org/hobix.com/textile/quick.html" target="_blank">{ Lang.g("language.textile.link") }</a>
      </p>
      <table>
        <tr>
          <td>{ Lang.g("language.key") }</td>
          <td>{ SHtml.text(langkey, langkey = _) % ("style" -> "width: 800px;") }</td>
        </tr>
        <tr>
          <td>{ Lang.g("language.lastmodified") }</td>
          <td>{ sanePeoplesFormat.format(litem.lastmodified.get) }</td>
        </tr>
        <tr>
          <td>{ Lang.g("language.texttype") }</td>
          <td>{ SHtml.select(textTypeSelector, Some(textType), textType = _) }</td>
        </tr>
        <tr>
          <td>{ Lang.g("language.english") }</td>
          <td>{ SHtml.textarea(en_us, en_us = _) % ("class" -> "column span-20 last") % ("style" -> "width: 800px; height: 120px;") }</td>
        </tr>
        <tr>
          <td>{ Lang.g("language.hungarian") }</td>
          <td>{ SHtml.textarea(hu_hu, hu_hu = _) % ("class" -> "column span-20 last") % ("style" -> "width: 800px; height: 120px;") }</td>
        </tr>
        <tr>
          <td>{ Lang.g("language.romanian") }</td>
          <td>{ SHtml.textarea(ro_ro, ro_ro = _) % ("class" -> "column span-20 last") % ("style" -> "width: 800px; height: 120px;") }</td>
        </tr>
        <tr>
          <td>{ SHtml.link(backLink, () => (), Lang.cancel) }</td>
          <td>{ SHtml.submit(Lang.saves, submit) }</td>
        </tr>
      </table>
    </div>

  }

  def renderFilter(): NodeSeq = {
    val fl = selectedFilterData.get.head //We know this exists as we created it at the beginning of this trait

    def doFilter() {
      val backLink = Lang.editorLink + SnLanguage.aINDEX
      S.redirectTo(backLink, () => selectedFilterData(Full(fl))) //redirect to this very same snippet so the view will update
    }

    <form action={ S.uri } method="post">
      <table>
        <tr class="bottomaligned">
          <td>{ Lang.g("lang.filter.key") }</td>
          <td>{ Lang.g("lang.filter.allfilter") }</td>
          <td></td>
        </tr>
        <tr class="topaligned">
          <td>{ SHtml.text(fl.key.getOrElse(""), a => fl.key = if (a.length == 0) Empty else Full(a)) }</td>
          <td>{ SHtml.text(fl.all.getOrElse(""), a => fl.all = if (a.length == 0) Empty else Full(a)) }</td>
          <td>{ FocusOnLoad(SHtml.submit(Lang.gs("item.filter"), doFilter)) }</td>
        </tr>
      </table>
    </form>
  }

}

/**
 * This paginator can be used with a filter, so when you click on a link the filter information is forwarded
 * How to use it:
 * protected object selectedFilterData extends RequestVar[Box[CDRFilter]](Full(new CDRFilter()))
 *
 *    val filter = selectedFilterData.is.open_!
 *    val (data, dataLength) = run queries here
 *  when you put the paginator in the page use it like this:
 *    getPaginator(dataLength, () => selectedFilterData(Full(filter)))
 *
 */
trait LanguagePaginator {
  val offsetParam = "offset"

  def zoomedPages(curPage: Int, pageCount: Int) = (
    List(curPage - 1000, curPage - 100, curPage - 10) ++
    (curPage - 9 to curPage + 9) ++
    List(curPage + 10, curPage + 100, curPage + 1000)).filter(n => n >= 0 && n <= pageCount)

  /** How many items to put on each page */
  def itemsPerPage = 200

  /** Returns a URL used to link to a page starting at the given record offset.  */
  def pageUrl(offset: Long): String = appendParams(S.uri, List(offsetParam -> offset.toString))

  /** Overrides the super's implementation so the first record can be overridden by a URL query parameter.   */
  def currentPage(): Int = S.param(offsetParam).map(toInt) openOr 0

  def objectStartOnPage(): Int = currentPage * itemsPerPage //Can be used for queries, like select from .... OFFSET (thisValue)
  def objectsEndOnPage(): Int = (currentPage + 1) * itemsPerPage

  /** Calculates the current page number, based on the value of 'first.'   */
  //def getCurPage() = (first / itemsPerPage).toInt

  def getPaginator(totalCount: Long, filter: () => Unit): NodeSeq = {
    val curPage = currentPage() //What page are we at?
    val pageCount = (totalCount / itemsPerPage).toInt - 1 + (if (totalCount % itemsPerPage > 0) 1 else 0) //We must be able to jump to the last page which may only contain a few items
    //println("totalCount: " + totalCount + " curPage: " + curPage + " pageCount: " + pageCount)
    val zoom = zoomedPages(curPage, pageCount).map(a => pageXml(curPage, totalCount, a, Text(a.toString), filter)).map(a => a ++ Text("|"))
    val jump2First = pageXml(curPage, pageCount, 0, Text("<<"), filter) ++ Text("|")
    val jump2Last = pageXml(curPage, pageCount, pageCount, Text(">>"), filter)
    val jump2Previous = pageXml(curPage, pageCount, curPage - 1, Text("<"), filter) ++ Text("|")
    val jump2Next = pageXml(curPage, pageCount, curPage + 1, Text(">"), filter) ++ Text("|")

    <span>{ jump2First }{ jump2Previous }{ zoom }{ jump2Next }{ jump2Last } /{ pageCount } ({ totalCount })</span>
    //println("curPage: " + curPage + " count: " + pageCount)
    //    if (curPage == 0) { //First page
    //      return <span>{ zoom }|{ pageXml(first + itemsPerPage min itemsPerPage * (pageCount - 1) max 0, totalCount, Text(">"), filter) }|{  } ({ totalCount })</span>
    //    } else if (curPage == pageCount - 1) { //Last page
    //      return <span>{  }|{ pageXml(first - itemsPerPage max 0, totalCount, Text("<"), filter) }|{ zoom }|{ pageXml(first, totalCount, Text(curPage.toString), filter) } ({ totalCount })</span>
    //    } else {
    //      return <span>{ pageXml(0, totalCount, Text("<<"), filter) }|{ pageXml(first - itemsPerPage max 0, totalCount, Text("<"), filter) }|{ zoom }|{ pageXml(first, totalCount, Text(curPage.toString), filter) }|{ pageXml(first + itemsPerPage min itemsPerPage * (pageCount - 1) max 0, totalCount, Text(">"), filter) }|{ pageXml(itemsPerPage * (pageCount - 1), totalCount, Text(">>"), filter) } ({ totalCount })</span>
    //    }
  }

  /**
   * Returns XML that links to a page starting at the given record offset, if the offset is valid and not the current one.
   * @param ns The link text, if the offset is valid and not the current offset; or, if that is not the case, the static unlinked text to display
   */
  def pageXml(curPage: Long, maxPage: Long, renderedPage: Long, ns: NodeSeq, filter: () => Unit): NodeSeq = {
    (renderedPage == curPage || renderedPage < 0 || renderedPage > maxPage) match {
      case true => ns
      case false => net.liftweb.http.SHtml.link(pageUrl(renderedPage), filter, ns) //<a href={ pageUrl(newFirst) }>{ ns }</a> 
    }
  }

}
 

