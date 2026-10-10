package com.example.fieldjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    // The storage functions need a Context to find the apps private folder
    val context = LocalContext.current
    // rememberSavable keeps text through rotation. Plain remember loses it.
    var noteText by rememberSaveable { mutableStateOf("") }
    // Observable list. Resets on rotation.
    val notes = remember {
        mutableStateListOf<String>().apply { addAll(loadNotes(context)) }
    }

    Column(modifier = modifier.padding(16.dp)) {
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            // stringResource reads text from strings.xml instead of hard-coding it.
            label = { Text(stringResource(id = R.string.field_note_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (noteText.isNotBlank()) {
                    notes.add(0, noteText.trim())
                    noteText = ""
                    saveNotes(context, notes)
                }
            },
            modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.save_note))
        }

        // Empty state: show message until first note exists.
        if (notes.isEmpty()) {
            Text(stringResource(
                R.string.empty_notes),
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            // Otherwise, show the list
            LazyColumn(modifier = Modifier.padding(top = 16.dp),
                // Adds space between cards.
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    NoteCard(text = note)
                }
            }
        }
    }
}

// Note cards showing a note. Photos can be added to this card later on.
@Composable
fun NoteCard(text: String, modifier: Modifier = Modifier) {
    Card(
        // fillMaxWidth makes every card as wide as the screen.
        modifier = modifier.fillMaxWidth(),
        // A small shadow so the card lifts off the background
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // Notecard fill color.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        // Padding keeps the text from touching the edge of the note card.
        Text(text = text, modifier = Modifier.padding(16.dp))
    }
}

// Preview of the note cards. Better testing and debugging.
@Preview(showBackground = true)
@Composable
fun NoteCardPreview() {
    FieldJournalTheme {
        NoteCard(text = "Monarch butterfly on milkweed near the creek")
    }
}