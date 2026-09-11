package com.example.onthejob.data.upload

import java.net.URI

/** Use a compatible raster preview without changing the stored original URL. */
fun photoPreviewUrl(source: String): String {
    return try {
        val uri = URI(source)
        val path = uri.rawPath ?: return source
        if (uri.scheme != "https" || uri.host != "res.cloudinary.com" ||
            !path.startsWith("/dskoyv2oe/image/upload/") ||
            !Regex("\\.(svg|heic|heif|tif|tiff|avif)$", RegexOption.IGNORE_CASE).containsMatchIn(path) ||
            path.contains("/f_png/")) return source
        source.replaceFirst("/image/upload/", "/image/upload/f_png/")
    } catch (_: Exception) { source }
}
