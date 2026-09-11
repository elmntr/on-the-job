package com.example.onthejob.ui.entrydetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.onthejob.data.upload.photoPreviewUrl
import coil3.compose.AsyncImage
import com.example.onthejob.data.upload.PhotoUploadState
import com.example.onthejob.data.upload.PickedPhoto
import com.example.onthejob.data.entry.effectiveLocalDate
import com.example.onthejob.ui.components.GhostButton
import com.example.onthejob.ui.components.PrimaryButton
import com.example.onthejob.ui.components.StatusChip
import com.example.onthejob.ui.components.dashedBorder
import com.example.onthejob.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private val headerDateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

@Composable
fun EntryDetailScreen(
    entryId: String,
    onBack: () -> Unit,
    onOpenPhotoViewer: (imageUrls: List<String>, startIndex: Int) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: EntryDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                EntryDetailViewModel(
                    application = context.applicationContext as android.app.Application,
                    entryId = entryId,
                )
            }
        },
    )

    val entry by viewModel.entry.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val editedText by viewModel.editedText.collectAsState()
    val editedHours by viewModel.editedHours.collectAsState()
    val existingPhotoUrls by viewModel.existingPhotoUrls.collectAsState()
    val newPhotos by viewModel.newPhotos.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val regenerateState by viewModel.regenerateState.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15),
    ) { uris -> if (uris.isNotEmpty()) viewModel.onPhotosPicked(uris) }

    LaunchedEffect(updateState) {
        if (updateState is EntryUpdateState.Saved) {
            viewModel.resetUpdateState()
        }
    }

    val current = entry

    Column(modifier = Modifier.fillMaxSize().background(Paper)) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (isEditing) viewModel.cancelEditing() else onBack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text(
                text = if (isEditing) "Edit entry" else current?.let {
                    it.effectiveLocalDate?.let { date -> "${date.format(headerDateFormat)} entry" } ?: "Entry"
                } ?: "Entry",
                fontFamily = CondFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Ink,
            )
            Spacer(Modifier.weight(1f))
            if (!isEditing && current != null) {
                val h = current.hours.toInt()
                val m = kotlin.math.round((current.hours - h) * 60).toInt()
                val dec = String.format(java.util.Locale.US, "%.2f", current.hours).trimEnd('0').trimEnd('.')
                val displayText = if (m > 0) "$dec hrs (${h}h ${m}m)" else "$dec hrs"
                Text(displayText, fontFamily = MonoFontFamily, fontSize = 10.sp, color = Muted)
            }
        }

        if (current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", fontFamily = BodyFontFamily, fontSize = 12.sp, color = Muted)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (!isEditing) {
                // ---------- VIEW MODE ----------
                if (current.imageUrls.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        userScrollEnabled = false,
                    ) {
                        itemsIndexed(current.imageUrls, key = { _, url -> url }) { index, url ->
                            RemotePhotoTile(
                                url = url,
                                onRemove = null,
                                onClick = { onOpenPhotoViewer(current.imageUrls, index) },
                            )
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                }

                FieldLabel(if (current.formattingStatus == "done") "Polished entry" else "Entry (unformatted)")
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface, RoundedCornerShape(12.dp))
                        .padding(11.dp),
                ) {
                    Text(current.text, fontFamily = BodyFontFamily, fontSize = 12.sp, color = Ink2, lineHeight = 18.sp)
                }

                Spacer(Modifier.height(12.dp))
                StatusChip(current.formattingStatus)

                Spacer(Modifier.height(16.dp))
                GhostButton(text = "Edit entry", onClick = { viewModel.startEditing() })
                Spacer(Modifier.height(16.dp))
            } else {
                // ---------- EDIT MODE ----------
                val totalPhotoCount = existingPhotoUrls.size + newPhotos.size
                FieldLabel("Photos · $totalPhotoCount / 15")
                Spacer(Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    userScrollEnabled = false,
                ) {
                    if (totalPhotoCount < 15) {
                        item {
                            AddTile(onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            })
                        }
                    }
                    items(existingPhotoUrls, key = { "existing:$it" }) { url ->
                        RemotePhotoTile(url = url, onRemove = { viewModel.removeExistingPhoto(url) })
                    }
                    items(newPhotos, key = { "new:${it.uri}" }) { photo ->
                        LocalPhotoTile(
                            photo = photo,
                            onRetry = { viewModel.retryNewPhoto(photo.uri) },
                            onRemove = { viewModel.removeNewPhoto(photo.uri) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                FieldLabel("Polished entry")
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = editedText,
                    onValueChange = viewModel::onTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 10,
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

                Spacer(Modifier.height(8.dp))
                GhostButton(
                    text = if (regenerateState is RegenerateState.Loading) "Regenerating…" else "Regenerate with AI",
                    onClick = { viewModel.regenerate() },
                )
                if (regenerateState is RegenerateState.Error) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = (regenerateState as RegenerateState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = BodyFontFamily,
                        fontSize = 11.sp,
                    )
                }

                var hoursInput by remember { mutableStateOf("") }
                var minutesInput by remember { mutableStateOf("") }

                LaunchedEffect(isEditing, editedHours) {
                    if (isEditing) {
                        val h = editedHours.toInt()
                        val m = kotlin.math.round((editedHours - h) * 60).toInt()
                        hoursInput = if (h > 0 || (h == 0 && m == 0)) h.toString() else "0"
                        minutesInput = if (m > 0) m.toString() else ""
                    }
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
                        FieldLabel("Hours")
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
                            textStyle = TextStyle(fontFamily = MonoFontFamily, fontSize = 16.sp),
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
                            textStyle = TextStyle(fontFamily = MonoFontFamily, fontSize = 16.sp),
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

                if (updateState is EntryUpdateState.Error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = (updateState as EntryUpdateState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = BodyFontFamily,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        if (isEditing) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GhostButton(
                    text = "Cancel",
                    onClick = { viewModel.cancelEditing() },
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Save",
                    onClick = { viewModel.save() },
                    enabled = updateState !is EntryUpdateState.Saving,
                    loading = updateState is EntryUpdateState.Saving,
                    modifier = Modifier.weight(1f),
                )
            }
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

/** Already-uploaded photo, referenced by URL. onRemove null = view-mode, no remove control. */
@Composable
private fun RemotePhotoTile(url: String, onRemove: (() -> Unit)?, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        AsyncImage(model = photoPreviewUrl(url), contentDescription = null, modifier = Modifier.fillMaxSize())
        if (onRemove != null) {
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
}

/** Newly picked photo this edit session — same upload-state handling as New Entry's PhotoTile. */
@Composable
private fun LocalPhotoTile(photo: PickedPhoto, onRetry: () -> Unit, onRemove: () -> Unit) {
    Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))) {
        AsyncImage(model = photo.uri, contentDescription = null, modifier = Modifier.fillMaxSize())
        when (val state = photo.state) {
            is PhotoUploadState.Uploading -> {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
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