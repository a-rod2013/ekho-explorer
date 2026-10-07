package com.fsql.data.internal

/**
 * Extension point for combining or batching multiple [BoundOp]s before execution, to reduce
 * Firestore free-tier quota usage. Not implemented.
 */
internal fun interface PlanOptimizer {
    fun optimize(ops: List<BoundOp>): List<BoundOp>
}

/** The only implementation today: passes every operation through unchanged. */
internal object NoOpOptimizer : PlanOptimizer {
    override fun optimize(ops: List<BoundOp>): List<BoundOp> = ops
}
