package com.fsql.compiler

import com.fsql.plan.ParamDecl
import com.fsql.plan.ParamType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HeaderParserTest {

    @Test
    fun `no header means no params and the whole text is the body`() {
        val result = HeaderParser.parse("SELECT * FROM notes")
        assertEquals(emptyList(), result.params)
        assertEquals("SELECT * FROM notes", result.body)
    }

    @Test
    fun `one required param`() {
        val result = HeaderParser.parse(
            """
            -- @id: STRING
            SELECT * FROM notes WHERE id = @id
            """.trimIndent()
        )
        assertEquals(listOf(ParamDecl("id", ParamType.STRING, nullable = false)), result.params)
        assertEquals("SELECT * FROM notes WHERE id = @id", result.body)
    }

    @Test
    fun `multiple params including nullable ones and every type`() {
        val result = HeaderParser.parse(
            """
            -- @minAge: INT
            -- @city: STRING (nullable)
            -- @limit: INT
            -- @score: FLOAT (nullable)
            -- @active: BOOL
            -- @since: TIMESTAMP
            SELECT * FROM users
            """.trimIndent()
        )
        assertEquals(
            listOf(
                ParamDecl("minAge", ParamType.INT, false),
                ParamDecl("city", ParamType.STRING, true),
                ParamDecl("limit", ParamType.INT, false),
                ParamDecl("score", ParamType.FLOAT, true),
                ParamDecl("active", ParamType.BOOL, false),
                ParamDecl("since", ParamType.TIMESTAMP, false),
            ),
            result.params,
        )
        assertEquals("SELECT * FROM users", result.body)
    }

    @Test
    fun `a list parameter uses the TYPE bracket syntax`() {
        val result = HeaderParser.parse("-- @tags: STRING[]\nSELECT * FROM t WHERE tag IN (@tags)")
        assertEquals(listOf(ParamDecl("tags", ParamType.STRING, nullable = false, isList = true)), result.params)
    }

    @Test
    fun `a list parameter can also be nullable`() {
        val result = HeaderParser.parse("-- @tags: STRING[] (nullable)\nSELECT 1")
        assertEquals(listOf(ParamDecl("tags", ParamType.STRING, nullable = true, isList = true)), result.params)
    }

    @Test
    fun `header lines are case insensitive for the type and the nullable marker`() {
        val result = HeaderParser.parse("-- @x: string (NULLABLE)\nSELECT 1")
        assertEquals(listOf(ParamDecl("x", ParamType.STRING, true)), result.params)
    }

    @Test
    fun `a plain comment that is not a param declaration ends the header`() {
        val result = HeaderParser.parse(
            """
            -- @id: STRING
            -- just a note, not a param
            SELECT * FROM notes WHERE id = @id
            """.trimIndent()
        )
        assertEquals(listOf(ParamDecl("id", ParamType.STRING, false)), result.params)
        assertEquals("-- just a note, not a param\nSELECT * FROM notes WHERE id = @id", result.body)
    }

    @Test
    fun `blank lines end the header even if params would follow`() {
        val result = HeaderParser.parse("-- @id: STRING\n\n-- @other: STRING\nSELECT 1")
        assertEquals(listOf(ParamDecl("id", ParamType.STRING, false)), result.params)
        assertEquals("\n-- @other: STRING\nSELECT 1", result.body)
    }

    @Test
    fun `an unknown type is rejected with a message naming the file line and the bad word`() {
        val e = assertFailsWith<FsqlFileException> {
            HeaderParser.parse("-- @x: WEIRD\nSELECT 1", fileName = "Bad.fsql")
        }
        assertEquals("Bad.fsql", e.fileName)
        assertEquals(1, e.line)
        assert(e.message!!.contains("WEIRD")) { e.message!! }
        assert(e.message!!.contains("STRING, INT, FLOAT, BOOL, TIMESTAMP")) { e.message!! }
    }

    @Test
    fun `a duplicate parameter name is rejected`() {
        val e = assertFailsWith<FsqlFileException> {
            HeaderParser.parse("-- @id: STRING\n-- @id: INT\nSELECT 1", fileName = "Bad.fsql")
        }
        assertEquals(2, e.line)
        assert(e.message!!.contains("@id")) { e.message!! }
        assert(e.message!!.contains("twice")) { e.message!! }
    }

    @Test
    fun `an empty file is rejected`() {
        val e = assertFailsWith<FsqlFileException> { HeaderParser.parse("   \n  ", fileName = "Empty.fsql") }
        assert(e.message!!.contains("empty")) { e.message!! }
    }

    @Test
    fun `at param substitution turns at names into colon names for the sql parser`() {
        assertEquals("SELECT * FROM t WHERE id = :id", HeaderParser.substituteParams("SELECT * FROM t WHERE id = @id"))
        assertEquals(
            "SELECT * FROM t WHERE a = :a AND b = :b_2",
            HeaderParser.substituteParams("SELECT * FROM t WHERE a = @a AND b = @b_2"),
        )
    }
}
