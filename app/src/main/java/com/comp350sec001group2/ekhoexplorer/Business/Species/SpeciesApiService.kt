package com.comp350sec001group2.ekhoexplorer.Business.Species

import android.text.Html
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun searchSpecies(query: String): List<SpeciesApiResult> =
        withContext(Dispatchers.IO) {

            if (query.isBlank()) {
                return@withContext emptyList()
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

    private fun getResponse(url: URL): String {

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception(
                    "API request failed: ${connection.responseCode}"
                )
            }

            return connection.inputStream.bufferedReader().use {
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
