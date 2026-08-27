package com.example.onthejob.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onthejob.ui.theme.*

@Composable
fun NewOjtDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, hoursRequired: Double) -> Unit,
) {
    var nameText by remember { mutableStateOf("") }
    var hoursText by remember { mutableStateOf("486") }

    val parsedHours = hoursText.toDoubleOrNull()
    val isValid = nameText.isNotBlank() && parsedHours != null && parsedHours > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "NEW OJT INSTANCE",
                fontFamily = CondFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp,
                color = Ink,
            )
        },
        text = {
            Column {
                Text(
                    text = "INSTANCE NAME / LABEL",
                    fontFamily = CondFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Acme Corp or Term 2", color = Muted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontFamily = BodyFontFamily, fontSize = 14.sp, color = Ink),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = Ink,
                        unfocusedBorderColor = Line,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                    ),
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "REQUIRED HOURS TARGET",
                    fontFamily = CondFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
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
                    suffix = { Text("hrs", fontFamily = MonoFontFamily, fontSize = 12.sp, color = Muted) },
                    isError = hoursText.isNotEmpty() && (parsedHours == null || parsedHours <= 0),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid && parsedHours != null) {
                        onConfirm(nameText.trim(), parsedHours)
                    }
                },
                enabled = isValid,
            ) {
                Text(
                    text = "CREATE",
                    fontFamily = CondFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isValid) Ink else Muted,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
