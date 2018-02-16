package code.snippet

import _root_.net.liftweb._
import http._
import mapper._
import S._
import SHtml._
import common._
import util._
import Helpers._

import _root_.scala.xml.{Group, NodeSeq, Text}
import _root_.java.util.Locale

import code.model.MapUser
import code.model.cache.AcsUser
import code.utils.HtmlUtils

import scala.BigDecimal
import net.liftweb.util.Helpers
import org.tresto.traits.LogHelper
import js.JsCmds._
import js.jquery._
import js._
import org.tresto.language.snippet.Lang

import scala.xml._
import org.tresto.utils.security._

import scala.collection.mutable.HashMap
import org.tresto.utils.{BHtml, BillingConstants, TPaginatorBootstrap}

class UserFilter {
  var anyField: Box[String] = Empty
  var isDeleted: Boolean = false
}


object SnUser extends TPaginatorBootstrap with LogHelper {

  private object selectedFilterData extends RequestVar[Box[UserFilter]](Some(new UserFilter())) //Store the filter data for the next request

  def indexLink = WebLinks.USER.getIndex()

  def list(): NodeSeq = {
    val userType = MapUser.curUserType.get.head
    val fl = selectedFilterData.get.head
    val data = getFilteredData(currentPage * itemsPerPage)

    val rows = data.sortWith((a, b) => a.id.get > b.id.get).map(u =>
      <tr class="table table-condensed table-striped">
        <td>{u.id.get}</td>
        <td class="hidden-xs">{u.firstName + " " + u.lastName}</td>
        <td>{u.email}</td>
        <th class="hidden-xs hidden-sm">{BillingConstants.accessRightsMap(u.userType.get)}</th>
        <td>{HtmlUtils.getEditLink(WebLinks.USER.getEdit(), u.id.get)}</td>
        <td>{HtmlUtils.getDeleteLink(WebLinks.USER.getDelete(), u.id.get)}</td>
      </tr>)

    <div>
      <!--<div class="row">{renderFilter()}</div>-->
      <div class="row" style="margin-top:20px">
        <div class="col-sm-6">
          <a href={WebLinks.USER.getAdd()}>{Lang.g("user.add")}</a>{HtmlUtils.space2}
        </div>
        <div class="col-sm-6 pull-right">{getPaginator(data.size, () => selectedFilterData(Full(fl)))}</div>
      </div>
      <table class="table table-condensed table-striped">
        <tr>
          <th>{Lang.g("user.id")}</th>
          <th class="hidden-xs">{Lang.g("user.name")}</th>
          <th>{Lang.g("user.email")}</th>
          <th class="hidden-xs hidden-sm">{Lang.g("user.rigths")}</th>
          <th>{Lang.g("user.edit")}</th>
          <th>{Lang.g("user.delete")}</th>
        </tr>{rows}
      </table>
    </div>

  }

  def getFilteredData(startAt: Int): (List[MapUser]) = {
    val fl = selectedFilterData.get.head
    var users = AcsUser.cacheGetAll(true)
    fl.anyField.map(a => users = users.filter(f => f.search(a)))
    users = users.filter(a => a.isDeleted.get == fl.isDeleted)
    users = users.slice(objectStartOnPage(), objectsEndOnPage())
    users = users.sortWith(AcsUser.emailSorter)
    return users
  }

  def renderFilter(): NodeSeq = {
    val fl = selectedFilterData.get.head

    def doFilter() {
      S.redirectTo(indexLink, () => selectedFilterData(Some(fl)))
    }

    <form action={S.uri} method="post">
      <table>
        <tr class="bottomaligned">
          <td>{Lang.g("user.filter.name")}</td>
          <td></td>
        </tr>
        <tr class="topaligned">
          <td>{SHtml.text("", a => fl.anyField = if (a.length() == 0) Empty else Some(a), new BasicElemAttr("placeholder", Lang.gs("user.filter.name")))}</td>
          <td>{SHtml.submit(Lang.gs("gen.filter"), doFilter)}</td>
        </tr>
      </table>
    </form>

  }


  def delete(): NodeSeq = {
    var obj = MapUser.findByKey(HtmlUtils.getIdLong()).head
    val backLink = S.referer.openOr(indexLink)
    val displayName = obj.email.get + " (" + obj.id.get + ") "

    def submit() {
      obj.delete_!
      notice(Lang.g("user.deleted", Lang.getLocale, List(displayName)))
      redirectTo(backLink)
    }

    <form action={S.uri} method="post">
      {Lang.g("user.do.you.want.to.delete", Lang.getLocale, List(displayName))}{HtmlUtils.space}<a href={backLink}>
      {Lang.cancel}
    </a>{HtmlUtils.space}{SHtml.submit(Lang.deletes, submit)}
    </form>
  }


  def edit(): NodeSeq = {
    var obj = MapUser.findByKey(HtmlUtils.getIdLong()).head
    change(obj, DBOperation.UPDATE)
  }

  def add(): NodeSeq = {
    val obj = MapUser.create
    change(obj, DBOperation.INSERT)
  }

  def change(item: MapUser, operation: String): NodeSeq = {
    val CODE_AREA = "codeArea"
    val backLink = S.referer.openOr(indexLink)
    val passwordGuiHelper = new PasswordGuiHelper(item.locale.get)

    def submit() {
      val now = new java.util.Date()
      log.debug("save user: " + operation)
      operation match {
        case DBOperation.UPDATE => AcsUser.cacheAndDbUpdate(item)
        case DBOperation.INSERT => AcsUser.cacheAndDbInsert(item)
      }
//      if (item.userType.dirty_? || item.email.dirty_?) {
//        AcsUser.cacheAndDbReloadAll()
//      }

      redirectTo(backLink)//      if (item.userType.dirty_? || item.email.dirty_?) {
//        AcsUser.cacheAndDbReloadAll()
//      }

    }


    <form method="post" action={ S.uri } role="form">
      <div class="row">
        <div class="col-lg-6 col-md-8">
          <div class="panel panel-info">
            <div class="panel-heading">{ Lang.g("user.setting.basic") }</div>
            <div class="panel-body">
              <div class="form-group"><label>{ Lang.g("user.type") }</label>{BHtml.select(BillingConstants.accessRightsList, Full(item.userType.get.toString), a => item.userType(a.toInt)) } </div>
              <div class="form-group"><label>{ Lang.g("user.email") }</label>{BHtml.text(item.email.get, a => item.email(a)) } </div>
              { passwordGuiHelper.gui1 }
              { passwordGuiHelper.gui2 }
              <div class="form-group"><label>{ Lang.g("user.firstname") }</label>{ BHtml.text(item.firstName.get, item.firstName(_)) }</div>
              <div class="form-group"><label>{ Lang.g("user.lastname") }</label>{ BHtml.text(item.lastName.get, item.lastName(_)) }</div>
              <div class="form-group"><label>{ Lang.g("user.locale") }</label>{ SHtml.select(BillingConstants.getLanguages(), Full(item.locale.get), item.locale(_)) % ("class" -> "form-control") }</div>
              <div class="form-group"><label>{ Lang.g("user.timezone") }</label>{ SHtml.select(MappedTimeZone.timeZoneList, Full(item.timezone.get), item.timezone(_)) % ("class" -> "form-control") }</div>
              <div class="form-group"><label>{ SHtml.link(backLink, () => (), Lang.cancel) } </label>{ HtmlUtils.space2 } { BHtml.button(Lang.saves, submit) }</div>
            </div>
          </div>
        </div>
      </div>
    </form>   }
}

///**
// * Handles the listing of the data in a TABLE ,and the editing of those table data rows in a FORM
// */
//object SnUserStaff extends TPaginatorBootstrap with LogHelper {
//
//  private object selectedRowData extends RequestVar[Box[MapUser]](Empty)
//  private object selectedFilterData extends RequestVar[Box[StaffFilter]](Full(new StaffFilter()))
//
//  val VIEW_STAFF = "staff"
//  val VIEW_CUSTOMERS = "customers"
//
////  def list():NodeSeq = Text("Users list")
//  def add():NodeSeq = Text("Users Add")
//  def edit():NodeSeq = Text("Users Edit")
//  def delete():NodeSeq = Text("Users Delete")
//
////  def getUserStaffEditLink(cid: Long, linkText: NodeSeq): NodeSeq = {
////    <a href={ getUserStaffEditUri(cid) }>{ linkText }</a>
////  }
////  def getUserStaffEditUri(userId: Long): String = WebLinks.USERSTAFF.getEdit + "?" + WebLinks.pANYID + "=" + userId
////  def getUserStaffAddUri(superCompanyId: Long): String = WebLinks.USERSTAFF.getAdd + "?" + WebLinks.pANYID + "=" + superCompanyId //This is the supercompany id, not the userid
////
//
//  def list(): NodeSeq = {
//    val cid = MapUser.curSuperCampanyId.is.head
//    val cuser = AcsUser.getCurrentUser
//    var users = AcsUser.cacheGetAll(true)
//    val fl = selectedFilterData.is.head
//    fl.anyField.map(a => users = users.filter(f => f.search(a)))
//    users = users.filter(a => a.isDeleted.get == fl.isDeleted)
//    users = users.slice(objectStartOnPage(), objectsEndOnPage())
//    users = users.sortWith(AcsUser.emailSorter)
//
//
//    val endisable = fl.isDeleted match {
//      case true => Lang.g("user.doenable")
//      case false => Lang.g("user.dodisable")
//    }
//
//    def getDeleteLink(u: MapUser): NodeSeq = {
//      u.isDeleted.get match {
//        case true => //This company is disabled
//          link(WebLinks.USERSTAFF.getIndex, () => { undelete(u); selectedFilterData(Full(fl)) }, Lang.g("user.enable"))
//        case false => //This company is enabled
//          cuser.userType.get match {
//            case MapUser.USERTYPE_XYTONVISOR =>
//              link(WebLinks.USERSTAFF.getDelete, () => { selectedRowData(Full(u)); selectedFilterData(Full(fl)) }, Lang.g("user.delete.disable"))
//            case _ =>
//              viewType match {
//                case VIEW_CUSTOMERS => // customers users view
//                  val ou = partnercompanies.get(u.superCompanyId.get)
//                  ou.isEmpty match {
//                    case true =>
//                      Lang.g("user.delete.cannot")
//                    case false =>
//                      ou.get.internalstatus.get match {
//                        case MapCompany.INTERNALSTATUS_CLAIMED => Lang.g("user.delete.cannot")
//                        case _ => link(WebLinks.USERSTAFF.getDelete, () => { selectedRowData(Full(u)); selectedFilterData(Full(fl)) }, Lang.g("user.disable"))
//                      }
//                  }
//                case _ => //staff users view
//                  link(WebLinks.USERSTAFF.getDelete, () => { selectedRowData(Full(u)); selectedFilterData(Full(fl)) }, Lang.g("user.disable"))
//              }
//          }
//      }
//    }
//
//    def getEditLink(u: MapUser): NodeSeq = {
//      viewType match {
//        case VIEW_STAFF => // customers users view
//          link(WebLinks.USERSTAFF.getEdit, () => { selectedRowData(Full(u)); selectedFilterData(Full(fl)) }, Lang.edit)
//        case VIEW_CUSTOMERS => // customers users view
//          val ou = partnercompanies.get(u.superCompanyId.get)
//          ou.isEmpty match {
//            case true => Lang.g("user.edit.cannot")
//            case false =>
//              ou.get.internalstatus.get match {
//                case MapCompany.INTERNALSTATUS_CREATED => link(WebLinks.USERSTAFF.getEdit, () => { selectedRowData(Full(u)); selectedFilterData(Full(fl)) }, Lang.edit)
//                case MapCompany.INTERNALSTATUS_CLAIMED => Lang.g("user.edit.cannot")
//              }
//          }
//      }
//    }
//    val tabledata = users.flatMap(u =>
//      <tr>
//        { if (cuser.userType.get == MapUser.USERTYPE_XYTONVISOR) <td>{ SnMainViewer.getPartnerEditLink(u.superCompanyId.get, Text(AcsCompany.cacheGetById(u.superCompanyId.get).map(a => a.organizationName.get).getOrElse("gen.unknown"))) }</td> }
//        { if (cuser.userType.get != MapUser.USERTYPE_XYTONVISOR && viewType == VIEW_CUSTOMERS) <td>{ partnercompanies.get(u.superCompanyId.get).map(a => a.organizationName.get).getOrElse("user.partner.unknown") }</td> }
//        { if (cuser.userType.get == MapUser.USERTYPE_XYTONVISOR) <td> { accessRightsMap.get(u.userType.get).getOrElse(Lang.g("gen.unknown")) } </td> }
//        <td>{ u.id }</td>
//        <td class="hidden-xs">{ u.firstName + " " + u.lastName }</td>
//        <td>{ u.email }</td>
//        <td class="hidden-xs">{ getAdditionalData(u) }</td>
//        <td>{ getEditLink(u) }</td>
//        <td>{ getDeleteLink(u) }</td>
//      </tr>)
//
//    <div>
//      { filter(viewType) }
//      <hr/>
//      <div class="row">
//        <div class="col-sm-6">
//          <a href={ WebLinks.USERSTAFF.getAdd }>{ Lang.g("user.staff.add") }</a>{ HtmlUtils.space() }
//          { if (isHyperOrSp) <a href={ WebLinks.USERSTAFF.getRange }>{ Lang.g("user.staff.addrange") }</a> }
//        </div>
//        <div class="col-sm-6 pull-right">
//          { getPaginator(users.size, () => selectedFilterData(Full(fl))) }
//        </div>
//      </div>
//      <table class="table table-condensed table-striped">
//        <tr>
//          { if ((cuser.userType.get == MapUser.USERTYPE_XYTONVISOR) || (cuser.userType.get != MapUser.USERTYPE_XYTONVISOR && viewType == VIEW_CUSTOMERS)) <th>{ Lang.g("supercompany.name") }</th> }
//          { if (cuser.userType.get == MapUser.USERTYPE_XYTONVISOR) <th>{ Lang.g("supercompany.rights") }</th> }
//          <th>{ Lang.g("user.id") }</th>
//          <th class="hidden-xs">{ Lang.g("user.name") }</th>
//          <th>{ Lang.g("user.email") }</th>
//          <th class="hidden-xs">{ Lang.g("user.additional") }</th>
//          <th>{ Lang.edits }</th>
//          <th style="width:100px">{ endisable }</th>
//        </tr>
//        { tabledata }
//      </table>
//    </div>
//  }
//
//
//  def undelete(u: MapUser) {
//    u.isDeleted(false)
//    AcsUser.cacheAndDbUpdate(u)
//  }
//
//  /**
//   * Confirm deleting a MapUser
//   */
//  def delete(): NodeSeq = {
//    val data = selectedRowData.is.open_!;
//    val dispname = AcsUser.getFullName(data)
//    log.debug("User ConfirmDelete: " + dispname)
//    val fl = selectedFilterData.is.open_!
//
//    def disableMe() {
//      notice(Lang.gs("user.staff.was.disabled") + ": " + dispname)
//      data.isDeleted(true)
//      AcsUser.cacheAndDbUpdate(data)
//      redirectTo(WebLinks.USERSTAFF.getIndex, () => selectedFilterData(Full(fl)))
//    }
//    def deleteMe() {
//      notice(Lang.gs("user.staff.was.deleted") + ": " + dispname)
//      AcsUser.cacheAndDbDelete(data)
//      redirectTo(WebLinks.USERSTAFF.getIndex, () => selectedFilterData(Full(fl)))
//    }
//
//    <form action={ S.uri } method="post">
//      <div>
//        { Lang.g("user.staff.really.disable", Lang.getLocale, List(dispname)) }{ HtmlUtils.space }
//        <a href={ WebLinks.USERSTAFF.getIndex }>{ Lang.cancel }</a>{ HtmlUtils.space }
//        { SHtml.submit(Lang.gs("user.staff.do.disable"), disableMe) }
//      </div>
//      <br/>
//      {
//        if (MapUser.curUserType.get.open_! == MapUser.USERTYPE_XYTONVISOR) {
//          <div>
//            { Lang.g("user.staff.really.delete", Lang.getLocale, List(dispname)) }{ HtmlUtils.space }
//            <a href={ WebLinks.USERSTAFF.getIndex }>{ Lang.cancel }</a>{ HtmlUtils.space }
//            { SHtml.submit(Lang.gs("user.staff.do.delete"), deleteMe) }
//          </div>
//        }
//      }
//    </form>
//
//  }
//
//  /**
//   * Edit a row in a FORM
//   */
//  def edit(): NodeSeq = {
//    val anyId = S.param(WebLinks.pANYID)
//    log.debug("edit user: " + anyId)
//    var item = anyId.isDefined match {
//      case true => AcsUser.cacheGetById(anyId.get.toLong).get
//      case false => selectedRowData.is.open_!;
//    }
//    change(item, LoGeneralCRUD.UPDATE)
//  }
//
//  /**
//   * Add a row in a FORM
//   */
//  def add(): NodeSeq = {
//    val curUser = AcsUser.cacheGetById(MapUser.curUserId.is.open_!).get
//    val obid = S.param(WebLinks.pANYID)
//    val superCompanyId = obid.isDefined match {
//      case true => obid.open_!.toLong //We got a parameter containing the supercompanyId
//      case false => curUser.superCompanyId.get //Put the user under the same supercompany that I am under
//    }
//    val us = MapUser.create.superCompanyId(superCompanyId).validated(true).uniqueId(Helpers.randomString(32)).locale(curUser.locale).timezone(curUser.timezone).userType(MapUser.curUserType.get.open_!)
//    us.sipNumberType(MapUser.NUMBERTYPE_EMAIL).userType(MapUser.USERTYPE_VOIP).sipNumberStatus(MapUser.NUMBER_STATUS_IN_USE)
//    change(us, LoGeneralCRUD.INSERT)
//  }
//
//  /**
//   * Render a Form, and save the data into the database
// *
//   * @param content - the snippet's enclosed xml data
//   * @param item - the item we should present in the form
//   */
//  def change(item: MapUser, operation: String): NodeSeq = {
//    val passwordGuiHelper = new PasswordGuiHelper(item.locale.get)
//    //    var password = item.password.get
//    //    var password2 = item.password.get
//    var sippassword = item.sipPassword.get
//    var email = item.email.get
//    //    var firstName = item.firstName.get
//    //    var lastName = item.lastName.get
//    //    var isEmailPublic = item.isEmailPublic.get
//    //    var locale = item.locale.get
//    //    var timezone = item.timezone.get
//    //    var sipShortNumber = item.sipShortNumber.get
//    //    var canLoginWeb = item.canLoginWeb.get
//    //var partnerCompanyId = item.partnerCompanyId.get.toString
//    val password_strength = ValueCell("")
//    val sippassword_strength = ValueCell("")
//    val link2return = S.referer.openOr(WebLinks.USERSTAFF.getIndex)
//    val userType = MapUser.curUserType.get.open_!
//    val companyList = List(("0", Lang.gs("gen.all"))) ++ AcsCompany.asList(AcsCompany.cacheGetAll(true))
//    val fl = selectedFilterData.is.open_!
//    val isHyperOrSp = org.tresto.helper.GenUser.isHyperOrServiceProvider()
//    val forwardTypesList = isHyperOrSp match {
//      case true => BillingConstants.forwardTypesSPList //The service provider can forward to a gateway too
//      case false => BillingConstants.forwardTypesList //normal user can only forward to numbers ,to scripts and apps
//    }
//    val sps4company = AcsServiceProvider.getServiceProviders4Company4VoIP(item.superCompanyId.get)
//    val allGws = sps4company.map(a => AcsGateway.getByCompanyId(a.companyId.get)).flatten
//    val gatewayList = ("0", org.tresto.language.snippet.Lang.gs("user.gateways.none")) :: AcsGateway.asList(allGws)
//    val cid = MapUser.curSuperCampanyId.is.open_!
//    val scriptList = MapUser.curUserType.is.open_! match {
//      case MapUser.USERTYPE_XYTONVISOR =>
//        AcsGuiScriptIvr.asList(AcsGuiScriptIvr.cacheGetAll())
//      case _ =>
//        AcsGuiScriptIvr.asList(AcsGuiScriptIvr.getByCompanyId(cid))
//    }
//
//    val isHypervisor = org.tresto.helper.GenUser.isHyperVisor()
//    val userDropdown = ("", Lang.gs("user.use.my.username")) :: org.tresto.helper.GenUser.getUsersDropdown(cid, isHypervisor)
//    if (item.apiKey.get == null || item.apiKey.get.length < 16) { //Create an api key, if it doesn't exist yet
//      item.apiKey(AcsUser.generateApiKey())
//    }
//
//    def submit() = { //Save the data
//      if (StringUtils.isNullOrEmpty(passwordGuiHelper.passwd1) == false) { //The password is displayed as * which is != the stored password, so it will be overwritten. so save it if the user typed in some password
//        val ctp = passwordGuiHelper.checkThePasswords
//        log.debug("checkThePasswords: " + ctp)
//        if (ctp.nonEmpty) { //If there was an error in the password display an error message
//          error(ctp)
//          S.redirectTo(link2return)
//        } else {
//          //item.setPasswordFromListString(List(passwordGuiHelper.passwd1, passwordGuiHelper.passwd2))
//          item.password(passwordGuiHelper.passwd1)
//        }
//      }
//      //      if (password.length <= 4) {
//      //        notice(Lang.gs("user.password.tooshort"))
//      //        S.redirectTo(link2return)
//      //        //S.redirectTo(S.uri)
//      //      }
//      if (AcsUser.isThereUserWithEmailBesidesMe(email, item.id.get) == true) {
//        notice(Lang.gs("notice.emailalreadyexists") + email)
//        S.redirectTo(link2return)
//      }
//      //Only hypervisors or a services can add a phone number = If he is NOT a hyper or a service provider and if it's a new phone number, we do not allow that.
//      if (org.tresto.helper.GenUser.isHyperOrServiceProvider() == false && operation == LoGeneralCRUD.INSERT && email.matches(BillingUtil.digitsOnlyRegex) == true) { //are there other characters which are NOT digits? Normal user cannot add phone number
//        notice(Lang.gs("notice.cannot.add.phonenumber", Lang.getLocale, List(email)))
//        S.redirectTo(link2return)
//      }
//      //Only hypervisors or a services can edit a phone number = If he is not a hyper or a service provider and if the email address changed and changed to numbers ?
//      if (org.tresto.helper.GenUser.isHyperOrServiceProvider() == false && operation == LoGeneralCRUD.UPDATE && email != item.email.get && email.matches(BillingUtil.digitsOnlyRegex) == true) { //are there other characters which are NOT digits? Normal user cannot add phone number
//        notice(Lang.gs("notice.cannot.edit.phonenumber", Lang.getLocale, List(email)))
//        S.redirectTo(link2return)
//      }
//
//      //      if ((passwordGuiHelper.passwd1 != item.password.get && operation == LoGeneralCRUD.UPDATE) || operation == LoGeneralCRUD.INSERT) {
//      //        item.password(passwordGuiHelper.passwd1) //save at new users, if existing user then check if it was changed (if existing we present the passowrd's HASH not the password, we shoud no save the password hash into the password)
//      //      }
//      log.debug("userupdate sippassword : " + item.sipPassword.get + " -> " + sippassword + " userpassword: " + item.password.get + " -> " + passwordGuiHelper.passwd1)
//      //item.partnerCompanyId(partnerCompanyId.toLong)
//      //item.userType(MapUser.USERTYPE_STAFF)
//      item.email(email.trim())
//      if (sippassword.length() > 5) {
//        item.sipPassword(sippassword)
//      }
//
//      //item.password(password)
//      //item.superUser(item.superUser)
//      operation match {
//        case LoGeneralCRUD.INSERT => AcsUser.cacheAndDbInsert(item)
//        case LoGeneralCRUD.UPDATE => AcsUser.cacheAndDbUpdate(item)
//      }
//
//      //val lgc = new LoGeneralCRUD(operation,finaluser.id, finaluser)
//      //FacadeTBilling.getInstance().bang(TrestoConstants.USER_CACHEDB_LOGENERALCRUDE,lgc)
//
//      //log.debug("submit: " + item)
//      S.redirectTo(link2return, () => selectedFilterData(Full(fl)))
//    }
//
//
//    val timeoutList = List(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20, 25, 30, 35, 40, 45, 50, 55).map(a => (a.toString, a.toString))
//    //<td>{ if ((isHyperOrSp == true) || (isHyperOrSp == false && email.matches(BillingUtil.digitsOnlyRegex) == false)) SHtml.text(email, a => email = a) else Text(email) } </td>
//    //<div class="form-group"><label>{ Lang.g("user.report") }</label>{ BHtml.select(BillingConstants.reportsList, Full(item.report.get.toString), a => item.report(a.toInt)) }</div>
//
//    <form method="post" action={ S.uri } role="form">
//      <div class="row">
//        <div class="col-lg-6 col-md-8">
//          <div class="panel panel-info">
//            <div class="panel-heading">{ Lang.g("user.setting.basic") }</div>
//            <div class="panel-body">
//              {
//                if (userType == MapUser.USERTYPE_XYTONVISOR)
//                  <div class="form-group"><label>{ Lang.g("user.company") }</label>{ SHtml.select(companyList, Some(item.superCompanyId.get.toString), a => item.superCompanyId(a.toLong)) % ("class" -> "form-control") }</div>
//                  <div class="form-group"><label>{ Lang.g("user.apps") }</label>{ BHtml.text(item.apps.get, item.apps(_)) }</div>
//              }
//              {
//                if (userType == MapUser.USERTYPE_XYTONVISOR || userType == MapUser.USERTYPE_STAFF) {
//                  { if (operation == LoGeneralCRUD.UPDATE) <div class="form-group"><label>{ Lang.g("user.id") }</label><p class="form-control-static">{ Text(item.id.get.toString) }</p></div> }
//                  <div class="form-group"><label>{ Lang.g("user.numbertype") }</label>{ BHtml.select(BillingConstants.numberTypesList, Some(item.sipNumberType.get), item.sipNumberType(_)) }</div>
//                  <div class="form-group"><label>{ Lang.g("user.numberstatus") }</label>{ BHtml.select(BillingConstants.numberStatusList, Some(item.sipNumberStatus.get), item.sipNumberStatus(_)) }</div>
//                }
//              }
//              <div class="form-group"><label>{ Lang.g("user.email") }</label>{ if (isHyperOrSp == true || item.sipNumberType.get == MapUser.NUMBERTYPE_EMAIL) BHtml.text(email, a => email = a) else <p class="form-control-static">{ email }</p> } </div>
//              { passwordGuiHelper.gui1 }
//              { passwordGuiHelper.gui2 }
//              <div class="form-group"><label>{ Lang.g("user.firstname") }</label>{ BHtml.text(item.firstName.get, item.firstName(_)) }</div>
//              <div class="form-group"><label>{ Lang.g("user.lastname") }</label>{ BHtml.text(item.lastName.get, item.lastName(_)) }</div>
//              <div class="form-group"><label>{ SHtml.checkbox(item.canLoginWeb.get, item.canLoginWeb(_)) } { HtmlUtils.space2 } { Lang.g("user.can.login.to.web") }</label></div>
//              <div class="form-group"><label>{ Lang.g("user.locale") }</label>{ SHtml.select(BillingConstants.getLanguages(), Full(item.locale.get), item.locale(_)) % ("class" -> "form-control") }</div>
//              <div class="form-group"><label>{ Lang.g("user.timezone") }</label>{ SHtml.select(MappedTimeZone.timeZoneList, Full(item.timezone.get), item.timezone(_)) % ("class" -> "form-control") }</div>
//              {
//                if (userType == MapUser.USERTYPE_XYTONVISOR || userType == MapUser.USERTYPE_STAFF) {
//
//                  <div class="form-group">
//                    <label>{ Lang.g("user.sip.password") }</label>
//                    { SHtml.ajaxText(sippassword, x => checkSipPassword(x)) % ("type" -> "password") % ("class" -> "form-control") % new UnprefixedAttribute("value", Text(sippassword), Null) }
//                    <span style="vertical-align:middle; padding-left: 20px; color:red; font-weight:bold;">{ WiringUI.asText(<span/>, sippassword_strength, JqWiringSupport.fade) }</span>
//                  </div>
//                  <div class="form-group"><label>{ Lang.g("user.sip.forward.type") }</label>{ SHtml.ajaxSelect(forwardTypesList, Full(item.sipForwardType.get), a => updateForwardType(a)) % ("class" -> "form-control") }</div>
//                  <div class="form-group" id="forwardType">{ getForwardValue(item.sipForwardType.get) }</div>
//                  <div class="form-group"><label>{ Lang.g("user.sip.caller.number") }</label>{ SHtml.select(userDropdown, Option(item.sipCallerNumber.get), a => item.sipCallerNumber(StringUtils.phoneNumberUncrapper(a))) % ("class" -> "form-control") }</div>
//                  <div class="form-group"><label>{ SHtml.checkbox(item.sipAnonymous.get, item.sipAnonymous(_)) } { Lang.g("user.sip.anonymous") }</label></div>
//                  <div class="form-group">
//                    <label>{ Lang.g("user.registration.timeout") }</label>
//                    { BHtml.text(item.sipRegTimoutCron.get, item.sipRegTimoutCron(_)) % new UnprefixedAttribute("placeholder", Lang.g("user.registration.timeout.ex"), Null) }
//                    <p class="help-block">{ Lang.g("user.registration.timeout.exs") }</p>
//                  </div>
//                  <div class="form-group"><label>{ Lang.g("user.skills") }</label>{ BHtml.text(item.skills.get, item.skills(_)) }</div>
//                  <div class="form-group"><label>{ Lang.g("user.api.key") }</label>{ item.apiKey.get }</div>
//                  <div class="form-group"><label>{ Lang.g("user.usertype") }</label>{ SHtml.select(List(MapUser.USERTYPE_AGENT, MapUser.USERTYPE_VOIP, MapUser.USERTYPE_STAFF).map(a => (a.toString, Lang.gs("user.type." + a))), Full(item.userType.get.toString), a => item.userType(a.toInt)) % ("class" -> "form-control") } </div>
//                  <div class="form-group"><label>{ SHtml.checkbox(item.isEmailPublic.get, a => item.isEmailPublic(a)) } { Lang.g("user.isemailpublic") }</label></div>
//                  <div class="form-group"><label>{ Lang.g("user.sipusername") }</label>{ BHtml.text(item.sipUsername.get, item.sipUsername(_)) } </div>
//                  <div class="form-group"><label>{ Lang.g("user.sip.short.number") }</label>{ BHtml.text(item.sipShortNumber.get, a => item.sipShortNumber(StringUtils.phoneNumberUncrapper(a))) }</div>
//                  <div class="form-group"><label>{ Lang.g("user.force.codec") }</label>{ SHtml.select(BillingConstants.forcedCodecsList, Full(item.sipForceCodec.get), item.sipForceCodec(_)) % ("class" -> "form-control") } </div>
//                  <div class="form-group"><label>{ Lang.g("user.rtp.type") }</label>{ SHtml.select(org.tresto.tswitch.sip.RTP_TYPE.getList().map(a => (a, Lang.gs("sip.rtp.type." + a))), Full(item.sipRtpType.get), item.sipRtpType(_)) % ("class" -> "form-control") } </div>
//                  <div class="form-group"><label>{ Lang.g("user.comments") }</label>{ SHtml.textarea(item.comments.get, item.comments(_)) % ("class" -> "form-control") % ("style" -> "height: 200px;") }</div>
//                }
//              }
//              <div class="form-group"><label>{ SHtml.link(link2return, () => (), Lang.cancel) } </label>{ HtmlUtils.space2 } { BHtml.button(Lang.saves, submit) }</div>
//            </div>
//          </div>
//        </div>
//      </div>
//    </form>
//  }
//
//  def addRange(): NodeSeq = {
//    val userType = MapUser.curUserType.get.open_!
//    val curUser = AcsUser.cacheGetById(MapUser.curUserId.is.open_!).get
//    val item = MapUser.create.superCompanyId(curUser.superCompanyId.get).validated(true).uniqueId(Helpers.randomString(32)).locale(curUser.locale).timezone(curUser.timezone).userType(userType)
//    var password = item.password.get
//    var sippassword = item.sipPassword.get
//    var email = item.email.get
//    val password_strength = ValueCell("")
//    val sippassword_strength = ValueCell("")
//    val phoneNumberCount = ValueCell("")
//    val link2return = S.referer.openOr(WebLinks.USERSTAFF.getIndex)
//    var phoneFrom = ""
//    var phoneUntil = ""
//
//    def checkPassword(pwd: String) {
//      if (pwd.length() <= 4) {
//        password_strength.set(Lang.gs("user.password.too.short"))
//      } else {
//        val res = PasswordCheck.CheckPasswordStrength(pwd, item.locale.get)
//        password = pwd
//        password_strength.set(res.verdict)
//      }
//      Noop
//    }
//
//    def checkSipPassword(pwd: String) {
//      if (pwd.length() <= 4) {
//        sippassword_strength.set(Lang.gs("user.sip.password.too.short"))
//      } else {
//        val res = PasswordCheck.CheckPasswordStrength(pwd, item.locale.get)
//        log.debug("checkSipPassword: " + pwd + " -> " + res.toString)
//        sippassword_strength.set(res.verdict)
//      }
//      sippassword = pwd
//      Noop
//    }
//
//    def submit() = { //Save the data
//      if (password.length <= 4) {
//        notice(Lang.gs("user.password.tooshort"))
//        S.redirectTo(link2return)
//      }
//      if (phoneFrom.length() == 0 || phoneUntil.length() == 0) {
//        notice(Lang.gs("user.phone.length.is.short"))
//        S.redirectTo(link2return)
//      }
//      if (phoneFrom.length() != phoneUntil.length()) {
//        notice(Lang.gs("user.phone.length.is.different"))
//        S.redirectTo(link2return)
//      }
//      if (phoneFrom.matches(BillingUtil.digitsOnlyRegex) == false || phoneUntil.matches(BillingUtil.digitsOnlyRegex) == false) {
//        notice(Lang.gs("user.not.phone.number"))
//        S.redirectTo(link2return)
//      }
//      item.email(phoneFrom)
//      item.password(password)
//      item.sipPassword(sippassword)
//      item.sipShortNumber(phoneUntil) //REMOVE THIS in the importer
//      val lid = Helpers.nextFuncName
//      AcsAnyStore.put(lid, item)
//      S.redirectTo(WebLinks.USERSTAFF.getUserCreator + "?" + WebLinks.pANYID + "=" + lid) //forward to the add snippet below
//    }
//
//    def calcRange(): JsCmd = {
//      if (phoneFrom.length() != phoneUntil.length()) {
//        JsCmds.SetHtml("rangeinfo", Lang.g("user.phone.length.is.different"))
//      } else {
//        if (phoneFrom.matches(BillingUtil.digitsOnlyRegex) == false || phoneUntil.matches(BillingUtil.digitsOnlyRegex) == false) {
//          JsCmds.SetHtml("rangeinfo", Lang.g("user.phones.only.numbers"))
//        } else {
//          val len = (phoneFrom.toLong to phoneUntil.toLong).toList.length
//          JsCmds.SetHtml("rangeinfo", Lang.g("user.phones.count", Lang.getLocale(), List(len.toString())))
//        }
//      }
//    }
//
//    <form action={ S.uri } method="post">
//      <table>
//        {
//          if (userType == MapUser.USERTYPE_XYTONVISOR)
//            <tr>
//              <td>{ Lang.g("user.company") }</td>
//              <td>{ SHtml.select(List(("0", Lang.gs("gen.all"))) ++ AcsCompany.asList(AcsCompany.cacheGetAll(true)), Some(item.superCompanyId.get.toString), a => item.superCompanyId(a.toLong)) }</td>
//            </tr>
//        }
//        <tr>
//          <td>{ Lang.g("user.phone.from") } - { Lang.g("user.phone.until") }</td>
//          <td>
//            { SHtml.ajaxText(phoneFrom, a => { phoneFrom = a.trim(); calcRange() }) }
//            -{ SHtml.ajaxText(phoneUntil, a => { phoneUntil = a.trim(); calcRange() }) }
//            <span id="rangeinfo" style="vertical-align:middle; padding-left: 20px; font-weight:bold;"/>
//          </td>
//        </tr>
//        <tr><td>{ Lang.g("user.numbertype") }</td><td>{ BHtml.select(BillingConstants.numberTypesList, Some(item.sipNumberType.get), item.sipNumberType(_)) }</td></tr>
//        <tr><td>{ Lang.g("user.numberstatus") }</td><td>{ BHtml.select(BillingConstants.numberStatusList, Some(item.sipNumberStatus.get), item.sipNumberStatus(_)) }</td></tr>
//        <tr>
//          <td>{ Lang.g("user.firstname") }</td>
//          <td>{ SHtml.text(item.firstName.get, item.firstName(_)) }</td>
//        </tr>
//        <tr>
//          <td>{ Lang.g("user.lastname") }</td>
//          <td>{ SHtml.text(item.lastName.get, item.lastName(_)) }</td>
//        </tr>
//        <tr>
//          <td>{ Lang.g("user.password") }</td>
//          <td>
//            { SHtml.ajaxText(password, x => checkPassword(x)) % ("type" -> "password") % new UnprefixedAttribute("value", Text(password), Null) }
//            <span style="vertical-align:middle; padding-left: 20px; color:red; font-weight:bold;">{ WiringUI.asText(<span/>, password_strength, JqWiringSupport.fade) }</span>
//          </td>
//        </tr>
//        <tr>
//          <td>{ Lang.g("user.sip.password") }</td>
//          <td>
//            { SHtml.ajaxText(sippassword, x => checkSipPassword(x)) % ("type" -> "password") % new UnprefixedAttribute("value", Text(sippassword), Null) }
//            <span style="vertical-align:middle; padding-left: 20px; color:red; font-weight:bold;">{ WiringUI.asText(<span/>, sippassword_strength, JqWiringSupport.fade) }</span>
//          </td>
//        </tr>
//        <tr>
//          <td>{ Lang.g("user.is.enabled") }</td>
//          <td>{ SHtml.checkbox(!item.isDeleted.get, a => item.isDeleted(!a)) }</td>
//        </tr>
//        <tr>
//          <td>{ Lang.g("user.isemailpublic") }</td>
//          <td>{ SHtml.checkbox(item.isEmailPublic.get, a => item.isEmailPublic(a)) }</td>
//        </tr>
//        <tr>
//          <td>{ Lang.g("user.can.login.to.web") }</td>
//          <td>{ SHtml.checkbox(item.canLoginWeb.get, item.canLoginWeb(_)) }</td>
//        </tr>
//        <tr>
//          <td>{ Lang.g("user.locale") }</td>
//          <td>{ SHtml.select(BillingConstants.getLanguages(), Full(item.locale.get), item.locale(_)) }</td>
//        </tr>
//        <tr>
//          <td>{ Lang.g("user.timezone") }</td>
//          <td>{ SHtml.select(MappedTimeZone.timeZoneList, Full(item.timezone.get), item.timezone(_)) }</td>
//        </tr>
//        <tr>
//          <td>{ Lang.g("user.comments") }</td>
//          <td>{ SHtml.textarea(item.comments.get, item.comments(_)) % ("style" -> "width:500px; height: 150px;") }</td>
//        </tr>
//        <tr>
//          <td>{ SHtml.link(link2return, () => (), Lang.cancel) }</td>
//          <td>{ SHtml.submit(Lang.gs("user.create"), submit) }</td>
//        </tr>
//      </table>
//    </form>
//  }
//
//
//  def filter(viewType: String): NodeSeq = {
//    val fl = selectedFilterData.is.open_! //We know this exists as we created it at the beginning of this trait
//    val companyList = List(("0", Lang.gs("gen.all"))) ++ AcsCompany.asList(AcsCompany.cacheGetAll(true))
//    val userType = MapUser.curUserType.get.open_!
//    val backLink = viewType match {
//      case VIEW_STAFF => WebLinks.USERSTAFF.getIndex
//      case VIEW_CUSTOMERS => WebLinks.USERSTAFF.getCustomers
//    }
//    ////        <div class="form-group">
//    //          { Lang.g("user.anyfield") }</td>
//    ////          { if (userType == MapUser.USERTYPE_XYTONVISOR) <td>{ Lang.g("user.partnercompany") }</td> }
//    ////          <td>{ Lang.g("user.isdeleted") }</td>
//    ////          <td></td>
//    ////        </div>
//
//    def doFilter() {
//      log.debug("doFilter: " + fl.toString + " viewType: " + viewType)
//      S.redirectTo(backLink, () => selectedFilterData(Full(fl))) //redirect to this very same snippet so the view will update
//    }
//
//    <form action={ S.uri } method="post" role="form" class="form-inline">
//      <div class="form-group">{ SHtml.text(fl.anyField.getOrElse(""), a => fl.anyField = if (a.length == 0) Empty else Full(a)) % new UnprefixedAttribute("placeholder", Lang.g("user.anyfield"), Null) }        </div>
//      { if (userType == MapUser.USERTYPE_XYTONVISOR) <div class="form-group">{ SHtml.select(companyList, fl.superCompanyId.map(_.toString), a => fl.superCompanyId = if (a == "0") Empty else Full(a.toLong)) }</div> }
//      <div class="form-group"><span>{ Lang.g("user.isdeleted") } </span>{ SHtml.checkbox(fl.isDeleted, fl.isDeleted = _) }</div>
//      <div class="form-group">{ FocusOnLoad(SHtml.submit(Lang.gs("user.filter"), doFilter)) }</div>
//    </form>
//  }
//
//}

