package com.example.domain

import androidx.annotation.StringRes
import com.example.R
import kotlin.math.abs

enum class UnitCategory(@StringRes val labelRes: Int) {
  LENGTH(R.string.cat_length),
  MASS(R.string.cat_mass),
  TEMPERATURE(R.string.cat_temperature),
  AREA(R.string.cat_area),
  VOLUME(R.string.cat_volume),
  SPEED(R.string.cat_speed),
  TIME(R.string.cat_time),
  DATA(R.string.cat_data),
  PRESSURE(R.string.cat_pressure),
  ENERGY(R.string.cat_energy),
  POWER(R.string.cat_power),
  ANGLE(R.string.cat_angle),
  FUEL(R.string.cat_fuel),
}

data class UnitDef(
  val id: String,
  val symbol: String,
  val name: String,
  val category: UnitCategory,
  val toBase: (Double) -> Double,
  val fromBase: (Double) -> Double,
) {
  companion object {
    fun linear(
      id: String,
      symbol: String,
      name: String,
      category: UnitCategory,
      factorToBase: Double,
    ) =
      UnitDef(
        id = id,
        symbol = symbol,
        name = name,
        category = category,
        toBase = { it * factorToBase },
        fromBase = { it / factorToBase },
      )
  }
}

object UnitCatalog {
  val units: List<UnitDef> =
    listOf(
      // Length (base: meter)
      UnitDef.linear("um", "µm", "Micrometer", UnitCategory.LENGTH, 1e-6),
      UnitDef.linear("mm", "mm", "Millimeter", UnitCategory.LENGTH, 0.001),
      UnitDef.linear("cm", "cm", "Centimeter", UnitCategory.LENGTH, 0.01),
      UnitDef.linear("m", "m", "Meter", UnitCategory.LENGTH, 1.0),
      UnitDef.linear("km", "km", "Kilometer", UnitCategory.LENGTH, 1000.0),
      UnitDef.linear("in", "in", "Inch", UnitCategory.LENGTH, 0.0254),
      UnitDef.linear("ft", "ft", "Foot", UnitCategory.LENGTH, 0.3048),
      UnitDef.linear("yd", "yd", "Yard", UnitCategory.LENGTH, 0.9144),
      UnitDef.linear("mi", "mi", "Mile", UnitCategory.LENGTH, 1609.344),
      UnitDef.linear("nmi", "nmi", "Nautical Mile", UnitCategory.LENGTH, 1852.0),

      // Mass (base: kilogram)
      UnitDef.linear("mg", "mg", "Milligram", UnitCategory.MASS, 1e-6),
      UnitDef.linear("g", "g", "Gram", UnitCategory.MASS, 0.001),
      UnitDef.linear("kg", "kg", "Kilogram", UnitCategory.MASS, 1.0),
      UnitDef.linear("t", "t", "Metric Ton", UnitCategory.MASS, 1000.0),
      UnitDef.linear("oz", "oz", "Ounce", UnitCategory.MASS, 0.028349523125),
      UnitDef.linear("ozt", "oz t", "Troy Ounce", UnitCategory.MASS, 0.0311034768),
      UnitDef.linear("lb", "lb", "Pound", UnitCategory.MASS, 0.45359237),
      UnitDef.linear("st", "st", "Stone", UnitCategory.MASS, 6.35029318),
      UnitDef.linear("ct", "ct", "Carat", UnitCategory.MASS, 0.0002),

      // Temperature (base: Celsius)
      UnitDef(
        id = "c",
        symbol = "°C",
        name = "Celsius",
        category = UnitCategory.TEMPERATURE,
        toBase = { it },
        fromBase = { it },
      ),
      UnitDef(
        id = "f",
        symbol = "°F",
        name = "Fahrenheit",
        category = UnitCategory.TEMPERATURE,
        toBase = { (it - 32.0) * 5.0 / 9.0 },
        fromBase = { (it * 9.0 / 5.0) + 32.0 },
      ),
      UnitDef(
        id = "k",
        symbol = "K",
        name = "Kelvin",
        category = UnitCategory.TEMPERATURE,
        toBase = { it - 273.15 },
        fromBase = { it + 273.15 },
      ),
      UnitDef(
        id = "r",
        symbol = "°R",
        name = "Rankine",
        category = UnitCategory.TEMPERATURE,
        toBase = { (it - 491.67) * 5.0 / 9.0 },
        fromBase = { (it + 273.15) * 9.0 / 5.0 },
      ),

      // Area (base: square meter)
      UnitDef.linear("mm2", "mm²", "Square Millimeter", UnitCategory.AREA, 1e-6),
      UnitDef.linear("cm2", "cm²", "Square Centimeter", UnitCategory.AREA, 1e-4),
      UnitDef.linear("m2", "m²", "Square Meter", UnitCategory.AREA, 1.0),
      UnitDef.linear("ha", "ha", "Hectare", UnitCategory.AREA, 10000.0),
      UnitDef.linear("km2", "km²", "Square Kilometer", UnitCategory.AREA, 1_000_000.0),
      UnitDef.linear("in2", "in²", "Square Inch", UnitCategory.AREA, 0.00064516),
      UnitDef.linear("ft2", "ft²", "Square Foot", UnitCategory.AREA, 0.09290304),
      UnitDef.linear("yd2", "yd²", "Square Yard", UnitCategory.AREA, 0.83612736),
      UnitDef.linear("ac", "ac", "Acre", UnitCategory.AREA, 4046.8564224),
      UnitDef.linear("mi2", "mi²", "Square Mile", UnitCategory.AREA, 2_589_988.110336),

      // Volume (base: liter)
      UnitDef.linear("ml", "mL", "Milliliter", UnitCategory.VOLUME, 0.001),
      UnitDef.linear("l", "L", "Liter", UnitCategory.VOLUME, 1.0),
      UnitDef.linear("m3", "m³", "Cubic Meter", UnitCategory.VOLUME, 1000.0),
      UnitDef.linear("tsp", "tsp", "Teaspoon (US)", UnitCategory.VOLUME, 0.00492892159375),
      UnitDef.linear("tbsp", "tbsp", "Tablespoon (US)", UnitCategory.VOLUME, 0.01478676478125),
      UnitDef.linear("floz", "fl oz", "Fluid Ounce (US)", UnitCategory.VOLUME, 0.0295735295625),
      UnitDef.linear("cup", "cup", "Cup (US)", UnitCategory.VOLUME, 0.2365882365),
      UnitDef.linear("pt", "pt", "Pint (US)", UnitCategory.VOLUME, 0.473176473),
      UnitDef.linear("qt", "qt", "Quart (US)", UnitCategory.VOLUME, 0.946352946),
      UnitDef.linear("gal_us", "gal US", "Gallon (US)", UnitCategory.VOLUME, 3.785411784),
      UnitDef.linear("gal_uk", "gal UK", "Gallon (UK)", UnitCategory.VOLUME, 4.54609),

      // Speed (base: m/s)
      UnitDef.linear("mps", "m/s", "Meters/sec", UnitCategory.SPEED, 1.0),
      UnitDef.linear("kmh", "km/h", "Kilometers/hour", UnitCategory.SPEED, 1.0 / 3.6),
      UnitDef.linear("mph", "mph", "Miles/hour", UnitCategory.SPEED, 0.44704),
      UnitDef.linear("kn", "kn", "Knot", UnitCategory.SPEED, 0.514444444444),
      UnitDef.linear("fts", "ft/s", "Feet/sec", UnitCategory.SPEED, 0.3048),
      UnitDef.linear("mach", "Mach", "Mach (Std)", UnitCategory.SPEED, 340.29),

      // Time (base: second)
      UnitDef.linear("ms", "ms", "Millisecond", UnitCategory.TIME, 0.001),
      UnitDef.linear("s", "s", "Second", UnitCategory.TIME, 1.0),
      UnitDef.linear("min", "min", "Minute", UnitCategory.TIME, 60.0),
      UnitDef.linear("h", "h", "Hour", UnitCategory.TIME, 3600.0),
      UnitDef.linear("d", "d", "Day", UnitCategory.TIME, 86400.0),
      UnitDef.linear("wk", "wk", "Week", UnitCategory.TIME, 604800.0),
      UnitDef.linear("mo", "mo", "Month (30d)", UnitCategory.TIME, 2_592_000.0),
      UnitDef.linear("yr", "yr", "Year (365d)", UnitCategory.TIME, 31_536_000.0),

      // Digital Data (base: byte)
      UnitDef.linear("bit", "bit", "Bit", UnitCategory.DATA, 0.125),
      UnitDef.linear("b", "B", "Byte", UnitCategory.DATA, 1.0),
      UnitDef.linear("kb", "KB", "Kilobyte", UnitCategory.DATA, 1000.0),
      UnitDef.linear("kib", "KiB", "Kibibyte", UnitCategory.DATA, 1024.0),
      UnitDef.linear("mb", "MB", "Megabyte", UnitCategory.DATA, 1_000_000.0),
      UnitDef.linear("mib", "MiB", "Mebibyte", UnitCategory.DATA, 1_048_576.0),
      UnitDef.linear("gb", "GB", "Gigabyte", UnitCategory.DATA, 1_000_000_000.0),
      UnitDef.linear("gib", "GiB", "Gibibyte", UnitCategory.DATA, 1_073_741_824.0),
      UnitDef.linear("tb", "TB", "Terabyte", UnitCategory.DATA, 1_000_000_000_000.0),
      UnitDef.linear("pb", "PB", "Petabyte", UnitCategory.DATA, 1_000_000_000_000_000.0),

      // Pressure (base: Pascal)
      UnitDef.linear("pa", "Pa", "Pascal", UnitCategory.PRESSURE, 1.0),
      UnitDef.linear("kpa", "kPa", "Kilopascal", UnitCategory.PRESSURE, 1000.0),
      UnitDef.linear("mpa", "MPa", "Megapascal", UnitCategory.PRESSURE, 1_000_000.0),
      UnitDef.linear("bar", "bar", "Bar", UnitCategory.PRESSURE, 100_000.0),
      UnitDef.linear("atm", "atm", "Atmosphere", UnitCategory.PRESSURE, 101_325.0),
      UnitDef.linear("psi", "psi", "PSI", UnitCategory.PRESSURE, 6894.757293168),
      UnitDef.linear("mmhg", "mmHg", "mmHg (Torr)", UnitCategory.PRESSURE, 133.322368421),

      // Energy (base: Joule)
      UnitDef.linear("j", "J", "Joule", UnitCategory.ENERGY, 1.0),
      UnitDef.linear("kj", "kJ", "Kilojoule", UnitCategory.ENERGY, 1000.0),
      UnitDef.linear("mj", "MJ", "Megajoule", UnitCategory.ENERGY, 1_000_000.0),
      UnitDef.linear("cal", "cal", "Calorie", UnitCategory.ENERGY, 4.184),
      UnitDef.linear("kcal", "kcal", "Kilocalorie", UnitCategory.ENERGY, 4184.0),
      UnitDef.linear("wh", "Wh", "Watt-hour", UnitCategory.ENERGY, 3600.0),
      UnitDef.linear("kwh", "kWh", "Kilowatt-hour", UnitCategory.ENERGY, 3_600_000.0),
      UnitDef.linear("btu", "BTU", "BTU", UnitCategory.ENERGY, 1055.05585262),

      // Power (base: Watt)
      UnitDef.linear("w", "W", "Watt", UnitCategory.POWER, 1.0),
      UnitDef.linear("kw", "kW", "Kilowatt", UnitCategory.POWER, 1000.0),
      UnitDef.linear("mw", "MW", "Megawatt", UnitCategory.POWER, 1_000_000.0),
      UnitDef.linear("hp", "hp", "Horsepower", UnitCategory.POWER, 745.699871582),
      UnitDef.linear("btuh", "BTU/h", "BTU/hour", UnitCategory.POWER, 0.29307107),

      // Angle (base: Degree)
      UnitDef.linear("deg", "°", "Degree", UnitCategory.ANGLE, 1.0),
      UnitDef.linear("rad", "rad", "Radian", UnitCategory.ANGLE, 180.0 / Math.PI),
      UnitDef.linear("grad", "grad", "Gradian", UnitCategory.ANGLE, 0.9),
      UnitDef.linear("arcmin", "′", "Arcminute", UnitCategory.ANGLE, 1.0 / 60.0),
      UnitDef.linear("arcsec", "″", "Arcsecond", UnitCategory.ANGLE, 1.0 / 3600.0),
      UnitDef.linear("turn", "turn", "Turn", UnitCategory.ANGLE, 360.0),

      // Fuel Economy (base: km/L)
      UnitDef(
        id = "kml",
        symbol = "km/L",
        name = "Kilometers / Liter",
        category = UnitCategory.FUEL,
        toBase = { it },
        fromBase = { it },
      ),
      UnitDef.linear("mpg_us", "mpg US", "Miles / Gallon (US)", UnitCategory.FUEL, 0.425143707),
      UnitDef.linear("mpg_uk", "mpg UK", "Miles / Gallon (UK)", UnitCategory.FUEL, 0.35400619),
      UnitDef(
        id = "l100km",
        symbol = "L/100km",
        name = "Liters / 100 km",
        category = UnitCategory.FUEL,
        toBase = { if (abs(it) < 1e-12) 0.0 else 100.0 / it },
        fromBase = { if (abs(it) < 1e-12) 0.0 else 100.0 / it },
      ),
    )

  fun unitsFor(category: UnitCategory): List<UnitDef> = units.filter { it.category == category }

  fun defaultPair(category: UnitCategory): Pair<UnitDef, UnitDef> {
    val list = unitsFor(category)
    return when (category) {
      UnitCategory.LENGTH -> (list.find { it.id == "km" } ?: list[0]) to (list.find { it.id == "mi" } ?: list[1])
      UnitCategory.MASS -> (list.find { it.id == "kg" } ?: list[0]) to (list.find { it.id == "lb" } ?: list[1])
      UnitCategory.TEMPERATURE -> (list.find { it.id == "c" } ?: list[0]) to (list.find { it.id == "f" } ?: list[1])
      UnitCategory.AREA -> (list.find { it.id == "m2" } ?: list[0]) to (list.find { it.id == "ft2" } ?: list[1])
      UnitCategory.VOLUME -> (list.find { it.id == "l" } ?: list[0]) to (list.find { it.id == "gal_us" } ?: list[1])
      UnitCategory.SPEED -> (list.find { it.id == "kmh" } ?: list[0]) to (list.find { it.id == "mph" } ?: list[1])
      UnitCategory.DATA -> (list.find { it.id == "gb" } ?: list[0]) to (list.find { it.id == "gib" } ?: list[1])
      else -> list[0] to list.getOrElse(1) { list[0] }
    }
  }

  fun convert(value: Double, from: UnitDef, to: UnitDef): Double {
    if (from.id == to.id) return value
    val base = from.toBase(value)
    return to.fromBase(base)
  }

  fun describeRelationship(from: UnitDef, to: UnitDef): String {
    if (from.id == to.id) return "1 ${from.symbol} = 1 ${to.symbol}"
    if (from.category == UnitCategory.TEMPERATURE) {
      return when ("${from.id}_${to.id}") {
        "c_f" -> "°F = (°C × 9/5) + 32"
        "f_c" -> "°C = (°F − 32) × 5/9"
        "c_k" -> "K = °C + 273.15"
        "k_c" -> "°C = K − 273.15"
        "f_k" -> "K = (°F − 32) × 5/9 + 273.15"
        "k_f" -> "°F = (K − 273.15) × 9/5 + 32"
        else -> "1 ${from.symbol} → ${FormattingUtils.formatSmart(convert(1.0, from, to))} ${to.symbol}"
      }
    }
    if (from.category == UnitCategory.FUEL && (from.id == "l100km" || to.id == "l100km")) {
      return "L/100km = 100 ÷ (km/L)"
    }
    val oneUnit = convert(1.0, from, to)
    return "1 ${from.symbol} = ${FormattingUtils.formatSmart(oneUnit)} ${to.symbol}"
  }
}
