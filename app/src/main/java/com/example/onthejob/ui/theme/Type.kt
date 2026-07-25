package com.example.onthejob.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.onthejob.R

// Archivo Black — used sparingly per DESIGN.md: app title, portfolio cover, big numbers on dark surfaces
val DisplayFontFamily = FontFamily(
    Font(R.font.archivo_black_regular, FontWeight.Black),
)

// Big Shoulders Text — section labels, screen titles, buttons: the "stamped label" feel
val CondFontFamily = FontFamily(
    Font(R.font.big_shoulders_medium, FontWeight.Medium),
    Font(R.font.big_shoulders_bold, FontWeight.Bold),
)

// IBM Plex Sans — body text, entry descriptions, general UI copy
val BodyFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
)

// IBM Plex Mono — numbers ONLY: hours, dates, clock time, stamp digits (DESIGN.md rule).
// Deliberately NOT wired into a Typography slot below — Material3's type scale is
// semantic (bodyLarge, labelSmall...), not content-aware, so there's no slot that
// means "this is a number." Apply MonoFontFamily directly at the call site instead, e.g.:
//   Text("342", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MonoFontFamily))
val MonoFontFamily = FontFamily(
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

val OnTheJobTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp),
    displayMedium = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 32.sp),

    headlineLarge = TextStyle(fontFamily = CondFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.2.sp),
    headlineMedium = TextStyle(fontFamily = CondFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp),

    titleLarge = TextStyle(fontFamily = CondFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp),
    titleMedium = TextStyle(fontFamily = CondFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp),
    titleSmall = TextStyle(fontFamily = CondFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),

    bodyLarge = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),

    labelLarge = TextStyle(fontFamily = CondFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontFamily = CondFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = CondFontFamily, fontWeight = FontWeight.Medium, fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 0.5.sp),
)