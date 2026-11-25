package com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards

data class CardIdentity(
    val cardId: String,
    val taxonId: Int?,
    val commonName: String,
    val scientificName: String,
    val category: JournalCategory,
    val description: String = "",
    val descriptionSource: String = "",
    val referencePhotoUrl: String? = null,
    val referencePhotoAttribution: String? = null,
) {
    companion object {
        fun species(
            taxonId: Int,
            commonName: String,
            scientificName: String,
            category: JournalCategory,
            description: String = "",
            descriptionSource: String = "",
            referencePhotoUrl: String? = null,
            referencePhotoAttribution: String? = null,
        ) = CardIdentity(
            CardIds.forTaxon(taxonId), taxonId, commonName, scientificName, category,
            description, descriptionSource, referencePhotoUrl, referencePhotoAttribution,
        )

        fun unknown(category: JournalCategory) = CardIdentity(
            CardIds.unknown(category), null, "Unidentified ${category.name.lowercase()}", "", category,
        )
    }
}

data class GeoPoint(val latitude: Double, val longitude: Double) {
    val isValid: Boolean get() = latitude in -90.0..90.0 && longitude in -180.0..180.0
}
// ===== JOURNAL B4: end =====
