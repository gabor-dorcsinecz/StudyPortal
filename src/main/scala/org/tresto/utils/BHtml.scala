package org.tresto.utils

import scala.xml._
import net.liftweb.http._
import net.liftweb.http.SHtml._
import net.liftweb.common._

/**
 * Extends SHtml with Bootstrap styles
 */
object BHtml {

  val formControlAtt = new BasicElemAttr("class", "form-control")
  def placeHolderAtt(name:NodeSeq) = new UnprefixedAttribute("placeholder", name, Null)

  def textStatic(value: String): Elem = {
    <p class="form-control-static">{ value }</p>
  }

  def textDisabled(value: String, func: String => Any, attrs: ElemAttr*): Elem = {
    val attrs2 = formControlAtt :: new BasicElemAttr("disabled", "disabled") :: attrs.toList
    SHtml.text(value, func, attrs2: _*)
  }

  def text(value: String, func: String => Any, attrs: ElemAttr*): Elem = {
    val attrs2 = formControlAtt :: attrs.toList
    SHtml.text(value, func, attrs2: _*)
  }

  //A text edit box with a label
  def formText(label: NodeSeq, value: String, func: String => Any, attrs: ElemAttr*): Elem = {
    formText(label, label.toString, value, func, attrs: _*)
  }
  /**
   * A vertical form part, with a label, and an editable text box with a placeholder
   */
  def formText(label: NodeSeq, placeholder: String, value: String, func: String => Any, attrs: ElemAttr*): Elem = {
    <div class="form-group"><label class="control-label">{ label }</label>{ text(value, func, attrs: _*) % new UnprefixedAttribute("placeholder", Text(placeholder), Null) }</div>
  }

  /**
   * A horizontal form part, with a label and an editable text box with a placeholder. The horizontal layout will degrade to vertical layout on sm and xs device sizes (col-sm-2 and col-sm-10) 
   */
  def formTextHorizontal(label: NodeSeq, placeholder: String, value: String, func: String => Any, attrs: ElemAttr*): Elem = {
    <div class="form-group">
      <label class="control-label col-sm-3">{ label }</label>
      <div class="col-sm-9">{ text(value, func, attrs: _*) % new UnprefixedAttribute("placeholder", Text(placeholder), Null) }</div>
    </div>
  }

  /**
   * Create a text area which you can add to a horizontal form
   */
  def formTextAreaHorizontal(label: NodeSeq, placeholder: String, value: String, func: String => Any, rows:Int, attrs: ElemAttr*): Elem = {
    val allAttrs = formControlAtt :: new BasicElemAttr("rows", rows.toString) :: attrs.toList
    <div class="form-group">
      <label class="control-label col-sm-3">{ label }</label>
      <div class="col-sm-9">{ textarea(value, func, allAttrs: _*) % new UnprefixedAttribute("placeholder", Text(placeholder), Null) }</div>
    </div>
  }

  def button(label: String, func: () => Any, attrs: ElemAttr*): Elem = {
    val styleClass = new BasicElemAttr("class", "btn btn-success")
    val attrs2 = styleClass :: attrs.toList
    SHtml.button(label, func, attrs2: _*)
  }

  def select(opts: Seq[(String, String)], deflt: Box[String], func: (String) => Any, attrs: ElemAttr*): Elem = {
    val attrs2 = formControlAtt :: attrs.toList
    SHtml.select(opts, deflt, func, attrs2: _*)
  }

  //  def formText(label: NodeSeq, placeholder: String, value: String, func: String => Any, attrs: ElemAttr*): Elem = {
  //    <div class="form-group"><label class="control-label">{ label }</label>{ text(value, func, attrs: _*) % new UnprefixedAttribute("placeholder", Text(placeholder), Null) }</div>
  //  }

  def styleTable(title: NodeSeq, table: NodeSeq): NodeSeq = {
    <div class="ibox float-e-margins">
      <div class="ibox-title">
        <h5>{ title }</h5>
        <div class="ibox-tools">
        </div>
      </div>
      <div class="ibox-content">
        { table }
      </div>
    </div>
  }

}

