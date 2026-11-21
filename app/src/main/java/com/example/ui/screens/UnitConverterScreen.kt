package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.domain.FormattingUtils
import com.example.domain.UnitCatalog
import com.example.domain.UnitCategory
import com.example.domain.UnitDef
import com.example.ui.UnitConverterState
import com.example.ui.components.SwapButton
import com.example.ui.components.UnitPickerBottomSheet
import com.example.ui.theme.NumericDisplayStyle
import com.example.ui.theme.NumericMediumStyle
import com.example.ui.theme.NumericSmallStyle

@Composable
fun UnitConverterScreen(
  state: UnitConverterState,
  isWideLayout: Boolean,
  onSelectCategory: (UnitCategory) -> Unit,
  onSelectFromUnit: (UnitDef) -> Unit,
  onSelectToUnit: (UnitDef) -> Unit,
  onSwap: () -> Unit,
  onInputChange: (String) -> Unit,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var pickingForFrom by remember { mutableStateOf<Boolean?>(null) }
  val unitsInCategory = remember(state.category) { UnitCatalog.unitsFor(state.category) }
  val clipboard = LocalClipboardManager.current
  val copiedMsg = stringResource(R.string.copied_feedback)

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 88.dp),
  ) {
    // Category Chips
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(UnitCategory.entries) { category ->
        val selected = category == state.category
        Surface(
          onClick = { onSelectCategory(category) },
          shape = RoundedCornerShape(12.dp),
          color =
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface,
          border =
            if (selected) null
            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.height(40.dp).testTag("unit_cat_${category.name.lowercase()}"),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(
              text = stringResource(category.labelRes),
              style = MaterialTheme.typography.labelLarge,
              color =
                if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    Spacer(Modifier.height(8.dp))

    if (isWideLayout) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
          UnitInputOutputCard(
            state = state,
            onInputChange = onInputChange,
            onPickFrom = { pickingForFrom = true },
            onPickTo = { pickingForFrom = false },
            onSwap = onSwap,
            onCopyOutput = {
              clipboard.setText(AnnotatedString(FormattingUtils.formatSmart(state.outputValue)))
              onShowMessage(copiedMsg)
            },
          )
        }
        Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
          AllUnitsBreakdownCard(
            state = state,
            unitsInCategory = unitsInCategory,
            onSelectTarget = onSelectToUnit,
          )
        }
      }
    } else {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        UnitInputOutputCard(
          state = state,
          onInputChange = onInputChange,
          onPickFrom = { pickingForFrom = true },
          onPickTo = { pickingForFrom = false },
          onSwap = onSwap,
          onCopyOutput = {
            clipboard.setText(AnnotatedString(FormattingUtils.formatSmart(state.outputValue)))
            onShowMessage(copiedMsg)
          },
        )
        AllUnitsBreakdownCard(
          state = state,
          unitsInCategory = unitsInCategory,
          onSelectTarget = onSelectToUnit,
        )
      }
    }
  }

  pickingForFrom?.let { isFrom ->
    UnitPickerBottomSheet(
      units = unitsInCategory,
      selectedUnit = if (isFrom) state.fromUnit else state.toUnit,
      onSelect = { chosen ->
        if (isFrom) onSelectFromUnit(chosen) else onSelectToUnit(chosen)
      },
      onDismiss = { pickingForFrom = null },
    )
  }
}

@Composable
private fun UnitInputOutputCard(
  state: UnitConverterState,
  onInputChange: (String) -> Unit,
  onPickFrom: () -> Unit,
  onPickTo: () -> Unit,
  onSwap: () -> Unit,
  onCopyOutput: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      // FROM block
      Text(
        text = stringResource(R.string.from_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        BasicTextField(
          value = state.inputText,
          onValueChange = onInputChange,
          singleLine = true,
          textStyle = NumericDisplayStyle.copy(color = MaterialTheme.colorScheme.onSurface),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f).testTag("unit_input_field"),
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
        UnitSelectorPill(
          unit = state.fromUnit,
          onClick = onPickFrom,
          modifier = Modifier.testTag("unit_from_selector"),
        )
      }

      Spacer(Modifier.height(14.dp))

      // Divider with Swap button
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwapButton(onClick = onSwap)
      }

      Spacer(Modifier.height(14.dp))

      // TO block
      Text(
        text = stringResource(R.string.to_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = FormattingUtils.formatSmart(state.outputValue),
          style = NumericDisplayStyle,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).testTag("unit_output_text"),
        )
        IconButton(onClick = onCopyOutput, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = stringResource(R.string.action_copy),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
        UnitSelectorPill(
          unit = state.toUnit,
          onClick = onPickTo,
          modifier = Modifier.testTag("unit_to_selector"),
        )
      }

      Spacer(Modifier.height(14.dp))

      // Formula chip
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Functions,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = UnitCatalog.describeRelationship(state.fromUnit, state.toUnit),
            style = NumericSmallStyle,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}

@Composable
private fun UnitSelectorPill(
  unit: UnitDef,
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
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Column {
        Text(
          text = unit.symbol,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Icon(
        imageVector = Icons.Rounded.ArrowDropDown,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun AllUnitsBreakdownCard(
  state: UnitConverterState,
  unitsInCategory: List<UnitDef>,
  onSelectTarget: (UnitDef) -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Text(
        text = stringResource(R.string.all_conversions_title),
        style = MaterialTheme.typography.titleMedium,
      )
      Spacer(Modifier.height(10.dp))
      unitsInCategory.forEachIndexed { idx, unit ->
        val value = UnitCatalog.convert(state.inputValue, state.fromUnit, unit)
        val isTarget = unit.id == state.toUnit.id
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable { onSelectTarget(unit) }
              .padding(horizontal = 8.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = unit.name,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium,
              color =
                if (isTarget) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = unit.symbol,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = "${FormattingUtils.formatSmart(value)} ${unit.symbol}",
            style = NumericMediumStyle,
            color =
              if (isTarget) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurface,
          )
        }
        if (idx < unitsInCategory.lastIndex) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
      }
    }
  }
}
