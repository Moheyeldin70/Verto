package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AppLanguage
import com.example.data.local.CachedChartPointEntity
import com.example.data.local.NavBarPosition
import com.example.data.local.SavedConversionEntity
import com.example.data.local.StartScreenPreference
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.location.CountryLocator
import com.example.data.remote.NetworkModule
import com.example.data.repository.ChartRange
import com.example.data.repository.MarketRepository
import com.example.data.repository.SyncStatus
import com.example.domain.CountryMarket
import com.example.domain.CountryMetalsCatalog
import com.example.domain.CurrencyCatalog
import com.example.domain.LocationLocaleResolver
import com.example.domain.MeasurementLocalePreference
import com.example.domain.MeasurementSystem
import com.example.domain.UnitCatalog
import com.example.domain.UnitCategory
import com.example.domain.UnitDef
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppTab {
  UNITS,
  CURRENCY,
  METALS,
}

enum class MetalChartUnit {
  OZ,
  GRAM,
  COIN,
}

enum class MetalType(val code: String) {
  GOLD("XAU"),
  SILVER("XAG"),
}

data class UnitConverterState(
  val category: UnitCategory = UnitCategory.LENGTH,
  val fromUnit: UnitDef = UnitCatalog.defaultPair(UnitCategory.LENGTH).first,
  val toUnit: UnitDef = UnitCatalog.defaultPair(UnitCategory.LENGTH).second,
  val inputText: String = "10",
) {
  val inputValue: Double
    get() = inputText.toDoubleOrNull() ?: 0.0

  val outputValue: Double
    get() = UnitCatalog.convert(inputValue, fromUnit, toUnit)
}

data class CurrencyConverterState(
  val fromCode: String = "USD",
  val toCode: String = "EUR",
  val inputText: String = "100",
  val chartRange: ChartRange = ChartRange.M1,
  val chartPoints: List<CachedChartPointEntity> = emptyList(),
  val isChartLoading: Boolean = false,
)

data class MetalsTrackerState(
  val selectedMetal: MetalType = MetalType.GOLD,
  val selectedCountry: CountryMarket = CountryMetalsCatalog.find("US"),
  val chartRange: ChartRange = ChartRange.M1,
  val chartUnit: MetalChartUnit = MetalChartUnit.OZ,
  val chartPurityFraction: Double = 1.0,
  val chartPoints: List<CachedChartPointEntity> = emptyList(),
  val isChartLoading: Boolean = false,
  val goldUsdPerOz: Double = 2635.0,
  val silverUsdPerOz: Double = 31.20,
  val goldChange24h: Double = 0.0,
  val silverChange24h: Double = 0.0,
  val calculatorGramsText: String = "10",
  val calculatorPurityFraction: Double = 1.0,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val db = AppDatabase.getInstance(application)
  private val prefsRepo = UserPreferencesRepository(application)
  private val marketRepo = MarketRepository(application, db.converterDao(), NetworkModule.apiService)
  private val countryLocator = CountryLocator(application)

  private val _activeTab = MutableStateFlow(AppTab.UNITS)
  val activeTab: StateFlow<AppTab> = _activeTab

  private val _unitState = MutableStateFlow(UnitConverterState())
  val unitState: StateFlow<UnitConverterState> = _unitState

  private val _currencyState = MutableStateFlow(CurrencyConverterState())
  val currencyState: StateFlow<CurrencyConverterState> = _currencyState

  private val _metalsState = MutableStateFlow(MetalsTrackerState())
  val metalsState: StateFlow<MetalsTrackerState> = _metalsState

  private val _isSyncing = MutableStateFlow(false)
  private val _isDetectingLocation = MutableStateFlow(false)
  val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation

  private val _prefsLoaded = MutableStateFlow(false)
  val prefsLoaded: StateFlow<Boolean> = _prefsLoaded

  private val _detectedRegionEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
  val detectedRegionEvent: SharedFlow<String> = _detectedRegionEvent

  private var hasRestoredInitialUnits = false
  private var hasAppliedInitialStartTab = false
  private var currencyChartJob: Job? = null
  private var currencyPrefetchJob: Job? = null
  private var metalChartJob: Job? = null
  private var metalPrefetchJob: Job? = null

  val userPreferences =
    prefsRepo.preferences.stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      UserPreferences(),
    )

  val currencyRatesMap: StateFlow<Map<String, Double>> =
    marketRepo.currencyRatesFlow
      .combine(MutableStateFlow(CurrencyCatalog.fallbackRatesMap())) { entities, fallback ->
        if (entities.isEmpty()) fallback
        else fallback + entities.associate { it.code to it.rateFromUsd }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyCatalog.fallbackRatesMap())

  val savedConversions: StateFlow<List<SavedConversionEntity>> =
    marketRepo.savedConversionsFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList(),
    )

  val syncStatus: StateFlow<SyncStatus> =
    combine(
      marketRepo.currencyRatesFlow,
      marketRepo.metalPricesFlow,
      _isSyncing,
    ) { fxList, metalList, syncing ->
      val latestFx = fxList.maxByOrNull { it.updatedAtMillis }
      val latestMetal = metalList.maxByOrNull { it.updatedAtMillis }
      val isLive = (latestFx?.isLiveSynced == true) || (latestMetal?.isLiveSynced == true)
      val updated = maxOf(latestFx?.updatedAtMillis ?: 0L, latestMetal?.updatedAtMillis ?: 0L)
      SyncStatus(
        isLive = isLive,
        isSyncing = syncing,
        isNetworkAvailable = marketRepo.isNetworkAvailable(),
        lastUpdatedMillis = updated,
      )
    }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SyncStatus(isLive = false, isSyncing = false, isNetworkAvailable = false, lastUpdatedMillis = 0L),
      )

  init {
    viewModelScope.launch {
      marketRepo.ensureOfflineBaselineSeeded()
      refreshAllRates()
    }

    // Observe saved user preferences for currency, country, and unit locale
    viewModelScope.launch {
      prefsRepo.preferences.collect { prefs ->
        _prefsLoaded.value = true

        if (!hasAppliedInitialStartTab) {
          hasAppliedInitialStartTab = true
          _activeTab.value =
            when (prefs.startScreen) {
              StartScreenPreference.UNITS -> AppTab.UNITS
              StartScreenPreference.CURRENCY -> AppTab.CURRENCY
              StartScreenPreference.METALS -> AppTab.METALS
              StartScreenPreference.LAST_OPENED ->
                runCatching { AppTab.valueOf(prefs.lastActiveTab) }.getOrDefault(AppTab.UNITS)
            }
        }

        var currencyChanged = false
        _currencyState.update { cur ->
          if (cur.fromCode != prefs.fromCurrency || cur.toCode != prefs.toCurrency) {
            currencyChanged = true
            cur.copy(fromCode = prefs.fromCurrency, toCode = prefs.toCurrency)
          } else cur
        }
        if (currencyChanged) loadCurrencyHistory()

        val country = CountryMetalsCatalog.find(prefs.selectedCountryIso)
        var countryChanged = false
        _metalsState.update { m ->
          if (m.selectedCountry.isoCode != country.isoCode) {
            countryChanged = true
            m.copy(selectedCountry = country)
          } else m
        }
        if (countryChanged) loadMetalHistory()

        if (!hasRestoredInitialUnits) {
          hasRestoredInitialUnits = true
          if (prefs.hasManualUnits && prefs.unitCategoryName.isNotBlank()) {
            val cat = runCatching { UnitCategory.valueOf(prefs.unitCategoryName) }.getOrNull()
            if (cat != null) {
              val units = UnitCatalog.unitsFor(cat)
              val from = units.find { it.id == prefs.fromUnitId }
              val to = units.find { it.id == prefs.toUnitId }
              if (from != null && to != null) {
                _unitState.update { it.copy(category = cat, fromUnit = from, toUnit = to) }
              }
            }
          } else {
            val sys = resolveActiveMeasurementSystem(prefs)
            val currentCat = _unitState.value.category
            val pair = LocationLocaleResolver.defaultUnitPair(currentCat, sys)
            _unitState.update { it.copy(fromUnit = pair.first, toUnit = pair.second) }
          }
        }
      }
    }

    // Observe cached metal prices from Room
    viewModelScope.launch {
      marketRepo.metalPricesFlow.collect { prices ->
        val gold = prices.find { it.metalCode == "XAU" }
        val silver = prices.find { it.metalCode == "XAG" }
        _metalsState.update { state ->
          state.copy(
            goldUsdPerOz = gold?.priceUsdPerOz ?: state.goldUsdPerOz,
            silverUsdPerOz = silver?.priceUsdPerOz ?: state.silverUsdPerOz,
            goldChange24h = gold?.change24hPercent ?: state.goldChange24h,
            silverChange24h = silver?.change24hPercent ?: state.silverChange24h,
          )
        }
      }
    }

    // Real-time auto-polling every 30 seconds while active
    viewModelScope.launch {
      while (isActive) {
        delay(30_000L)
        refreshAllRates()
      }
    }

    // Initial chart loads
    loadCurrencyHistory()
    loadMetalHistory()
  }

  private fun resolveActiveMeasurementSystem(prefs: UserPreferences = userPreferences.value): MeasurementSystem {
    val countryIso =
      prefs.detectedCountryIso.ifBlank {
        Locale.getDefault().country.ifBlank { "US" }
      }
    return LocationLocaleResolver.resolveMeasurementSystem(prefs.measurementLocale, countryIso)
  }

  fun hasCoarseLocationPermission(): Boolean = countryLocator.hasCoarseLocationPermission()

  fun markLocationPrompted() {
    viewModelScope.launch { prefsRepo.markLocationPrompted() }
  }

  fun onLocationPermissionResult(granted: Boolean, forceApplyAll: Boolean) {
    viewModelScope.launch {
      prefsRepo.markLocationPrompted()
      if (granted) {
        detectAndApplyLocation(forceApplyAll = forceApplyAll)
      }
    }
  }

  fun detectAndApplyLocation(forceApplyAll: Boolean) {
    if (_isDetectingLocation.value) return
    viewModelScope.launch {
      _isDetectingLocation.value = true
      try {
        val region = countryLocator.detectCountry()
        val (defFromCurr, defToCurr) = LocationLocaleResolver.defaultCurrencyPairForCountry(region.countryIso)
        val prefs = userPreferences.value

        prefsRepo.saveDetectedRegion(
          countryIso = region.countryIso,
          resetManualOverrides = forceApplyAll,
          defaultFromCurrency = defFromCurr,
          defaultToCurrency = defToCurr,
        )

        if (forceApplyAll || !prefs.hasManualUnits) {
          val system = LocationLocaleResolver.measurementSystemForCountry(region.countryIso)
          val cat = _unitState.value.category
          val pair = LocationLocaleResolver.defaultUnitPair(cat, system)
          _unitState.update { it.copy(fromUnit = pair.first, toUnit = pair.second) }
        }

        if (forceApplyAll) {
          _detectedRegionEvent.tryEmit(region.countryIso)
        }
      } finally {
        _isDetectingLocation.value = false
      }
    }
  }

  fun setMeasurementLocale(preference: MeasurementLocalePreference) {
    viewModelScope.launch {
      prefsRepo.setMeasurementLocale(preference)
      val prefs = userPreferences.value
      val countryIso = prefs.detectedCountryIso.ifBlank { Locale.getDefault().country.ifBlank { "US" } }
      val system = LocationLocaleResolver.resolveMeasurementSystem(preference, countryIso)
      val cat = _unitState.value.category
      val pair = LocationLocaleResolver.defaultUnitPair(cat, system)
      _unitState.update { it.copy(fromUnit = pair.first, toUnit = pair.second) }
    }
  }

  fun selectTab(tab: AppTab) {
    _activeTab.value = tab
    viewModelScope.launch { prefsRepo.setLastActiveTab(tab.name) }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { prefsRepo.setThemeMode(mode) }
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch { prefsRepo.setLanguage(language) }
  }

  fun setNavBarPosition(position: NavBarPosition) {
    viewModelScope.launch { prefsRepo.setNavBarPosition(position) }
  }

  fun setStartScreen(startScreen: StartScreenPreference) {
    viewModelScope.launch { prefsRepo.setStartScreen(startScreen) }
  }

  fun refreshAllRates() {
    if (_isSyncing.value) return
    viewModelScope.launch {
      _isSyncing.value = true
      try {
        marketRepo.refreshLiveRates()
      } finally {
        _isSyncing.value = false
      }
    }
  }

  // ---- Unit Converter Actions ----
  fun selectUnitCategory(category: UnitCategory) {
    val system = resolveActiveMeasurementSystem()
    val pair = LocationLocaleResolver.defaultUnitPair(category, system)
    _unitState.update {
      it.copy(category = category, fromUnit = pair.first, toUnit = pair.second)
    }
  }

  fun setFromUnit(unit: UnitDef) {
    val current = _unitState.value
    _unitState.update { it.copy(fromUnit = unit) }
    viewModelScope.launch {
      prefsRepo.setUnitSelection(current.category.name, unit.id, current.toUnit.id, isManual = true)
    }
  }

  fun setToUnit(unit: UnitDef) {
    val current = _unitState.value
    _unitState.update { it.copy(toUnit = unit) }
    viewModelScope.launch {
      prefsRepo.setUnitSelection(current.category.name, current.fromUnit.id, unit.id, isManual = true)
    }
  }

  fun swapUnits() {
    val current = _unitState.value
    val newFrom = current.toUnit
    val newTo = current.fromUnit
    _unitState.update { it.copy(fromUnit = newFrom, toUnit = newTo) }
    viewModelScope.launch {
      prefsRepo.setUnitSelection(current.category.name, newFrom.id, newTo.id, isManual = true)
    }
  }

  fun updateUnitInput(text: String) {
    if (text.isEmpty() || text == "-" || text == "." || text == "-." || text.toDoubleOrNull() != null) {
      _unitState.update { it.copy(inputText = text) }
    }
  }

  // ---- Currency Converter Actions ----
  fun setCurrencyInput(text: String) {
    if (text.isEmpty() || text == "." || text.toDoubleOrNull() != null) {
      _currencyState.update { it.copy(inputText = text) }
    }
  }

  fun setFromCurrency(code: String) {
    val to = _currencyState.value.toCode
    _currencyState.update { it.copy(fromCode = code) }
    viewModelScope.launch { prefsRepo.setCurrencyPair(code, to) }
    loadCurrencyHistory()
  }

  fun setToCurrency(code: String) {
    val from = _currencyState.value.fromCode
    _currencyState.update { it.copy(toCode = code) }
    viewModelScope.launch { prefsRepo.setCurrencyPair(from, code) }
    loadCurrencyHistory()
  }

  fun swapCurrencies() {
    val cur = _currencyState.value
    val newFrom = cur.toCode
    val newTo = cur.fromCode
    _currencyState.update { it.copy(fromCode = newFrom, toCode = newTo) }
    viewModelScope.launch { prefsRepo.setCurrencyPair(newFrom, newTo) }
    loadCurrencyHistory()
  }

  fun setCurrencyChartRange(range: ChartRange) {
    _currencyState.update { it.copy(chartRange = range) }
    loadCurrencyHistory()
  }

  fun loadCurrencyHistory() {
    val state = _currencyState.value
    val seriesKey = "FX_${state.fromCode}_${state.toCode}_${state.chartRange.name}"
    currencyChartJob?.cancel()
    currencyPrefetchJob?.cancel()

    currencyChartJob =
      viewModelScope.launch {
        val cached = marketRepo.getCachedChartPoints(seriesKey)
        if (cached.size >= 2) {
          _currencyState.update { it.copy(chartPoints = cached, isChartLoading = false) }
        } else {
          _currencyState.update { it.copy(isChartLoading = true) }
        }

        val fresh = marketRepo.fetchCurrencyHistory(state.fromCode, state.toCode, state.chartRange)
        if (fresh.size >= 2) {
          _currencyState.update { it.copy(chartPoints = fresh, isChartLoading = false) }
        } else {
          _currencyState.update { it.copy(isChartLoading = false) }
        }

        currencyPrefetchJob =
          viewModelScope.launch {
            marketRepo.prefetchCurrencyRanges(state.fromCode, state.toCode, state.chartRange)
          }
      }
  }

  fun saveCurrentCurrencyConversion() {
    val state = _currencyState.value
    val rates = currencyRatesMap.value
    val fromRate = rates[state.fromCode] ?: 1.0
    val toRate = rates[state.toCode] ?: 1.0
    val unitRate = toRate / fromRate
    val input = state.inputText.toDoubleOrNull() ?: 1.0
    val output = input * unitRate
    viewModelScope.launch {
      marketRepo.saveConversion(state.fromCode, state.toCode, input, output, unitRate)
    }
  }

  fun applySavedConversion(item: SavedConversionEntity) {
    _currencyState.update {
      it.copy(
        fromCode = item.fromCode,
        toCode = item.toCode,
        inputText = item.inputAmount.let { v -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString() },
      )
    }
    viewModelScope.launch { prefsRepo.setCurrencyPair(item.fromCode, item.toCode) }
    loadCurrencyHistory()
  }

  fun deleteSavedConversion(id: Long) {
    viewModelScope.launch { marketRepo.deleteSavedConversion(id) }
  }

  // ---- Gold & Silver Tracker Actions ----
  fun selectMetal(metal: MetalType) {
    _metalsState.update {
      it.copy(
        selectedMetal = metal,
        calculatorPurityFraction = 1.0,
        chartPurityFraction = 1.0,
      )
    }
    loadMetalHistory()
  }

  fun selectCountry(country: CountryMarket) {
    _metalsState.update { it.copy(selectedCountry = country) }
    viewModelScope.launch { prefsRepo.setCountryIso(country.isoCode) }
    loadMetalHistory()
  }

  fun setMetalChartRange(range: ChartRange) {
    _metalsState.update { it.copy(chartRange = range) }
    loadMetalHistory()
  }

  fun setMetalChartUnit(unit: MetalChartUnit) {
    _metalsState.update { it.copy(chartUnit = unit) }
  }

  fun setMetalChartPurity(fraction: Double) {
    _metalsState.update { it.copy(chartPurityFraction = fraction) }
  }

  fun loadMetalHistory() {
    val state = _metalsState.value
    val metalCode = state.selectedMetal.code
    val currencyCode = state.selectedCountry.currencyCode
    val seriesKey = "METAL_${metalCode}_${currencyCode}_${state.chartRange.name}"
    metalChartJob?.cancel()
    metalPrefetchJob?.cancel()

    metalChartJob =
      viewModelScope.launch {
        val cached = marketRepo.getCachedChartPoints(seriesKey)
        if (cached.size >= 2) {
          _metalsState.update { it.copy(chartPoints = cached, isChartLoading = false) }
        } else {
          _metalsState.update { it.copy(isChartLoading = true) }
        }

        val fresh =
          marketRepo.fetchMetalHistory(
            metalCode = metalCode,
            currencyCode = currencyCode,
            range = state.chartRange,
          )
        if (fresh.size >= 2) {
          _metalsState.update { it.copy(chartPoints = fresh, isChartLoading = false) }
        } else {
          _metalsState.update { it.copy(isChartLoading = false) }
        }

        metalPrefetchJob =
          viewModelScope.launch {
            marketRepo.prefetchMetalRanges(metalCode, currencyCode, state.chartRange)
          }
      }
  }

  fun updateBullionGrams(text: String) {
    if (text.isEmpty() || text == "." || text.toDoubleOrNull() != null) {
      _metalsState.update { it.copy(calculatorGramsText = text) }
    }
  }

  fun setBullionPurity(fraction: Double) {
    _metalsState.update { it.copy(calculatorPurityFraction = fraction) }
  }
}
