package com.twingo.fithub.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.WorkoutMachine
import com.twingo.fithub.domain.model.WorkoutPlan
import com.twingo.fithub.domain.model.WorkoutStep
import com.twingo.fithub.domain.model.WorkoutStepKind
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
import java.time.DayOfWeek

@Composable
fun WorkoutEditorScreen(
    onSave: (WorkoutPlan) -> Unit,
    onCancel: () -> Unit,
    existingPlan: WorkoutPlan? = null,
    selectedProfileId: Long,
    modifier: Modifier = Modifier,
) {
    var plan by remember { mutableStateOf(existingPlan ?: createEmptyPlan(selectedProfileId)) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showMachinePicker by remember { mutableStateOf(false) }
    var showAddStepMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Int?>(null) }

    val isEditing = existingPlan != null

    // Validation
    val isValid = plan.title.isNotBlank() &&
        plan.steps.isNotEmpty() &&
        plan.steps.all { it.durationSeconds > 0 }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(FitHubBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Top bar
            EditorTopBar(
                title = if (isEditing) "MODIFIER LA SÉANCE" else "NOUVELLE SÉANCE",
                onCancel = onCancel,
                onSave = { onSave(plan) },
                enabled = isValid,
            )

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header section
                PlanHeaderSection(
                    plan = plan,
                    onTitleChange = { plan = plan.copy(title = it) },
                    onDescriptionChange = { plan = plan.copy(description = it) },
                    onDayChange = { day ->
                        plan = plan.copy(dayOfWeek = day)
                        showDayPicker = false
                    },
                    onMachineChange = { machine ->
                        plan = plan.copy(machine = machine)
                        showMachinePicker = false
                    },
                    onShowDayPicker = { showDayPicker = true },
                    onShowMachinePicker = { showMachinePicker = true },
                    showDayPicker = showDayPicker,
                    showMachinePicker = showMachinePicker,
                )

                // Steps list
                if (plan.steps.isEmpty()) {
                    EmptyStepsView(onAddStep = { showAddStepMenu = true })
                } else {
                    StepsListSection(
                        steps = plan.steps,
                        onReorder = { from, to ->
                            val newSteps = plan.steps.toMutableList()
                            val item = newSteps.removeAt(from)
                            newSteps.add(to, item)
                            plan = plan.copy(steps = newSteps)
                        },
                        onDelete = { index -> showDeleteConfirm = index },
                        onEdit = { index ->
                            showAddStepMenu = true
                        },
                        onAddStep = { showAddStepMenu = true },
                    )
                }

                // Add step floating button
                AddStepFab(onClick = { showAddStepMenu = true })
            }
        }

        // Simple dialogs using AlertDialog with confirmButton/dismissButton
        if (showDayPicker) {
            DayPickerDialog(onSelect = { day ->
                plan = plan.copy(dayOfWeek = day)
                showDayPicker = false
            }, onDismiss = { showDayPicker = false })
        }

        if (showMachinePicker) {
            MachinePickerDialog(onSelect = { machine ->
                plan = plan.copy(machine = machine)
                showMachinePicker = false
            }, onDismiss = { showMachinePicker = false })
        }

        if (showAddStepMenu) {
            AddStepMenuDialog(
                onDismiss = { showAddStepMenu = false },
                onAddStep = { kind ->
                    val newStep = createDefaultStep(kind, plan.steps.size)
                    plan = plan.copy(steps = plan.steps + newStep)
                    showAddStepMenu = false
                },
            )
        }

        if (showDeleteConfirm != null) {
            DeleteConfirmDialog(
                index = showDeleteConfirm!!,
                onDismiss = { showDeleteConfirm = null },
                onConfirm = {
                    val newSteps = plan.steps.toMutableList()
                    newSteps.removeAt(showDeleteConfirm!!)
                    plan = plan.copy(steps = newSteps)
                    showDeleteConfirm = null
                },
            )
        }
    }
}

private fun createEmptyPlan(profileId: Long): WorkoutPlan {
    return WorkoutPlan(
        id = 0L,
        profileId = profileId,
        dayOfWeek = DayOfWeek.MONDAY,
        title = "",
        description = "",
        steps = emptyList(),
        machine = WorkoutMachine.OUTDOOR,
    )
}

private fun createDefaultStep(kind: WorkoutStepKind, index: Int): WorkoutStep {
    return when (kind) {
        WorkoutStepKind.WARM_UP -> WorkoutStep(
            title = "Échauffement",
            kind = WorkoutStepKind.WARM_UP,
            durationSeconds = 10 * 60,
            zoneFrom = HeartRateZone.Z1,
            zoneTo = HeartRateZone.Z2,
            targetDetail = "Échauffement progressif",
            instruction = "Montée en puissance douce, prépare le corps à l'effort.",
        )
        WorkoutStepKind.INTERVAL -> WorkoutStep(
            title = "Intervalle",
            kind = WorkoutStepKind.INTERVAL,
            durationSeconds = 30,
            repetitions = 1,
            recoverySeconds = 0,
            zoneFrom = HeartRateZone.Z4,
            zoneTo = HeartRateZone.Z5,
            targetDetail = "Effort intense",
            instruction = "Bloc d'effort à haute intensité.",
        )
        WorkoutStepKind.ENDURANCE -> WorkoutStep(
            title = "Endurance",
            kind = WorkoutStepKind.ENDURANCE,
            durationSeconds = 20 * 60,
            zoneFrom = HeartRateZone.Z2,
            zoneTo = HeartRateZone.Z3,
            targetDetail = "Allure endurance",
            instruction = "Effort soutenu en zone aérobie.",
        )
        WorkoutStepKind.COOL_DOWN -> WorkoutStep(
            title = "Retour au calme",
            kind = WorkoutStepKind.COOL_DOWN,
            durationSeconds = 5 * 60,
            zoneFrom = HeartRateZone.Z1,
            targetDetail = "Récupération active",
            instruction = "Laisse redescendre la fréquence cardiaque.",
        )
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    enabled: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FitHubSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, FitHubOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) {
                Text(text = "ANNULER", color = FitHubTextMuted)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = FitHubText,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (enabled) FitHubLime else FitHubOutline,
                    contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else FitHubTextMuted,
                ),
                enabled = enabled,
            ) {
                Text(text = "SAUVEGARDER", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlanHeaderSection(
    plan: WorkoutPlan,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDayChange: (DayOfWeek) -> Unit,
    onMachineChange: (WorkoutMachine) -> Unit,
    onShowDayPicker: () -> Unit,
    onShowMachinePicker: () -> Unit,
    showDayPicker: Boolean,
    showMachinePicker: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Title
        TextField(
            value = plan.title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = "Titre de la séance", color = FitHubTextMuted) },
            singleLine = true,
        )

        // Description
        TextField(
            value = plan.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = "Description (optionnel)", color = FitHubTextMuted) },
            singleLine = true,
        )

        // Day & Machine pickers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PickerButton(
                label = "JOUR",
                value = formatDayOfWeek(plan.dayOfWeek),
                icon = Icons.Default.CalendarMonth,
                onClick = onShowDayPicker,
                expanded = showDayPicker,
            )
            PickerButton(
                label = "MACHINE",
                value = plan.machine.displayName,
                icon = Icons.Default.FitnessCenter,
                onClick = onShowMachinePicker,
                expanded = showMachinePicker,
            )
        }
    }
}

@Composable
private fun PickerButton(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    expanded: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) FitHubLime.copy(alpha = 0.12f) else FitHubBackground,
            contentColor = if (expanded) FitHubLime else FitHubText,
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (expanded) 2.dp else 1.dp,
            if (expanded) FitHubLime else FitHubOutline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (expanded) FitHubLime else FitHubTextMuted,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = FitHubTextMuted)
                Text(text = value, style = MaterialTheme.typography.titleMedium, color = if (expanded) FitHubLime else FitHubText)
            }
        }
    }
}

@Composable
private fun EmptyStepsView(onAddStep: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onAddStep() },
        colors = CardDefaults.cardColors(containerColor = FitHubBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, FitHubOutline),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = FitHubLime,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "AJOUTER UNE ÉTAPE",
                    style = MaterialTheme.typography.titleMedium,
                    color = FitHubLime,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Appuyez pour commencer à construire votre séance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitHubTextMuted,
                )
            }
        }
    }
}

@Composable
private fun StepsListSection(
    steps: List<WorkoutStep>,
    onReorder: (Int, Int) -> Unit,
    onDelete: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onAddStep: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "ÉTAPES (${steps.size})", style = MaterialTheme.typography.labelSmall, color = FitHubLime, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Glisser pour réordonner", style = MaterialTheme.typography.bodySmall, color = FitHubTextMuted)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(items = steps, key = { index, step -> "$index-${step.title}" }) { index, step ->
                StepEditorRow(
                    index = index,
                    step = step,
                    isFirst = index == 0,
                    isLast = index == steps.size - 1,
                    onMoveUp = { if (index > 0) onReorder(index, index - 1) },
                    onMoveDown = { if (index < steps.size - 1) onReorder(index, index + 1) },
                    onDelete = { onDelete(index) },
                    onEdit = { onEdit(index) },
                )
            }
        }
    }
}

@Composable
private fun StepEditorRow(
    index: Int,
    step: WorkoutStep,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val kindColor = when (step.kind) {
        WorkoutStepKind.WARM_UP -> FitHubBlue
        WorkoutStepKind.INTERVAL -> FitHubOrange
        WorkoutStepKind.ENDURANCE -> FitHubLime
        WorkoutStepKind.COOL_DOWN -> FitHubTextMuted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FitHubSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, kindColor.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Déplacer",
                    tint = FitHubTextMuted,
                )
            }

            // Step info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(12.dp)
                            .background(kindColor, RoundedCornerShape(6.dp)),
                    )
                    Text(
                        text = "${index + 1}. ${step.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = FitHubText,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatDuration(step.totalDurationSeconds),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = kindColor,
                    )
                }
                Text(
                    text = step.targetDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = FitHubTextMuted,
                    maxLines = 1,
                )
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { if (!isFirst) onMoveUp() }) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Monter", tint = if (isFirst) FitHubTextMuted.copy(alpha = 0.3f) else FitHubText)
                }
                IconButton(onClick = { if (!isLast) onMoveDown() }) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre", tint = if (isLast) FitHubTextMuted.copy(alpha = 0.3f) else FitHubText)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = FitHubLime)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = FitHubError)
                }
            }
        }
    }
}

@Composable
private fun AddStepFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        androidx.compose.material3.FloatingActionButton(
            onClick = onClick,
            containerColor = FitHubLime,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter une étape", modifier = Modifier.size(28.dp))
        }
    }
}

// Simplified dialogs using AlertDialog with confirmButton/dismissButton
@Composable
private fun DayPickerDialog(onSelect: (DayOfWeek) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "FERMER", color = FitHubTextMuted) }
        },
        title = {
            Column {
                Text(text = "CHOISIR LE JOUR", style = MaterialTheme.typography.labelSmall, color = FitHubLime, letterSpacing = 2.sp)
                Text(text = "Jour de la séance", style = MaterialTheme.typography.headlineSmall, color = FitHubText)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.values().forEach { day ->
                    Text(text = formatDayOfWeek(day), modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clickable { onSelect(day) }
                        .background(FitHubBackground),
                        style = MaterialTheme.typography.titleMedium,
                        color = FitHubText
                    )
                }
            }
        },
    )
}

@Composable
private fun MachinePickerDialog(onSelect: (WorkoutMachine) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "FERMER", color = FitHubTextMuted) }
        },
        title = {
            Column {
                Text(text = "TYPE DE MACHINE", style = MaterialTheme.typography.labelSmall, color = FitHubLime, letterSpacing = 2.sp)
                Text(text = "Équipement cible", style = MaterialTheme.typography.headlineSmall, color = FitHubText)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutMachine.values().forEach { machine ->
                    Text(text = machine.displayName, modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clickable { onSelect(machine) }
                        .background(FitHubBackground),
                        style = MaterialTheme.typography.titleMedium,
                        color = FitHubText
                    )
                }
            }
        },
    )
}

@Composable
private fun AddStepMenuDialog(
    onDismiss: () -> Unit,
    onAddStep: (WorkoutStepKind) -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "ANNULER", color = FitHubTextMuted) }
        },
        title = {
            Column {
                Text(text = "AJOUTER UNE ÉTAPE", style = MaterialTheme.typography.labelSmall, color = FitHubLime, letterSpacing = 2.sp)
                Text(text = "Choisir le type", style = MaterialTheme.typography.headlineSmall, color = FitHubText)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    WorkoutStepKind.WARM_UP to "Échauffement",
                    WorkoutStepKind.ENDURANCE to "Bloc endurance",
                    WorkoutStepKind.INTERVAL to "Intervalle simple",
                    WorkoutStepKind.COOL_DOWN to "Retour au calme",
                ).forEach { (kind, label) ->
                    val color = when (kind) {
                        WorkoutStepKind.WARM_UP -> FitHubBlue
                        WorkoutStepKind.INTERVAL -> FitHubOrange
                        WorkoutStepKind.ENDURANCE -> FitHubLime
                        WorkoutStepKind.COOL_DOWN -> FitHubTextMuted
                    }
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .clickable { onAddStep(kind) }
                            .background(FitHubBackground),
                        style = MaterialTheme.typography.titleMedium,
                        color = FitHubText
                    )
                }
            }
        },
    )
}

@Composable
private fun DeleteConfirmDialog(
    index: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = FitHubError, contentColor = MaterialTheme.colorScheme.onError)) {
                Text(text = "SUPPRIMER")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "ANNULER", color = FitHubTextMuted) }
        },
        title = {
            Column {
                Text(text = "SUPPRIMER L'ÉTAPE", style = MaterialTheme.typography.labelSmall, color = FitHubError, letterSpacing = 2.sp)
                Text(text = "Étape ${index + 1}", style = MaterialTheme.typography.headlineSmall, color = FitHubText)
            }
        },
        text = {
            Text(text = "Cette action est irréversible. L'étape sera supprimée définitivement.", style = MaterialTheme.typography.bodyMedium, color = FitHubTextMuted)
        },
    )
}

private fun formatDayOfWeek(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "LUNDI"
    DayOfWeek.TUESDAY -> "MARDI"
    DayOfWeek.WEDNESDAY -> "MERCREDI"
    DayOfWeek.THURSDAY -> "JEUDI"
    DayOfWeek.FRIDAY -> "VENDREDI"
    DayOfWeek.SATURDAY -> "SAMEDI"
    DayOfWeek.SUNDAY -> "DIMANCHE"
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%dh %02dmin", h, m) else String.format("%dmin %02ds", m, s)
}