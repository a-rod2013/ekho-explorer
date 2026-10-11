package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle



@Composable
fun FeedHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val d = AppStyle.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppStyle.colors.background)
            .statusBarsPadding()
            .padding(horizontal = d.lg, vertical = d.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.md),
    ) {
        LogoPlaceholder()
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun LogoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AppStyle.dimens.logo)
            .clip(RoundedCornerShape(AppStyle.dimens.radiusSm))
            .background(AppStyle.colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "L", style = AppStyle.type.logo, color = AppStyle.colors.onAccent)
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = AppStyle.dimens
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = AppStyle.type.searchText.copy(color = AppStyle.colors.textPrimary),
        cursorBrush = SolidColor(AppStyle.colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        modifier = modifier
            .height(d.searchHeight)
            .clip(RoundedCornerShape(d.pill))
            .background(AppStyle.colors.surface),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.padding(horizontal = d.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = AppStyle.colors.textSecondary,
                    modifier = Modifier.size(d.icon),
                )
                Spacer(Modifier.width(d.sm))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search",
                            style = AppStyle.type.searchHint,
                            color = AppStyle.colors.textSecondary,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = AppStyle.colors.textSecondary,
                        modifier = Modifier
                            .size(d.icon)
                            .clickable { onQueryChange("") },
                    )
                }
            }
        },
    )
}
