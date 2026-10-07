package com.fsql.compiler

import com.fsql.plan.AggKind
import com.fsql.plan.Cond
import com.fsql.plan.FilterOp
import com.fsql.plan.Literal
import com.fsql.plan.Operand
import com.fsql.plan.OrderBy
import com.fsql.plan.ParamDecl
import com.fsql.plan.PlanBody
import com.fsql.plan.PlanValue
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
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.AllColumns
import net.sf.jsqlparser.statement.select.Limit
import net.sf.jsqlparser.statement.select.OrderByElement
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update

/**
 * Turns a parsed SQL [Statement] into a serializable [PlanBody], at build time, with no parameter
 * values available yet. Every check that does not depend on actual values lives here (a join, a
 * subquery, GROUP BY, the dotted-field-name trap, the IN-combined-with-OR parser quirk, an arithmetic
 * shape mismatch, a column/value count mismatch, a missing WHERE on UPDATE/DELETE). Anything that
 * does depend on values (a null parameter in an unguarded comparison, a non-string id, an out-of-range
 * LIMIT, a bulk write whose guard folds to "matches everything") is deferred to PlanBinder, which runs
 * at call time once real values exist.
 */
object PlanBuilder {

    /** @throws FsqlFileException naming [procedure]; every problem here is independent of call-time values. */
    fun bind(procedure: String, statement: Statement, decls: List<ParamDecl>): PlanBody =
        Builder(procedure, decls).bind(statement)

    private class Builder(private val procedure: String, decls: List<ParamDecl>) {
        private val declByName = decls.associateBy { it.name }

        private fun reject(message: String): Nothing = throw FsqlFileException(procedure, 0, "$procedure: $message")

        fun bind(statement: Statement): PlanBody = when (statement) {
            is PlainSelect -> select(statement)
            is Insert -> insert(statement)
            is Update -> update(statement)
            is Delete -> delete(statement)
            else -> reject(
                "This is not a SELECT, INSERT, UPDATE or DELETE statement " +
                        "(found ${statement::class.simpleName})."
            )
        }

        // ---------------------------------------------------------------- SELECT

        private fun select(s: PlainSelect): PlanBody {
            if (!s.joins.isNullOrEmpty()) {
                reject("JOIN is not supported. Firestore has no joins; read from each collection separately.")
            }
            if (s.groupBy != null) {
                reject("GROUP BY is not supported. Firestore only supports whole-query COUNT, SUM and AVG.")
            }
            if (s.having != null) reject("HAVING is not supported.")
            val collection = collectionOf(s.fromItem)

            val items = s.selectItems
            val functionItems = items.filter { it.expression is Function }
            if (functionItems.isNotEmpty()) {
                if (functionItems.size != items.size || items.size != 1) {
                    reject("An aggregate (COUNT, SUM, AVG) cannot be combined with plain columns.")
                }
                return aggregate(collection, s.where, items[0].expression as Function)
            }

            val columns = if (items.size == 1 && items[0].expression.toString() == "*") {
                null
            } else {
                items.map { item -> fieldName(item.expression as? Column ?: reject("Only plain column names are supported in SELECT.")) }
            }

            if (isIdEquals(s.where)) {
                val id = operandOf((s.where as EqualsTo).rightExpression)
                return PlanBody.DocGet(collection, id, columns)
            }

            val where = s.where?.let { translateWhere(it) }
            return PlanBody.Read(collection, where, orderBy(s.orderByElements), s.limit?.let { limitOperand(it) }, columns)
        }

        private fun aggregate(collection: String, where: Expression?, fn: Function): PlanBody {
            val name = fn.name.uppercase()
            val kind = when (name) {
                "COUNT" -> AggKind.COUNT
                "SUM" -> AggKind.SUM
                "AVG" -> AggKind.AVG
                else -> reject("Unsupported function '${fn.name}'. Only COUNT(*), SUM(column) and AVG(column) are supported.")
            }
            val field = when (kind) {
                AggKind.COUNT -> {
                    val params = fn.parameters
                    val isStar = params != null && params.size == 1 && params[0] is AllColumns
                    if (!isStar) reject("Use COUNT(*), not COUNT(column).")
                    null
                }
                else -> {
                    val params = fn.parameters
                    if (params == null || params.size != 1) reject("${name}(...) needs exactly one column.")
                    fieldName(params[0] as? Column ?: reject("${name}(...) needs a plain column name."))
                }
            }
            val cond = where?.let { translateWhere(it) }
            return PlanBody.Aggregate(collection, cond, kind, field)
        }

        private fun limitOperand(limit: Limit): Operand {
            val rowCount = limit.rowCount ?: reject("LIMIT needs a number.")
            return operandOf(rowCount)
        }

        private fun orderBy(elements: List<OrderByElement>?): List<OrderBy> =
            elements.orEmpty().map { e ->
                val column = e.expression as? Column ?: reject("ORDER BY needs a plain column name.")
                OrderBy(fieldName(column), !e.isAsc)
            }

        // INSERT

        private fun insert(s: Insert): PlanBody {
            val collection = collectionOf(s.table)
            val columns = s.columns?.map { fieldName(it) } ?: reject("INSERT needs an explicit column list.")
            val exprs = s.values?.expressions ?: reject("INSERT needs a VALUES clause.")
            if (columns.size != exprs.size) {
                reject("The statement lists ${columns.size} columns but ${exprs.size} values.")
            }
            var id: Operand? = null
            val out = LinkedHashMap<String, PlanValue>()
            columns.forEachIndexed { index, column ->
                val expr = exprs[index] as Expression
                if (column == "id") {
                    id = operandOf(expr)
                } else {
                    out[column] = planValue(column, expr)
                }
            }
            return PlanBody.Insert(collection, id, out)
        }

        // UPDATE

        private fun update(s: Update): PlanBody {
            val collection = collectionOf(s.table)
            val sets = LinkedHashMap<String, PlanValue>()
            for (set in s.updateSets) {
                for (i in set.columns.indices) {
                    val column = fieldName(set.columns[i])
                    if (column == "id") reject("The id column cannot be changed.")
                    sets[column] = planValue(column, set.values[i] as Expression)
                }
            }
            if (isIdEquals(s.where)) {
                val id = operandOf((s.where as EqualsTo).rightExpression)
                return PlanBody.UpdateDoc(collection, id, sets)
            }
            if (s.where == null) {
                reject("UPDATE needs a WHERE clause. Firestore has no way to update every document safely from one call.")
            }
            return PlanBody.BulkUpdate(collection, translateWhere(s.where), sets)
        }

        // DELETE

        private fun delete(s: Delete): PlanBody {
            val collection = collectionOf(s.table)
            if (isIdEquals(s.where)) {
                val id = operandOf((s.where as EqualsTo).rightExpression)
                return PlanBody.DeleteDoc(collection, id)
            }
            if (s.where == null) {
                reject("DELETE needs a WHERE clause. Firestore has no way to delete every document safely from one call.")
            }
            return PlanBody.BulkDelete(collection, translateWhere(s.where))
        }

        // shared helpers

        private fun collectionOf(item: Any?): String =
            (item as? Table)?.name ?: reject("FROM (or the table after UPDATE/DELETE) must be a plain collection name.")

        private fun fieldName(column: Column): String {
            if (!column.tableName.isNullOrBlank()) {
                reject(
                    "'${column.fullyQualifiedName}' looks like a nested field. Dotted field names are not " +
                            "supported in v1; use a plain top-level field name."
                )
            }
            return column.columnName
        }

        private fun isIdEquals(where: Expression?): Boolean =
            where is EqualsTo && (where.leftExpression as? Column)?.let { it.tableName.isNullOrBlank() && it.columnName == "id" } == true

        private fun planValue(field: String, expr: Expression): PlanValue = when {
            expr is Function && expr.name.equals("NOW", ignoreCase = true) -> PlanValue.ServerTimestamp
            expr is Addition || expr is Subtraction -> increment(field, expr)
            else -> PlanValue.Op(operandOf(expr))
        }

        private fun increment(field: String, expr: Expression): PlanValue {
            val (left, right, sign) = when (expr) {
                is Addition -> Triple(expr.leftExpression, expr.rightExpression, 1)
                is Subtraction -> Triple(expr.leftExpression, expr.rightExpression, -1)
                else -> error("unreachable")
            }
            val column = left as? Column
            if (column == null || !column.tableName.isNullOrBlank() || column.columnName != field) {
                reject("Only 'col = col + @n' or 'col = col - @n' is supported as arithmetic in SET.")
            }
            return PlanValue.Increment(field, sign, operandOf(right))
        }

        /** Resolves a parameter or literal to an [Operand]. Rejects anything else. */
        private fun operandOf(expr: Expression): Operand = when (expr) {
            is JdbcNamedParameter -> Operand.Param(expr.name)
            is LongValue -> Operand.Lit(Literal.Int64(expr.value))
            is StringValue -> Operand.Lit(Literal.Str(expr.value))
            is DoubleValue -> Operand.Lit(Literal.Dbl(expr.value))
            else -> reject("Only a @parameter or a literal is allowed here (found '$expr').")
        }

        // ---------------------------------------------------------------- WHERE translation

        private fun translateWhere(expr: Expression): Cond = when (expr) {
            is ParenthesedExpressionList<*> -> {
                if (expr.size != 1) reject("Unsupported expression '$expr'.")
                translateWhere(expr[0] as Expression)
            }
            is AndExpression -> Cond.And(listOf(translateWhere(expr.leftExpression), translateWhere(expr.rightExpression)))
            is OrExpression -> Cond.Or(listOf(translateWhere(expr.leftExpression), translateWhere(expr.rightExpression)))
            is IsNullExpression -> isNull(expr)
            is InExpression -> inList(expr)
            is ComparisonOperator -> comparison(expr)
            else -> reject("'$expr' is not a supported WHERE condition.")
        }

        private fun isNull(expr: IsNullExpression): Cond {
            val target = expr.leftExpression
            if (target is JdbcNamedParameter) {
                return Cond.ParamIsNull(target.name, expr.isNot)
            }
            val field = fieldName(target as? Column ?: reject("IS NULL needs a column or a @parameter."))
            return Cond.IsNull(field, expr.isNot)
        }

        private fun comparison(expr: ComparisonOperator): Cond {
            val field = fieldName(expr.leftExpression as? Column ?: reject("Put the column on the left of the comparison."))
            return Cond.Cmp(field, opFor(expr), operandOf(expr.rightExpression))
        }

        private fun opFor(expr: ComparisonOperator): FilterOp = when (expr) {
            is EqualsTo -> FilterOp.EQ
            is NotEqualsTo -> FilterOp.NEQ
            is GreaterThan -> FilterOp.GT
            is GreaterThanEquals -> FilterOp.GTE
            is MinorThan -> FilterOp.LT
            is MinorThanEquals -> FilterOp.LTE
            else -> reject("Unsupported comparison operator '$expr'.")
        }

        private fun inList(expr: InExpression): Cond {
            if (expr.isNot) reject("NOT IN is not supported.")
            val field = fieldName(expr.leftExpression as? Column ?: reject("Put the column on the left of IN."))
            val right = expr.rightExpression
            if (right is Select) {
                reject("A subquery is not supported. Read it as a separate procedure.")
            }
            if (right is AndExpression || right is OrExpression) {
                reject(
                    "IN combined with OR or AND needs its own parentheses. Write it as " +
                            "(column IN (...)) OR other = ..., not column IN (...) OR other = ..."
                )
            }
            val list = right as? ExpressionList<*> ?: reject("IN needs a list of values.")

            if (list.size == 1 && list[0] is JdbcNamedParameter) {
                val paramName = (list[0] as JdbcNamedParameter).name
                if (declByName[paramName]?.isList == true) {
                    return Cond.InList(field, listParam = paramName)
                }
            }

            val items = list.map { operandOf(it as Expression) }
            return Cond.InList(field, items = items)
        }
    }
}
