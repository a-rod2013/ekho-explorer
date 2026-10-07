package com.fsql.compiler

import com.fsql.plan.AggKind
import com.fsql.plan.Cond
import com.fsql.plan.FilterOp
import com.fsql.plan.Literal
import com.fsql.plan.Operand
import com.fsql.plan.OrderBy
import com.fsql.plan.ParamDecl
import com.fsql.plan.ParamType
import com.fsql.plan.PlanBody
import com.fsql.plan.PlanValue
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlanBuilderTest {

    private fun build(sql: String, decls: List<ParamDecl> = emptyList()): PlanBody {
        val statement = CCJSqlParserUtil.parse(HeaderParser.substituteParams(sql))
        return PlanBuilder.bind("P", statement, decls)
    }

    private fun rejected(sql: String, decls: List<ParamDecl> = emptyList()): String {
        val e = assertFailsWith<FsqlFileException> { build(sql, decls) }
        return e.message!!
    }

    private fun cmp(field: String, op: FilterOp, rhs: Operand) = Cond.Cmp(field, op, rhs)
    private fun lit(n: Long) = Operand.Lit(Literal.Int64(n))
    private fun lit(s: String) = Operand.Lit(Literal.Str(s))
    private fun param(name: String) = Operand.Param(name)
    private fun p(name: String, type: ParamType = ParamType.STRING, nullable: Boolean = false, isList: Boolean = false) =
        ParamDecl(name, type, nullable, isList)

    // ------------------------------------------------------------ direct document access (shape only)

    @Test
    fun `id equality produces a DocGet with the id as an operand`() {
        val body = build("SELECT * FROM notes WHERE id = @id", listOf(p("id")))
        assertEquals(PlanBody.DocGet("notes", param("id"), null), body)
    }

    @Test
    fun `a column list on a direct read is kept`() {
        val body = build("SELECT title, body FROM notes WHERE id = @id", listOf(p("id")))
        assertEquals(PlanBody.DocGet("notes", param("id"), listOf("title", "body")), body)
    }

    @Test
    fun `id equality with a literal string produces a literal operand`() {
        val body = build("SELECT * FROM notes WHERE id = 'n1'")
        assertEquals(PlanBody.DocGet("notes", lit("n1"), null), body)
    }

    // ------------------------------------------------------------ plain reads (shape only)

    @Test
    fun `select star with a filter order by and limit`() {
        val body = build(
            "SELECT * FROM users WHERE age >= @minAge ORDER BY age DESC LIMIT 20",
            listOf(p("minAge", ParamType.INT)),
        )
        assertEquals(
            PlanBody.Read("users", cmp("age", FilterOp.GTE, param("minAge")), listOf(OrderBy("age", true)), lit(20L), null),
            body,
        )
    }

    @Test
    fun `and or in and is null`() {
        val body = build("SELECT * FROM t WHERE a = 1 AND ((b IN (2, 3)) OR c IS NULL)")
        assertEquals(
            PlanBody.Read(
                "t",
                Cond.And(
                    listOf(
                        cmp("a", FilterOp.EQ, lit(1L)),
                        Cond.Or(listOf(Cond.InList("b", items = listOf(lit(2L), lit(3L))), Cond.IsNull("c", false))),
                    )
                ),
                emptyList(), null, null,
            ),
            body,
        )
    }

    @Test
    fun `IN combined with OR without its own parens is rejected with a message explaining the fix`() {
        assertEquals(
            "P: IN combined with OR or AND needs its own parentheses. Write it as " +
                    "(column IN (...)) OR other = ..., not column IN (...) OR other = ...",
            rejected("SELECT * FROM t WHERE b IN (2, 3) OR c IS NULL"),
        )
    }

    @Test
    fun `is not null`() {
        val body = build("SELECT * FROM t WHERE a IS NOT NULL")
        assertEquals(PlanBody.Read("t", Cond.IsNull("a", true), emptyList(), null, null), body)
    }

    @Test
    fun `not equals`() {
        val body = build("SELECT * FROM t WHERE a <> 1")
        assertEquals(PlanBody.Read("t", cmp("a", FilterOp.NEQ, lit(1L)), emptyList(), null, null), body)
    }

    @Test
    fun `a param guard produces a ParamIsNull node`() {
        val body = build(
            "SELECT * FROM users WHERE (@city IS NULL OR city = @city)",
            listOf(p("city", nullable = true)),
        )
        assertEquals(
            PlanBody.Read(
                "users",
                Cond.Or(listOf(Cond.ParamIsNull("city", false), cmp("city", FilterOp.EQ, param("city")))),
                emptyList(), null, null,
            ),
            body,
        )
    }

    @Test
    fun `in with a single list parameter produces a listParam node`() {
        val body = build("SELECT * FROM t WHERE tag IN (@ids)", listOf(p("ids", isList = true)))
        assertEquals(PlanBody.Read("t", Cond.InList("tag", listParam = "ids"), emptyList(), null, null), body)
    }

    @Test
    fun `in with literal values produces literal operands`() {
        val body = build("SELECT * FROM t WHERE tag IN ('a', 'b')")
        assertEquals(PlanBody.Read("t", Cond.InList("tag", items = listOf(lit("a"), lit("b"))), emptyList(), null, null), body)
    }

    // ------------------------------------------------------------------- aggregates (shape only)

    @Test
    fun `count star`() {
        val body = build("SELECT COUNT(*) FROM orders WHERE status = 'open'")
        assertEquals(PlanBody.Aggregate("orders", cmp("status", FilterOp.EQ, lit("open")), AggKind.COUNT, null), body)
    }

    @Test
    fun `sum and avg need exactly one column`() {
        assertEquals(PlanBody.Aggregate("t", null, AggKind.SUM, "price"), build("SELECT SUM(price) FROM t"))
        assertEquals(PlanBody.Aggregate("t", null, AggKind.AVG, "price"), build("SELECT AVG(price) FROM t"))
    }

    @Test
    fun `count of a column is rejected`() {
        assertEquals("P: Use COUNT(*), not COUNT(column).", rejected("SELECT COUNT(id) FROM t"))
    }

    @Test
    fun `an aggregate cannot be combined with a plain column`() {
        assertEquals(
            "P: An aggregate (COUNT, SUM, AVG) cannot be combined with plain columns.",
            rejected("SELECT COUNT(*), name FROM t"),
        )
    }

    // ------------------------------------------------------------------------ insert (shape only)

    @Test
    fun `insert without an id`() {
        val body = build(
            "INSERT INTO notes (title, body) VALUES (@title, @body)",
            listOf(p("title"), p("body", nullable = true)),
        )
        assertEquals(
            PlanBody.Insert("notes", null, mapOf("title" to PlanValue.Op(param("title")), "body" to PlanValue.Op(param("body")))),
            body,
        )
    }

    @Test
    fun `insert with an explicit id and a server timestamp`() {
        val body = build(
            "INSERT INTO notes (id, title, createdAt) VALUES (@id, @title, NOW())",
            listOf(p("id"), p("title")),
        )
        assertEquals(
            PlanBody.Insert("notes", param("id"), mapOf("title" to PlanValue.Op(param("title")), "createdAt" to PlanValue.ServerTimestamp)),
            body,
        )
    }

    @Test
    fun `insert column and value counts must match`() {
        assertEquals("P: The statement lists 2 columns but 1 values.", rejected("INSERT INTO notes (a, b) VALUES (1)"))
    }

    // ------------------------------------------------------------------------ update (shape only)

    @Test
    fun `update by id with an increment keeps the sign at compile time`() {
        val body = build(
            "UPDATE counters SET value = value + @by WHERE id = @id",
            listOf(p("id"), p("by", ParamType.INT)),
        )
        assertEquals(PlanBody.UpdateDoc("counters", param("id"), mapOf("value" to PlanValue.Increment("value", 1, param("by")))), body)
    }

    @Test
    fun `decrement has sign -1`() {
        val body = build(
            "UPDATE counters SET value = value - @by WHERE id = @id",
            listOf(p("id"), p("by", ParamType.INT)),
        )
        assertEquals(PlanValue.Increment("value", -1, param("by")), (body as PlanBody.UpdateDoc).sets["value"])
    }

    @Test
    fun `update by filter is a bulk update`() {
        val body = build(
            "UPDATE notes SET archived = @a WHERE title = @t",
            listOf(p("a", ParamType.BOOL), p("t")),
        )
        assertEquals(
            PlanBody.BulkUpdate("notes", cmp("title", FilterOp.EQ, param("t")), mapOf("archived" to PlanValue.Op(param("a")))),
            body,
        )
    }

    @Test
    fun `update without a where is rejected`() {
        assertEquals(
            "P: UPDATE needs a WHERE clause. Firestore has no way to update every document safely from one call.",
            rejected("UPDATE notes SET a = 1"),
        )
    }

    @Test
    fun `an unrecognised arithmetic expression is rejected`() {
        assertEquals(
            "P: Only 'col = col + @n' or 'col = col - @n' is supported as arithmetic in SET.",
            rejected("UPDATE t SET a = b + 1 WHERE id = 'x'"),
        )
    }

    // ------------------------------------------------------------------------ delete (shape only)

    @Test
    fun `delete by id`() {
        assertEquals(PlanBody.DeleteDoc("notes", lit("n1")), build("DELETE FROM notes WHERE id = 'n1'"))
    }

    @Test
    fun `delete by filter is bulk`() {
        val body = build("DELETE FROM notes WHERE title = @t", listOf(p("t")))
        assertEquals(PlanBody.BulkDelete("notes", cmp("title", FilterOp.EQ, param("t"))), body)
    }

    @Test
    fun `delete without a where is rejected`() {
        assertEquals(
            "P: DELETE needs a WHERE clause. Firestore has no way to delete every document safely from one call.",
            rejected("DELETE FROM notes"),
        )
    }

    // ------------------------------------------------------------------------ rejections (unchanged)

    @Test
    fun `a dotted nested field name is rejected instead of being silently truncated`() {
        assertEquals(
            "P: 'address.city' looks like a nested field. Dotted field names are not " +
                    "supported in v1; use a plain top-level field name.",
            rejected("SELECT * FROM users WHERE address.city = 'Oslo'"),
        )
    }

    @Test
    fun `joins are rejected`() {
        assertEquals(
            "P: JOIN is not supported. Firestore has no joins; read from each collection separately.",
            rejected("SELECT * FROM a JOIN b ON a.x = b.x"),
        )
    }

    @Test
    fun `subqueries are rejected`() {
        assertEquals(
            "P: A subquery is not supported. Read it as a separate procedure.",
            rejected("SELECT * FROM t WHERE a IN (SELECT b FROM u)"),
        )
    }

    @Test
    fun `group by is rejected`() {
        assertEquals(
            "P: GROUP BY is not supported. Firestore only supports whole-query COUNT, SUM and AVG.",
            rejected("SELECT a, COUNT(*) FROM t GROUP BY a"),
        )
    }

    @Test
    fun `an unsupported statement type is rejected`() {
        val e = assertFailsWith<FsqlFileException> {
            PlanBuilder.bind("P", CCJSqlParserUtil.parse("CREATE TABLE t (a INT)"), emptyList())
        }
        assert(e.message!!.contains("not a SELECT, INSERT, UPDATE or DELETE")) { e.message!! }
    }
}
