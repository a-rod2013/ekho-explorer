package com.comp350sec001group2.ekhoexplorer.Business.Trails

import java.io.File

interface WalkStore {
    fun append(point: LatLngPoint)
    fun load(): List<LatLngPoint>
    fun clear()
}

class FileWalkStore(private val file: File) : WalkStore {

    override fun append(point: LatLngPoint) {
        file.parentFile?.mkdirs()
        file.appendText("${point.lat},${point.lng}\n")
    }

    override fun load(): List<LatLngPoint> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size != 2) return@mapNotNull null
            val lat = parts[0].toDoubleOrNull()
            val lng = parts[1].toDoubleOrNull()
            if (lat == null || lng == null) null else LatLngPoint(lat, lng)
        }
    }

    override fun clear() {
        file.delete()
    }
}
