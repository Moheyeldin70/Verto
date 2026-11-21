package com.example.domain

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.local.CachedChartPointEntity
import com.example.data.repository.SyncStatus
import com.example.ui.AppTab
import com.example.ui.CurrencyConverterState
import com.example.ui.MetalChartUnit
import com.example.ui.MetalType
import com.example.ui.MetalsTrackerState
import com.example.ui.UnitConverterState
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs

private const val GRAMS_PER_TROY_OZ = 31.1034768
private const val STANDARD_COIN_GRAMS = 8.0

enum class ShareExportMode {
  SUMMARY,
  CHART,
}

data class ShareExportPayload(
  val tab: AppTab,
  val title: String,
  val subtitle: String,
  val heroPrimaryText: String,
  val heroSecondaryText: String,
  val breakdownLines: List<Pair<String, String>>,
  val chartTitle: String?,
  val chartPoints: List<Pair<String, Double>>,
  val chartSuffix: String,
  val statusBadgeText: String,
  val timestampText: String,
  val accentColor: Int,
  val gainColor: Int,
  val lossColor: Int,
  val isDarkTheme: Boolean,
  val textSummary: String,
) {
  val hasChart: Boolean
    get() = chartPoints.size >= 2
}

object ShareExportHelper {

  fun buildPayload(
    activeTab: AppTab,
    unitState: UnitConverterState,
    currencyState: CurrencyConverterState,
    metalsState: MetalsTrackerState,
    ratesMap: Map<String, Double>,
    syncStatus: SyncStatus,
    displayLocale: Locale,
    isDarkTheme: Boolean,
    categoryDisplayTitle: String,
    tabTitle: String,
    chartDisplayTitle: String,
    liveLabel: String,
    offlineLabel: String,
  ): ShareExportPayload {
    val gainColor = if (isDarkTheme) 0xFF34D399.toInt() else 0xFF0D9F6E.toInt()
    val lossColor = if (isDarkTheme) 0xFFF87171.toInt() else 0xFFDC2626.toInt()
    val brandOrange = 0xFFFF6500.toInt()
    val goldColor = if (isDarkTheme) 0xFFFBBF24.toInt() else 0xFFD97706.toInt()
    val silverColor = if (isDarkTheme) 0xFFCBD5E1.toInt() else 0xFF64748B.toInt()

    val statusText = if (syncStatus.isLive) liveLabel else offlineLabel
    val timeText = FormattingUtils.formatTimestamp(syncStatus.lastUpdatedMillis, displayLocale)

    return when (activeTab) {
      AppTab.UNITS -> {
        val inFormatted = "${FormattingUtils.formatSmart(unitState.inputValue)} ${unitState.fromUnit.symbol}"
        val outFormatted = "${FormattingUtils.formatSmart(unitState.outputValue)} ${unitState.toUnit.symbol}"
        val formula = UnitCatalog.describeRelationship(unitState.fromUnit, unitState.toUnit)
        val topEquivalents =
          UnitCatalog.unitsFor(unitState.category)
            .filter { it.id != unitState.fromUnit.id && it.id != unitState.toUnit.id }
            .take(4)
            .map { u ->
              u.name to "${FormattingUtils.formatSmart(UnitCatalog.convert(unitState.inputValue, unitState.fromUnit, u))} ${u.symbol}"
            }

        val bullets = buildList {
          add("Formula" to formula)
          addAll(topEquivalents)
        }

        val summaryText = buildString {
          appendLine("Verto · $tabTitle ($categoryDisplayTitle)")
          appendLine("$inFormatted = $outFormatted")
          appendLine("Formula: $formula")
          if (topEquivalents.isNotEmpty()) {
            appendLine()
            topEquivalents.forEach { (name, value) -> appendLine("• $name: $value") }
          }
        }.trim()

        ShareExportPayload(
          tab = AppTab.UNITS,
          title = tabTitle,
          subtitle = categoryDisplayTitle,
          heroPrimaryText = outFormatted,
          heroSecondaryText = "$inFormatted =",
          breakdownLines = bullets,
          chartTitle = null,
          chartPoints = emptyList(),
          chartSuffix = "",
          statusBadgeText = unitState.category.name,
          timestampText = categoryDisplayTitle,
          accentColor = brandOrange,
          gainColor = gainColor,
          lossColor = lossColor,
          isDarkTheme = isDarkTheme,
          textSummary = summaryText,
        )
      }

      AppTab.CURRENCY -> {
        val fromInfo = CurrencyCatalog.find(currencyState.fromCode)
        val toInfo = CurrencyCatalog.find(currencyState.toCode)
        val fromRate = ratesMap[currencyState.fromCode] ?: fromInfo.fallbackRateFromUsd
        val toRate = ratesMap[currencyState.toCode] ?: toInfo.fallbackRateFromUsd
        val unitRate = if (fromRate > 0.0) toRate / fromRate else 1.0
        val inverseRate = if (unitRate > 0.0) 1.0 / unitRate else 1.0
        val inputVal = currencyState.inputText.toDoubleOrNull() ?: 0.0
        val outputVal = inputVal * unitRate

        val inText = FormattingUtils.formatMoney(inputVal, currencyState.fromCode)
        val outText = FormattingUtils.formatMoney(outputVal, currencyState.toCode)
        val rateLine = "1 ${currencyState.fromCode} = ${FormattingUtils.formatRate(unitRate)} ${currencyState.toCode}"
        val invLine = "1 ${currencyState.toCode} = ${FormattingUtils.formatRate(inverseRate)} ${currencyState.fromCode}"

        val chartPts = currencyState.chartPoints.map { it.label to it.value }
        val chartStats = computeChartStats(chartPts, currencyState.toCode)

        val bullets = buildList {
          add("Rate" to rateLine)
          add("Inverse" to invLine)
          if (chartStats != null) {
            add("${currencyState.chartRange.name} High / Low" to "${chartStats.highText} / ${chartStats.lowText}")
            add("${currencyState.chartRange.name} Change" to chartStats.changeText)
          }
        }

        val summaryText = buildString {
          appendLine("Verto · $tabTitle")
          appendLine("$inText → $outText")
          appendLine(rateLine)
          appendLine(invLine)
          if (chartStats != null) {
            appendLine("${currencyState.chartRange.name} Trend: ${chartStats.changeText} (High ${chartStats.highText}, Low ${chartStats.lowText})")
          }
          appendLine("$statusText · $timeText")
        }.trim()

        ShareExportPayload(
          tab = AppTab.CURRENCY,
          title = tabTitle,
          subtitle = "${fromInfo.flag} ${fromInfo.code}  →  ${toInfo.flag} ${toInfo.code}",
          heroPrimaryText = outText,
          heroSecondaryText = "$inText =",
          breakdownLines = bullets,
          chartTitle = "$chartDisplayTitle (${currencyState.chartRange.name})",
          chartPoints = chartPts,
          chartSuffix = currencyState.toCode,
          statusBadgeText = statusText,
          timestampText = timeText,
          accentColor = brandOrange,
          gainColor = gainColor,
          lossColor = lossColor,
          isDarkTheme = isDarkTheme,
          textSummary = summaryText,
        )
      }

      AppTab.METALS -> {
        val country = metalsState.selectedCountry
        val cur = country.currencyCode
        val fx = ratesMap[cur] ?: 1.0
        val goldOz = metalsState.goldUsdPerOz * fx
        val silverOz = metalsState.silverUsdPerOz * fx
        val goldG = goldOz / GRAMS_PER_TROY_OZ
        val silverG = silverOz / GRAMS_PER_TROY_OZ
        val isGold = metalsState.selectedMetal == MetalType.GOLD
        val activePerG = if (isGold) goldG else silverG
        val activePerOz = if (isGold) goldOz else silverOz
        val metalTitle = if (isGold) "Gold (XAU)" else "Silver (XAG)"
        val ratio = if (metalsState.silverUsdPerOz > 0.0) metalsState.goldUsdPerOz / metalsState.silverUsdPerOz else 0.0

        val unitFactor =
          when (metalsState.chartUnit) {
            MetalChartUnit.OZ -> 1.0
            MetalChartUnit.GRAM -> 1.0 / GRAMS_PER_TROY_OZ
            MetalChartUnit.COIN -> STANDARD_COIN_GRAMS / GRAMS_PER_TROY_OZ
          }
        val unitLabel =
          when (metalsState.chartUnit) {
            MetalChartUnit.OZ -> "oz"
            MetalChartUnit.GRAM -> "g"
            MetalChartUnit.COIN -> "8g"
          }
        val combinedFactor = unitFactor * metalsState.chartPurityFraction
        val chartPts = metalsState.chartPoints.map { it.label to (it.value * combinedFactor) }
        val chartStats = computeChartStats(chartPts, "$cur/$unitLabel")

        val bullets = buildList {
          add("Spot / oz" to FormattingUtils.formatMoney(activePerOz, cur))
          if (isGold) {
            add("22K / g" to FormattingUtils.formatMoney(goldG * (22.0 / 24.0), cur))
            add("21K / g" to FormattingUtils.formatMoney(goldG * (21.0 / 24.0), cur))
            add("18K / g" to FormattingUtils.formatMoney(goldG * (18.0 / 24.0), cur))
          } else {
            add("925 Sterling / g" to FormattingUtils.formatMoney(silverG * 0.925, cur))
            add("Gold / Silver Ratio" to "${String.format(Locale.US, "%.2f", ratio)}×")
          }
          country.localUnit?.let { u ->
            add(u.name to FormattingUtils.formatMoney(activePerG * u.grams, cur))
          }
          if (chartStats != null) {
            add("${metalsState.chartRange.name} Change ($unitLabel)" to chartStats.changeText)
          }
        }

        val summaryText = buildString {
          appendLine("Verto · $metalTitle · ${country.localizedName(displayLocale)} ($cur)")
          appendLine("Spot: ${FormattingUtils.formatMoney(activePerG, cur)} / g | ${FormattingUtils.formatMoney(activePerOz, cur)} / oz")
          if (isGold) {
            appendLine("22K: ${FormattingUtils.formatMoney(goldG * (22.0 / 24.0), cur)}/g · 21K: ${FormattingUtils.formatMoney(goldG * (21.0 / 24.0), cur)}/g · 18K: ${FormattingUtils.formatMoney(goldG * (18.0 / 24.0), cur)}/g")
          } else {
            appendLine("925 Sterling: ${FormattingUtils.formatMoney(silverG * 0.925, cur)}/g")
          }
          appendLine("Au/Ag Ratio: ${String.format(Locale.US, "%.2f", ratio)}×")
          if (chartStats != null) {
            appendLine("${metalsState.chartRange.name} Trend ($unitLabel): ${chartStats.changeText} (High ${chartStats.highText}, Low ${chartStats.lowText})")
          }
          appendLine("$statusText · $timeText")
        }.trim()

        ShareExportPayload(
          tab = AppTab.METALS,
          title = tabTitle,
          subtitle = "${country.flag} ${country.localizedName(displayLocale)} · $metalTitle",
          heroPrimaryText = "${FormattingUtils.formatMoney(activePerG, cur)} / g",
          heroSecondaryText = "24K / 999 Spot",
          breakdownLines = bullets,
          chartTitle = "$chartDisplayTitle (${metalsState.chartRange.name} · $unitLabel)",
          chartPoints = chartPts,
          chartSuffix = "$cur/$unitLabel",
          statusBadgeText = statusText,
          timestampText = timeText,
          accentColor = if (isGold) goldColor else silverColor,
          gainColor = gainColor,
          lossColor = lossColor,
          isDarkTheme = isDarkTheme,
          textSummary = summaryText,
        )
      }
    }
  }

  private data class ChartStats(
    val highText: String,
    val lowText: String,
    val changeText: String,
    val pctChange: Double,
  )

  private fun computeChartStats(points: List<Pair<String, Double>>, suffix: String): ChartStats? {
    if (points.size < 2) return null
    val first = points.first().second
    val last = points.last().second
    val minVal = points.minOf { it.second }
    val maxVal = points.maxOf { it.second }
    val pct = if (abs(first) > 1e-12) ((last - first) / first) * 100.0 else 0.0
    val sign = if (pct >= 0.0) "+" else ""
    return ChartStats(
      highText = "${FormattingUtils.formatRate(maxVal)} $suffix".trim(),
      lowText = "${FormattingUtils.formatRate(minVal)} $suffix".trim(),
      changeText = "$sign${String.format(Locale.US, "%.2f", pct)}%",
      pctChange = pct,
    )
  }

  fun renderExportBitmap(
    payload: ShareExportPayload,
    mode: ShareExportMode,
  ): Bitmap {
    val width = 1080
    val includeChart = payload.hasChart && (mode == ShareExportMode.CHART || payload.tab != AppTab.UNITS)
    val chartBlockHeight = if (includeChart) 440 else 0
    val breakdownBlockHeight = (payload.breakdownLines.size.coerceAtLeast(1) * 74) + 56
    val height = (480 + chartBlockHeight + breakdownBlockHeight + 110).coerceIn(880, 1760)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = if (payload.isDarkTheme) 0xFF11141B.toInt() else 0xFFF5F6FA.toInt()
    val cardColor = if (payload.isDarkTheme) 0xFF1B1F2A.toInt() else 0xFFFFFFFF.toInt()
    val borderColor = if (payload.isDarkTheme) 0xFF2D3343.toInt() else 0xFFE2E8F0.toInt()
    val primaryTextColor = if (payload.isDarkTheme) 0xFFF8FAFC.toInt() else 0xFF0F172A.toInt()
    val secondaryTextColor = if (payload.isDarkTheme) 0xFF94A3B8.toInt() else 0xFF475569.toInt()
    val chipBgColor = if (payload.isDarkTheme) 0xFF242937.toInt() else 0xFFF1F5F9.toInt()

    canvas.drawColor(bgColor)

    val cardRect = RectF(48f, 48f, width - 48f, height - 48f)
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardColor }
    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = 3f
      }
    canvas.drawRoundRect(cardRect, 42f, 42f, cardPaint)
    canvas.drawRoundRect(cardRect, 42f, 42f, borderPaint)

    // Accent strip along top of card
    canvas.save()
    val clipPath = Path().apply { addRoundRect(cardRect, 42f, 42f, Path.Direction.CW) }
    canvas.clipPath(clipPath)
    val stripPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            cardRect.left,
            cardRect.top,
            cardRect.right,
            cardRect.top,
            payload.accentColor,
            0xFFF59E0B.toInt(),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRect(cardRect.left, cardRect.top, cardRect.right, cardRect.top + 14f, stripPaint)
    canvas.restore()

    val padX = 96f
    var y = 128f

    // Brand + status pill
    val brandPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = payload.accentColor
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    canvas.drawText("VERTO", padX, y, brandPaint)

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      }
    canvas.drawText("·  ${payload.title}", padX + brandPaint.measureText("VERTO") + 16f, y, titlePaint)

    val badgePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val badgeWidth = badgePaint.measureText(payload.statusBadgeText) + 44f
    val badgeRect = RectF(width - padX - badgeWidth, y - 36f, width - padX, y + 14f)
    val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = chipBgColor }
    canvas.drawRoundRect(badgeRect, 24f, 24f, badgeBgPaint)
    canvas.drawText(payload.statusBadgeText, badgeRect.left + 22f, y - 2f, badgePaint)

    y += 56f
    val subtitlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    canvas.drawText(payload.subtitle, padX, y, subtitlePaint)

    y += 66f
    val secPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 32f
      }
    canvas.drawText(payload.heroSecondaryText, padX, y, secPaint)

    y += 84f
    val heroPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = payload.accentColor
        textSize = 70f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val maxHeroWidth = width - padX * 2f
    while (heroPaint.measureText(payload.heroPrimaryText) > maxHeroWidth && heroPaint.textSize > 38f) {
      heroPaint.textSize -= 4f
    }
    canvas.drawText(payload.heroPrimaryText, padX, y, heroPaint)

    y += 42f
    val dividerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        strokeWidth = 2f
      }
    canvas.drawLine(padX, y, width - padX, y, dividerPaint)

    // Chart Section
    if (includeChart) {
      y += 52f
      val chartHeaderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = primaryTextColor
          textSize = 30f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
      canvas.drawText(payload.chartTitle ?: "Trend", padX, y, chartHeaderPaint)

      val stats = computeChartStats(payload.chartPoints, payload.chartSuffix)
      if (stats != null) {
        val pctColor = if (stats.pctChange >= 0.0) payload.gainColor else payload.lossColor
        val pctPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pctColor
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
          }
        canvas.drawText(stats.changeText, width - padX, y, pctPaint)
      }

      y += 28f
      val plotBox = RectF(padX, y, width - padX, y + 290f)
      val plotBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = chipBgColor }
      canvas.drawRoundRect(plotBox, 26f, 26f, plotBgPaint)
      drawSmoothCurve(
        canvas = canvas,
        bounds = plotBox,
        points = payload.chartPoints,
        accentColor = payload.accentColor,
        gridColor = borderColor,
      )
      y = plotBox.bottom + 36f
      val labelPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = secondaryTextColor
          textSize = 24f
        }
      canvas.drawText(payload.chartPoints.first().first, padX, y, labelPaint)
      labelPaint.textAlign = Paint.Align.RIGHT
      canvas.drawText(payload.chartPoints.last().first, width - padX, y, labelPaint)
      y += 24f
    } else {
      y += 20f
    }

    // Breakdown rows
    val keyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 28f
      }
    val valPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 29f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
      }

    payload.breakdownLines.forEachIndexed { idx, (k, v) ->
      y += 58f
      canvas.drawText(k, padX, y, keyPaint)
      canvas.drawText(v, width - padX, y, valPaint)
      if (idx < payload.breakdownLines.lastIndex) {
        canvas.drawLine(padX, y + 18f, width - padX, y + 18f, dividerPaint)
      }
    }

    // Footer timestamp
    val footerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 24f
      }
    canvas.drawText(payload.timestampText, padX, height - 86f, footerPaint)
    footerPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText("Generated by Verto", width - padX, height - 86f, footerPaint)

    return bitmap
  }

  private fun drawSmoothCurve(
    canvas: Canvas,
    bounds: RectF,
    points: List<Pair<String, Double>>,
    accentColor: Int,
    gridColor: Int,
  ) {
    if (points.size < 2) return
    val padH = 26f
    val padV = 30f
    val left = bounds.left + padH
    val right = bounds.right - padH
    val top = bounds.top + padV
    val bottom = bounds.bottom - padV
    val plotW = (right - left).coerceAtLeast(1f)
    val plotH = (bottom - top).coerceAtLeast(1f)

    val gridPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridColor
        strokeWidth = 2f
      }
    for (i in 1..3) {
      val gy = top + (plotH * i / 4f)
      canvas.drawLine(left, gy, right, gy, gridPaint)
    }

    val minVal = points.minOf { it.second }
    val maxVal = points.maxOf { it.second }
    val span = (maxVal - minVal).takeIf { it > 1e-12 } ?: 1.0

    val coords =
      points.mapIndexed { idx, (_, value) ->
        val x = left + (idx.toFloat() / points.lastIndex.toFloat()) * plotW
        val norm = ((value - minVal) / span).toFloat()
        val y = bottom - (norm * plotH)
        x to y
      }

    val linePath = Path().apply {
      moveTo(coords.first().first, coords.first().second)
      for (i in 1 until coords.size) {
        val (prevX, prevY) = coords[i - 1]
        val (curX, curY) = coords[i]
        val midX = (prevX + curX) / 2f
        cubicTo(midX, prevY, midX, curY, curX, curY)
      }
    }

    val fillPath = Path(linePath).apply {
      lineTo(coords.last().first, bottom)
      lineTo(coords.first().first, bottom)
      close()
    }

    val fillPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader =
          LinearGradient(
            0f,
            top,
            0f,
            bottom,
            Color.argb(95, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawPath(fillPath, fillPaint)

    val strokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
      }
    canvas.drawPath(linePath, strokePaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
    val (lx, ly) = coords.last()
    canvas.drawCircle(lx, ly, 10f, dotPaint)
  }

  fun shareText(
    context: Context,
    chooserTitle: String,
    text: String,
  ) {
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
      }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
  }

  fun shareImage(
    context: Context,
    bitmap: Bitmap,
    chooserTitle: String,
    captionText: String,
  ) {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    // Clean stale exports before writing a new one
    exportDir.listFiles()?.forEach { old ->
      if (System.currentTimeMillis() - old.lastModified() > 3_600_000L) {
        old.delete()
      }
    }
    val outFile = File(exportDir, "verto_share_${System.currentTimeMillis()}.png")
    FileOutputStream(outFile).use { stream ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
      stream.flush()
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, captionText)
        clipData = ClipData.newRawUri(chooserTitle, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
  }
}
