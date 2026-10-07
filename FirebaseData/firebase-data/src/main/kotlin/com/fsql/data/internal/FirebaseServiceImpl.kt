package com.fsql.data.internal

import android.util.Log
import com.fsql.data.DbError
import com.fsql.data.DbQuery
import com.fsql.data.DbResult
import com.fsql.data.FirebaseService
import com.fsql.data.RawResult
import com.fsql.plan.Plan
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull

internal class FirebaseServiceImpl(
    private val plans: Deferred<Map<String, Plan>>,
    private val executor: OpExecutor,
    private val firestore: () -> FirebaseFirestore,
    private val defaultTimeoutMs: Long,
) : FirebaseService {

    private companion object {
        const val TAG = "FirebaseData"
    }

    private class Holder<T>(val value: T)

    private sealed interface Guarded<out T> {
        data class Value<T>(val value: T) : Guarded<T>
        data class Failed(val error: DbError) : Guarded<Nothing>
    }

    override suspend fun execute(query: DbQuery): DbResult {
        val loaded = try {
            plans.await()
        } catch (e: Throwable) {
            currentCoroutineContext().ensureActive()
            val detail = e.message.orEmpty()
            val suffixed = if (detail.endsWith("Rebuild the app.")) {
                "The stored procedures could not be loaded: $detail"
            } else {
                "The stored procedures could not be loaded: $detail. Rebuild the app."
            }
            return failure(query.procedure, DbError.Unknown(suffixed, e))
        }

        val plan = loaded[query.procedure]
            ?: return failure(
                query.procedure,
                DbError.InvalidQuery(
                    "Unknown procedure '${query.procedure}'. Known procedures: " +
                            loaded.keys.sorted().joinToString(", ").ifEmpty { "none" } + ".",
                ),
            )

        val values = when (val checked = ParamChecker.check(plan.name, plan.params, query.paramPairs)) {
            is ParamChecker.Outcome.Invalid -> return failure(query.procedure, DbError.InvalidQuery(checked.message))
            is ParamChecker.Outcome.Ok -> checked.values
        }

        val op = when (val bound = PlanBinder.bind(plan, values)) {
            is PlanBinder.Result.Invalid -> return failure(query.procedure, DbError.InvalidQuery(bound.message))
            is PlanBinder.Result.Ok -> bound.op
        }

        // Extension point for batching/combining BoundOps before execution (see PlanOptimizer,
        val timeout = query.timeoutMs?.takeIf { it > 0 } ?: defaultTimeoutMs
        return when (val outcome = guarded(timeout, "The procedure '${plan.name}'") { executor.run(op) }) {
            is Guarded.Value -> outcome.value
            is Guarded.Failed -> failure(query.procedure, outcome.error)
        }
    }

    override suspend fun <T> raw(timeoutMs: Long?, block: suspend (FirebaseFirestore) -> T): RawResult<T> {
        val timeout = timeoutMs?.takeIf { it > 0 } ?: defaultTimeoutMs
        return when (val outcome = guarded(timeout, "The raw Firestore call") { block(firestore()) }) {
            is Guarded.Value -> RawResult.Success(outcome.value)
            is Guarded.Failed -> {
                Log.w(TAG, "raw call failed: ${outcome.error}", outcome.error.cause)
                RawResult.Failure(outcome.error)
            }
        }
    }

    /** Runs [block] with a timeout and turns every exception (except caller cancellation) into a failure. */
    private suspend fun <T> guarded(timeoutMs: Long, what: String, block: suspend () -> T): Guarded<T> =
        try {
            val holder = withTimeoutOrNull(timeoutMs) { Holder(block()) }
            if (holder == null) {
                Guarded.Failed(
                    DbError.Timeout(
                        "$what did not finish within $timeoutMs ms. If it was a write and the device was offline, " +
                                "the write may still be applied when the connection returns."
                    )
                )
            } else {
                Guarded.Value(holder.value)
            }
        } catch (e: Throwable) {
            if (e is CancellationException) currentCoroutineContext().ensureActive()
            Guarded.Failed(ErrorMapper.map(e))
        }

    private fun failure(procedure: String, error: DbError): DbResult.Failure {
        Log.w(TAG, "$procedure failed: $error", error.cause)
        return DbResult.Failure(error)
    }
}
