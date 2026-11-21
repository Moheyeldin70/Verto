package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.AiAssistantState
import com.example.ai.AiChatMessage
import com.example.ai.AiUiState
import com.example.ui.AppTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
  activeTab: AppTab,
  contextLabel: String,
  isNetworkAvailable: Boolean,
  aiState: AiAssistantState,
  onSendMessage: (String) -> Unit,
  onClearChat: () -> Unit,
  onDismissError: () -> Unit,
  onDismiss: () -> Unit,
) {
  var promptInput by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()
  val isBusy = aiState.uiState is AiUiState.Loading || aiState.uiState is AiUiState.Streaming
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val quickPrompts =
    remember(activeTab) {
      when (activeTab) {
        AppTab.UNITS ->
          listOf(
            R.string.ai_prompt_unit_explain,
            R.string.ai_prompt_unit_realworld,
            R.string.ai_prompt_unit_compare,
          )
        AppTab.CURRENCY ->
          listOf(
            R.string.ai_prompt_fx_explain,
            R.string.ai_prompt_fx_predict,
            R.string.ai_prompt_fx_tips,
          )
        AppTab.METALS ->
          listOf(
            R.string.ai_prompt_metal_explain,
            R.string.ai_prompt_metal_predict,
            R.string.ai_prompt_metal_purity,
          )
      }
    }

  LaunchedEffect(aiState.messages.size, aiState.messages.lastOrNull()?.text?.length) {
    if (aiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(aiState.messages.lastIndex)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.86f)
          .imePadding(),
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier =
              Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
          Column {
            Text(
              text = stringResource(R.string.ai_sheet_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = contextLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Row {
          if (aiState.messages.isNotEmpty()) {
            IconButton(
              onClick = onClearChat,
              modifier = Modifier.size(48.dp).testTag("ai_clear_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.action_clear),
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp).testTag("ai_close_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = stringResource(R.string.action_close),
            )
          }
        }
      }

      Spacer(Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

      // Quick Suggestion Chips
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        quickPrompts.forEachIndexed { index, promptRes ->
          val promptText = stringResource(promptRes)
          Surface(
            onClick = {
              if (!isBusy) onSendMessage(promptText)
            },
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.height(40.dp).testTag("ai_quick_prompt_$index"),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.padding(horizontal = 14.dp),
            ) {
              Text(
                text = promptText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
              )
            }
          }
        }
      }

      if (!isNetworkAvailable) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.CloudOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = stringResource(R.string.ai_offline_notice),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Chat Messages Area
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (aiState.messages.isEmpty()) {
          item {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = stringResource(R.string.ai_welcome_title),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.ai_welcome_body),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(aiState.messages, key = { it.id }) { msg ->
            AiMessageBubble(message = msg)
          }
        }

        if (aiState.uiState is AiUiState.Error) {
          item {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth().testTag("ai_error_banner"),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
                Text(
                  text = aiState.uiState.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
                TextButton(
                  onClick = {
                    val lastUserPrompt = aiState.messages.lastOrNull { it.isUser }?.text
                    onDismissError()
                    if (!lastUserPrompt.isNullOrBlank()) {
                      onSendMessage(lastUserPrompt)
                    }
                  },
                  modifier = Modifier.testTag("ai_retry_button"),
                ) {
                  Text(text = stringResource(R.string.ai_retry))
                }
              }
            }
          }
        }
      }

      // Disclaimer & Input Bar
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = stringResource(R.string.ai_disclaimer),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = promptInput,
            onValueChange = { promptInput = it },
            placeholder = { Text(stringResource(R.string.ai_input_hint)) },
            maxLines = 3,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions =
              KeyboardActions(
                onSend = {
                  val text = promptInput.trim()
                  if (text.isNotEmpty() && !isBusy) {
                    promptInput = ""
                    onSendMessage(text)
                  }
                },
              ),
            modifier = Modifier.weight(1f).testTag("ai_prompt_input"),
          )
          FilledIconButton(
            onClick = {
              val text = promptInput.trim()
              if (text.isNotEmpty() && !isBusy) {
                promptInput = ""
                onSendMessage(text)
              }
            },
            enabled = promptInput.isNotBlank() && !isBusy,
            colors =
              IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier = Modifier.size(52.dp).testTag("ai_send_button"),
          ) {
            if (isBusy) {
              CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
              )
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = stringResource(R.string.ai_send),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AiMessageBubble(message: AiChatMessage) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
  ) {
    Surface(
      shape =
        RoundedCornerShape(
          topStart = 18.dp,
          topEnd = 18.dp,
          bottomStart = if (message.isUser) 18.dp else 6.dp,
          bottomEnd = if (message.isUser) 6.dp else 18.dp,
        ),
      color =
        if (message.isUser) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainer,
      border =
        if (message.isUser) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.widthIn(max = 320.dp),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (message.text.isBlank() && message.isStreaming) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CircularProgressIndicator(
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = stringResource(R.string.ai_thinking),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color =
              if (message.isUser) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}
