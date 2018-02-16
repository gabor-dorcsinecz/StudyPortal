package code.snippet

import code.model.cache.AcsUser
import code.model.{MapAssignement, MapTopic, MapUser}
import code.utils.HtmlUtils
import net.liftweb.http._
import net.liftweb.http.S._
import net.liftweb.http.SHtml._
import org.tresto.traits.LogHelper
import org.tresto.utils._
import net.liftweb.mapper._
import net.liftweb.common._
import org.tresto.language.snippet.Lang
import net.liftweb.http.SHtml.BasicElemAttr

import scala.xml.{NodeSeq, Unparsed}

class TopicFilter() {
  var name: Box[String] = Empty
}


object SnTopic extends TPaginatorBootstrap with LogHelper {
  private object selectedFilterData extends RequestVar[Box[TopicFilter]](Some(new TopicFilter())) //Store the filter data for the next request

  def indexLink = WebLinks.TOPIC.getIndex()

  def list():NodeSeq = {
    val userType = MapUser.curUserType.get.head
    val fl = selectedFilterData.get.head
    val data = getFilteredData(currentPage * itemsPerPage)

    val rows = data.sortWith((a,b) => a.id.get > b.id.get).map(u =>
      <tr class="table table-condensed table-striped">
        <td>{ u.sequenceNumber.get }</td>
        <td>{ u.name.get}</td>
        <td><a href={ u.docsUrl.get}>{ u.docsUrl.get}</a></td>
        <td class="hidden-xs hidden-sm">{ FormatUtils.sanePeoples2Minute.format(u.modifiedDate.get)}</td>
          <td>{HtmlUtils.getEditLink(WebLinks.TOPIC.getEdit(), u.id.get)}</td>
          <td class="hidden-xs hidden-sm">{ HtmlUtils.getDeleteLink(WebLinks.TOPIC.getDelete(), u.id.get) }</td>
      </tr>)

    <div>
      <!--<div class="row">{ renderFilter() }</div>-->
      <div class="row" style="margin-top:20px">
        <div class="col-sm-6"><a href={ WebLinks.TOPIC.getAdd() }>{ Lang.g("topic.add") }</a>{ HtmlUtils.space2 }</div>
        <div class="col-sm-6 pull-right">{ getPaginator(data.size, () => selectedFilterData(Full(fl))) }</div>
      </div>
      <table class="table table-condensed table-striped">
        <tr>
          <th>{ Lang.g("topic.sequenceNumber") }</th>
          <th>{ Lang.g("topic.name") }</th>
          <th>{ Lang.g("topic.confluenceurl") }</th>
          <th  class="hidden-xs hidden-sm">{ Lang.g("topic.date") }</th>
          <th>{ Lang.g("topic.edit") }</th>
          <th class="hidden-xs hidden-sm">{ Lang.g("topic.delete") }</th>
        </tr>
        { rows }
      </table>
    </div>

  }

  def getFilteredData(startAt: Int): (List[MapTopic]) = {
    val fl = selectedFilterData.get.head
    val queryParams = new scala.collection.mutable.ArrayBuffer[QueryParam[MapTopic]]
    fl.name.foreach(a => queryParams += Like(MapTopic.name ,s"%$a%"))
    queryParams += OrderBy(MapTopic.sequenceNumber, Ascending)
    queryParams += MaxRows(itemsPerPage)
    queryParams += StartAt(startAt)
    val lbs = MapTopic.findAll(queryParams: _*)
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
          <td>{ Lang.g("topic.filter.name") }</td>
          <td></td>
        </tr>
        <tr class="topaligned">
          <td>{ SHtml.text("", a => fl.name = if (a.length() == 0) Empty else Some(a) , new BasicElemAttr("placeholder",Lang.gs("topic.filter.name"))) }</td>
          <td>{ SHtml.submit(Lang.gs("gen.filter"), doFilter) }</td>
        </tr>
      </table>
    </form>

  }


  def delete(): NodeSeq = {
    var obj = MapTopic.findByKey(HtmlUtils.getIdLong()).head
    val backLink = S.referer.openOr(indexLink)
    val displayName = obj.id.get //+ " (" + obj.id.get + ") "

    def submit() {
      obj.delete_!
      notice(Lang.g("topic.deleted", Lang.getLocale, List(displayName)))
      redirectTo(backLink)
    }

    <form action={ S.uri } method="post">
      { Lang.g("topic.do.you.want.to.delete", Lang.getLocale, List(displayName)) }{ HtmlUtils.space }
      <a href={ backLink }>{Lang.cancel }</a>{ HtmlUtils.space }
      { SHtml.submit(Lang.deletes, submit) }
    </form>
  }


  def edit(): NodeSeq = {
    var obj = MapTopic.findByKey(HtmlUtils.getIdLong()).head
    change(obj, DBOperation.UPDATE)
  }

  def add(): NodeSeq = {
    val obj = MapTopic.create.modifiedDate(new java.util.Date())
    change(obj, DBOperation.INSERT)
  }

  def change(obj:MapTopic,operation: String):NodeSeq = {
    val CODE_AREA = "codeArea"
    val backLink = S.referer.openOr(indexLink)

    def submit() {
      val now = new java.util.Date()
      obj.modifiedDate(now)
      log.debug("save, operation: " + operation)
      obj.save()
      redirectTo(backLink)
    }


    <form method="post" action={ S.uri } role="form">
      <div class="row">
        <div class="col-lg-6 col-md-8">
          <div class="panel panel-info">
            <div class="panel-heading">{ Lang.g("topic.setting.basic") }</div>
            <div class="panel-body">
              <div class="form-group"><label>{ Lang.g("topic.name") }</label>{BHtml.text(obj.name.get, a => obj.name(a)) } </div>
              <div class="form-group"><label>{ Lang.g("topic.sequence.number") }</label>{BHtml.text(obj.sequenceNumber.get.toString, a => obj.sequenceNumber(a.toInt)) } </div>
              <div class="form-group"><label>{ Lang.g("topic.url") }</label>{BHtml.text(obj.docsUrl.get, a => obj.docsUrl(a)) } </div>
              <div class="form-group"><label>{ SHtml.link(backLink, () => (), Lang.cancel) } </label>{ HtmlUtils.space2 } { BHtml.button(Lang.saves, submit) }</div>
            </div>
          </div>
        </div>
      </div>
    </form>
  }
}
