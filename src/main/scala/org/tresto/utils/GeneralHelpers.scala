package org.tresto.utils

import java.util.Locale

import code.model.MapUser
import org.tresto.language.snippet.Lang

object BillingConstants {

  def accessRigths = List(MapUser.USERTYPE_STUDENT, MapUser.USERTYPE_STAFF, MapUser.USERTYPE_SITEVISOR)
  def accessRightsList = accessRigths.map(a => (a.toString, Lang.gs("user.rights." + a)))
  def accessRightsMap = accessRigths.map(a => (a.toLong, Lang.gs("user.rights." + a))).toMap

  def getLanguages(): List[(String, String)] = {
    Locale.getAvailableLocales.toList.filter(a => a.toString == "en").sortWith((a, b) => a.getDisplayName < b.getDisplayName).map(lo => (lo.toString, lo.getDisplayName))
  }

  val countriesMap = Map(1 -> "United States", 2 -> "Afghanistan", 3 -> "Albania", 4 -> "Algeria", 5 -> "Andorra", 6 -> "Angola", 7 -> "Antigua and Barbuda", 8 -> "Argentina",
    9 -> "Armenia", 10 -> "Australia", 11 -> "Austria", 12 -> "Azerbaijan", 13 -> "Bahamas, The", 14 -> "Bahrain", 15 -> "Bangladesh", 16 -> "Barbados", 17 -> "Belarus", 18 -> "Belgium",
    19 -> "Belize", 20 -> "Benin", 21 -> "Bhutan", 22 -> "Bolivia", 23 -> "Bosnia and Herzegovina", 24 -> "Botswana", 25 -> "Brazil", 26 -> "Brunei", 27 -> "Bulgaria", 28 -> "Burkina Faso",
    29 -> "Burundi", 30 -> "Cambodia", 31 -> "Cameroon", 32 -> "Canada", 33 -> "Cape Verde", 34 -> "Central African Republic", 35 -> "Chad", 36 -> "Chile", 37 -> "China, People's Republic of",
    38 -> "Colombia", 39 -> "Comoros", 40 -> "Congo, Democratic Republic of the (Congo Kinshasa)", 41 -> "Congo, Republic of the (Congo Brazzaville)", 42 -> "Costa Rica",
    43 -> "Cote d'Ivoire (Ivory Coast)", 44 -> "Croatia", 45 -> "Cuba", 46 -> "Cyprus", 47 -> "Czech Republic", 48 -> "Denmark", 49 -> "Djibouti", 50 -> "Dominica", 51 -> "Dominican Republic",
    52 -> "Ecuador", 53 -> "Egypt", 54 -> "El Salvador", 55 -> "Equatorial Guinea", 56 -> "Eritrea", 57 -> "Estonia", 58 -> "Ethiopia", 59 -> "Fiji", 60 -> "Finland", 61 -> "France",
    62 -> "Gabon", 63 -> "Gambia, The", 64 -> "Georgia", 65 -> "Germany", 66 -> "Ghana", 67 -> "Greece", 68 -> "Grenada", 69 -> "Guatemala", 70 -> "Guinea", 71 -> "Guinea-Bissau",
    72 -> "Guyana", 73 -> "Haiti", 74 -> "Honduras", 75 -> "Hungary", 76 -> "Iceland", 77 -> "India", 78 -> "Indonesia", 79 -> "Iran", 80 -> "Iraq", 81 -> "Ireland", 82 -> "Israel",
    83 -> "Italy", 84 -> "Jamaica", 85 -> "Japan", 86 -> "Jordan", 87 -> "Kazakhstan", 88 -> "Kenya", 89 -> "Kiribati", 90 -> "Korea, Democratic People's Republic of (North Korea)",
    91 -> "Korea, Republic of  (South Korea)", 92 -> "Kuwait", 93 -> "Kyrgyzstan", 94 -> "Laos", 95 -> "Latvia", 96 -> "Lebanon", 97 -> "Lesotho", 98 -> "Liberia", 99 -> "Libya",
    100 -> "Liechtenstein", 101 -> "Lithuania", 102 -> "Luxembourg", 103 -> "Macedonia", 104 -> "Madagascar", 105 -> "Malawi", 106 -> "Malaysia", 107 -> "Maldives", 108 -> "Mali",
    109 -> "Malta", 110 -> "Marshall Islands", 111 -> "Mauritania", 112 -> "Mauritius", 113 -> "Mexico", 114 -> "Micronesia", 115 -> "Moldova", 116 -> "Monaco", 117 -> "Mongolia",
    118 -> "Montenegro", 119 -> "Morocco", 120 -> "Mozambique", 121 -> "Myanmar (Burma)", 122 -> "Namibia", 123 -> "Nauru", 124 -> "Nepal", 125 -> "Netherlands", 126 -> "New Zealand",
    127 -> "Nicaragua", 128 -> "Niger", 129 -> "Nigeria", 130 -> "Norway", 131 -> "Oman", 132 -> "Pakistan", 133 -> "Palau", 134 -> "Panama", 135 -> "Papua New Guinea", 136 -> "Paraguay",
    137 -> "Peru", 138 -> "Philippines", 139 -> "Poland", 140 -> "Portugal", 141 -> "Qatar", 142 -> "Romania", 143 -> "Russia", 144 -> "Rwanda", 145 -> "Saint Kitts and Nevis",
    146 -> "Saint Lucia", 147 -> "Saint Vincent and the Grenadines", 148 -> "Samoa", 149 -> "San Marino", 150 -> "Sao Tome and Principe", 151 -> "Saudi Arabia", 152 -> "Senegal",
    153 -> "Serbia", 154 -> "Seychelles", 155 -> "Sierra Leone", 156 -> "Singapore", 157 -> "Slovakia", 158 -> "Slovenia", 159 -> "Solomon Islands", 160 -> "Somalia", 161 -> "South Africa",
    162 -> "Spain", 163 -> "Sri Lanka", 164 -> "Sudan", 165 -> "Suriname", 166 -> "Swaziland", 167 -> "Sweden", 168 -> "Switzerland", 169 -> "Syria", 170 -> "Tajikistan", 171 -> "Tanzania",
    172 -> "Thailand", 173 -> "Timor-Leste (East Timor)", 174 -> "Togo", 175 -> "Tonga", 176 -> "Trinidad and Tobago", 177 -> "Tunisia", 178 -> "Turkey", 179 -> "Turkmenistan", 180 -> "Tuvalu",
    181 -> "Uganda", 182 -> "Ukraine", 183 -> "United Arab Emirates", 184 -> "United Kingdom", 185 -> "Uruguay", 186 -> "Uzbekistan", 187 -> "Vanuatu", 188 -> "Vatican City", 189 -> "Venezuela",
    190 -> "Vietnam", 191 -> "Yemen", 192 -> "Zambia", 193 -> "Zimbabwe", 194 -> "Abkhazia", 195 -> "China, Republic of (Taiwan)", 196 -> "Nagorno-Karabakh", 197 -> "Northern Cyprus",
    198 -> "Pridnestrovie (Transnistria)", 199 -> "Somaliland", 200 -> "South Ossetia", 201 -> "Ashmore and Cartier Islands", 202 -> "Christmas Island", 203 -> "Cocos (Keeling) Islands",
    204 -> "Coral Sea Islands", 205 -> "Heard Island and McDonald Islands", 206 -> "Norfolk Island", 207 -> "New Caledonia", 208 -> "French Polynesia", 209 -> "Mayotte",
    210 -> "Saint Barthelemy", 211 -> "Saint Martin", 212 -> "Saint Pierre and Miquelon", 213 -> "Wallis and Futuna", 214 -> "French Southern and Antarctic Lands", 215 -> "Clipperton Island",
    216 -> "Bouvet Island", 217 -> "Cook Islands", 218 -> "Niue", 219 -> "Tokelau", 220 -> "Guernsey", 221 -> "Isle of Man", 222 -> "Jersey", 223 -> "Anguilla", 224 -> "Bermuda",
    225 -> "British Indian Ocean Territory", 226 -> "British Sovereign Base Areas", 227 -> "British Virgin Islands", 228 -> "Cayman Islands", 229 -> "Falkland Islands (Islas Malvinas)",
    230 -> "Gibraltar", 231 -> "Montserrat", 232 -> "Pitcairn Islands", 233 -> "Saint Helena", 234 -> "South Georgia and the South Sandwich Islands", 235 -> "Turks and Caicos Islands",
    236 -> "Northern Mariana Islands", 237 -> "Puerto Rico", 238 -> "American Samoa", 239 -> "Baker Island", 240 -> "Guam", 241 -> "Howland Island", 242 -> "Jarvis Island",
    243 -> "Johnston Atoll", 244 -> "Kingman Reef", 245 -> "Midway Islands", 246 -> "Navassa Island", 247 -> "Palmyra Atoll", 248 -> "U.S. Virgin Islands", 249 -> "Wake Island",
    250 -> "Hong Kong", 251 -> "Macau", 252 -> "Faroe Islands", 253 -> "Greenland", 254 -> "French Guiana", 255 -> "Guadeloupe", 256 -> "Martinique", 257 -> "Reunion", 258 -> "Aland",
    259 -> "Aruba", 260 -> "Netherlands Antilles", 261 -> "Svalbard", 262 -> "Ascension", 263 -> "Tristan da Cunha", 264 -> "Antarctica", 265 -> "Kosovo",
    266 -> "Palestinian Territories (Gaza Strip and West Bank)", 267 -> "Western Sahara", 268 -> "Australian Antarctic Territory", 269 -> "Ross Dependency", 270 -> "Peter I Island",
    271 -> "Queen Maud Land", 272 -> "British Antarctic Territory")

  val countriesList = countriesMap.toList.map(a => (a._1.toString, a._2)).sortWith((a, b) => a._2 < b._2)

  //Country code in (net.liftweb.mapper.Countries(code) -> VAT rate)  link (look for telecom) http://ec.europa.eu/taxation_customs/resources/documents/taxation/vat/how_vat_works/rates/vat_rates_en.pdf
  val countryVATMap = Map((18 -> 21), (27 -> 20), (47 -> 21), (48 -> 25), (65 -> 19), (57 -> 20), (67 -> 23), (162 -> 21), (61 -> 20), (44 -> 25), (81 -> 23), (83 -> 22), (46 -> 19),
    (95 -> 21), (101 -> 21), (102 -> 17), (75 -> 27), (109 -> 18), (125 -> 21), (11 -> 20), (139 -> 23), (140 -> 23), (142 -> 24), (158 -> 22), (157 -> 20), (60 -> 24), (167 -> 25), (184 -> 20))
}

