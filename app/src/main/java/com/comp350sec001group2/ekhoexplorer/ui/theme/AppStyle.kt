package com.comp350sec001group2.ekhoexplorer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppStyle {
    val colors: AppColors
        @Composable
        get() {
            val scheme = MaterialTheme.colorScheme

            return AppColors(
                background = scheme.background,
                surface = scheme.surface,
                textPrimary = scheme.onSurface,
                textSecondary = scheme.onSurfaceVariant,
                accent = scheme.primary,
                onAccent = scheme.onPrimary,
                divider = scheme.outline,
                upvote = AppExtendedColors.upvote,
                downvote = AppExtendedColors.downvote,
                dotActive = AppExtendedColors.dotActive,
                dotInactive = AppExtendedColors.dotInactive
            )
        }

    val type: AppTypography
        @Composable
        get() {
            val typography = MaterialTheme.typography

            return AppTypography(
                username = typography.titleSmall,
                caption = typography.bodyMedium,
                captionAction = typography.labelLarge,
                count = typography.labelLarge,
                searchText = SearchTextStyle,
                searchHint = SearchTextStyle,
                navLabel = typography.labelMedium,
                logo = LogoTextStyle,
            )
        }

    val dimens = AppDimens()
}

data class AppColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val upvote: Color,
    val downvote: Color,
    val divider: Color,
    val dotActive: Color,
    val dotInactive: Color,
)

// Compatibility Layer - Each property forwards to shared style
data class AppTypography(
    val username: TextStyle,
    val caption: TextStyle,
    val captionAction: TextStyle,
    val count: TextStyle,
    val searchText: TextStyle,
    val searchHint: TextStyle,
    val navLabel: TextStyle,
    val logo: TextStyle,
)
// Spacing and Radii forward to Spacing.kt and Shape.kt
// Left established constants - these belong to specific components, remain in AppStyle.kt
data class AppDimens(
    // Spacing
    val xs: Dp = Spacing.xs,
    val sm: Dp = Spacing.sm,
    val md: Dp = Spacing.md,
    val lg: Dp = Spacing.lg,
    val xl: Dp = Spacing.xl,
    // Shape
    val radiusSm: Dp = RadiusSmall,
    val radiusMd: Dp = RadiusMedium,
    val pill: Dp = RadiusPill,
    // Specific Components
    val avatar: Dp = 40.dp,
    val logo: Dp = 40.dp,
    val searchHeight: Dp = 44.dp,
    val icon: Dp = 22.dp,
    val touchTarget: Dp = 48.dp,
    val dot: Dp = 6.dp,
    val bottomBar: Dp = 80.dp,
)
