package com.hisaabkitaab.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hisaabkitaab.data.ThemePreferences
import com.hisaabkitaab.ui.theme.GlassCard
import com.hisaabkitaab.ui.theme.GlassIconButton
import com.hisaabkitaab.ui.theme.GlassPrimaryButton
import com.hisaabkitaab.ui.theme.GlassTonalButton
import com.hisaabkitaab.ui.theme.HisaabBackdrop
import com.hisaabkitaab.ui.theme.HisaabBrandLogo
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, animationSpec = tween(400))
        delay(650)
        onFinished()
    }

    HisaabBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HisaabBrandLogo(
                size = 86.dp,
                modifier = Modifier.scale(scale.value)
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "HisaabKitaab",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(alpha.value)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Offline Personal Finance & SQLite Ledger",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}

@Composable
fun DatabaseOnboardingScreen(
    hasSavedLocalDb: Boolean,
    themePrefs: ThemePreferences,
    isBusy: Boolean,
    onReconnectLast: () -> Unit,
    onOpenDbFile: () -> Unit,
    onCreateBlankDb: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onCycleSurfaceStyle: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current

    HisaabBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar with quick visual toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HisaabBrandLogo(size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HisaabKitaab",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = cs.onBackground
                            )
                            Text(
                                text = themePrefs.surfaceStyle.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.primary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIconButton(
                            icon = Icons.Rounded.AutoAwesome,
                            contentDescription = "Cycle Surface Style (${themePrefs.surfaceStyle.label})",
                            tint = if (themePrefs.isLiquidGlass) cs.primary else cs.onSurfaceVariant,
                            onClick = onCycleSurfaceStyle,
                            testTag = "onboarding_cycle_style_button"
                        )
                        GlassIconButton(
                            icon = if (themePrefs.isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Toggle Dark or Light Mode",
                            onClick = onToggleDarkMode,
                            testTag = "onboarding_toggle_theme_button"
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Choose your Hisaab database",
                        style = MaterialTheme.typography.headlineLarge,
                        color = cs.onBackground
                    )
                    Text(
                        text = "Your financial records stay 100% private on this device in a standard SQLite .db file — seamlessly compatible with HisaabKitaab web backups.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                }

                if (isBusy) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Text("Preparing SQLite database…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (hasSavedLocalDb) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevated = true,
                        accentTint = cs.primary,
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(cs.primary.copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null, tint = cs.primary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Resume Previous Ledger",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cs.onSurface
                                )
                                Text(
                                    text = if (themePrefs.linkedDbUri.isNotBlank()) {
                                        "Live-linked • ${themePrefs.lastDbName}"
                                    } else {
                                        "${themePrefs.lastDbName} (local copy)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (themePrefs.linkedDbUri.isNotBlank()) ledger.income else cs.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        GlassPrimaryButton(
                            text = "Reconnect Database",
                            icon = Icons.Rounded.Storage,
                            onClick = onReconnectLast,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "reconnect_db_button"
                        )
                    }
                }

                OnboardingActionCard(
                    icon = Icons.Rounded.FolderOpen,
                    iconTint = ledger.transfer,
                    title = "Open & Live-Link .db File",
                    subtitle = "Open an existing HisaabKitaab .db file — every edit writes directly back to it.",
                    buttonText = "Browse .db File",
                    isPrimary = !hasSavedLocalDb,
                    enabled = !isBusy,
                    testTag = "open_db_button",
                    onClick = onOpenDbFile
                )

                OnboardingActionCard(
                    icon = Icons.Rounded.AddCircleOutline,
                    iconTint = ledger.income,
                    title = "Create Blank Database",
                    subtitle = "Choose where to save your new .db file with default payment mediums — stays live-linked.",
                    buttonText = "Choose Save Location",
                    isPrimary = false,
                    enabled = !isBusy,
                    testTag = "create_blank_db_button",
                    onClick = onCreateBlankDb
                )

                // Privacy pill footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.surfaceContainerLow.copy(alpha = 0.6f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = ledger.income,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Zero servers, zero login, zero tracking. Every change writes live to your chosen .db file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingActionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    buttonText: String,
    isPrimary: Boolean,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentTint = iconTint,
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        if (isPrimary) {
            GlassPrimaryButton(
                text = buttonText,
                icon = icon,
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                testTag = testTag
            )
        } else {
            GlassTonalButton(
                text = buttonText,
                icon = icon,
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                testTag = testTag
            )
        }
    }
}
