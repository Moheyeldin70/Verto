package com.example.domain

enum class MeasurementSystem {
  METRIC,
  IMPERIAL_US,
  UK,
}

enum class MeasurementLocalePreference(val key: String) {
  AUTO("AUTO"),
  METRIC("METRIC"),
  IMPERIAL_US("IMPERIAL_US"),
  UK("UK");

  companion object {
    fun fromKey(key: String?): MeasurementLocalePreference =
      entries.find { it.key == key } ?: AUTO
  }
}

object LocationLocaleResolver {
  private val usImperialCountries = setOf("US", "LR", "MM")
  private val ukMixedCountries = setOf("GB", "UK")

  fun measurementSystemForCountry(countryIso: String): MeasurementSystem {
    val upper = countryIso.uppercase()
    return when {
      upper in usImperialCountries -> MeasurementSystem.IMPERIAL_US
      upper in ukMixedCountries -> MeasurementSystem.UK
      else -> MeasurementSystem.METRIC
    }
  }

  fun resolveMeasurementSystem(
    preference: MeasurementLocalePreference,
    countryIso: String,
  ): MeasurementSystem {
    return when (preference) {
      MeasurementLocalePreference.AUTO -> measurementSystemForCountry(countryIso)
      MeasurementLocalePreference.METRIC -> MeasurementSystem.METRIC
      MeasurementLocalePreference.IMPERIAL_US -> MeasurementSystem.IMPERIAL_US
      MeasurementLocalePreference.UK -> MeasurementSystem.UK
    }
  }

  fun defaultUnitPair(
    category: UnitCategory,
    system: MeasurementSystem,
  ): Pair<UnitDef, UnitDef> {
    val units = UnitCatalog.unitsFor(category)
    fun findOr(id: String, fallbackIdx: Int): UnitDef =
      units.find { it.id == id } ?: units.getOrElse(fallbackIdx) { units.first() }

    return when (system) {
      MeasurementSystem.IMPERIAL_US ->
        when (category) {
          UnitCategory.LENGTH -> findOr("mi", 0) to findOr("km", 1)
          UnitCategory.MASS -> findOr("lb", 0) to findOr("kg", 1)
          UnitCategory.TEMPERATURE -> findOr("f", 0) to findOr("c", 1)
          UnitCategory.AREA -> findOr("ft2", 0) to findOr("m2", 1)
          UnitCategory.VOLUME -> findOr("gal_us", 0) to findOr("l", 1)
          UnitCategory.SPEED -> findOr("mph", 0) to findOr("kmh", 1)
          UnitCategory.PRESSURE -> findOr("psi", 0) to findOr("bar", 1)
          UnitCategory.FUEL -> findOr("mpg_us", 0) to findOr("l100km", 1)
          else -> UnitCatalog.defaultPair(category)
        }

      MeasurementSystem.UK ->
        when (category) {
          UnitCategory.LENGTH -> findOr("mi", 0) to findOr("km", 1)
          UnitCategory.MASS -> findOr("st", 0) to findOr("kg", 1)
          UnitCategory.TEMPERATURE -> findOr("c", 0) to findOr("f", 1)
          UnitCategory.AREA -> findOr("ft2", 0) to findOr("m2", 1)
          UnitCategory.VOLUME -> findOr("gal_uk", 0) to findOr("l", 1)
          UnitCategory.SPEED -> findOr("mph", 0) to findOr("kmh", 1)
          UnitCategory.PRESSURE -> findOr("bar", 0) to findOr("psi", 1)
          UnitCategory.FUEL -> findOr("mpg_uk", 0) to findOr("l100km", 1)
          else -> UnitCatalog.defaultPair(category)
        }

      MeasurementSystem.METRIC -> UnitCatalog.defaultPair(category)
    }
  }

  fun defaultCurrencyPairForCountry(countryIso: String): Pair<String, String> {
    val market = CountryMetalsCatalog.find(countryIso)
    val target = market.currencyCode
    val base = if (target.equals("USD", ignoreCase = true)) "EUR" else "USD"
    return base to target
  }

  fun defaultCountryMarket(countryIso: String): CountryMarket =
    CountryMetalsCatalog.find(countryIso)
}
