package com.comp350sec001group2.ekhoexplorer.Business.Species

import android.text.Html
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SpeciesApiResult(
    val id: Int,
    val scientificName: String,
    val commonName: String?,
    val category: String?,
    val imageUrl: String?,
    val description: String?
)

class SpeciesApiService {

    private val baseUrl = "https://api.inaturalist.org/v1/taxa"
    private val browseUrl =
        "https://api.inaturalist.org/v1/observations/species_counts"

    companion object {
        private val requestMutex = Mutex()
        private var lastRequestTime = 0L
        private const val requestInterval = 1100L
    }

    private val cache = ConcurrentHashMap<String, Pair<Long, List<SpeciesApiResult>>>()
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
        if (cache.size >= 30) {
            cache.clear()
        }

        cache[key] = SystemClock.elapsedRealtime() to results
    }

    suspend fun searchSpecies(query: String): List<SpeciesApiResult> =
        withContext(Dispatchers.IO) {

            if (query.isBlank()) {
                return@withContext emptyList()
            }

            val cacheKey = "search:${query.trim().lowercase(Locale.ROOT)}"

            getCached(cacheKey)?.let {
                return@withContext it
            }

            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")

            val response = getResponse(
                URL("$baseUrl?q=$encodedQuery&per_page=20")
            )

            val results = JSONObject(response).getJSONArray("results")
            val speciesList = mutableListOf<SpeciesApiResult>()

            for (i in 0 until results.length()) {
                parseSpecies(results.getJSONObject(i))?.let {
                    speciesList.add(it)
                }
            }

            saveCached(cacheKey, speciesList)
            speciesList
        }

    suspend fun browseSpecies(category: String): List<SpeciesApiResult> =
        withContext(Dispatchers.IO) {

            val taxonId = when (category) {
                "Plants" -> 47126
                "Animals" -> 1
                "Insects" -> 47157
                "Fungi" -> 47170
                else -> return@withContext emptyList()
            }

            val cacheKey = "browse:$category"

            getCached(cacheKey)?.let {
                return@withContext it
            }

            val response = getResponse(
                URL("$browseUrl?taxon_id=$taxonId&per_page=50")
            )

            val results = JSONObject(response).getJSONArray("results")
            val speciesList = mutableListOf<SpeciesApiResult>()

            for (i in 0 until results.length()) {
                val taxon = results
                    .getJSONObject(i)
                    .optJSONObject("taxon") ?: continue

                parseSpecies(taxon)?.let {
                    speciesList.add(it)
                }
            }
            saveCached(cacheKey, speciesList)
            speciesList
        }

    suspend fun getSpeciesDetails(id: Int): SpeciesApiResult? =
        withContext(Dispatchers.IO) {

            val response = getResponse(
                URL("$baseUrl/$id")
            )

            val results = JSONObject(response).getJSONArray("results")

            if (results.length() == 0) {
                return@withContext null
            }

            parseSpecies(results.getJSONObject(0))
        }

    private suspend fun getResponse(url: URL): String =
        requestMutex.withLock {

            // Wait before sending another API request.
            val elapsed = SystemClock.elapsedRealtime() - lastRequestTime
            val waitTime = requestInterval - elapsed

            if (lastRequestTime != 0L && waitTime > 0) {
                delay(waitTime)
            }

            lastRequestTime = SystemClock.elapsedRealtime()

            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> Unit

                    429 -> throw Exception(
                        "Too many requests. Please wait a moment and try again."
                    )

                    in 500..599 -> throw Exception(
                        "iNaturalist is temporarily unavailable. Please try again later."
                    )

                    else -> throw Exception(
                        "Unable to load species. Please try again."
                    )
                }

                connection.inputStream.bufferedReader().use {
                    it.readText()
                }
            } finally {
                connection.disconnect()
            }
        }

    private fun parseSpecies(species: JSONObject): SpeciesApiResult? {

        val id = species.optInt("id", 0)
        val scientificName = species.optString("name")

        if (id == 0 || scientificName.isBlank()) {
            return null
        }

        val photo = species.optJSONObject("default_photo")

        val rawDescription = species
            .optString("wikipedia_summary")
            .ifBlank { null }

        val description = rawDescription?.let {
            Html.fromHtml(
                it,
                Html.FROM_HTML_MODE_LEGACY
            ).toString().trim().ifBlank { null }
        }

        return SpeciesApiResult(
            id = id,
            scientificName = scientificName,
            commonName = species
                .optString("preferred_common_name")
                .ifBlank { null },
            category = species
                .optString("iconic_taxon_name")
                .ifBlank { null },
            imageUrl = photo
                ?.optString("medium_url")
                ?.ifBlank { null },
            description = description
        )
    }
}
