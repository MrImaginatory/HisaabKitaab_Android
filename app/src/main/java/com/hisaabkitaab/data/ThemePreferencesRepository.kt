package com.hisaabkitaab.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.hisaabDataStore by preferencesDataStore(name = "hisaab_kitaab_prefs")

class ThemePreferencesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val IS_LIQUID_GLASS = booleanPreferencesKey("is_liquid_glass")
        val SURFACE_STYLE = stringPreferencesKey("surface_style")
        val ACCENT_HEX = stringPreferencesKey("accent_hex")
        val CHART_PALETTE = stringPreferencesKey("chart_palette")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val ACTIVE_CUSTOM_FONT_ID = stringPreferencesKey("active_custom_font_id")
        val CUSTOM_FONTS_JSON = stringPreferencesKey("custom_fonts_json")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val LAST_DB_NAME = stringPreferencesKey("last_db_name")
        val HAS_ACTIVE_DB = booleanPreferencesKey("has_active_db")
        val LINKED_DB_URI = stringPreferencesKey("linked_db_uri")

        // Transaction form persistence
        val LAST_TXN_TYPE = stringPreferencesKey("last_txn_type")
        val LAST_ACCOUNT_ID = stringPreferencesKey("last_account_id")
        val LAST_TO_ACCOUNT_ID = stringPreferencesKey("last_to_account_id")
        val LAST_CATEGORY_ID = stringPreferencesKey("last_category_id")
        val LAST_PAYMENT_GROUP = stringPreferencesKey("last_payment_group")
        val LAST_PAYMENT_MEDIUM_ID = stringPreferencesKey("last_payment_medium_id")
    }

    private fun decodeCustomFonts(raw: String?): List<CustomFontEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<CustomFontEntry>>(raw) }
            .getOrDefault(emptyList())
            .filter { CustomFontManager.fontFileExists(context, it) }
    }

    val themePreferences: Flow<ThemePreferences> = context.hisaabDataStore.data.map { prefs ->
        val savedCustomFonts = decodeCustomFonts(prefs[Keys.CUSTOM_FONTS_JSON])
        val storedActiveCustomId = prefs[Keys.ACTIVE_CUSTOM_FONT_ID].orEmpty()
        val resolvedActiveCustomId = if (savedCustomFonts.any { it.id == storedActiveCustomId }) {
            storedActiveCustomId
        } else {
            ""
        }

        ThemePreferences(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
            surfaceStyle = SurfaceStyle.fromKey(
                key = prefs[Keys.SURFACE_STYLE],
                legacyLiquidGlass = prefs[Keys.IS_LIQUID_GLASS]
            ),
            accentHex = prefs[Keys.ACCENT_HEX] ?: "#FCD535",
            chartPalette = ChartPalette.fromKey(prefs[Keys.CHART_PALETTE]),
            fontFamily = AppFontFamily.fromKey(prefs[Keys.FONT_FAMILY]),
            activeCustomFontId = resolvedActiveCustomId,
            customFonts = savedCustomFonts,
            fontScale = FontScaleOption.fromKey(prefs[Keys.FONT_SCALE]),
            lastDbName = prefs[Keys.LAST_DB_NAME] ?: "hisaab_kitaab.db",
            hasActiveDb = prefs[Keys.HAS_ACTIVE_DB] ?: false,
            linkedDbUri = prefs[Keys.LINKED_DB_URI] ?: ""
        )
    }

    val txnFormDefaults: Flow<TransactionFormDefaults> = context.hisaabDataStore.data.map { prefs ->
        TransactionFormDefaults(
            type = TransactionType.fromDb(prefs[Keys.LAST_TXN_TYPE] ?: "expense"),
            accountId = prefs[Keys.LAST_ACCOUNT_ID] ?: "",
            toAccountId = prefs[Keys.LAST_TO_ACCOUNT_ID] ?: "",
            categoryId = prefs[Keys.LAST_CATEGORY_ID] ?: "",
            paymentGroup = PaymentGroup.fromDb(prefs[Keys.LAST_PAYMENT_GROUP] ?: "online"),
            paymentMediumId = prefs[Keys.LAST_PAYMENT_MEDIUM_ID] ?: ""
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.hisaabDataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    suspend fun setSurfaceStyle(style: SurfaceStyle) {
        context.hisaabDataStore.edit {
            it[Keys.SURFACE_STYLE] = style.key
            it[Keys.IS_LIQUID_GLASS] = style != SurfaceStyle.CLASSIC_M3
        }
    }

    suspend fun setAccentHex(hex: String) {
        context.hisaabDataStore.edit { it[Keys.ACCENT_HEX] = StringUtils.normalizeHexColor(hex, "#FCD535") }
    }

    suspend fun setChartPalette(palette: ChartPalette) {
        context.hisaabDataStore.edit { it[Keys.CHART_PALETTE] = palette.key }
    }

    suspend fun setFontFamily(font: AppFontFamily) {
        context.hisaabDataStore.edit {
            it[Keys.FONT_FAMILY] = font.key
            it[Keys.ACTIVE_CUSTOM_FONT_ID] = ""
        }
    }

    suspend fun addAndActivateCustomFont(entry: CustomFontEntry) {
        context.hisaabDataStore.edit { prefs ->
            val current = decodeCustomFonts(prefs[Keys.CUSTOM_FONTS_JSON])
            val updated = listOf(entry) + current.filterNot { it.id == entry.id }
            prefs[Keys.CUSTOM_FONTS_JSON] = json.encodeToString(updated)
            prefs[Keys.ACTIVE_CUSTOM_FONT_ID] = entry.id
        }
    }

    suspend fun selectCustomFont(fontId: String) {
        context.hisaabDataStore.edit { prefs ->
            val current = decodeCustomFonts(prefs[Keys.CUSTOM_FONTS_JSON])
            if (current.any { it.id == fontId }) {
                prefs[Keys.ACTIVE_CUSTOM_FONT_ID] = fontId
            }
        }
    }

    suspend fun removeCustomFont(fontId: String): CustomFontEntry? {
        var removed: CustomFontEntry? = null
        context.hisaabDataStore.edit { prefs ->
            val current = decodeCustomFonts(prefs[Keys.CUSTOM_FONTS_JSON])
            removed = current.firstOrNull { it.id == fontId }
            val updated = current.filterNot { it.id == fontId }
            prefs[Keys.CUSTOM_FONTS_JSON] = json.encodeToString(updated)
            if (prefs[Keys.ACTIVE_CUSTOM_FONT_ID] == fontId) {
                prefs[Keys.ACTIVE_CUSTOM_FONT_ID] = ""
            }
        }
        return removed
    }

    suspend fun setFontScale(scale: FontScaleOption) {
        context.hisaabDataStore.edit { it[Keys.FONT_SCALE] = scale.key }
    }

    suspend fun resetAppearanceDefaults() {
        context.hisaabDataStore.edit {
            it[Keys.IS_DARK_MODE] = true
            it[Keys.SURFACE_STYLE] = SurfaceStyle.LIQUID_GLASS.key
            it[Keys.IS_LIQUID_GLASS] = true
            it[Keys.ACCENT_HEX] = "#FCD535"
            it[Keys.CHART_PALETTE] = ChartPalette.VIBRANT_JEWEL.key
            it[Keys.FONT_FAMILY] = AppFontFamily.PLUS_JAKARTA_SANS.key
            // Switch back to default bundled font while keeping saved custom font files intact
            it[Keys.ACTIVE_CUSTOM_FONT_ID] = ""
            it[Keys.FONT_SCALE] = FontScaleOption.M.key
        }
    }

    suspend fun setActiveDbInfo(name: String, active: Boolean) {
        context.hisaabDataStore.edit {
            it[Keys.LAST_DB_NAME] = name.ifBlank { "hisaab_kitaab.db" }
            it[Keys.HAS_ACTIVE_DB] = active
        }
    }

    suspend fun setLinkedDbInfo(name: String, linkedUri: String?, active: Boolean) {
        context.hisaabDataStore.edit {
            it[Keys.LAST_DB_NAME] = name.ifBlank { "hisaab_kitaab.db" }
            it[Keys.LINKED_DB_URI] = linkedUri.orEmpty()
            it[Keys.HAS_ACTIVE_DB] = active
        }
    }

    suspend fun saveTxnFormDefaults(defaults: TransactionFormDefaults) {
        context.hisaabDataStore.edit {
            it[Keys.LAST_TXN_TYPE] = defaults.type.dbValue
            it[Keys.LAST_ACCOUNT_ID] = defaults.accountId
            it[Keys.LAST_TO_ACCOUNT_ID] = defaults.toAccountId
            it[Keys.LAST_CATEGORY_ID] = defaults.categoryId
            it[Keys.LAST_PAYMENT_GROUP] = defaults.paymentGroup.dbValue
            it[Keys.LAST_PAYMENT_MEDIUM_ID] = defaults.paymentMediumId
        }
    }
}
