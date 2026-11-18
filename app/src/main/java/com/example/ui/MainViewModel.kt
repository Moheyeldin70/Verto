package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AppLanguage
import com.example.data.local.CachedChartPointEntity
import com.example.data.local.SavedConversionEntity
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.NetworkModule
import com.example.data.repository.ChartRange
import com.example.data.repository.MarketRepository
import com.example.data.repository.SyncStatus
import com.example.domain.CountryMarket
import com.example.domain.CountryMetalsCatalog
import com.example.domain.CurrencyCatalog
import com.example.domain.UnitCatalog
import com.example.domain.UnitCategory
import com.example.domain.UnitDef
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab {
  UNITS,
  CURRENCY,
  METALS,
}

enum class UnitChartMode {
  BARS,
  CURVE,
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
  val chartMode: UnitChartMode = UnitChartMode.BARS,
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

  private val _activeTab = MutableStateFlow(AppTab.UNITS)
  val activeTab: StateFlow<AppTab> = _activeTab

  private val _unitState = MutableStateFlow(UnitConverterState())
  val unitState: StateFlow<UnitConverterState> = _unitState

  private val _currencyState = MutableStateFlow(CurrencyConverterState())
  val currencyState: StateFlow<CurrencyConverterState> = _currencyState

  private val _metalsState = MutableStateFlow(MetalsTrackerState())
  val metalsState: StateFlow<MetalsTrackerState> = _metalsState

  private val _isSyncing = MutableStateFlow(false)

  val userPreferences =
    prefsRepo.preferences.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      com.example.data.local.UserPreferences(),
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

    // Observe saved user preferences for currency & country
    viewModelScope.launch {
      prefsRepo.preferences.collect { prefs ->
        _currencyState.update { cur ->
          if (cur.fromCode != prefs.fromCurrency || cur.toCode != prefs.toCurrency) {
            cur.copy(fromCode = prefs.fromCurrency, toCode = prefs.toCurrency)
          } else cur
        }
        val country = CountryMetalsCatalog.find(prefs.selectedCountryIso)
        _metalsState.update { m ->
          if (m.selectedCountry.isoCode != country.isoCode) {
            m.copy(selectedCountry = country)
          } else m
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

  fun selectTab(tab: AppTab) {
    _activeTab.value = tab
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { prefsRepo.setThemeMode(mode) }
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch { prefsRepo.setLanguage(language) }
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
    val pair = UnitCatalog.defaultPair(category)
    _unitState.update {
      it.copy(category = category, fromUnit = pair.first, toUnit = pair.second)
    }
  }

  fun setFromUnit(unit: UnitDef) {
    _unitState.update { it.copy(fromUnit = unit) }
  }

  fun setToUnit(unit: UnitDef) {
    _unitState.update { it.copy(toUnit = unit) }
  }

  fun swapUnits() {
    _unitState.update { it.copy(fromUnit = it.toUnit, toUnit = it.fromUnit) }
  }

  fun updateUnitInput(text: String) {
    if (text.isEmpty() || text == "-" || text == "." || text == "-." || text.toDoubleOrNull() != null) {
      _unitState.update { it.copy(inputText = text) }
    }
  }

  fun setUnitChartMode(mode: UnitChartMode) {
    _unitState.update { it.copy(chartMode = mode) }
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
    viewModelScope.launch {
      _currencyState.update { it.copy(isChartLoading = true) }
      val points = marketRepo.fetchCurrencyHistory(state.fromCode, state.toCode, state.chartRange)
      _currencyState.update { it.copy(chartPoints = points, isChartLoading = false) }
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
      it.copy(selectedMetal = metal, calculatorPurityFraction = 1.0)
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

  fun loadMetalHistory() {
    val state = _metalsState.value
    viewModelScope.launch {
      _metalsState.update { it.copy(isChartLoading = true) }
      val points =
        marketRepo.fetchMetalHistory(
          metalCode = state.selectedMetal.code,
          currencyCode = state.selectedCountry.currencyCode,
          range = state.chartRange,
        )
      _metalsState.update { it.copy(chartPoints = points, isChartLoading = false) }
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
