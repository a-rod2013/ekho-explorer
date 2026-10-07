package com.fsql.data.internal

import com.fsql.data.DbError
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import java.io.IOException

/** Turns anything that can go wrong while talking to Firestore into a [DbError]. Never throws. */
internal object ErrorMapper {

    private val URL = Regex("https?://\\S+")

    fun map(error: Throwable): DbError = when (error) {
        is FirebaseFirestoreException -> fromFirestore(error)
        is IOException -> DbError.Network(detail(error), error)
        is IllegalArgumentException ->
            DbError.InvalidQuery("Firestore rejected a value or path: ${detail(error)}", error)
        else -> DbError.Unknown(detail(error), error)
    }

    private fun fromFirestore(e: FirebaseFirestoreException): DbError {
        val dev = "Firestore ${e.code.name}: ${e.message}"
        return when (e.code) {
            Code.UNAVAILABLE -> DbError.Network(dev, e)
            Code.DEADLINE_EXCEEDED -> DbError.Timeout(dev, e)
            Code.PERMISSION_DENIED -> DbError.PermissionDenied(dev, e)
            Code.UNAUTHENTICATED -> DbError.Unauthenticated(dev, e)
            Code.NOT_FOUND -> DbError.NotFound(dev, e)
            Code.RESOURCE_EXHAUSTED -> DbError.QuotaExceeded(dev, e)
            Code.ALREADY_EXISTS, Code.ABORTED -> DbError.Conflict(dev, e)
            Code.INVALID_ARGUMENT -> DbError.InvalidQuery(dev, e)
            Code.FAILED_PRECONDITION -> {
                val message = e.message.orEmpty()
                if (message.contains("index", ignoreCase = true)) {
                    val link = URL.find(message)?.value?.trimEnd('.', ',', ';', ')', ']', '"', '\'')
                    val how = if (link != null) "Create the index here: $link" else "Create the index in the Firebase console."
                    DbError.MissingIndex(link, "$dev. This query needs a composite index. $how", e)
                } else {
                    DbError.Conflict(dev, e)
                }
            }
            else -> DbError.Unknown(dev, e)
        }
    }

    private fun detail(t: Throwable) = "${t::class.simpleName}: ${t.message}"
}
