package com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions

import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote

object VoteMath {
    /** The vote after a tap. Tapping the active vote clears it. */
    fun next(previous: Vote, tapped: Vote): Vote =
        if (tapped == Vote.NONE) previous else if (tapped == previous) Vote.NONE else tapped

    /** How (upCount, downCount) change when moving from [previous] to [next]. */
    fun deltas(previous: Vote, next: Vote): Pair<Int, Int> {
        var up = 0
        var down = 0
        when (previous) { Vote.UP -> up--; Vote.DOWN -> down--; Vote.NONE -> Unit }
        when (next) { Vote.UP -> up++; Vote.DOWN -> down++; Vote.NONE -> Unit }
        return up to down
    }
}
