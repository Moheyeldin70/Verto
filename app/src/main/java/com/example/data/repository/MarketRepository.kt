package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.CachedChartPointEntity
import com.example.data.local.CachedCurrencyRateEntity
import com.example.data.local.CachedMetalPriceEntity
import com.example.data.local.ConverterDao
import com.example.data.local.SavedConversionEntity
import com.example.data.remote.MarketApiService
import com.example.domain.CurrencyCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ChartRange(val days: Int) {
  D7(7),
  M1(30),
  M3(90),
  Y1(365),
}

data class SyncStatus(
  val isLive: Boolean,
  val isSyncing: Boolean,
  val isNetworkAvailable: Boolean,
  val lastUpdatedMillis: Long,
)

class MarketRepository(
  private val context: Context,
  private val dao: ConverterDao,
  private val api: MarketApiService,
) {
  val currencyRatesFlow: Flow<List<CachedCurrencyRateEntity>> = dao.observeCurrencyRates()
  val metalPricesFlow: Flow<List<CachedMetalPriceEntity>> = dao.observeMetalPrices()
  val savedConversionsFlow: Flow<List<SavedConversionEntity>> = dao.observeSavedConversions()

  fun observeChartPoints(seriesKey: String): Flow<List<CachedChartPointEntity>> =
    dao.observeChartPoints(seriesKey)

  fun isNetworkAvailable(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  suspend fun ensureOfflineBaselineSeeded() =
    withContext(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      if (dao.countCurrencyRates() == 0) {
        val baseline =
          CurrencyCatalog.all.map { info ->
            CachedCurrencyRateEntity(
              code = info.code,
              rateFromUsd = info.fallbackRateFromUsd,
              updatedAtMillis = now,
              isLiveSynced = false,
            )
          }
        dao.upsertCurrencyRates(baseline)
      }
      if (dao.countMetalPrices() == 0) {
        dao.upsertMetalPrices(
          listOf(
            CachedMetalPriceEntity("XAU", 2635.0, 0.0, now, isLiveSynced = false),
            CachedMetalPriceEntity("XAG", 31.20, 0.0, now, isLiveSynced = false),
          )
        )
      }
    }

  suspend fun refreshLiveRates(): Boolean =
    withContext(Dispatchers.IO) {
      if (!isNetworkAvailable()) return@withContext false
      val now = System.currentTimeMillis()
      var fxSuccess = false
      var metalsSuccess = false

      // 1. Fetch all 160+ world currencies
      val mergedRates = mutableMapOf<String, Double>()
      try {
        val er = api.getErApiLatestRates()
        er.rates?.forEach { (code, rate) ->
          if (rate > 0.0) mergedRates[code.uppercase()] = rate
        }
        if (mergedRates.isNotEmpty()) fxSuccess = true
      } catch (_: Exception) {
        // Fallback to fawazahmed0 currency-api below
      }

      // 2. Also fetch fawazahmed0 latest USD rates (covers metals XAU/XAG + extra currencies)
      var fawazXauUsd: Double? = null
      var fawazXagUsd: Double? = null
      try {
        val body =
          api.getRawJson(
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"
          )
        val root = JSONObject(body.string())
        val usdObj = root.optJSONObject("usd")
        if (usdObj != null) {
          val keys = usdObj.keys()
          while (keys.hasNext()) {
            val k = keys.next()
            val v = usdObj.optDouble(k, -1.0)
            if (v > 0.0) {
              val upper = k.uppercase()
              if (upper == "XAU") {
                fawazXauUsd = 1.0 / v
              } else if (upper == "XAG") {
                fawazXagUsd = 1.0 / v
              } else if (!mergedRates.containsKey(upper)) {
                mergedRates[upper] = v
              }
            }
          }
          if (mergedRates.isNotEmpty()) fxSuccess = true
        }
      } catch (_: Exception) {
        // Ignore if CDN fails
      }

      if (mergedRates.isNotEmpty()) {
        mergedRates["USD"] = 1.0
        val entities =
          mergedRates.map { (code, rate) ->
            CachedCurrencyRateEntity(
              code = code,
              rateFromUsd = rate,
              updatedAtMillis = now,
              isLiveSynced = true,
            )
          }
        dao.upsertCurrencyRates(entities)
      }

      // 3. Fetch real-time Gold & Silver spot prices (with 24h change when available)
      var xauPrice: Double? = null
      var xagPrice: Double? = null
      var xauPc = 0.0
      var xagPc = 0.0

      try {
        val org = api.getGoldPriceOrgRates()
        val item = org.items?.firstOrNull()
        if (item != null && (item.xauPrice ?: 0.0) > 0.0 && (item.xagPrice ?: 0.0) > 0.0) {
          xauPrice = item.xauPrice
          xagPrice = item.xagPrice
          xauPc = item.pcXau ?: 0.0
          xagPc = item.pcXag ?: 0.0
        }
      } catch (_: Exception) {
        // Fall through to gold-api
      }

      if (xauPrice == null || xagPrice == null) {
        try {
          coroutineScope {
            val gDeferred = async { api.getGoldApiSpot("XAU") }
            val sDeferred = async { api.getGoldApiSpot("XAG") }
            val g = gDeferred.await()
            val s = sDeferred.await()
            if ((g.price ?: 0.0) > 0.0) xauPrice = g.price
            if ((s.price ?: 0.0) > 0.0) xagPrice = s.price
          }
        } catch (_: Exception) {
          // Fall through to fawazahmed0 spot
        }
      }

      val finalXau = xauPrice ?: fawazXauUsd
      val finalXag = xagPrice ?: fawazXagUsd

      if (finalXau != null && finalXag != null && finalXau > 0.0 && finalXag > 0.0) {
        dao.upsertMetalPrices(
          listOf(
            CachedMetalPriceEntity("XAU", finalXau, xauPc, now, isLiveSynced = true),
            CachedMetalPriceEntity("XAG", finalXag, xagPc, now, isLiveSynced = true),
          )
        )
        metalsSuccess = true
      }

      fxSuccess || metalsSuccess
    }

  suspend fun fetchCurrencyHistory(from: String, to: String, range: ChartRange): List<CachedChartPointEntity> =
    withContext(Dispatchers.IO) {
      val seriesKey = "FX_${from}_${to}_${range.name}"
      if (from == to) {
        val flat = buildFlatSeries(seriesKey, 1.0, range)
        dao.replaceChartSeries(seriesKey, flat)
        return@withContext flat
      }
      if (!isNetworkAvailable()) {
        return@withContext dao.getChartPoints(seriesKey)
      }

      val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val labelFmt = SimpleDateFormat("MMM d", Locale.US)
      val cal = Calendar.getInstance()
      val endDate = dateFmt.format(cal.time)
      cal.add(Calendar.DAY_OF_YEAR, -range.days)
      val startDate = dateFmt.format(cal.time)

      // Try Frankfurter first (fast single call for major currencies)
      try {
        val resp = api.getFrankfurterSeries("${startDate}..${endDate}", from, to)
        val ratesMap = resp.rates
        if (!ratesMap.isNullOrEmpty()) {
          val sorted = ratesMap.entries.sortedBy { it.key }
          val points =
            sorted.mapIndexedNotNull { idx, entry ->
              val v = entry.value[to] ?: return@mapIndexedNotNull null
              val parsedDate = runCatching { dateFmt.parse(entry.key) }.getOrNull()
              CachedChartPointEntity(
                id = "${seriesKey}_$idx",
                seriesKey = seriesKey,
                pointIndex = idx,
                timestampMillis = parsedDate?.time ?: System.currentTimeMillis(),
                value = v,
                label = parsedDate?.let { labelFmt.format(it) } ?: entry.key,
              )
            }
          if (points.size >= 2) {
            dao.replaceChartSeries(seriesKey, points)
            return@withContext points
          }
        }
      } catch (_: Exception) {
        // Fallback to sampled dated snapshots for all 165+ currencies
      }

      val sampled = fetchDatedCurrencySnapshots(seriesKey, from, to, range)
      if (sampled.size >= 2) {
        dao.replaceChartSeries(seriesKey, sampled)
        return@withContext sampled
      }

      dao.getChartPoints(seriesKey)
    }

  suspend fun fetchMetalHistory(
    metalCode: String, // "XAU" or "XAG"
    currencyCode: String,
    range: ChartRange,
  ): List<CachedChartPointEntity> =
    withContext(Dispatchers.IO) {
      val seriesKey = "METAL_${metalCode}_${currencyCode}_${range.name}"
      if (!isNetworkAvailable()) {
        return@withContext dao.getChartPoints(seriesKey)
      }

      val points = fetchDatedMetalSnapshots(seriesKey, metalCode, currencyCode, range)
      if (points.size >= 2) {
        dao.replaceChartSeries(seriesKey, points)
        return@withContext points
      }

      dao.getChartPoints(seriesKey)
    }

  private suspend fun fetchDatedCurrencySnapshots(
    seriesKey: String,
    from: String,
    to: String,
    range: ChartRange,
  ): List<CachedChartPointEntity> = coroutineScope {
    val dates = sampleDates(range, count = 12)
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt = SimpleDateFormat("MMM d", Locale.US)
    val fromKey = from.lowercase()
    val toKey = to.lowercase()

    val results =
      dates.mapIndexed { idx, date ->
        async {
          val dateStr = dateFmt.format(date)
          val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$dateStr/v1/currencies/usd.json"
          runCatching {
            val raw = api.getRawJson(url).string()
            val usd = JSONObject(raw).optJSONObject("usd") ?: return@runCatching null
            val rFrom = if (fromKey == "usd") 1.0 else usd.optDouble(fromKey, -1.0)
            val rTo = if (toKey == "usd") 1.0 else usd.optDouble(toKey, -1.0)
            if (rFrom > 0.0 && rTo > 0.0) {
              CachedChartPointEntity(
                id = "${seriesKey}_$idx",
                seriesKey = seriesKey,
                pointIndex = idx,
                timestampMillis = date.time,
                value = rTo / rFrom,
                label = labelFmt.format(date),
              )
            } else null
          }.getOrNull()
        }
      }.awaitAll()

    results.filterNotNull().mapIndexed { i, p -> p.copy(id = "${seriesKey}_$i", pointIndex = i) }
  }

  private suspend fun fetchDatedMetalSnapshots(
    seriesKey: String,
    metalCode: String,
    currencyCode: String,
    range: ChartRange,
  ): List<CachedChartPointEntity> = coroutineScope {
    val dates = sampleDates(range, count = 12)
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt = SimpleDateFormat("MMM d", Locale.US)
    val metalKey = metalCode.lowercase()
    val currKey = currencyCode.lowercase()

    val results =
      dates.mapIndexed { idx, date ->
        async {
          val dateStr = dateFmt.format(date)
          val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$dateStr/v1/currencies/usd.json"
          runCatching {
            val raw = api.getRawJson(url).string()
            val usd = JSONObject(raw).optJSONObject("usd") ?: return@runCatching null
            val metalPerUsd = usd.optDouble(metalKey, -1.0)
            val currPerUsd = if (currKey == "usd") 1.0 else usd.optDouble(currKey, -1.0)
            if (metalPerUsd > 0.0 && currPerUsd > 0.0) {
              val pricePerOzInCurrency = (1.0 / metalPerUsd) * currPerUsd
              CachedChartPointEntity(
                id = "${seriesKey}_$idx",
                seriesKey = seriesKey,
                pointIndex = idx,
                timestampMillis = date.time,
                value = pricePerOzInCurrency,
                label = labelFmt.format(date),
              )
            } else null
          }.getOrNull()
        }
      }.awaitAll()

    results.filterNotNull().mapIndexed { i, p -> p.copy(id = "${seriesKey}_$i", pointIndex = i) }
  }

  private fun sampleDates(range: ChartRange, count: Int): List<java.util.Date> {
    val cal = Calendar.getInstance()
    // End at yesterday so historical CDN snapshots are reliably published
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val endMillis = cal.timeInMillis
    val stepMillis = (range.days.toLong() * 86_400_000L) / (count - 1).coerceAtLeast(1)
    val startMillis = endMillis - (range.days.toLong() * 86_400_000L)
    return (0 until count).map { i -> java.util.Date(startMillis + i * stepMillis) }
  }

  private fun buildFlatSeries(
    seriesKey: String,
    value: Double,
    range: ChartRange,
  ): List<CachedChartPointEntity> {
    val labelFmt = SimpleDateFormat("MMM d", Locale.US)
    return sampleDates(range, 8).mapIndexed { idx, date ->
      CachedChartPointEntity(
        id = "${seriesKey}_$idx",
        seriesKey = seriesKey,
        pointIndex = idx,
        timestampMillis = date.time,
        value = value,
        label = labelFmt.format(date),
      )
    }
  }

  suspend fun saveConversion(
    fromCode: String,
    toCode: String,
    inputAmount: Double,
    outputAmount: Double,
    rateSnapshot: Double,
  ) {
    withContext(Dispatchers.IO) {
      dao.insertSavedConversion(
        SavedConversionEntity(
          fromCode = fromCode,
          toCode = toCode,
          inputAmount = inputAmount,
          outputAmount = outputAmount,
          rateSnapshot = rateSnapshot,
          createdAtMillis = System.currentTimeMillis(),
        )
      )
    }
  }

  suspend fun deleteSavedConversion(id: Long) {
    withContext(Dispatchers.IO) { dao.deleteSavedConversion(id) }
  }
}
