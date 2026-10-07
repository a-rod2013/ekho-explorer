package com.fsql.data.internal

import com.fsql.plan.Cond
import com.fsql.plan.Literal
import com.fsql.plan.Operand
import com.fsql.plan.Plan
import com.fsql.plan.PlanBody
import com.fsql.plan.PlanValue

/**
 * Turns a [Plan] plus already-checked parameter values into a [BoundOp], at call time. Pure Kotlin:
 * it never touches Firestore or JSqlParser.
 */
internal object PlanBinder {

    sealed interface Result {
        data class Ok(val op: BoundOp) : Result
        data class Invalid(val message: String) : Result
    }

    fun bind(plan: Plan, values: Map<String, Any?>): Result =
        try {
            Result.Ok(Binder(values).bind(plan.body))
        } catch (e: BindException) {
            Result.Invalid("${plan.name}: ${e.message}")
        }

    private class BindException(message: String) : Exception(message)

    private class Binder(private val values: Map<String, Any?>) {

        private fun fail(message: String): Nothing = throw BindException(message)

        fun bind(body: PlanBody): BoundOp = when (body) {
            is PlanBody.DocGet -> BoundOp.DocGet(body.collection, idValue(body.id), body.columns)
            is PlanBody.Read -> read(body)
            is PlanBody.Aggregate -> aggregate(body)
            is PlanBody.Insert -> insert(body)
            is PlanBody.UpdateDoc -> BoundOp.UpdateDoc(body.collection, idValue(body.id), boundValues(body.sets))
            is PlanBody.DeleteDoc -> BoundOp.DeleteDoc(body.collection, idValue(body.id))
            is PlanBody.BulkUpdate -> bulkUpdate(body)
            is PlanBody.BulkDelete -> bulkDelete(body)
        }

        // ---------------------------------------------------------------- SELECT

        private fun read(body: PlanBody.Read): BoundOp = when (val filter = evaluate(body.where)) {
            Evaluated.Never -> BoundOp.Empty
            is Evaluated.Filter -> BoundOp.Read(
                body.collection, filter.value, body.orderBy, body.limit?.let { limitValue(it) }, body.columns,
            )
        }

        private fun aggregate(body: PlanBody.Aggregate): BoundOp = when (val filter = evaluate(body.where)) {
            Evaluated.Never -> BoundOp.EmptyAggregate(body.kind)
            is Evaluated.Filter -> BoundOp.Aggregate(body.collection, filter.value, body.kind, body.field)
        }

        private fun limitValue(operand: Operand): Int {
            val n = (resolveScalar(operand) as? Number)?.toInt() ?: fail("LIMIT must not be null.")
            if (n < 1) fail("LIMIT must be at least 1.")
            return n
        }

        // INSERT

        private fun insert(body: PlanBody.Insert): BoundOp {
            val id = body.id?.let { idValue(it) }
            return BoundOp.Insert(body.collection, id, boundValues(body.values))
        }

        // bulk UPDATE / DELETE

        private fun bulkUpdate(body: PlanBody.BulkUpdate): BoundOp = when (val filter = evaluate(body.where)) {
            Evaluated.Never -> BoundOp.Empty
            is Evaluated.Filter -> BoundOp.BulkUpdate(body.collection, requireFilter("UPDATE", filter), boundValues(body.sets))
        }

        private fun bulkDelete(body: PlanBody.BulkDelete): BoundOp = when (val filter = evaluate(body.where)) {
            Evaluated.Never -> BoundOp.Empty
            is Evaluated.Filter -> BoundOp.BulkDelete(body.collection, requireFilter("DELETE", filter))
        }

        /** Bulk UPDATE/DELETE must always narrow to specific documents, even after NULL guards fold away. */
        private fun requireFilter(verb: String, filter: Evaluated.Filter): BoundFilter =
            filter.value ?: fail(
                "This WHERE matches every document once its NULL guards are applied. " +
                        "$verb ALL is not supported; narrow the WHERE so it always excludes some documents."
            )

        // values

        private fun idValue(operand: Operand): String {
            val v = resolveScalar(operand)
            val s = v as? String ?: fail("The id must be a string.")
            if (s.isEmpty()) fail("The id must not be empty.")
            return s
        }

        private fun boundValues(values: Map<String, PlanValue>): Map<String, BoundValue> =
            values.mapValues { boundValue(it.value) }

        private fun boundValue(value: PlanValue): BoundValue = when (value) {
            is PlanValue.Op -> BoundValue.Plain(resolveScalar(value.operand))
            is PlanValue.Increment -> {
                val amount = resolveScalar(value.amount) as? Number
                    ?: fail("The amount added to '${value.field}' must be a number.")
                BoundValue.Increment(if (value.sign < 0) negate(amount) else amount)
            }
            PlanValue.ServerTimestamp -> BoundValue.ServerTimestamp
        }

        private fun negate(n: Number): Number = when (n) {
            is Long -> -n
            is Double -> -n
            else -> -n.toDouble()
        }

        private fun resolveScalar(operand: Operand): Any? = when (operand) {
            is Operand.Param -> values[operand.name]
            is Operand.Lit -> literalValue(operand.value)
        }

        private fun literalValue(literal: Literal): Any = when (literal) {
            is Literal.Str -> literal.value
            is Literal.Int64 -> literal.value
            is Literal.Dbl -> literal.value
        }

        // WHERE folding

        private sealed interface Evaluated {
            data object Never : Evaluated
            data class Filter(val value: BoundFilter?) : Evaluated
        }

        private fun evaluate(cond: Cond?): Evaluated {
            if (cond == null) return Evaluated.Filter(null)
            return when (val folded = fold(cond)) {
                Folded.True -> Evaluated.Filter(null)
                Folded.False -> Evaluated.Never
                is Folded.Filter -> Evaluated.Filter(folded.filter)
                is Folded.Bad -> fail(folded.message)
            }
        }

        private sealed interface Folded {
            data object True : Folded
            data object False : Folded
            data class Filter(val filter: BoundFilter) : Folded
            data class Bad(val message: String) : Folded
        }

        /**
         * [Cond.And]/[Cond.Or] are produced by PlanBuilder as exactly binary (JSqlParser's AndExpression
         * and OrExpression are always binary nodes), so this reduces left-to-right using the same
         * short-circuit rule as a single binary combination, applied pairwise.
         */
        private fun fold(cond: Cond): Folded = when (cond) {
            is Cond.And -> cond.items.map { fold(it) }.reduce(::andPair)
            is Cond.Or -> cond.items.map { fold(it) }.reduce(::orPair)
            is Cond.ParamIsNull -> {
                val isNull = values[cond.param] == null
                if (isNull != cond.negated) Folded.True else Folded.False
            }
            is Cond.IsNull ->
                Folded.Filter(BoundFilter.Leaf(cond.field, if (cond.negated) FilterOp.IS_NOT_NULL else FilterOp.IS_NULL, null))
            is Cond.Cmp -> {
                val v = resolveScalar(cond.rhs)
                if (v == null) {
                    // A literal can never resolve to null (see literalValue), so a null here always
                    // means the right-hand side was a parameter.
                    val paramName = (cond.rhs as Operand.Param).name
                    Folded.Bad(
                        "@$paramName is null in a comparison on '${cond.field}'. " +
                                "Use IS NULL, or guard it with (@$paramName IS NULL OR ...)."
                    )
                } else {
                    Folded.Filter(BoundFilter.Leaf(cond.field, cond.op, v))
                }
            }
            is Cond.InList -> inList(cond)
        }

        private fun andPair(l: Folded, r: Folded): Folded = when {
            l is Folded.False || r is Folded.False -> Folded.False
            l is Folded.Bad -> l
            r is Folded.Bad -> r
            l is Folded.True -> r
            r is Folded.True -> l
            else -> Folded.Filter(BoundFilter.And(listOf((l as Folded.Filter).filter, (r as Folded.Filter).filter)))
        }

        private fun orPair(l: Folded, r: Folded): Folded = when {
            l is Folded.True || r is Folded.True -> Folded.True
            l is Folded.Bad -> l
            r is Folded.Bad -> r
            l is Folded.False -> r
            r is Folded.False -> l
            else -> Folded.Filter(BoundFilter.Or(listOf((l as Folded.Filter).filter, (r as Folded.Filter).filter)))
        }

        private fun inList(cond: Cond.InList): Folded {
            val items: List<Any?> = if (cond.listParam != null) {
                (values[cond.listParam] as? List<*>)?.toList() ?: fail("@${cond.listParam} must not be null.")
            } else {
                cond.items.map { resolveScalar(it) }
            }
            return if (items.isEmpty()) Folded.False else Folded.Filter(BoundFilter.Leaf(cond.field, FilterOp.IN, items))
        }
    }
}
