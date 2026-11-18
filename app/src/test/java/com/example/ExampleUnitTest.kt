package com.example

import com.example.domain.CountryMetalsCatalog
import com.example.domain.CurrencyCatalog
import com.example.domain.FormattingUtils
import com.example.domain.UnitCatalog
import com.example.domain.UnitCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ExampleUnitTest {
  @Test
  fun unitConversions_areAccurate() {
    val km = UnitCatalog.unitsFor(UnitCategory.LENGTH).first { it.id == "km" }
    val mi = UnitCatalog.unitsFor(UnitCategory.LENGTH).first { it.id == "mi" }
    assertEquals(6.2137119, UnitCatalog.convert(10.0, km, mi), 1e-4)

    val c = UnitCatalog.unitsFor(UnitCategory.TEMPERATURE).first { it.id == "c" }
    val f = UnitCatalog.unitsFor(UnitCategory.TEMPERATURE).first { it.id == "f" }
    val k = UnitCatalog.unitsFor(UnitCategory.TEMPERATURE).first { it.id == "k" }
    assertEquals(212.0, UnitCatalog.convert(100.0, c, f), 1e-6)
    assertEquals(273.15, UnitCatalog.convert(0.0, c, k), 1e-6)
    assertEquals(0.0, UnitCatalog.convert(32.0, f, c), 1e-6)

    val kml = UnitCatalog.unitsFor(UnitCategory.FUEL).first { it.id == "kml" }
    val l100 = UnitCatalog.unitsFor(UnitCategory.FUEL).first { it.id == "l100km" }
    assertEquals(5.0, UnitCatalog.convert(20.0, kml, l100), 1e-6)
  }

  @Test
  fun currencyAndCountryCatalogs_coverGlobalMarkets() {
    assertTrue(CurrencyCatalog.all.size >= 150)
    assertTrue(CurrencyCatalog.all.all { it.fallbackRateFromUsd > 0.0 })
    assertTrue(CountryMetalsCatalog.countries.size >= 190)

    val sa = CountryMetalsCatalog.find("SA")
    assertEquals("SAR", sa.currencyCode)
    assertTrue(sa.localizedName(Locale.forLanguageTag("ar")).isNotBlank())
    assertTrue(sa.localizedName(Locale.JAPANESE).isNotBlank())
  }

  @Test
  fun formattingUtils_formatsCleanly() {
    assertEquals("1,234.5", FormattingUtils.formatSmart(1234.5))
    assertEquals("100.00 USD", FormattingUtils.formatMoney(100.0, "USD"))
  }
}
