package com.example.onthejob.ui.newentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import com.example.onthejob.data.upload.PhotoUploadState
import com.example.onthejob.data.upload.PickedPhoto
import com.example.onthejob.ui.components.PrimaryButton
import com.example.onthejob.ui.components.dashedBorder
import com.example.onthejob.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NewEntryScreen(onBack: () -> Unit, viewModel: NewEntryViewModel = viewModel()) {
    val photos by viewModel.photos.collectAsState()
    val description by viewModel.description.collectAsState()
    val hours by viewModel.hours.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15),
    ) { uris -> if (uris.isNotEmpty()) viewModel.onPhotosPicked(uris) }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved) {
            onBack()
        }
    }

    val entryDate by viewModel.entryDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text(
                "New entry",
                fontFamily = CondFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Ink,
            )
            Spacer(Modifier.weight(1f))
            Text(
                entryDate.format(dateFormatter).uppercase(),
                fontFamily = MonoFontFamily,
                fontSize = 10.sp,
                color = Muted,
                modifier = Modifier.clickable { showDatePicker = true },
            )

            if (showDatePicker) {
                EntryDatePickerDialog(
                    initialDate = entryDate,
                    onDismiss = { showDatePicker = false },
                    onConfirm = { viewModel.onEntryDateChanged(it) },
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            FieldLabel("Photos · ${photos.size} / 15")
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    AddTile(onClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    })
                }
                items(photos, key = { it.uri.toString() }) { photo ->
                    PhotoTile(
                        photo = photo,
                        onRetry = { viewModel.retry(photo.uri) },
                        onRemove = { viewModel.removePhoto(photo.uri) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            FieldLabel("Description")
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::onDescriptionChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What did you do today?", color = Muted) },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = Ink,
                    unfocusedBorderColor = Line,
                    focusedTextColor = Ink2,
                    unfocusedTextColor = Ink2,
                ),
            )

            var hoursInput by remember { mutableStateOf("") }
            var minutesInput by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                val h = hours.toInt()
                val m = kotlin.math.round((hours - h) * 60).toInt()
                hoursInput = if (h > 0 || (h == 0 && m == 0)) h.toString() else "0"
                minutesInput = if (m > 0) m.toString() else ""
            }

            fun updateHours(hStr: String, mStr: String) {
                val hVal = hStr.toDoubleOrNull() ?: 0.0
                val mVal = mStr.toDoubleOrNull() ?: 0.0
                val totalHours = hVal + (mVal / 60.0)
                viewModel.onHoursChanged(totalHours)
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("Hours today")
                    Spacer(Modifier.height(7.dp))
                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                hoursInput = input
                                updateHours(input, minutesInput)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0", color = Muted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = MonoFontFamily, fontSize = 16.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface,
                            focusedBorderColor = Ink,
                            unfocusedBorderColor = Line,
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink,
                        ),
                        suffix = { Text("hrs", fontFamily = MonoFontFamily, fontSize = 12.sp, color = Muted) },
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("Minutes")
                    Spacer(Modifier.height(7.dp))
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d{0,3}$"""))) {
                                minutesInput = input
                                updateHours(hoursInput, input)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0", color = Muted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = MonoFontFamily, fontSize = 16.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface,
                            focusedBorderColor = Ink,
                            unfocusedBorderColor = Line,
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink,
                        ),
                        suffix = { Text("mins", fontFamily = MonoFontFamily, fontSize = 12.sp, color = Muted) },
                    )
                }
            }

            val hVal = hoursInput.toDoubleOrNull() ?: 0.0
            val mVal = minutesInput.toDoubleOrNull() ?: 0.0
            val totalVal = hVal + (mVal / 60.0)
            if (totalVal > 0) {
                Spacer(Modifier.height(4.dp))
                val hInt = totalVal.toInt()
                val mInt = kotlin.math.round((totalVal - hInt) * 60).toInt()
                val totalFormatted = String.format(java.util.Locale.US, "%.2f", totalVal).trimEnd('0').trimEnd('.')
                val displaySummary = buildString {
                    if (hInt > 0) append("$hInt hr${if (hInt > 1) "s" else ""}")
                    if (mInt > 0) {
                        if (hInt > 0) append(" ")
                        append("$mInt min${if (mInt > 1) "s" else ""}")
                    }
                    if (mInt > 0 || (hInt > 0 && totalFormatted != hInt.toString())) {
                        append(" ($totalFormatted total hrs)")
                    }
                }
                Text(
                    text = displaySummary,
                    fontFamily = MonoFontFamily,
                    fontSize = 11.sp,
                    color = Muted,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }

            if (saveState is SaveState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (saveState as SaveState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = BodyFontFamily,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        Box(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(
                text = "Save entry",
                onClick = { viewModel.saveEntry() },
                enabled = saveState !is SaveState.Saving,
                loading = saveState is SaveState.Saving,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = CondFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
        color = Muted,
    )
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .dashedBorder(color = Muted, cornerRadius = 8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add photo", tint = Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun PhotoTile(photo: PickedPhoto, onRetry: () -> Unit, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        when (val state = photo.state) {
            is PhotoUploadState.Uploading -> {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = Amber,
                )
            }
            is PhotoUploadState.Failed -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Text("Retry", color = Color.White, fontFamily = CondFontFamily, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            is PhotoUploadState.Success -> {}
            PhotoUploadState.Pending -> {}
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(18.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Text("×", color = Color.White, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}