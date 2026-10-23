package com.comp350sec001group2.ekhoexplorer.Business.Database

import com.firestoreaccess.mapping.DocumentReader
import com.firestoreaccess.mapping.readSafely
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch

abstract class FirestoreTransaction<out T>: FirestoreOperation() {
    override val kind: OperationKind get() = OperationKind.TRANSACTION

    open val maxAttempts: Int get() = 5

    abstract fun run(tx: TransactionScope): T
}

class TransactionScope internal constructor(
    val db: FirestoreContext,
    private val tx: Transaction,
) {
    fun <T> read(ref: DocumentReference, reader: DocumentReader<T>) : T? {
        val snapshot = tx.get(ref)
        return if(snapshot.exists()) reader.readSafely(snapshot) else null
    }

    fun set(ref: DocumentReference, fields: Map<String, Any?>) {
        tx.set(ref, fields)
    }

    fun update(ref: DocumentReference, fields: Map<String, Any?>) {
        tx.update(ref, fields)
    }

    fun delete(ref: DocumentReference){
        tx.delete(ref)
    }
}

abstract class FirestoreBatch : FirestoreOperation() {
    override val kind: OperationKind get() = OperationKind.BATCH

    abstract fun stage(batch: BatchScope)
}

class BatchScope internal constructor(
    val db: FirestoreContext,
    private val batch: WriteBatch,
) {
    var size: Int = 0
        private set

    fun set(ref: DocumentReference, fields: Map<String, Any?>, merge: Boolean = false) {
        count()
        if(merge) batch.set(ref, fields, SetOptions.merge()) else batch.set(ref, fields)
    }

    fun update(ref: DocumentReference, fields: Map<String, Any?>) {
        count()
        batch.update(ref, fields)
    }

    fun delete(ref: DocumentReference) {
        count()
        batch.delete(ref)
    }

    private fun count() {
        if(size >= MAX_OPERATIONS) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "A batch holds at most $MAX_OPERATIONS writes")
        }

        size++
    }

    companion object {
        const val MAX_OPERATIONS = 500
    }
}

