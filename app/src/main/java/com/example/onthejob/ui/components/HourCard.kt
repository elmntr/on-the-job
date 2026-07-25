package com.example.onthejob.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onthejob.ui.theme.Amber
import com.example.onthejob.ui.theme.CondFontFamily
import com.example.onthejob.ui.theme.Ink
import com.example.onthejob.ui.theme.MonoFontFamily
import com.example.onthejob.ui.theme.Paper

@Composable
fun HoursCard(hoursRendered: Double, hoursRequired: Double, modifier: Modifier = Modifier) {
    val progress = (hoursRendered / hoursRequired).toFloat().coerceIn(0f, 1f)
    val rendered = if (hoursRendered == hoursRendered.toLong().toDouble()) hoursRendered.toLong().toString() else hoursRendered.toString()
    val required = if (hoursRequired == hoursRequired.toLong().toDouble()) hoursRequired.toLong().toString() else hoursRequired.toString()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Ink, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            "HOURS RENDERED",
            fontFamily = CondFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = Paper.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(rendered, fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, color = Paper)
            Text(" / $required hrs", fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Paper.copy(alpha = 0.5f))
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Paper.copy(alpha = 0.18f), RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Amber, RoundedCornerShape(4.dp)),
            )
        }
    }
}