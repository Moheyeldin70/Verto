package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ConverterDao {
  @Query("SELECT * FROM cached_currency_rates ORDER BY code ASC")
  fun observeCurrencyRates(): Flow<List<CachedCurrencyRateEntity>>

  @Query("SELECT COUNT(*) FROM cached_currency_rates")
  suspend fun countCurrencyRates(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCurrencyRates(rates: List<CachedCurrencyRateEntity>)

  @Query("SELECT * FROM cached_metal_prices")
  fun observeMetalPrices(): Flow<List<CachedMetalPriceEntity>>

  @Query("SELECT COUNT(*) FROM cached_metal_prices")
  suspend fun countMetalPrices(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMetalPrices(prices: List<CachedMetalPriceEntity>)

  @Query("SELECT * FROM cached_chart_points WHERE seriesKey = :seriesKey ORDER BY pointIndex ASC")
  fun observeChartPoints(seriesKey: String): Flow<List<CachedChartPointEntity>>

  @Query("SELECT * FROM cached_chart_points WHERE seriesKey = :seriesKey ORDER BY pointIndex ASC")
  suspend fun getChartPoints(seriesKey: String): List<CachedChartPointEntity>

  @Query("DELETE FROM cached_chart_points WHERE seriesKey = :seriesKey")
  suspend fun deleteChartSeries(seriesKey: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertChartPoints(points: List<CachedChartPointEntity>)

  @Transaction
  suspend fun replaceChartSeries(seriesKey: String, points: List<CachedChartPointEntity>) {
    deleteChartSeries(seriesKey)
    insertChartPoints(points)
  }

  @Query("SELECT * FROM saved_conversions ORDER BY createdAtMillis DESC")
  fun observeSavedConversions(): Flow<List<SavedConversionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSavedConversion(item: SavedConversionEntity)

  @Query("DELETE FROM saved_conversions WHERE id = :id")
  suspend fun deleteSavedConversion(id: Long)
}
