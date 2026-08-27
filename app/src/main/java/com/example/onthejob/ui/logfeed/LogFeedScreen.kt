package com.example.onthejob.ui.logfeed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onthejob.data.entry.Entry
import com.example.onthejob.ui.components.EditGoalDialog
import com.example.onthejob.ui.components.EntryCard
import com.example.onthejob.ui.components.HoursCard
import com.example.onthejob.ui.theme.*
import com.example.onthejob.data.entry.effectiveLocalDate
import java.time.format.TextStyle
import java.util.Locale

private fun stampParts(entry: Entry): Pair<String, String> {
    val date = entry.effectiveLocalDate ?: return "—" to "-"
    return date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase() to date.dayOfMonth.toString()
}

@Composable
fun LogFeedScreen(
    onNewEntry: () -> Unit,
    onOpenEntry: (String) -> Unit,
    activeInstance: com.example.onthejob.data.ojt.OjtInstance?,
    instances: List<com.example.onthejob.data.ojt.OjtInstance>,
    onSelectInstance: (String) -> Unit,
    onCreateInstance: (String, Double) -> Unit,
    onUpdateInstanceTarget: (Double) -> Unit,
    viewModel: LogFeedViewModel = viewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    val hoursRendered by viewModel.hoursRendered.collectAsState()
    val hoursRequired = activeInstance?.hoursRequired ?: 486.0

    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showNewOjtDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Paper)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "ON THE JOB",
                    fontFamily = DisplayFontFamily,
                    fontSize = 15.sp,
                    color = Ink,
                )
                com.example.onthejob.ui.components.OjtInstancePicker(
                    activeInstance = activeInstance,
                    instances = instances,
                    onSelectInstance = onSelectInstance,
                    onNewInstanceClick = { showNewOjtDialog = true },
                )
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                HoursCard(
                    hoursRendered = hoursRendered,
                    hoursRequired = hoursRequired,
                    onEditGoal = { showEditGoalDialog = true },
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "LOG",
                        fontFamily = CondFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                        color = Muted,
                    )
                    Text(
                        "${entries.size} entries",
                        fontFamily = CondFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                        color = Muted,
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No entries yet — tap + to log your first day.",
                            fontFamily = BodyFontFamily,
                            fontSize = 12.sp,
                            color = Muted,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(entries, key = { it.id }) { entry ->
                            val (month, day) = stampParts(entry)
                            EntryCard(
                                month = month,
                                day = day,
                                description = entry.text,
                                hours = entry.hours,
                                formattingStatus = entry.formattingStatus,
                                imageUrls = entry.imageUrls,
                                onClick = { onOpenEntry(entry.id) },
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Amber)
                .clickable(onClick = onNewEntry),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New entry", tint = Ink)
        }
    }

    if (showEditGoalDialog) {
        EditGoalDialog(
            currentGoal = hoursRequired,
            onDismiss = { showEditGoalDialog = false },
            onConfirm = { newGoal ->
                onUpdateInstanceTarget(newGoal)
                showEditGoalDialog = false
            },
        )
    }

    if (showNewOjtDialog) {
        com.example.onthejob.ui.components.NewOjtDialog(
            onDismiss = { showNewOjtDialog = false },
            onConfirm = { name, target ->
                onCreateInstance(name, target)
                showNewOjtDialog = false
            },
        )
    }
}