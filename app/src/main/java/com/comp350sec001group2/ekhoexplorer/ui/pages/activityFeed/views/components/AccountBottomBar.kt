package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun AccountBottomBar(onAccountClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val d = AppStyle.dimens

    Column(modifier = modifier.fillMaxWidth().background(AppStyle.colors.background)) {
        HorizontalDivider(color = AppStyle.colors.divider)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(d.bottomBar)
                .padding(vertical = d.sm),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier
                .clip(RoundedCornerShape(d.radiusMd))
                .clickable(onClick=onAccountClick)
                .padding(horizontal = d.xl, vertical = d.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = AppStyle.colors.accent,
                    modifier = Modifier.size(d.icon),
                )

                Text(text="Account", style=AppStyle.type.navLabel, color = AppStyle.colors.accent)
            }
        }
    }
}
