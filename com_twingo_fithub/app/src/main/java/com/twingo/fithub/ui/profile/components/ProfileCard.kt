package com.twingo.fithub.ui.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.ui.profile.formatWeight
import com.twingo.fithub.ui.profile.initials
import com.twingo.fithub.ui.theme.FitHubBackground
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubLimeDeep
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurface
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted
import com.twingo.fithub.ui.theme.FitHubError
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert

@Composable
fun ProfileCard(
    profile: Profile,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) FitHubLimeDeep.copy(alpha = 0.55f) else FitHubSurface,
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) FitHubLime else FitHubOutline,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (selected) FitHubLime else FitHubSurfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profile.initials(),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else FitHubText,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = FitHubText,
                    )
                    if (profile.isGuest) {
                        GuestBadge()
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (!profile.isGuest) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options du profil",
                                tint = FitHubTextMuted,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${formatWeight(profile.weightKg)} · FC max ${profile.maxHeartRate} · " +
                        "FC repos ${profile.restingHeartRate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitHubTextMuted,
                )
            }

            if (selected) {
                SelectionIndicator()
            }
        }
    }

    // Profile actions menu
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            containerColor = FitHubSurface,
            title = {
                Column {
                    Text(text = "PROFIL ${profile.name.uppercase()}", style = MaterialTheme.typography.labelSmall, color = FitHubLime, letterSpacing = 2.sp)
                    Text(text = "Actions", style = MaterialTheme.typography.headlineSmall, color = FitHubText)
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) { Text(text = "FERMER", color = FitHubTextMuted) }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (onEdit != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMenu = false; onEdit() },
                            colors = CardDefaults.cardColors(containerColor = FitHubBackground),
                        ) {
                            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = FitHubLime)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "MODIFIER", style = MaterialTheme.typography.titleMedium, color = FitHubText)
                            }
                        }
                    }
                    if (onDelete != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMenu = false; onDelete() },
                            colors = CardDefaults.cardColors(containerColor = FitHubError.copy(alpha = 0.1f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FitHubError),
                        ) {
                            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = FitHubError)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "SUPPRIMER", style = MaterialTheme.typography.titleMedium, color = FitHubError)
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun GuestBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = FitHubOrange.copy(alpha = 0.16f),
    ) {
        Text(
            text = "INVITÉ",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = FitHubOrange,
        )
    }
}

@Composable
private fun SelectionIndicator() {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(FitHubLime.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(FitHubLime),
        )
    }
}
