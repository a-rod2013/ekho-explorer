package com.fsql.data

/**
 * The outcome of a query. It is always returned, never thrown.
 */
sealed interface DbResult {
    /**
     * @property rows the documents read, or one row of aggregate values for COUNT/SUM/AVG
     * @property affectedCount how many documents were written or deleted
     * @property generatedIds ids of documents created without an explicit id column
     */
    data class Success(
        val rows: List<Row> = emptyList(),
        val affectedCount: Int = 0,
        val generatedIds: List<String> = emptyList()
    ) : DbResult

    data class Failure(val error: DbError): DbResult

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun errorOnNull(): DbError? = (this as? Failure).error
    fun getOrNull(): Success? = this as? Success

    /** @throws DbException if this is a failure. Use only if an exception is desired. */
    fun getOrThrow(): Success = when(this) {
        is Success -> this
        is Failure -> throw DbException(error)
    }

    fun onSuccess(action: (Success) -> Unit): DbResult {
        if(this is Success) action(this)
        return this
    }

    fun onFailure(action: (DbError) -> Unit): DbResult {
        if(this is Failure) action(error)
        return this
    }
}