package com.netify.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NetifyTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
)
