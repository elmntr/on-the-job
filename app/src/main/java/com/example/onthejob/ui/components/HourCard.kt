package com.example.onthejob.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
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
fun HoursCard(
    hoursRendered: Double,
    hoursRequired: Double,
    modifier: Modifier = Modifier,
    onEditGoal: (() -> Unit)? = null,
) {
    val safeRequired = if (hoursRequired <= 0) 1.0 else hoursRequired
    val progress = (hoursRendered / safeRequired).toFloat().coerceIn(0f, 1f)
    val rendered = String.format(java.util.Locale.US, "%.2f", hoursRendered).trimEnd('0').trimEnd('.')
    val required = String.format(java.util.Locale.US, "%.2f", hoursRequired).trimEnd('0').trimEnd('.')

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Ink, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "HOURS RENDERED",
                fontFamily = CondFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = Paper.copy(alpha = 0.6f),
            )
            if (onEditGoal != null) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onEditGoal),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit goal hours",
                        tint = Paper.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = if (onEditGoal != null) Modifier.clickable(onClick = onEditGoal) else Modifier,
        ) {
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