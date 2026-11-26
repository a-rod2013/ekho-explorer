
package com.comp350sec001group2.ekhoexplorer.ui.pages.settings.views.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = AppStyle.colors.textSecondary,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = AppStyle.dimens.lg,
                bottom = AppStyle.dimens.sm
            )
    )
}
