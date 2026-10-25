package com.comp350sec001group2.ekhoexplorer.Business.Database

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class DocumentData(
    val id: String,
    val path: String,
    private val fields: Map<String, Any?>,
) {
    companion object {
        fun from(snapshot: DocumentSnapshot): DocumentData = DocumentData(
            id = snapshot.id,
            path = snapshot.reference.path,
            fields = snapshot.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: emptyMap(),
        )
    }

    fun has(name: String): Boolean = fields[name] != null

    fun string(name: String): String = stringOrNull(name) ?: missing(name)
    fun stringOrNull(name: String): String? = typed(name, "string") { it as? String }

    fun boolean(name: String): Boolean = booleanOrNull(name) ?: missing(name)
    fun booleanOrNull(name: String): Boolean? = typed(name, "boolean") { it as? Boolean }

    fun long(name: String): Long = longOrNull(name) ?: missing(name)
    fun longOrNull(name: String): Long? = typed(name, "whole number") {
        when (it) {
            is Long -> it
            is Int -> it.toLong()
            is Double -> if (it.isFinite() && it == Math.floor(it)) it.toLong() else null
            else -> null
        }
    }

    fun int(name: String): Int {
        val v = long(name)
        if (v < Int.MIN_VALUE || v > Int.MAX_VALUE) throw mismatch("field '$name' does not fit in Int")
        return v.toInt()
    }

    fun double(name: String): Double = doubleOrNull(name) ?: missing(name)
    fun doubleOrNull(name: String): Double? = typed(name, "number") { (it as? Number)?.toDouble() }

    fun instant(name: String): Instant = instantOrNull(name) ?: missing(name)
    fun instantOrNull(name: String): Instant? = typed(name, "timestamp") {
        (it as? Timestamp)?.let { ts -> Instant.ofEpochSecond(ts.seconds, ts.nanoseconds.toLong()) }
    }


    inline fun <reified E : Enum<E>> enum(name: String): E {
        val raw = string(name)
        return enumValues<E>().firstOrNull { it.name == raw }
            ?: throw mismatch("field '$name' has unknown ${E::class.java.simpleName} value")
    }


    inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E {
        val raw = stringOrNull(name) ?: return default
        return enumValues<E>().firstOrNull { it.name == raw } ?: default
    }


    fun stringList(name: String): List<String> = typedList(name, "string") { it as? String }


    fun objectList(name: String): List<DocumentData> {
        val parent = this
        return typedList(name, "map") { element ->
            @Suppress("UNCHECKED_CAST")
            (element as? Map<String, Any?>)?.let { DocumentData(parent.id, "${parent.path}.$name", it) }
        }
    }

    fun nestedOrNull(name: String): DocumentData? = typed(name, "map") { value ->
        @Suppress("UNCHECKED_CAST")
        (value as? Map<String, Any?>)?.let { DocumentData(id, "$path.$name", it) }
    }

    fun nested(name: String): DocumentData = nestedOrNull(name) ?: missing(name)

    fun mismatch(detail: String): FirestoreException =
        FirestoreException(FirestoreErrorKind.SCHEMA_MISMATCH, "$path: $detail")

    private fun missing(name: String): Nothing = throw mismatch("required field '$name' is missing")

    private inline fun <T : Any> typed(name: String, expected: String, cast: (Any) -> T?): T? {
        val value = fields[name] ?: return null
        return cast(value) ?: throw mismatch("field '$name' expected $expected but was ${value::class.java.simpleName}")
    }

    private inline fun <T : Any> typedList(name: String, expected: String, cast: (Any) -> T?): List<T> {
        val raw = fields[name] ?: return emptyList()
        if (raw !is List<*>) throw mismatch("field '$name' expected list but was ${raw::class.java.simpleName}")
        return raw.mapIndexed { index, element ->
            if (element == null) throw mismatch("field '$name'[$index] is null")
            cast(element) ?: throw mismatch("field '$name'[$index] expected $expected but was ${element::class.java.simpleName}")
        }
    }
}


fun interface DocumentReader<out T> {
    fun read(doc: DocumentData): T
}


fun interface DocumentWriter<in T> {
    fun toFields(model: T): Map<String, Any?>
}

interface FirestoreMapper<T> : DocumentReader<T>, DocumentWriter<T>

internal fun <T> DocumentReader<T>.readSafely(snapshot: DocumentSnapshot): T {
    val data = DocumentData.from(snapshot)
    try {
        return read(data)
    } catch (e: FirestoreException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: RuntimeException) {
        throw FirestoreException(FirestoreErrorKind.SCHEMA_MISMATCH, "${data.path}: mapper failed (${e::class.java.simpleName})", e)
    }
}
