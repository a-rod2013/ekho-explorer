package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun ExpandableCaption(text: String, modifier: Modifier = Modifier, collapsedLines: Int = 2) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var overflows by rememberSaveable(text) { mutableStateOf(false) }

    Column(modifier) {
        Text(
            text = text,
            style = AppStyle.type.caption,
            color = AppStyle.colors.textPrimary,
            maxLines = if(expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = {if(!expanded) overflows = it.hasVisualOverflow},
        )

        if(overflows) {
            Text(
                text = if(expanded) "less" else "more",
                style = AppStyle.type.captionAction,
                color = AppStyle.colors.textSecondary,
                modifier = Modifier
                    .clickable{expanded = !expanded}
                    .padding(top = AppStyle.dimens.xs),
            )
        }
    }
}
