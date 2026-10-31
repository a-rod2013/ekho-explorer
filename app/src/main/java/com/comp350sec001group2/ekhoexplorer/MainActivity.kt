package com.comp350sec001group2.ekhoexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.comp350sec001group2.ekhoexplorer.ui.navigation.views.AppNavigation
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme
import com.comp350sec001group2.ekhoexplorer.ui.theme.ThemeMode
import com.comp350sec001group2.ekhoexplorer.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDarkTheme by rememberSaveable() { mutableStateOf(ThemeMode.SYSTEM) }

            EkhoExplorerTheme(darkTheme = isDarkTheme.isDark()) {
                AppNavigation()
            }
        }
    }
}
