package com.fsql.data

import com.google.firebase.firestore.FirebaseFirestore

/**
 * The one entry point for database access.
 *
 * `execute` NEVER throws for database, network, timeout, permission, quota or bad-query problems:
 * it returns [DbResult.Failure].
 */
interface FirebaseService {

    /** Runs a stored procedure with the values in [query] and reports the outcome. */
    suspend fun execute(query: DbQuery): DbResult
    suspend fun <T> raw(timeoutMs: Long? = null, block: suspend (FirebaseFirestore) -> T): RawResult<T>
}

/** The outcome of [FirebaseService.raw]. */
sealed interface RawResult<out T> {
    data class Success<out T>(val value: T) : RawResult<T>
    data class Failure(val error: DbError) : RawResult<Nothing>
}
