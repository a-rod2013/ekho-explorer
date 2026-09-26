package com.fsql.data

import com.google.firebase.Timestamp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class Cursor internal constructor(
    internal val procedure: String,
    internal val values: List<Any?>,
    internal val documentPath: String,
)
{
    fun serialize(): String = JsonObject
    {
        mapOf(
            "p" to JsonPrimitive(procedure),
            "v" to JsonArray(values.map { encode(it)}),
            "d" to JsonPrimitive(documentPath),
        ).toString()
    }
}