package com.twingo.fithub.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutPlan
import com.twingo.fithub.core.sync.SyncState
import com.twingo.fithub.ui.profile.components.ProfileCard
import com.twingo.fithub.ui.profile.components.WorkoutStepCard
import com.twingo.fithub.ui.theme.FitHubBackground
import com.twingo.fithub.ui.theme.FitHubLime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurface
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted
import java.time.LocalDate
import java.util.Locale

private val PanelShape = RoundedCornerShape(28.dp)

@Composable
fun ProfileSelectionScreen(
    state: ProfileSelectionUiState,
    onProfileSelected: (Long) -> Unit,
    onStartWorkout: (WorkoutPlan) -> Unit,
    onOpenEditor: () -> Unit,
    onSyncClick: () -> Unit = {},
    syncState: SyncState = SyncState.Idle,
    modifier: Modifier = Modifier,
) {
    var showProfileEditor by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(FitHubBackground, Color(0xFF101A13), FitHubBackground),
                ),
            ),
    ) {
        val profilePaneWidth = (maxWidth * 0.32f).coerceIn(320.dp, 440.dp)
        val isWideLayout = maxWidth >= 900.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AppHeader(
                today = state.today,
                onSettingsClick = { onOpenEditor() },
                onSyncClick = onSyncClick,
                syncState = syncState,
            )

            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    ProfilePane(
                        state = state,
                        onProfileSelected = onProfileSelected,
                        onOpenEditor = onOpenEditor,
                        modifier = Modifier
                            .width(profilePaneWidth)
                            .fillMaxHeight(),
                    )
                    WorkoutPane(
                        state = state,
                        onStartWorkout = onStartWorkout,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CompactProfileStrip(
                        state = state,
                        onProfileSelected = onProfileSelected,
                        onOpenEditor = onOpenEditor,
                    )
                    WorkoutPane(
                        state = state,
                        onStartWorkout = onStartWorkout,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }

    // Profile Editor Dialog
    if (showProfileEditor) {
        ProfileEditorDialog(
            onDismiss = { showProfileEditor = false; editingProfile = null },
            onSave = { _ ->
                showProfileEditor = false
                editingProfile = null
                onOpenEditor()
            },
            existingProfile = editingProfile,
        )
    }
}

@Composable
private fun AppHeader(
    today: LocalDate,
    onSettingsClick: () -> Unit,
    onSyncClick: () -> Unit,
    syncState: SyncState = SyncState.Idle,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "FITHUB",
                style = MaterialTheme.typography.labelSmall,
                color = FitHubLime,
                letterSpacing = 3.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Prêt pour la séance ?",
                style = MaterialTheme.typography.headlineLarge,
                color = FitHubText,
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = FitHubSurface,
            border = BorderStroke(1.dp, FitHubOutline),
        ) {
            Text(
                text = formatFullDate(today),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = FitHubTextMuted,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Sync button with status indicator
        Box {
            IconButton(onClick = onSyncClick, enabled = syncState != SyncState.Syncing) {
                Icon(
                    imageVector = when (syncState) {
                        is SyncState.Syncing -> Icons.Default.Sync
                        is SyncState.Connected -> Icons.Default.CloudSync
                        is SyncState.Error -> Icons.Default.Error
                        else -> Icons.Default.CloudSync
                    },
                    contentDescription = "Synchronisation serveur",
                    tint = when (syncState) {
                        is SyncState.Syncing -> FitHubLime
                        is SyncState.Connected -> FitHubLime
                        is SyncState.Error -> FitHubOrange
                        else -> FitHubTextMuted
                    },
                    modifier = Modifier.size(24.dp),
                )
            }
            // Sync status indicator
            if (syncState != SyncState.Idle) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            when (syncState) {
                                is SyncState.Syncing -> FitHubLime
                                is SyncState.Connected -> FitHubLime
                                is SyncState.Error -> FitHubOrange
                                else -> FitHubTextMuted
                            },
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Settings / Editor button
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Réglages et éditeur",
                tint = FitHubLime,
                modifier = Modifier
                    .padding(16.dp)
                    .size(24.dp),
            )
        }
    }
}



@Composable
private fun ProfilePane(
    state: ProfileSelectionUiState,
    onProfileSelected: (Long) -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "PROFIL ACTIF",
            style = MaterialTheme.typography.labelSmall,
            color = FitHubLime,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Qui s'entraîne ?",
            style = MaterialTheme.typography.headlineMedium,
            color = FitHubText,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Sélectionnez un profil pour afficher la séance du jour.",
            style = MaterialTheme.typography.bodyMedium,
            color = FitHubTextMuted,
        )
        Spacer(modifier = Modifier.height(20.dp))

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FitHubLime)
                }
            }

            state.profiles.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Aucun profil enregistré sur cette tablette.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = FitHubTextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.profiles, key = Profile::id) { profile ->
                        ProfileCard(
                            profile = profile,
                            selected = state.selectedProfile?.id == profile.id,
                            onClick = { onProfileSelected(profile.id) },
                            onEdit = { 
                                // Open editor with this profile
                                onOpenEditor()
                            },
                            onDelete = {
                                // Delete profile via view model
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactProfileStrip(
    state: ProfileSelectionUiState,
    onProfileSelected: (Long) -> Unit,
    onOpenEditor: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PROFIL ACTIF",
                    style = MaterialTheme.typography.labelSmall,
                    color = FitHubLime,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "Qui s'entraîne ?",
                    style = MaterialTheme.typography.titleLarge,
                    color = FitHubText,
                )
            }
            if (!state.isLoading && state.profiles.isNotEmpty()) {
                Text(
                    text = "${state.profiles.size} profils",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitHubTextMuted,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = FitHubLime,
                    )
                }
            }

            state.profiles.isEmpty() -> {
                Text(
                    text = "Aucun profil enregistré sur cette tablette.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitHubTextMuted,
                )
            }

            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(items = state.profiles, key = Profile::id) { profile ->
                        ProfileCard(
                            profile = profile,
                            selected = state.selectedProfile?.id == profile.id,
                            onClick = { onProfileSelected(profile.id) },
                            onEdit = onOpenEditor,
                            modifier = Modifier.width(340.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutPane(
    state: ProfileSelectionUiState,
    onStartWorkout: (WorkoutPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedProfile = state.selectedProfile
    when {
        state.isLoading -> LoadingPanel(modifier = modifier)
        selectedProfile == null -> EmptySelectionPanel(modifier = modifier)
        state.todayWorkout == null -> RestDayPanel(
            profile = selectedProfile,
            modifier = modifier,
        )

        else -> TodayWorkoutPanel(
            profile = selectedProfile,
            plan = state.todayWorkout,
            today = state.today,
            onStartWorkout = onStartWorkout,
            modifier = modifier,
        )
    }
}

@Composable
private fun LoadingPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = PanelShape,
        color = FitHubSurface,
        border = BorderStroke(1.dp, FitHubOutline),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FitHubLime)
        }
    }
}

@Composable
private fun EmptySelectionPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = PanelShape,
        color = FitHubSurface,
        border = BorderStroke(1.dp, FitHubOutline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "SÉANCE DU JOUR",
                style = MaterialTheme.typography.labelSmall,
                color = FitHubLime,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Choisissez votre profil",
                style = MaterialTheme.typography.headlineLarge,
                color = FitHubText,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Le programme du jour, les cibles cardiaques et les consignes " +
                    "s'afficheront ici.",
                modifier = Modifier.widthIn(max = 480.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = FitHubTextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RestDayPanel(
    profile: Profile,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = PanelShape,
        color = FitHubSurface,
        border = BorderStroke(1.dp, FitHubOutline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "RÉCUPÉRATION",
                style = MaterialTheme.typography.labelSmall,
                color = FitHubOrange,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Jour de repos",
                style = MaterialTheme.typography.headlineLarge,
                color = FitHubText,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Aucune séance n'est planifiée aujourd'hui pour ${profile.name}. " +
                    "Marche légère, hydratation et sommeil : la récupération fait partie du programme.",
                modifier = Modifier.widthIn(max = 560.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = FitHubTextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = FitHubSurfaceElevated,
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
                    Text(
                        text = "PROCHAIN RENDEZ-VOUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitHubLime,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Le programme reprend au prochain jour planifié de la semaine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitHubTextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayWorkoutPanel(
    profile: Profile,
    plan: WorkoutPlan,
    today: LocalDate,
    onStartWorkout: (WorkoutPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = PanelShape,
        color = FitHubSurface,
        border = BorderStroke(1.dp, FitHubOutline),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            if (maxWidth >= 700.dp) {
                WideWorkoutContent(
                    profile = profile,
                    plan = plan,
                    today = today,
                    onStartWorkout = onStartWorkout,
                )
            } else {
                CompactWorkoutContent(
                    profile = profile,
                    plan = plan,
                    today = today,
                    onStartWorkout = onStartWorkout,
                )
            }
        }
    }
}

@Composable
private fun WideWorkoutContent(
    profile: Profile,
    plan: WorkoutPlan,
    today: LocalDate,
    onStartWorkout: (WorkoutPlan) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 240.dp, max = 300.dp)
                .fillMaxHeight(),
        ) {
            Text(
                text = "SÉANCE DU JOUR · ${formatWeekday(today).uppercase(Locale.FRENCH)}",
                style = MaterialTheme.typography.labelSmall,
                color = FitHubLime,
                letterSpacing = 1.6.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = plan.title,
                style = MaterialTheme.typography.headlineLarge,
                color = FitHubText,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodyMedium,
                color = FitHubTextMuted,
            )
            Spacer(modifier = Modifier.height(18.dp))
            DurationBadge(seconds = plan.totalDurationSeconds)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${plan.steps.size} blocs · Profil ${profile.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = FitHubTextMuted,
            )

            Spacer(modifier = Modifier.weight(1f))

            StartWorkoutButton(
                plan = plan,
                onStartWorkout = onStartWorkout,
            )
        }

        WorkoutStepsList(
            plan = plan,
            profile = profile,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun CompactWorkoutContent(
    profile: Profile,
    plan: WorkoutPlan,
    today: LocalDate,
    onStartWorkout: (WorkoutPlan) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SÉANCE DU JOUR · ${formatWeekday(today).uppercase(Locale.FRENCH)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = FitHubLime,
                    letterSpacing = 1.6.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = FitHubText,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitHubTextMuted,
                )
            }
            DurationBadge(seconds = plan.totalDurationSeconds)
        }

        Spacer(modifier = Modifier.height(14.dp))

        WorkoutStepsList(
            plan = plan,
            profile = profile,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.height(14.dp))

        StartWorkoutButton(
            plan = plan,
            onStartWorkout = onStartWorkout,
        )
    }
}

@Composable
private fun WorkoutStepsList(
    plan: WorkoutPlan,
    profile: Profile,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = plan.steps,
            key = { index, step -> "$index-${step.title}" },
        ) { index, step ->
            WorkoutStepCard(
                index = index,
                step = step,
                profile = profile,
            )
        }
    }
}

@Composable
private fun StartWorkoutButton(
    plan: WorkoutPlan,
    onStartWorkout: (WorkoutPlan) -> Unit,
) {
    Button(
        onClick = { onStartWorkout(plan) },
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FitHubLime,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(
            text = "Démarrer l'entraînement",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DurationBadge(seconds: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FitHubLime,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatDuration(seconds),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "DURÉE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
            )
        }
    }
}


