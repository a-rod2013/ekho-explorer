package com.fsql.compiler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FsqlCompilerTest {

    private fun file(path: String, text: String) = SourceFile(path, text.trimIndent())

    private val getAll = "SELECT * FROM notes"

    @Test
    fun `good files compile to plans sorted by name`() {
        val result = FsqlCompiler.compile(
            listOf(
                file("/p/GetOne.fsql", "-- @id: STRING\nSELECT * FROM notes WHERE id = @id"),
                file("/p/GetAll.fsql", getAll),
            )
        )
        assertTrue(result.ok)
        assertEquals(listOf("GetAll", "GetOne"), result.plans.map { it.name })
    }

    @Test
    fun `every broken file is reported, and good files still compile`() {
        val result = FsqlCompiler.compile(
            listOf(
                file("/p/A.fsql", "SELECT * FROM a JOIN b ON a.x = b.x"),
                file("/p/B.fsql", getAll),
                file("/p/C.fsql", "-- @x: WEIRD\nSELECT 1"),
            )
        )
        assertFalse(result.ok)
        assertEquals(2, result.diagnostics.size)
        assertEquals("/p/A.fsql", result.diagnostics[0].file)
        assertTrue(result.diagnostics[0].message.contains("JOIN is not supported"), result.diagnostics[0].message)
        assertEquals("/p/C.fsql", result.diagnostics[1].file)
        assertEquals(1, result.diagnostics[1].line)
        assertTrue(result.diagnostics[1].message.contains("WEIRD"), result.diagnostics[1].message)
        assertEquals(listOf("B"), result.plans.map { it.name })
    }

    @Test
    fun `the same procedure name from two folders is an error`() {
        val result = FsqlCompiler.compile(
            listOf(file("/p/users/GetAll.fsql", getAll), file("/p/notes/GetAll.fsql", getAll)),
        )
        assertEquals(1, result.diagnostics.size)
        assertTrue(
            result.diagnostics.single().message.contains("already defines the procedure 'GetAll'"),
            result.diagnostics.single().message,
        )
        assertEquals(1, result.plans.size)
    }

    @Test
    fun `diagnostics format like a Kotlin compiler error so the IDE can link them`() {
        assertEquals("e: file:///C:/proj/Foo.fsql:3:1 boom", Diagnostic("C:\\proj\\Foo.fsql", 3, 1, "boom").format())
        assertEquals("e: file:///home/me/Foo.fsql:1:1 boom", Diagnostic("/home/me/Foo.fsql", 1, 1, "boom").format())
    }

    @Test
    fun `an unused declared parameter is caught here too`() {
        val result = FsqlCompiler.compile(listOf(file("/p/Bad.fsql", "-- @id: STRING\nSELECT * FROM notes")))
        assertTrue(result.diagnostics.single().message.contains("declared but never used"), result.diagnostics.single().message)
    }
}
