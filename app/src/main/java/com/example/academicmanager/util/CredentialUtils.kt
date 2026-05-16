package com.example.academicmanager.util

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random

object CredentialUtils {

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateUsername(fullName: String, title: String): String {
        // Common academic titles to strip
        val titlesToStrip = listOf("prof.", "dr.", "doç.", "öğr.", "gör.", "ars.", "uzm.")
        
        var namePart = fullName.lowercase(Locale("tr", "TR"))
        
        titlesToStrip.forEach { t ->
            namePart = namePart.replace(t, "")
        }

        // Custom normalization for Turkish characters
        namePart = namePart
            .replace('ş', 's')
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ö', 'o')
            .replace('ç', 'c')
            .replace("i̇", "i") // Handle dotted i normalization issues

        // Normalize and remove non-ascii, then format as first_last
        val cleanName = Normalizer.normalize(namePart, Normalizer.Form.NFD)
            .replace("[^\\p{ASCII}]".toRegex(), "")
            .trim()
            .replace("\\s+".toRegex(), "_")

        return cleanName
    }

    fun generatePassword(): String {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..6)
            .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
            .joinToString("")
    }
}
