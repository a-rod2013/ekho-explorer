package com.fsql.data.internal

/**
 * A request to run one stored procedure, with values for its parameters.
 *
 * @property procedure the procedure name, which equals the `.fsql` file name without its extension
 * @property timeoutMs how long to wait for the database; null uses the service default (10 seconds)
 */
class DbQuery (
    val procedure: String,
    vararg params: Pair<String, Any?>,
    val timeoutMs: Long? = null,
) {
    internal val paramPairs: List<Pair<String, Any?>> = params.toList()
    val params: Map<String, Any?> get() = paramPairs.toMap()
    override fun toString(): String = "DbQuery($procedure, params=${paramPairs.map {it.first}})"
}
