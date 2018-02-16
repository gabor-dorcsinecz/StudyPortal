package org.tresto.utils;


import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author Gábor
 */
public class TrestoLogger {

  /**
     * This will return the full stack trace of an exception
     * @param e The exception we want to turn to string
     * @return
     */
    public static String stack2string(Exception e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return "-----------------\r\n" + sw.toString();
        } catch (Exception e2) {
            return "bad stack2string";
        }
    }
}

