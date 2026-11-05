package com.comp350sec001group2.ekhoexplorer.Business.Database

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate


data class StreamUpdate<out T>(
    val value: T,
    val fromCache: Boolean,
    val hasPendingWrites: Boolean,
)
internal fun <T> observeDocumentFlow(
    ref: DocumentReference,
    reader: DocumentReader<T>,
    includeMetadataChanges: Boolean,
): Flow<StreamUpdate<T?>> = callbackFlow {
    val registration = ref.addSnapshotListener(metadata(includeMetadataChanges)) { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot == null) return@addSnapshotListener
        try {
            val value = if (snapshot.exists()) reader.readSafely(snapshot) else null
            trySend(StreamUpdate(value, snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites()))
        } catch (e: Exception) {
            close(e)
        }
    }
    awaitClose { registration.remove() }
}.conflate()

internal fun <T> observeQueryFlow(
    query: Query,
    reader: DocumentReader<T>,
    includeMetadataChanges: Boolean,
): Flow<StreamUpdate<List<T>>> = callbackFlow {
    val registration = query.addSnapshotListener(metadata(includeMetadataChanges)) { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot == null) return@addSnapshotListener
        try {
            val items = snapshot.documents.map { reader.readSafely(it) }
            trySend(StreamUpdate(items, snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites()))
        } catch (e: Exception) {
            close(e)
        }
    }
    awaitClose { registration.remove() }
}.conflate()

private fun metadata(include: Boolean) = if (include) MetadataChanges.INCLUDE else MetadataChanges.EXCLUDE
