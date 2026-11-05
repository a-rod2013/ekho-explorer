package com.comp350sec001group2.ekhoexplorer.Business.Database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

sealed interface FirestoreResult<out T> {
    data class Success<out T>(val data: T) : FirestoreResult<T>
    data class Failure(val error: FirestoreError) : FirestoreResult<Nothing>
}

fun <T> FirestoreResult<T>.getOrThrow(): T = when(this) {
    is FirestoreResult.Success -> data
    is FirestoreResult.Failure -> throw error.toException()
}

fun <T> FirestoreResult<T>.getOrNull(): T? = (this as? FirestoreResult.Success)?.data

suspend fun <T> firestoreResult(block: suspend () -> T): FirestoreResult<T> =
    try {
        FirestoreResult.Success(block())
    } catch(e: FirestoreException) {
        FirestoreResult.Failure(e.error)
    }

fun <T> Flow<T>.asFirestoreResults(): Flow<FirestoreResult<T>> =
    map<T, FirestoreResult<T>> { FirestoreResult.Success(it) }
        .catch{e-> if(e is FirestoreException) emit(FirestoreResult.Failure(e.error)) else throw e}
