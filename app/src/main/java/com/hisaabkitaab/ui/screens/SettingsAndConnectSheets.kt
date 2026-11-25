package com.hisaabkitaab.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.ui.graphics.Brush
import com.hisaabkitaab.data.ChartPalette
import com.hisaabkitaab.data.CustomFontManager
import com.hisaabkitaab.data.DbLinkStatus
import com.hisaabkitaab.data.SurfaceStyle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hisaabkitaab.data.AppDestination
import com.hisaabkitaab.data.AppFontFamily
import com.hisaabkitaab.data.FontScaleOption
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.ThemePreferences
import com.hisaabkitaab.ui.theme.BundledFonts
import com.hisaabkitaab.ui.theme.ColorSwatchSelector
import com.hisaabkitaab.ui.theme.GlassCard
import com.hisaabkitaab.ui.theme.GlassPrimaryButton
import com.hisaabkitaab.ui.theme.GlassTonalButton
import com.hisaabkitaab.ui.theme.HisaabBrandLogo
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import com.hisaabkitaab.ui.theme.SegmentedPillBar
import com.hisaabkitaab.ui.theme.parseHexColorSafe
import com.hisaabkitaab.ui.viewmodel.HisaabViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: HisaabViewModel,
    themePrefs: ThemePreferences,
    dbLinkStatus: DbLinkStatus,
    isSyncingDb: Boolean,
    dbSizeBytes: Long,
    accountCount: Int,
    txnCount: Int,
    categoryCount: Int,
    mediumCount: Int,
    onLinkCurrentDbToFile: () -> Unit,
    onSyncLinkedDbNow: () -> Unit,
    onCreateLinkedBlankDb: () -> Unit,
    onExportDbBackup: () -> Unit,
    onImportDbFile: () -> Unit,
    onSwitchDatabase: () -> Unit,
    onImportCustomFont: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    var selectedTab by remember { mutableStateOf("Appearance") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            SegmentedPillBar(
                options = listOf("Appearance", "Typography", "Database"),
                selected = selectedTab,
                onSelect = { selectedTab = it },
                labelOf = { it },
                testTagPrefix = "settings_tab"
            )

            when (selectedTab) {
                "Appearance" -> {
                    // 3-Preset Surface Style Visual Selector (Classic M3, Frosted, Liquid Glass)
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = cs.primary)
                                Column {
                                    Text("Surface Style", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        themePrefs.surfaceStyle.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.hisaabkitaab.data.SurfaceStyle.entries.forEach { preset ->
                                SurfaceStylePresetTile(
                                    style = preset,
                                    selected = themePrefs.surfaceStyle == preset,
                                    accent = cs.primary,
                                    isDark = themePrefs.isDarkMode,
                                    onClick = { viewModel.setSurfaceStyle(preset) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = cs.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = cs.primary)
                                Column {
                                    Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
                                    Text("High-contrast ledger palette", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = themePrefs.isDarkMode,
                                onCheckedChange = { viewModel.setDarkMode(it) },
                                modifier = Modifier.testTag("settings_dark_mode_switch")
                            )
                        }
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Text("Accent Color", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        ColorSwatchSelector(
                            selectedHex = themePrefs.accentHex,
                            onHexSelected = { viewModel.setAccentHex(it) }
                        )
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = cs.primary)
                            Column {
                                Text("Chart Color Palette", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Distinct hues for donut & trend charts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChartPalette.entries.forEach { palette ->
                                val selected = themePrefs.chartPalette == palette
                                val previewDots = remember(palette) {
                                    listOf(palette.incomeHex, palette.expenseHex) + palette.categoryColors.take(4)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selected) cs.primary.copy(alpha = 0.16f)
                                            else cs.surfaceContainerHigh.copy(alpha = 0.65f)
                                        )
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) cs.primary else cs.outlineVariant,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { viewModel.setChartPalette(palette) }
                                        .testTag("settings_chart_palette_${palette.key}")
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (selected) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "Selected palette",
                                                tint = cs.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = palette.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = cs.onSurface
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        previewDots.forEach { hex ->
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColorSafe(hex))
                                                    .border(
                                                        0.8.dp,
                                                        Color.White.copy(alpha = 0.35f),
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GlassTonalButton(
                        text = "Reset Appearance to Defaults",
                        icon = Icons.Rounded.RestartAlt,
                        onClick = { viewModel.resetAppearanceDefaults() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_reset_defaults_button"
                    )
                }

                "Typography" -> {
                    // User-Imported Custom Fonts (.ttf & .otf only, persisted in app storage)
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.FontDownload, contentDescription = null, tint = cs.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Custom Fonts (.ttf / .otf)", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Saved in app storage across restarts • numbers stay tabular mono",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        GlassPrimaryButton(
                            text = "Import .ttf or .otf Font",
                            icon = Icons.Rounded.UploadFile,
                            onClick = onImportCustomFont,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "settings_import_custom_font_button"
                        )

                        if (themePrefs.customFonts.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                themePrefs.customFonts.forEach { customFont ->
                                    val selected = themePrefs.activeCustomFontId == customFont.id
                                    val previewFamily = remember(customFont.id, customFont.fileName) {
                                        CustomFontManager.loadFontFamily(context, customFont)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (selected) cs.primary.copy(alpha = 0.18f)
                                                else cs.surfaceContainerHigh.copy(alpha = 0.6f)
                                            )
                                            .border(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) cs.primary else cs.outlineVariant,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { viewModel.selectCustomFont(customFont.id) }
                                            .testTag("settings_custom_font_${customFont.id}")
                                            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    Icons.Rounded.Check,
                                                    contentDescription = "Selected custom font",
                                                    tint = cs.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = customFont.displayName,
                                                    fontFamily = previewFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = cs.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Aa Bb Cc • ${customFont.extension}",
                                                    fontFamily = previewFamily,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) cs.primary else cs.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteCustomFont(customFont.id) },
                                            modifier = Modifier.testTag("settings_delete_custom_font_${customFont.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete ${customFont.displayName}",
                                                tint = ledger.expense,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Text("Built-in Fonts (Bundled Offline)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppFontFamily.entries.forEach { font ->
                                val selected = themePrefs.activeCustomFontId.isBlank() && themePrefs.fontFamily == font
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) cs.primary.copy(alpha = 0.18f) else cs.surfaceContainerHigh.copy(alpha = 0.6f))
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) cs.primary else cs.outlineVariant,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { viewModel.setFontFamily(font) }
                                        .testTag("settings_font_${font.key}")
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (selected) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "Selected font",
                                                tint = cs.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = font.displayName,
                                            fontFamily = BundledFonts.resolve(font),
                                            fontWeight = FontWeight.SemiBold,
                                            color = cs.onSurface
                                        )
                                    }
                                    Text(
                                        text = "₹1,23,456",
                                        fontFamily = LocalLedgerColors.current.monoFont,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) cs.primary else cs.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Font Scale", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${themePrefs.fontScale.label} (${(themePrefs.fontScale.multiplier * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelLarge,
                                color = cs.primary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        SegmentedPillBar(
                            options = FontScaleOption.entries,
                            selected = themePrefs.fontScale,
                            onSelect = { viewModel.setFontScale(it) },
                            labelOf = { it.label },
                            testTagPrefix = "settings_scale"
                        )
                    }
                }

                "Database" -> {
                    val statusTint = when (dbLinkStatus) {
                        is DbLinkStatus.LiveLinked -> ledger.income
                        is DbLinkStatus.NeedsRelink -> ledger.warning
                        is DbLinkStatus.LocalOnly -> cs.onSurfaceVariant
                    }
                    val statusBg = when (dbLinkStatus) {
                        is DbLinkStatus.LiveLinked -> ledger.incomeContainer
                        is DbLinkStatus.NeedsRelink -> ledger.warningContainer
                        is DbLinkStatus.LocalOnly -> cs.surfaceContainerHigh
                    }
                    val statusLabel = when {
                        isSyncingDb -> "Syncing…"
                        dbLinkStatus is DbLinkStatus.LiveLinked -> "Live Sync Active"
                        dbLinkStatus is DbLinkStatus.NeedsRelink -> "Relink Required"
                        else -> "Local Only"
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentTint = statusTint,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active SQLite Ledger", style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(statusBg)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("settings_db_link_status_pill"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isSyncingDb) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.8.dp,
                                        color = statusTint
                                    )
                                } else {
                                    Icon(
                                        imageVector = when (dbLinkStatus) {
                                            is DbLinkStatus.LiveLinked -> Icons.Rounded.CheckCircle
                                            is DbLinkStatus.NeedsRelink -> Icons.Rounded.WarningAmber
                                            is DbLinkStatus.LocalOnly -> Icons.Rounded.Link
                                        },
                                        contentDescription = statusLabel,
                                        tint = statusTint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusTint
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            themePrefs.lastDbName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.primary
                        )

                        Spacer(Modifier.height(4.dp))
                        when (dbLinkStatus) {
                            is DbLinkStatus.LiveLinked -> {
                                val formattedTime = remember(dbLinkStatus.lastSyncedAtMillis) {
                                    SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        .format(Date(dbLinkStatus.lastSyncedAtMillis))
                                }
                                Text(
                                    "Writes every entry directly to your .db file • Last synced $formattedTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                            is DbLinkStatus.NeedsRelink -> {
                                Text(
                                    "Your latest entries are safely saved locally. ${dbLinkStatus.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ledger.warning
                                )
                            }
                            is DbLinkStatus.LocalOnly -> {
                                Text(
                                    "Saved inside app storage only. Link a .db file below to sync every edit directly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatPill("Size", StringUtils.formatFileSize(dbSizeBytes))
                            StatPill("Accounts", "$accountCount")
                            StatPill("Entries", "$txnCount")
                            StatPill("Categories", "$categoryCount")
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "$mediumCount payment mediums configured • 100% compatible with HisaabKitaab web .db files.",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant
                        )
                    }

                    when (dbLinkStatus) {
                        is DbLinkStatus.NeedsRelink -> {
                            GlassPrimaryButton(
                                text = "Relink & Push Changes to .db",
                                icon = Icons.Rounded.Link,
                                onClick = {
                                    onDismiss()
                                    onLinkCurrentDbToFile()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "settings_relink_db_button"
                            )
                            GlassTonalButton(
                                text = if (isSyncingDb) "Syncing…" else "Retry Sync Now",
                                icon = Icons.Rounded.Sync,
                                onClick = onSyncLinkedDbNow,
                                enabled = !isSyncingDb,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "settings_sync_now_button"
                            )
                        }
                        is DbLinkStatus.LocalOnly -> {
                            GlassPrimaryButton(
                                text = "Link to Live .db File",
                                icon = Icons.Rounded.Link,
                                onClick = {
                                    onDismiss()
                                    onLinkCurrentDbToFile()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "settings_link_db_button"
                            )
                        }
                        is DbLinkStatus.LiveLinked -> {
                            GlassPrimaryButton(
                                text = if (isSyncingDb) "Syncing to .db…" else "Sync Now",
                                icon = Icons.Rounded.Sync,
                                onClick = onSyncLinkedDbNow,
                                enabled = !isSyncingDb,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "settings_sync_now_button"
                            )
                        }
                    }

                    GlassTonalButton(
                        text = "Open & Live-Link Another .db File",
                        icon = Icons.Rounded.FolderOpen,
                        onClick = {
                            onDismiss()
                            onImportDbFile()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_import_db_button"
                    )
                    GlassTonalButton(
                        text = "Create New Linked .db File",
                        icon = Icons.Rounded.AddCircleOutline,
                        onClick = {
                            onDismiss()
                            onCreateLinkedBlankDb()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_create_linked_db_button"
                    )
                    GlassTonalButton(
                        text = "Export .db Backup Copy",
                        icon = Icons.Rounded.SaveAlt,
                        onClick = {
                            onDismiss()
                            onExportDbBackup()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_export_db_button"
                    )
                    GlassTonalButton(
                        text = "Switch Database Screen",
                        icon = Icons.Rounded.SwapHoriz,
                        onClick = {
                            onDismiss()
                            onSwitchDatabase()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_switch_db_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun SurfaceStylePresetTile(
    style: SurfaceStyle,
    selected: Boolean,
    accent: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val tileShape = RoundedCornerShape(18.dp)

    val previewBg = when (style) {
        SurfaceStyle.CLASSIC_M3 -> Brush.verticalGradient(
            listOf(cs.surfaceContainerHigh, cs.surfaceContainerLow)
        )
        SurfaceStyle.FROSTED -> Brush.linearGradient(
            listOf(
                accent.copy(alpha = if (isDark) 0.24f else 0.18f),
                cs.surfaceContainerHigh.copy(alpha = 0.85f)
            )
        )
        SurfaceStyle.LIQUID_GLASS -> Brush.linearGradient(
            listOf(
                accent.copy(alpha = if (isDark) 0.42f else 0.32f),
                Color(0xFF0ECB81).copy(alpha = if (isDark) 0.26f else 0.20f),
                Color(0xFF3B82F6).copy(alpha = if (isDark) 0.30f else 0.22f)
            )
        )
    }

    val cardOverlay = when (style) {
        SurfaceStyle.CLASSIC_M3 -> cs.surfaceContainerHighest
        SurfaceStyle.FROSTED -> if (isDark) Color(0xCC1E2838) else Color(0xD9FFFFFF)
        SurfaceStyle.LIQUID_GLASS -> if (isDark) Color(0x881A2638) else Color(0x9EFFFFFF)
    }

    Column(
        modifier = modifier
            .clip(tileShape)
            .background(if (selected) accent.copy(alpha = 0.14f) else cs.surfaceContainerHigh.copy(alpha = 0.50f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else cs.outlineVariant,
                shape = tileShape
            )
            .clickable(onClick = onClick)
            .testTag("settings_surface_${style.key}")
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mini visual swatch depicting the surface optics
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(previewBg)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(cardOverlay)
                    .border(
                        width = 1.dp,
                        color = when (style) {
                            SurfaceStyle.CLASSIC_M3 -> cs.outlineVariant
                            SurfaceStyle.FROSTED -> Color.White.copy(alpha = if (isDark) 0.25f else 0.75f)
                            SurfaceStyle.LIQUID_GLASS -> Color.White.copy(alpha = if (isDark) 0.55f else 0.95f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = cs.onPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = style.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else cs.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontFamily = LocalLedgerColors.current.monoFont,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHubBottomSheet(
    onNavigate: (AppDestination) -> Unit,
    onScanUpiReceipt: () -> Unit,
    onOpenSettings: () -> Unit,
    onExportDbBackup: () -> Unit,
    onOpenConnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surfaceContainerLow,
        contentColor = cs.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Manage Ledger", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))

            ManageRowItem(
                Icons.Rounded.DocumentScanner,
                "Scan UPI Receipt",
                "Auto-fill from GPay, PhonePe, Paytm or BHIM screenshot",
                ledger.income,
                "manage_scan_upi_item"
            ) {
                onDismiss()
                onScanUpiReceipt()
            }
            ManageRowItem(Icons.Rounded.Category, "Categories", "Manage tags, colors & CSV bulk import", cs.primary, "manage_categories_item") {
                onDismiss()
                onNavigate(AppDestination.CATEGORIES)
            }
            ManageRowItem(Icons.Rounded.CreditCard, "Payment Mediums", "Online (UPI, Cards) & Offline (Cash, Cheque)", ledger.transfer, "manage_mediums_item") {
                onDismiss()
                onNavigate(AppDestination.PAYMENT_MEDIUMS)
            }
            ManageRowItem(Icons.Rounded.Person, "Profile & Watermark", "Personal info, currency symbol & PDF watermark", ledger.income, "manage_profile_item") {
                onDismiss()
                onNavigate(AppDestination.PROFILE)
            }
            HorizontalDivider(color = cs.outlineVariant)
            ManageRowItem(Icons.Rounded.Settings, "Appearance & Settings", "Liquid Glass UI, Accent Picker, Font Scale & DB", cs.primary, "manage_settings_item") {
                onDismiss()
                onOpenSettings()
            }
            ManageRowItem(Icons.Rounded.SaveAlt, "Export .db Backup", "Save portable SQLite backup to your device", ledger.income, "manage_backup_item") {
                onDismiss()
                onExportDbBackup()
            }
            ManageRowItem(Icons.Rounded.Code, "Connect with Developer", "GitHub, Instagram, Reddit, X & LinkedIn", ledger.transfer, "manage_connect_item") {
                onDismiss()
                onOpenConnect()
            }
        }
    }
}

@Composable
private fun ManageRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surfaceContainerHigh.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperConnectSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    val links = remember {
        listOf(
            Triple("GitHub Repository", "MrImaginatory / HisaabKitaab", "https://github.com/MrImaginatory/HisaabKitaab"),
            Triple("Instagram", "@mr.imaginatory", "https://www.instagram.com/mr.imaginatory"),
            Triple("Reddit", "u/Mr_Imaginatory", "https://www.reddit.com/user/Mr_Imaginatory/"),
            Triple("X (Twitter)", "@mr_imaginatory", "https://x.com/mr_imaginatory"),
            Triple("LinkedIn", "Prabhat Sharma", "https://www.linkedin.com/in/prabhat-sharma-501ab12a9/")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surfaceContainerLow,
        contentColor = cs.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HisaabBrandLogo(size = 42.dp)
                Column {
                    Text("Connect with Developer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "HisaabKitaab — 100% private offline finance tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            links.forEach { (title, handle, url) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.surfaceContainerHigh.copy(alpha = 0.6f))
                        .clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Text(handle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open link", tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
