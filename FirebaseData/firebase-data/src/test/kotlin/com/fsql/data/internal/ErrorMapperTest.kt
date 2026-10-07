package com.fsql.data.internal

import com.fsql.data.DbError
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ErrorMapperTest {

    private fun firestore(code: Code, message: String = "boom") = FirebaseFirestoreException(message, code)

    @Test
    fun `each Firestore code maps to the documented error`() {
        assertIs<DbError.Network>(ErrorMapper.map(firestore(Code.UNAVAILABLE)))
        assertIs<DbError.Timeout>(ErrorMapper.map(firestore(Code.DEADLINE_EXCEEDED)))
        assertIs<DbError.PermissionDenied>(ErrorMapper.map(firestore(Code.PERMISSION_DENIED)))
        assertIs<DbError.Unauthenticated>(ErrorMapper.map(firestore(Code.UNAUTHENTICATED)))
        assertIs<DbError.NotFound>(ErrorMapper.map(firestore(Code.NOT_FOUND)))
        assertIs<DbError.QuotaExceeded>(ErrorMapper.map(firestore(Code.RESOURCE_EXHAUSTED)))
        assertIs<DbError.Conflict>(ErrorMapper.map(firestore(Code.ALREADY_EXISTS)))
        assertIs<DbError.Conflict>(ErrorMapper.map(firestore(Code.ABORTED)))
        assertIs<DbError.InvalidQuery>(ErrorMapper.map(firestore(Code.INVALID_ARGUMENT)))
        for (code in listOf(Code.INTERNAL, Code.UNKNOWN, Code.CANCELLED)) {
            assertIs<DbError.Unknown>(ErrorMapper.map(firestore(code)), code.name)
        }
    }

    @Test
    fun `a missing index error carries the console link`() {
        val link = "https://console.firebase.google.com/v1/r/project/p/firestore/indexes?create_composite=abc"
        val error = ErrorMapper.map(
            firestore(Code.FAILED_PRECONDITION, "The query requires an index. You can create it here: $link.")
        )
        assertIs<DbError.MissingIndex>(error)
        assertEquals(link, error.indexLink)
    }

    @Test
    fun `an index error without a link still maps to MissingIndex`() {
        val error = ErrorMapper.map(firestore(Code.FAILED_PRECONDITION, "This query needs an INDEX."))
        assertIs<DbError.MissingIndex>(error)
        assertNull(error.indexLink)
    }

    @Test
    fun `a failed precondition without the word index is a conflict`() {
        assertIs<DbError.Conflict>(ErrorMapper.map(firestore(Code.FAILED_PRECONDITION, "something else")))
    }

    @Test
    fun `the original exception is kept as the cause`() {
        val original = firestore(Code.PERMISSION_DENIED, "Missing or insufficient permissions.")
        val error = ErrorMapper.map(original)
        assertSame(original, error.cause)
        assertTrue(error.devMessage.contains("PERMISSION_DENIED"), error.devMessage)
    }

    @Test
    fun `network style exceptions map to Network`() {
        assertIs<DbError.Network>(ErrorMapper.map(IOException("socket closed")))
    }

    @Test
    fun `values Firestore cannot store map to InvalidQuery`() {
        assertIs<DbError.InvalidQuery>(ErrorMapper.map(IllegalArgumentException("Invalid document reference")))
    }

    @Test
    fun `anything else is Unknown and never throws`() {
        assertIs<DbError.Unknown>(ErrorMapper.map(IllegalStateException("oops")))
        assertIs<DbError.Unknown>(ErrorMapper.map(RuntimeException()))
    }
}
