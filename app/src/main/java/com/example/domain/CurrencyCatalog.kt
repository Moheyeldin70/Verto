package com.example.domain

data class CurrencyInfo(
  val code: String,
  val name: String,
  val symbol: String,
  val flag: String,
  val fallbackRateFromUsd: Double,
)

object CurrencyCatalog {
  val all: List<CurrencyInfo> =
    listOf(
      CurrencyInfo("USD", "US Dollar", "$", "🇺🇸", 1.0),
      CurrencyInfo("EUR", "Euro", "€", "🇪🇺", 0.92),
      CurrencyInfo("GBP", "British Pound", "£", "🇬🇧", 0.79),
      CurrencyInfo("JPY", "Japanese Yen", "¥", "🇯🇵", 154.0),
      CurrencyInfo("CNY", "Chinese Yuan", "¥", "🇨🇳", 7.24),
      CurrencyInfo("SAR", "Saudi Riyal", "﷼", "🇸🇦", 3.75),
      CurrencyInfo("AED", "UAE Dirham", "د.إ", "🇦🇪", 3.6725),
      CurrencyInfo("EGP", "Egyptian Pound", "E£", "🇪🇬", 49.3),
      CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳", 84.4),
      CurrencyInfo("CAD", "Canadian Dollar", "C$", "🇨🇦", 1.40),
      CurrencyInfo("AUD", "Australian Dollar", "A$", "🇦🇺", 1.54),
      CurrencyInfo("CHF", "Swiss Franc", "CHF", "🇨🇭", 0.88),
      CurrencyInfo("KRW", "South Korean Won", "₩", "🇰🇷", 1395.0),
      CurrencyInfo("TRY", "Turkish Lira", "₺", "🇹🇷", 34.4),
      CurrencyInfo("BRL", "Brazilian Real", "R$", "🇧🇷", 5.78),
      CurrencyInfo("MXN", "Mexican Peso", "MX$", "🇲🇽", 20.3),
      CurrencyInfo("SGD", "Singapore Dollar", "S$", "🇸🇬", 1.34),
      CurrencyInfo("HKD", "Hong Kong Dollar", "HK$", "🇭🇰", 7.78),
      CurrencyInfo("NZD", "New Zealand Dollar", "NZ$", "🇳🇿", 1.70),
      CurrencyInfo("SEK", "Swedish Krona", "kr", "🇸🇪", 10.9),
      CurrencyInfo("NOK", "Norwegian Krone", "kr", "🇳🇴", 11.0),
      CurrencyInfo("DKK", "Danish Krone", "kr", "🇩🇰", 6.89),
      CurrencyInfo("PLN", "Polish Zloty", "zł", "🇵🇱", 4.10),
      CurrencyInfo("ZAR", "South African Rand", "R", "🇿🇦", 18.1),
      CurrencyInfo("RUB", "Russian Ruble", "₽", "🇷🇺", 98.5),
      CurrencyInfo("KWD", "Kuwaiti Dinar", "د.ك", "🇰🇼", 0.307),
      CurrencyInfo("BHD", "Bahraini Dinar", "د.ب", "🇧🇭", 0.376),
      CurrencyInfo("OMR", "Omani Rial", "ر.ع.", "🇴🇲", 0.385),
      CurrencyInfo("QAR", "Qatari Riyal", "ر.ق", "🇶🇦", 3.64),
      CurrencyInfo("JOD", "Jordanian Dinar", "د.أ", "🇯🇴", 0.709),
      CurrencyInfo("IQD", "Iraqi Dinar", "ع.د", "🇮🇶", 1310.0),
      CurrencyInfo("LBP", "Lebanese Pound", "ل.ل", "🇱🇧", 89500.0),
      CurrencyInfo("SYP", "Syrian Pound", "£S", "🇸🇾", 13000.0),
      CurrencyInfo("YER", "Yemeni Rial", "﷼", "🇾🇪", 250.0),
      CurrencyInfo("MAD", "Moroccan Dirham", "د.م.", "🇲🇦", 9.95),
      CurrencyInfo("DZD", "Algerian Dinar", "د.ج", "🇩🇿", 133.5),
      CurrencyInfo("TND", "Tunisian Dinar", "د.ت", "🇹🇳", 3.13),
      CurrencyInfo("LYD", "Libyan Dinar", "ل.د", "🇱🇾", 4.86),
      CurrencyInfo("SDG", "Sudanese Pound", "£", "🇸🇩", 601.0),
      CurrencyInfo("PKR", "Pakistani Rupee", "₨", "🇵🇰", 277.8),
      CurrencyInfo("BDT", "Bangladeshi Taka", "৳", "🇧🇩", 119.5),
      CurrencyInfo("LKR", "Sri Lankan Rupee", "Rs", "🇱🇰", 292.0),
      CurrencyInfo("NPR", "Nepalese Rupee", "₨", "🇳🇵", 135.0),
      CurrencyInfo("IDR", "Indonesian Rupiah", "Rp", "🇮🇩", 15850.0),
      CurrencyInfo("MYR", "Malaysian Ringgit", "RM", "🇲🇾", 4.46),
      CurrencyInfo("THB", "Thai Baht", "฿", "🇹🇭", 34.7),
      CurrencyInfo("VND", "Vietnamese Dong", "₫", "🇻🇳", 25400.0),
      CurrencyInfo("PHP", "Philippine Peso", "₱", "🇵🇭", 58.7),
      CurrencyInfo("TWD", "New Taiwan Dollar", "NT$", "🇹🇼", 32.5),
      CurrencyInfo("AFN", "Afghan Afghani", "؋", "🇦🇫", 68.0),
      CurrencyInfo("ALL", "Albanian Lek", "L", "🇦🇱", 92.5),
      CurrencyInfo("AMD", "Armenian Dram", "֏", "🇦🇲", 387.0),
      CurrencyInfo("ANG", "Netherlands Antillean Guilder", "ƒ", "🇨🇼", 1.79),
      CurrencyInfo("AOA", "Angolan Kwanza", "Kz", "🇦🇴", 912.0),
      CurrencyInfo("ARS", "Argentine Peso", "$", "🇦🇷", 1002.0),
      CurrencyInfo("AWG", "Aruban Florin", "ƒ", "🇦🇼", 1.79),
      CurrencyInfo("AZN", "Azerbaijani Manat", "₼", "🇦🇿", 1.70),
      CurrencyInfo("BAM", "Bosnia Convertible Mark", "KM", "🇧🇦", 1.80),
      CurrencyInfo("BBD", "Barbadian Dollar", "$", "🇧🇧", 2.0),
      CurrencyInfo("BGN", "Bulgarian Lev", "лв", "🇧🇬", 1.80),
      CurrencyInfo("BIF", "Burundian Franc", "FBu", "🇧🇮", 2910.0),
      CurrencyInfo("BMD", "Bermudian Dollar", "$", "🇧🇲", 1.0),
      CurrencyInfo("BND", "Brunei Dollar", "$", "🇧🇳", 1.34),
      CurrencyInfo("BOB", "Bolivian Boliviano", "Bs.", "🇧🇴", 6.91),
      CurrencyInfo("BSD", "Bahamian Dollar", "$", "🇧🇸", 1.0),
      CurrencyInfo("BTN", "Bhutanese Ngultrum", "Nu.", "🇧🇹", 84.4),
      CurrencyInfo("BWP", "Botswana Pula", "P", "🇧🇼", 13.6),
      CurrencyInfo("BYN", "Belarusian Ruble", "Br", "🇧🇾", 3.27),
      CurrencyInfo("BZD", "Belize Dollar", "BZ$", "🇧🇿", 2.0),
      CurrencyInfo("CDF", "Congolese Franc", "FC", "🇨🇩", 2850.0),
      CurrencyInfo("CLP", "Chilean Peso", "$", "🇨🇱", 975.0),
      CurrencyInfo("COP", "Colombian Peso", "$", "🇨🇴", 4420.0),
      CurrencyInfo("CRC", "Costa Rican Colón", "₡", "🇨🇷", 510.0),
      CurrencyInfo("CUP", "Cuban Peso", "$", "🇨🇺", 24.0),
      CurrencyInfo("CVE", "Cape Verdean Escudo", "$", "🇨🇻", 101.5),
      CurrencyInfo("CZK", "Czech Koruna", "Kč", "🇨🇿", 23.8),
      CurrencyInfo("DJF", "Djiboutian Franc", "Fdj", "🇩🇯", 177.7),
      CurrencyInfo("DOP", "Dominican Peso", "RD$", "🇩🇴", 60.2),
      CurrencyInfo("ERN", "Eritrean Nakfa", "Nfk", "🇪🇷", 15.0),
      CurrencyInfo("ETB", "Ethiopian Birr", "Br", "🇪🇹", 123.0),
      CurrencyInfo("FJD", "Fijian Dollar", "FJ$", "🇫🇯", 2.26),
      CurrencyInfo("FKP", "Falkland Islands Pound", "£", "🇫🇰", 0.79),
      CurrencyInfo("FOK", "Faroese Króna", "kr", "🇫🇴", 6.89),
      CurrencyInfo("GEL", "Georgian Lari", "₾", "🇬🇪", 2.73),
      CurrencyInfo("GGP", "Guernsey Pound", "£", "🇬🇬", 0.79),
      CurrencyInfo("GHS", "Ghanaian Cedi", "₵", "🇬🇭", 16.0),
      CurrencyInfo("GIP", "Gibraltar Pound", "£", "🇬🇮", 0.79),
      CurrencyInfo("GMD", "Gambian Dalasi", "D", "🇬🇲", 71.0),
      CurrencyInfo("GNF", "Guinean Franc", "FG", "🇬🇳", 8600.0),
      CurrencyInfo("GTQ", "Guatemalan Quetzal", "Q", "🇬🇹", 7.72),
      CurrencyInfo("GYD", "Guyanese Dollar", "$", "🇬🇾", 209.0),
      CurrencyInfo("HNL", "Honduran Lempira", "L", "🇭🇳", 25.2),
      CurrencyInfo("HRK", "Croatian Kuna (Legacy)", "kn", "🇭🇷", 6.95),
      CurrencyInfo("HTG", "Haitian Gourde", "G", "🇭🇹", 131.5),
      CurrencyInfo("HUF", "Hungarian Forint", "Ft", "🇭🇺", 384.0),
      CurrencyInfo("ILS", "Israeli New Shekel", "₪", "🇮🇱", 3.74),
      CurrencyInfo("IMP", "Isle of Man Pound", "£", "🇮🇲", 0.79),
      CurrencyInfo("IRR", "Iranian Rial", "﷼", "🇮🇷", 42000.0),
      CurrencyInfo("ISK", "Icelandic Króna", "kr", "🇮🇸", 138.0),
      CurrencyInfo("JEP", "Jersey Pound", "£", "🇯🇪", 0.79),
      CurrencyInfo("JMD", "Jamaican Dollar", "J$", "🇯🇲", 158.0),
      CurrencyInfo("KES", "Kenyan Shilling", "KSh", "🇰🇪", 129.0),
      CurrencyInfo("KGS", "Kyrgystani Som", "с", "🇰🇬", 86.2),
      CurrencyInfo("KHR", "Cambodian Riel", "៛", "🇰🇭", 4065.0),
      CurrencyInfo("KID", "Kiribati Dollar", "$", "🇰🇮", 1.54),
      CurrencyInfo("KMF", "Comorian Franc", "CF", "🇰🇲", 453.0),
      CurrencyInfo("KYD", "Cayman Islands Dollar", "$", "🇰🇾", 0.83),
      CurrencyInfo("KZT", "Kazakhstani Tenge", "₸", "🇰🇿", 495.0),
      CurrencyInfo("LAK", "Laotian Kip", "₭", "🇱🇦", 21900.0),
      CurrencyInfo("LRD", "Liberian Dollar", "$", "🇱🇷", 185.0),
      CurrencyInfo("LSL", "Lesotho Loti", "L", "🇱🇸", 18.1),
      CurrencyInfo("MDL", "Moldovan Leu", "L", "🇲🇩", 18.1),
      CurrencyInfo("MGA", "Malagasy Ariary", "Ar", "🇲🇬", 4640.0),
      CurrencyInfo("MKD", "Macedonian Denar", "ден", "🇲🇰", 56.8),
      CurrencyInfo("MMK", "Myanmar Kyat", "K", "🇲🇲", 2100.0),
      CurrencyInfo("MNT", "Mongolian Tugrik", "₮", "🇲🇳", 3410.0),
      CurrencyInfo("MOP", "Macanese Pataca", "MOP$", "🇲🇴", 8.01),
      CurrencyInfo("MRU", "Mauritanian Ouguiya", "UM", "🇲🇷", 39.8),
      CurrencyInfo("MUR", "Mauritian Rupee", "₨", "🇲🇺", 46.7),
      CurrencyInfo("MVR", "Maldivian Rufiyaa", "Rf", "🇲🇻", 15.4),
      CurrencyInfo("MWK", "Malawian Kwacha", "MK", "🇲🇼", 1734.0),
      CurrencyInfo("MZN", "Mozambican Metical", "MT", "🇲🇿", 63.9),
      CurrencyInfo("NAD", "Namibian Dollar", "$", "🇳🇦", 18.1),
      CurrencyInfo("NGN", "Nigerian Naira", "₦", "🇳🇬", 1680.0),
      CurrencyInfo("NIO", "Nicaraguan Córdoba", "C$", "🇳🇮", 36.8),
      CurrencyInfo("PAB", "Panamanian Balboa", "B/.", "🇵🇦", 1.0),
      CurrencyInfo("PEN", "Peruvian Sol", "S/", "🇵🇪", 3.79),
      CurrencyInfo("PGK", "Papua New Guinean Kina", "K", "🇵🇬", 4.02),
      CurrencyInfo("PYG", "Paraguayan Guarani", "₲", "🇵🇾", 7820.0),
      CurrencyInfo("RON", "Romanian Leu", "lei", "🇷🇴", 4.59),
      CurrencyInfo("RSD", "Serbian Dinar", "дин.", "🇷🇸", 108.0),
      CurrencyInfo("RWF", "Rwandan Franc", "FRw", "🇷🇼", 1370.0),
      CurrencyInfo("SBD", "Solomon Islands Dollar", "$", "🇸🇧", 8.4),
      CurrencyInfo("SCR", "Seychellois Rupee", "₨", "🇸🇨", 13.7),
      CurrencyInfo("SLE", "Sierra Leonean Leone", "Le", "🇸🇱", 22.7),
      CurrencyInfo("SOS", "Somali Shilling", "Sh", "🇸🇴", 571.0),
      CurrencyInfo("SRD", "Surinamese Dollar", "$", "🇸🇷", 35.5),
      CurrencyInfo("SSP", "South Sudanese Pound", "£", "🇸🇸", 3500.0),
      CurrencyInfo("STN", "São Tomé Dobra", "Db", "🇸🇹", 22.6),
      CurrencyInfo("SZL", "Swazi Lilangeni", "E", "🇸🇿", 18.1),
      CurrencyInfo("TJS", "Tajikistani Somoni", "ЅМ", "🇹🇯", 10.7),
      CurrencyInfo("TMT", "Turkmenistani Manat", "m", "🇹🇲", 3.5),
      CurrencyInfo("TOP", "Tongan Paʻanga", "T$", "🇹🇴", 2.38),
      CurrencyInfo("TTD", "Trinidad & Tobago Dollar", "TT$", "🇹🇹", 6.78),
      CurrencyInfo("TVD", "Tuvaluan Dollar", "$", "🇹🇻", 1.54),
      CurrencyInfo("TZS", "Tanzanian Shilling", "TSh", "🇹🇿", 2650.0),
      CurrencyInfo("UAH", "Ukrainian Hryvnia", "₴", "🇺🇦", 41.3),
      CurrencyInfo("UGX", "Ugandan Shilling", "USh", "🇺🇬", 3670.0),
      CurrencyInfo("UYU", "Uruguayan Peso", "\$U", "🇺🇾", 42.3),
      CurrencyInfo("UZS", "Uzbekistan Som", "so'm", "🇺🇿", 12820.0),
      CurrencyInfo("VES", "Venezuelan Bolívar", "Bs.", "🇻🇪", 45.5),
      CurrencyInfo("VUV", "Vanuatu Vatu", "VT", "🇻🇺", 120.0),
      CurrencyInfo("WST", "Samoan Tala", "WS$", "🇼🇸", 2.79),
      CurrencyInfo("XAF", "Central African CFA Franc", "FCFA", "🇨🇲", 605.0),
      CurrencyInfo("XCD", "East Caribbean Dollar", "EC$", "🇦🇬", 2.70),
      CurrencyInfo("XDR", "IMF Special Drawing Rights", "SDR", "🌐", 0.75),
      CurrencyInfo("XOF", "West African CFA Franc", "CFA", "🇸🇳", 605.0),
      CurrencyInfo("XPF", "CFP Franc", "₣", "🇵🇫", 110.0),
      CurrencyInfo("ZMW", "Zambian Kwacha", "ZK", "🇿🇲", 27.1),
      CurrencyInfo("ZWL", "Zimbabwean Gold (ZiG)", "ZiG", "🇿🇼", 25.6),
      CurrencyInfo("KPW", "North Korean Won", "₩", "🇰🇵", 900.0),
      CurrencyInfo("SHP", "Saint Helena Pound", "£", "🇸🇭", 0.79),
    )

  private val byCode: Map<String, CurrencyInfo> = all.associateBy { it.code }

  val popularCodes = listOf("USD", "EUR", "GBP", "JPY", "CNY", "SAR", "AED", "EGP", "INR", "CHF", "CAD", "AUD")

  private val menaCodes =
    setOf(
      "SAR", "AED", "EGP", "KWD", "BHD", "OMR", "QAR", "JOD", "IQD", "LBP", "SYP", "YER",
      "MAD", "DZD", "TND", "LYD", "SDG", "ILS", "IRR", "MRU", "SOS", "DJF", "ZAR", "NGN",
      "KES", "GHS", "ETB", "TZS", "UGX", "RWF", "ZMW", "MAD", "NAD", "BWP", "MUR", "SCR",
      "AOA", "CDF", "XAF", "XOF", "MGA", "MWK", "MZN", "SZL", "LSL", "GMD", "GNF", "LRD",
      "SLE", "SSP", "STN", "CVE", "KMF", "BIF", "ERN", "SHP", "ZWL",
    )

  private val europeCodes =
    setOf(
      "EUR", "GBP", "CHF", "SEK", "NOK", "DKK", "PLN", "TRY", "RUB", "UAH", "CZK", "HUF",
      "RON", "RSD", "BGN", "ISK", "BAM", "MKD", "MDL", "ALL", "BYN", "GEL", "AMD", "AZN",
      "FOK", "GGP", "GIP", "IMP", "JEP", "HRK",
    )

  private val americasCodes =
    setOf(
      "USD", "CAD", "BRL", "MXN", "ARS", "CLP", "COP", "PEN", "UYU", "PYG", "BOB", "CRC",
      "DOP", "GTQ", "HNL", "NIO", "PAB", "VES", "JMD", "TTD", "BSD", "BBD", "BZD", "KYD",
      "GYD", "SRD", "HTG", "CUP", "XCD", "ANG", "AWG", "BMD", "FKP",
    )

  private val asiaPacificCodes =
    setOf(
      "JPY", "CNY", "INR", "AUD", "NZD", "KRW", "SGD", "HKD", "TWD", "IDR", "MYR", "THB",
      "VND", "PHP", "PKR", "BDT", "LKR", "NPR", "KZT", "UZS", "AZN", "AFN", "MMK", "KHR",
      "LAK", "MNT", "MOP", "BND", "BTN", "MVR", "KGS", "TJS", "TMT", "KPW", "FJD", "PGK",
      "SBD", "TOP", "VUV", "WST", "KID", "TVD", "XPF",
    )

  enum class FilterRegion {
    ALL,
    POPULAR,
    SAVED,
    MENA,
    EUROPE,
    ASIA_PACIFIC,
    AMERICAS,
  }

  fun filterCurrencies(
    currencies: List<CurrencyInfo>,
    query: String,
    region: FilterRegion,
    savedCodes: Set<String> = emptySet(),
  ): List<CurrencyInfo> {
    val popularSet = popularCodes.toSet()
    val regional =
      when (region) {
        FilterRegion.ALL -> currencies
        FilterRegion.POPULAR -> currencies.filter { it.code in popularSet }
        FilterRegion.SAVED -> currencies.filter { it.code in savedCodes }
        FilterRegion.MENA -> currencies.filter { it.code in menaCodes }
        FilterRegion.EUROPE -> currencies.filter { it.code in europeCodes }
        FilterRegion.ASIA_PACIFIC -> currencies.filter { it.code in asiaPacificCodes }
        FilterRegion.AMERICAS -> currencies.filter { it.code in americasCodes }
      }

    val q = query.trim().lowercase()
    if (q.isEmpty()) return regional

    return regional
      .mapNotNull { item ->
        val code = item.code.lowercase()
        val name = item.name.lowercase()
        val symbol = item.symbol.lowercase()
        val score =
          when {
            code == q -> 0
            code.startsWith(q) -> 1
            name.startsWith(q) -> 2
            code.contains(q) || name.contains(q) || symbol.contains(q) -> 3
            else -> return@mapNotNull null
          }
        score to item
      }
      .sortedBy { it.first }
      .map { it.second }
  }

  fun find(code: String): CurrencyInfo =
    byCode[code.uppercase()]
      ?: CurrencyInfo(
        code = code.uppercase(),
        name = code.uppercase(),
        symbol = code.uppercase(),
        flag = "🏳️",
        fallbackRateFromUsd = 1.0,
      )

  fun fallbackRatesMap(): Map<String, Double> = all.associate { it.code to it.fallbackRateFromUsd }
}
