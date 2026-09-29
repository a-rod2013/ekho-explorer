package com.fsql.data.internal

import com.google.firebase.Timestamp
import java.sql.Timestamp
import java.util.Date

/**
 * Checks the values from a query against the parameters its proc declares and converts them into the type
 * the rest of the library expects. Runs pre db call.
 */
internal object ParamChecker {
    sealed interface Outcome {
        data class Ok(val values: Map<String, Any?>) : Outcome
        data class Invalid(val message: String) : Outcome
    }

    fun check(procedure: String, decls: List<ParamDecl>, supplied: List<Pair<String, Any?>>) : Outcome {
        val problems = mutableListOf<String>()

        val byName = LinkedHashMap<String, Any?>()
        for((key, value) in supplied) {
            if(byName.containsKey(key)) {
                problems += "`$key` was supplied more than once"
                byName[key] = value
            }
        }

        val declared = decls.associateBy{it.name}
        val unknown = byName.keys.filter{it !in declared}

        if(unknown.isNotEmpty()) {
            val names = decls.joinToString(", ") { "@${it.name}"}.ifEmpty {"none"}
            problems += "unknown parameter(s) ${unknown.joinToString(", ") {"`$it`"}} (the procedure declares: $names)"
        }

        val missing = decls.filter {it.name !in byName}
        if(missing.isNotEmpty()) {
            problems += "missing required parameters(s): ${missing.joinToString(", "){"@${it.name}"}}"
        }

        val values = LinkedHashMap<String, Any?>()
        for(decl in decls) {
            if(!byName.containsKey(decl.name)) {
                continue
            }

            val raw = byName[decl.name]
            if(raw == null) {
                if(decl.nullable) {
                    values[decl.name] = null
                } else {
                    problems += "@${decl.name} is null, but ${decl.type} does not allow null " +
                            "(declare it as `${decl.type} (nullable)` to allow that"
                }
                continue
            }

            if(decl.isList) {
                if(raw !is List<*>) {
                    problems += "@${decl.name} expects a list of ${decl.type}, got ${describe(raw)}"
                    continue
                }

                val out = ArrayList<Any?>(raw.size)
                var bad = false
                raw.forEachIndexed { index, item ->
                    if(item == null) {
                        problems += "@${decl.name} has a null at position $index; lists cannot contain null"
                        bad = true
                    } else {
                        val converted = convert(decl.type, item)

                        if(converted == NOT_CONVERTIBLE) {
                            problems += "@${decl.name}[$index] expects ${decl.type}, got ${describe(item)}"
                            bad = true
                        } else {
                            out += converted
                        }
                    }
                }

                if(!bad) {
                    values[decl.name] = out
                }

                continue
            }

            val converted = convert(decl.type, raw)
            if(converted == NOT_CONVERTIBLE) {
                problems += "@${decl.name} expects ${decl.type}, got ${describe(raw)}"
            } else {
                values[decl.name] = converted
            }
        }

        if(problems.isEmpty()) {
            return Outcome.Ok(values)
        } else {
            return Outcome.Invalid("$procedure: ${problems.joinToString("; ")}")
        }
    }

    private val NOT_CONVERTIBLE = Any()

    private fun convert(type: ParamType, value: Any): Any = when(type) {
        ParamType.STRING -> value as? String ?: NOT_CONVERTIBLE
        ParamType.INT -> when(value) {
            is Int -> value.toLong()
            is Long -> value
            is Short -> value.toLong()
            is Byte -> value.toLong()
            else -> NOT_CONVERTIBLE
        }

        ParamType.FLOAT -> when(value) {
            is Double -> value
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            else -> NOT_CONVERTIBLE
        }

        ParamType.BOOL -> value as? Boolean ?: NOT_CONVERTIBLE
        ParamType.TIMESTAMP -> when(value) {
            is Timestamp -> value
            is Date -> Timestamp(value)
            else -> NOT_CONVERTIBLE
        }
    }

    private fun describe(value: Any): String = value::class.simpleName ?: value.javaClass.name
}