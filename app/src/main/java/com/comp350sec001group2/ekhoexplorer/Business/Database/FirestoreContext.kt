package com.comp350sec001group2.ekhoexplorer.Business.Database

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirestoreContext(val firestore: FirebaseFirestore) {
    fun collection(vararg path: String): CollectionReference =
        firestore.collection(FirestorePaths.join(path, expectDocument = false))

    fun document(vararg path: String): DocumentReference =
        firestore.document(FirestorePaths.join(path, expectDocument = true))

    suspend fun <T> getDocument(
        ref: DocumentReference,
        reader: DocumentReader<T>,
        source: Source = Source.DEFAULT,
    ): T? {
        val snapshot = ref.get(source).await()
        return if(snapshot.exists()) reader.readSafely(snapshot) else null
    }

    suspend fun <T> getDocuments(
        query: Query,
        limit: Int,
        reader: DocumentReader<T>,
        source: Source = Source.DEFAULT,
    ): List<T> {
        val snapshot = query.limit(requireReadLimit(limit).toLong()).get(source).await()
        return snapshot.documents.map {reader.readSafely(it)}
    }

    fun <T> observeDocument(
        ref: DocumentReference,
        reader: DocumentReader<T>,
        includeMetadataChanges: Boolean = false
    ): Flow<StreamUpdate<T?>> = observeDocumentFlow(ref, reader, includeMetadataChanges)

    fun <T> observeQuery(
        query: Query,
        limit: Int,
        reader: DocumentReader<T>,
        includeMetadataChanges: Boolean = false,
    ): Flow<StreamUpdate<List<T>>> = observeQueryFlow(query.limit(requireReadLimit(limit).toLong()), reader, includeMetadataChanges)

    companion object {
        const val MAX_READ_LIMIT = 1000

        internal fun requireReadLimit(limit: Int): Int {
            if(limit !in 1..MAX_READ_LIMIT) {
                throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "limit must be in 1..$MAX_READ_LIMIT but was $limit")
            }

            return limit
        }
    }

}
