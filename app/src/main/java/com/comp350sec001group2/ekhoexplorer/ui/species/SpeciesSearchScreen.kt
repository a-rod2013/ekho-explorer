package com.comp350sec001group2.ekhoexplorer.ui.species

// --- Species Search Screen ---

// to create a user interface
import androidx.compose.runtime.Composable
// displays text on the screen
import androidx.compose.material3.Text
// arranges screen from top to bottom
import androidx.compose.foundation.layout.Column
// creates a border text box
import androidx.compose.material3.OutlinedTextField
// reads the stored text
import androidx.compose.runtime.getValue
// creates a value to track
import androidx.compose.runtime.mutableStateOf
// keeps the value
import androidx.compose.runtime.remember
// updates the stored text
import androidx.compose.runtime.setValue
// around corners of the search bar
import androidx.compose.foundation.shape.RoundedCornerShape
// keep UI measurements consistent
import androidx.compose.ui.unit.dp
// displays icons on the screen
import androidx.compose.material3.Icon

// displays a preview of the screen
import androidx.compose.ui.tooling.preview.Preview
@Composable
fun SpeciesSearchScreen()
{
    var searchText by remember { mutableStateOf("") }

    Column{
        Text("Search Species")

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search species") },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SpeciesSearchScreenPreview() {
    SpeciesSearchScreen()
}

