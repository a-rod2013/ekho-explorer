package com.fsql.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlanJsonTest {

    @Test
    fun `round trips every Cond, Operand, PlanValue and PlanBody variant`() {
        val where = Cond.And(
            listOf(
                Cond.Cmp("age", FilterOp.GTE, Operand.Param("minAge")),
                Cond.Or(
                    listOf(
                        Cond.ParamIsNull("city"),
                        Cond.Cmp("city", FilterOp.EQ, Operand.Param("city")),
                    )
                ),
                Cond.InList("tag", listOf(Operand.Lit(Literal.Str("a")), Operand.Lit(Literal.Int64(2)))),
                Cond.InList("tag2", listParam = "tags"),
                Cond.IsNull("x", negated = true),
            )
        )
        val plans = listOf(
            Plan(
                "GetUsers",
                listOf(
                    ParamDecl("minAge", ParamType.INT, nullable = false),
                    ParamDecl("city", ParamType.STRING, nullable = true),
                    ParamDecl("tags", ParamType.STRING, nullable = false, isList = true),
                ),
                PlanBody.Read(
                    "users",
                    where,
                    listOf(OrderBy("age", descending = true)),
                    Operand.Lit(Literal.Int64(20)),
                    listOf("name", "age"),
                ),
            ),
            Plan("GetById", listOf(ParamDecl("id", ParamType.STRING, false)),
                PlanBody.DocGet("users", Operand.Param("id"), null)),
            Plan("CountOpen", emptyList(),
                PlanBody.Aggregate("orders", Cond.Cmp("status", FilterOp.EQ, Operand.Lit(Literal.Str("open"))), AggKind.COUNT, null)),
            Plan("SumPrice", emptyList(), PlanBody.Aggregate("orders", null, AggKind.SUM, "price")),
            Plan(
                "InsertNote",
                listOf(ParamDecl("title", ParamType.STRING, false)),
                PlanBody.Insert(
                    "notes", null,
                    mapOf(
                        "title" to PlanValue.Op(Operand.Param("title")),
                        "createdAt" to PlanValue.ServerTimestamp,
                    ),
                ),
            ),
            Plan(
                "Bump",
                listOf(ParamDecl("id", ParamType.STRING, false), ParamDecl("by", ParamType.INT, false)),
                PlanBody.UpdateDoc(
                    "counters", Operand.Param("id"),
                    mapOf("value" to PlanValue.Increment("value", -1, Operand.Param("by"))),
                ),
            ),
            Plan("DeleteById", listOf(ParamDecl("id", ParamType.STRING, false)),
                PlanBody.DeleteDoc("notes", Operand.Param("id"))),
            Plan("BulkUp", listOf(ParamDecl("t", ParamType.STRING, false)),
                PlanBody.BulkUpdate("notes", Cond.Cmp("title", FilterOp.EQ, Operand.Param("t")), mapOf("a" to PlanValue.Op(Operand.Lit(Literal.Int64(1)))))),
            Plan("BulkDel", listOf(ParamDecl("t", ParamType.STRING, false)),
                PlanBody.BulkDelete("notes", Cond.Cmp("title", FilterOp.EQ, Operand.Param("t")))),
        )

        val decoded = PlanJson.decode(PlanJson.encode(plans))

        assertEquals(PLAN_FORMAT_VERSION, decoded.formatVersion)
        assertEquals(plans, decoded.plans)
    }

    @Test
    fun `decode rejects a file written by a newer format`() {
        val error = assertFailsWith<IllegalStateException> {
            PlanJson.decode("""{"formatVersion":99,"plans":[]}""")
        }
        assertTrue(error.message!!.contains("99"), error.message!!)
        assertTrue(error.message!!.contains("Rebuild"), error.message!!)
    }

    @Test
    fun `an empty plan list round trips`() {
        val decoded = PlanJson.decode(PlanJson.encode(emptyList()))
        assertEquals(emptyList(), decoded.plans)
    }
}
