package com.example.ai

import android.content.Context
import com.example.data.local.CachedChartPointEntity
import com.example.data.repository.SyncStatus
import com.example.domain.FormattingUtils
import com.example.domain.UnitCatalog
import com.example.ui.AppTab
import com.example.ui.CurrencyConverterState
import com.example.ui.MetalChartUnit
import com.example.ui.MetalType
import com.example.ui.MetalsTrackerState
import com.example.ui.UnitConverterState
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import kotlin.math.abs

private const val DEFAULT_MODEL_NAME = "gemini-3.8-flash"
private const val GRAMS_PER_TROY_OZ = 31.1034768

sealed interface AiUiState {
  data object Idle : AiUiState
  data object Loading : AiUiState
  data class Streaming(val text: String) : AiUiState
  data class Success(val text: String) : AiUiState
  data class Error(val message: String) : AiUiState
}

data class AiChatMessage(
  val id: Long,
  val isUser: Boolean,
  val text: String,
  val isStreaming: Boolean = false,
  val contextLabel: String = "",
)

data class AiAssistantState(
  val uiState: AiUiState = AiUiState.Idle,
  val messages: List<AiChatMessage> = emptyList(),
)

object VertoAiContextBuilder {
  fun buildShortContextLabel(
    activeTab: AppTab,
    unitState: UnitConverterState,
    currencyState: CurrencyConverterState,
    metalsState: MetalsTrackerState,
    displayLocale: Locale,
  ): String =
    when (activeTab) {
      AppTab.UNITS ->
        "${unitState.inputText.ifBlank { "0" }} ${unitState.fromUnit.symbol} → ${
          FormattingUtils.formatSmart(unitState.outputValue)
        } ${unitState.toUnit.symbol}"

      AppTab.CURRENCY ->
        "${currencyState.fromCode}/${currencyState.toCode} · ${currencyState.chartRange.name}"

      AppTab.METALS -> {
        val metal = if (metalsState.selectedMetal == MetalType.GOLD) "Gold (XAU)" else "Silver (XAG)"
        "$metal · ${metalsState.selectedCountry.localizedName(displayLocale)} (${metalsState.selectedCountry.currencyCode})"
      }
    }

  fun buildContextSnapshot(
    activeTab: AppTab,
    unitState: UnitConverterState,
    currencyState: CurrencyConverterState,
    metalsState: MetalsTrackerState,
    ratesMap: Map<String, Double>,
    syncStatus: SyncStatus,
    displayLocale: Locale,
  ): String {
    val sb = StringBuilder()
    sb.appendLine("Respond naturally in the user's app display language: ${displayLocale.toLanguageTag()} (${displayLocale.getDisplayLanguage(Locale.ENGLISH)}).")
    sb.appendLine("Active Screen: ${activeTab.name}")
    sb.appendLine(
      "Market Data Mode: ${
        when {
          syncStatus.isLive -> "Live synced"
          syncStatus.isNetworkAvailable -> "Cached rates"
          else -> "Offline (cached rates)"
        }
      }, Last Updated: ${FormattingUtils.formatTimestamp(syncStatus.lastUpdatedMillis, Locale.US)}",
    )

    when (activeTab) {
      AppTab.UNITS -> {
        val input = unitState.inputValue
        val output = unitState.outputValue
        sb.appendLine("Unit Category: ${unitState.category.name}")
        sb.appendLine(
          "Current Conversion: ${FormattingUtils.formatSmart(input)} ${unitState.fromUnit.name} (${unitState.fromUnit.symbol}) = ${FormattingUtils.formatSmart(output)} ${unitState.toUnit.name} (${unitState.toUnit.symbol})",
        )
        sb.appendLine("Relationship Formula: ${UnitCatalog.describeRelationship(unitState.fromUnit, unitState.toUnit)}")
        val otherUnits = UnitCatalog.unitsFor(unitState.category).take(6).joinToString(", ") { u ->
          "${FormattingUtils.formatSmart(UnitCatalog.convert(input, unitState.fromUnit, u))} ${u.symbol}"
        }
        sb.appendLine("Equivalent Values: $otherUnits")
      }

      AppTab.CURRENCY -> {
        val fromRate = ratesMap[currencyState.fromCode] ?: 1.0
        val toRate = ratesMap[currencyState.toCode] ?: 1.0
        val unitRate = if (fromRate > 0.0) toRate / fromRate else 1.0
        val input = currencyState.inputText.toDoubleOrNull() ?: 0.0
        val output = input * unitRate
        sb.appendLine("Currency Pair: ${currencyState.fromCode} -> ${currencyState.toCode}")
        sb.appendLine("User Input: ${FormattingUtils.formatMoney(input, currencyState.fromCode)} = ${FormattingUtils.formatMoney(output, currencyState.toCode)}")
        sb.appendLine("Spot Rate: 1 ${currencyState.fromCode} = ${FormattingUtils.formatRate(unitRate)} ${currencyState.toCode}")
        sb.appendLine("Inverse Rate: 1 ${currencyState.toCode} = ${FormattingUtils.formatRate(if (unitRate > 0) 1.0 / unitRate else 0.0)} ${currencyState.fromCode}")
        appendChartStats(sb, currencyState.chartRange.name, currencyState.chartPoints, currencyState.toCode)
      }

      AppTab.METALS -> {
        val country = metalsState.selectedCountry
        val cur = country.currencyCode
        val fx = ratesMap[cur] ?: 1.0
        val goldOz = metalsState.goldUsdPerOz * fx
        val goldG = goldOz / GRAMS_PER_TROY_OZ
        val silverOz = metalsState.silverUsdPerOz * fx
        val silverG = silverOz / GRAMS_PER_TROY_OZ
        val ratio = if (metalsState.silverUsdPerOz > 0.0) metalsState.goldUsdPerOz / metalsState.silverUsdPerOz else 0.0

        sb.appendLine("Selected Metal: ${if (metalsState.selectedMetal == MetalType.GOLD) "Gold (XAU)" else "Silver (XAG)"}")
        sb.appendLine("Selected Market: ${country.name} ($cur)")
        sb.appendLine("Gold Spot: $${FormattingUtils.formatMoney(metalsState.goldUsdPerOz)} USD/oz | ${FormattingUtils.formatMoney(goldG, cur)}/g (24K) | 24h: ${String.format(Locale.US, "%+.2f%%", metalsState.goldChange24h)}")
        sb.appendLine("Silver Spot: $${FormattingUtils.formatMoney(metalsState.silverUsdPerOz)} USD/oz | ${FormattingUtils.formatMoney(silverG, cur)}/g (999) | 24h: ${String.format(Locale.US, "%+.2f%%", metalsState.silverChange24h)}")
        sb.appendLine("Gold/Silver Ratio: ${String.format(Locale.US, "%.2f", ratio)}x")
        val calcGrams = metalsState.calculatorGramsText.toDoubleOrNull() ?: 0.0
        val basePerG = if (metalsState.selectedMetal == MetalType.GOLD) goldG else silverG
        val calcValue = calcGrams * basePerG * metalsState.calculatorPurityFraction
        sb.appendLine("Bullion Calculator: ${FormattingUtils.formatSmart(calcGrams)}g @ purity ${String.format(Locale.US, "%.1f%%", metalsState.calculatorPurityFraction * 100)} = ${FormattingUtils.formatMoney(calcValue, cur)}")

        val unitFactor =
          when (metalsState.chartUnit) {
            MetalChartUnit.OZ -> 1.0
            MetalChartUnit.GRAM -> 1.0 / GRAMS_PER_TROY_OZ
            MetalChartUnit.COIN -> 8.0 / GRAMS_PER_TROY_OZ
          }
        val combined = unitFactor * metalsState.chartPurityFraction
        val scaledPoints = metalsState.chartPoints.map { it.copy(value = it.value * combined) }
        val unitName =
          when (metalsState.chartUnit) {
            MetalChartUnit.OZ -> "oz"
            MetalChartUnit.GRAM -> "g"
            MetalChartUnit.COIN -> "8g coin"
          }
        appendChartStats(sb, "${metalsState.chartRange.name} ($unitName)", scaledPoints, cur)
      }
    }
    return sb.toString().trim()
  }

  private fun appendChartStats(
    sb: StringBuilder,
    rangeLabel: String,
    points: List<CachedChartPointEntity>,
    unitSuffix: String,
  ) {
    if (points.size < 2) {
      sb.appendLine("Chart ($rangeLabel): No historical trend points loaded.")
      return
    }
    val first = points.first()
    val last = points.last()
    val minVal = points.minOf { it.value }
    val maxVal = points.maxOf { it.value }
    val pct = if (abs(first.value) > 1e-12) ((last.value - first.value) / first.value) * 100.0 else 0.0
    val recentSamples = points.takeLast(6).joinToString(", ") { "${it.label}: ${FormattingUtils.formatRate(it.value)}" }
    sb.appendLine(
      "Chart ($rangeLabel): Period ${first.label} to ${last.label} | Start ${FormattingUtils.formatRate(first.value)} $unitSuffix | Latest ${FormattingUtils.formatRate(last.value)} $unitSuffix | Low ${FormattingUtils.formatRate(minVal)} | High ${FormattingUtils.formatRate(maxVal)} | Change ${String.format(Locale.US, "%+.2f%%", pct)}",
    )
    sb.appendLine("Recent Trend Samples: $recentSamples")
  }
}

class VertoAiClient(private val context: Context) {
  private val appContext: Context = context.applicationContext ?: context

  private val model by lazy {
    if (!ensureFirebaseAppInitialized(appContext)) {
      val detail =
        if (!hasFirebaseBuildConfigResources(appContext)) {
          "Firebase configuration (google-services.json) is missing from this APK build. Rebuild with app/google-services.json included."
        } else {
          "Default FirebaseApp could not be initialized in this process."
        }
      throw IllegalStateException(detail)
    }
    Firebase.ai.generativeModel(
      modelName = DEFAULT_MODEL_NAME,
      generationConfig =
        generationConfig {
          temperature = 0.45f
          maxOutputTokens = 1024
        },
      systemInstruction =
        content {
          text(
            """
            You are Verto AI, a concise, insightful analyst embedded inside the Verto Unit, Global Currency, and Gold & Silver converter app.
            Your role:
            1. Explain conversion formulas, unit scales, currency cross-rates, purity pricing (24K/22K/21K/18K/14K & 999/925), and historical price trends clearly.
            2. Discuss drivers behind exchange rates and precious-metal moves (central banks, inflation, USD index, geopolitics, gold/silver ratio).
            3. When the user asks for predictions or forecasts, offer balanced scenario-based outlooks (bullish, base, bearish) grounded in the supplied historical trend data (range high/low, momentum, % change) and clearly note that forecasts are uncertain and informational only—not financial advice.
            4. Always ground calculations in the exact live/cached snapshot provided with the prompt. Keep formatting clean, scannable with short paragraphs or bullet points, and respond in the user's language.
            """.trimIndent(),
          )
        },
    )
  }

  fun streamReply(
    history: List<AiChatMessage>,
    userPrompt: String,
    contextSnapshot: String,
  ): Flow<String> {
    configureAppCheck(appContext, null)
    val sdkHistory: List<Content> =
      history
        .filter { !it.isStreaming && it.text.isNotBlank() }
        .takeLast(12)
        .map { msg ->
          content(role = if (msg.isUser) "user" else "model") {
            text(msg.text)
          }
        }

    val chat = model.startChat(history = sdkHistory)
    val enrichedPrompt =
      """
      [VERTO LIVE APP SNAPSHOT]
      $contextSnapshot

      [USER QUESTION]
      $userPrompt
      """.trimIndent()

    return chat.sendMessageStream(enrichedPrompt).map { chunk -> chunk.text.orEmpty() }
  }
}
