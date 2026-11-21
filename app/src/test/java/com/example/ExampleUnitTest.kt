package com.example

import com.example.ai.VertoAiContextBuilder
import com.example.data.local.AppLanguage
import com.example.data.local.CachedChartPointEntity
import com.example.data.local.NavBarPosition
import com.example.data.local.StartScreenPreference
import com.example.data.local.UserPreferences
import com.example.data.repository.ChartRange
import com.example.data.repository.SyncStatus
import com.example.domain.CountryMetalsCatalog
import com.example.domain.CurrencyCatalog
import com.example.domain.FormattingUtils
import com.example.domain.HistoricalSeriesParser
import com.example.domain.LocationLocaleResolver
import com.example.domain.MeasurementLocalePreference
import com.example.domain.MeasurementSystem
import com.example.domain.ShareExportHelper
import com.example.domain.UnitCatalog
import com.example.domain.UnitCategory
import com.example.ui.AppTab
import com.example.ui.CurrencyConverterState
import com.example.ui.MetalChartUnit
import com.example.ui.MetalType
import com.example.ui.MetalsTrackerState
import com.example.ui.UnitConverterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

  @Test
  fun historicalSeriesParser_handlesFiveYearFeedsAndDownsampling() {
    assertEquals(1825, ChartRange.Y5.days)

    val csv =
      """
      Date,Open,High,Low,Close
      2022-01-02,1800.0,1830.0,1790.0,1815.5
      2023-06-11,1950.0,1970.0,1930.0,1960.2
      2026-11-15,2600.0,2640.0,2590.0,2635.0
      """.trimIndent()
    val stooqPoints = HistoricalSeriesParser.parseStooqCsv(csv, 0L)
    assertEquals(3, stooqPoints.size)
    assertEquals(1815.5, stooqPoints.first().second, 1e-6)
    assertEquals(2635.0, stooqPoints.last().second, 1e-6)

    val dense = (0 until 260).map { idx -> (idx * 7L * 86_400_000L) to (1800.0 + idx) }
    val sampled = HistoricalSeriesParser.downsamplePairs(dense, maxPoints = 75)
    assertTrue(sampled.size <= 75)
    assertEquals(dense.first(), sampled.first())
    assertEquals(dense.last(), sampled.last())

    val fx = listOf(stooqPoints[0].first to 15.7, stooqPoints[1].first to 30.9, stooqPoints[2].first to 49.5)
    val inEgp = HistoricalSeriesParser.multiplySeriesByFx(stooqPoints, fx, fallbackFxRate = 50.0)
    assertEquals(1815.5 * 15.7, inEgp[0].second, 1e-4)
    assertEquals(2635.0 * 49.5, inEgp[2].second, 1e-4)

    val cross = HistoricalSeriesParser.alignCrossRateSeries(
      usdToFrom = listOf(stooqPoints[0].first to 0.90, stooqPoints[2].first to 0.95),
      usdToTo = fx,
    )
    assertEquals(2, cross.size)
    assertEquals(15.7 / 0.90, cross[0].second, 1e-4)
    assertEquals(49.5 / 0.95, cross[1].second, 1e-4)
  }

  @Test
  fun settingsEnumsAndMetalChartUnits_roundTripCleanly() {
    assertEquals(NavBarPosition.BOTTOM, NavBarPosition.fromKey(null))
    assertEquals(NavBarPosition.TOP, NavBarPosition.fromKey("TOP"))
    assertEquals(NavBarPosition.LEFT, NavBarPosition.fromKey("LEFT"))
    assertEquals(NavBarPosition.RIGHT, NavBarPosition.fromKey("RIGHT"))

    assertEquals(StartScreenPreference.UNITS, StartScreenPreference.fromKey(null))
    assertEquals(StartScreenPreference.LAST_OPENED, StartScreenPreference.fromKey("LAST_OPENED"))
    assertEquals(StartScreenPreference.METALS, StartScreenPreference.fromKey("METALS"))

    assertEquals(3, MetalChartUnit.entries.size)
  }

  @Test
  fun shareExportHelper_buildsRichTextAndChartPayloads() {
    val unitsPayload =
      ShareExportHelper.buildPayload(
        activeTab = AppTab.UNITS,
        unitState = UnitConverterState(inputText = "10"),
        currencyState = CurrencyConverterState(),
        metalsState = MetalsTrackerState(),
        ratesMap = emptyMap(),
        syncStatus = SyncStatus(isLive = false, isSyncing = false, isNetworkAvailable = false, lastUpdatedMillis = 0L),
        displayLocale = Locale.US,
        isDarkTheme = true,
        categoryDisplayTitle = "Length",
        tabTitle = "Unit Converter",
        chartDisplayTitle = "",
        liveLabel = "Live",
        offlineLabel = "Offline",
      )
    assertFalse(unitsPayload.hasChart)
    assertTrue(unitsPayload.textSummary.contains("10"))
    assertTrue(unitsPayload.textSummary.contains("Formula:"))

    val fxPoints =
      listOf(
        CachedChartPointEntity("FX_USD_EUR_M1_0", "FX_USD_EUR_M1", 0, 1_700_000_000_000L, 0.90, "Oct 21"),
        CachedChartPointEntity("FX_USD_EUR_M1_1", "FX_USD_EUR_M1", 1, 1_700_864_000_000L, 0.95, "Nov 21"),
      )
    val fxPayload =
      ShareExportHelper.buildPayload(
        activeTab = AppTab.CURRENCY,
        unitState = UnitConverterState(),
        currencyState = CurrencyConverterState(fromCode = "USD", toCode = "EUR", inputText = "100", chartPoints = fxPoints),
        metalsState = MetalsTrackerState(),
        ratesMap = mapOf("USD" to 1.0, "EUR" to 0.95),
        syncStatus = SyncStatus(isLive = true, isSyncing = false, isNetworkAvailable = true, lastUpdatedMillis = 1_700_864_000_000L),
        displayLocale = Locale.US,
        isDarkTheme = false,
        categoryDisplayTitle = "",
        tabTitle = "Global Currency",
        chartDisplayTitle = "Historical Rate Trend",
        liveLabel = "Live",
        offlineLabel = "Offline",
      )
    assertTrue(fxPayload.hasChart)
    assertTrue(fxPayload.textSummary.contains("USD"))
    assertTrue(fxPayload.textSummary.contains("EUR"))
    assertTrue(fxPayload.textSummary.contains("+5.56%"))
  }

  @Test
  fun aiContextBuilder_formatsLiveSnapshotForCurrencyAndMetals() {
    val label =
      VertoAiContextBuilder.buildShortContextLabel(
        activeTab = AppTab.METALS,
        unitState = UnitConverterState(),
        currencyState = CurrencyConverterState(),
        metalsState = MetalsTrackerState(selectedMetal = MetalType.GOLD, selectedCountry = CountryMetalsCatalog.find("EG")),
        displayLocale = Locale.US,
      )
    assertTrue(label.contains("Gold (XAU)"))
    assertTrue(label.contains("EGP"))

    val snapshot =
      VertoAiContextBuilder.buildContextSnapshot(
        activeTab = AppTab.METALS,
        unitState = UnitConverterState(),
        currencyState = CurrencyConverterState(),
        metalsState = MetalsTrackerState(selectedMetal = MetalType.GOLD, selectedCountry = CountryMetalsCatalog.find("EG")),
        ratesMap = mapOf("EGP" to 49.3),
        syncStatus = SyncStatus(isLive = false, isSyncing = false, isNetworkAvailable = false, lastUpdatedMillis = 1_700_864_000_000L),
        displayLocale = Locale.forLanguageTag("ar"),
      )
    assertTrue(snapshot.contains("Gold (XAU)"))
    assertTrue(snapshot.contains("Offline"))
    assertTrue(snapshot.contains("EGP"))
  }

  @Test
  fun unitCatalog_searchUnits_supportsExactPrefixAndCrossCategory() {
    val allTemp = UnitCatalog.searchUnits("fahrenheit", categoryFilter = null)
    assertTrue(allTemp.isNotEmpty())
    assertEquals("f", allTemp.first().id)

    val exactSymbol = UnitCatalog.searchUnits("kw", categoryFilter = null)
    assertEquals("kw", exactSymbol.first().id)
    assertEquals(UnitCategory.POWER, exactSymbol.first().category)

    val scoped = UnitCatalog.searchUnits("fahrenheit", categoryFilter = UnitCategory.LENGTH)
    assertTrue(scoped.isEmpty())

    val catMatch = UnitCatalog.searchUnits("temperature", categoryFilter = null)
    assertTrue(catMatch.any { it.category == UnitCategory.TEMPERATURE })
  }

  @Test
  fun currencyCatalog_filterCurrencies_filtersByQueryRegionAndSaved() {
    val exactCode =
      CurrencyCatalog.filterCurrencies(
        currencies = CurrencyCatalog.all,
        query = "egp",
        region = CurrencyCatalog.FilterRegion.ALL,
      )
    assertEquals("EGP", exactCode.first().code)

    val menaList =
      CurrencyCatalog.filterCurrencies(
        currencies = CurrencyCatalog.all,
        query = "",
        region = CurrencyCatalog.FilterRegion.MENA,
      )
    assertTrue(menaList.any { it.code == "SAR" })
    assertTrue(menaList.any { it.code == "EGP" })
    assertFalse(menaList.any { it.code == "JPY" })

    val europeEur =
      CurrencyCatalog.filterCurrencies(
        currencies = CurrencyCatalog.all,
        query = "swiss",
        region = CurrencyCatalog.FilterRegion.EUROPE,
      )
    assertEquals(listOf("CHF"), europeEur.map { it.code })

    val savedOnly =
      CurrencyCatalog.filterCurrencies(
        currencies = CurrencyCatalog.all,
        query = "",
        region = CurrencyCatalog.FilterRegion.SAVED,
        savedCodes = setOf("USD", "EGP"),
      )
    assertEquals(setOf("USD", "EGP"), savedOnly.map { it.code }.toSet())

    val popularOnly =
      CurrencyCatalog.filterCurrencies(
        currencies = CurrencyCatalog.all,
        query = "",
        region = CurrencyCatalog.FilterRegion.POPULAR,
      )
    assertEquals(CurrencyCatalog.popularCodes.size, popularOnly.size)
  }
}
