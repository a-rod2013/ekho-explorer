package com.comp350sec001group2.ekhoexplorer.Business.Trails

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/** Distances, path length, simplifying a recorded path, and a compact text form for storing it. */
object TrailGeo {

    private const val EARTH_RADIUS_METERS = 6_371_008.8
    private const val METERS_PER_DEGREE_LAT = 111_195.0
    private const val POLYLINE_SCALE = 1e6

    fun distanceMeters(a: LatLngPoint, b: LatLngPoint): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun lengthMeters(points: List<LatLngPoint>): Double =
        points.zipWithNext().sumOf { (a, b) -> distanceMeters(a, b) }

    fun simplify(points: List<LatLngPoint>, maxPoints: Int): List<LatLngPoint> {
        require(maxPoints >= 2) { "maxPoints must be at least 2" }
        if (points.size <= maxPoints) return points
        var tolerance = 1.0
        while (tolerance < 1_000_000.0) {
            val kept = douglasPeucker(points, tolerance)
            if (kept.size <= maxPoints) return kept
            tolerance *= 2
        }
        return evenlySpaced(points, maxPoints)
    }

    private fun douglasPeucker(points: List<LatLngPoint>, toleranceMeters: Double): List<LatLngPoint> {
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(0 to points.lastIndex)
        while (stack.isNotEmpty()) {
            val (from, to) = stack.removeLast()
            var worst = -1.0
            var worstIndex = -1
            for (i in from + 1 until to) {
                val d = distanceToSegmentMeters(points[i], points[from], points[to])
                if (d > worst) { worst = d; worstIndex = i }
            }
            if (worstIndex >= 0 && worst > toleranceMeters) {
                keep[worstIndex] = true
                stack.addLast(from to worstIndex)
                stack.addLast(worstIndex to to)
            }
        }
        return points.filterIndexed { i, _ -> keep[i] }
    }

    private fun evenlySpaced(points: List<LatLngPoint>, count: Int): List<LatLngPoint> =
        List(count) { i -> points[(i.toLong() * points.lastIndex / (count - 1)).toInt()] }

    /** Flat-earth distance from [p] to the segment [a]-[b], good enough at trail scale. */
    private fun distanceToSegmentMeters(p: LatLngPoint, a: LatLngPoint, b: LatLngPoint): Double {
        val lngScale = METERS_PER_DEGREE_LAT * cos(Math.toRadians(a.lat))
        val bx = (b.lng - a.lng) * lngScale
        val by = (b.lat - a.lat) * METERS_PER_DEGREE_LAT
        val px = (p.lng - a.lng) * lngScale
        val py = (p.lat - a.lat) * METERS_PER_DEGREE_LAT
        val lengthSquared = bx * bx + by * by
        if (lengthSquared == 0.0) return hypot(px, py)
        val t = ((px * bx + py * by) / lengthSquared).coerceIn(0.0, 1.0)
        return hypot(px - t * bx, py - t * by)
    }

    // ---- compact text form: the standard "encoded polyline" at six decimal places ----

    fun encode(points: List<LatLngPoint>): String {
        val out = StringBuilder()
        var previousLat = 0L
        var previousLng = 0L
        for (p in points) {
            val lat = (p.lat * POLYLINE_SCALE).roundToLong()
            val lng = (p.lng * POLYLINE_SCALE).roundToLong()
            appendValue(out, lat - previousLat)
            appendValue(out, lng - previousLng)
            previousLat = lat
            previousLng = lng
        }
        return out.toString()
    }

    fun decode(encoded: String): List<LatLngPoint> {
        val points = ArrayList<LatLngPoint>()
        var index = 0
        var lat = 0L
        var lng = 0L
        while (index < encoded.length) {
            var shift = 0
            var result = 0L
            var chunk: Int
            do {
                chunk = encoded[index++].code - 63
                result = result or ((chunk and 0x1f).toLong() shl shift)
                shift += 5
            } while (chunk >= 0x20 && index < encoded.length)
            lat += if (result and 1L != 0L) (result shr 1).inv() else result shr 1

            shift = 0
            result = 0L
            do {
                chunk = encoded[index++].code - 63
                result = result or ((chunk and 0x1f).toLong() shl shift)
                shift += 5
            } while (chunk >= 0x20 && index < encoded.length)
            lng += if (result and 1L != 0L) (result shr 1).inv() else result shr 1

            points += LatLngPoint(lat / POLYLINE_SCALE, lng / POLYLINE_SCALE)
        }
        return points
    }

    private fun appendValue(out: StringBuilder, value: Long) {
        var v = (value shl 1) xor (value shr 63)
        while (v >= 0x20) {
            out.append(((0x20L or (v and 0x1f)) + 63).toInt().toChar())
            v = v shr 5
        }
        out.append((v + 63).toInt().toChar())
    }
}
