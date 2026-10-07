package com.fsql.data.internal

import com.google.firebase.Timestamp
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParamCheckerTest {

    private val decls = listOf(
        ParamDecl("name", ParamType.STRING, nullable = false),
        ParamDecl("age", ParamType.INT, nullable = false),
        ParamDecl("score", ParamType.FLOAT, nullable = false),
        ParamDecl("active", ParamType.BOOL, nullable = false),
        ParamDecl("born", ParamType.TIMESTAMP, nullable = false),
        ParamDecl("city", ParamType.STRING, nullable = true),
    )

    private val valid: List<Pair<String, Any?>> = listOf(
        "name" to "Ann", "age" to 30, "score" to 1.5, "active" to true,
        "born" to Timestamp(10, 0), "city" to null,
    )

    private fun ok(supplied: List<Pair<String, Any?>>): Map<String, Any?> {
        val outcome = ParamChecker.check("Proc", decls, supplied)
        assertIs<ParamChecker.Outcome.Ok>(outcome, outcome.toString())
        return outcome.values
    }

    private fun invalid(supplied: List<Pair<String, Any?>>): String {
        val outcome = ParamChecker.check("Proc", decls, supplied)
        assertIs<ParamChecker.Outcome.Invalid>(outcome, outcome.toString())
        return outcome.message
    }

    private fun with(vararg changes: Pair<String, Any?>): List<Pair<String, Any?>> =
        valid.filter { (k, _) -> changes.none { it.first == k } } + changes

    @Test
    fun `valid values pass through with normalised types`() {
        val values = ok(valid)
        assertEquals("Ann", values["name"])
        assertEquals(30L, values["age"])
        assertEquals(1.5, values["score"])
        assertEquals(true, values["active"])
        assertEquals(Timestamp(10, 0), values["born"])
        assertEquals(null, values["city"])
    }

    @Test
    fun `numbers are converted to the declared type`() {
        assertEquals(7L, ok(with("age" to 7.toShort()))["age"])
        assertEquals(3.0, ok(with("score" to 3))["score"])
        assertEquals(2.5, ok(with("score" to 2.5f))["score"])
    }

    @Test
    fun `an int passed for a string parameter is rejected with a clear message`() {
        val message = invalid(with("name" to 42))
        assertTrue(message.startsWith("Proc: "), message)
        assertTrue(message.contains("@name expects STRING, got Int"), message)
    }

    @Test
    fun `strict types are enforced for every declared type`() {
        assertTrue(invalid(with("age" to "5")).contains("@age expects INT, got String"))
        assertTrue(invalid(with("age" to 1.5)).contains("@age expects INT, got Double"))
        assertTrue(invalid(with("active" to 1)).contains("@active expects BOOL, got Int"))
        assertTrue(invalid(with("score" to "x")).contains("@score expects FLOAT, got String"))
    }

    @Test
    fun `dates and timestamps are both accepted for TIMESTAMP`() {
        val values = ok(with("born" to Date(5_000L)))
        assertEquals(Timestamp(Date(5_000L)), values["born"])
        assertTrue(invalid(with("born" to "2020-01-01")).contains("@born expects TIMESTAMP, got String"))
    }

    @Test
    fun `null is only accepted for nullable parameters`() {
        assertEquals(null, ok(with("city" to null))["city"])
        val message = invalid(with("name" to null))
        assertTrue(message.contains("@name is null, but STRING does not allow null"), message)
    }

    @Test
    fun `missing unknown and duplicate parameters are reported`() {
        val missing = invalid(valid.filter { it.first != "name" && it.first != "age" })
        assertTrue(missing.contains("missing required parameter(s): @name, @age"), missing)

        val unknown = invalid(valid + ("nmae" to "x"))
        assertTrue(unknown.contains("unknown parameter(s) 'nmae'"), unknown)

        val duplicate = invalid(valid + ("name" to "again"))
        assertTrue(duplicate.contains("'name' was supplied more than once"), duplicate)
    }

    @Test
    fun `every problem is reported in one message`() {
        val message = invalid(with("name" to 1, "age" to "x") + ("bogus" to 1))
        assertTrue(message.contains("@name expects"), message)
        assertTrue(message.contains("@age expects"), message)
        assertTrue(message.contains("unknown parameter(s) 'bogus'"), message)
    }

    @Test
    fun `a procedure without parameters accepts none and rejects any`() {
        val outcome = ParamChecker.check("None", emptyList(), emptyList())
        assertIs<ParamChecker.Outcome.Ok>(outcome)
        val bad = ParamChecker.check("None", emptyList(), listOf("x" to 1))
        assertIs<ParamChecker.Outcome.Invalid>(bad)
        assertTrue(bad.message.contains("the procedure declares: none"), bad.message)
    }
}
