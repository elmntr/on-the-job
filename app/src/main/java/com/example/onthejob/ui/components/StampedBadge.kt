package com.example.onthejob.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onthejob.ui.theme.CardSurface
import com.example.onthejob.ui.theme.CondFontFamily
import com.example.onthejob.ui.theme.Ink

/**
 * The recurring "rubber stamp" motif from DESIGN.md — every date-related
 * display should use this, not a second date style (see DESIGN.md principles).
 *
 * @param month 3-letter uppercase month, e.g. "JUL"
 * @param day day of month, e.g. "18"
 */
@Composable
fun StampBadge(month: String, day: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp)
            .rotate(-8f)
            .background(CardSurface, CircleShape)
            .border(BorderStroke(2.dp, Ink), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = month.uppercase(),
                fontFamily = CondFontFamily,
                fontWeight = MaterialTheme.typography.labelSmall.fontWeight,
                fontSize = 8.sp,
                letterSpacing = 0.5.sp,
                color = Ink,
                lineHeight = 9.sp,
            )
            Text(
                text = day,
                fontFamily = CondFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Ink,
                lineHeight = 16.sp,
            )
        }
    }
}