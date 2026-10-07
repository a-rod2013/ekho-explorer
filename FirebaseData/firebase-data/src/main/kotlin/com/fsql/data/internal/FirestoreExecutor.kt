package com.fsql.data.internal

import com.fsql.data.DbResult
import com.fsql.data.Row
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.tasks.await

/**
 * Translates [BoundOp]s into Firestore calls. It contains no decisions: parsing, type checking and
 * NULL folding are already done, so this class only maps one operation to one SDK call sequence.
 * Exceptions propagate; the service maps them to errors.
 */
internal class FirestoreExecutor(private val db: FirebaseFirestore) : OpExecutor {

    private companion object {
        const val MAX_BATCH = 500
    }

    override suspend fun run(op: BoundOp): DbResult = when (op) {
        BoundOp.Empty -> DbResult.Success()
        is BoundOp.EmptyAggregate -> DbResult.Success(rows = listOf(emptyAggregateRow(op.kind)))
        is BoundOp.DocGet -> docGet(op)
        is BoundOp.Read -> read(op)
        is BoundOp.Aggregate -> aggregate(op)
        is BoundOp.Insert -> insert(op)
        is BoundOp.UpdateDoc -> {
            document(op.collection, op.id).update(toFirestoreMap(op.sets)).await()
            DbResult.Success(affectedCount = 1)
        }
        is BoundOp.DeleteDoc -> {
            document(op.collection, op.id).delete().await()
            DbResult.Success(affectedCount = 1)
        }
        is BoundOp.BulkUpdate -> bulk(op.collection, op.filter) { batch, ref -> batch.update(ref, toFirestoreMap(op.sets)) }
        is BoundOp.BulkDelete -> bulk(op.collection, op.filter) { batch, ref -> batch.delete(ref) }
    }

    // reads

    private fun collection(name: String): CollectionReference = db.collection(name)

    private fun document(collection: String, id: String): DocumentReference = db.collection(collection).document(id)

    private suspend fun docGet(op: BoundOp.DocGet): DbResult {
        val snapshot = document(op.collection, op.id).get().await()
        val rows = if (snapshot.exists()) listOf(toRow(snapshot, op.columns)) else emptyList()
        return DbResult.Success(rows = rows)
    }

    private suspend fun read(op: BoundOp.Read): DbResult {
        var query: Query = collection(op.collection)
        if (op.filter != null) query = query.where(toFilter(op.filter))
        for (order in op.orderBy) query = query.orderBy(fieldPath(order.field), direction(order.descending))
        if (op.limit != null) query = query.limit(op.limit.toLong())
        val snapshot = query.get().await()
        return DbResult.Success(rows = snapshot.documents.map { toRow(it, op.columns) })
    }

    private suspend fun aggregate(op: BoundOp.Aggregate): DbResult {
        var query: Query = collection(op.collection)
        if (op.filter != null) query = query.where(toFilter(op.filter))
        val field = toAggregateField(op)
        val snapshot = query.aggregate(field).get(AggregateSource.SERVER).await()
        val data = mapOf(resultKey(op.kind, op.field) to snapshot.get(field))
        return DbResult.Success(rows = listOf(Row(null, data)))
    }

    private fun emptyAggregateRow(kind: AggKind): Row {
        val value: Any? = if (kind == AggKind.AVG) null else 0L
        return Row(null, mapOf(resultKey(kind, null) to value))
    }

    private fun resultKey(kind: AggKind, field: String?): String = when (kind) {
        AggKind.COUNT -> "count"
        AggKind.SUM -> "sum_$field"
        AggKind.AVG -> "avg_$field"
    }

    private fun toAggregateField(op: BoundOp.Aggregate): AggregateField = when (op.kind) {
        AggKind.COUNT -> AggregateField.count()
        AggKind.SUM -> AggregateField.sum(op.field!!)
        AggKind.AVG -> AggregateField.average(op.field!!)
    }

    // filters

    private fun fieldPath(field: String): FieldPath = FieldPath.of(field)

    private fun direction(descending: Boolean) =
        if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING

    private fun toFilter(filter: BoundFilter): Filter = when (filter) {
        is BoundFilter.Leaf -> leaf(filter)
        is BoundFilter.And -> Filter.and(*filter.items.map { toFilter(it) }.toTypedArray())
        is BoundFilter.Or -> Filter.or(*filter.items.map { toFilter(it) }.toTypedArray())
    }

    private fun leaf(leaf: BoundFilter.Leaf): Filter {
        val path = fieldPath(leaf.field)
        val value = leaf.value
        return when (leaf.op) {
            FilterOp.EQ -> Filter.equalTo(path, value)
            FilterOp.NEQ -> Filter.notEqualTo(path, value)
            FilterOp.LT -> Filter.lessThan(path, value!!)
            FilterOp.LTE -> Filter.lessThanOrEqualTo(path, value!!)
            FilterOp.GT -> Filter.greaterThan(path, value!!)
            FilterOp.GTE -> Filter.greaterThanOrEqualTo(path, value!!)
            FilterOp.IN -> Filter.inArray(path, value as List<*>)
            FilterOp.IS_NULL -> Filter.equalTo(path, null)
            FilterOp.IS_NOT_NULL -> Filter.notEqualTo(path, null)
        }
    }

    // rows

    private fun toRow(doc: DocumentSnapshot, columns: List<String>?): Row {
        val data: Map<String, Any?> = doc.data ?: emptyMap()
        val visible = if (columns == null) data else columns.associateWith { data[it] }
        return Row(doc.id, visible, doc)
    }

    // writes

    private suspend fun insert(op: BoundOp.Insert): DbResult {
        val coll = collection(op.collection)
        val data = toFirestoreMap(op.values)
        val id = op.id
        if (id == null) {
            val ref = coll.add(data).await()
            return DbResult.Success(affectedCount = 1, generatedIds = listOf(ref.id))
        }
        val ref = coll.document(id)
        val existed = db.runTransaction(
            Transaction.Function { tx ->
                val exists = tx.get(ref).exists()
                if (!exists) tx.set(ref, data)
                exists
            }
        ).await()
        if (existed) throw alreadyExists(ref)
        return DbResult.Success(affectedCount = 1)
    }

    private fun alreadyExists(ref: DocumentReference) = FirebaseFirestoreException(
        "The document ${ref.path} already exists.",
        FirebaseFirestoreException.Code.ALREADY_EXISTS,
    )

    /** Loads every matching document first, then writes them in batches of 500. */
    private suspend fun bulk(
        collection: String,
        filter: BoundFilter,
        write: (com.google.firebase.firestore.WriteBatch, DocumentReference) -> Unit,
    ): DbResult {
        val docs = collection(collection).where(toFilter(filter)).get().await().documents
        for (chunk in docs.chunked(MAX_BATCH)) {
            val batch = db.batch()
            for (doc in chunk) write(batch, doc.reference)
            batch.commit().await()
        }
        return DbResult.Success(affectedCount = docs.size)
    }

    // values

    private fun toFirestoreMap(values: Map<String, BoundValue>): Map<String, Any?> =
        values.mapValues { toFirestoreValue(it.value) }

    private fun toFirestoreValue(value: BoundValue): Any? = when (value) {
        is BoundValue.Plain -> value.value
        is BoundValue.Increment -> {
            val amount = value.amount
            if (amount is Long) FieldValue.increment(amount) else FieldValue.increment(amount.toDouble())
        }
        BoundValue.ServerTimestamp -> FieldValue.serverTimestamp()
    }
}
