package com.comp350sec001group2.ekhoexplorer.ui.pages.activity.views

import androidx.compose.runtime.Composable

class ActivitySection(
    val id: String,
    val title: String,
    val content: @Composable () -> Unit,
)
