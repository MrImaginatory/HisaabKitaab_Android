package com.hisaabkitaab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.UserProfile
import com.hisaabkitaab.ui.theme.GlassCard
import com.hisaabkitaab.ui.theme.GlassPrimaryButton
import com.hisaabkitaab.ui.theme.GlassTextField
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import com.hisaabkitaab.ui.theme.MaxLengthTransformation
import com.hisaabkitaab.ui.theme.glassTextFieldColors
import com.hisaabkitaab.ui.viewmodel.HisaabViewModel

@Composable
fun ProfileScreen(
    viewModel: HisaabViewModel,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current

    var name by remember { mutableStateOf(profile.name) }
    var address by remember { mutableStateOf(profile.address) }
    var email by remember { mutableStateOf(profile.email) }
    var contact by remember { mutableStateOf(profile.contact) }
    var watermark by remember { mutableStateOf(profile.watermark) }
    var currencyName by remember { mutableStateOf(profile.effectiveCurrencyName) }
    var currencySymbol by remember { mutableStateOf(profile.effectiveSymbol) }

    LaunchedEffect(profile) {
        name = profile.name
        address = profile.address
        email = profile.email
        contact = profile.contact
        watermark = profile.watermark
        currencyName = profile.effectiveCurrencyName
        currencySymbol = profile.effectiveSymbol
    }

    val currencyPresets = remember {
        listOf(
            "INR" to "₹",
            "USD" to "$",
            "EUR" to "€",
            "GBP" to "£",
            "JPY" to "¥",
            "AED" to "د.إ"
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                Text("Personal & Statement Profile", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Stored inside your local SQLite .db file and printed on exported PDF & Excel statements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name / Business Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )
                    GlassTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("profile_email_input")
                    )
                    GlassTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Contact / Phone") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("profile_contact_input")
                    )
                    GlassTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("profile_address_input")
                    )
                    GlassTextField(
                        value = watermark,
                        onValueChange = { watermark = it },
                        label = { Text("PDF Diagonal Watermark Text") },
                        placeholder = { Text("e.g. CONFIDENTIAL — PRABHAT") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("profile_watermark_input")
                    )

                    Text("Currency", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(currencyPresets) { (code, sym) ->
                            val selected = currencyName.equals(code, ignoreCase = true) && currencySymbol == sym
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) cs.primary else cs.surfaceContainerHigh)
                                    .border(1.dp, if (selected) cs.primary else cs.outlineVariant, RoundedCornerShape(999.dp))
                                    .clickable {
                                        currencyName = code
                                        currencySymbol = sym
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "$sym $code",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) cs.onPrimary else cs.onSurface
                                )
                            }
                        }
                    }

                    val currencyCodeLimit = remember { MaxLengthTransformation(10) }
                    val currencySymbolLimit = remember { MaxLengthTransformation(5) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassTextField(
                            value = currencyName,
                            onValueChange = { currencyName = it },
                            label = { Text("Currency Code") },
                            singleLine = true,
                            inputTransformation = currencyCodeLimit,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        GlassTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            label = { Text("Symbol") },
                            singleLine = true,
                            inputTransformation = currencySymbolLimit,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    GlassPrimaryButton(
                        text = "Save Profile",
                        icon = Icons.Rounded.Save,
                        onClick = {
                            viewModel.saveProfile(
                                UserProfile(
                                    name = name,
                                    address = address,
                                    email = email,
                                    contact = contact,
                                    watermark = watermark,
                                    currencyName = currencyName,
                                    currencySymbol = currencySymbol
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "profile_save_button"
                    )
                }
            }
        }

        // Live Statement Export Preview Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentTint = cs.primary,
                contentPadding = PaddingValues(18.dp)
            ) {
                Text("Live Statement Export Preview", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.surfaceContainerHigh.copy(alpha = 0.65f))
                        .border(1.dp, cs.outlineVariant, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    // Watermark overlay preview
                    Text(
                        text = watermark.ifBlank { name.ifBlank { "HISAABKITAAB" } }.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = cs.onSurface.copy(alpha = 0.08f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .rotate(-16f)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = name.ifBlank { "Account Holder Name" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (address.isNotBlank()) {
                            Text(address, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        if (email.isNotBlank() || contact.isNotBlank()) {
                            Text(
                                listOf(email, contact).filter { it.isNotBlank() }.joinToString("  •  "),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = cs.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sample Formatting", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                            Text(
                                text = StringUtils.formatCurrency(125450.75, currencySymbol.ifBlank { "₹" }),
                                fontFamily = ledger.monoFont,
                                fontWeight = FontWeight.Bold,
                                color = ledger.income
                            )
                        }
                    }
                }
            }
        }
    }
}
