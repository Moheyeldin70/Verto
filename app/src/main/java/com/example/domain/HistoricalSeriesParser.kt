package com.example.domain

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object HistoricalSeriesParser {
  fun parseYahooChartResponse(
    rawJson: String,
    minTimestampMillis: Long = 0L,
  ): List<Pair<Long, Double>> {
    return runCatching {
      val root = JSONObject(rawJson)
      val resultArr = root.optJSONObject("chart")?.optJSONArray("result") ?: return emptyList()
      if (resultArr.length() == 0) return emptyList()
      val result = resultArr.optJSONObject(0) ?: return emptyList()
      val timestamps = result.optJSONArray("timestamp") ?: return emptyList()
      val quoteArr =
        result.optJSONObject("indicators")?.optJSONArray("quote") ?: return emptyList()
      if (quoteArr.length() == 0) return emptyList()
      val closes = quoteArr.optJSONObject(0)?.optJSONArray("close") ?: return emptyList()

      val len = minOf(timestamps.length(), closes.length())
      val out = ArrayList<Pair<Long, Double>>(len)
      for (i in 0 until len) {
        if (closes.isNull(i)) continue
        val tsSec = timestamps.optLong(i, -1L)
        val v = closes.optDouble(i, -1.0)
        if (tsSec > 0L && v > 0.0 && !v.isNaN() && !v.isInfinite()) {
          val tsMs = tsSec * 1000L
          if (tsMs >= minTimestampMillis) {
            out.add(tsMs to v)
          }
        }
      }
      out.sortedBy { it.first }
    }.getOrDefault(emptyList())
  }

  fun parseStooqCsv(
    rawCsv: String,
    minTimestampMillis: Long = 0L,
  ): List<Pair<Long, Double>> {
    return runCatching {
      val lines = rawCsv.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
      if (lines.size < 3) return emptyList()
      val dateFmt =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
      val out = ArrayList<Pair<Long, Double>>(lines.size)
      // Header: Date,Open,High,Low,Close[,Volume]
      for (idx in 1 until lines.size) {
        val parts = lines[idx].split(',')
        if (parts.size < 5) continue
        val parsed = runCatching { dateFmt.parse(parts[0]) }.getOrNull() ?: continue
        val close = parts[4].toDoubleOrNull() ?: continue
        if (close > 0.0 && parsed.time >= minTimestampMillis) {
          out.add(parsed.time to close)
        }
      }
      out.sortedBy { it.first }
    }.getOrDefault(emptyList())
  }

  fun downsamplePairs(
    points: List<Pair<Long, Double>>,
    maxPoints: Int = 75,
  ): List<Pair<Long, Double>> {
    if (points.size <= maxPoints || maxPoints < 2) return points
    val step = (points.size - 1).toDouble() / (maxPoints - 1).toDouble()
    val result = ArrayList<Pair<Long, Double>>(maxPoints)
    var lastIndex = -1
    for (i in 0 until maxPoints) {
      val srcIndex = (i * step).toInt().coerceIn(0, points.lastIndex)
      if (srcIndex != lastIndex) {
        result.add(points[srcIndex])
        lastIndex = srcIndex
      }
    }
    if (result.lastOrNull()?.first != points.last().first) {
      result[result.lastIndex] = points.last()
    }
    return result
  }

  fun nearestValue(
    sortedSeries: List<Pair<Long, Double>>,
    targetMillis: Long,
    maxDistanceMillis: Long = 14L * 86_400_000L,
  ): Double? {
    if (sortedSeries.isEmpty()) return null
    var low = 0
    var high = sortedSeries.lastIndex
    while (low <= high) {
      val mid = (low + high) ushr 1
      val midTs = sortedSeries[mid].first
      when {
        midTs < targetMillis -> low = mid + 1
        midTs > targetMillis -> high = mid - 1
        else -> return sortedSeries[mid].second
      }
    }
    val candidates = buildList {
      if (low in sortedSeries.indices) add(sortedSeries[low])
      if (high in sortedSeries.indices) add(sortedSeries[high])
    }
    val best = candidates.minByOrNull { abs(it.first - targetMillis) } ?: return null
    return if (abs(best.first - targetMillis) <= maxDistanceMillis) best.second else null
  }

  fun alignCrossRateSeries(
    usdToFrom: List<Pair<Long, Double>>,
    usdToTo: List<Pair<Long, Double>>,
  ): List<Pair<Long, Double>> {
    if (usdToFrom.size < 2 || usdToTo.size < 2) return emptyList()
    return usdToFrom.mapNotNull { (ts, fromRate) ->
      if (fromRate <= 0.0) return@mapNotNull null
      val toRate = nearestValue(usdToTo, ts) ?: return@mapNotNull null
      if (toRate <= 0.0) return@mapNotNull null
      ts to (toRate / fromRate)
    }
  }

  fun multiplySeriesByFx(
    metalUsdPoints: List<Pair<Long, Double>>,
    usdToCurrency: List<Pair<Long, Double>>?,
    fallbackFxRate: Double,
  ): List<Pair<Long, Double>> {
    if (metalUsdPoints.isEmpty()) return emptyList()
    if (usdToCurrency.isNullOrEmpty()) {
      return metalUsdPoints.map { (ts, usdPrice) -> ts to (usdPrice * fallbackFxRate) }
    }
    return metalUsdPoints.map { (ts, usdPrice) ->
      val fx = nearestValue(usdToCurrency, ts, maxDistanceMillis = 21L * 86_400_000L) ?: fallbackFxRate
      ts to (usdPrice * fx)
    }
  }
}
