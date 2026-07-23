package com.example.onthejob.ui.newentry

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.onthejob.R
import com.example.onthejob.data.upload.CloudinaryUploader
import com.example.onthejob.data.upload.PhotoUploadState
import com.example.onthejob.data.upload.PickedPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val UPLOAD_PRESET = "onthejob_unsigned"
private const val MAX_PHOTOS = 15

class NewEntryViewModel(application: Application) : AndroidViewModel(application) {
    private val uploader = CloudinaryUploader()

    private val _photos = MutableStateFlow<List<PickedPhoto>>(emptyList())
    val photos: StateFlow<List<PickedPhoto>> = _photos.asStateFlow()

    fun onPhotosPicked(uris: List<Uri>) {
        val clamped = uris.take(MAX_PHOTOS)
        _photos.value = clamped.map { PickedPhoto(it) }
        clamped.forEach { uploadPhoto(it) }
    }

    fun retry(uri: Uri) = uploadPhoto(uri)

    fun removePhoto(uri: Uri) {
        _photos.value = _photos.value.filter { it.uri != uri }
    }

    private fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            updateState(uri, PhotoUploadState.Uploading(0f))
            val cloudName = getApplication<Application>().getString(R.string.cloudinary_cloud_name)
            val result = uploader.upload(
                context = getApplication(),
                uri = uri,
                cloudName = cloudName,
                uploadPreset = UPLOAD_PRESET,
                onProgress = { progress -> updateState(uri, PhotoUploadState.Uploading(progress)) },
            )
            result.fold(
                onSuccess = { url -> updateState(uri, PhotoUploadState.Success(url)) },
                onFailure = { e -> updateState(uri, PhotoUploadState.Failed(e.message ?: "Upload failed")) },
            )
        }
    }

    private fun updateState(uri: Uri, state: PhotoUploadState) {
        _photos.value = _photos.value.map { if (it.uri == uri) it.copy(state = state) else it }
    }
}