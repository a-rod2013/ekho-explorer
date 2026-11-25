package com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards

data class CardSummary(val count: Int, val firstObservedAt: Long, val lastObservedAt: Long)

object CardMath {
    fun first(observedAt: Long) = CardSummary(1, observedAt, observedAt)

    fun add(summary: CardSummary, observedAt: Long) = CardSummary(
        count = summary.count + 1,
        firstObservedAt = minOf(summary.firstObservedAt, observedAt),
        lastObservedAt = maxOf(summary.lastObservedAt, observedAt),
    )

    fun remove(summary: CardSummary, remainingFirst: Long?, remainingLast: Long?): CardSummary? =
        if (summary.count <= 1 || remainingFirst == null || remainingLast == null) null
        else CardSummary(summary.count - 1, remainingFirst, remainingLast)
}
