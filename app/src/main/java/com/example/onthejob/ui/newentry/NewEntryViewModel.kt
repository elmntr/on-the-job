package com.example.onthejob.ui.newentry

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.onthejob.R
import com.example.onthejob.data.aiformat.AiFormatClient
import com.example.onthejob.data.aiformat.AiFormatResult
import com.example.onthejob.data.entry.Entry
import com.example.onthejob.data.entry.EntryRepository
import com.example.onthejob.data.upload.CloudinaryUploader
import com.example.onthejob.data.upload.PhotoCache
import com.example.onthejob.data.upload.PhotoUploadState
import com.example.onthejob.data.upload.PhotoUploadWorker
import com.example.onthejob.data.upload.PickedPhoto
import com.example.onthejob.util.NetworkStatus
import com.example.onthejob.util.awaitTask
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val UPLOAD_PRESET = "onthejob_unsigned"
private const val MAX_PHOTOS = 15
private const val RATE_LIMIT_RETRY_DELAY_MS = 1500L

sealed class SaveState {
    data object Idle : SaveState()
    data object Saving : SaveState()
    data object Saved : SaveState()
    data class Error(val message: String) : SaveState()
}

class NewEntryViewModel(application: Application) : AndroidViewModel(application) {
    private val uploader = CloudinaryUploader()
    private val aiFormatClient = AiFormatClient()
    private val entryRepository = EntryRepository()

    private val _photos = MutableStateFlow<List<PickedPhoto>>(emptyList())
    val photos: StateFlow<List<PickedPhoto>> = _photos.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _hours = MutableStateFlow(8.0)
    val hours: StateFlow<Double> = _hours.asStateFlow()

    private val _entryDate = MutableStateFlow(LocalDate.now())
    val entryDate: StateFlow<LocalDate> = _entryDate.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    /** Clamped defensively even though the date picker's SelectableDates already
     *  restricts to today-or-earlier — belt and suspenders against any future
     *  caller that sets this some other way. */
    fun onEntryDateChanged(date: LocalDate) {
        _entryDate.value = if (date.isAfter(LocalDate.now())) LocalDate.now() else date
    }

    fun onDescriptionChanged(text: String) {
        _description.value = text
    }

    fun onHoursChanged(hours: Double) {
        _hours.value = hours
    }

    /**
     * Photos are cached to durable app-internal storage immediately on pick,
     * before any upload is attempted — this is what makes offline-first
     * upload possible later, since a content:// Uri isn't guaranteed to stay
     * readable across app restarts, but our own copy in filesDir is.
     */
    fun onPhotosPicked(uris: List<Uri>) {
        val clamped = uris.take(MAX_PHOTOS)
        viewModelScope.launch {
            val cached = clamped.mapNotNull { uri ->
                val file = withContext(Dispatchers.IO) { PhotoCache.cache(getApplication(), uri) }
                file?.let { PickedPhoto(uri = uri, localFile = it) }
            }
            _photos.value = cached
            cached.forEach { uploadPhoto(it.uri) }
        }
    }

    fun retry(uri: Uri) = uploadPhoto(uri)

    fun removePhoto(uri: Uri) {
        // Clean up the cached local copy too, not just the in-memory state,
        // so removed photos don't linger as orphaned files.
        _photos.value.find { it.uri == uri }?.localFile?.delete()
        _photos.value = _photos.value.filter { it.uri != uri }
    }

    private fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            val photo = _photos.value.find { it.uri == uri } ?: return@launch

            if (!NetworkStatus.isOnline(getApplication())) {
                updatePhotoState(
                    uri,
                    PhotoUploadState.Failed("No internet — will upload automatically once you're back online"),
                )
                return@launch
            }

            updatePhotoState(uri, PhotoUploadState.Uploading(0f))
            val cloudName = getApplication<Application>().getString(R.string.cloudinary_cloud_name)
            val result = uploader.upload(
                file = photo.localFile,
                cloudName = cloudName,
                uploadPreset = UPLOAD_PRESET,
                onProgress = { progress -> updatePhotoState(uri, PhotoUploadState.Uploading(progress)) },
            )
            result.fold(
                onSuccess = { url ->
                    updatePhotoState(uri, PhotoUploadState.Success(url))
                    photo.localFile.delete() // uploaded and will be recorded on save — cached copy no longer needed
                },
                onFailure = { e ->
                    updatePhotoState(uri, PhotoUploadState.Failed(e.message ?: "Upload failed"))
                },
            )
        }
    }

    private fun updatePhotoState(uri: Uri, state: PhotoUploadState) {
        _photos.value = _photos.value.map { if (it.uri == uri) it.copy(state = state) else it }
    }

    /**
     * Full save flow: description -> AI format (with retry/fallback) -> Firestore.
     * The entry is ALWAYS saved, even if AI formatting fails, or some photos
     * haven't finished uploading yet — those get handed off to WorkManager
     * to finish in the background and attach themselves once done.
     */
    fun saveEntry() {
        val rawText = _description.value.trim()
        if (rawText.isEmpty()) {
            _saveState.value = SaveState.Error("Please add a description before saving.")
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _saveState.value = SaveState.Error("You're signed out — please sign in again.")
            return
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Saving

            val currentPhotos = _photos.value
            val imageUrls = currentPhotos.mapNotNull { (it.state as? PhotoUploadState.Success)?.url }

            var finalText = rawText
            var formattingStatus = "failed_other"

            val idToken = if (NetworkStatus.isOnline(getApplication())) {
                runCatching { user.getIdToken(false).awaitTask().token }.getOrNull()
            } else {
                null
            }

            if (idToken != null) {
                var result = attemptFormat(rawText, idToken)

                if (result is AiFormatResult.Failure && result.reason == "unauthorized") {
                    val freshToken = runCatching { user.getIdToken(true).awaitTask().token }.getOrNull()
                    if (freshToken != null) {
                        result = attemptFormat(rawText, freshToken)
                    }
                }

                when (result) {
                    is AiFormatResult.Success -> {
                        finalText = result.formattedText
                        formattingStatus = "done"
                    }
                    is AiFormatResult.Failure -> {
                        formattingStatus = if (result.reason == "quota_exhausted") "failed_quota" else "failed_other"
                    }
                }
            }

            val entry = Entry(
                userId = user.uid,
                rawText = rawText,
                text = finalText,
                imageUrls = imageUrls,
                hours = _hours.value,
                formattingStatus = formattingStatus,
                entryDate = _entryDate.value.toString(), // LocalDate.toString() is already ISO "yyyy-MM-dd"
            )

            entryRepository.saveEntry(entry).fold(
                onSuccess = { entryId ->
                    enqueuePendingPhotoUploads(entryId, user.uid, currentPhotos)
                    _saveState.value = SaveState.Saved
                },
                onFailure = { e -> _saveState.value = SaveState.Error(e.message ?: "Could not save entry.") },
            )
        }
    }

    /**
     * Any photo not yet uploaded by the time Save succeeds gets handed to
     * WorkManager with a network constraint — it'll run automatically once
     * connectivity returns (even after an app kill or reboot) and append its
     * URL to this entry once it succeeds. No manual retry needed.
     */
    private fun enqueuePendingPhotoUploads(entryId: String, userId: String, photos: List<PickedPhoto>) {
        val cloudName = getApplication<Application>().getString(R.string.cloudinary_cloud_name)
        val pending = photos.filter { it.state !is PhotoUploadState.Success }

        pending.forEach { photo ->
            val data = workDataOf(
                PhotoUploadWorker.KEY_USER_ID to userId,
                PhotoUploadWorker.KEY_ENTRY_ID to entryId,
                PhotoUploadWorker.KEY_FILE_PATH to photo.localFile.absolutePath,
                PhotoUploadWorker.KEY_CLOUD_NAME to cloudName,
                PhotoUploadWorker.KEY_UPLOAD_PRESET to UPLOAD_PRESET,
            )
            val request = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build()
            WorkManager.getInstance(getApplication()).enqueue(request)
        }
    }

    /** One quiet retry on rate_limited, per CLAUDE.md's "resolves in seconds" UX. */
    private suspend fun attemptFormat(rawText: String, idToken: String): AiFormatResult {
        val first = aiFormatClient.format(rawText, idToken)
        if (first is AiFormatResult.Failure && first.reason == "rate_limited") {
            delay(RATE_LIMIT_RETRY_DELAY_MS)
            return aiFormatClient.format(rawText, idToken)
        }
        return first
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}