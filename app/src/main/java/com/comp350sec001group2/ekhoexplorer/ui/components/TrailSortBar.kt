package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailSort
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

private fun TrailSort.label(): String = when (this) {
    TrailSort.DISTANCE -> "Distance"
    TrailSort.DIFFICULTY -> "Difficulty"
    TrailSort.VISIBILITY -> "Visibility"
    TrailSort.POPULARITY -> "Popularity"
}

@Composable
fun TrailSortBar(
    selected: TrailSort,
    ascending: Boolean,
    onSort: (TrailSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = AppStyle.dimens
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(horizontal = d.lg, vertical = d.sm),
        horizontalArrangement = Arrangement.spacedBy(d.sm),
    ) {
        TrailSort.entries.forEach { sort ->
            val active = sort == selected
            FilterChip(
                selected = active,
                onClick = { onSort(sort) },
                label = { Text(sort.label()) },
                trailingIcon = if (active) {
                    {
                        Icon(
                            imageVector = if (ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = if (ascending) "Ascending" else "Descending",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else null,
                modifier = Modifier.testTag("sort-${sort.name}"),
            )
        }
    }
}
