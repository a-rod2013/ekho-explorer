package com.example.fieldjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fieldjournal.ui.theme.FieldJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FieldJournalTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NoteScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun NoteScreen(modifier: Modifier = Modifier) {
    var noteText by remember { mutableStateOf("") }
    val notes = remember { mutableStateListOf<String>() }

    Column(modifier = modifier.padding(16.dp)) {
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Field note") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (noteText.isNotBlank()) {
                    notes.add(0, noteText.trim())
                    noteText = ""
                }
            },
            modifier = Modifier.padding(top = 8.dp)) {
            Text("Save note")
        }
        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            items(notes) { note ->
                Text(note, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}