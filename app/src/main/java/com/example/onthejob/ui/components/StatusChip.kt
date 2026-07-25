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
import com.example.onthejob.ui.theme.Success
import com.example.onthejob.ui.theme.SuccessBg

/**
 * Maps directly from Entry.formattingStatus. Per CLAUDE.md/DESIGN.md, the
 * student only ever sees two states here — "Polished" or "Formatting
 * pending" — failed_quota/failed_other collapse into "pending" at the UI
 * layer; raw failure reasons are never surfaced.
 */
@Composable
fun StatusChip(formattingStatus: String, modifier: Modifier = Modifier) {
    val isDone = formattingStatus == "done"
    val bg = if (isDone) SuccessBg else AmberDark.copy(alpha = 0.18f)
    val fg = if (isDone) Success else AmberDark
    val label = if (isDone) "Polished" else "Formatting pending"

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