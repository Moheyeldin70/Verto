package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.FormattingUtils
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.theme.NumericMediumStyle
import com.example.ui.theme.NumericSmallStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class ChartSeriesPoint(
  val label: String,
  val value: Double,
)

@Composable
fun FinancialTrendChart(
  points: List<ChartSeriesPoint>,
  accentColor: Color,
  valueSuffix: String,
  isLoading: Boolean,
  modifier: Modifier = Modifier,
) {
  val financial = LocalFinancialColors.current
  val gridColor = financial.chartGrid
  val surfaceColor = MaterialTheme.colorScheme.surface
  var scrubbedIndex by remember(points) { mutableStateOf<Int?>(null) }

  Column(modifier = modifier.fillMaxWidth()) {
    if (points.size >= 2) {
      val first = points.first().value
      val last = points.last().value
      val minVal = points.minOf { it.value }
      val maxVal = points.maxOf { it.value }
      val pctChange = if (abs(first) > 1e-12) ((last - first) / first) * 100.0 else 0.0
      val isPositive = pctChange >= 0.0
      val activeIdx = scrubbedIndex?.coerceIn(0, points.lastIndex)
      val inspectedPoint = activeIdx?.let { points[it] }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text =
              if (inspectedPoint != null) {
                "${FormattingUtils.formatRate(inspectedPoint.value)} $valueSuffix"
              } else {
                "${FormattingUtils.formatRate(last)} $valueSuffix"
              },
            style = NumericMediumStyle,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = inspectedPoint?.label ?: "${points.first().label} – ${points.last().label}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color =
            (if (isPositive) financial.gain else financial.loss).copy(alpha = 0.14f),
        ) {
          val sign = if (isPositive) "+" else ""
          Text(
            text = "$sign${String.format(Locale.US, "%.2f", pctChange)}%",
            style = NumericSmallStyle.copy(fontWeight = FontWeight.Bold),
            color = if (isPositive) financial.gain else financial.loss,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      if (isLoading) {
        LinearProgressIndicator(
          color = accentColor,
          trackColor = accentColor.copy(alpha = 0.15f),
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(2.dp).clip(RoundedCornerShape(1.dp)),
        )
        Spacer(Modifier.height(6.dp))
      } else {
        Spacer(Modifier.height(12.dp))
      }

      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(176.dp)
            .pointerInput(points) {
              detectTapGestures { offset ->
                val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                scrubbedIndex = (ratio * (points.size - 1)).roundToInt()
              }
            }
            .pointerInput(points) {
              detectDragGestures(
                onDragEnd = { scrubbedIndex = null },
                onDragCancel = { scrubbedIndex = null },
                onDrag = { change, _ ->
                  change.consume()
                  val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                  scrubbedIndex = (ratio * (points.size - 1)).roundToInt()
                },
              )
            }
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val topPad = 14.dp.toPx()
          val bottomPad = 14.dp.toPx()
          val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
          val chartW = size.width.coerceAtLeast(1f)
          val span = (maxVal - minVal).let { if (it < 1e-9) maxVal * 0.02 + 1e-6 else it }

          // Dashed horizontal grid lines
          val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
          for (i in 0..3) {
            val y = topPad + (chartH / 3f) * i
            drawLine(
              color = gridColor,
              start = Offset(0f, y),
              end = Offset(chartW, y),
              strokeWidth = 1.dp.toPx(),
              pathEffect = dash,
            )
          }

          val coords =
            points.mapIndexed { idx, pt ->
              val x = (idx.toFloat() / (points.size - 1)) * chartW
              val normY = ((pt.value - minVal) / span).toFloat().coerceIn(0f, 1f)
              val y = topPad + chartH * (1f - normY)
              Offset(x, y)
            }

          val linePath = Path()
          val fillPath = Path()
          coords.forEachIndexed { idx, pt ->
            if (idx == 0) {
              linePath.moveTo(pt.x, pt.y)
              fillPath.moveTo(pt.x, size.height)
              fillPath.lineTo(pt.x, pt.y)
            } else {
              val prev = coords[idx - 1]
              val ctrlX = (prev.x + pt.x) / 2f
              linePath.cubicTo(ctrlX, prev.y, ctrlX, pt.y, pt.x, pt.y)
              fillPath.cubicTo(ctrlX, prev.y, ctrlX, pt.y, pt.x, pt.y)
            }
          }
          fillPath.lineTo(coords.last().x, size.height)
          fillPath.close()

          drawPath(
            path = fillPath,
            brush =
              Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.28f), accentColor.copy(alpha = 0.0f)),
                startY = topPad,
                endY = size.height,
              ),
          )

          drawPath(
            path = linePath,
            color = accentColor,
            style =
              Stroke(
                width = 2.6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
              ),
          )

          // Highlight inspected or end dot
          val targetIdx = activeIdx ?: coords.lastIndex
          val targetPt = coords[targetIdx]
          if (activeIdx != null) {
            drawLine(
              color = accentColor.copy(alpha = 0.5f),
              start = Offset(targetPt.x, 0f),
              end = Offset(targetPt.x, size.height),
              strokeWidth = 1.2.dp.toPx(),
              pathEffect = dash,
            )
          }
          drawCircle(
            color = surfaceColor,
            radius = 6.dp.toPx(),
            center = targetPt,
          )
          drawCircle(
            color = accentColor,
            radius = 4.dp.toPx(),
            center = targetPt,
          )
        }
      }

      Spacer(Modifier.height(10.dp))

      // High / Low footer stats
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "${stringResource(R.string.period_low)}: ${FormattingUtils.formatRate(minVal)}",
          style = NumericSmallStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "${stringResource(R.string.period_high)}: ${FormattingUtils.formatRate(maxVal)}",
          style = NumericSmallStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else if (isLoading) {
      Box(
        modifier = Modifier.fillMaxWidth().height(190.dp),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(28.dp),
          color = accentColor,
          strokeWidth = 2.5.dp,
        )
      }
    } else {
      Box(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Rounded.ShowChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(32.dp),
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.offline_banner),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
