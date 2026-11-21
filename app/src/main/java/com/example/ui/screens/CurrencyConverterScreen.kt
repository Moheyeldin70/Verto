package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SavedConversionEntity
import com.example.data.repository.ChartRange
import com.example.data.repository.SyncStatus
import com.example.domain.CurrencyCatalog
import com.example.domain.CurrencyInfo
import com.example.domain.FormattingUtils
import com.example.ui.CurrencyConverterState
import com.example.ui.components.ChartRangeSelector
import com.example.ui.components.ChartSeriesPoint
import com.example.ui.components.CurrencyPickerBottomSheet
import com.example.ui.components.FinancialTrendChart
import com.example.ui.components.SwapButton
import com.example.ui.theme.NumericDisplayStyle
import com.example.ui.theme.NumericMediumStyle
import com.example.ui.theme.NumericSmallStyle
import java.util.Locale

@Composable
fun CurrencyConverterScreen(
  state: CurrencyConverterState,
  ratesMap: Map<String, Double>,
  savedConversions: List<SavedConversionEntity>,
  syncStatus: SyncStatus,
  displayLocale: Locale,
  isWideLayout: Boolean,
  onInputChange: (String) -> Unit,
  onSelectFromCurrency: (String) -> Unit,
  onSelectToCurrency: (String) -> Unit,
  onSwap: () -> Unit,
  onSelectRange: (ChartRange) -> Unit,
  onSaveConversion: () -> Unit,
  onApplySaved: (SavedConversionEntity) -> Unit,
  onDeleteSaved: (Long) -> Unit,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var pickingForFrom by remember { mutableStateOf<Boolean?>(null) }
  val clipboard = LocalClipboardManager.current
  val copiedMsg = stringResource(R.string.copied_feedback)
  val savedMsg = stringResource(R.string.action_saved)

  val allCurrencies =
    remember(ratesMap) {
      val knownCodes = CurrencyCatalog.all.map { it.code }.toSet()
      val extra =
        ratesMap.keys
          .filter { it !in knownCodes && it != "XAU" && it != "XAG" }
          .sorted()
          .map { code -> CurrencyInfo(code, code, code, "🌐", ratesMap[code] ?: 1.0) }
      CurrencyCatalog.all + extra
    }

  val savedCodes =
    remember(savedConversions) {
      savedConversions.flatMap { listOf(it.fromCode, it.toCode) }.toSet()
    }

  val fromInfo = remember(state.fromCode) { CurrencyCatalog.find(state.fromCode) }
  val toInfo = remember(state.toCode) { CurrencyCatalog.find(state.toCode) }
  val fromRate = ratesMap[state.fromCode] ?: fromInfo.fallbackRateFromUsd
  val toRate = ratesMap[state.toCode] ?: toInfo.fallbackRateFromUsd
  val unitRate = if (fromRate > 0.0) toRate / fromRate else 1.0
  val inverseRate = if (unitRate > 0.0) 1.0 / unitRate else 1.0
  val inputAmount = state.inputText.toDoubleOrNull() ?: 0.0
  val outputAmount = inputAmount * unitRate

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (!syncStatus.isLive && !syncStatus.isNetworkAvailable) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = stringResource(R.string.offline_banner),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    if (isWideLayout) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
          CurrencyConverterCard(
            state = state,
            fromInfo = fromInfo,
            toInfo = toInfo,
            outputAmount = outputAmount,
            unitRate = unitRate,
            inverseRate = inverseRate,
            lastUpdatedMillis = syncStatus.lastUpdatedMillis,
            displayLocale = displayLocale,
            onInputChange = onInputChange,
            onPickFrom = { pickingForFrom = true },
            onPickTo = { pickingForFrom = false },
            onSwap = onSwap,
            onCopy = {
              clipboard.setText(AnnotatedString(FormattingUtils.formatMoney(outputAmount, toInfo.code)))
              onShowMessage(copiedMsg)
            },
            onSave = {
              onSaveConversion()
              onShowMessage(savedMsg)
            },
          )
          SavedPairsCard(
            savedConversions = savedConversions,
            onApply = onApplySaved,
            onDelete = onDeleteSaved,
          )
        }
        Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
          CurrencyChartCard(
            state = state,
            onSelectRange = onSelectRange,
          )
          MajorWorldCurrenciesCard(
            allCurrencies = allCurrencies,
            fromCode = state.fromCode,
            toCode = state.toCode,
            inputAmount = inputAmount,
            fromRate = fromRate,
            ratesMap = ratesMap,
            savedCodes = savedCodes,
            onSelectTarget = onSelectToCurrency,
          )
        }
      }
    } else {
      CurrencyConverterCard(
        state = state,
        fromInfo = fromInfo,
        toInfo = toInfo,
        outputAmount = outputAmount,
        unitRate = unitRate,
        inverseRate = inverseRate,
        lastUpdatedMillis = syncStatus.lastUpdatedMillis,
        displayLocale = displayLocale,
        onInputChange = onInputChange,
        onPickFrom = { pickingForFrom = true },
        onPickTo = { pickingForFrom = false },
        onSwap = onSwap,
        onCopy = {
          clipboard.setText(AnnotatedString(FormattingUtils.formatMoney(outputAmount, toInfo.code)))
          onShowMessage(copiedMsg)
        },
        onSave = {
          onSaveConversion()
          onShowMessage(savedMsg)
        },
      )
      CurrencyChartCard(
        state = state,
        onSelectRange = onSelectRange,
      )
      MajorWorldCurrenciesCard(
        allCurrencies = allCurrencies,
        fromCode = state.fromCode,
        toCode = state.toCode,
        inputAmount = inputAmount,
        fromRate = fromRate,
        ratesMap = ratesMap,
        savedCodes = savedCodes,
        onSelectTarget = onSelectToCurrency,
      )
      SavedPairsCard(
        savedConversions = savedConversions,
        onApply = onApplySaved,
        onDelete = onDeleteSaved,
      )
    }
  }

  pickingForFrom?.let { isFrom ->
    CurrencyPickerBottomSheet(
      currencies = allCurrencies,
      selectedCode = if (isFrom) state.fromCode else state.toCode,
      ratesMap = ratesMap,
      baseCurrencyCode = state.fromCode,
      savedCodes = savedCodes,
      onSelect = { chosen ->
        if (isFrom) onSelectFromCurrency(chosen.code) else onSelectToCurrency(chosen.code)
      },
      onDismiss = { pickingForFrom = null },
    )
  }
}

@Composable
private fun CurrencyConverterCard(
  state: CurrencyConverterState,
  fromInfo: CurrencyInfo,
  toInfo: CurrencyInfo,
  outputAmount: Double,
  unitRate: Double,
  inverseRate: Double,
  lastUpdatedMillis: Long,
  displayLocale: Locale,
  onInputChange: (String) -> Unit,
  onPickFrom: () -> Unit,
  onPickTo: () -> Unit,
  onSwap: () -> Unit,
  onCopy: () -> Unit,
  onSave: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Text(
        text = stringResource(R.string.from_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BasicTextField(
          value = state.inputText,
          onValueChange = onInputChange,
          singleLine = true,
          textStyle = NumericDisplayStyle.copy(color = MaterialTheme.colorScheme.onSurface),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f).testTag("currency_input_field"),
          decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
              if (state.inputText.isEmpty()) {
                Text(
                  text = "0",
                  style = NumericDisplayStyle,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
              }
              inner()
            }
          },
        )
        Spacer(Modifier.width(12.dp))
        CurrencySelectorPill(
          info = fromInfo,
          onClick = onPickFrom,
          modifier = Modifier.testTag("currency_from_selector"),
        )
      }

      Spacer(Modifier.height(14.dp))
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwapButton(onClick = onSwap)
      }
      Spacer(Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.to_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = FormattingUtils.formatMoney(outputAmount),
          style = NumericDisplayStyle,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).testTag("currency_output_text"),
        )
        IconButton(onClick = onCopy, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = stringResource(R.string.action_copy),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
        CurrencySelectorPill(
          info = toInfo,
          onClick = onPickTo,
          modifier = Modifier.testTag("currency_to_selector"),
        )
      }

      Spacer(Modifier.height(14.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.exchange_rate_label),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "1 ${fromInfo.code} = ${FormattingUtils.formatRate(unitRate)} ${toInfo.code}",
              style = NumericSmallStyle,
              fontWeight = FontWeight.Bold,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.inverse_rate_label),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "1 ${toInfo.code} = ${FormattingUtils.formatRate(inverseRate)} ${fromInfo.code}",
              style = NumericSmallStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.last_updated_label),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = FormattingUtils.formatTimestamp(lastUpdatedMillis, displayLocale),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(Modifier.height(12.dp))

      FilledTonalButton(
        onClick = onSave,
        colors =
          ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
          ),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_currency_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.BookmarkAdd,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.action_save))
      }
    }
  }
}

@Composable
private fun CurrencySelectorPill(
  info: CurrencyInfo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = modifier.height(48.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(text = info.flag, style = MaterialTheme.typography.titleMedium)
      Text(
        text = info.code,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Icon(
        imageVector = Icons.Rounded.ArrowDropDown,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CurrencyChartCard(
  state: CurrencyConverterState,
  onSelectRange: (ChartRange) -> Unit,
) {
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
            text = stringResource(R.string.historical_chart_title),
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "${state.fromCode} / ${state.toCode}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Spacer(Modifier.height(10.dp))
        ChartRangeSelector(
          selected = state.chartRange,
          onSelect = onSelectRange,
          modifier = Modifier.fillMaxWidth(),
        )
      }
      Spacer(Modifier.height(14.dp))
      FinancialTrendChart(
        points = state.chartPoints.map { ChartSeriesPoint(it.label, it.value) },
        accentColor = MaterialTheme.colorScheme.primary,
        valueSuffix = state.toCode,
        isLoading = state.isChartLoading,
      )
    }
  }
}

@Composable
private fun MajorWorldCurrenciesCard(
  allCurrencies: List<CurrencyInfo>,
  fromCode: String,
  toCode: String,
  inputAmount: Double,
  fromRate: Double,
  ratesMap: Map<String, Double>,
  savedCodes: Set<String>,
  onSelectTarget: (String) -> Unit,
) {
  var inlineQuery by remember { mutableStateOf("") }
  var selectedRegion by remember { mutableStateOf(CurrencyCatalog.FilterRegion.POPULAR) }
  var userPinnedRegion by remember { mutableStateOf(false) }

  val effectiveRegion =
    if (inlineQuery.isNotBlank() && !userPinnedRegion) CurrencyCatalog.FilterRegion.ALL
    else selectedRegion

  val displayed =
    remember(allCurrencies, inlineQuery, effectiveRegion, savedCodes, fromCode) {
      CurrencyCatalog.filterCurrencies(
        currencies = allCurrencies,
        query = inlineQuery,
        region = effectiveRegion,
        savedCodes = savedCodes,
      ).filter { inlineQuery.isNotBlank() || it.code != fromCode }
    }

  val regionChips =
    remember(savedCodes) {
      buildList {
        add(CurrencyCatalog.FilterRegion.POPULAR to R.string.filter_popular)
        add(CurrencyCatalog.FilterRegion.ALL to R.string.filter_all)
        if (savedCodes.isNotEmpty()) {
          add(CurrencyCatalog.FilterRegion.SAVED to R.string.filter_saved)
        }
        add(CurrencyCatalog.FilterRegion.MENA to R.string.region_mena)
        add(CurrencyCatalog.FilterRegion.EUROPE to R.string.region_europe)
        add(CurrencyCatalog.FilterRegion.ASIA_PACIFIC to R.string.region_asia)
        add(CurrencyCatalog.FilterRegion.AMERICAS to R.string.region_americas)
      }
    }

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Text(
        text = stringResource(R.string.popular_currencies_title),
        style = MaterialTheme.typography.titleMedium,
      )
      Spacer(Modifier.height(10.dp))
      OutlinedTextField(
        value = inlineQuery,
        onValueChange = { inlineQuery = it },
        placeholder = { Text(stringResource(R.string.search_currencies)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (inlineQuery.isNotEmpty()) {
            IconButton(onClick = { inlineQuery = "" }) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("currency_inline_filter_input"),
      )
      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        regionChips.forEach { (region, labelRes) ->
          val active = effectiveRegion == region
          FilterChip(
            selected = active,
            onClick = {
              selectedRegion = region
              userPinnedRegion = true
            },
            label = { Text(stringResource(labelRes), maxLines = 1) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary,
              ),
            modifier =
              Modifier
                .heightIn(min = 40.dp)
                .testTag("currency_inline_filter_${region.name.lowercase()}"),
          )
        }
      }
      Spacer(Modifier.height(8.dp))

      if (displayed.isEmpty()) {
        Text(
          text = stringResource(R.string.no_results),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 12.dp),
        )
      } else if (displayed.size > 12) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
          itemsIndexed(displayed, key = { _, item -> item.code }) { idx, info ->
            CurrencyBreakdownRow(
              info = info,
              isTarget = info.code == toCode,
              inputAmount = inputAmount,
              fromRate = fromRate,
              ratesMap = ratesMap,
              showDivider = idx < displayed.lastIndex,
              onSelect = { onSelectTarget(info.code) },
            )
          }
        }
      } else {
        displayed.forEachIndexed { idx, info ->
          CurrencyBreakdownRow(
            info = info,
            isTarget = info.code == toCode,
            inputAmount = inputAmount,
            fromRate = fromRate,
            ratesMap = ratesMap,
            showDivider = idx < displayed.lastIndex,
            onSelect = { onSelectTarget(info.code) },
          )
        }
      }
    }
  }
}

@Composable
private fun CurrencyBreakdownRow(
  info: CurrencyInfo,
  isTarget: Boolean,
  inputAmount: Double,
  fromRate: Double,
  ratesMap: Map<String, Double>,
  showDivider: Boolean,
  onSelect: () -> Unit,
) {
  val rate = ratesMap[info.code] ?: info.fallbackRateFromUsd
  val converted = if (fromRate > 0.0) inputAmount * (rate / fromRate) else 0.0
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onSelect)
        .padding(horizontal = 8.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Text(text = info.flag, style = MaterialTheme.typography.titleLarge)
      Spacer(Modifier.width(10.dp))
      Column {
        Text(
          text = info.code,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color =
            if (isTarget) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = info.name,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    Text(
      text = "${FormattingUtils.formatMoney(converted)} ${info.symbol}",
      style = NumericMediumStyle,
      color =
        if (isTarget) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
    )
  }
  if (showDivider) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
  }
}

@Composable
private fun SavedPairsCard(
  savedConversions: List<SavedConversionEntity>,
  onApply: (SavedConversionEntity) -> Unit,
  onDelete: (Long) -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Text(
        text = stringResource(R.string.saved_pairs_title),
        style = MaterialTheme.typography.titleMedium,
      )
      Spacer(Modifier.height(10.dp))
      if (savedConversions.isEmpty()) {
        Text(
          text = stringResource(R.string.no_saved_yet),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        savedConversions.forEachIndexed { idx, item ->
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onApply(item) }
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${FormattingUtils.formatMoney(item.inputAmount, item.fromCode)} → ${FormattingUtils.formatMoney(item.outputAmount, item.toCode)}",
                style = NumericSmallStyle,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "1 ${item.fromCode} = ${FormattingUtils.formatRate(item.rateSnapshot)} ${item.toCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(48.dp)) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.action_clear),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          if (idx < savedConversions.lastIndex) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          }
        }
      }
    }
  }
}
