package com.example.onthejob.data.upload

internal fun photoFormat(bytes: ByteArray): String? {
    fun starts(signature: IntArray) = bytes.size >= signature.size && signature.indices.all { (bytes[it].toInt() and 255) == signature[it] }
    return when {
        starts(intArrayOf(255, 216, 255)) -> "jpg"
        starts(intArrayOf(137, 80, 78, 71, 13, 10, 26, 10)) -> "png"
        bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "webp"
        else -> null
    }
}
