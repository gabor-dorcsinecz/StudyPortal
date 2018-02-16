package org.tresto.utils

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

object FormatUtils {

  val sanePeoplesFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val sanePeoplesMillisec = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
  val sanePeoples2Minute = new SimpleDateFormat("yyyy-MM-dd HH:mm")
  val sanePeoples2Day = new SimpleDateFormat("yyyy-MM-dd")
  val sanePeoples2Month = new SimpleDateFormat("yyyy-MM")

  val postgreDBFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val reportDateFormat = new SimpleDateFormat("yyyyMMdd")

  val fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss")

  val saneMoneyFormat = new DecimalFormat("###,##0.00")
  val integerFormat = new DecimalFormat("0")
  val apiNumberFormat = new DecimalFormat("#####0.####")

  val crapRemoverRegex = "[^\\p{L}\\p{N}]" //Leave only characters and numbers, no special characters 

  def booleanToInt(in: Boolean): Int = {
    in match {
      case false => 0;
      case true => 1;
    }
  }
  def IntToBoolean(in: Int): Boolean = {
    in match {
      case 1 => true;
      case 0 => false;
    }
  }

}