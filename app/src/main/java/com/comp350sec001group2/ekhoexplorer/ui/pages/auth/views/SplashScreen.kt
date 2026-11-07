
package com.comp350sec001group2.ekhoexplorer.ui.pages.auth.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import androidx.compose.ui.tooling.preview.Preview
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppStyle.colors.background)
            .testTag("splash-screen"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppStyle.dimens.md),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = AppStyle.colors.accent,
            )

            Text(
                text = "Preparing your adventure...",
                color = AppStyle.colors.textSecondary,
                style = AppStyle.type.caption,
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    EkhoExplorerTheme {
        SplashScreen()
    }
}
