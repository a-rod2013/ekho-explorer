package com.comp350sec001group2.ekhoexplorer.ui.navigation.views

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import com.comp350sec001group2.ekhoexplorer.ui.navigation.models.Screen
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

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
        Screen.Map to Icons.Filled.Map,
        Screen.Feed to Icons.Filled.Home,
        Screen.Profile to Icons.Filled.Person
    )

    NavigationBar(
        modifier = modifier.height(dimens.bottomBar),
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
                        contentDescription = screen.title
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
}
