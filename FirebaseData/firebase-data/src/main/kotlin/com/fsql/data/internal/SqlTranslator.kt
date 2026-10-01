package com.fsql.data.internal

import net.sf.jsqlparser.expression.Expression
import net.sf.jsqlparser.expression.Function
import net.sf.jsqlparser.expression.JdbcNamedParameter
import net.sf.jsqlparser.expression.LongValue
import net.sf.jsqlparser.expression.StringValue
import net.sf.jsqlparser.expression.DoubleValue
import net.sf.jsqlparser.expression.operators.arithmetic.Addition
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction
import net.sf.jsqlparser.expression.operators.conditional.AndExpression
import net.sf.jsqlparser.expression.operators.conditional.OrExpression
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator
import net.sf.jsqlparser.expression.operators.relational.EqualsTo
import net.sf.jsqlparser.expression.operators.relational.ExpressionList
import net.sf.jsqlparser.expression.operators.relational.GreaterThan
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals
import net.sf.jsqlparser.expression.operators.relational.InExpression
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression
import net.sf.jsqlparser.expression.operators.relational.MinorThan
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.OrderByElement
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.update.Update
import java.net.BindException


/**
 * Turns a parsed SQL [Statement] and prior typed checked parameters into a [BoundOp].
 */
internal object SqlTranslator {
    sealed interface Result {
        data class Ok(val op: BoundOp) : Result
        data class Invalid(val message: String) : Result
    }

    fun bind(procedure: String, statement: Statement, decls: List<ParamDecl>, values: Map<String, Any?>): Result =
        try {
            Result.Ok(Binder(procedure, decls, values).bind(statement))
        } catch(e: BindException) {
            Result.Invalid("$procedure: ${e.message}")
        }

    private class BindException(message: String) : Exception(message)

    private class Binder(
        private val procedure: String,
        decls: List<ParamDecl>,
        private val values: Map<String, Any?>,
    ) {
        private val declByName = decls.associateBy {it.name}

        /** Result of invalid supplied values */
        private fun fail(message: String) : Nothing = throw SqlTranslator.BindException(message)

        /** A problem with the SQL itself */
        private fun reject(message: String) : Nothing = throw FsqlFileException(procedure, 0, "$procedure: $message")

        fun bind(statement: Statement): BoundOp = when(statement) {
            is PlainSelect -> select(statement)
            is Insert -> insert(statement)
            is Update -> update(statement)
            is Delete -> delete(statement)
            else -> reject(
                "This is not a SELECT, INSERT, UPDATE or DELETE statement " +
                "(found ${statement::class.simpleName})."
            )
        }

        // select logic

        private fun select(s: PlainSelect): BoundOp {
            if(!s.joins.isNullOrEmpty()) {
                reject("JOIN is not supported. Firebase has no joins; read from each collection seperately.")
            }

            if(s.groupBy != null) {
                reject("GROUP BY is not supported. Firebase only supports whole query COUNT, SUM and AVG.")
            }

            if(s.having != null) {
                reject("HAVING is not supported.")
            }

            val collection = collectionOf(s.fromItem)

            val items = s.selectItems
            val functionItems = items.filter {it.expression is Function}
            if(functionItems.isNotEmpty()) {
                if(functionItems.size != items.size || items.size != 1) {
                    reject("An aggregate (COUNT, SUM, AVG) cannot be combined with plain columns.")
                }

                return aggregate(collection, s.where, items[0].expression as Function)
            }

            val columns = if(items.size == 1 && items[0].expression.toString() == "*") {
                null
            } else {
                items.map {item -> fieldname(item.expression as? Column ?: reject("Only plain column names are supported in SELECT."))}
            }

            if(isIdEquals(s.where)) {
                val id = idValue((s.where as EqualsTo).rightExpression)
                return BoundOp.DocGet(collection, id, columns)
            }

            return when (val filter = evaluate(s.where)) {
                Evaluated.Never -> BoundOp.Empty
                is Evaluated.Filter -> {
                    BoundOp.Read(collection, filter.value, orderBy(s.orderByElements), limit(s), columns)
                }
            }
        }

        private fun aggregate(collection: String, where: Expression?, fn: Function): BoundOp {
            val name = fn.name.uppercase()

            val kind = when(name) {
                "COUNT" -> AggKind.COUNT
                "SUM" -> AggKind.SUM
                "AVG" -> AggKind.AVG
                else -> reject("Unsupported function `${fn.name}`. Only COUNT(*), SUM(column), and AVG(column) are supported.")
            }

            val field = when(kind) {
                AggKind.COUNT -> {
                    val params = fn.parameters
                    val isStar = params != null && params.size == 1 && params[0] is net.sf.jsqlparser.statement.select.AllColumns

                    if(!isStar) {
                        reject("Use COUNT(*), not COUNT(column).")
                    }
                    null
                }
                else -> {
                    val params = fn.parameters
                    if(params == null || params.size != 1) {
                        reject("${name}(...) needs exactly one column.")
                    }

                    fieldName(params[0] as? Column ?: reject("${name}(...) needs a plain column name."))
                }
            }

            return when(val filter = evaluate(where)) {
                Evaluated.Never -> BoundOp.EmptyAggregate(kind)
                is Evaluated.Filter -> BoundOp.Aggregate(collection, filter.value, kind, field)
            }
        }

        private fun limit(s: PlainSelect) : Int? {
            val limit = s.limit ?: return null
            val rowCount = limit.rowCount ?: reject("LIMIT needs a number.")
            val n = (resolveScalar(rowCount) as? Number)?.toInt() ?: fail("LIMIT must not be null.")

            if(n<1) {
                fail("LIMIT must be at least 1.")
            }

            return n
        }

        // insert

        private fun insert(s: Insert) : BoundOp {
            val collection = collectionOf(s.table)
            val columns = s.columns?.map {fieldName(it)} ?: reject("INSERT needs an explicit column list.")
            val exprs = s.values?.expressions ?: reject("INSERT needs a VALUES clause.")

            if(columns.size != exprs.size) {
                reject("The statement lists ${columns.size} columns but ${exprs.size} values.")
            }

            var id: String? = null
            val out = LinkedHashMap<String, BoundValue>()
            columns.forEachIndexed {index, column ->
                val expr = exprs[index] as Expression

                if(column == "id") {
                    id = idValue(expr)
                } else {
                    out[column] = boundValue(column, expr)
                }
            }

            return BoundOp.Insert(collection, id, out)
        }

        // update

        private fun update(s: Update): BoundOp {
            val collection = collectionOf(s.table)
            val sets = LinkedHashMap<String, BoundValue>()

            for(set in s.updateSets) {
                for(i in set.columns.indices) {
                    val column = fieldName(set.columns[i])

                    if(column == "id") {
                        reject("The id column cannot be changed.")
                    }

                    sets[column] = boundValue(column, set.values[i] as Expression)
                }
            }

            if(isIdEquals(s.where)) {
                val id = idValue((s.where as EqualsTo).rightExpression)
                return BoundOp.UpdateDoc(collection, id, sets)
            }

            if(s.where == null) {
                reject("UPDATE needs a where clause. Firebase has no way to update every document safely from one call.")
            }

            return when (val filter = evaluate(s.where)) {
                Evaluated.Never -> BoundOp.Empty
                is Evaluated.Filter -> BoundOp.BulkUpdate(collection, requireFilter("UPDATE", filter), sets)
            }
        }

        /** Bulk UPDATE/DELETE must always narrow to specific documents. */
        private fun requireFilter(verb: String, filter: Evaluated.Fitler): BoundFilter =
            filter.value ?: fail(
                "This WHERE matches every document once its NULL guards are applied. " +
                "$verb ALL is not supported; narrow the WHERE clause so it always exclude some documents."
            )

        // delete

        private fun delete(s: Delete): BoundOp {
            val collection = collectionOf(s.table)

            if(isIdEquals(s.where)) {
                val id = idValue((s.where as EqualsTo).rightExpression)
                return BoundOp.DeleteDoc(collection, id)
            }

            if(s.where == null) {
                reject("DELETE needs a WHERE clause. Firebase has no way to delete every document safely from one call.")
            }

            return when (val filter = evaluate(s.where)) {
                Evaluated.Never -> BoundOp.Empty
                is Evaluated.Filter -> BoundOp.BulkDelete(collection, requireFilter("DELETE", filter))
            }
        }

        // helpers

        private fun collectionOf(item: Any?): String =
            (item as? Table)?.name ?: reject("FROM (or the table after UPDATE/DELETE) must be a plain collectio name.")

        /** A plain field name from a [Column] */
        private fun fieldname(column: Column): String {
            if(!column.tableName.isNullOrBlank()) {
                reject(
                    "`${column.fullyQualifiedName}` looks like a nested field. Dotted field names are not " +
                    "supported; use a plain top level field name."
                )
            }

            return column.columnName
        }

        private fun isIdEquals(where: Expression?): Boolean =
            where is EqualsTo && (where.leftExpression as? Column)?.let {it.tableName.isNullOrBlank() && it.columnName == "id"} == true

        private fun idValue(expr: Expression): String {
            val v = resolveScalar(expr)
            val s = v as? String ?: fail("The id must be a string.")

            if(s.isEmpty()) {
                fail("The Id must not be empty.")
            }

            return s
        }

        private fun boundValue(field: String, expr: Expression): BoundValue = when {
            expr is Function && expr.name.equals("NOW", ignoreCase = true) -> BoundValue.ServerTimestamp
            expr is Addition || expr is Subtraction -> increment(field, expr)
            else -> BoundValue.Plain(resolveScalar(expr))
        }

        private fun increment(field: String, expr: Expression): BoundValue {
            val (left, right, sign) = when(expr) {
                is Addition -> Triple(expr.leftExpression, expr.rightExpression, 1)
                is Subtraction -> Triple(expr.leftExpression, expr.rightExpression, -1)
                else -> error("unreachable")
            }

            val column = left as? Column
            if(column == null || column.tableName.isNullOrBlank().not() || column.columnName != field) {
                reject("Only 'col = col + @n' or 'col = col - @n' is supported as arithmetic in SET.")
            }

            val amount = resolveScalar(right) as? Number ?: fail("The amount added to '$field' must be a number.")
            return BoundValue.Increment(if(sign < 0) negate(amount) else amount)
        }

        private fun negate(n: Number): Number = when(n) {
            is Long -> -n
            is Double -> -n
            else -> -n.toDouble()
        }

        /** Resolves a parameter or literal to its Kotlin value. Rejects anything else. */
        private fun resolveScalar(expr: Expression): Any? = when(expr) {
            is JdbcNamedParameter -> values[expr.name]
            is LongValue -> expr.value
            is StringValue -> expr.value
            is DoubleValue -> expr.value
            else -> reject("Only a @parameter or a literal is allowed here (found '$expr').")
        }

        // where handling

        /** The result of evaul a WHERE clause once the params are known. */
        private sealed interface Evaluated {
            data object Never : Evaluated
            data class Filter(val value: BoundFilter) : Evaluated
        }

        private fun evaluate(where: Expression?): Evaluated {
            if(where == null) {
                return Evaluated.Filter(null)
            }
            
            return when(val folded = fold(where)) {
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

        private fun fold(expr: Expression): Folded = when(expr) {
            is net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList<*> -> {
                if(expr.size != 1) {
                    reject("Unsupported expression '$expr'.")
                }

                fold(expr[0] as Expression)
            }

            is AndExpression -> {
                val l = fold(expr.leftExpression)
                val r = fold(expr.rightExpression)

                when {
                    l is Folded.False || r is Folded.False -> Folded.False
                    l is Folded.Bad -> l
                    r is Folded.Bad -> r
                    l is Folded.True -> r
                    r is Folded.True -> l
                    else -> Folded.Filter(BoundFilter.And(listOf((l as Folded.Filter).filter, (r as Folded.Filter).filter)))
                }
            }

            is OrExpression -> {
                val l = fold(expr.leftExpression)
                val r = fold(expr.rightExpression)

                when {
                    l is Folded.True || r is Folded.True -> Folded.True
                    l is Folded.Bad -> l
                    r is Folded.Bad -> r
                    l is Folded.False -> r
                    r is Folded.False -> l
                    else -> Folded.Filter(BoundFilter.Or(listOf((l as Folded.Filter).filter, (r as Folded.Filter).filter)))
                }
            }

            is IsNullExpression -> isNull(expr)
            is InExpression -> inList(expr)
            is ComparisonOperator -> comparison(expr)
            else -> reject("'$expr' is not a supported WHERE condition.")
        }

        private fun isNull(expr: IsNullExpression): Folded {
            val target = expr.leftExpression
            if(target is JdbcNamedParameter) {
                val isNull = values[target.name] == null
                return if(isNull != expr.isNot) Folded.True else Folded.False
            }

            val field = fieldname(target as? Column ?: reject("IS NULL needs a column or a @parameter."))
            return Folded.Filter(BoundFilter.Leaf(field, if(expr.isNot) FilterOp.IS_NOT_NULL else FilterOp.IS_NULL, null))
        }

        private fun comparison(expr: ComparisonOperator): Folded {
            val field = fieldname(expr.leftExpression as? Column ?: reject("Put the column on the left side of the comparison."))
            val right = expr.rightExpression

            if(right is JdbcNamedParameter) {
                val v = values[right.name]

                if(v == null) {
                    return Folded.Bad(
                        "@${right.name} is null in a comparison on '$field'. " +
                        "Use IS NULL, or guard it with (@${right.name} IS NULL OR ...)."
                    )
                }

                return Folded.Filter(BoundFilter.Leaf(field, opFor(expr), v))
            }

            val value = resolveScalar(right)
            return Folded.Filter(BoundFilter.Leaf(field, opFor(expr), value))
        }

        private fun opFor(expr: ComparisonOperator): FilterOp = when(expr) {
            is EqualsTo -> FilterOp.EQ
            is NotEqualsTo -> FilterOp.NEQ
            is GreaterThan -> FilterOp.GT
            is GreaterThanEquals -> FilterOp.GTE
            is MinorThan -> FilterOp.LT
            is MinorThanEquals -> FilterOp.LTE
            else -> reject("Unsupported comparison operator '$expr'.")
        }

        private fun inList(expr: InExpression): Folded {
            if(expr.isNot) {
                reject("NOT IN is not supported.")
            }

            val field = fieldname(expr.leftExpression as? Column ?: reject("Put the column on the left side of IN."))
            val right = expr.rightExpression

            if(right is net.sf.jsqlparser.statement.select.Select) {
                reject("A subquery is not supported. Read it as a separate procedure.")
            }

            if(right is AndExpression || right is OrExpression) {
                // Known loophole

                reject(
                    "IN combined with OR or AND needs its own parenthesis. Write it as (column IN (...)) OR other = ..., " +
                    "not column IN (...) OR other = ..."
                )
            }

            val list = right as? ExpressionList<*> ?: reject("IN needs a list of values.")

            if(list.size == 1 && list[0] is JdbcNamedParameter) {
                val paramName = (list[0] as JdbcNamedParameter).name

                if(declByName[paramName]?.isList == true) {
                    val items = values[paramName] as? List<*> ?: fail("@$paramName must not be null.")
                    return if(items.isEmpty()) Folded.False else Folded.Filter(BoundFilter.Leaf(field, FilterOp.IN, items))
                }
            }

            val items = list.map {resolveScalar(it as Expression)}
            return if(items.isEmpty()) Folded.False else Folded.Filter(BoundFilter.Leaf(field, FilterOp.IN, items))
        }
    }
}