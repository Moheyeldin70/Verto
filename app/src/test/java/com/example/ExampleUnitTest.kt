package com.example

import com.example.data.local.AppLanguage
import com.example.data.local.UserPreferences
import com.example.domain.CountryMetalsCatalog
import com.example.domain.CurrencyCatalog
import com.example.domain.FormattingUtils
import com.example.domain.LocationLocaleResolver
import com.example.domain.MeasurementLocalePreference
import com.example.domain.MeasurementSystem
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

  @Test
  fun locationLocaleResolver_mapsRegionsAndManualOverrides() {
    // Measurement system mapping by country
    assertEquals(MeasurementSystem.IMPERIAL_US, LocationLocaleResolver.measurementSystemForCountry("US"))
    assertEquals(MeasurementSystem.UK, LocationLocaleResolver.measurementSystemForCountry("GB"))
    assertEquals(MeasurementSystem.METRIC, LocationLocaleResolver.measurementSystemForCountry("EG"))
    assertEquals(MeasurementSystem.METRIC, LocationLocaleResolver.measurementSystemForCountry("DE"))

    // Manual override supersedes detected country
    assertEquals(
      MeasurementSystem.METRIC,
      LocationLocaleResolver.resolveMeasurementSystem(MeasurementLocalePreference.METRIC, "US"),
    )
    assertEquals(
      MeasurementSystem.IMPERIAL_US,
      LocationLocaleResolver.resolveMeasurementSystem(MeasurementLocalePreference.IMPERIAL_US, "EG"),
    )

    // Default unit pairs per system
    val usTemp = LocationLocaleResolver.defaultUnitPair(UnitCategory.TEMPERATURE, MeasurementSystem.IMPERIAL_US)
    assertEquals("f", usTemp.first.id)
    assertEquals("c", usTemp.second.id)

    val egLength = LocationLocaleResolver.defaultUnitPair(UnitCategory.LENGTH, MeasurementSystem.METRIC)
    assertEquals("km", egLength.first.id)
    assertEquals("mi", egLength.second.id)

    val ukVolume = LocationLocaleResolver.defaultUnitPair(UnitCategory.VOLUME, MeasurementSystem.UK)
    assertEquals("gal_uk", ukVolume.first.id)
    assertEquals("l", ukVolume.second.id)

    // Default currency pair and country market per detected country
    assertEquals("EUR" to "USD", LocationLocaleResolver.defaultCurrencyPairForCountry("US"))
    assertEquals("USD" to "EGP", LocationLocaleResolver.defaultCurrencyPairForCountry("EG"))
    assertEquals("USD" to "GBP", LocationLocaleResolver.defaultCurrencyPairForCountry("GB"))
    assertEquals("EGP", LocationLocaleResolver.defaultCountryMarket("EG").currencyCode)

    // Default language remains the device system language
    assertEquals(AppLanguage.SYSTEM, UserPreferences().language)
  }
}
