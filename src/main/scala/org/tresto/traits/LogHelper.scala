package org.tresto.traits

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File
import org.tresto.utils.TrestoLogger
import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.MDC

trait LogHelper {

  //val log = TrestoLogger.getLogger(getClass,logContext)
  //val log = new MyLog(getClass)
  def log(): Logger = {
    LoggerFactory.getLogger(getClass);
    //return Logger.getLogger(getClass);
  }

  def mdcdebug(mdcKey:String,mdcValue: String, msg: String) {
    MDC.put(mdcKey, mdcValue)
    log.debug(msg)
    MDC.remove(mdcKey)
  }

  def mdcwarn(mdcKey:String,mdcValue: String, msg: String) {
    MDC.put(mdcKey, mdcValue)
    log.warn(msg)
    MDC.remove(mdcKey)
  }

  def mdcerror(mdcKey:String,mdcValue: String, msg: String) {
    MDC.put(mdcKey, mdcValue)
    log.debug(msg)
    MDC.remove(mdcKey)
  }
  //  /**
  //   * Set the log context by the context name which we get at application start
  //   * The input is like:  C:\programs\Red5_0.8.0\webapps\pictureserver
  //   */
  //  def setLogContextByPath(p_context:String) {
  //    ""
  //  }

  /**
   * This will return the full stack trace of an exception
   * @param e The exception we want to turn to string
   * @return
   */
  def stack2string(e: Exception): String = {
    try {
      val sw = new StringWriter();
      val pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      //return "--Catched--ERROR--Exception--\r\n" + sw.toString();
      return sw.toString()
    } catch {
      case e2: Exception =>
        return "bad stack2string";
    }
  }

}


//class MDCLogger(mdcKey:String) extends Logger {
//  def debugMDC(mdcValue:String,msg:String) {
//    MDC.put(mdcKey, mdcValue)
//    debug(msg)
//    MDC.remove(mdcKey)
//  } 
//  
//  def warnMDC(mdcValue:String,msg:String) {
//    MDC.put(mdcKey, mdcValue)
//    warn(msg)
//    MDC.remove(mdcKey)
//  } 
//
//    def errorMDC(mdcValue:String,msg:String) {
//    MDC.put(mdcKey, mdcValue)
//    error(msg)
//    MDC.remove(mdcKey)
//  } 
//
//}

