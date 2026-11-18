package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_currency_rates")
data class CachedCurrencyRateEntity(
  @PrimaryKey val code: String,
  val rateFromUsd: Double,
  val updatedAtMillis: Long,
  val isLiveSynced: Boolean,
)

@Entity(tableName = "cached_metal_prices")
data class CachedMetalPriceEntity(
  @PrimaryKey val metalCode: String, // "XAU" or "XAG"
  val priceUsdPerOz: Double,
  val change24hPercent: Double,
  val updatedAtMillis: Long,
  val isLiveSynced: Boolean,
)

@Entity(tableName = "cached_chart_points")
data class CachedChartPointEntity(
  @PrimaryKey val id: String, // "${seriesKey}_${index}"
  val seriesKey: String,
  val pointIndex: Int,
  val timestampMillis: Long,
  val value: Double,
  val label: String,
)

@Entity(tableName = "saved_conversions")
data class SavedConversionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val fromCode: String,
  val toCode: String,
  val inputAmount: Double,
  val outputAmount: Double,
  val rateSnapshot: Double,
  val createdAtMillis: Long,
)
