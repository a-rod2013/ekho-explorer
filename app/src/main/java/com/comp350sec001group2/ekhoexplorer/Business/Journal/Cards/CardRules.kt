package com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards

enum class JournalCategory {
    PLANT, ANIMAL, INSECT, FUNGI;

    companion object {
        fun fromIconicTaxon(name: String?): JournalCategory? = when (name?.trim()) {
            "Plantae" -> PLANT
            "Aves", "Mammalia", "Reptilia", "Amphibia", "Actinopterygii", "Mollusca", "Animalia" -> ANIMAL
            "Insecta", "Arachnida" -> INSECT
            "Fungi" -> FUNGI
            else -> null
        }
    }
}

object CardIds {
    fun forTaxon(taxonId: Int): String = taxonId.toString()
    fun unknown(category: JournalCategory): String = "unknown_${category.name.lowercase()}"
    fun isUnknown(id: String): Boolean = id.startsWith("unknown_")
    fun unknownCategory(id: String): JournalCategory? =
        if (isUnknown(id)) JournalCategory.entries.firstOrNull { it.name.equals(id.removePrefix("unknown_"), ignoreCase = true) } else null
}

object PinRules {
    private const val DAY = 24L * 60 * 60 * 1000

    fun lifetimeMillis(category: JournalCategory): Long = when (category) {
        JournalCategory.ANIMAL, JournalCategory.INSECT -> 3 * DAY
        JournalCategory.FUNGI -> 14 * DAY
        JournalCategory.PLANT -> 30 * DAY
    }

    fun expiresAt(category: JournalCategory, createdAtMillis: Long): Long = createdAtMillis + lifetimeMillis(category)

    fun roundForPublic(degrees: Double): Double = Math.round(degrees * 1000.0) / 1000.0
}

object Geohash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    const val PRECISION = 5

    fun encode(latitude: Double, longitude: Double, precision: Int = PRECISION): String {
        var latMin = -90.0
        var latMax = 90.0
        var lngMin = -180.0
        var lngMax = 180.0
        val out = StringBuilder()
        var bits = 0
        var ch = 0
        var longitudeBit = true
        while (out.length < precision) {
            if (longitudeBit) {
                val mid = (lngMin + lngMax) / 2
                if (longitude >= mid) { ch = ch * 2 + 1; lngMin = mid } else { ch *= 2; lngMax = mid }
            } else {
                val mid = (latMin + latMax) / 2
                if (latitude >= mid) { ch = ch * 2 + 1; latMin = mid } else { ch *= 2; latMax = mid }
            }
            longitudeBit = !longitudeBit
            if (++bits == 5) {
                out.append(BASE32[ch])
                bits = 0
                ch = 0
            }
        }
        return out.toString()
    }

    fun cellsCovering(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double, maxCells: Int = 30): Set<String>? {
        val latStep = 180.0 / (1 shl 12)   // 5 characters = 25 bits: 13 for longitude, 12 for latitude
        val lngStep = 360.0 / (1 shl 13)
        val cells = linkedSetOf<String>()
        var lat = minLat
        while (true) {
            var lng = minLng
            while (true) {
                cells += encode(lat.coerceIn(-90.0, 90.0), lng.coerceIn(-180.0, 180.0))
                if (cells.size > maxCells) return null
                if (lng >= maxLng) break
                lng = minOf(lng + lngStep, maxLng)
            }
            if (lat >= maxLat) break
            lat = minOf(lat + latStep, maxLat)
        }
        return cells
    }
}
