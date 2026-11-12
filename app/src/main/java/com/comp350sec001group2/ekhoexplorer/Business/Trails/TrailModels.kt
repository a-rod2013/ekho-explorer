package com.comp350sec001group2.ekhoexplorer.Business.Trails

enum class TrailVisibility(val key: String) {
    PUBLIC("public"),
    FRIENDS("friends"),
    PRIVATE("private");

    companion object {
        fun fromKey(key: String): TrailVisibility? = entries.firstOrNull { it.key.equals(key.trim(), ignoreCase = true) }
    }
}

enum class TrailStatus(val key: String) {
    UNVERIFIED("unverified"),
    VERIFIED("verified");

    companion object {
        fun fromKey(key: String): TrailStatus? = entries.firstOrNull { it.key.equals(key.trim(), ignoreCase = true) }
    }
}

data class LatLngPoint(val lat: Double, val lng: Double)

data class Trail(
    val id: String,
    val ownerId: String,
    val ownerName: String,
    val name: String,
    val description: String,
    val location: String,
    val lengthMeters: Double,
    val difficulty: Int,
    val visibility: TrailVisibility,
    val status: TrailStatus,
    val verificationRequested: Boolean,
    val pathEncoded: String,
    val startLat: Double?,
    val startLng: Double?,
    val photoCount: Int,
    val galleryPhotos: List<String>,
    val ratingSum: Long,
    val ratingCount: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val start: LatLngPoint? get() = if (startLat != null && startLng != null) LatLngPoint(startLat, startLng) else null
}

data class TrailInput(
    val name: String,
    val description: String,
    val location: String,
    val lengthMeters: Double,
    val difficulty: Int,
    val visibility: TrailVisibility,
    val photoCount: Int,
)

data class Viewer(val userId: String, val isAdmin: Boolean, val following: Set<String>)
