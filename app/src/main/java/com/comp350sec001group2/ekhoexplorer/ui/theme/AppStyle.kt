package com.comp350sec001group2.ekhoexplorer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppStyle {
    val colors = AppColors()
    val type = AppTypography()
    val dimens = AppDimens()
}

data class AppColors(
    val background: Color = Color(0xFFFFFFFF),
    val surface: Color = Color(0xFFF1F3F1),
    val textPrimary: Color = Color(0xFF121512),
    val textSecondary: Color = Color(0xFF5E665E),
    val accent: Color = Color(0xFF2E9E4F),
    val onAccent: Color = Color(0xFFFFFFFF),
    val upvote: Color = Color(0xFF2E9E4F),
    val downvote: Color = Color(0xFFD9534F),
    val divider: Color = Color(0xFFE1E5E1),
    val dotActive: Color = Color(0xFFFFFFFF),
    val dotInactive: Color = Color(0x80FFFFFF),
)

data class AppTypography(
    val username: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    val caption: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    val captionAction: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    val count: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    val searchText: TextStyle = TextStyle(fontSize = 15.sp),
    val searchHint: TextStyle = TextStyle(fontSize = 15.sp),
    val navLabel: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    val logo: TextStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
)

data class AppDimens(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val radiusSm: Dp = 8.dp,
    val radiusMd: Dp = 12.dp,
    val pill: Dp = 100.dp,
    val avatar: Dp = 40.dp,
    val logo: Dp = 40.dp,
    val searchHeight: Dp = 44.dp,
    val icon: Dp = 22.dp,
    val touchTarget: Dp = 48.dp,
    val dot: Dp = 6.dp,
    val bottomBar: Dp = 64.dp,
)
