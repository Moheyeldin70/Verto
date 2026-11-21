package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.MeasurementLocalePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "verto_prefs")

enum class ThemeMode(val key: String) {
  SYSTEM("SYSTEM"),
  LIGHT("LIGHT"),
  DARK("DARK");

  companion object {
    fun fromKey(key: String?): ThemeMode = entries.find { it.key == key } ?: SYSTEM
  }
}

enum class NavBarPosition(val key: String) {
  BOTTOM("BOTTOM"),
  TOP("TOP"),
  LEFT("LEFT"),
  RIGHT("RIGHT");

  companion object {
    fun fromKey(key: String?): NavBarPosition = entries.find { it.key == key } ?: BOTTOM
  }
}

enum class StartScreenPreference(val key: String) {
  UNITS("UNITS"),
  CURRENCY("CURRENCY"),
  METALS("METALS"),
  LAST_OPENED("LAST_OPENED");

  companion object {
    fun fromKey(key: String?): StartScreenPreference = entries.find { it.key == key } ?: UNITS
  }
}

enum class AppLanguage(val code: String, val nativeLabel: String, val flag: String) {
  SYSTEM("system", "System", "🌐"),
  ENGLISH("en", "English", "🇺🇸"),
  ARABIC("ar", "العربية", "🇸🇦"),
  JAPANESE("ja", "日本語", "🇯🇵"),
  CHINESE("zh", "中文 (简体)", "🇨🇳"),
  SPANISH("es", "Español", "🇪🇸"),
  FRENCH("fr", "Français", "🇫🇷"),
  GERMAN("de", "Deutsch", "🇩🇪"),
  HINDI("hi", "हिन्दी", "🇮🇳"),
  TURKISH("tr", "Türkçe", "🇹🇷"),
  KOREAN("ko", "한국어", "🇰🇷");

  companion object {
    fun fromCode(code: String?): AppLanguage = entries.find { it.code == code } ?: SYSTEM
  }
}

data class UserPreferences(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val language: AppLanguage = AppLanguage.SYSTEM,
  val navBarPosition: NavBarPosition = NavBarPosition.BOTTOM,
  val startScreen: StartScreenPreference = StartScreenPreference.UNITS,
  val lastActiveTab: String = "UNITS",
  val fromCurrency: String = "USD",
  val toCurrency: String = "EUR",
  val selectedCountryIso: String = "US",
  val detectedCountryIso: String = "",
  val measurementLocale: MeasurementLocalePreference = MeasurementLocalePreference.AUTO,
  val hasPromptedLocation: Boolean = false,
  val hasManualUnits: Boolean = false,
  val hasManualCurrency: Boolean = false,
  val hasManualCountry: Boolean = false,
  val unitCategoryName: String = "",
  val fromUnitId: String = "",
  val toUnitId: String = "",
)

class UserPreferencesRepository(private val context: Context) {
  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LANGUAGE = stringPreferencesKey("language_code")
    val NAV_BAR_POSITION = stringPreferencesKey("nav_bar_position")
    val START_SCREEN = stringPreferencesKey("start_screen")
    val LAST_ACTIVE_TAB = stringPreferencesKey("last_active_tab")
    val FROM_CURRENCY = stringPreferencesKey("from_currency")
    val TO_CURRENCY = stringPreferencesKey("to_currency")
    val COUNTRY_ISO = stringPreferencesKey("country_iso")
    val DETECTED_COUNTRY_ISO = stringPreferencesKey("detected_country_iso")
    val MEASUREMENT_LOCALE = stringPreferencesKey("measurement_locale")
    val HAS_PROMPTED_LOCATION = booleanPreferencesKey("has_prompted_location")
    val HAS_MANUAL_UNITS = booleanPreferencesKey("has_manual_units")
    val HAS_MANUAL_CURRENCY = booleanPreferencesKey("has_manual_currency")
    val HAS_MANUAL_COUNTRY = booleanPreferencesKey("has_manual_country")
    val UNIT_CATEGORY = stringPreferencesKey("unit_category")
    val FROM_UNIT_ID = stringPreferencesKey("from_unit_id")
    val TO_UNIT_ID = stringPreferencesKey("to_unit_id")
  }

  val preferences: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      UserPreferences(
        themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
        language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
        navBarPosition = NavBarPosition.fromKey(prefs[Keys.NAV_BAR_POSITION]),
        startScreen = StartScreenPreference.fromKey(prefs[Keys.START_SCREEN]),
        lastActiveTab = prefs[Keys.LAST_ACTIVE_TAB] ?: "UNITS",
        fromCurrency = prefs[Keys.FROM_CURRENCY] ?: "USD",
        toCurrency = prefs[Keys.TO_CURRENCY] ?: "EUR",
        selectedCountryIso = prefs[Keys.COUNTRY_ISO] ?: "US",
        detectedCountryIso = prefs[Keys.DETECTED_COUNTRY_ISO] ?: "",
        measurementLocale = MeasurementLocalePreference.fromKey(prefs[Keys.MEASUREMENT_LOCALE]),
        hasPromptedLocation = prefs[Keys.HAS_PROMPTED_LOCATION] ?: false,
        hasManualUnits = prefs[Keys.HAS_MANUAL_UNITS] ?: false,
        hasManualCurrency = prefs[Keys.HAS_MANUAL_CURRENCY] ?: false,
        hasManualCountry = prefs[Keys.HAS_MANUAL_COUNTRY] ?: false,
        unitCategoryName = prefs[Keys.UNIT_CATEGORY] ?: "",
        fromUnitId = prefs[Keys.FROM_UNIT_ID] ?: "",
        toUnitId = prefs[Keys.TO_UNIT_ID] ?: "",
      )
    }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
  }

  suspend fun setNavBarPosition(position: NavBarPosition) {
    context.dataStore.edit { it[Keys.NAV_BAR_POSITION] = position.key }
  }

  suspend fun setStartScreen(startScreen: StartScreenPreference) {
    context.dataStore.edit { it[Keys.START_SCREEN] = startScreen.key }
  }

  suspend fun setLastActiveTab(tabName: String) {
    context.dataStore.edit { it[Keys.LAST_ACTIVE_TAB] = tabName }
  }

  suspend fun setCurrencyPair(from: String, to: String, isManual: Boolean = true) {
    context.dataStore.edit {
      it[Keys.FROM_CURRENCY] = from
      it[Keys.TO_CURRENCY] = to
      if (isManual) it[Keys.HAS_MANUAL_CURRENCY] = true
    }
  }

  suspend fun setCountryIso(iso: String, isManual: Boolean = true) {
    context.dataStore.edit {
      it[Keys.COUNTRY_ISO] = iso
      if (isManual) it[Keys.HAS_MANUAL_COUNTRY] = true
    }
  }

  suspend fun setUnitSelection(
    categoryName: String,
    fromUnitId: String,
    toUnitId: String,
    isManual: Boolean = true,
  ) {
    context.dataStore.edit {
      it[Keys.UNIT_CATEGORY] = categoryName
      it[Keys.FROM_UNIT_ID] = fromUnitId
      it[Keys.TO_UNIT_ID] = toUnitId
      if (isManual) it[Keys.HAS_MANUAL_UNITS] = true
    }
  }

  suspend fun setMeasurementLocale(preference: MeasurementLocalePreference) {
    context.dataStore.edit {
      it[Keys.MEASUREMENT_LOCALE] = preference.key
      it[Keys.HAS_MANUAL_UNITS] = preference != MeasurementLocalePreference.AUTO
    }
  }

  suspend fun markLocationPrompted() {
    context.dataStore.edit { it[Keys.HAS_PROMPTED_LOCATION] = true }
  }

  suspend fun saveDetectedRegion(
    countryIso: String,
    resetManualOverrides: Boolean,
    defaultFromCurrency: String,
    defaultToCurrency: String,
  ) {
    context.dataStore.edit { prefs ->
      prefs[Keys.DETECTED_COUNTRY_ISO] = countryIso
      prefs[Keys.HAS_PROMPTED_LOCATION] = true

      val applyCurrency = resetManualOverrides || !(prefs[Keys.HAS_MANUAL_CURRENCY] ?: false)
      if (applyCurrency) {
        prefs[Keys.FROM_CURRENCY] = defaultFromCurrency
        prefs[Keys.TO_CURRENCY] = defaultToCurrency
        if (resetManualOverrides) prefs[Keys.HAS_MANUAL_CURRENCY] = false
      }

      val applyCountry = resetManualOverrides || !(prefs[Keys.HAS_MANUAL_COUNTRY] ?: false)
      if (applyCountry) {
        prefs[Keys.COUNTRY_ISO] = countryIso
        if (resetManualOverrides) prefs[Keys.HAS_MANUAL_COUNTRY] = false
      }

      if (resetManualOverrides) {
        prefs[Keys.MEASUREMENT_LOCALE] = MeasurementLocalePreference.AUTO.key
        prefs[Keys.HAS_MANUAL_UNITS] = false
      }
    }
  }
}
