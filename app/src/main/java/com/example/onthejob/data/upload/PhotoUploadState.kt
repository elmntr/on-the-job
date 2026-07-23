package com.example.onthejob.data.upload

import android.net.Uri

sealed class PhotoUploadState {
    data object Pending : PhotoUploadState()
    data class Uploading(val progress: Float) : PhotoUploadState()
    data class Success(val url: String) : PhotoUploadState()
    data class Failed(val message: String) : PhotoUploadState()
}

data class PickedPhoto(
    val uri: Uri,
    val state: PhotoUploadState = PhotoUploadState.Pending,
)