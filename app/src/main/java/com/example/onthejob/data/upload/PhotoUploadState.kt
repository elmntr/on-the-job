package com.example.onthejob.data.upload

import android.net.Uri
import java.io.File

sealed class PhotoUploadState {
    data object Pending : PhotoUploadState()
    data class Uploading(val progress: Float) : PhotoUploadState()
    data class Success(val url: String) : PhotoUploadState()
    data class Failed(val message: String) : PhotoUploadState()
}

data class PickedPhoto(
    val uri: Uri,
    val localFile: File,
    val state: PhotoUploadState = PhotoUploadState.Pending,
)