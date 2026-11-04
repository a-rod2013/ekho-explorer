package com.comp350sec001group2.ekhoexplorer.Business.ImageService

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class SupabaseConfig(projectUrl: String, apiKey: String, bucket: String) {
    val baseUrl: HttpUrl
    val apiKey: String = apiKey.trim()
    val bucket: String = bucket.trim()

    init {
        val parsed = projectUrl.trim().toHttpUrlOrNull()
        require(parsed != null) { "projectUrl is not a valid URL" }
        require(parsed.encodedPath == "/" && parsed.query == null && parsed.fragment == null) {
            "projectUrl must be just the project origin."
        }
        require(this.apiKey.isNotEmpty()) { "apiKey must not be blank" }
        require(BUCKET_NAME.matches(this.bucket)) { "bucket must contain only letters, digits, '-' and '_'" }
        baseUrl = parsed
    }

    /** Path prefix every public object URL in this bucket starts with. */
    val publicPrefix: String = "/storage/v1/object/public/${this.bucket}/"

    /** Endpoint to POST the bytes to for an object at [path]. */
    fun uploadUrl(path: String): HttpUrl =
        baseUrl.newBuilder().addPathSegments("storage/v1/object/$bucket/$path").build()

    /** The permanent, shareable URL for the object at [path]. */
    fun publicUrl(path: String): String =
        baseUrl.newBuilder().addPathSegments("storage/v1/object/public/$bucket/$path").build().toString()

    /** True only for a plain URL on this project's host that points into this bucket. */
    fun owns(url: String): Boolean = pathOf(url) != null

    /** "folder/name.jpg" for one of our public URLs, or null for anything else. */
    fun pathOf(url: String): String? {
        val parsed = url.trim().toHttpUrlOrNull() ?: return null
        if (parsed.scheme != baseUrl.scheme || parsed.host != baseUrl.host || parsed.port != baseUrl.port) return null
        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) return null
        if (parsed.query != null || parsed.fragment != null) return null
        val path = parsed.encodedPath
        if (!path.startsWith(publicPrefix) || path.length == publicPrefix.length) return null
        return path.removePrefix(publicPrefix)
    }

    /** Never prints the key. */
    override fun toString(): String =
        "SupabaseConfig(projectUrl=$baseUrl, bucket=$bucket, apiKey=<redacted>)"

    private companion object {
        val BUCKET_NAME = Regex("[A-Za-z0-9_-]{1,100}")
    }
}
