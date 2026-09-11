package com.example.onthejob.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onthejob.ui.theme.AmberDark
import com.example.onthejob.ui.theme.MonoFontFamily
import com.example.onthejob.ui.theme.Muted
import com.example.onthejob.ui.theme.Success
import com.example.onthejob.ui.theme.SuccessBg

/**
 * Maps directly from Entry.formattingStatus. Per DESIGN.md, the student
 * only ever sees human-friendly states — raw failure reasons and technical
 * status values are never surfaced:
 *   "done"    → green "Polished" chip
 *   "skipped" → neutral "Raw entry" chip (user intentionally skipped AI)
 *   anything else → amber "Formatting pending" chip
 */
@Composable
fun StatusChip(formattingStatus: String, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (formattingStatus) {
        "done" -> Triple(SuccessBg, Success, "Polished")
        "skipped" -> Triple(
            Muted.copy(alpha = 0.12f),
            Muted,
            "Raw entry",
        )
        else -> Triple(AmberDark.copy(alpha = 0.18f), AmberDark, "Formatting pending")
    }

    Text(
        text = label,
        modifier = modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = fg,
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.sp,
    )
}