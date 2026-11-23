package com.comp350sec001group2.ekhoexplorer.Business.Species

import android.os.SystemClock
import android.text.Html
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SpeciesApiResult(
    val id: Int,
    val scientificName: String,
    val commonName: String?,
    val category: String?,
    val imageUrl: String?,
    val description: String?
)

data class SpeciesPlace(
    val id: Int,
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

class SpeciesApiService {
    private val taxaUrl = "https://api.inaturalist.org/v1/taxa"
    private val browseUrl = "https://api.inaturalist.org/v1/observations/species_counts"

    companion object {
        private val requestMutex = Mutex()
        private var lastRequestTime = 0L
        private const val requestInterval = 1100L
    }

    private val cache = ConcurrentHashMap<String, Pair<Long, List<SpeciesApiResult>>>()
    private val taxonIdCache = ConcurrentHashMap<String, Int>()
    private val cacheDuration = 5 * 60 * 1000L

    private fun getCached(key: String): List<SpeciesApiResult>? {
        val entry = cache[key] ?: return null
        return if (SystemClock.elapsedRealtime() - entry.first < cacheDuration) {
            entry.second
        } else {
            cache.remove(key)
            null
        }
    }

    private fun saveCached(key: String, results: List<SpeciesApiResult>) {
        if (cache.size >= 30) cache.clear()
        cache[key] = SystemClock.elapsedRealtime() to results
    }

    // Taxonomy is resolved by exact scientific name, never a guessed ID.
    private suspend fun resolveTaxonId(scientificName: String): Int {
        taxonIdCache[scientificName]?.let { return it }
        val encoded = URLEncoder.encode(scientificName, "UTF-8")
        val response = getResponse(URL("$taxaUrl/autocomplete?q=$encoded&per_page=30"))
        val results = JSONObject(response).getJSONArray("results")
        for (i in 0 until results.length()) {
            val candidate = results.getJSONObject(i)
            if (candidate.optString("name").equals(scientificName, ignoreCase = true)) {
                val id = candidate.optInt("id")
                if (id > 0) {
                    taxonIdCache[scientificName] = id
                    return id
                }
            }
        }
        throw IllegalStateException("Unable to find the $scientificName taxonomy on iNaturalist.")
    }

    suspend fun searchPlaces(query: String): List<SpeciesPlace> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val response = getResponse(URL("https://api.inaturalist.org/v1/places/autocomplete?q=$encoded&per_page=10"))
        val results = JSONObject(response).getJSONArray("results")
        buildList {
            for (i in 0 until results.length()) {
                val place = results.getJSONObject(i)
                val id = place.optInt("id")
                val name = place.optString("display_name")
                    .ifBlank { place.optString("name") }

                val coordinates = place.optString("location").split(",")
                val latitude = coordinates.getOrNull(0)?.toDoubleOrNull()
                val longitude = coordinates.getOrNull(1)?.toDoubleOrNull()

                if (id > 0 && name.isNotBlank()) {
                    add(SpeciesPlace(id, name, latitude, longitude))
                }
            }
        }
    }

    // All filtering is applied to observations on the server, before the top 50 species are selected.
    // All filtering is applied to observations on the server, before the top 50 species are selected.
    suspend fun browseSpecies(
        category: String,
        placeId: Int? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Int = 5,
        taxonomy: String? = null,
        query: String = ""
    ): List<SpeciesApiResult> = withContext(Dispatchers.IO) {
        val categoryId = when (category) {
            "Animals" -> 1
            "Plants" -> 47126
            "Insects" -> 47158
            "Fungi" -> 47170
            else -> null
        }
        val resolvedId = taxonomy?.let { resolveTaxonId(it) } ?: categoryId
        val search = query.trim()
        val key = "browse:$category:$resolvedId:$placeId:$latitude:$longitude:$radius:${search.lowercase(Locale.ROOT)}"
        getCached(key)?.let { return@withContext it }
        val url = buildString {
            append("$browseUrl?per_page=50")
            if (resolvedId != null) append("&taxon_id=$resolvedId")
            if (placeId != null) append("&place_id=$placeId")
            if (latitude != null && longitude != null) {
                append("&lat=$latitude&lng=$longitude&radius=$radius")
            }
            if (search.isNotBlank()) {
                append("&q=${URLEncoder.encode(search, "UTF-8")}&search_on=names")
            }
        }

        val results = JSONObject(getResponse(URL(url))).getJSONArray("results")
        val speciesList = mutableListOf<SpeciesApiResult>()
        for (i in 0 until results.length()) {
            val taxon = results.getJSONObject(i).optJSONObject("taxon") ?: continue
            parseSpecies(taxon)?.let(speciesList::add)
        }
        saveCached(key, speciesList)
        speciesList
    }

    // Retained for any other callers that still use the original name-search method.
    suspend fun searchSpecies(query: String): List<SpeciesApiResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val key = "search:${query.trim().lowercase(Locale.ROOT)}"
        getCached(key)?.let { return@withContext it }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val results = JSONObject(getResponse(URL("$taxaUrl?q=$encoded&per_page=20"))).getJSONArray("results")
        val speciesList = mutableListOf<SpeciesApiResult>()
        for (i in 0 until results.length()) {
            parseSpecies(results.getJSONObject(i))?.let(speciesList::add)
        }
        saveCached(key, speciesList)
        speciesList
    }

    suspend fun getSpeciesDetails(id: Int): SpeciesApiResult? = withContext(Dispatchers.IO) {
        val results = JSONObject(getResponse(URL("$taxaUrl/$id"))).getJSONArray("results")
        if (results.length() == 0) null else parseSpecies(results.getJSONObject(0))
    }

    private suspend fun getResponse(url: URL): String = requestMutex.withLock {
        val elapsed = SystemClock.elapsedRealtime() - lastRequestTime
        val waitTime = requestInterval - elapsed
        if (lastRequestTime != 0L && waitTime > 0) delay(waitTime)
        lastRequestTime = SystemClock.elapsedRealtime()
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                429 -> throw Exception("Too many requests. Please wait a moment and try again.")
                in 500..599 -> throw Exception("iNaturalist is temporarily unavailable. Please try again later.")
                else -> throw Exception("Unable to load species. Please try again.")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSpecies(species: JSONObject): SpeciesApiResult? {
        val id = species.optInt("id", 0)
        val scientificName = species.optString("name")
        if (id == 0 || scientificName.isBlank()) return null
        val photo = species.optJSONObject("default_photo")
        val description = species.optString("wikipedia_summary").ifBlank { null }?.let {
            Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim().ifBlank { null }
        }
        return SpeciesApiResult(
            id = id,
            scientificName = scientificName,
            commonName = species.optString("preferred_common_name").ifBlank { null },
            category = species.optString("iconic_taxon_name").ifBlank { null },
            imageUrl = photo?.optString("medium_url")?.ifBlank { null },
            description = description
        )
    }
}
