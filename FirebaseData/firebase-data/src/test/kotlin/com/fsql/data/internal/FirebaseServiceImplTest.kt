package com.fsql.data.internal

import com.fsql.data.DbError
import com.fsql.data.DbQuery
import com.fsql.data.DbResult
import com.fsql.data.RawResult
import com.fsql.plan.Operand
import com.fsql.plan.ParamDecl
import com.fsql.plan.ParamType
import com.fsql.plan.Plan
import com.fsql.plan.PlanBody
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseServiceImplTest {

    private class FakeExecutor(var handler: suspend (BoundOp) -> DbResult) : OpExecutor {
        val ops = mutableListOf<BoundOp>()
        override suspend fun run(op: BoundOp): DbResult {
            ops += op
            return handler(op)
        }
    }

    private val getByIdPlan = Plan(
        "GetById",
        listOf(ParamDecl("id", ParamType.STRING, nullable = false)),
        PlanBody.DocGet("notes", Operand.Param("id"), null),
    )
    private val plans: Map<String, Plan> = mapOf("GetById" to getByIdPlan)

    private fun dummyFirestore(): FirebaseFirestore {
        val field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null)
        @Suppress("UNCHECKED_CAST")
        return unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
            .invoke(unsafe, FirebaseFirestore::class.java) as FirebaseFirestore
    }

    private fun service(executor: FakeExecutor, timeoutMs: Long = 1_000, loaded: Map<String, Plan> = plans) =
        FirebaseServiceImpl(CompletableDeferred(loaded), executor, { dummyFirestore() }, timeoutMs)

    private fun query(vararg params: Pair<String, Any?>, timeoutMs: Long? = null) =
        DbQuery("GetById", *params, timeoutMs = timeoutMs)

    @Test
    fun `a valid query runs the bound operation and returns its result`() = runTest {
        val executor = FakeExecutor { DbResult.Success(affectedCount = 7) }
        val result = service(executor).execute(query("id" to "n1"))
        assertEquals(DbResult.Success(affectedCount = 7), result)
        assertEquals(1, executor.ops.size)
        assertIs<BoundOp.DocGet>(executor.ops.single())
    }

    @Test
    fun `an unknown procedure fails without touching the database`() = runTest {
        val executor = FakeExecutor { error("must not run") }
        val result = service(executor).execute(DbQuery("Nope"))
        assertIs<DbResult.Failure>(result)
        assertIs<DbError.InvalidQuery>(result.error)
        assertTrue(result.error.devMessage.contains("Unknown procedure 'Nope'"), result.error.devMessage)
        assertTrue(result.error.devMessage.contains("GetById"), result.error.devMessage)
        assertTrue(executor.ops.isEmpty())
    }

    @Test
    fun `a parameter of the wrong type fails without touching the database`() = runTest {
        val executor = FakeExecutor { error("must not run") }
        val result = service(executor).execute(query("id" to 42))
        assertIs<DbResult.Failure>(result)
        assertIs<DbError.InvalidQuery>(result.error)
        assertTrue(result.error.devMessage.contains("@id expects STRING, got Int"), result.error.devMessage)
        assertTrue(executor.ops.isEmpty())
    }

    @Test
    fun `a binding problem fails without touching the database`() = runTest {
        val executor = FakeExecutor { error("must not run") }
        val result = service(executor).execute(query("id" to ""))
        assertIs<DbResult.Failure>(result)
        assertIs<DbError.InvalidQuery>(result.error)
        assertTrue(executor.ops.isEmpty())
    }

    @Test
    fun `an exception from the executor becomes a failure result and is never thrown`() = runTest {
        val executor = FakeExecutor { throw IllegalStateException("kaboom") }
        val result = service(executor).execute(query("id" to "n1"))
        assertIs<DbResult.Failure>(result)
        assertIs<DbError.Unknown>(result.error)
        assertTrue(result.error.devMessage.contains("kaboom"))
    }

    @Test
    fun `a slow database returns a timeout failure`() = runTest {
        val executor = FakeExecutor {
            delay(10_000)
            DbResult.Success()
        }
        val result = service(executor, timeoutMs = 50).execute(query("id" to "n1"))
        assertIs<DbResult.Failure>(result)
        assertIs<DbError.Timeout>(result.error)
        assertTrue(result.error.devMessage.contains("50 ms"), result.error.devMessage)
    }

    @Test
    fun `a per query timeout overrides the default`() = runTest {
        val executor = FakeExecutor {
            delay(10_000)
            DbResult.Success()
        }
        val result = service(executor, timeoutMs = 5_000).execute(query("id" to "n1", timeoutMs = 20))
        assertIs<DbResult.Failure>(result)
        assertTrue(result.error.devMessage.contains("20 ms"), result.error.devMessage)
    }

    @Test
    fun `plans that failed to load turn into a failure result`() = runTest {
        val broken = CompletableDeferred<Map<String, Plan>>().apply {
            completeExceptionally(IllegalStateException("bad plan file"))
        }
        val svc = FirebaseServiceImpl(broken, FakeExecutor { error("must not run") }, { dummyFirestore() }, 1_000)
        val result = svc.execute(query("id" to "n1"))
        assertIs<DbResult.Failure>(result)
        assertIs<DbError.Unknown>(result.error)
        assertTrue(result.error.devMessage.contains("bad plan file"), result.error.devMessage)
    }

    @Test
    fun `cancelling the caller is not swallowed`() = runTest {
        var result: DbResult? = null
        val executor = FakeExecutor { awaitCancellation() }
        val job = launch { result = service(executor).execute(query("id" to "n1")) }
        runCurrent()
        job.cancel()
        job.join()
        assertNull(result)
    }

    // ------------------------------------------------------------------ raw

    @Test
    fun `raw returns the block value`() = runTest {
        val result = service(FakeExecutor { DbResult.Success() }).raw { 42 }
        assertEquals(RawResult.Success(42), result)
    }

    @Test
    fun `a null raw result is a success not a timeout`() = runTest {
        val result = service(FakeExecutor { DbResult.Success() }).raw<String?> { null }
        assertEquals(RawResult.Success<String?>(null), result)
    }

    @Test
    fun `raw maps exceptions and timeouts`() = runTest {
        val svc = service(FakeExecutor { DbResult.Success() }, timeoutMs = 50)
        val failed = svc.raw<Int> { throw IllegalArgumentException("bad path") }
        assertIs<RawResult.Failure>(failed)
        assertIs<DbError.InvalidQuery>(failed.error)

        val slow = svc.raw<Int> {
            delay(10_000)
            1
        }
        assertIs<RawResult.Failure>(slow)
        assertIs<DbError.Timeout>(slow.error)
    }
}
