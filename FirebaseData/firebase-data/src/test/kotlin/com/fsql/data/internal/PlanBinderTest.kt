package com.fsql.data.internal

import com.fsql.plan.AggKind
import com.fsql.plan.Cond
import com.fsql.plan.FilterOp
import com.fsql.plan.Literal
import com.fsql.plan.Operand
import com.fsql.plan.OrderBy
import com.fsql.plan.Plan
import com.fsql.plan.PlanBody
import com.fsql.plan.PlanValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlanBinderTest {

    private fun bind(body: PlanBody, values: Map<String, Any?> = emptyMap()): BoundOp {
        val result = PlanBinder.bind(Plan("P", emptyList(), body), values)
        assertIs<PlanBinder.Result.Ok>(result, result.toString())
        return result.op
    }

    private fun invalid(body: PlanBody, values: Map<String, Any?> = emptyMap()): String {
        val result = PlanBinder.bind(Plan("P", emptyList(), body), values)
        assertIs<PlanBinder.Result.Invalid>(result, result.toString())
        return result.message
    }

    private fun leaf(field: String, op: FilterOp, value: Any?) = BoundFilter.Leaf(field, op, value)
    private fun param(name: String) = Operand.Param(name)
    private fun lit(n: Long) = Operand.Lit(Literal.Int64(n))
    private fun lit(s: String) = Operand.Lit(Literal.Str(s))

    // ------------------------------------------------------------ direct document access

    @Test
    fun `a parameter id resolves to a string`() {
        val op = bind(PlanBody.DocGet("notes", param("id"), null), mapOf("id" to "n1"))
        assertEquals(BoundOp.DocGet("notes", "n1", null), op)
    }

    @Test
    fun `columns pass through unchanged`() {
        val op = bind(PlanBody.DocGet("notes", param("id"), listOf("title", "body")), mapOf("id" to "n1"))
        assertEquals(BoundOp.DocGet("notes", "n1", listOf("title", "body")), op)
    }

    @Test
    fun `a literal id resolves directly, with no values needed`() {
        val op = bind(PlanBody.DocGet("notes", lit("n1"), null))
        assertEquals(BoundOp.DocGet("notes", "n1", null), op)
    }

    @Test
    fun `a non string id is rejected`() {
        assertEquals(
            "P: The id must be a string.",
            invalid(PlanBody.DocGet("notes", param("id"), null), mapOf("id" to 5L)),
        )
    }

    @Test
    fun `an empty string id is rejected`() {
        assertEquals(
            "P: The id must not be empty.",
            invalid(PlanBody.DocGet("notes", param("id"), null), mapOf("id" to "")),
        )
    }

    // ------------------------------------------------------------ plain reads

    @Test
    fun `a comparison resolves its operand and keeps order by and limit`() {
        val op = bind(
            PlanBody.Read(
                "users",
                Cond.Cmp("age", FilterOp.GTE, param("minAge")),
                listOf(OrderBy("age", true)),
                lit(20L),
                null,
            ),
            mapOf("minAge" to 18L),
        )
        assertEquals(BoundOp.Read("users", leaf("age", FilterOp.GTE, 18L), listOf(OrderBy("age", true)), 20, null), op)
    }

    @Test
    fun `and or in and is null resolve together`() {
        val op = bind(
            PlanBody.Read(
                "t",
                Cond.And(
                    listOf(
                        Cond.Cmp("a", FilterOp.EQ, lit(1L)),
                        Cond.Or(listOf(Cond.InList("b", items = listOf(lit(2L), lit(3L))), Cond.IsNull("c", false))),
                    )
                ),
                emptyList(), null, null,
            ),
        )
        assertEquals(
            BoundOp.Read(
                "t",
                BoundFilter.And(
                    listOf(
                        leaf("a", FilterOp.EQ, 1L),
                        BoundFilter.Or(listOf(leaf("b", FilterOp.IN, listOf(2L, 3L)), leaf("c", FilterOp.IS_NULL, null))),
                    )
                ),
                emptyList(), null, null,
            ),
            op,
        )
    }

    @Test
    fun `a list parameter resolves to its supplied list`() {
        val op = bind(
            PlanBody.Read("t", Cond.InList("tag", listParam = "ids"), emptyList(), null, null),
            mapOf("ids" to listOf("a", "b")),
        )
        assertEquals(BoundOp.Read("t", leaf("tag", FilterOp.IN, listOf("a", "b")), emptyList(), null, null), op)
    }

    @Test
    fun `an empty list parameter matches nothing`() {
        val op = bind(
            PlanBody.Read("t", Cond.InList("tag", listParam = "ids"), emptyList(), null, null),
            mapOf("ids" to emptyList<String>()),
        )
        assertEquals(BoundOp.Empty, op)
    }

    @Test
    fun `limit resolves from a parameter too`() {
        val op = bind(PlanBody.Read("t", null, emptyList(), param("n"), null), mapOf("n" to 5))
        assertEquals(BoundOp.Read("t", null, emptyList(), 5, null), op)
    }

    @Test
    fun `limit must be at least 1`() {
        assertEquals("P: LIMIT must be at least 1.", invalid(PlanBody.Read("t", null, emptyList(), lit(0L), null)))
    }

    // ---------------------------------------------------- optional filter folding

    @Test
    fun `an optional filter disappears when its parameter is null`() {
        val where = Cond.Or(listOf(Cond.ParamIsNull("city", false), Cond.Cmp("city", FilterOp.EQ, param("city"))))
        val op = bind(PlanBody.Read("users", where, emptyList(), null, null), mapOf("city" to null))
        assertEquals(BoundOp.Read("users", null, emptyList(), null, null), op)
    }

    @Test
    fun `an optional filter applies when its parameter is supplied`() {
        val where = Cond.Or(listOf(Cond.ParamIsNull("city", false), Cond.Cmp("city", FilterOp.EQ, param("city"))))
        val op = bind(PlanBody.Read("users", where, emptyList(), null, null), mapOf("city" to "Oslo"))
        assertEquals(BoundOp.Read("users", leaf("city", FilterOp.EQ, "Oslo"), emptyList(), null, null), op)
    }

    @Test
    fun `folding does not depend on which side of OR the guard is on`() {
        val where = Cond.Or(listOf(Cond.Cmp("city", FilterOp.EQ, param("city")), Cond.ParamIsNull("city", false)))
        val op = bind(PlanBody.Read("users", where, emptyList(), null, null), mapOf("city" to null))
        assertEquals(BoundOp.Read("users", null, emptyList(), null, null), op)
    }

    @Test
    fun `a false guard means no rows without asking the database`() {
        val where = Cond.And(listOf(Cond.ParamIsNull("c", true), Cond.Cmp("city", FilterOp.EQ, param("c"))))
        val op = bind(PlanBody.Read("users", where, emptyList(), null, null), mapOf("c" to null))
        assertEquals(BoundOp.Empty, op)
    }

    @Test
    fun `a false guard also short circuits a bulk update or delete`() {
        val where = Cond.And(listOf(Cond.ParamIsNull("t", true), Cond.Cmp("title", FilterOp.EQ, param("t"))))
        assertEquals(
            BoundOp.Empty,
            bind(PlanBody.BulkUpdate("notes", where, mapOf("a" to PlanValue.Op(lit(1L)))), mapOf("t" to null)),
        )
        assertEquals(BoundOp.Empty, bind(PlanBody.BulkDelete("notes", where), mapOf("t" to null)))
    }

    @Test
    fun `an unguarded null parameter in a comparison is rejected with a hint to use IS NULL`() {
        val where = Cond.Cmp("city", FilterOp.EQ, param("c"))
        assertEquals(
            "P: @c is null in a comparison on 'city'. Use IS NULL, or guard it with (@c IS NULL OR ...).",
            invalid(PlanBody.Read("users", where, emptyList(), null, null), mapOf("c" to null)),
        )
    }

    // ------------------------------------------------------------------- aggregates

    @Test
    fun `an aggregate resolves its filter like a read`() {
        val op = bind(PlanBody.Aggregate("orders", Cond.Cmp("status", FilterOp.EQ, lit("open")), AggKind.COUNT, null))
        assertEquals(BoundOp.Aggregate("orders", leaf("status", FilterOp.EQ, "open"), AggKind.COUNT, null), op)
    }

    @Test
    fun `an aggregate whose filter folds to always false is an empty aggregate with no Firestore call`() {
        val where = Cond.And(listOf(Cond.ParamIsNull("s", true), Cond.Cmp("status", FilterOp.EQ, param("s"))))
        val op = bind(PlanBody.Aggregate("orders", where, AggKind.COUNT, null), mapOf("s" to null))
        assertEquals(BoundOp.EmptyAggregate(AggKind.COUNT), op)
    }

    // ------------------------------------------------------------------------ insert

    @Test
    fun `insert without an id resolves every value`() {
        val op = bind(
            PlanBody.Insert("notes", null, mapOf("title" to PlanValue.Op(param("title")), "body" to PlanValue.Op(param("body")))),
            mapOf("title" to "Hi", "body" to null),
        )
        assertEquals(
            BoundOp.Insert("notes", null, mapOf("title" to BoundValue.Plain("Hi"), "body" to BoundValue.Plain(null))),
            op,
        )
    }

    @Test
    fun `insert with an explicit id and a server timestamp`() {
        val op = bind(
            PlanBody.Insert("notes", param("id"), mapOf("title" to PlanValue.Op(param("title")), "createdAt" to PlanValue.ServerTimestamp)),
            mapOf("id" to "k1", "title" to "Hi"),
        )
        assertEquals(
            BoundOp.Insert("notes", "k1", mapOf("title" to BoundValue.Plain("Hi"), "createdAt" to BoundValue.ServerTimestamp)),
            op,
        )
    }

    // ------------------------------------------------------------------------ update

    @Test
    fun `update by id resolves an increment with its sign`() {
        val op = bind(
            PlanBody.UpdateDoc("counters", param("id"), mapOf("value" to PlanValue.Increment("value", 1, param("by")))),
            mapOf("id" to "c1", "by" to 5L),
        )
        assertEquals(BoundOp.UpdateDoc("counters", "c1", mapOf("value" to BoundValue.Increment(5L))), op)
    }

    @Test
    fun `a negative sign negates the resolved amount`() {
        val op = bind(
            PlanBody.UpdateDoc("counters", param("id"), mapOf("value" to PlanValue.Increment("value", -1, param("by")))),
            mapOf("id" to "c1", "by" to 5L),
        )
        assertEquals(BoundValue.Increment(-5L), (op as BoundOp.UpdateDoc).sets["value"])
    }

    @Test
    fun `the increment amount must resolve to a number`() {
        assertEquals(
            "P: The amount added to 'value' must be a number.",
            invalid(
                PlanBody.UpdateDoc("counters", param("id"), mapOf("value" to PlanValue.Increment("value", 1, param("by")))),
                mapOf("id" to "c1", "by" to "oops"),
            ),
        )
    }

    @Test
    fun `update by filter is a bulk update`() {
        val op = bind(
            PlanBody.BulkUpdate("notes", Cond.Cmp("title", FilterOp.EQ, param("t")), mapOf("archived" to PlanValue.Op(param("a")))),
            mapOf("a" to true, "t" to "x"),
        )
        assertEquals(
            BoundOp.BulkUpdate("notes", leaf("title", FilterOp.EQ, "x"), mapOf("archived" to BoundValue.Plain(true))),
            op,
        )
    }

    @Test
    fun `a where that folds to matching everything is refused for a bulk update or delete`() {
        val where = Cond.Or(listOf(Cond.ParamIsNull("t", false), Cond.Cmp("title", FilterOp.EQ, param("t"))))
        assertEquals(
            "P: This WHERE matches every document once its NULL guards are applied. " +
                    "UPDATE ALL is not supported; narrow the WHERE so it always excludes some documents.",
            invalid(PlanBody.BulkUpdate("notes", where, mapOf("a" to PlanValue.Op(lit(1L)))), mapOf("t" to null)),
        )
        assertEquals(
            "P: This WHERE matches every document once its NULL guards are applied. " +
                    "DELETE ALL is not supported; narrow the WHERE so it always excludes some documents.",
            invalid(PlanBody.BulkDelete("notes", where), mapOf("t" to null)),
        )
    }

    // ------------------------------------------------------------------------ delete

    @Test
    fun `delete by id`() {
        assertEquals(BoundOp.DeleteDoc("notes", "n1"), bind(PlanBody.DeleteDoc("notes", lit("n1"))))
    }

    @Test
    fun `delete by filter is bulk`() {
        val op = bind(PlanBody.BulkDelete("notes", Cond.Cmp("title", FilterOp.EQ, param("t"))), mapOf("t" to "x"))
        assertEquals(BoundOp.BulkDelete("notes", leaf("title", FilterOp.EQ, "x")), op)
    }
}
