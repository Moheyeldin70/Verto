package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.ThemeMode
import com.example.data.repository.ChartRange
import com.example.domain.CountryMarket
import com.example.domain.CountryMetalsCatalog
import com.example.domain.CurrencyInfo
import com.example.domain.MeasurementLocalePreference
import com.example.domain.UnitDef
import com.example.ui.theme.LocalFinancialColors
import java.util.Locale

@Composable
fun StatusPill(
  isLive: Boolean,
  isSyncing: Boolean,
  modifier: Modifier = Modifier,
) {
  val financial = LocalFinancialColors.current
  val dotColor =
    when {
      isSyncing -> MaterialTheme.colorScheme.primary
      isLive -> financial.gain
      else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
  val label =
    when {
      isSyncing -> stringResource(R.string.status_syncing)
      isLive -> stringResource(R.string.status_live)
      else -> stringResource(R.string.status_cached)
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = dotColor.copy(alpha = 0.14f),
    border = BorderStroke(1.dp, dotColor.copy(alpha = 0.25f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dotColor))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = dotColor,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun SwapButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilledTonalIconButton(
    onClick = onClick,
    modifier = modifier.size(48.dp).testTag("swap_button"),
    colors =
      IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
      ),
  ) {
    Icon(
      imageVector = Icons.Rounded.SwapVert,
      contentDescription = stringResource(R.string.action_swap),
    )
  }
}

@Composable
fun ChartRangeSelector(
  selected: ChartRange,
  onSelect: (ChartRange) -> Unit,
  modifier: Modifier = Modifier,
) {
  val items =
    listOf(
      ChartRange.D7 to R.string.range_7d,
      ChartRange.M1 to R.string.range_1m,
      ChartRange.M3 to R.string.range_3m,
      ChartRange.Y1 to R.string.range_1y,
    )
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    items.forEach { (range, labelRes) ->
      val active = range == selected
      Surface(
        onClick = { onSelect(range) },
        shape = RoundedCornerShape(10.dp),
        color =
          if (active) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(34.dp).testTag("range_${range.name.lowercase()}"),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.padding(horizontal = 12.dp),
        ) {
          Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color =
              if (active) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitPickerBottomSheet(
  units: List<UnitDef>,
  selectedUnit: UnitDef,
  onSelect: (UnitDef) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val filtered =
    remember(units, query) {
      if (query.isBlank()) units
      else {
        val q = query.trim().lowercase()
        units.filter { it.name.lowercase().contains(q) || it.symbol.lowercase().contains(q) }
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
      Text(
        text = stringResource(R.string.select_unit),
        style = MaterialTheme.typography.titleLarge,
      )
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text(stringResource(R.string.search_units)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(12.dp))
      LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
        if (filtered.isEmpty()) {
          item {
            Text(
              text = stringResource(R.string.no_results),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 24.dp),
            )
          }
        } else {
          items(filtered, key = { it.id }) { unit ->
            val isSelected = unit.id == selectedUnit.id
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    onSelect(unit)
                    onDismiss()
                  }
                  .padding(horizontal = 12.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                  Text(
                    text = unit.symbol,
                    style = MaterialTheme.typography.labelLarge,
                    color =
                      if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                  text = unit.name,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
              }
              if (isSelected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          }
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerBottomSheet(
  currencies: List<CurrencyInfo>,
  selectedCode: String,
  onSelect: (CurrencyInfo) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val filtered =
    remember(currencies, query) {
      if (query.isBlank()) currencies
      else {
        val q = query.trim().lowercase()
        currencies.filter {
          it.code.lowercase().contains(q) ||
            it.name.lowercase().contains(q) ||
            it.symbol.lowercase().contains(q)
        }
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
      Text(
        text = stringResource(R.string.select_currency),
        style = MaterialTheme.typography.titleLarge,
      )
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text(stringResource(R.string.search_currencies)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("currency_search_input"),
      )
      Spacer(Modifier.height(12.dp))
      LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
        if (filtered.isEmpty()) {
          item {
            Text(
              text = stringResource(R.string.no_results),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 24.dp),
            )
          }
        } else {
          items(filtered, key = { it.code }) { item ->
            val isSelected = item.code == selectedCode
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    onSelect(item)
                    onDismiss()
                  }
                  .padding(horizontal = 10.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(text = item.flag, style = MaterialTheme.typography.headlineSmall)
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${item.code} • ${item.symbol}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = item.name,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              if (isSelected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          }
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerBottomSheet(
  countries: List<CountryMarket>,
  selectedCountry: CountryMarket,
  displayLocale: Locale,
  onSelect: (CountryMarket) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val filtered =
    remember(countries, query, displayLocale) {
      if (query.isBlank()) countries
      else {
        val q = query.trim().lowercase()
        countries.filter {
          it.name.lowercase().contains(q) ||
            it.localizedName(displayLocale).lowercase().contains(q) ||
            it.currencyCode.lowercase().contains(q) ||
            it.isoCode.lowercase().contains(q)
        }
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
      Text(
        text = stringResource(R.string.select_country),
        style = MaterialTheme.typography.titleLarge,
      )
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text(stringResource(R.string.search_countries)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("country_search_input"),
      )
      Spacer(Modifier.height(12.dp))
      LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
        if (filtered.isEmpty()) {
          item {
            Text(
              text = stringResource(R.string.no_results),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 24.dp),
            )
          }
        } else {
          items(filtered, key = { it.isoCode }) { country ->
            val isSelected = country.isoCode == selectedCountry.isoCode
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    onSelect(country)
                    onDismiss()
                  }
                  .padding(horizontal = 10.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(text = country.flag, style = MaterialTheme.typography.headlineSmall)
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = country.localizedName(displayLocale),
                  style = MaterialTheme.typography.titleMedium,
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val subtitle =
                  if (country.localUnit != null) "${country.currencyCode} • ${country.localUnit.name}"
                  else country.currencyCode
                Text(
                  text = subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (isSelected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          }
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageAndThemeSheet(
  selectedLanguage: AppLanguage,
  selectedTheme: ThemeMode,
  selectedMeasurementLocale: MeasurementLocalePreference,
  detectedCountryIso: String,
  isDetectingLocation: Boolean,
  displayLocale: Locale,
  onDetectLocation: () -> Unit,
  onSelectMeasurementLocale: (MeasurementLocalePreference) -> Unit,
  onSelectLanguage: (AppLanguage) -> Unit,
  onSelectTheme: (ThemeMode) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
      Text(
        text = stringResource(R.string.location_region_section),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(8.dp))

      val activeRegion =
        remember(detectedCountryIso) {
          if (detectedCountryIso.isNotBlank()) CountryMetalsCatalog.find(detectedCountryIso) else null
        }

      FilledTonalButton(
        onClick = onDetectLocation,
        enabled = !isDetectingLocation,
        colors =
          ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
          ),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("detect_location_button"),
      ) {
        if (isDetectingLocation) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(Modifier.width(8.dp))
          Text(text = stringResource(R.string.detecting_location))
        } else {
          Icon(
            imageVector = Icons.Rounded.MyLocation,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          val suffix =
            if (activeRegion != null) " (${activeRegion.flag} ${activeRegion.localizedName(displayLocale)})"
            else ""
          Text(
            text = stringResource(R.string.detect_location_button) + suffix,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(Modifier.height(10.dp))
      Text(
        text = stringResource(R.string.unit_system_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf(
          MeasurementLocalePreference.AUTO to R.string.unit_system_auto,
          MeasurementLocalePreference.METRIC to R.string.unit_system_metric,
          MeasurementLocalePreference.IMPERIAL_US to R.string.unit_system_imperial_us,
          MeasurementLocalePreference.UK to R.string.unit_system_uk,
        ).forEach { (pref, labelRes) ->
          val active = pref == selectedMeasurementLocale
          Surface(
            onClick = { onSelectMeasurementLocale(pref) },
            shape = RoundedCornerShape(10.dp),
            color =
              if (active) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f).height(48.dp).testTag("unit_sys_${pref.key.lowercase()}"),
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
              Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color =
                  if (active) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(18.dp))
      Text(
        text = stringResource(R.string.choose_theme),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          ThemeMode.SYSTEM to R.string.theme_system,
          ThemeMode.LIGHT to R.string.theme_light,
          ThemeMode.DARK to R.string.theme_dark,
        ).forEach { (mode, labelRes) ->
          val active = mode == selectedTheme
          Surface(
            onClick = { onSelectTheme(mode) },
            shape = RoundedCornerShape(12.dp),
            color =
              if (active) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f).height(48.dp).testTag("theme_${mode.key.lowercase()}"),
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
              Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color =
                  if (active) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(20.dp))
      Text(
        text = stringResource(R.string.choose_language),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(8.dp))

      LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
        items(AppLanguage.entries) { lang ->
          val isSelected = lang == selectedLanguage
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  onSelectLanguage(lang)
                  onDismiss()
                }
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = lang.flag, style = MaterialTheme.typography.titleLarge)
              Spacer(Modifier.width(12.dp))
              Text(
                text =
                  if (lang == AppLanguage.SYSTEM) stringResource(R.string.language_system)
                  else lang.nativeLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color =
                  if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface,
              )
            }
            if (isSelected) {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}
