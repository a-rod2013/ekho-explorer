package com.fsql.data.internal

import com.fsql.data.DbResult

/** Runs a bound operation. Implementations may throw; the service maps every exception to a DbError. */
internal interface OpExecutor {
    suspend fun run(op: BoundOp): DbResult
}
