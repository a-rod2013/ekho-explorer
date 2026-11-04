package com.comp350sec001group2.ekhoexplorer.Business.ImageService

enum class ImageFormat(val mimeType: String) {
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp"),
}

/**
 * Identifies an image by its first bytes, not by file name or a server-supplied type.
 */
object ImageSniffer {
    fun detect(bytes: ByteArray): ImageFormat? = when {
        isJpeg(bytes) -> ImageFormat.JPEG
        isPng(bytes) -> ImageFormat.PNG
        isWebp(bytes) -> ImageFormat.WEBP
        else -> null
    }

    private fun isJpeg(b: ByteArray) =
        b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte()

    private val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    private fun isPng(b: ByteArray) =
        b.size >= pngSignature.size && pngSignature.indices.all { b[it] == pngSignature[it] }

    // "RIFF" + 4 size bytes + "WEBP"
    private fun isWebp(b: ByteArray) =
        b.size >= 12 &&
            b[0] == 'R'.code.toByte() && b[1] == 'I'.code.toByte() && b[2] == 'F'.code.toByte() && b[3] == 'F'.code.toByte() &&
            b[8] == 'W'.code.toByte() && b[9] == 'E'.code.toByte() && b[10] == 'B'.code.toByte() && b[11] == 'P'.code.toByte()
}
