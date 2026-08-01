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
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
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
    val isValid = parsed != null && parsed > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "EDIT GOAL HOURS",
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
                    text = "REQUIRED HOURS",
                    fontFamily = CondFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(7.dp))
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
                    isError = hoursText.isNotEmpty() && !isValid,
                )
                if (hoursText.isNotEmpty() && !isValid) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Please enter a positive number",
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
                        onConfirm(parsed)
                    }
                },
                enabled = isValid,
            ) {
                Text(
                    text = "SAVE",
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
