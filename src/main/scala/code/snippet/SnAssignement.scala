package code.snippet

import code.model.{MapAssignement, MapSolution, MapTopic, MapUser}
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

class AssignementFilter() {
  var name: Box[String] = Empty
}


object SnAssignement extends TPaginatorBootstrap with LogHelper {
  private object selectedFilterData extends RequestVar[Box[AssignementFilter]](Some(new AssignementFilter())) //Store the filter data for the next request

  def indexLink = WebLinks.ASSIGNEMENT.getIndex()


  def list():NodeSeq = {
    val userType = MapUser.curUserType.get.head
    val fl = selectedFilterData.get.head
    val assignements = getFilteredData(currentPage * itemsPerPage)
    //val topicMap = MapTopic.findAll(ByList(MapTopic.id, data.map(_.topicId.get))).groupBy(_.id.get)
    val topics = MapTopic.findAll(OrderBy(MapTopic.sequenceNumber, Ascending))

    def getAssignements4Topic(topicId:Long):NodeSeq = {
      assignements.filter(a => a.topicId.get == topicId).map(u =>
        <tr class="table table-condensed table-striped">
          <td>{ u.id.get }</td>
          <td>{ StringUtils.limitString(StringUtils.removeXMLTags(u.description.get),90) }</td>
          {
          if (userType == MapUser.USERTYPE_STAFF || userType == MapUser.USERTYPE_SITEVISOR) {
            <td>{HtmlUtils.getEditLink(WebLinks.ASSIGNEMENT.getEdit(), u.id.get)}</td>
              <td>{HtmlUtils.getDeleteLink(WebLinks.ASSIGNEMENT.getDelete(), u.id.get)}</td>
          }else {
            <td>{HtmlUtils.getAnyLink(WebLinks.SOLUTION.getAdd(),u.id.get.toString,Lang.g("solution.solve"))}</td>
          }
          }
        </tr>)
    }

    val mainRows = topics.map(t =>
      <tr data-toggle="collapse" data-target={ "#AssignementRow" + t.id.get.toString } class="accordion-toggle">
        <td><button class="btn btn-default btn-xs"><span class="glyphicon glyphicon-eye-open"></span></button></td>
        <td><h3>{t.name.get}</h3></td>
        <td><a href={t.docsUrl.get}><h3>{Lang.g("assignement.docs.link")}</h3></a></td>
        </tr>
        <tr>
          <td colspan="10" class="hiddenRow">
            <div id={ "AssignementRow" + t.id.get.toString } class="accordian-body collapse">
              <table class="table table-condensed table-striped">
                {getAssignements4Topic(t.id.get)}
              </table>
            </div>
          </td>
        </tr>

    )

    val unassigned = assignements.filter(a => a.topicId.get == 0).length match {
      case 0 => NodeSeq.Empty
      case _ => <span><tr><td colspan="10"><h2>Unassigned Assignements</h2></td></tr>{getAssignements4Topic(0)}</span>
    }

    <div>
      <div class="row">
      <!-- { renderFilter() }-->
      </div>
      <div class="row" style="margin-top:20px">
        <div class="col-sm-6"><a href={ WebLinks.ASSIGNEMENT.getAdd() }>{ Lang.g("assignement.add") }</a>{ HtmlUtils.space2 }</div>
        <!--<div class="col-sm-6 pull-right">{ getPaginator(data.size, () => selectedFilterData(Full(fl))) }</div>-->
      </div>
      <table class="table">
        { mainRows }
        {unassigned.headOption.getOrElse(NodeSeq.Empty) }
      </table>
    </div>

  }

  def getFilteredData(startAt: Int): (List[MapAssignement]) = {
    val fl = selectedFilterData.get.head
    val queryParams = new scala.collection.mutable.ArrayBuffer[QueryParam[MapAssignement]]

    fl.name.foreach(a => queryParams += Like(MapAssignement.name,s"%$a%"))
    queryParams += OrderBy(MapAssignement.id, Ascending)
    //queryParams += MaxRows(itemsPerPage)
    //queryParams += StartAt(startAt)
    val lbs = MapAssignement.findAll(queryParams: _*)
    return lbs
  }

  def renderFilter(): NodeSeq = {
    val fl = selectedFilterData.get.head

    def doFilter() {
      S.redirectTo(indexLink, () => selectedFilterData(Some(fl)))
    }

    <form action={ S.uri } method="post">
        <table>
          <tr class="topaligned">
            <td>{ SHtml.text("", a => fl.name = if (a.length() == 0) Empty else Some(a) , new BasicElemAttr("placeholder",Lang.gs("assignement.filter.name"))) }</td>
            <td>{ SHtml.submit(Lang.gs("gen.filter"), doFilter) }</td>
          </tr>
        </table>
      </form>

  }


  def delete(): NodeSeq = {
    var obj = MapAssignement.findByKey(HtmlUtils.getIdLong()).head
    val backLink = S.referer.openOr(indexLink)
    val displayName = obj.name.get + " (" + obj.id.get + ") "

    def submit() {
      obj.delete_!
      notice(Lang.g("assignement.deleted", Lang.getLocale, List(displayName)))
      redirectTo(backLink)
    }

    <form action={ S.uri } method="post">
      { Lang.g("assignement.do.you.want.to.delete", Lang.getLocale, List(displayName)) }{ HtmlUtils.space }
      <a href={ backLink }>{Lang.cancel }</a>{ HtmlUtils.space }
      { SHtml.submit(Lang.deletes, submit) }
    </form>
  }


  def edit(): NodeSeq = {
    var obj = MapAssignement.findByKey(HtmlUtils.getIdLong()).head
    change(obj, DBOperation.UPDATE)
  }

  def add(): NodeSeq = {
    val obj = MapAssignement.create.modifiedDate(new java.util.Date()).language(MapAssignement.LANG_SCALA)
    change(obj, DBOperation.INSERT)
  }

  def change(obj:MapAssignement,operation: String):NodeSeq = {
    val CODE_AREA = "codeArea"
    val backLink = S.referer.openOr(indexLink)
    val topicsAll = MapTopic.findAll(OrderBy(MapTopic.sequenceNumber,Ascending))
    val topicsMap = topicsAll.map(a => a.id.get -> a.name.get).toMap
    val topicsList = topicsAll.map(a => (a.id.get.toString, a.sequenceNumber.get + " " + a.name.get))

    def submit() {
      val now = new java.util.Date()
      obj.modifiedDate(now)
      log.debug("save, operation: " + operation)
      obj.save()
      redirectTo(backLink)
    }


    <div>
      <!--
      <head>
        <script type="text/javascript" src="/static/plugins/codemirror/codemirror.js"></script>
        <liFnk rel="stylesheet" href="/static/plugins/codemirror/codemirror.css"/>
        <link rel="stylesheet" href="/static/plugins/codemirror/theme/ambiance.css"/>
        <script type="text/javascript" src="/static/plugins/codemirror/mode/xml/xml.js"></script>
        <script type="text/javascript">{
          Unparsed("""  $(document).ready(function(){
             var codeEditor = CodeMirror.fromTextArea(document.getElementById('""" + CODE_AREA + """'), {
                 lineNumbers: true,
                 matchBrackets: true,
                 styleActiveLine: true,
                 lineWrapping: true,
                 theme:"ambiance",
                 mode:"text/xml"
             });
             codeEditor.setSize("100%", 600);
    });
""")
          }</script>
      </head>
      -->
      <form action={ S.uri } method="post">
        <table class="table table-striped formtable">
          <!--<tr><td>{ Lang.g("assignement.name") }</td><td>{ SHtml.text(obj.name.get, obj.name(_)) }</td></tr>-->
          { if (operation == DBOperation.UPDATE) <tr><td>{ Lang.g("assignement.changed") }</td><td>{ FormatUtils.sanePeoples2Minute.format(obj.modifiedDate.get) }</td></tr> }
          <tr><td colspan="2">{ SHtml.textarea(obj.description.get, obj.description(_), new SHtml.BasicElemAttr("id" , CODE_AREA) ,new SHtml.BasicElemAttr("style" , "width: 100%; height: 600px;")) }</td></tr>
          <tr><td>{Lang.g("assignement.language")}</td><td>{SHtml.select(MapSolution.solutionTypeList(), Full(obj.solutionType.get), a => obj.solutionType(a) ) }</td></tr>
          <tr><td>{Lang.g("assignement.topic")}</td><td>{SHtml.select(topicsList, Full(topicsMap.get(obj.topicId.get).getOrElse("")), a => obj.topicId(a.toLong) ) }</td></tr>
          <tr><td><a href={ backLink}>{Lang.cancel()}</a></td><td>{ SHtml.submit(Lang.saves(), submit) }</td></tr>
        </table>
      </form>
    </div>
  }
}
