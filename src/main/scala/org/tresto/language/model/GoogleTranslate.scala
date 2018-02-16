package org.tresto.language.model

import java.net.URL
import java.net.URLEncoder
import org.tresto.traits.LogHelper
import net.liftweb.json.DefaultFormats
//import net.liftweb.json.JsonParser._
import java.net.HttpURLConnection
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Translate from one language to another language via the google translate API
 * Google API docs: https://developers.google.com/translate/v2/using_rest
 * Lift JSON docs: https://www.assembla.com/spaces/liftweb/wiki/JSON_Support
 *
 */
case class GoogleTranslate(apiKey: String) extends LogHelper {
  import GoogleLanguage._
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  val URL_TEMPLATE = "https://www.googleapis.com/language/translate/v2?key=%s&source=%s&target=%s&q=%s";

  def translate(text: String, from: GoogleLanguage, to: GoogleLanguage) {
    val populatedTemplate = String.format(URL_TEMPLATE, apiKey, from, to, URLEncoder.encode(text, "UTF-8"));

    val json = httpGet(populatedTemplate)
    log.trace("json response: " + json)
    val jres = net.liftweb.json.JsonParser.parse(json)
    log.trace("json parsed: " + jres.extract[GoogleData])
    //val json = retrieveJSON(url);
  }

  
  /**
   * Http Get message to the given url
   * @param url the url of the http GET request
   * @return the response as a string from the url
   */
  def httpGet(url: String): String = {
    val apiurl = new URL(url);
    log.trace("Billing api GET call: " + apiurl)
    val connection = apiurl.openConnection.asInstanceOf[HttpURLConnection];
    connection.setRequestMethod("GET");
    connection.connect();
    val rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    val sb = new StringBuilder();

    var line = rd.readLine()
    while (line != null) {
      sb.append(line + '\n');
      line = rd.readLine()
    }
    connection.disconnect();
    return sb.toString
  }   
}
case class GoogleTranslatedText(translatedText:String)
case class GoogleTranslations(translations:Array[GoogleTranslatedText])
case class GoogleData(data:GoogleTranslations)

object GoogleLanguage extends Enumeration {
  type GoogleLanguage = Value
  val AFRIKAANS = Value("af")
  val ALBANIAN = Value("sq")
  val AMHARIC = Value("am")
  val ARABIC = Value("ar")
  val ARMENIAN = Value("hy")
  val AZERBAIJANI = Value("az")
  val BASQUE = Value("eu")
  val BELARUSIAN = Value("be")
  val BENGALI = Value("bn")
  val BIHARI = Value("bh")
  val BULGARIAN = Value("bg")
  val BURMESE = Value("my")
  val CATALAN = Value("ca")
  val CHEROKEE = Value("chr")
  val CHINESE = Value("zh")
  val CHINESE_SIMPLIFIED = Value("zh-CN")
  val CHINESE_TRADITIONAL = Value("zh-TW")
  val CROATIAN = Value("hr")
  val CZECH = Value("cs")
  val DANISH = Value("da")
  val DHIVEHI = Value("dv")
  val DUTCH = Value("nl")
  val ENGLISH = Value("en")
  val ESPERANTO = Value("eo")
  val ESTONIAN = Value("et")
  val FILIPINO = Value("tl")
  val FINNISH = Value("fi")
  val FRENCH = Value("fr")
  val GALICIAN = Value("gl")
  val GEORGIAN = Value("ka")
  val GERMAN = Value("de")
  val GREEK = Value("el")
  val GUARANI = Value("gn")
  val GUJARATI = Value("gu")
  val HEBREW = Value("iw")
  val HINDI = Value("hi")
  val HUNGARIAN = Value("hu")
  val ICELANDIC = Value("is")
  val INDONESIAN = Value("id")
  val INUKTITUT = Value("iu")
  val IRISH = Value("ga")
  val ITALIAN = Value("it")
  val JAPANESE = Value("ja")
  val KANNADA = Value("kn")
  val KAZAKH = Value("kk")
  val KHMER = Value("km")
  val KOREAN = Value("ko")
  val KURDISH = Value("ku")
  val KYRGYZ = Value("ky")
  val LAOTHIAN = Value("lo")
  val LATVIAN = Value("lv")
  val LITHUANIAN = Value("lt")
  val MACEDONIAN = Value("mk")
  val MALAY = Value("ms")
  val MALAYALAM = Value("ml")
  val MALTESE = Value("mt")
  val MARATHI = Value("mr")
  val MONGOLIAN = Value("mn")
  val NEPALI = Value("ne")
  val NORWEGIAN = Value("no")
  val ORIYA = Value("or")
  val PASHTO = Value("ps")
  val PERSIAN = Value("fa")
  val POLISH = Value("pl")
  val PORTUGUESE = Value("pt")
  val PUNJABI = Value("pa")
  val ROMANIAN = Value("ro")
  val RUSSIAN = Value("ru")
  val SANSKRIT = Value("sa")
  val SERBIAN = Value("sr")
  val SINDHI = Value("sd")
  val SINHALESE = Value("si")
  val SLOVAK = Value("sk")
  val SLOVENIAN = Value("sl")
  val SPANISH = Value("es")
  val SWAHILI = Value("sw")
  val SWEDISH = Value("sv")
  val TAJIK = Value("tg")
  val TAMIL = Value("ta")
  val TAGALOG = Value("tl")
  val TELUGU = Value("te")
  val THAI = Value("th")
  val TIBETAN = Value("bo")
  val TURKISH = Value("tr")
  val UKRANIAN = Value("uk")
  val URDU = Value("ur")
  val UZBEK = Value("uz")
  val UIGHUR = Value("ug")
  val VIETNAMESE = Value("vi")
  val WELSH = Value("cy")
  val YIDDISH = Value("yi");
}