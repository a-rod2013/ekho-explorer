package com.comp350sec001group2.ekhoexplorer.ui.theme


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Three Constants
val RadiusSmall = 8.dp
val RadiusMedium = 12.dp
val RadiusPill = 100.dp

// Initializes an object to handle shapes
val AppShapes = Shapes(
    small = RoundedCornerShape(RadiusSmall),
    medium = RoundedCornerShape(RadiusMedium),
)
