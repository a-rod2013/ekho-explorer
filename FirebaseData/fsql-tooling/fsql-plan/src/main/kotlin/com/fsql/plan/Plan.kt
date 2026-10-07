package com.fsql.plan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Bumped whenever the plan file layout changes in a way an older runtime cannot execute. */
const val PLAN_FORMAT_VERSION = 1

@Serializable
enum class ParamType { STRING, INT, FLOAT, BOOL, TIMESTAMP }

/** One declared parameter, read from a `-- @name: TYPE` header line. */
@Serializable
data class ParamDecl(
    val name: String,
    val type: ParamType,
    val nullable: Boolean,
    val isList: Boolean = false,
)

@Serializable
sealed interface Literal {
    @Serializable @SerialName("str") data class Str(val value: String) : Literal
    @Serializable @SerialName("int") data class Int64(val value: Long) : Literal
    @Serializable @SerialName("dbl") data class Dbl(val value: Double) : Literal
}

/** Either a reference to a declared `@parameter` or a literal value written directly in the SQL. */
@Serializable
sealed interface Operand {
    @Serializable @SerialName("param") data class Param(val name: String) : Operand
    @Serializable @SerialName("lit") data class Lit(val value: Literal) : Operand
}

@Serializable
enum class FilterOp { EQ, NEQ, LT, LTE, GT, GTE, IN, IS_NULL, IS_NOT_NULL }

/** A WHERE condition, exactly as written. Nothing here is resolved against real values yet. */
@Serializable
sealed interface Cond {
    @Serializable @SerialName("cmp") data class Cmp(val field: String, val op: FilterOp, val rhs: Operand) : Cond
    @Serializable @SerialName("and") data class And(val items: List<Cond>) : Cond
    @Serializable @SerialName("or") data class Or(val items: List<Cond>) : Cond

    /** `@param IS [NOT] NULL`: an optional-filter guard, resolved only when a query actually runs. */
    @Serializable @SerialName("pnull") data class ParamIsNull(val param: String, val negated: Boolean = false) : Cond

    /** A plain field null check (not a parameter guard). */
    @Serializable @SerialName("isnull") data class IsNull(val field: String, val negated: Boolean) : Cond

    /**
     * `field IN (...)`. Either [items] holds literal/parameter operands directly, or [listParam] names
     * a single list-typed parameter supplying the whole list (never both at once).
     */
    @Serializable @SerialName("in") data class InList(
        val field: String,
        val items: List<Operand> = emptyList(),
        val listParam: String? = null,
    ) : Cond
}

@Serializable
enum class AggKind { COUNT, SUM, AVG }

@Serializable
data class OrderBy(val field: String, val descending: Boolean)

/** A value written to one document field, exactly as written. */
@Serializable
sealed interface PlanValue {
    @Serializable @SerialName("op") data class Op(val operand: Operand) : PlanValue

    @Serializable @SerialName("inc") data class Increment(val field: String, val sign: Int, val amount: Operand) : PlanValue

    @Serializable @SerialName("now") data object ServerTimestamp : PlanValue
}

@Serializable
sealed interface PlanBody {
    @Serializable @SerialName("docget") data class DocGet(val collection: String, val id: Operand, val columns: List<String>?) : PlanBody

    @Serializable @SerialName("read") data class Read(
        val collection: String,
        val where: Cond?,
        val orderBy: List<OrderBy> = emptyList(),
        val limit: Operand? = null,
        val columns: List<String>? = null,
    ) : PlanBody

    @Serializable @SerialName("agg") data class Aggregate(val collection: String, val where: Cond?, val kind: AggKind, val field: String?) : PlanBody

    @Serializable @SerialName("insert") data class Insert(val collection: String, val id: Operand?, val values: Map<String, PlanValue>) : PlanBody

    @Serializable @SerialName("update") data class UpdateDoc(val collection: String, val id: Operand, val sets: Map<String, PlanValue>) : PlanBody

    @Serializable @SerialName("delete") data class DeleteDoc(val collection: String, val id: Operand) : PlanBody

    @Serializable @SerialName("bulkup") data class BulkUpdate(val collection: String, val where: Cond, val sets: Map<String, PlanValue>) : PlanBody

    @Serializable @SerialName("bulkdel") data class BulkDelete(val collection: String, val where: Cond) : PlanBody
}

@Serializable
data class Plan(val name: String, val params: List<ParamDecl>, val body: PlanBody)

@Serializable
data class PlanFile(val formatVersion: Int, val plans: List<Plan>)
