package com.hisaabkitaab.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabkitaab.data.AnalyticsSlice
import com.hisaabkitaab.data.DailyFlowPoint
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import com.hisaabkitaab.ui.theme.parseHexColorSafe
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LedgerDonutChart(
    title: String,
    slices: List<AnalyticsSlice>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    itemSingular: String = "category",
    itemPlural: String = "categories",
    initialVisibleLegendCount: Int = 6
) {
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme
    var selectedIdx by remember(slices) { mutableIntStateOf(-1) }
    var animatePlayed by remember(slices) { mutableStateOf(false) }
    var legendExpanded by remember(slices) { mutableStateOf(false) }

    LaunchedEffect(slices) {
        animatePlayed = true
    }

    val sweepProgress by animateFloatAsState(
        targetValue = if (animatePlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "donutSweep"
    )

    val focusFactor by animateFloatAsState(
        targetValue = if (selectedIdx >= 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "donutFocus"
    )

    val total = remember(slices) { slices.sumOf { it.amount } }
    val activeSlice = slices.getOrNull(selectedIdx)
    val sliceColors = remember(slices, cs.primary) {
        slices.map { parseHexColorSafe(it.colorHex, cs.primary) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface
            )
            if (activeSlice != null) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { selectedIdx = -1 }
                        .testTag("donut_reset_selection")
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (slices.isEmpty() || total <= 0.0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expense data in this period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            }
        } else {
            val innerDiscColor = if (ledger.isDark) {
                cs.surfaceContainerLowest.copy(alpha = 0.72f)
            } else {
                cs.surfaceContainerLowest.copy(alpha = 0.92f)
            }
            val innerRimColor = cs.outlineVariant.copy(alpha = if (ledger.isDark) 0.35f else 0.55f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp)
                    .testTag("ledger_donut_chart"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(220.dp)
                        .pointerInput(slices) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                val dist = hypot(dx, dy)
                                val baseStrokePx = 30.dp.toPx()
                                val minDim = minOf(size.width, size.height).toFloat()
                                val ringRadius = (minDim - baseStrokePx - 14.dp.toPx()) / 2f
                                val innerRadius = ringRadius - baseStrokePx / 2f
                                val outerRadius = ringRadius + baseStrokePx / 2f

                                if (dist < innerRadius - 4.dp.toPx()) {
                                    // Tap inside center hole resets selection
                                    selectedIdx = -1
                                    return@detectTapGestures
                                }

                                if (dist <= outerRadius + 14.dp.toPx()) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                    if (angle < 0f) angle += 360f
                                    var cumulative = 0f
                                    var hitIdx = -1
                                    for (i in slices.indices) {
                                        val sweep = (slices[i].percentage / 100f) * 360f
                                        if (angle >= cumulative && angle <= cumulative + sweep) {
                                            hitIdx = i
                                            break
                                        }
                                        cumulative += sweep
                                    }
                                    if (hitIdx >= 0) {
                                        val next = if (selectedIdx == hitIdx) -1 else hitIdx
                                        selectedIdx = next
                                        if (next >= initialVisibleLegendCount) {
                                            legendExpanded = true
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    val baseStroke = 30.dp.toPx()
                    val activeExtraStroke = 5.dp.toPx() * focusFactor
                    val insetPad = 14.dp.toPx()
                    val diameter = size.minDimension - baseStroke - insetPad
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val innerHoleRadius = (diameter - baseStroke) / 2f

                    // Subtle recessed center disc
                    drawCircle(
                        color = innerDiscColor,
                        radius = innerHoleRadius,
                        center = center
                    )
                    drawCircle(
                        color = innerRimColor,
                        radius = innerHoleRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Subtle base ring track behind sweep
                    drawArc(
                        color = cs.outlineVariant.copy(alpha = 0.22f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = baseStroke)
                    )

                    // First pass: draw all non-selected slices seamlessly
                    var startAngle = -90f
                    var activeStartAngle = -90f
                    var activeSweepAngle = 0f
                    var activeColor = cs.primary

                    slices.forEachIndexed { idx, slice ->
                        val rawSweep = (slice.percentage / 100f) * 360f * sweepProgress
                        val sweep = rawSweep.coerceAtLeast(if (rawSweep > 0f) 0.8f else 0f)
                        val sliceColor = sliceColors[idx]

                        if (selectedIdx == idx) {
                            activeStartAngle = startAngle
                            activeSweepAngle = sweep
                            activeColor = sliceColor
                        } else {
                            val dimmedAlpha = 1f - (0.74f * focusFactor)
                            drawArc(
                                color = sliceColor.copy(alpha = dimmedAlpha),
                                startAngle = startAngle,
                                sweepAngle = sweep + if (slices.size > 1) 0.4f else 0f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = baseStroke, cap = StrokeCap.Butt)
                            )
                        }
                        startAngle += rawSweep
                    }

                    // Second pass: draw selected slice on top with gentle emphasis & soft glow halo
                    if (selectedIdx in slices.indices && activeSweepAngle > 0f) {
                        val activeStroke = baseStroke + activeExtraStroke
                        // Soft outer ambient halo
                        drawArc(
                            color = activeColor.copy(alpha = 0.24f * focusFactor),
                            startAngle = activeStartAngle,
                            sweepAngle = activeSweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = activeStroke + 8.dp.toPx(), cap = StrokeCap.Butt)
                        )
                        // Primary crisp focused arc
                        drawArc(
                            color = activeColor,
                            startAngle = activeStartAngle,
                            sweepAngle = activeSweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = activeStroke, cap = StrokeCap.Butt)
                        )
                    }
                }

                // Center typography strictly constrained inside the inner hole so text never clips into the ring
                AnimatedContent(
                    targetState = activeSlice,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
                    },
                    label = "donutCenterLabel"
                ) { slice ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .width(126.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        if (slice == null) {
                            Text(
                                text = "TOTAL SPENT",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                letterSpacing = 0.9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = StringUtils.formatCompactCurrency(total, currencySymbol),
                                fontFamily = ledger.monoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "${slices.size} ${if (slices.size == 1) itemSingular else itemPlural}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.5.sp,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val pctText = if (slice.percentage > 0f && slice.percentage < 1f) {
                                String.format(Locale.US, "%.1f%% of total spent", slice.percentage)
                            } else {
                                String.format(Locale.US, "%.0f%% of total spent", slice.percentage)
                            }
                            Text(
                                text = slice.label.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                lineHeight = 12.5.sp,
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = StringUtils.formatCompactCurrency(slice.amount, currencySymbol),
                                fontFamily = ledger.monoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = pctText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Collapsible legend chips with inline "+N more" / "Show less" button
            val hasHiddenSlices = slices.size > initialVisibleLegendCount
            val visibleCount = if (!hasHiddenSlices || legendExpanded) {
                slices.size
            } else {
                initialVisibleLegendCount
            }
            val hiddenCount = (slices.size - initialVisibleLegendCount).coerceAtLeast(0)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (idx in 0 until visibleCount) {
                    val slice = slices[idx]
                    val isSelected = selectedIdx == idx
                    val sliceColor = sliceColors[idx]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isSelected) sliceColor.copy(alpha = 0.22f)
                                else cs.surfaceContainerHigh.copy(alpha = 0.7f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) sliceColor else cs.outlineVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable {
                                selectedIdx = if (selectedIdx == idx) -1 else idx
                            }
                            .testTag("donut_legend_item_$idx")
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(sliceColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = slice.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = String.format(Locale.US, "%.0f%%", slice.percentage),
                            fontFamily = ledger.monoFont,
                            fontSize = 10.sp,
                            color = cs.onSurfaceVariant
                        )
                    }
                }

                if (hasHiddenSlices) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(cs.primary.copy(alpha = 0.14f))
                            .border(
                                width = 1.dp,
                                color = cs.primary.copy(alpha = 0.42f),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable { legendExpanded = !legendExpanded }
                            .testTag("donut_legend_more_button")
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (legendExpanded) "Show less" else "+$hiddenCount more",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.primary
                        )
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = if (legendExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (legendExpanded) "Show fewer categories" else "Show more categories",
                            tint = cs.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseLineChart(
    points: List<DailyFlowPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    incomeColor: Color = LocalLedgerColors.current.income,
    expenseColor: Color = LocalLedgerColors.current.expense
) {
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme
    var selectedIdx by remember(points) { mutableIntStateOf(-1) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Income vs Expense Trend",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot("Income", incomeColor)
                LegendDot("Expense", expenseColor)
            }
        }

        val focused = points.getOrNull(selectedIdx)
        if (focused != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceContainerHigh.copy(alpha = 0.75f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringUtils.formatDisplayDate(focused.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "+${StringUtils.formatCompactCurrency(focused.income, currencySymbol)}",
                        fontFamily = ledger.monoFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = incomeColor
                    )
                    Text(
                        text = "-${StringUtils.formatCompactCurrency(focused.expense, currencySymbol)}",
                        fontFamily = ledger.monoFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = expenseColor
                    )
                }
            }
        }

        if (points.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(170.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No activity in selected period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            }
            return
        }

        val maxVal = remember(points) {
            max(1.0, points.maxOf { max(it.income, it.expense) }) * 1.12
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.size <= 1) {
                                selectedIdx = 0
                            } else {
                                val padX = 12.dp.toPx()
                                val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                                val ratio = ((offset.x - padX) / usableW).coerceIn(0f, 1f)
                                selectedIdx = (ratio * (points.size - 1)).toInt().coerceIn(0, points.lastIndex)
                            }
                        }
                    }
            ) {
                val padX = 12.dp.toPx()
                val padTop = 12.dp.toPx()
                val padBottom = 14.dp.toPx()
                val chartW = (size.width - padX * 2).coerceAtLeast(1f)
                val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                // Horizontal gridlines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = padTop + (chartH / gridLines) * i
                    drawLine(
                        color = cs.outlineVariant.copy(alpha = 0.45f),
                        start = Offset(padX, y),
                        end = Offset(size.width - padX, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                fun xAt(index: Int): Float = if (points.size <= 1) {
                    padX + chartW / 2f
                } else {
                    padX + (index.toFloat() / (points.size - 1)) * chartW
                }

                fun yAt(value: Double): Float {
                    val ratio = (value / maxVal).coerceIn(0.0, 1.0).toFloat()
                    return padTop + chartH * (1f - ratio)
                }

                fun drawSeries(values: List<Double>, strokeColor: Color) {
                    val linePath = Path()
                    val fillPath = Path()
                    values.forEachIndexed { idx, v ->
                        val x = xAt(idx)
                        val y = yAt(v)
                        if (idx == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, padTop + chartH)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = xAt(idx - 1)
                            val prevY = yAt(values[idx - 1])
                            val midX = (prevX + x) / 2f
                            linePath.cubicTo(midX, prevY, midX, y, x, y)
                            fillPath.cubicTo(midX, prevY, midX, y, x, y)
                        }
                        if (idx == values.lastIndex) {
                            fillPath.lineTo(x, padTop + chartH)
                            fillPath.close()
                        }
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(strokeColor.copy(alpha = 0.24f), Color.Transparent),
                            startY = padTop,
                            endY = padTop + chartH
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = strokeColor,
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                drawSeries(points.map { it.income }, incomeColor)
                drawSeries(points.map { it.expense }, expenseColor)

                // Draw active touch indicator
                if (selectedIdx in points.indices) {
                    val sx = xAt(selectedIdx)
                    drawLine(
                        color = cs.primary.copy(alpha = 0.65f),
                        start = Offset(sx, padTop),
                        end = Offset(sx, padTop + chartH),
                        strokeWidth = 1.4.dp.toPx()
                    )
                    drawCircle(
                        color = incomeColor,
                        radius = 4.5.dp.toPx(),
                        center = Offset(sx, yAt(points[selectedIdx].income))
                    )
                    drawCircle(
                        color = expenseColor,
                        radius = 4.5.dp.toPx(),
                        center = Offset(sx, yAt(points[selectedIdx].expense))
                    )
                }
            }
        }

        // X-axis first, middle, last date labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(points.first().shortLabel, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            if (points.size > 2) {
                Text(points[points.size / 2].shortLabel, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
            if (points.size > 1) {
                Text(points.last().shortLabel, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
