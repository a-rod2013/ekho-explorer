package com.comp350sec001group2.ekhoexplorer.Business.Database

import java.security.SecureRandom

/**
 * Makes a document id the same way Firestore's own auto-ids look (20 letters and digits), when
 * the id has to be known before the write happens.
 */
object FirestoreIds {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val random = SecureRandom()

    fun newId(): String = buildString(20) { repeat(20) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
}
