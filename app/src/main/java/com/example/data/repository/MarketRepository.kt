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
import com.example.domain.HistoricalSeriesParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class ChartRange(val days: Int) {
  D7(7),
  M1(30),
  M3(90),
  Y1(365),
  Y5(1825),
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

  private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val chartMemoryCache = ConcurrentHashMap<String, List<CachedChartPointEntity>>()
  private val usdSnapshotCache = ConcurrentHashMap<String, Map<String, Double>>()
  private val inFlightDateFetches = ConcurrentHashMap<String, Deferred<Map<String, Double>?>>()
  private val latestUsdRatesCache = ConcurrentHashMap<String, Double>()
  private val fiveYearUsdFxCache = ConcurrentHashMap<String, List<Pair<Long, Double>>>()
  private val fiveYearMetalUsdCache = ConcurrentHashMap<String, List<Pair<Long, Double>>>()

  fun observeChartPoints(seriesKey: String): Flow<List<CachedChartPointEntity>> =
    dao.observeChartPoints(seriesKey)

  suspend fun getCachedChartPoints(seriesKey: String): List<CachedChartPointEntity> {
    chartMemoryCache[seriesKey]?.let { if (it.size >= 2) return it }
    return withContext(Dispatchers.IO) {
      val dbPoints = dao.getChartPoints(seriesKey)
      if (dbPoints.size >= 2) {
        chartMemoryCache[seriesKey] = dbPoints
      }
      dbPoints
    }
  }

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
        latestUsdRatesCache.putAll(mergedRates)
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
        chartMemoryCache[seriesKey] = flat
        dao.replaceChartSeries(seriesKey, flat)
        return@withContext flat
      }
      if (!isNetworkAvailable()) {
        return@withContext getCachedChartPoints(seriesKey)
      }

      if (range == ChartRange.Y5) {
        val fiveYear = fetchFiveYearCurrencyHistory(seriesKey, from, to)
        if (fiveYear.size >= 2) {
          chartMemoryCache[seriesKey] = fiveYear
          dao.replaceChartSeries(seriesKey, fiveYear)
          return@withContext fiveYear
        }
        return@withContext getCachedChartPoints(seriesKey)
      }

      // 1. Try Frankfurter 1Y single-shot call and slice short ranges (7D, 1M, 3M, 1Y) at once
      val frankfurterLoaded = tryFetchFrankfurterShortRanges(from, to)
      if (frankfurterLoaded) {
        chartMemoryCache[seriesKey]?.let { if (it.size >= 2) return@withContext it }
      }

      // 2. Fallback to dated jsDelivr snapshots with shared in-memory date cache
      val sampled = fetchDatedCurrencySnapshots(seriesKey, from, to, range)
      if (sampled.size >= 2) {
        chartMemoryCache[seriesKey] = sampled
        dao.replaceChartSeries(seriesKey, sampled)
        return@withContext sampled
      }

      getCachedChartPoints(seriesKey)
    }

  suspend fun fetchMetalHistory(
    metalCode: String, // "XAU" or "XAG"
    currencyCode: String,
    range: ChartRange,
  ): List<CachedChartPointEntity> =
    withContext(Dispatchers.IO) {
      val seriesKey = "METAL_${metalCode}_${currencyCode}_${range.name}"
      if (!isNetworkAvailable()) {
        return@withContext getCachedChartPoints(seriesKey)
      }

      if (range == ChartRange.Y5) {
        val fiveYear = fetchFiveYearMetalHistory(seriesKey, metalCode, currencyCode)
        if (fiveYear.size >= 2) {
          chartMemoryCache[seriesKey] = fiveYear
          dao.replaceChartSeries(seriesKey, fiveYear)
          return@withContext fiveYear
        }
        return@withContext getCachedChartPoints(seriesKey)
      }

      val points = fetchDatedMetalSnapshots(seriesKey, metalCode, currencyCode, range)
      if (points.size >= 2) {
        chartMemoryCache[seriesKey] = points
        dao.replaceChartSeries(seriesKey, points)
        return@withContext points
      }

      getCachedChartPoints(seriesKey)
    }

  suspend fun prefetchCurrencyRanges(from: String, to: String, exclude: ChartRange) =
    withContext(Dispatchers.IO) {
      if (!isNetworkAvailable() || from == to) return@withContext
      val shortRanges = listOf(ChartRange.D7, ChartRange.M1, ChartRange.M3, ChartRange.Y1)
      val allShortCached =
        shortRanges.all { r -> (chartMemoryCache["FX_${from}_${to}_${r.name}"]?.size ?: 0) >= 2 }

      if (!allShortCached) {
        shortRanges
          .filter { it != exclude }
          .forEach { range ->
            val key = "FX_${from}_${to}_${range.name}"
            if ((chartMemoryCache[key]?.size ?: 0) < 2) {
              val pts = fetchDatedCurrencySnapshots(key, from, to, range)
              if (pts.size >= 2) {
                chartMemoryCache[key] = pts
                dao.replaceChartSeries(key, pts)
              }
            }
          }
      }

      if (exclude != ChartRange.Y5) {
        val key5y = "FX_${from}_${to}_${ChartRange.Y5.name}"
        if ((chartMemoryCache[key5y]?.size ?: 0) < 2) {
          val pts = fetchFiveYearCurrencyHistory(key5y, from, to)
          if (pts.size >= 2) {
            chartMemoryCache[key5y] = pts
            dao.replaceChartSeries(key5y, pts)
          }
        }
      }
    }

  suspend fun prefetchMetalRanges(metalCode: String, currencyCode: String, exclude: ChartRange) =
    withContext(Dispatchers.IO) {
      if (!isNetworkAvailable()) return@withContext
      ChartRange.entries
        .filter { it != exclude }
        .forEach { range ->
          val key = "METAL_${metalCode}_${currencyCode}_${range.name}"
          if ((chartMemoryCache[key]?.size ?: 0) < 2) {
            val pts =
              if (range == ChartRange.Y5) {
                fetchFiveYearMetalHistory(key, metalCode, currencyCode)
              } else {
                fetchDatedMetalSnapshots(key, metalCode, currencyCode, range)
              }
            if (pts.size >= 2) {
              chartMemoryCache[key] = pts
              dao.replaceChartSeries(key, pts)
            }
          }
        }
    }

  private suspend fun tryFetchFrankfurterShortRanges(from: String, to: String): Boolean {
    return try {
      val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val labelFmt = SimpleDateFormat("MMM d", Locale.US)
      val cal = Calendar.getInstance()
      val endMillis = cal.timeInMillis
      val endDate = dateFmt.format(cal.time)
      cal.add(Calendar.DAY_OF_YEAR, -ChartRange.Y1.days)
      val startDate = dateFmt.format(cal.time)

      val resp = api.getFrankfurterSeries("${startDate}..${endDate}", from, to)
      val ratesMap = resp.rates ?: return false
      if (ratesMap.isEmpty()) return false

      val allEntries =
        ratesMap.entries
          .sortedBy { it.key }
          .mapNotNull { entry ->
            val v = entry.value[to] ?: return@mapNotNull null
            val parsed = dateFmt.parse(entry.key) ?: return@mapNotNull null
            Triple(parsed.time, labelFmt.format(parsed), v)
          }
      if (allEntries.size < 2) return false

      val shortRanges = listOf(ChartRange.D7, ChartRange.M1, ChartRange.M3, ChartRange.Y1)
      for (range in shortRanges) {
        val cutoff = endMillis - (range.days.toLong() * 86_400_000L)
        val sliced = allEntries.filter { it.first >= cutoff }.let { list ->
          if (list.size >= 2) list else allEntries.takeLast(minOf(5, allEntries.size))
        }
        val key = "FX_${from}_${to}_${range.name}"
        val entities =
          sliced.mapIndexed { idx, (ts, lbl, v) ->
            CachedChartPointEntity(
              id = "${key}_$idx",
              seriesKey = key,
              pointIndex = idx,
              timestampMillis = ts,
              value = v,
              label = lbl,
            )
          }
        chartMemoryCache[key] = entities
        dao.replaceChartSeries(key, entities)
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  private suspend fun fetchFiveYearCurrencyHistory(
    seriesKey: String,
    from: String,
    to: String,
  ): List<CachedChartPointEntity> {
    // 1. Direct 5-year Frankfurter ECB time-series (works for 30+ major currencies)
    val directFrankfurter = fetchFrankfurterPairSeries(from, to, ChartRange.Y5.days)
    if (directFrankfurter.size >= 2) {
      return toFiveYearEntities(seriesKey, directFrankfurter)
    }

    // 2. Cross-rate via USD using Frankfurter / Yahoo Finance v8 / Stooq CSV
    val pairs: List<Pair<Long, Double>> =
      when {
        from.equals("USD", ignoreCase = true) -> fetchFiveYearUsdRateSeries(to)
        to.equals("USD", ignoreCase = true) -> {
          fetchFiveYearUsdRateSeries(from).mapNotNull { (ts, r) ->
            if (r > 0.0) ts to (1.0 / r) else null
          }
        }
        else -> {
          val usdToFrom = fetchFiveYearUsdRateSeries(from)
          val usdToTo = fetchFiveYearUsdRateSeries(to)
          HistoricalSeriesParser.alignCrossRateSeries(usdToFrom, usdToTo)
        }
      }

    if (pairs.size >= 2) {
      return toFiveYearEntities(seriesKey, pairs)
    }
    return emptyList()
  }

  private suspend fun fetchFiveYearMetalHistory(
    seriesKey: String,
    metalCode: String,
    currencyCode: String,
  ): List<CachedChartPointEntity> {
    val metalUsd = fetchFiveYearMetalUsdSeries(metalCode)
    if (metalUsd.size < 2) return emptyList()

    val converted =
      if (currencyCode.equals("USD", ignoreCase = true)) {
        metalUsd
      } else {
        val fxSeries = fetchFiveYearUsdRateSeries(currencyCode)
        val fallbackRate =
          latestUsdRatesCache[currencyCode] ?: CurrencyCatalog.find(currencyCode).fallbackRateFromUsd
        HistoricalSeriesParser.multiplySeriesByFx(metalUsd, fxSeries, fallbackRate)
      }

    return toFiveYearEntities(seriesKey, converted)
  }

  private suspend fun fetchFrankfurterPairSeries(
    from: String,
    to: String,
    daysBack: Int,
  ): List<Pair<Long, Double>> {
    return try {
      val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val cal = Calendar.getInstance()
      val endDate = dateFmt.format(cal.time)
      cal.add(Calendar.DAY_OF_YEAR, -daysBack)
      val startDate = dateFmt.format(cal.time)

      val resp = api.getFrankfurterSeries("${startDate}..${endDate}", from, to)
      val ratesMap = resp.rates ?: return emptyList()
      ratesMap.entries
        .sortedBy { it.key }
        .mapNotNull { entry ->
          val v = entry.value[to] ?: return@mapNotNull null
          val parsed = dateFmt.parse(entry.key) ?: return@mapNotNull null
          if (v > 0.0) parsed.time to v else null
        }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private suspend fun fetchFiveYearUsdRateSeries(currencyCode: String): List<Pair<Long, Double>> {
    val code = currencyCode.uppercase(Locale.US)
    if (code == "USD") return emptyList()
    fiveYearUsdFxCache[code]?.let { if (it.size >= 2) return it }

    val minTs = System.currentTimeMillis() - (ChartRange.Y5.days.toLong() * 86_400_000L)

    // 1. Frankfurter ECB 5Y
    val frankfurter = fetchFrankfurterPairSeries("USD", code, ChartRange.Y5.days)
    if (frankfurter.size >= 2) {
      fiveYearUsdFxCache[code] = frankfurter
      return frankfurter
    }

    // 2. Yahoo Finance v8 explicit USD-base ticker (e.g. USDEGP=X)
    val yahoo =
      runCatching {
        val raw =
          api.getRawJsonWithAgent(
            "https://query1.finance.yahoo.com/v8/finance/chart/USD${code}=X?range=5y&interval=1wk",
          ).string()
        HistoricalSeriesParser.parseYahooChartResponse(raw, minTs)
      }.getOrDefault(emptyList())
    if (yahoo.size >= 2) {
      fiveYearUsdFxCache[code] = yahoo
      return yahoo
    }

    // 3. Stooq weekly FX CSV (e.g. s=usdegp&i=w)
    val stooq =
      runCatching {
        val raw =
          api.getRawJsonWithAgent(
            "https://stooq.com/q/d/l/?s=usd${code.lowercase(Locale.US)}&i=w",
          ).string()
        HistoricalSeriesParser.parseStooqCsv(raw, minTs)
      }.getOrDefault(emptyList())
    if (stooq.size >= 2) {
      fiveYearUsdFxCache[code] = stooq
      return stooq
    }

    return emptyList()
  }

  private suspend fun fetchFiveYearMetalUsdSeries(metalCode: String): List<Pair<Long, Double>> {
    val code = metalCode.uppercase(Locale.US)
    fiveYearMetalUsdCache[code]?.let { if (it.size >= 2) return it }

    val minTs = System.currentTimeMillis() - (ChartRange.Y5.days.toLong() * 86_400_000L)
    val stooqSymbol = if (code == "XAU") "xauusd" else "xagusd"
    val yahooSymbol = if (code == "XAU") "GC=F" else "SI=F"

    // 1. Stooq 5Y weekly spot XAUUSD / XAGUSD
    val stooq =
      runCatching {
        val raw =
          api.getRawJsonWithAgent("https://stooq.com/q/d/l/?s=${stooqSymbol}&i=w").string()
        HistoricalSeriesParser.parseStooqCsv(raw, minTs)
      }.getOrDefault(emptyList())
    if (stooq.size >= 2) {
      fiveYearMetalUsdCache[code] = stooq
      return stooq
    }

    // 2. Yahoo Finance v8 5Y weekly Gold / Silver futures
    val yahoo =
      runCatching {
        val raw =
          api.getRawJsonWithAgent(
            "https://query1.finance.yahoo.com/v8/finance/chart/${yahooSymbol}?range=5y&interval=1wk",
          ).string()
        HistoricalSeriesParser.parseYahooChartResponse(raw, minTs)
      }.getOrDefault(emptyList())
    if (yahoo.size >= 2) {
      fiveYearMetalUsdCache[code] = yahoo
      return yahoo
    }

    return emptyList()
  }

  private fun toFiveYearEntities(
    seriesKey: String,
    rawPairs: List<Pair<Long, Double>>,
  ): List<CachedChartPointEntity> {
    val sampled = HistoricalSeriesParser.downsamplePairs(rawPairs, maxPoints = 75)
    val labelFmt = SimpleDateFormat("MMM ''yy", Locale.US)
    return sampled.mapIndexed { idx, (ts, value) ->
      CachedChartPointEntity(
        id = "${seriesKey}_$idx",
        seriesKey = seriesKey,
        pointIndex = idx,
        timestampMillis = ts,
        value = value,
        label = labelFmt.format(Date(ts)),
      )
    }
  }

  private data class SampleDateInfo(
    val dateStr: String,
    val label: String,
    val timeMillis: Long,
  )

  private fun prepareSampleDates(range: ChartRange): List<SampleDateInfo> {
    val count =
      when (range) {
        ChartRange.D7 -> 7
        ChartRange.M1 -> 8
        ChartRange.M3 -> 9
        ChartRange.Y1 -> 10
        ChartRange.Y5 -> 12
      }
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt =
      SimpleDateFormat(if (range == ChartRange.Y5) "MMM ''yy" else "MMM d", Locale.US)
    return sampleDates(range, count).map { date ->
      SampleDateInfo(
        dateStr = dateFmt.format(date),
        label = labelFmt.format(date),
        timeMillis = date.time,
      )
    }
  }

  private suspend fun fetchUsdSnapshotForDate(dateStr: String): Map<String, Double>? {
    usdSnapshotCache[dateStr]?.let { return it }
    val deferred =
      inFlightDateFetches.getOrPut(dateStr) {
        repoScope.async {
          try {
            val url =
              "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$dateStr/v1/currencies/usd.json"
            val raw = api.getRawJson(url).string()
            val usd = JSONObject(raw).optJSONObject("usd") ?: return@async null
            val map = HashMap<String, Double>(200)
            val keys = usd.keys()
            while (keys.hasNext()) {
              val k = keys.next()
              val v = usd.optDouble(k, -1.0)
              if (v > 0.0) map[k] = v
            }
            if (map.isNotEmpty()) {
              usdSnapshotCache[dateStr] = map
              map
            } else null
          } catch (_: Exception) {
            null
          } finally {
            inFlightDateFetches.remove(dateStr)
          }
        }
      }
    return deferred.await()
  }

  private suspend fun fetchDatedCurrencySnapshots(
    seriesKey: String,
    from: String,
    to: String,
    range: ChartRange,
  ): List<CachedChartPointEntity> = coroutineScope {
    val samples = prepareSampleDates(range)
    val fromKey = from.lowercase()
    val toKey = to.lowercase()

    val results =
      samples.mapIndexed { idx, info ->
        async {
          val usd = fetchUsdSnapshotForDate(info.dateStr) ?: return@async null
          val rFrom = if (fromKey == "usd") 1.0 else (usd[fromKey] ?: -1.0)
          val rTo = if (toKey == "usd") 1.0 else (usd[toKey] ?: -1.0)
          if (rFrom > 0.0 && rTo > 0.0) {
            CachedChartPointEntity(
              id = "${seriesKey}_$idx",
              seriesKey = seriesKey,
              pointIndex = idx,
              timestampMillis = info.timeMillis,
              value = rTo / rFrom,
              label = info.label,
            )
          } else null
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
    val samples = prepareSampleDates(range)
    val metalKey = metalCode.lowercase()
    val currKey = currencyCode.lowercase()

    val results =
      samples.mapIndexed { idx, info ->
        async {
          val usd = fetchUsdSnapshotForDate(info.dateStr) ?: return@async null
          val metalPerUsd = usd[metalKey] ?: -1.0
          val currPerUsd = if (currKey == "usd") 1.0 else (usd[currKey] ?: -1.0)
          if (metalPerUsd > 0.0 && currPerUsd > 0.0) {
            val pricePerOzInCurrency = (1.0 / metalPerUsd) * currPerUsd
            CachedChartPointEntity(
              id = "${seriesKey}_$idx",
              seriesKey = seriesKey,
              pointIndex = idx,
              timestampMillis = info.timeMillis,
              value = pricePerOzInCurrency,
              label = info.label,
            )
          } else null
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
    val labelFmt =
      SimpleDateFormat(if (range == ChartRange.Y5) "MMM ''yy" else "MMM d", Locale.US)
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
