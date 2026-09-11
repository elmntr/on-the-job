package com.example.onthejob.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onthejob.data.upload.photoPreviewUrl
import coil3.compose.AsyncImage
import com.example.onthejob.ui.theme.*
import androidx.compose.foundation.clickable

@Composable
fun EntryCard(
    month: String,
    day: String,
    description: String,
    hours: Double,
    formattingStatus: String,
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val hoursFormatted = String.format(java.util.Locale.US, "%.2f", hours).trimEnd('0').trimEnd('.')
    val hInt = hours.toInt()
    val mInt = kotlin.math.round((hours - hInt) * 60).toInt()
    val hoursText = if (mInt > 0) "$hoursFormatted hrs (${hInt}h ${mInt}m)" else "$hoursFormatted hrs"
    val thumbnailCount = imageUrls.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 9.dp) // room for the stamp to overhang the top edge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .background(CardSurface, RoundedCornerShape(14.dp))
                .border(BorderStroke(0.5.dp, Line), RoundedCornerShape(14.dp))
                .padding(start = 56.dp, top = 13.dp, end = 13.dp, bottom = 11.dp),
        ) {
            Text(
                text = description,
                fontFamily = BodyFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Ink2,
            )
            Spacer(Modifier.height(7.dp))
            if (thumbnailCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    // First tile: the entry's actual first photo, if we have one.
                    if (imageUrls.isNotEmpty()) {
                        AsyncImage(
                            model = photoPreviewUrl(imageUrls[0]),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Amber.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                        )
                    }
                    // Second tile: still a placeholder block (not wired to imageUrls[1] yet).
                    // Only rendered when there are >= 2 photos, matching the original
                    // "show up to 2 tiles, then +N overflow" layout.
                    repeat(minOf(thumbnailCount, 2) - 1) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(Amber.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                        )
                    }
                    if (thumbnailCount > 2) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(Paper2, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "+${thumbnailCount - 2}",
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                color = Muted,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(formattingStatus)
                Text(
                    hoursText,
                    fontFamily = MonoFontFamily,
                    fontSize = 10.sp,
                    color = Muted,
                )
            }
        }
        StampBadge(
            month = month,
            day = day,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 11.dp, y = (-9).dp),
        )
    }
}