package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun InfoMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppStyle.type.caption,
        color = AppStyle.colors.textSecondary,
        modifier = modifier.padding(AppStyle.dimens.lg),
    )
}
