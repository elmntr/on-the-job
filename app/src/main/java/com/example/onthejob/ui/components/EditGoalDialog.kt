package com.example.onthejob.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.onthejob.util.NetworkStatus
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onthejob.ui.theme.*

@Composable
fun EditGoalDialog(
    currentGoal: Double,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: suspend (String, Double) -> Result<Unit>,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var name by remember(currentName) { mutableStateOf(currentName) }
    var hoursText by remember(currentGoal) {
        mutableStateOf(
            if (currentGoal == currentGoal.toLong().toDouble()) {
                currentGoal.toLong().toString()
            } else {
                currentGoal.toString()
            }
        )
    }

    val parsed = hoursText.toDoubleOrNull()
    val isValid = parsed != null && parsed.isFinite() && parsed > 0 && parsed <= 10000 && name.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = Paper,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "EDIT OJT INSTANCE",
                fontFamily = CondFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp,
                color = Ink,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    enabled = !saving,
                    value = name,
                    onValueChange = { if (it.length <= 100) name = it },
                    label = { Text("OJT instance name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "REQUIRED HOURS",
                    fontFamily = CondFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    enabled = !saving,
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontFamily = MonoFontFamily, fontSize = 16.sp, color = Ink),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = Ink,
                        unfocusedBorderColor = Line,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                    ),
                    isError = hoursText.isNotEmpty() && !isValid,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (hoursText.isNotEmpty() && !isValid) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Enter a name and required hours between 1 and 10,000",
                        fontFamily = BodyFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (parsed != null && isValid) {
                        if (!NetworkStatus.isOnline(context)) {
                            error = "Reconnect to save your OJT instance."
                        } else {
                            saving = true
                            error = null
                            scope.launch {
                                onConfirm(name.trim(), parsed).onFailure {
                                    error = it.message ?: "Could not save your OJT instance."
                                }
                                saving = false
                            }
                        }
                    }
                },
                enabled = isValid && !saving,
            ) {
                Text(
                    text = if (saving) "SAVING…" else "SAVE",
                    fontFamily = CondFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isValid) Ink else Muted,
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    fontFamily = CondFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Muted,
                )
            }
        },
    )
}
