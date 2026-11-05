package com.comp350sec001group2.ekhoexplorer.Business.Database

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlin.coroutines.cancellation.CancellationException

enum class FirestoreErrorKind {
    PERMISSION_DENIED,
    UNAUTHENTICATED,

    UNAVAILABLE,
    TIMEOUT,
    NOT_FOUND,
    ALREADY_EXISTS,
    INVALID_ARGUMENT,

    PRECONDITION_FAILED,

    CONTENTION,
    RESOURCE_EXHAUSTED,

    SCHEMA_MISMATCH,
    UNKNOWN,
}

data class FirestoreError(
    val kind: FirestoreErrorKind,
    val operation: String,
    val detail: String,
    val cause: Throwable? = null,
) {
    fun toException(): FirestoreException = FirestoreException(kind, detail, cause, operation)
}

class FirestoreException(
    val kind: FirestoreErrorKind,
    val detail: String,
    cause: Throwable? = null,
    val operation: String = "",
) : RuntimeException(buildMessage(kind, detail, operation), cause) {
    val error: FirestoreError get() = FirestoreError(kind, operation, detail, cause)

    internal fun withOperation(name: String): FirestoreException =
        if(operation.isNotEmpty()) this else FirestoreException(kind, detail, cause, name)

    private companion object {
        fun buildMessage(kind: FirestoreErrorKind, detail: String, operation: String): String =
            if(operation.isEmpty()) "$kind: $detail" else "[$operation] $kind: $detail"
    }
}

internal fun translateError(t: Throwable, operation: String): FirestoreException {
    if (t is CancellationException) throw t
    var current: Throwable? = t
    var depth = 0
    while (current != null && depth++ < MAX_CAUSE_DEPTH) {
        when (current) {
            is FirestoreException -> return current.withOperation(operation)
            is FirebaseFirestoreException ->
                return FirestoreException(kindOf(current.code), "Firestore code ${current.code.name}", current, operation)
        }
        current = current.cause
    }
    return FirestoreException(FirestoreErrorKind.UNKNOWN, "Unexpected ${t::class.java.simpleName}", t, operation)
}

private const val MAX_CAUSE_DEPTH = 10

private fun kindOf(code: FirebaseFirestoreException.Code): FirestoreErrorKind = when (code) {
    FirebaseFirestoreException.Code.PERMISSION_DENIED -> FirestoreErrorKind.PERMISSION_DENIED
    FirebaseFirestoreException.Code.UNAUTHENTICATED -> FirestoreErrorKind.UNAUTHENTICATED
    FirebaseFirestoreException.Code.UNAVAILABLE -> FirestoreErrorKind.UNAVAILABLE
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> FirestoreErrorKind.TIMEOUT
    FirebaseFirestoreException.Code.NOT_FOUND -> FirestoreErrorKind.NOT_FOUND
    FirebaseFirestoreException.Code.ALREADY_EXISTS -> FirestoreErrorKind.ALREADY_EXISTS
    FirebaseFirestoreException.Code.INVALID_ARGUMENT,
    FirebaseFirestoreException.Code.OUT_OF_RANGE -> FirestoreErrorKind.INVALID_ARGUMENT
    FirebaseFirestoreException.Code.FAILED_PRECONDITION -> FirestoreErrorKind.PRECONDITION_FAILED
    FirebaseFirestoreException.Code.ABORTED -> FirestoreErrorKind.CONTENTION
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> FirestoreErrorKind.RESOURCE_EXHAUSTED
    else -> FirestoreErrorKind.UNKNOWN
}
