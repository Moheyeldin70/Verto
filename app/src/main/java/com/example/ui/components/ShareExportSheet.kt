package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ShareExportHelper
import com.example.domain.ShareExportMode
import com.example.domain.ShareExportPayload
import com.example.ui.theme.NumericSmallStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareExportSheet(
  payload: ShareExportPayload,
  onShowMessage: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val copiedMsg = stringResource(R.string.copied_feedback)
  val shareChooserTitle = stringResource(R.string.share_chooser_title)
  val shareErrorMsg = stringResource(R.string.share_error_message)

  var selectedMode by remember(payload) {
    mutableStateOf(if (payload.hasChart) ShareExportMode.CHART else ShareExportMode.SUMMARY)
  }

  val previewBitmap =
    remember(payload, selectedMode) {
      ShareExportHelper.renderExportBitmap(payload, selectedMode)
    }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = stringResource(R.string.share_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.action_close),
          )
        }
      }

      if (payload.hasChart) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          val modes =
            listOf(
              ShareExportMode.CHART to (R.string.share_mode_chart to Icons.Rounded.ShowChart),
              ShareExportMode.SUMMARY to (R.string.share_mode_summary to Icons.Rounded.Description),
            )
          modes.forEach { (mode, labelAndIcon) ->
            val (labelRes, icon) = labelAndIcon
            val active = selectedMode == mode
            Surface(
              onClick = { selectedMode = mode },
              shape = RoundedCornerShape(12.dp),
              color =
                if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
              border =
                BorderStroke(
                  width = if (active) 1.5.dp else 1.dp,
                  color =
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier =
                Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("share_mode_${mode.name.lowercase()}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint =
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = stringResource(labelRes),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                  color =
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }

      // Visual Image Card Preview
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        val ratio = previewBitmap.width.toFloat() / previewBitmap.height.toFloat().coerceAtLeast(1f)
        Box(
          modifier = Modifier.fillMaxWidth().padding(10.dp),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = previewBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.share_image_preview_desc),
            contentScale = ContentScale.Fit,
            modifier =
              Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(12.dp)),
          )
        }
      }

      // Text Summary Preview
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.share_text_preview_label),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
              onClick = {
                clipboard.setText(AnnotatedString(payload.textSummary))
                onShowMessage(copiedMsg)
              },
              modifier = Modifier.height(48.dp).testTag("copy_summary_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(text = stringResource(R.string.action_copy), style = MaterialTheme.typography.labelLarge)
            }
          }
          Text(
            text = payload.textSummary,
            style = NumericSmallStyle,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      // Primary Share Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FilledTonalButton(
          onClick = {
            runCatching {
              ShareExportHelper.shareText(
                context = context,
                chooserTitle = shareChooserTitle,
                text = payload.textSummary,
              )
            }.onFailure {
              onShowMessage(shareErrorMsg)
            }
            onDismiss()
          },
          modifier = Modifier.weight(1f).height(52.dp).testTag("share_as_text_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(text = stringResource(R.string.share_as_text))
        }

        Button(
          onClick = {
            runCatching {
              ShareExportHelper.shareImage(
                context = context,
                bitmap = previewBitmap,
                chooserTitle = shareChooserTitle,
                captionText = payload.textSummary,
              )
            }.onFailure {
              onShowMessage(shareErrorMsg)
            }
            onDismiss()
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.weight(1f).height(52.dp).testTag("share_as_image_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(text = stringResource(R.string.share_as_image))
        }
      }
    }
  }
}
