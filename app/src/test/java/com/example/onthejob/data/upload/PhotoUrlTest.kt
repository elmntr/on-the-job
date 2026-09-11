package com.example.onthejob.data.upload
import org.junit.Assert.assertEquals
import org.junit.Test
class PhotoUrlTest {
    private val root = "https://res.cloudinary.com/dskoyv2oe/image/upload/"
    @Test fun svgFromWebUsesPngPreview() {
        assertEquals(root + "f_png/v1789003786/fsacqimotz7ehmpepvrw.svg", photoPreviewUrl(root + "v1789003786/fsacqimotz7ehmpepvrw.svg"))
    }
    @Test fun unsupportedCameraFormatsUseCompatiblePreview() {
        for (extension in listOf("heic", "heif", "tif", "tiff", "avif"))
            assertEquals(root + "f_png/v1/photo.$extension", photoPreviewUrl(root + "v1/photo.$extension"))
    }
    @Test fun regularImagesAndExistingPreviewAreUnchanged() {
        for (url in listOf(root + "v1/photo.jpg", root + "v1/photo.png", root + "f_png/v1/photo.svg", "https://example.com/photo.svg", "bad url"))
            assertEquals(url, photoPreviewUrl(url))
    }
}
