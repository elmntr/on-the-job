package com.example.onthejob.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onthejob.data.entry.DayStatus
import com.example.onthejob.data.entry.dayStatus
import com.example.onthejob.data.entry.localDate
import com.example.onthejob.ui.components.EntryCard
import com.example.onthejob.ui.theme.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
    onOpenEntry: (String) -> Unit = {},
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val monthGrid by viewModel.monthGrid.collectAsState()
    val entriesByDate by viewModel.entriesByDate.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDayEntries by viewModel.selectedDayEntries.collectAsState()
    val (daysLogged, daysElapsed) = viewModel.monthSummary.collectAsState().value

    Column(modifier = Modifier.fillMaxSize().background(Paper)) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.goToPreviousMonth() }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month", tint = Ink)
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                fontFamily = CondFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$daysLogged / $daysElapsed days",
                fontFamily = MonoFontFamily,
                fontSize = 10.sp,
                color = Muted,
            )
            IconButton(onClick = { viewModel.goToNextMonth() }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month", tint = Ink)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { dow ->
                    Text(
                        text = dow,
                        fontFamily = MonoFontFamily,
                        fontSize = 9.sp,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                    )
                }
            }

            monthGrid.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            status = dayStatus(date?.let { entriesByDate[it] }),
                            isSelected = date == selectedDate,
                            isToday = date == LocalDate.now(),
                            onClick = { date?.let { viewModel.selectDate(it) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LegendItem(color = Success, label = "Polished")
                LegendItem(color = Amber, label = "Pending")
                LegendItem(color = Line, label = "No entry")
            }

            Text(
                text = "Selected · ${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${selectedDate.dayOfMonth}",
                fontFamily = CondFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.1.sp,
                color = Muted,
                modifier = Modifier.padding(bottom = 7.dp),
            )

            if (selectedDayEntries.isEmpty()) {
                Text(
                    text = "No entry logged this day.",
                    fontFamily = BodyFontFamily,
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            } else {
                selectedDayEntries.forEach { entry ->
                    val date = entry.localDate ?: selectedDate
                    EntryCard(
                        month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                        day = date.dayOfMonth.toString(),
                        description = entry.text,
                        hours = entry.hours,
                        formattingStatus = entry.formattingStatus,
                        thumbnailCount = entry.imageUrls.size,
                        onClick = { onOpenEntry(entry.id) },
                        modifier = Modifier.padding(bottom = 11.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    status: DayStatus,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    date == null -> Color.Transparent
                    isSelected -> Amber.copy(alpha = 0.25f)
                    else -> CardSurface
                }
            )
            .let { if (isToday) it.border(1.5.dp, Ink, RoundedCornerShape(8.dp)) else it }
            .let { if (date != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontFamily = MonoFontFamily,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 11.sp,
                    color = Ink2,
                )
                if (status != DayStatus.NONE) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (status == DayStatus.DONE) Success else Amber),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text = label, fontSize = 10.sp, color = Muted, fontFamily = BodyFontFamily)
    }
}