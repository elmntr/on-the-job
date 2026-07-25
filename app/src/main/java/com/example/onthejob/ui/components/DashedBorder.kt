package com.example.onthejob.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 8.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 4.dp,
): Modifier = this.drawWithContent {
    drawContent()
    drawRoundRect(
        color = color,
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashLength.toPx(), gapLength.toPx()),
                phase = 0f,
            ),
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
    )
}