package com.comp350sec001group2.ekhoexplorer.Business.Photos

/** A photo - WebP bytes within the size budget, plus its pixel size. */
class EncodedPhoto(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
) {
    val sizeBytes: Int get() = bytes.size
}

/** Which kinds of file the encoder accepts, recognised by their first bytes, not their name. */
enum class PhotoFormat {
    JPEG, PNG, WEBP;

    companion object {
        private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

        fun detect(b: ByteArray): PhotoFormat? = when {
            b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte() -> JPEG
            b.size >= PNG_SIGNATURE.size && PNG_SIGNATURE.indices.all { b[it] == PNG_SIGNATURE[it] } -> PNG
            b.size >= 12 &&
                b[0] == 'R'.code.toByte() && b[1] == 'I'.code.toByte() && b[2] == 'F'.code.toByte() && b[3] == 'F'.code.toByte() &&
                b[8] == 'W'.code.toByte() && b[9] == 'E'.code.toByte() && b[10] == 'B'.code.toByte() && b[11] == 'P'.code.toByte() -> WEBP
            else -> null
        }
    }
}
