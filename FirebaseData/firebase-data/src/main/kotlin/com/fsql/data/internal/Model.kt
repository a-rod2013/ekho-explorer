package com.fsql.data.internal

/** The types a `.fsql` file can declare for a `@param`. */
internal typealias ParamType = com.fsql.plan.ParamType

/**
 * One declared parameter, read from a `-- @name: TYPE` header line.
 * [isList] is true for `-- @name: TYPE[]`, meaning the value is a list of [type], used with `IN (@name)`.
 */
internal typealias ParamDecl = com.fsql.plan.ParamDecl

internal typealias FilterOp = com.fsql.plan.FilterOp
internal typealias AggKind = com.fsql.plan.AggKind
internal typealias OrderBy = com.fsql.plan.OrderBy

/** A WHERE condition, after parameters were substituted and any NULL guards folded away. */
internal sealed interface BoundFilter {
    data class Leaf(val field: String, val op: FilterOp, val value: Any?) : BoundFilter
    data class And(val items: List<BoundFilter>) : BoundFilter
    data class Or(val items: List<BoundFilter>) : BoundFilter
}

/** A value written to one document field. */
internal sealed interface BoundValue {
    data class Plain(val value: Any?) : BoundValue

    /** Firestore increment; the amount already carries its sign (`n = n - 3` is `Increment(-3)`). */
    data class Increment(val amount: Number) : BoundValue

    /** `NOW()` in a VALUES or SET: Firestore fills in the server's time when it writes. */
    data object ServerTimestamp : BoundValue
}

/** One concrete database operation, ready for the executor. No parsing or type checking is left to do. */
internal sealed interface BoundOp {
    /** The WHERE clause can never match (an optional filter guard folded to always-false); nothing to run. */
    data object Empty : BoundOp

    data class DocGet(val collection: String, val id: String, val columns: List<String>?) : BoundOp

    data class Read(
        val collection: String,
        val filter: BoundFilter?,
        val orderBy: List<OrderBy>,
        val limit: Int?,
        val columns: List<String>?,
    ) : BoundOp

    data class Aggregate(val collection: String, val filter: BoundFilter?, val kind: AggKind, val field: String?) :
        BoundOp

    /** An aggregate whose WHERE folded to "matches nothing"; the result is a zero/null row, no Firestore call. */
    data class EmptyAggregate(val kind: AggKind) : BoundOp

    data class Insert(val collection: String, val id: String?, val values: Map<String, BoundValue>) : BoundOp

    data class UpdateDoc(val collection: String, val id: String, val sets: Map<String, BoundValue>) : BoundOp

    data class DeleteDoc(val collection: String, val id: String) : BoundOp

    data class BulkUpdate(val collection: String, val filter: BoundFilter, val sets: Map<String, BoundValue>) :
        BoundOp

    data class BulkDelete(val collection: String, val filter: BoundFilter) : BoundOp
}
