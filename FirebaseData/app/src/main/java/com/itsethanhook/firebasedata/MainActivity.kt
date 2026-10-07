package com.itsethanhook.firebasedata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fsql.data.DbQuery
import com.fsql.data.DbResult
import com.fsql.data.FirebaseData
import com.itsethanhook.firebasedata.ui.theme.FirebaseDataTheme
import kotlinx.coroutines.launch
import com.fsql.data.Row as DbRow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FirebaseDataTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NotesScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf<List<DbRow>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        when (val result = FirebaseData.service.execute(DbQuery("ListNotes", "limit" to 50))) {
            is DbResult.Success -> {
                notes = result.rows
                message = null
            }
            is DbResult.Failure -> message = result.error.userMessage
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Notes", style = MaterialTheme.typography.headlineMedium)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("New note title") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    val text = title.trim()
                    scope.launch {
                        when (val result = FirebaseData.service.execute(
                            DbQuery("InsertNote", "title" to text, "body" to null)
                        )) {
                            is DbResult.Success -> {
                                title = ""
                                refresh()
                            }
                            is DbResult.Failure -> message = result.error.userMessage
                        }
                    }
                },
            ) { Text("Add") }
        }

        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(notes, key = { it.id.orEmpty() }) { note ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(note.string("title") ?: "(untitled)", Modifier.weight(1f))
                    TextButton(onClick = {
                        val id = note.id ?: return@TextButton
                        scope.launch {
                            when (val result = FirebaseData.service.execute(DbQuery("DeleteNote", "id" to id))) {
                                is DbResult.Success -> refresh()
                                is DbResult.Failure -> message = result.error.userMessage
                            }
                        }
                    }) { Text("Delete") }
                }
            }
        }
    }
}