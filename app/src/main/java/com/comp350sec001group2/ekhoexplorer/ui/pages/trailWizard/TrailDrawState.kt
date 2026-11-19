package com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard

import com.comp350sec001group2.ekhoexplorer.Business.Trails.LatLngPoint
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailLimits

class TrailDrawState {
    private val pins = mutableListOf<LatLngPoint>()

    val points: List<LatLngPoint> get() = pins.toList()

    var selected: Int? = null
        private set

    fun add(point: LatLngPoint): Boolean {
        if (pins.size >= TrailLimits.MAX_PINS) return false
        pins += point
        return true
    }

    fun move(index: Int, point: LatLngPoint) {
        if (index in pins.indices) pins[index] = point
    }

    fun undo() {
        if (pins.isNotEmpty()) pins.removeAt(pins.lastIndex)
        selected = selected?.takeIf { it in pins.indices }
    }

    fun clear() {
        pins.clear()
        selected = null
    }

    fun replaceAll(points: List<LatLngPoint>) {
        pins.clear()
        pins.addAll(points)
        selected = null
    }

    fun select(index: Int?) {
        selected = index?.takeIf { it in pins.indices }
    }
}
