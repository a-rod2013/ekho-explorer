package com.example.fieldjournal

import android.content.Context
import org.json.JSONArray
import java.io.File


// The file's name. Only this file can see it.
private const val NOTES_FILE = "notes.json"

// Reads saved notes. Return empty list if nothing has been saved.
fun loadNotes(context: Context): List<String> {
    // filesDir is a folder only you app can read.
    val file = File(context.filesDir, NOTES_FILE)
    // First launch: no file yet, so there's nothing to load.
    if (!file.exists()) return emptyList()
    return try {
        // Read the file's text as JSON array of strings
        val array = JSONArray(file.readText())
        // Build a normal Kotlin list from it, one item per index.
        List(array.length()) { index -> array.getString(index) }
    } catch (e: Exception) {
        // A damaged file should not crash the app, so start with no notes.
        emptyList()
    }
}

// Writes the whole list to the file, replaces what exists inside.
fun saveNotes(context: Context, notes: List<String>) {
    val array = JSONArray()
    notes.forEach { array.put(it) }
    File(context.filesDir, NOTES_FILE).writeText(array.toString())
}