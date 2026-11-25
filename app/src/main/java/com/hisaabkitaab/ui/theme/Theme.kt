package com.hisaabkitaab.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabkitaab.R
import com.hisaabkitaab.data.AppFontFamily
import com.hisaabkitaab.data.CustomFontManager
import com.hisaabkitaab.data.SurfaceStyle
import com.hisaabkitaab.data.ThemePreferences

object BundledFonts {
    val PlusJakartaSans = FontFamily(
        Font(R.font.plus_jakarta_sans, FontWeight.Normal),
        Font(R.font.plus_jakarta_sans, FontWeight.Medium),
        Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_sans, FontWeight.Bold)
    )
    val Inter = FontFamily(
        Font(R.font.inter, FontWeight.Normal),
        Font(R.font.inter, FontWeight.Medium),
        Font(R.font.inter, FontWeight.SemiBold),
        Font(R.font.inter, FontWeight.Bold)
    )
    val Poppins = FontFamily(
        Font(R.font.poppins, FontWeight.Normal),
        Font(R.font.poppins, FontWeight.Medium),
        Font(R.font.poppins, FontWeight.SemiBold),
        Font(R.font.poppins, FontWeight.Bold)
    )
    val Outfit = FontFamily(
        Font(R.font.outfit, FontWeight.Normal),
        Font(R.font.outfit, FontWeight.Medium),
        Font(R.font.outfit, FontWeight.SemiBold),
        Font(R.font.outfit, FontWeight.Bold)
    )
    val JetBrainsMono = FontFamily(
        Font(R.font.jetbrains_mono, FontWeight.Normal),
        Font(R.font.jetbrains_mono, FontWeight.Medium),
        Font(R.font.jetbrains_mono, FontWeight.SemiBold),
        Font(R.font.jetbrains_mono, FontWeight.Bold)
    )

    fun resolve(appFont: AppFontFamily): FontFamily = when (appFont) {
        AppFontFamily.PLUS_JAKARTA_SANS -> PlusJakartaSans
        AppFontFamily.INTER -> Inter
        AppFontFamily.POPPINS -> Poppins
        AppFontFamily.OUTFIT -> Outfit
    }
}

data class AccentPreset(val name: String, val hex: String)

val AccentPresets = listOf(
    AccentPreset("Amber Gold", "#FCD535"),
    AccentPreset("Hisaab Lime", "#6BDA0A"),
    AccentPreset("Emerald", "#0ECB81"),
    AccentPreset("Ocean Blue", "#3B82F6"),
    AccentPreset("Electric Violet", "#8B5CF6"),
    AccentPreset("Coral Rose", "#EC4899"),
    AccentPreset("Tangerine", "#F97316"),
    AccentPreset("Turquoise", "#2DBDB6")
)

fun parseHexColorSafe(hex: String, fallback: Color = Color(0xFFFCD535)): Color {
    return runCatching {
        val clean = hex.trim().removePrefix("#")
        val parsed = clean.toLong(16)
        when (clean.length) {
            6 -> Color((0xFF000000L or parsed).toInt())
            8 -> Color(parsed.toInt())
            else -> fallback
        }
    }.getOrDefault(fallback)
}

@Immutable
data class LedgerColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val brandLime: Color,
    val glassSurface: Color,
    val glassSurfaceStrong: Color,
    val glassSheen: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val dockSurface: Color,
    val subtleText: Color,
    val hairline: Color,
    val orbBlurRadius: Dp,
    val isDark: Boolean,
    val surfaceStyle: SurfaceStyle,
    val monoFont: FontFamily
) {
    val isLiquidGlass: Boolean
        get() = surfaceStyle != SurfaceStyle.CLASSIC_M3

    val isFullLiquidGlass: Boolean
        get() = surfaceStyle == SurfaceStyle.LIQUID_GLASS
}

val LocalLedgerColors = staticCompositionLocalOf {
    LedgerColors(
        income = Color(0xFF0ECB81),
        incomeContainer = Color(0x260ECB81),
        expense = Color(0xFFF6465D),
        expenseContainer = Color(0x26F6465D),
        transfer = Color(0xFF4D94FF),
        transferContainer = Color(0x264D94FF),
        warning = Color(0xFFF59E0B),
        warningContainer = Color(0x2BF59E0B),
        brandLime = Color(0xFF6BDA0A),
        glassSurface = Color(0x88141C28),
        glassSurfaceStrong = Color(0xBD1D2838),
        glassSheen = Color(0x24FFFFFF),
        glassBorderTop = Color(0x5EFFFFFF),
        glassBorderBottom = Color(0x16FFFFFF),
        dockSurface = Color(0xD4161F2D),
        subtleText = Color(0xFF94A3B8),
        hairline = Color(0xFF263244),
        orbBlurRadius = 68.dp,
        isDark = true,
        surfaceStyle = SurfaceStyle.LIQUID_GLASS,
        monoFont = BundledFonts.JetBrainsMono
    )
}

/**
 * Derives an expressive Material 3 ColorScheme tinted subtly by the user's chosen Accent Color
 * so surfaces, containers, and outlines feel cohesive across Classic M3, Frosted, and Liquid Glass.
 */
private fun buildColorScheme(accent: Color, isDark: Boolean): ColorScheme {
    val onAccent = if (accent.luminance() > 0.42f) Color(0xFF0D1117) else Color(0xFFFFFFFF)
    return if (isDark) {
        val baseBg = lerp(Color(0xFF080C12), accent, 0.025f)
        val surfLowest = lerp(Color(0xFF06090E), accent, 0.02f)
        val surfLow = lerp(Color(0xFF101620), accent, 0.035f)
        val surf = lerp(Color(0xFF151D29), accent, 0.045f)
        val surfHigh = lerp(Color(0xFF1E2837), accent, 0.06f)
        val surfHighest = lerp(Color(0xFF283447), accent, 0.08f)

        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.24f),
            onPrimaryContainer = Color(0xFFF8FAFC),
            secondary = Color(0xFF2DBDB6),
            onSecondary = Color(0xFF071A19),
            secondaryContainer = Color(0xFF133638),
            onSecondaryContainer = Color(0xFFB7F4F0),
            tertiary = Color(0xFF6BDA0A),
            onTertiary = Color(0xFF0C1E02),
            background = baseBg,
            onBackground = Color(0xFFF1F5F9),
            surface = surf,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = surfHigh,
            onSurfaceVariant = Color(0xFF9BA8BA),
            surfaceContainerLowest = surfLowest,
            surfaceContainerLow = surfLow,
            surfaceContainer = surf,
            surfaceContainerHigh = surfHigh,
            surfaceContainerHighest = surfHighest,
            outline = Color(0xFF39485E),
            outlineVariant = Color(0xFF243042),
            error = Color(0xFFF6465D),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0x33F6465D),
            onErrorContainer = Color(0xFFFFB4BD)
        )
    } else {
        val baseBg = lerp(Color(0xFFF2F5FA), accent, 0.035f)
        val surfLow = lerp(Color(0xFFF8FAFD), accent, 0.025f)
        val surf = lerp(Color(0xFFF0F4FA), accent, 0.04f)
        val surfHigh = lerp(Color(0xFFE5ECF5), accent, 0.055f)
        val surfHighest = lerp(Color(0xFFD9E3F0), accent, 0.07f)

        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.20f),
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = Color(0xFF0D9488),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFCCFBF1),
            onSecondaryContainer = Color(0xFF0F3D39),
            tertiary = Color(0xFF4CA805),
            onTertiary = Color(0xFFFFFFFF),
            background = baseBg,
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = surfHigh,
            onSurfaceVariant = Color(0xFF475569),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = surfLow,
            surfaceContainer = surf,
            surfaceContainerHigh = surfHigh,
            surfaceContainerHighest = surfHighest,
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFDDE5EF),
            error = Color(0xFFDC2626),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFF7F1D1D)
        )
    }
}

private fun buildScaledTypography(fontFamily: FontFamily, scale: Float): Typography {
    fun s(spVal: Float) = (spVal * scale).sp
    return Typography(
        displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = s(38f), lineHeight = s(44f), letterSpacing = (-0.8).sp),
        displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = s(30f), lineHeight = s(36f), letterSpacing = (-0.5).sp),
        displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = s(24f), lineHeight = s(30f), letterSpacing = (-0.3).sp),
        headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = s(22f), lineHeight = s(28f), letterSpacing = (-0.2).sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = s(19f), lineHeight = s(25f)),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = s(17f), lineHeight = s(23f)),
        titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = s(16f), lineHeight = s(22f)),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = s(14.5f), lineHeight = s(20f)),
        titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = s(13f), lineHeight = s(18f)),
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = s(14.5f), lineHeight = s(20f)),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = s(13.5f), lineHeight = s(19f)),
        bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = s(12f), lineHeight = s(16f)),
        labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = s(13.5f), lineHeight = s(18f), letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = s(12f), lineHeight = s(16f)),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = s(10.5f), lineHeight = s(14f))
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HisaabKitaabTheme(
    preferences: ThemePreferences,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accent = parseHexColorSafe(preferences.accentHex, Color(0xFFFCD535))
    val colorScheme = buildColorScheme(accent, preferences.isDarkMode)
    val activeCustom = preferences.activeCustomFont
    val fontFamily = remember(preferences.fontFamily, activeCustom?.id, activeCustom?.fileName) {
        activeCustom?.let { CustomFontManager.loadFontFamily(context, it) }
            ?: BundledFonts.resolve(preferences.fontFamily)
    }
    val typography = remember(fontFamily, preferences.fontScale) {
        buildScaledTypography(fontFamily, preferences.fontScale.multiplier)
    }
    val style = preferences.surfaceStyle

    val ledgerColors = if (preferences.isDarkMode) {
        val baseDarkGlass = lerp(Color(0xFF121A26), accent, 0.04f)
        val strongDarkGlass = lerp(Color(0xFF1A2434), accent, 0.06f)
        when (style) {
            SurfaceStyle.CLASSIC_M3 -> LedgerColors(
                income = Color(0xFF0ECB81),
                incomeContainer = Color(0x260ECB81),
                expense = Color(0xFFF6465D),
                expenseContainer = Color(0x26F6465D),
                transfer = Color(0xFF4D94FF),
                transferContainer = Color(0x264D94FF),
                warning = Color(0xFFF59E0B),
                warningContainer = Color(0x26F59E0B),
                brandLime = Color(0xFF6BDA0A),
                glassSurface = colorScheme.surfaceContainerLow,
                glassSurfaceStrong = colorScheme.surfaceContainerHigh,
                glassSheen = Color.Transparent,
                glassBorderTop = colorScheme.outlineVariant,
                glassBorderBottom = colorScheme.outlineVariant,
                dockSurface = colorScheme.surfaceContainerHigh,
                subtleText = Color(0xFF9BA8BA),
                hairline = colorScheme.outlineVariant,
                orbBlurRadius = 0.dp,
                isDark = true,
                surfaceStyle = style,
                monoFont = BundledFonts.JetBrainsMono
            )
            SurfaceStyle.FROSTED -> LedgerColors(
                income = Color(0xFF0ECB81),
                incomeContainer = Color(0x260ECB81),
                expense = Color(0xFFF6465D),
                expenseContainer = Color(0x26F6465D),
                transfer = Color(0xFF4D94FF),
                transferContainer = Color(0x264D94FF),
                warning = Color(0xFFF59E0B),
                warningContainer = Color(0x26F59E0B),
                brandLime = Color(0xFF6BDA0A),
                glassSurface = baseDarkGlass.copy(alpha = 0.78f),
                glassSurfaceStrong = strongDarkGlass.copy(alpha = 0.88f),
                glassSheen = Color.White.copy(alpha = 0.08f),
                glassBorderTop = Color.White.copy(alpha = 0.24f),
                glassBorderBottom = Color.White.copy(alpha = 0.07f),
                dockSurface = strongDarkGlass.copy(alpha = 0.90f),
                subtleText = Color(0xFF9BA8BA),
                hairline = Color(0xFF283548),
                orbBlurRadius = 52.dp,
                isDark = true,
                surfaceStyle = style,
                monoFont = BundledFonts.JetBrainsMono
            )
            SurfaceStyle.LIQUID_GLASS -> LedgerColors(
                income = Color(0xFF0ECB81),
                incomeContainer = Color(0x2B0ECB81),
                expense = Color(0xFFF6465D),
                expenseContainer = Color(0x2BF6465D),
                transfer = Color(0xFF4D94FF),
                transferContainer = Color(0x2B4D94FF),
                warning = Color(0xFFF59E0B),
                warningContainer = Color(0x2BF59E0B),
                brandLime = Color(0xFF6BDA0A),
                glassSurface = baseDarkGlass.copy(alpha = 0.58f),
                glassSurfaceStrong = strongDarkGlass.copy(alpha = 0.74f),
                glassSheen = Color.White.copy(alpha = 0.16f),
                glassBorderTop = Color.White.copy(alpha = 0.42f),
                glassBorderBottom = Color.White.copy(alpha = 0.10f),
                dockSurface = strongDarkGlass.copy(alpha = 0.80f),
                subtleText = Color(0xFFA6B4C8),
                hairline = Color(0xFF2D3C52),
                orbBlurRadius = 72.dp,
                isDark = true,
                surfaceStyle = style,
                monoFont = BundledFonts.JetBrainsMono
            )
        }
    } else {
        when (style) {
            SurfaceStyle.CLASSIC_M3 -> LedgerColors(
                income = Color(0xFF059669),
                incomeContainer = Color(0x1F059669),
                expense = Color(0xFFDC2626),
                expenseContainer = Color(0x1FDC2626),
                transfer = Color(0xFF2563EB),
                transferContainer = Color(0x1F2563EB),
                warning = Color(0xFFB45309),
                warningContainer = Color(0x1FD97706),
                brandLime = Color(0xFF56B806),
                glassSurface = colorScheme.surfaceContainerLowest,
                glassSurfaceStrong = colorScheme.surfaceContainerHigh,
                glassSheen = Color.Transparent,
                glassBorderTop = colorScheme.outlineVariant,
                glassBorderBottom = colorScheme.outlineVariant,
                dockSurface = colorScheme.surfaceContainerLowest,
                subtleText = Color(0xFF64748B),
                hairline = colorScheme.outlineVariant,
                orbBlurRadius = 0.dp,
                isDark = false,
                surfaceStyle = style,
                monoFont = BundledFonts.JetBrainsMono
            )
            SurfaceStyle.FROSTED -> LedgerColors(
                income = Color(0xFF059669),
                incomeContainer = Color(0x1F059669),
                expense = Color(0xFFDC2626),
                expenseContainer = Color(0x1FDC2626),
                transfer = Color(0xFF2563EB),
                transferContainer = Color(0x1F2563EB),
                warning = Color(0xFFB45309),
                warningContainer = Color(0x1FD97706),
                brandLime = Color(0xFF56B806),
                glassSurface = Color.White.copy(alpha = 0.78f),
                glassSurfaceStrong = Color.White.copy(alpha = 0.90f),
                glassSheen = Color.White.copy(alpha = 0.38f),
                glassBorderTop = Color.White.copy(alpha = 0.92f),
                glassBorderBottom = Color(0x3364748B),
                dockSurface = Color.White.copy(alpha = 0.90f),
                subtleText = Color(0xFF526277),
                hairline = Color(0xFFDDE5EF),
                orbBlurRadius = 52.dp,
                isDark = false,
                surfaceStyle = style,
                monoFont = BundledFonts.JetBrainsMono
            )
            SurfaceStyle.LIQUID_GLASS -> LedgerColors(
                income = Color(0xFF059669),
                incomeContainer = Color(0x22059669),
                expense = Color(0xFFDC2626),
                expenseContainer = Color(0x22DC2626),
                transfer = Color(0xFF2563EB),
                transferContainer = Color(0x222563EB),
                warning = Color(0xFFB45309),
                warningContainer = Color(0x22D97706),
                brandLime = Color(0xFF56B806),
                glassSurface = Color.White.copy(alpha = 0.62f),
                glassSurfaceStrong = Color.White.copy(alpha = 0.78f),
                glassSheen = Color.White.copy(alpha = 0.55f),
                glassBorderTop = Color.White.copy(alpha = 0.98f),
                glassBorderBottom = Color(0x4264748B),
                dockSurface = Color.White.copy(alpha = 0.82f),
                subtleText = Color(0xFF475569),
                hairline = Color(0xFFD4DFEC),
                orbBlurRadius = 72.dp,
                isDark = false,
                surfaceStyle = style,
                monoFont = BundledFonts.JetBrainsMono
            )
        }
    }

    val selectionColors = remember(colorScheme.primary, preferences.isDarkMode) {
        TextSelectionColors(
            handleColor = colorScheme.primary,
            backgroundColor = colorScheme.primary.copy(alpha = if (preferences.isDarkMode) 0.38f else 0.30f)
        )
    }

    CompositionLocalProvider(LocalLedgerColors provides ledgerColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = AppShapes
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colorScheme.onBackground,
                LocalTextSelectionColors provides selectionColors,
                content = content
            )
        }
    }
}
