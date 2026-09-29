package com.fsql.data.internal

/** Types accepted as parameters in .fsql files. */
internal enum class ParamType{
    STRING,
    INT,
    FLOAT,
    BOOL,
    TIMESTAMP
}

/** Represents a single declared parameter. */
internal data class ParamDecl(
    val name: String,
    val type: ParamType,
    val nullable: Boolean,
    val isList: Boolean = false,
)

/** A parsed, ready to run stored proc. Includes its params and SQL statement. */
internal data class ParsedProcedure(
    val name: String,
    val params: List<ParamDecl>,
    val statement: net.sf.jsqlparser.statement.Statement,
)

internal enum class FilterOp {
    EQ,
    NEQ,
    LT,
    LTE,
    GT,
    GTE,
    IN,
    IS_NULL,
    IS_NOT_NULL
}

// Where conditionals. Post param sub and post null guard fold
internal sealed interface BoundFilter {
    data class Leaf(val field: String, val op: FilterOp, val value: Any?) : BoundFilter
    data class And(val items: List<BoundFilter>) : BoundFilter
    data class Or(val items: List<BoundFilter>) : BoundFilter
}

internal enum class AggKind {
    COUNT,
    SUM,
    AVG
}

// represents a value written to a doc field
internal sealed interface BoundValue {
    data class Plain(val value: Any?) : BoundValue

    // Firebase increment
    data class Increment(val amount: Number) : BoundValue

    // Now() call from the db
    data object ServerTimestamp : BoundValue
}

internal data class OrderBy(val field: String, val descending: Boolean)

// Final product operator. Already type checked and ready to fire
internal sealed interface BoundOp {
    data object Empty : BoundOp

    data class DocGet(val collection: String, val id: String, val columns: List<String>?) : BoundOp

    data class Read(
        val collection: String,
        val filter: BoundFilter?,
        val orderBy: List<OrderBy>,
        val limit: Int?,
        val columns: List<String>?,
    ) : BoundOp

    data class Aggregate(val collection: String, val filter: BoundFilter?, val kind: AggKind, val field: String?) : BoundOp

    data class EmptyAggregate(val kind: AggKind) : BoundOp

    data class Insert(val collection: String, val id: String?, val values: Map<String, BoundValue>) : BoundOp

    data class UpdateDoc(val collection: String, val id: String, val sets: Map<String, BoundValue>) : BoundOp

    data class DeleteDoc(val collection: String, val id: String) : BoundOp

    data class BulkUpdate(val collection: String, val filter: BoundFilter, val sets: Map<String, BoundValue>) : BoundOp

    data class BulkDelete(val collection: String, val filter: BoundFilter) : BoundOp
}