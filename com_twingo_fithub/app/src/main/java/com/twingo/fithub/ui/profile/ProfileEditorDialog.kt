package com.twingo.fithub.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.ui.theme.FitHubBackground
import com.twingo.fithub.ui.theme.FitHubBlue
import com.twingo.fithub.ui.theme.FitHubError
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurface
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted

@Composable
fun ProfileEditorDialog(
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit,
    existingProfile: Profile? = null,
) {
    val isEditing = existingProfile != null
    var name by remember { mutableStateOf(existingProfile?.name ?: "") }
    var weightKg by remember { mutableStateOf(existingProfile?.weightKg.toString() ?: "70") }
    var maxHeartRate by remember { mutableStateOf(existingProfile?.maxHeartRate.toString() ?: "190") }
    var restingHeartRate by remember { mutableStateOf(existingProfile?.restingHeartRate.toString() ?: "55") }

    val weight = weightKg.toDoubleOrNull() ?: 70.0
    val maxHr = maxHeartRate.toIntOrNull() ?: 190
    val restHr = restingHeartRate.toIntOrNull() ?: 55

    val zones = if (maxHr > restHr && maxHr <= 220 && restHr >= 30) {
        HeartRateZone.entries.toList()
    } else {
        emptyList()
    }

    LaunchedEffect(Unit) {
        // Focus management could be added here
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitHubSurface,
        titleContentColor = FitHubText,
        textContentColor = FitHubTextMuted,
        title = {
            Column {
                Text(
                    text = if (isEditing) "MODIFIER LE PROFIL" else "NOUVEAU PROFIL",
                    style = MaterialTheme.typography.labelSmall,
                    color = FitHubLime,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = existingProfile?.name ?: "Créer un profil",
                    style = MaterialTheme.typography.headlineSmall,
                    color = FitHubText,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Form fields
                ProfileTextField(
                    label = "PRÉNOM",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Ex: Yanis",
                    maxLength = 20,
                )

                ProfileTextField(
                    label = "POIDS (kg)",
                    value = weightKg,
                    onValueChange = { weightKg = it.filter { it.isDigit() || it == '.' } },
                    placeholder = "70",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )

                ProfileTextField(
                    label = "FC MAX (bpm)",
                    value = maxHeartRate,
                    onValueChange = { maxHeartRate = it.filter { it.isDigit() } },
                    placeholder = "190",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )

                ProfileTextField(
                    label = "FC REPOS (bpm)",
                    value = restingHeartRate,
                    onValueChange = { restingHeartRate = it.filter { it.isDigit() } },
                    placeholder = "55",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )

                // Live zone preview
                if (zones.isNotEmpty()) {
                    Text(
                        text = "ZONES CARDIAQUES (Karvonen)",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitHubLime,
                        letterSpacing = 1.5.sp,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        zones.forEach { zone ->
                            ZonePreviewRow(zone = zone, profileMax = maxHr, profileRest = restHr)
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "ANNULER", color = FitHubTextMuted)
            }
        },
        confirmButton = {
            val canSave = name.isNotBlank() &&
                weight > 0 &&
                maxHr > restHr &&
                maxHr <= 220 &&
                restHr >= 30 &&
                restHr < maxHr

            Button(
                onClick = {
                    val profile = Profile(
                        id = existingProfile?.id ?: 0L,
                        name = name.trim(),
                        weightKg = weight,
                        maxHeartRate = maxHr,
                        restingHeartRate = restHr,
                        isGuest = existingProfile?.isGuest ?: false,
                    )
                    onSave(profile)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canSave) FitHubLime else FitHubOutline,
                    contentColor = if (canSave) MaterialTheme.colorScheme.onPrimary else FitHubTextMuted,
                ),
                enabled = canSave,
            ) {
                Text(text = if (isEditing) "SAUVEGARDER" else "CRÉER")
            }
        },
    )
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int = 10,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = FitHubTextMuted,
            letterSpacing = 1.2.sp,
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = placeholder, color = FitHubTextMuted.copy(alpha = 0.5f)) },
            singleLine = true,
        )
    }
}

@Composable
private fun ZonePreviewRow(
    zone: HeartRateZone,
    profileMax: Int,
    profileRest: Int,
) {
    val range = zone.bpmRangeFor(
        com.twingo.fithub.domain.model.Profile(
            id = 0L, name = "", weightKg = 0.0, maxHeartRate = profileMax,
            restingHeartRate = profileRest, isGuest = false
        )
    )
    val color = when (zone) {
        HeartRateZone.Z1 -> FitHubBlue
        HeartRateZone.Z2 -> FitHubLime
        HeartRateZone.Z3 -> FitHubLime
        HeartRateZone.Z4 -> FitHubOrange
        HeartRateZone.Z5 -> FitHubError
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f),
            contentColor = color,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, shape = RoundedCornerShape(5.dp)),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = zone.label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${range.first}–${range.last} bpm",
                style = MaterialTheme.typography.bodyMedium,
                color = FitHubText,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = zone.label,
                style = MaterialTheme.typography.bodySmall,
                color = FitHubTextMuted,
                maxLines = 1,
            )
        }
    }
}