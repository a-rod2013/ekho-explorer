package com.comp350sec001group2.ekhoexplorer.Business.Database

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.TransactionOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.TimeSource

class FirestoreService(
    private val firestore: FirebaseFirestore,
    private val diagnostics: FirestoreDiagnostics = FirestoreDiagnostics.None,
    private val operationTimeout: Duration? = null,
) {
    private val context = FirestoreContext(firestore)

    suspend fun<T> execute(operation: FirestoreAccessor<T>): T =
        instrumented(operation) {operation.run(context)}

    suspend fun execute(operation: FirestoreBatch) {
        instrumented(operation) {
            val writeBatch = firestore.batch()
            val scope = BatchScope(context, writeBatch)
            operation.stage(scope)
            if (scope.size == 0) {
                throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "Batch staged no writes")
            }
            writeBatch.commit().await()
        }
    }

    suspend fun <T> execute(operation: FirestoreTransaction<T>): T = instrumented(operation) {
        val options = TransactionOptions.Builder().setMaxAttempts(operation.maxAttempts).build()
        firestore.runTransaction(options, Transaction.Function { tx -> operation.run(TransactionScope(context, tx)) }).await()
    }

    suspend fun <T> executeCatching(operation: FirestoreAccessor<T>): FirestoreResult<T> =
        firestoreResult { execute(operation) }

    fun <T> stream(operation: FirestoreStream<T>): Flow<T> = flow {
        val start = TimeSource.Monotonic.markNow()
        var first = true
        emitAll(
            operation.observe(context).onEach {
                if (first) {
                    first = false
                    diagnostics.onSuccess(operation.name, operation.kind, start.elapsedNow().inWholeMilliseconds)
                }
            },
        )
    }.catch { e ->
        val failure = translateError(e, operation.name)
        diagnostics.onFailure(operation.name, operation.kind, 0, failure.error)
        throw failure
    }

    private suspend fun <T> instrumented(operation: FirestoreOperation, block: suspend () -> T): T {
        val start = TimeSource.Monotonic.markNow()
        try {
            val result = withOperationTimeout(operation, block)
            diagnostics.onSuccess(operation.name, operation.kind, start.elapsedNow().inWholeMilliseconds)
            return result
        } catch (e: Exception) {
            val failure = translateError(e, operation.name) // rethrows cancellation, so it is never reported
            diagnostics.onFailure(operation.name, operation.kind, start.elapsedNow().inWholeMilliseconds, failure.error)
            throw failure
        }
    }

    private suspend fun <T> withOperationTimeout(operation: FirestoreOperation, block: suspend () -> T): T {
        val limit = operationTimeout ?: return block()
        val boxed = withTimeoutOrNull(limit) { Box(block()) }
            ?: throw FirestoreException(FirestoreErrorKind.TIMEOUT, "No result within $limit", operation = operation.name)
        return boxed.value
    }

    private class Box<T>(val value: T)
}
