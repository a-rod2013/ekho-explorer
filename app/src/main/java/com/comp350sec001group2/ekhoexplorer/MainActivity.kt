package com.comp350sec001group2.ekhoexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.comp350sec001group2.ekhoexplorer.ui.navigation.views.AppNavigation
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EkhoExplorerTheme {
                AppNavigation()
            }
        }
    }
}
