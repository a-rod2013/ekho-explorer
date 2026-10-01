package com.fsql.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 *  One document from a query result.
 *
 *  Getters return null when the field is missing or null, and throw [IllegalStateException]
 *  (naming the field) when the field holds a different type than you asked for.
 */
class Row internal constructor(
    val id: String?,
    private val data: Map<String, Any?>,
    private val snapshot: DocumentSnapshot? = null,
) {
    /** The raw field values. */
    val fields: Map<String, Any?> get() = data

    fun has(name: String): Boolean = value(name) != null

    fun string(name: String): String? = typed(name, "a string") { it as? String}

    fun long(name: String): Long? = typed(name, "a whole number") {
        when(it) {
            is Long -> it
            is Int -> it.toLong()
            else -> null
        }
    }

    fun double(name: String): Double? = typed(name, "a number") { (it as? Number)?.toDouble() }

    fun boolean(name: String): Boolean? = typed(name, "a boolean") { it as? Boolean }

    fun timestamp(name: String): Timestamp? = typed(name, "a timestamp") { it as? Timestamp }

    fun list(name: String): List<Any?>? = typed(name, "a list") { (it as? List<*>)?.toList() }

    fun map(name: String): Map<String, Any?>? = typed(name, "a map") {
        (it as? Map<*, *>)?.entries?.associate { entry -> entry.key.toString() to entry.value }
    }

    /**
     * Converts the whole document to your own class using Firestore's rules (a public no-argument
     * constructor or default values for every property). Only rows read from a real document can be
     * mapped; aggregate rows (COUNT/SUM/AVG) cannot.
     */
    fun <T : Any> mapTo(type: Class<T>): T {
        val doc = snapshot ?: error("mapTo needs a row read from a document; aggregate rows cannot be mapped.")
        return doc.toObject(type) ?: error("The document '$id' has no data to map to ${type.simpleName}.")
    }

    inline fun <reified T : Any> mapTo(): T = mapTo(T::class.java)

    private fun value(name: String): Any? {
        if (data.containsKey(name)) return data[name]
        if (name == "id") return id
        var current: Any? = data
        for (part in name.split('.')) {
            current = (current as? Map<*, *>)?.get(part) ?: return null
        }
        return current
    }

    private fun <T> typed(name: String, expected: String, cast: (Any) -> T?): T? {
        val v = value(name) ?: return null
        return cast(v)
            ?: throw IllegalStateException("Field '$name' holds ${v::class.simpleName}, not $expected.")
    }
}