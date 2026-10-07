package com.fsql.data

/**
 * A request to run one stored procedure, with the values for its parameters.
 *
 * The procedure declares which parameters exist and what type each one is (see the `.fsql` file's
 * `-- @name: TYPE` header lines). Anything that does not match (a missing parameter, an unknown name,
 * an Int where the procedure wants a STRING) makes the query fail with [DbError.InvalidQuery] before
 * the database is contacted.
 *
 * @property procedure the procedure name, which equals the `.fsql` file name without its extension
 * @property timeoutMs how long to wait for the database; null uses the service default (10 seconds)
 */
class DbQuery(
    val procedure: String,
    vararg params: Pair<String, Any?>,
    val timeoutMs: Long? = null,
) {
    internal val paramPairs: List<Pair<String, Any?>> = params.toList()

    /** The supplied values by parameter name. */
    val params: Map<String, Any?> get() = paramPairs.toMap()

    override fun toString(): String = "DbQuery($procedure, params=${paramPairs.map { it.first }})"
}
