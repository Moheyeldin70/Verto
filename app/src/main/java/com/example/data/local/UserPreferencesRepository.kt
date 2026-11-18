package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
  val fromCurrency: String = "USD",
  val toCurrency: String = "EUR",
  val selectedCountryIso: String = "US",
)

class UserPreferencesRepository(private val context: Context) {
  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LANGUAGE = stringPreferencesKey("language_code")
    val FROM_CURRENCY = stringPreferencesKey("from_currency")
    val TO_CURRENCY = stringPreferencesKey("to_currency")
    val COUNTRY_ISO = stringPreferencesKey("country_iso")
  }

  val preferences: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      UserPreferences(
        themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
        language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
        fromCurrency = prefs[Keys.FROM_CURRENCY] ?: "USD",
        toCurrency = prefs[Keys.TO_CURRENCY] ?: "EUR",
        selectedCountryIso = prefs[Keys.COUNTRY_ISO] ?: "US",
      )
    }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
  }

  suspend fun setCurrencyPair(from: String, to: String) {
    context.dataStore.edit {
      it[Keys.FROM_CURRENCY] = from
      it[Keys.TO_CURRENCY] = to
    }
  }

  suspend fun setCountryIso(iso: String) {
    context.dataStore.edit { it[Keys.COUNTRY_ISO] = iso }
  }
}
