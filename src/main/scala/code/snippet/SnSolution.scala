package code.snippet

import code.model.cache.AcsUser
import code.model.{MapAssignement, MapSolution, MapUser}
import code.utils.HtmlUtils
import net.liftweb.http._
import net.liftweb.http.S._
import net.liftweb.http.SHtml._
import org.tresto.traits.LogHelper
import org.tresto.utils.{FormatUtils, StringUtils, TPaginatorBootstrap}
import net.liftweb.mapper._
import net.liftweb.common._
import org.tresto.language.snippet.Lang
import net.liftweb.http.SHtml.BasicElemAttr

import scala.xml.{NodeSeq, Unparsed}

class SolutionFilter() {
  var name: Box[String] = Empty
}


object SnSolution extends TPaginatorBootstrap with LogHelper {
  private object selectedFilterData extends RequestVar[Box[SolutionFilter]](Some(new SolutionFilter())) //Store the filter data for the next request

  def indexLink = WebLinks.SOLUTION.getIndex()

  def list():NodeSeq = {
    val userType = MapUser.curUserType.get.head
    val fl = selectedFilterData.get.head
    val data = getFilteredData(currentPage * itemsPerPage)
    val assignmentMap = MapAssignement.findAll(ByList(MapAssignement.id, data.map(_.assignementId.get))).groupBy(_.id.get)

    val rows = data.sortWith((a,b) => a.id.get > b.id.get).map(u =>
      <tr class="table table-condensed table-striped">
        <td>{ u.id.get }</td>
        {if (userType == MapUser.USERTYPE_STAFF  || userType == MapUser.USERTYPE_SITEVISOR) <td class="hidden-xs">{AcsUser.cacheGetById(u.userId.get).map(_.niceName).getOrElse(Lang.gs("user.unknown"))}</td>}
        <td>{ assignmentMap.get(u.assignementId.get).flatMap(_.headOption).map(a => StringUtils.limitString(StringUtils.removeXMLTags(a.description.get),30)).getOrElse(Lang.g("assignement.unknown"))}</td>
        <td class="hidden-xs hidden-sm">{ FormatUtils.sanePeoples2Minute.format(u.modifiedDate.get)}</td>
        { userType match {
        case MapUser.USERTYPE_STAFF | MapUser.USERTYPE_SITEVISOR =>
          <td>{HtmlUtils.getEditLink(WebLinks.SOLUTION.getEdit(), u.id.get)}</td>
          <td class="hidden-xs hidden-sm">{ HtmlUtils.getDeleteLink(WebLinks.SOLUTION.getDelete(), u.id.get) }</td>
        case _ =>
          <td>{HtmlUtils.getAnyLink(WebLinks.SOLUTION.getEdit(), u.id.get.toString, Lang.g("solution.view"))}</td>
        }}
      </tr>)

    <div>
      <!--<div class="row">{ renderFilter() }</div>-->
      <div class="row" style="margin-top:20px">
        <!--<div class="col-sm-6"><a href={ WebLinks.SOLUTION.getAdd() }>{ Lang.g("solution.add") }</a>{ HtmlUtils.space2 }</div>-->
        <div class="col-sm-6 pull-right">{ getPaginator(data.size, () => selectedFilterData(Full(fl))) }</div>
      </div>
      <table class="table table-condensed table-striped">
        <tr>
          <th>{ Lang.g("solution.id") }</th>
          {if (userType == MapUser.USERTYPE_STAFF ||  userType == MapUser.USERTYPE_SITEVISOR) <th  class="hidden-xs">{Lang.g("solution.username")}</th>}
          <th>{ Lang.g("solution.assignement") }</th>
          <th  class="hidden-xs hidden-sm">{ Lang.g("solution.date") }</th>
          { userType match {
          case MapUser.USERTYPE_STAFF | MapUser.USERTYPE_SITEVISOR =>
            <th>{ Lang.g("solution.edit") }</th>
            <th class="hidden-xs hidden-sm">{ Lang.g("solution.delete") }</th>
          case _ =>
            <th>{ Lang.g("solution.view") }</th>
          }}
        </tr>
        { rows }
      </table>
    </div>

  }

  def getFilteredData(startAt: Int): (List[MapSolution]) = {
    val fl = selectedFilterData.get.head
    val queryParams = new scala.collection.mutable.ArrayBuffer[QueryParam[MapSolution]]
    if (MapUser.curUserType.get.head == MapUser.USERTYPE_STUDENT) {  //Students can only see their solutions
        queryParams += By(MapSolution.userId, MapUser.curUserId.get.head)
    }
    //fl.name.foreach(a => queryParams += Like(MapSolution.name,s"%$a%"))
    queryParams += OrderBy(MapSolution.id, Descending)
    queryParams += MaxRows(itemsPerPage)
    queryParams += StartAt(startAt)
    val lbs = MapSolution.findAll(queryParams: _*)
    return lbs
  }

  def renderFilter(): NodeSeq = {
    val fl = selectedFilterData.get.head

    def doFilter() {
      S.redirectTo(indexLink, () => selectedFilterData(Some(fl)))
    }

    <form action={ S.uri } method="post">
      <table>
        <tr class="bottomaligned">
          <td>{ Lang.g("solution.filter.name") }</td>
          <td></td>
        </tr>
        <tr class="topaligned">
          <td>{ SHtml.text("", a => fl.name = if (a.length() == 0) Empty else Some(a) , new BasicElemAttr("placeholder",Lang.gs("solution.filter.name"))) }</td>
          <td>{ SHtml.submit(Lang.gs("gen.filter"), doFilter) }</td>
        </tr>
      </table>
    </form>

  }


  def delete(): NodeSeq = {
    var obj = MapSolution.findByKey(HtmlUtils.getIdLong()).head
    val backLink = S.referer.openOr(indexLink)
    val displayName = obj.id.get //+ " (" + obj.id.get + ") "

    def submit() {
      obj.delete_!
      notice(Lang.g("solution.deleted", Lang.getLocale, List(displayName)))
      redirectTo(backLink)
    }

    <form action={ S.uri } method="post">
      { Lang.g("solution.do.you.want.to.delete", Lang.getLocale, List(displayName)) }{ HtmlUtils.space }
      <a href={ backLink }>{Lang.cancel }</a>{ HtmlUtils.space }
      { SHtml.submit(Lang.deletes, submit) }
    </form>
  }


  def edit(): NodeSeq = {
    var obj = MapSolution.findByKey(HtmlUtils.getIdLong()).head
    change(obj, DBOperation.UPDATE)
  }

  def add(): NodeSeq = {
    var assignementId = HtmlUtils.getIdLong()
    val obj = MapSolution.create.assignementId(assignementId).modifiedDate(new java.util.Date())
    change(obj, DBOperation.INSERT)
  }

  def change(obj:MapSolution,operation: String):NodeSeq = {
    val CODE_AREA = "codeArea"
    val backLink = S.referer.openOr(indexLink)
    obj.userId(MapUser.curUserId.get.head)
    val assignement = MapAssignement.find(obj.assignementId.get).head

    def submit() {
      val now = new java.util.Date()
      obj.modifiedDate(now)
      log.debug("save, operation: " + operation)
      obj.save()
      redirectTo(backLink)
    }


    <div>
      <head>
        <script type="text/javascript" src="/static/plugins/codemirror/codemirror.js"></script>
        <link rel="stylesheet" href="/static/plugins/codemirror/codemirror.css"/>
        <link rel="stylesheet" href="/static/plugins/codemirror/theme/ambiance.css"/>
        <script type="text/javascript" src="/static/plugins/codemirror/mode/clike/clike.js"></script>
        <script type="text/javascript">{
          Unparsed("""  $(document).ready(function(){
             var codeEditor = CodeMirror.fromTextArea(document.getElementById('""" + CODE_AREA + """'), {
                 lineNumbers: true,
                 matchBrackets: true,
                 styleActiveLine: true,
                 lineWrapping: true,
                 theme:"ambiance",
                 mode:"text/x-scala"
             });
             codeEditor.setSize("100%", 600);
    });
""")
          }</script>
      </head>
      <form action={ S.uri } method="post">
        <table class="table table-striped formtable">
          <tr><td colspan="2">{assignement.description.get}</td></tr>
          <tr><td colspan="2">{ SHtml.textarea(obj.solution.get, obj.solution(_), new SHtml.BasicElemAttr("id" , CODE_AREA) ,new SHtml.BasicElemAttr("style" , "width: 100%; height: 600px;")) }</td></tr>
          <tr><td><a href={ backLink}>{Lang.back()}</a></td><td>{ SHtml.submit(Lang.saves(), submit) }</td></tr>
        </table>
      </form>
    </div>
  }
}
