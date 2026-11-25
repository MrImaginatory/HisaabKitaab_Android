package com.hisaabkitaab.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.SurfaceStyle

/**
 * Full-bleed atmospheric backdrop adapting across all 3 Surface Styles:
 * - CLASSIC_M3: Crisp Material 3 canvas with subtle top accent wash.
 * - FROSTED: Softly blurred ambient orbs for gentle frosted-glass translucency.
 * - LIQUID_GLASS: Deep refractive atmospheric mesh with hardware-accelerated blur (API 31+)
 *   and luminous accent-reactive orbs.
 */
@Composable
fun HisaabBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val ledger = LocalLedgerColors.current
    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        when (ledger.surfaceStyle) {
            SurfaceStyle.CLASSIC_M3 -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primary.copy(alpha = if (ledger.isDark) 0.055f else 0.045f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.35f
                        )
                    )
                }
            }

            SurfaceStyle.FROSTED, SurfaceStyle.LIQUID_GLASS -> {
                val isLiquid = ledger.isFullLiquidGlass
                val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ledger.orbBlurRadius > 0.dp) {
                    Modifier.blur(ledger.orbBlurRadius)
                } else {
                    Modifier
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(blurModifier)
                ) {
                    val primaryAlpha = when {
                        isLiquid && ledger.isDark -> 0.28f
                        isLiquid && !ledger.isDark -> 0.24f
                        ledger.isDark -> 0.16f
                        else -> 0.14f
                    }
                    val greenAlpha = if (isLiquid) (if (ledger.isDark) 0.22f else 0.18f) else 0.11f
                    val blueAlpha = if (isLiquid) (if (ledger.isDark) 0.24f else 0.18f) else 0.11f

                    // Primary accent orb (top-right)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = primaryAlpha), Color.Transparent),
                            center = Offset(size.width * 0.86f, size.height * 0.08f),
                            radius = size.minDimension * 0.78f
                        ),
                        center = Offset(size.width * 0.86f, size.height * 0.08f),
                        radius = size.minDimension * 0.78f
                    )
                    // Income emerald orb (mid-left)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ledger.income.copy(alpha = greenAlpha), Color.Transparent),
                            center = Offset(size.width * 0.08f, size.height * 0.42f),
                            radius = size.minDimension * 0.74f
                        ),
                        center = Offset(size.width * 0.08f, size.height * 0.42f),
                        radius = size.minDimension * 0.74f
                    )
                    // Transfer cobalt orb (bottom-right)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ledger.transfer.copy(alpha = blueAlpha), Color.Transparent),
                            center = Offset(size.width * 0.84f, size.height * 0.86f),
                            radius = size.minDimension * 0.80f
                        ),
                        center = Offset(size.width * 0.84f, size.height * 0.86f),
                        radius = size.minDimension * 0.80f
                    )
                    // Extra warm accent counter-glow in Liquid Glass mode
                    if (isLiquid) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primary.copy(alpha = primaryAlpha * 0.65f), Color.Transparent),
                                center = Offset(size.width * 0.18f, size.height * 0.88f),
                                radius = size.minDimension * 0.58f
                            ),
                            center = Offset(size.width * 0.18f, size.height * 0.88f),
                            radius = size.minDimension * 0.58f
                        )
                    }
                }
            }
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            content()
        }
    }
}

/**
 * Expressive adaptive surface card supporting Classic M3, Frosted, and Refractive Liquid Glass,
 * with directional specular light rims, subtle prismatic sheen, and spring + ripple tap feedback.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevated: Boolean = false,
    accentTint: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && pressed) 0.982f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardSpringScale"
    )

    val bgBrush = when (ledger.surfaceStyle) {
        SurfaceStyle.CLASSIC_M3 -> {
            val base = if (elevated) cs.surfaceContainerHigh else cs.surfaceContainerLow
            val tinted = if (accentTint != null) {
                lerp(base, accentTint, if (ledger.isDark) 0.08f else 0.06f)
            } else {
                base
            }
            Brush.verticalGradient(listOf(tinted, base))
        }

        SurfaceStyle.FROSTED -> {
            val base = if (elevated) ledger.glassSurfaceStrong else ledger.glassSurface
            val topTint = if (accentTint != null) {
                lerp(base, accentTint, if (ledger.isDark) 0.12f else 0.08f)
            } else {
                lerp(base, Color.White, if (ledger.isDark) 0.05f else 0.25f)
            }
            Brush.verticalGradient(listOf(topTint, base))
        }

        SurfaceStyle.LIQUID_GLASS -> {
            val base = if (elevated) ledger.glassSurfaceStrong else ledger.glassSurface
            val topColor = if (accentTint != null) {
                lerp(base, accentTint, if (ledger.isDark) 0.20f else 0.13f)
            } else {
                lerp(base, Color.White, if (ledger.isDark) 0.11f else 0.36f)
            }
            val midColor = base
            val bottomColor = if (ledger.isDark) {
                lerp(base, Color.Black, 0.12f)
            } else {
                lerp(base, accentTint ?: cs.primary, 0.04f)
            }
            Brush.linearGradient(
                colors = listOf(topColor, midColor, bottomColor)
            )
        }
    }

    val borderWidth = if (ledger.isFullLiquidGlass) 1.15.dp else 1.dp
    val borderBrush = when (ledger.surfaceStyle) {
        SurfaceStyle.CLASSIC_M3 -> {
            val c = accentTint?.copy(alpha = 0.36f) ?: cs.outlineVariant
            Brush.verticalGradient(listOf(c, c))
        }
        SurfaceStyle.FROSTED -> {
            Brush.verticalGradient(
                listOf(
                    accentTint?.copy(alpha = 0.42f) ?: ledger.glassBorderTop,
                    ledger.glassBorderBottom
                )
            )
        }
        SurfaceStyle.LIQUID_GLASS -> {
            Brush.linearGradient(
                colors = listOf(
                    if (accentTint != null) lerp(ledger.glassBorderTop, accentTint, 0.45f) else ledger.glassBorderTop,
                    accentTint?.copy(alpha = 0.24f) ?: Color.White.copy(alpha = if (ledger.isDark) 0.14f else 0.55f),
                    ledger.glassBorderBottom
                )
            )
        }
    }

    val sheenModifier = if (ledger.isFullLiquidGlass) {
        Modifier.drawBehind {
            // Subtle refractive specular highlight band across top-left glass curvature
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        ledger.glassSheen,
                        Color.Transparent,
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.65f, size.height * 0.55f)
                )
            )
        }
    } else {
        Modifier
    }

    val elevationShadow = if (elevated && !ledger.isDark) {
        Modifier.shadow(
            elevation = if (ledger.isFullLiquidGlass) 10.dp else 4.dp,
            shape = shape,
            ambientColor = (accentTint ?: cs.primary).copy(alpha = 0.14f),
            spotColor = Color.Black.copy(alpha = 0.10f)
        )
    } else {
        Modifier
    }

    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .scale(scale)
            .then(elevationShadow)
            .clip(shape)
            .background(bgBrush, shape)
            .then(sheenModifier)
            .border(width = borderWidth, brush = borderBrush, shape = shape)
            .then(clickableMod)
            .padding(contentPadding)
    ) {
        CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
            content()
        }
    }
}

/**
 * Smoothly animated monetary counter displaying tabular JetBrains Mono currency values.
 */
@Composable
fun AnimatedCurrencyText(
    amount: Double,
    symbol: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showPositiveSign: Boolean = false,
    prefix: String = "",
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 26.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    maxLines: Int = 1
) {
    val ledger = LocalLedgerColors.current
    val animatedVal by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "currencyCounter"
    )
    val display = if (compact) {
        prefix + StringUtils.formatCompactCurrency(animatedVal.toDouble(), symbol)
    } else {
        prefix + StringUtils.formatCurrency(animatedVal.toDouble(), symbol, showPositiveSign = showPositiveSign)
    }
    Text(
        text = display,
        modifier = modifier,
        fontFamily = ledger.monoFont,
        fontWeight = fontWeight,
        fontSize = fontSize,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Expressive empty state card with circular icon medallion, helpful copy, and optional action button.
 */
@Composable
fun ExpressiveEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(cs.primary.copy(alpha = 0.16f))
                    .border(1.dp, cs.primary.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(28.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                GlassPrimaryButton(
                    text = actionLabel,
                    icon = Icons.Rounded.Add,
                    onClick = onAction
                )
            }
        }
    }
}

/**
 * Modern M3 Expressive primary pill CTA with ripple feedback, spring press physics,
 * and specular luminous glass highlight.
 */
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )
    val baseColor = if (enabled) cs.primary else cs.surfaceContainerHighest
    val contentColor = if (enabled) cs.onPrimary else cs.onSurfaceVariant
    val topGlow = if (enabled) lerp(baseColor, Color.White, if (ledger.isFullLiquidGlass) 0.22f else 0.08f) else baseColor

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topGlow, baseColor)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (enabled) 0.48f else 0.08f),
                        Color.White.copy(alpha = if (enabled) 0.12f else 0.04f)
                    )
                ),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dedicated expressive Quick-Add (+) FAB engineered to pair cleanly beside the Floating Glass Pill Dock.
 */
@Composable
fun QuickAddGlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Quick Add Transaction",
    testTag: String = "quick_add_fab"
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "quickAddScale"
    )
    val shape = RoundedCornerShape(20.dp)
    val topGlow = lerp(cs.primary, Color.White, if (ledger.isFullLiquidGlass) 0.25f else 0.10f)

    Box(
        modifier = modifier
            .scale(scale)
            .size(56.dp)
            .shadow(
                elevation = if (ledger.isLiquidGlass) 14.dp else 6.dp,
                shape = shape,
                ambientColor = cs.primary.copy(alpha = 0.45f),
                spotColor = cs.primary.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(topGlow, cs.primary)
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.14f))
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = contentDescription,
            tint = cs.onPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun GlassTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerTint: Color? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tonalScale"
    )
    val bg = containerTint ?: when {
        ledger.isLiquidGlass && ledger.isDark -> ledger.glassSurfaceStrong
        ledger.isLiquidGlass -> cs.surfaceContainerHigh.copy(alpha = 0.82f)
        else -> cs.surfaceContainerHigh
    }
    val borderColor = when {
        containerTint != null -> tint.copy(alpha = 0.30f)
        ledger.isLiquidGlass && ledger.isDark -> ledger.glassBorderTop
        else -> cs.outlineVariant
    }

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) tint else cs.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) tint else cs.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlassDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val ledger = LocalLedgerColors.current
    GlassTonalButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        tint = ledger.expense,
        containerTint = ledger.expenseContainer,
        enabled = enabled,
        testTag = testTag
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color? = null,
    testTag: String? = null
) {
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconBtnScale"
    )
    val bg = containerColor ?: when {
        ledger.isLiquidGlass && ledger.isDark -> ledger.glassSurfaceStrong
        ledger.isLiquidGlass -> cs.surfaceContainerHigh.copy(alpha = 0.82f)
        else -> cs.surfaceContainerHigh
    }
    val borderBrush = if (ledger.isLiquidGlass && ledger.isDark) {
        Brush.verticalGradient(listOf(ledger.glassBorderTop, ledger.glassBorderBottom))
    } else {
        Brush.verticalGradient(listOf(cs.outlineVariant, cs.outlineVariant))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(48.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderBrush, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
fun HisaabBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    transparent: Boolean = false
) {
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(size * 0.24f)
    val resId = when {
        transparent -> com.hisaabkitaab.R.drawable.ic_hisaab_logo_transparent
        ledger.isDark -> com.hisaabkitaab.R.drawable.ic_hisaab_logo_dark
        else -> com.hisaabkitaab.R.drawable.ic_hisaab_logo_light
    }
    val borderMod = if (!transparent && !ledger.isDark) {
        Modifier.border(1.dp, cs.outlineVariant.copy(alpha = 0.85f), shape)
    } else if (!transparent) {
        Modifier.border(1.dp, Color.White.copy(alpha = 0.14f), shape)
    } else {
        Modifier
    }

    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = resId),
        contentDescription = androidx.compose.ui.res.stringResource(id = com.hisaabkitaab.R.string.cd_app_logo),
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(borderMod)
    )
}

@Composable
fun <T> SegmentedPillBar(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String,
    modifier: Modifier = Modifier,
    colorOf: ((T) -> Color)? = null,
    testTagPrefix: String = "segment"
) {
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme
    val trackBg = if (ledger.isLiquidGlass) ledger.glassSurface else cs.surfaceContainerHigh
    val borderBrush = if (ledger.isLiquidGlass) {
        Brush.verticalGradient(listOf(ledger.glassBorderTop, ledger.glassBorderBottom))
    } else {
        Brush.verticalGradient(listOf(cs.outlineVariant, cs.outlineVariant))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(trackBg)
            .border(1.dp, borderBrush, RoundedCornerShape(999.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { opt ->
            val isSelected = opt == selected
            val activeColor = colorOf?.invoke(opt) ?: cs.primary
            val bg by animateColorAsState(
                targetValue = if (isSelected) activeColor else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "segBg"
            )
            val txtColor = if (isSelected) {
                if (activeColor.luminance() > 0.42f) Color(0xFF0D1117) else Color.White
            } else {
                cs.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .clickable { onSelect(opt) }
                    .testTag("${testTagPrefix}_${labelOf(opt).lowercase().replace(" ", "_")}")
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelOf(opt),
                    style = MaterialTheme.typography.labelLarge,
                    color = txtColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GlassSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    testTag: String = "search_input"
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    GlassTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = cs.onSurfaceVariant)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = cs.onSurfaceVariant)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(999.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = cs.onSurface,
            unfocusedTextColor = cs.onSurface,
            focusedPlaceholderColor = cs.onSurfaceVariant,
            unfocusedPlaceholderColor = cs.onSurfaceVariant,
            cursorColor = cs.primary,
            focusedContainerColor = if (ledger.isLiquidGlass) ledger.glassSurfaceStrong else cs.surfaceContainerLow,
            unfocusedContainerColor = if (ledger.isLiquidGlass) ledger.glassSurface else cs.surfaceContainerLow,
            focusedBorderColor = cs.primary,
            unfocusedBorderColor = if (ledger.isLiquidGlass) ledger.glassBorderTop else cs.outlineVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GlassDropdownField(
    label: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select...",
    itemColorHex: ((T) -> String?)? = null,
    enabled: Boolean = true,
    testTag: String = "dropdown_field"
) {
    var expanded by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedItem?.let(itemLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = selectedItem?.let { sel ->
                itemColorHex?.invoke(sel)?.let { hex ->
                    {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(parseHexColorSafe(hex, cs.primary))
                        )
                    }
                }
            },
            trailingIcon = {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = cs.onSurface,
                unfocusedTextColor = cs.onSurface,
                disabledTextColor = cs.onSurfaceVariant,
                focusedLabelColor = cs.primary,
                unfocusedLabelColor = cs.onSurfaceVariant,
                focusedPlaceholderColor = cs.onSurfaceVariant,
                unfocusedPlaceholderColor = cs.onSurfaceVariant,
                focusedContainerColor = if (ledger.isLiquidGlass) ledger.glassSurfaceStrong else cs.surfaceContainerLow,
                unfocusedContainerColor = if (ledger.isLiquidGlass) ledger.glassSurface else cs.surfaceContainerLow,
                focusedBorderColor = cs.primary,
                unfocusedBorderColor = if (ledger.isLiquidGlass) ledger.glassBorderTop else cs.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                .testTag(testTag)
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            if (items.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No options available", color = cs.onSurfaceVariant) },
                    onClick = { expanded = false }
                )
            } else {
                items.forEach { item ->
                    val dotHex = itemColorHex?.invoke(item)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (dotHex != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColorSafe(dotHex, cs.primary))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(itemLabel(item))
                            }
                        },
                        onClick = {
                            onSelect(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    isoDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "date_picker_field"
) {
    var showDialog by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current

    OutlinedTextField(
        value = StringUtils.formatDisplayDate(isoDate),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = "Pick date", tint = cs.primary)
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = cs.onSurface,
            unfocusedTextColor = cs.onSurface,
            focusedLabelColor = cs.primary,
            unfocusedLabelColor = cs.onSurfaceVariant,
            focusedContainerColor = if (ledger.isLiquidGlass) ledger.glassSurfaceStrong else cs.surfaceContainerLow,
            unfocusedContainerColor = if (ledger.isLiquidGlass) ledger.glassSurface else cs.surfaceContainerLow,
            focusedBorderColor = cs.primary,
            unfocusedBorderColor = if (ledger.isLiquidGlass) ledger.glassBorderTop else cs.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .testTag(testTag)
    )

    if (showDialog) {
        val initialMillis = remember(isoDate) {
            StringUtils.isoToEpochMillis(isoDate) ?: System.currentTimeMillis()
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onDateSelected(StringUtils.epochMillisToIso(millis))
                        }
                        showDialog = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun ColorSwatchSelector(
    selectedHex: String,
    onHexSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val swatches = remember {
        listOf(
            "#FCD535", "#6BDA0A", "#0ECB81", "#2DBDB6",
            "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899",
            "#F6465D", "#F97316", "#EAB308", "#14B8A6"
        )
    }
    var hexInput by remember(selectedHex) { mutableStateOf(selectedHex.uppercase()) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(swatches) { hex ->
                val color = parseHexColorSafe(hex)
                val isSelected = selectedHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.White.copy(alpha = 0.30f),
                            shape = CircleShape
                        )
                        .clickable {
                            hexInput = hex
                            onHexSelected(hex)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Selected color",
                            tint = if (color.luminance() > 0.45f) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        val hexLimit = remember { MaxLengthTransformation(7) }
        GlassTextField(
            value = hexInput,
            onValueChange = { raw ->
                hexInput = raw
                if (StringUtils.isValidHexColor(raw)) {
                    onHexSelected(StringUtils.normalizeHexColor(raw))
                }
            },
            label = { Text("Custom Hex Color") },
            singleLine = true,
            inputTransformation = hexLimit,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(parseHexColorSafe(selectedHex))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = glassTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Shared high-contrast OutlinedTextField colors matching the Record Transaction form across
 * Classic M3, Frosted, and Liquid Glass surfaces in both Dark and Light modes.
 */
@Composable
fun glassTextFieldColors(): TextFieldColors {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = cs.onSurface,
        unfocusedTextColor = cs.onSurface,
        disabledTextColor = cs.onSurfaceVariant,
        focusedLabelColor = cs.primary,
        unfocusedLabelColor = cs.onSurfaceVariant,
        focusedPlaceholderColor = cs.onSurfaceVariant,
        unfocusedPlaceholderColor = cs.onSurfaceVariant,
        cursorColor = cs.primary,
        focusedContainerColor = if (ledger.isLiquidGlass) ledger.glassSurfaceStrong else cs.surfaceContainer,
        unfocusedContainerColor = if (ledger.isLiquidGlass) ledger.glassSurface else cs.surfaceContainer,
        focusedBorderColor = cs.primary,
        unfocusedBorderColor = if (ledger.isLiquidGlass) ledger.glassBorderTop else cs.outlineVariant
    )
}

/**
 * Unified expressive ModalBottomSheet form shell matching the Record Transaction form.
 * Gives every create/edit/import/confirm workflow generous horizontal space, crisp dark/light
 * contrast, keyboard-safe scrolling, and an icon-badged header with dismiss control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassFormBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = cs.surfaceContainerLow,
        contentColor = cs.onSurface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.16f))
                            .border(1.dp, iconTint.copy(alpha = 0.32f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = cs.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.55f))

            content()
        }
    }
}

@Composable
fun CaptchaDeleteDialog(
    title: String,
    description: String,
    confirmButtonLabel: String = "Delete Permanently",
    requireSettlementAccount: Boolean = false,
    activeAccounts: List<Account> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (settlementAccountId: String?) -> Unit
) {
    var confirmWord by remember { mutableStateOf("") }
    var captchaCode by remember { mutableStateOf(StringUtils.generateCaptcha()) }
    var captchaInput by remember { mutableStateOf("") }
    var selectedSettlement by remember { mutableStateOf(activeAccounts.firstOrNull()) }
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme

    val isMatch = confirmWord.trim().equals("confirm", ignoreCase = true) &&
        captchaInput.trim().equals(captchaCode, ignoreCase = false) &&
        (!requireSettlementAccount || selectedSettlement != null)

    GlassFormBottomSheet(
        title = title,
        subtitle = "Protected action — verification required",
        icon = Icons.Rounded.WarningAmber,
        iconTint = ledger.expense,
        onDismiss = onDismiss
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )

        if (requireSettlementAccount) {
            GlassDropdownField(
                label = "Settlement Account (Required)",
                items = activeAccounts,
                selectedItem = selectedSettlement,
                itemLabel = { StringUtils.displayName(it.name) },
                onSelect = { selectedSettlement = it },
                placeholder = "Choose active account"
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cs.surfaceContainerHigh,
            border = BorderStroke(1.dp, cs.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = captchaCode,
                    fontFamily = ledger.monoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 6.sp,
                    color = cs.primary
                )
                IconButton(onClick = { captchaCode = StringUtils.generateCaptcha() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh CAPTCHA", tint = cs.onSurfaceVariant)
                }
            }
        }

        GlassTextField(
            value = confirmWord,
            onValueChange = { confirmWord = it },
            label = { Text("Type \"confirm\"") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = glassTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("captcha_confirm_word_input")
        )

        val captchaTransform = remember { UppercaseMaxLengthTransformation(6) }
        GlassTextField(
            value = captchaInput,
            onValueChange = { captchaInput = it },
            label = { Text("Enter 6-char CAPTCHA") },
            singleLine = true,
            inputTransformation = captchaTransform,
            shape = RoundedCornerShape(16.dp),
            colors = glassTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("captcha_code_input")
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassTonalButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            GlassDangerButton(
                text = confirmButtonLabel,
                enabled = isMatch,
                onClick = { onConfirm(selectedSettlement?.id) },
                modifier = Modifier.weight(1.4f),
                testTag = "captcha_delete_confirm_button"
            )
        }
    }
}

@Composable
fun SimpleConfirmDeleteDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme
    val ready = input.trim().equals("confirm", ignoreCase = true)

    GlassFormBottomSheet(
        title = title,
        subtitle = "Type confirm below to proceed",
        icon = Icons.Rounded.WarningAmber,
        iconTint = ledger.expense,
        onDismiss = onDismiss
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )
        GlassTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Type \"confirm\" to continue") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = glassTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("simple_confirm_input")
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassTonalButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            GlassDangerButton(
                text = "Delete",
                enabled = ready,
                onClick = onConfirm,
                modifier = Modifier.weight(1.3f),
                testTag = "simple_confirm_delete_button"
            )
        }
    }
}
