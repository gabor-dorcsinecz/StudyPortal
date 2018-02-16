package org.tresto.utils

import net.liftweb.sitemap.Loc.Link
import net.liftweb.http.S
import net.liftweb.util.Helpers.appendParams
import net.liftweb.util.Helpers.toInt
import scala.xml._


case class TLink(ls: List[String]) {
  def getLink(): String = {
    ls.mkString("/", "/", "/") //Points to a directory
  }
  def getIndex(): String = getLink + "index"
  def getDelete(): String = getLink + "delete"
  def getAdd(): String = getLink + "add"
  def getEdit(): String = getLink + "edit"
  def getExport(): String = getLink + "export"

  def getList(): List[String] = ls
  def getIndexList(): List[String] = ls :+ "index"
  def getIndexLink(): Link[Unit] = new Link(getIndexList(), false)
  def getDeleteList(): List[String] = ls :+ "delete"
  def getDeleteLink(): Link[Unit] = new Link(getDeleteList(), false)
  def getAddList(): List[String] = ls :+ "add"
  def getAddLink(): Link[Unit] = new Link(getAddList(), false)
  def getEditList(): List[String] = ls :+ "edit"
  def getEditLink(): Link[Unit] = new Link(getEditList(), false)
  def getExportList(): List[String] = ls :+ "export"
  def getExportLink(): Link[Unit] = new Link(getExportList(), false)
}


/**
  * This paginator must be used with the bootstrap template, and it will present the next and previous page, no jump to the last.
  * This paginator can be used with a filter, so when you click on a link the filter information is forwarded to the next page, therefore the information is retained
  * This paginator does not have a precalculated final size, so cannot jump to the last, and no pagecount/total object count
  * How to use it:
  *
  * object XXX extends TPaginatorSimple
  *
  * protected object selectedFilterData extends RequestVar[Box[CDRFilter]](Full(new CDRFilter()))
  *
  *    val filter = selectedFilterData.is.open_!
  *    val data = run queries here
  *  when you put the paginator in the page use it like this:
  *    getPaginator(() => selectedFilterData(Full(filter)))
  *
  */
trait TPaginatorBootstrap {
  val offsetParam = "offset"

  //def zoomedPages(curPage: Int) = (curPage - 2 to curPage + 2).filter(n => n >= 0)

  /** How many items to put on each page */
  def itemsPerPage = 50

  /** Returns a URL used to link to a page starting at the given record offset.  */
  def pageUrl(offset: Long): String = appendParams(S.uri, List(offsetParam -> offset.toString))

  /** Overrides the super's implementation so the first record can be overridden by a URL query parameter.   */
  def currentPage(): Int = S.param(offsetParam).map(toInt) openOr 0

  def objectStartOnPage(): Int = currentPage * itemsPerPage //Can be used for queries, like select from .... OFFSET (thisValue)
  def objectsEndOnPage(): Int = (currentPage + 1) * itemsPerPage

  /** Calculates the current page number, based on the value of 'first.'   */
  //def getCurPage() = (first / itemsPerPage).toInt

  /**
    *  Get the bootstrap styled paginator. First is page = 0 (curPage)
    *  Example: we are on page 0 and itemsOnCurrentPage < itemsPerPage => show no paginator! Why include a useless control?
    *  Example: we are on page 0 and itemsOnCurrentPage = itemsPerPage => show 0 1 >   (0 disabled)
    *  Example: we are on page 1  () and the itemsOnCurrentPage < itemsPerPage => show << < 0 1     (1 disabled)
    *  Example: we are on page 1  () and the itemsOnCurrentPage = itemsPerPage => show << < 0 1 2 > (1 disabled)
    *  Example: we are on page n  () and the itemsOnCurrentPage < itemsPerPage => show << < n-1 n     (n disabled)
    *  Example: we are on page n  () and the itemsOnCurrentPage = itemsPerPage => show << < n-1 n n+1 > (n disabled)
    */
  def getPaginator(itemsOnCurrentPage: Int, filter: () => Unit): NodeSeq = {
    val curPage = currentPage() //What page are we at?
    if (curPage == 0 && itemsOnCurrentPage < itemsPerPage) { //show no paginator! Why include a useless control?
      return NodeSeq.Empty
    }
    val isLastPage = (itemsOnCurrentPage < itemsPerPage)
    val isFirstPage = (curPage == 0)
    //println(" curPage: " + curPage + " itemsOnCurrentPage: " + itemsOnCurrentPage + " isFirstPage: " + isFirstPage + " isLastPage: " + isLastPage)
    val zp = isLastPage match {
      case true => (curPage - 1 to curPage).filter(n => n >= 0)
      case false => (curPage - 1 to curPage + 1).filter(n => n >= 0)
    }
    val zoom = zp.map(a => pageXml(curPage, a, Text(a.toString), filter))
    val jump2First = isFirstPage match {
      case true => NodeSeq.Empty
      case false => pageXml(curPage, 0, Text("<<"), filter)
    }
    val jump2Previous = isFirstPage match {
      case true => NodeSeq.Empty
      case false => pageXml(curPage, curPage - 1, Text("<"), filter)
    }
    val jump2Next = isLastPage match {
      case true => NodeSeq.Empty //The last page does not have a next!
      case false => pageXml(curPage, curPage + 1, Text(">"), filter)
    }

    <div class="pull-right"><nav><ul class="pagination paglow">{ jump2First }{ jump2Previous }{ zoom }{ jump2Next }</ul></nav></div>
  }

  /** Render one page number, taking into consideration that it may be invalid page (cannot click on it)*/
  def pageXml(curPage: Long, renderedPage: Long, ns: NodeSeq, filter: () => Unit): NodeSeq = {
    if (renderedPage == curPage) {
      return <li class="active">{ net.liftweb.http.SHtml.link(pageUrl(renderedPage), filter, ns) }</li>
    }
    if (renderedPage < 0) {
      return <li class="disabled">{ net.liftweb.http.SHtml.link(pageUrl(renderedPage), filter, ns) }</li>
    }
    return <li>{ net.liftweb.http.SHtml.link(pageUrl(renderedPage), filter, ns) }</li>

  }

}