package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.ChartRange
import com.example.data.repository.SyncStatus
import com.example.domain.CountryMarket
import com.example.domain.CountryMetalsCatalog
import com.example.domain.FormattingUtils
import com.example.ui.MetalChartUnit
import com.example.ui.MetalType
import com.example.ui.MetalsTrackerState
import com.example.ui.components.ChartRangeSelector
import com.example.ui.components.ChartSeriesPoint
import com.example.ui.components.CountryPickerBottomSheet
import com.example.ui.components.FinancialTrendChart
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.theme.NumericDisplayStyle
import com.example.ui.theme.NumericMediumStyle
import com.example.ui.theme.NumericSmallStyle
import java.util.Locale

private const val GRAMS_PER_TROY_OZ = 31.1034768
private const val STANDARD_COIN_GRAMS = 8.0

@Composable
fun MetalsTrackerScreen(
  state: MetalsTrackerState,
  ratesMap: Map<String, Double>,
  syncStatus: SyncStatus,
  displayLocale: Locale,
  isWideLayout: Boolean,
  onSelectMetal: (MetalType) -> Unit,
  onSelectCountry: (CountryMarket) -> Unit,
  onSelectRange: (ChartRange) -> Unit,
  onSelectChartUnit: (MetalChartUnit) -> Unit,
  onSelectChartPurity: (Double) -> Unit,
  onCalculatorGramsChange: (String) -> Unit,
  onCalculatorPurityChange: (Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCountryPicker by remember { mutableStateOf(false) }
  val financial = LocalFinancialColors.current

  val currencyCode = state.selectedCountry.currencyCode
  val fxFromUsd = ratesMap[currencyCode] ?: 1.0
  val goldLocalPerOz = state.goldUsdPerOz * fxFromUsd
  val silverLocalPerOz = state.silverUsdPerOz * fxFromUsd
  val goldLocalPerGram = goldLocalPerOz / GRAMS_PER_TROY_OZ
  val silverLocalPerGram = silverLocalPerOz / GRAMS_PER_TROY_OZ
  val ratio = if (state.silverUsdPerOz > 0.0) state.goldUsdPerOz / state.silverUsdPerOz else 0.0

  val activeMetalColor =
    if (state.selectedMetal == MetalType.GOLD) financial.gold else financial.silver

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Country Selector
    Surface(
      onClick = { showCountryPicker = true },
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("country_selector_pill"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Text(text = state.selectedCountry.flag, style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.width(10.dp))
          Text(
            text = "${state.selectedCountry.localizedName(displayLocale)} ($currencyCode)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Icon(
          imageVector = Icons.Rounded.ArrowDropDown,
          contentDescription = stringResource(R.string.select_country),
        )
      }
    }

    // Spot Cards: Gold & Silver side-by-side
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MetalSpotCard(
        title = stringResource(R.string.gold_label),
        localPricePerOz = goldLocalPerOz,
        localPricePerGram = goldLocalPerGram,
        usdPerOz = state.goldUsdPerOz,
        changePct = state.goldChange24h,
        currencyCode = currencyCode,
        accentColor = financial.gold,
        isSelected = state.selectedMetal == MetalType.GOLD,
        onClick = { onSelectMetal(MetalType.GOLD) },
        modifier = Modifier.weight(1f).testTag("gold_spot_card"),
      )
      MetalSpotCard(
        title = stringResource(R.string.silver_label),
        localPricePerOz = silverLocalPerOz,
        localPricePerGram = silverLocalPerGram,
        usdPerOz = state.silverUsdPerOz,
        changePct = state.silverChange24h,
        currencyCode = currencyCode,
        accentColor = financial.silver,
        isSelected = state.selectedMetal == MetalType.SILVER,
        onClick = { onSelectMetal(MetalType.SILVER) },
        modifier = Modifier.weight(1f).testTag("silver_spot_card"),
      )
    }

    // Gold/Silver Ratio + Timestamp bar
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${stringResource(R.string.gold_silver_ratio)}: ${String.format(Locale.US, "%.2f", ratio)}×",
          style = NumericSmallStyle,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = FormattingUtils.formatTimestamp(syncStatus.lastUpdatedMillis, displayLocale),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (isWideLayout) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
          MetalTrendChartCard(
            state = state,
            currencyCode = currencyCode,
            accentColor = activeMetalColor,
            onSelectRange = onSelectRange,
            onSelectChartUnit = onSelectChartUnit,
            onSelectChartPurity = onSelectChartPurity,
          )
          BullionCalculatorCard(
            state = state,
            perGram24k = if (state.selectedMetal == MetalType.GOLD) goldLocalPerGram else silverLocalPerGram,
            currencyCode = currencyCode,
            onGramsChange = onCalculatorGramsChange,
            onPurityChange = onCalculatorPurityChange,
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
          PurityBreakdownCard(
            selectedMetal = state.selectedMetal,
            country = state.selectedCountry,
            perGramPure = if (state.selectedMetal == MetalType.GOLD) goldLocalPerGram else silverLocalPerGram,
            perOz = if (state.selectedMetal == MetalType.GOLD) goldLocalPerOz else silverLocalPerOz,
            currencyCode = currencyCode,
          )
          GlobalMarketsSnapshotCard(
            goldUsdPerOz = state.goldUsdPerOz,
            silverUsdPerOz = state.silverUsdPerOz,
            ratesMap = ratesMap,
            displayLocale = displayLocale,
            selectedIso = state.selectedCountry.isoCode,
            onSelectCountry = onSelectCountry,
          )
        }
      }
    } else {
      MetalTrendChartCard(
        state = state,
        currencyCode = currencyCode,
        accentColor = activeMetalColor,
        onSelectRange = onSelectRange,
        onSelectChartUnit = onSelectChartUnit,
        onSelectChartPurity = onSelectChartPurity,
      )
      PurityBreakdownCard(
        selectedMetal = state.selectedMetal,
        country = state.selectedCountry,
        perGramPure = if (state.selectedMetal == MetalType.GOLD) goldLocalPerGram else silverLocalPerGram,
        perOz = if (state.selectedMetal == MetalType.GOLD) goldLocalPerOz else silverLocalPerOz,
        currencyCode = currencyCode,
      )
      BullionCalculatorCard(
        state = state,
        perGram24k = if (state.selectedMetal == MetalType.GOLD) goldLocalPerGram else silverLocalPerGram,
        currencyCode = currencyCode,
        onGramsChange = onCalculatorGramsChange,
        onPurityChange = onCalculatorPurityChange,
      )
      GlobalMarketsSnapshotCard(
        goldUsdPerOz = state.goldUsdPerOz,
        silverUsdPerOz = state.silverUsdPerOz,
        ratesMap = ratesMap,
        displayLocale = displayLocale,
        selectedIso = state.selectedCountry.isoCode,
        onSelectCountry = onSelectCountry,
      )
    }
  }

  if (showCountryPicker) {
    CountryPickerBottomSheet(
      countries = CountryMetalsCatalog.countries,
      selectedCountry = state.selectedCountry,
      displayLocale = displayLocale,
      onSelect = onSelectCountry,
      onDismiss = { showCountryPicker = false },
    )
  }
}

@Composable
private fun MetalSpotCard(
  title: String,
  localPricePerOz: Double,
  localPricePerGram: Double,
  usdPerOz: Double,
  changePct: Double,
  currencyCode: String,
  accentColor: Color,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val financial = LocalFinancialColors.current
  val isGain = changePct >= 0.0
  Card(
    onClick = onClick,
    modifier = modifier,
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelected) accentColor.copy(alpha = 0.08f)
          else MaterialTheme.colorScheme.surface,
      ),
    border =
      BorderStroke(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
      ),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = accentColor,
        )
        if (changePct != 0.0) {
          val sign = if (isGain) "+" else ""
          Text(
            text = "$sign${String.format(Locale.US, "%.2f", changePct)}%",
            style = NumericSmallStyle,
            color = if (isGain) financial.gain else financial.loss,
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = FormattingUtils.formatMoney(localPricePerGram, currencyCode),
        style = NumericMediumStyle.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "/ g (24K / 999)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = "${FormattingUtils.formatMoney(localPricePerOz, currencyCode)} / oz",
        style = NumericSmallStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (currencyCode != "USD") {
        Text(
          text = "$${FormattingUtils.formatMoney(usdPerOz)} USD / oz",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
      }
    }
  }
}

@Composable
private fun MetalTrendChartCard(
  state: MetalsTrackerState,
  currencyCode: String,
  accentColor: Color,
  onSelectRange: (ChartRange) -> Unit,
  onSelectChartUnit: (MetalChartUnit) -> Unit,
  onSelectChartPurity: (Double) -> Unit,
) {
  val unitFactor =
    when (state.chartUnit) {
      MetalChartUnit.OZ -> 1.0
      MetalChartUnit.GRAM -> 1.0 / GRAMS_PER_TROY_OZ
      MetalChartUnit.COIN -> STANDARD_COIN_GRAMS / GRAMS_PER_TROY_OZ
    }

  val purities =
    if (state.selectedMetal == MetalType.GOLD) {
      listOf(
        "24K" to 1.0,
        "22K" to (22.0 / 24.0),
        "21K" to (21.0 / 24.0),
        "18K" to (18.0 / 24.0),
        "14K" to (14.0 / 24.0),
      )
    } else {
      listOf(
        "999" to 1.0,
        "925" to 0.925,
        "900" to 0.900,
        "800" to 0.800,
      )
    }

  val activePurityLabel =
    purities.minByOrNull { kotlin.math.abs(it.second - state.chartPurityFraction) }?.first
      ?: if (state.selectedMetal == MetalType.GOLD) "24K" else "999"

  val coinLabelRes =
    if (state.selectedMetal == MetalType.GOLD) R.string.graph_unit_gold_coin
    else R.string.graph_unit_silver_coin

  val unitSuffix =
    when (state.chartUnit) {
      MetalChartUnit.OZ -> "oz"
      MetalChartUnit.GRAM -> "g"
      MetalChartUnit.COIN -> stringResource(R.string.suffix_coin)
    }

  val scaledPoints =
    remember(state.chartPoints, unitFactor, state.chartPurityFraction) {
      val combinedFactor = unitFactor * state.chartPurityFraction
      state.chartPoints.map { ChartSeriesPoint(it.label, it.value * combinedFactor) }
    }

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.metal_chart_title),
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "${state.selectedMetal.code} / $currencyCode · $activePurityLabel ($unitSuffix)",
            style = MaterialTheme.typography.bodySmall,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Spacer(Modifier.height(10.dp))
        ChartRangeSelector(
          selected = state.chartRange,
          onSelect = onSelectRange,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        // Graph Unit Selector: Troy Oz / Gram / Gold or Silver Coin
        Text(
          text = stringResource(R.string.chart_unit_label),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(
            MetalChartUnit.OZ to R.string.graph_unit_oz,
            MetalChartUnit.GRAM to R.string.graph_unit_gram,
            MetalChartUnit.COIN to coinLabelRes,
          ).forEach { (unitOption, labelRes) ->
            val active = state.chartUnit == unitOption
            Surface(
              onClick = { onSelectChartUnit(unitOption) },
              shape = RoundedCornerShape(10.dp),
              color =
                if (active) accentColor
                else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.weight(1f).height(48.dp).testTag("metal_chart_unit_${unitOption.name.lowercase()}"),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                Text(
                  text = stringResource(labelRes),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color =
                    if (active) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
        Spacer(Modifier.height(10.dp))
        // Graph Purity Selector below the unit options
        Text(
          text = stringResource(R.string.chart_purity_label),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          purities.forEach { (label, frac) ->
            val active = kotlin.math.abs(state.chartPurityFraction - frac) < 0.001
            Surface(
              onClick = { onSelectChartPurity(frac) },
              shape = RoundedCornerShape(10.dp),
              color =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.weight(1f).height(48.dp).testTag("metal_chart_purity_${label.lowercase()}"),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color =
                    if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
      Spacer(Modifier.height(14.dp))
      FinancialTrendChart(
        points = scaledPoints,
        accentColor = accentColor,
        valueSuffix = "$currencyCode/$unitSuffix",
        isLoading = state.isChartLoading,
      )
    }
  }
}

@Composable
private fun PurityBreakdownCard(
  selectedMetal: MetalType,
  country: CountryMarket,
  perGramPure: Double,
  perOz: Double,
  currencyCode: String,
) {
  val rows =
    if (selectedMetal == MetalType.GOLD) {
      buildList {
        add(stringResource(R.string.gram_24k_label) to perGramPure)
        add(stringResource(R.string.gram_22k_label) to perGramPure * (22.0 / 24.0))
        add(stringResource(R.string.gram_21k_label) to perGramPure * (21.0 / 24.0))
        add(stringResource(R.string.gram_18k_label) to perGramPure * (18.0 / 24.0))
        add(stringResource(R.string.gram_14k_label) to perGramPure * (14.0 / 24.0))
        add(stringResource(R.string.spot_oz_label) to perOz)
        add(stringResource(R.string.kilogram_label) to perGramPure * 1000.0)
        country.localUnit?.let { u ->
          add("${u.name} (${stringResource(R.string.local_unit_label)})" to perGramPure * u.grams)
        }
      }
    } else {
      buildList {
        add(stringResource(R.string.silver_999_label) to perGramPure)
        add(stringResource(R.string.silver_925_label) to perGramPure * 0.925)
        add(stringResource(R.string.silver_900_label) to perGramPure * 0.900)
        add(stringResource(R.string.silver_800_label) to perGramPure * 0.800)
        add(stringResource(R.string.spot_oz_label) to perOz)
        add(stringResource(R.string.kilogram_label) to perGramPure * 1000.0)
        country.localUnit?.let { u ->
          add("${u.name} (${stringResource(R.string.local_unit_label)})" to perGramPure * u.grams)
        }
      }
    }

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Text(
        text = stringResource(R.string.purity_breakdown_title),
        style = MaterialTheme.typography.titleMedium,
      )
      Spacer(Modifier.height(10.dp))
      rows.forEachIndexed { idx, (label, price) ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = FormattingUtils.formatMoney(price, currencyCode),
            style = NumericMediumStyle,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        if (idx < rows.lastIndex) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
      }
    }
  }
}

@Composable
private fun BullionCalculatorCard(
  state: MetalsTrackerState,
  perGram24k: Double,
  currencyCode: String,
  onGramsChange: (String) -> Unit,
  onPurityChange: (Double) -> Unit,
) {
  val purities =
    if (state.selectedMetal == MetalType.GOLD) {
      listOf(
        "24K" to 1.0,
        "22K" to (22.0 / 24.0),
        "21K" to (21.0 / 24.0),
        "18K" to (18.0 / 24.0),
        "14K" to (14.0 / 24.0),
      )
    } else {
      listOf(
        "999" to 1.0,
        "925" to 0.925,
        "900" to 0.900,
        "800" to 0.800,
      )
    }
  val grams = state.calculatorGramsText.toDoubleOrNull() ?: 0.0
  val total = grams * perGram24k * state.calculatorPurityFraction

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Text(
        text = stringResource(R.string.weight_calculator_title),
        style = MaterialTheme.typography.titleMedium,
      )
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = state.calculatorGramsText,
        onValueChange = onGramsChange,
        label = { Text(stringResource(R.string.weight_grams_hint)) },
        suffix = { Text("g") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("bullion_grams_input"),
      )
      Spacer(Modifier.height(12.dp))
      Text(
        text = stringResource(R.string.purity_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        purities.forEach { (label, frac) ->
          val active = kotlin.math.abs(state.calculatorPurityFraction - frac) < 0.001
          Surface(
            onClick = { onPurityChange(frac) },
            shape = RoundedCornerShape(10.dp),
            color =
              if (active) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f).height(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color =
                  if (active) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
      Spacer(Modifier.height(14.dp))
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.estimated_value),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = FormattingUtils.formatMoney(total, currencyCode),
            style = NumericDisplayStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun GlobalMarketsSnapshotCard(
  goldUsdPerOz: Double,
  silverUsdPerOz: Double,
  ratesMap: Map<String, Double>,
  displayLocale: Locale,
  selectedIso: String,
  onSelectCountry: (CountryMarket) -> Unit,
) {
  val featuredIsos = listOf("US", "SA", "AE", "EG", "IN", "CN", "JP", "GB", "DE", "TR", "CH")
  val markets = remember { featuredIsos.map { CountryMetalsCatalog.find(it) } }

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Public,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = stringResource(R.string.global_markets_title),
          style = MaterialTheme.typography.titleMedium,
        )
      }
      Spacer(Modifier.height(10.dp))
      markets.forEachIndexed { idx, country ->
        val fx = ratesMap[country.currencyCode] ?: 1.0
        val goldG = (goldUsdPerOz * fx) / GRAMS_PER_TROY_OZ
        val silverG = (silverUsdPerOz * fx) / GRAMS_PER_TROY_OZ
        val active = country.isoCode == selectedIso
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable { onSelectCountry(country) }
              .padding(horizontal = 8.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(text = country.flag, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(10.dp))
            Column {
              Text(
                text = country.localizedName(displayLocale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                color =
                  if (active) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = country.currencyCode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "Au ${FormattingUtils.formatMoney(goldG, country.currencyCode)}",
              style = NumericSmallStyle,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Ag ${FormattingUtils.formatMoney(silverG, country.currencyCode)}",
              style = NumericSmallStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        if (idx < markets.lastIndex) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
      }
    }
  }
}
