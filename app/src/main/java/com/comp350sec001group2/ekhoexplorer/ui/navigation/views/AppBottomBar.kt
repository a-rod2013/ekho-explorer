package com.comp350sec001group2.ekhoexplorer.ui.navigation.views

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Explore
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Forum
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.layout.layoutId
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.Screen
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import androidx.compose.material.icons.filled.Search

@Composable
fun AppBottomBar(
    currentDestination: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppStyle.colors
    val dimens = AppStyle.dimens
    val typography = AppStyle.type

    val items = listOf(
        Screen.Feed to Icons.Filled.Forum,
        Screen.Map to Icons.Filled.Explore,
        Screen.Species to Icons.Filled.Search,
        Screen.Profile to Icons.Filled.Person
    )

    Box {
        NavigationBar(
            modifier = modifier
                .height(dimens.bottomBar)
                .padding(vertical = 0.dp),
            containerColor = colors.background,
            contentColor = colors.textPrimary
        ) {
            items.forEach { (screen, icon) ->
                val selected = currentDestination == screen.destination

                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(screen) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = screen.title,
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            style = typography.navLabel
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.accent,
                        selectedTextColor = colors.accent,
                        unselectedIconColor = colors.textSecondary,
                        unselectedTextColor = colors.textSecondary,
                        indicatorColor = colors.surface
                    )
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            thickness = 5.dp,
            color = colors.divider
        )

    }
}
