package com.example.onthejob.ui.entrydetail

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_PHOTOS = 15
private const val UPLOAD_PRESET = "onthejob_unsigned"
private const val RATE_LIMIT_RETRY_DELAY_MS = 1500L

sealed class EntryUpdateState {
    data object Idle : EntryUpdateState()
    data object Saving : EntryUpdateState()
    data object Saved : EntryUpdateState()
    data class Error(val message: String) : EntryUpdateState()
}

sealed class RegenerateState {
    data object Idle : RegenerateState()
    data object Loading : RegenerateState()
    data class Error(val message: String) : RegenerateState()
}

class EntryDetailViewModel(
    application: Application,
    private val entryId: String,
) : AndroidViewModel(application) {

    private val entryRepository = EntryRepository()
    private val aiFormatClient = AiFormatClient()
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    /** Live document — reflects Regenerate/edit saves and any future Cron retry immediately. */
    val entry: StateFlow<Entry?> =
        (userId?.let { entryRepository.getEntry(it, entryId) } ?: flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editedText = MutableStateFlow("")
    val editedText: StateFlow<String> = _editedText.asStateFlow()

    private val _editedHours = MutableStateFlow(0.0)
    val editedHours: StateFlow<Double> = _editedHours.asStateFlow()

    /** Existing already-uploaded photo URLs, minus any removed during this edit session. */
    private val _existingPhotoUrls = MutableStateFlow<List<String>>(emptyList())
    val existingPhotoUrls: StateFlow<List<String>> = _existingPhotoUrls.asStateFlow()

    /** Newly added photos this edit session — same upload pipeline as New Entry. */
    private val _newPhotos = MutableStateFlow<List<PickedPhoto>>(emptyList())
    val newPhotos: StateFlow<List<PickedPhoto>> = _newPhotos.asStateFlow()

    private val _updateState = MutableStateFlow<EntryUpdateState>(EntryUpdateState.Idle)
    val updateState: StateFlow<EntryUpdateState> = _updateState.asStateFlow()

    private val _regenerateState = MutableStateFlow<RegenerateState>(RegenerateState.Idle)
    val regenerateState: StateFlow<RegenerateState> = _regenerateState.asStateFlow()

    /** Enters edit mode, seeding editable fields from the currently-loaded entry. */
    fun startEditing() {
        val current = entry.value ?: return
        _editedText.value = current.text
        _editedHours.value = current.hours
        _existingPhotoUrls.value = current.imageUrls
        _newPhotos.value = emptyList()
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
        _newPhotos.value.forEach { it.localFile.delete() } // don't leave orphaned cached files
        _newPhotos.value = emptyList()
        _updateState.value = EntryUpdateState.Idle
        _regenerateState.value = RegenerateState.Idle
    }

    fun onTextChanged(text: String) {
        _editedText.value = text
    }

    fun onHoursChanged(hours: Double) {
        _editedHours.value = hours
    }

    fun removeExistingPhoto(url: String) {
        _existingPhotoUrls.value = _existingPhotoUrls.value.filter { it != url }
    }

    fun onPhotosPicked(uris: List<Uri>) {
        val room = MAX_PHOTOS - (_existingPhotoUrls.value.size + _newPhotos.value.size)
        if (room <= 0) return
        val clamped = uris.take(room)
        viewModelScope.launch {
            val cached = clamped.mapNotNull { uri ->
                val file = withContext(Dispatchers.IO) { PhotoCache.cache(getApplication(), uri) }
                file?.let { PickedPhoto(uri = uri, localFile = it) }
            }
            _newPhotos.value = _newPhotos.value + cached
            cached.forEach { uploadNewPhoto(it.uri) }
        }
    }

    fun retryNewPhoto(uri: Uri) = uploadNewPhoto(uri)

    fun removeNewPhoto(uri: Uri) {
        _newPhotos.value.find { it.uri == uri }?.localFile?.delete()
        _newPhotos.value = _newPhotos.value.filter { it.uri != uri }
    }

    private fun uploadNewPhoto(uri: Uri) {
        viewModelScope.launch {
            val photo = _newPhotos.value.find { it.uri == uri } ?: return@launch

            if (!NetworkStatus.isOnline(getApplication())) {
                updateNewPhotoState(
                    uri,
                    PhotoUploadState.Failed("No internet — will upload automatically once you're back online"),
                )
                return@launch
            }

            updateNewPhotoState(uri, PhotoUploadState.Uploading(0f))
            val uploader = CloudinaryUploader()
            val cloudName = getApplication<Application>().getString(R.string.cloudinary_cloud_name)
            val result = uploader.upload(
                file = photo.localFile,
                cloudName = cloudName,
                uploadPreset = UPLOAD_PRESET,
                onProgress = { progress -> updateNewPhotoState(uri, PhotoUploadState.Uploading(progress)) },
            )
            result.fold(
                onSuccess = { url ->
                    updateNewPhotoState(uri, PhotoUploadState.Success(url))
                    photo.localFile.delete()
                },
                onFailure = { e ->
                    updateNewPhotoState(uri, PhotoUploadState.Failed(e.message ?: "Upload failed"))
                },
            )
        }
    }

    private fun updateNewPhotoState(uri: Uri, state: PhotoUploadState) {
        _newPhotos.value = _newPhotos.value.map { if (it.uri == uri) it.copy(state = state) else it }
    }

    /**
     * Re-calls the AI formatter using the entry's original rawText (not the
     * currently edited text — Regenerate means "try the AI again from
     * scratch," not "reformat my manual edits"). On success, updates the
     * editable text field for review; does NOT persist until Save is
     * tapped, matching "editable preview before saving."
     */
    fun regenerate() {
        val current = entry.value ?: return
        val user = FirebaseAuth.getInstance().currentUser ?: return

        viewModelScope.launch {
            _regenerateState.value = RegenerateState.Loading

            if (!NetworkStatus.isOnline(getApplication())) {
                _regenerateState.value = RegenerateState.Error("No internet — connect and try again.")
                return@launch
            }

            val idToken = runCatching { user.getIdToken(false).awaitTask().token }.getOrNull()
            if (idToken == null) {
                _regenerateState.value = RegenerateState.Error("Couldn't verify your sign-in — try again.")
                return@launch
            }

            var result = attemptFormat(current.rawText, idToken)
            if (result is AiFormatResult.Failure && result.reason == "unauthorized") {
                val freshToken = runCatching { user.getIdToken(true).awaitTask().token }.getOrNull()
                if (freshToken != null) result = attemptFormat(current.rawText, freshToken)
            }

            when (result) {
                is AiFormatResult.Success -> {
                    _editedText.value = result.formattedText
                    _regenerateState.value = RegenerateState.Idle
                }
                is AiFormatResult.Failure -> {
                    _regenerateState.value = RegenerateState.Error(
                        when (result.reason) {
                            "quota_exhausted" -> "Daily AI limit reached — this will auto-retry once quota resets, or try again tomorrow."
                            else -> "Couldn't reach the AI right now — try again in a moment."
                        },
                    )
                }
            }
        }
    }

    private suspend fun attemptFormat(rawText: String, idToken: String): AiFormatResult {
        val first = aiFormatClient.format(rawText, idToken)
        if (first is AiFormatResult.Failure && first.reason == "rate_limited") {
            delay(RATE_LIMIT_RETRY_DELAY_MS)
            return aiFormatClient.format(rawText, idToken)
        }
        return first
    }

    /**
     * Saves edits: text, hours, and the final photo list (kept existing
     * URLs + any new photos that finished uploading in time). Any new photo
     * still uploading when Save is tapped is handed to WorkManager exactly
     * like New Entry's own flow — appendImageUrl on this same entryId once
     * it finishes, surviving app kill/reboot.
     */
    fun save() {
        val current = entry.value ?: return
        val uid = userId ?: return

        viewModelScope.launch {
            _updateState.value = EntryUpdateState.Saving

            val uploadedNewUrls = _newPhotos.value.mapNotNull { (it.state as? PhotoUploadState.Success)?.url }
            val finalImageUrls = _existingPhotoUrls.value + uploadedNewUrls

            // Regenerate already replaced editedText in-place if it succeeded (setting status to "done");
            // if the entry was "skipped", "failed_quota", or "failed_other", and the user didn't regenerate it,
            // keep its existing formattingStatus rather than silently overriding it.
            val newStatus = if (_editedText.value != current.text && current.formattingStatus != "skipped") "done" else current.formattingStatus

            entryRepository.updateEntry(
                userId = uid,
                entryId = entryId,
                text = _editedText.value,
                hours = _editedHours.value,
                imageUrls = finalImageUrls,
                formattingStatus = newStatus,
            ).fold(
                onSuccess = {
                    enqueuePendingPhotoUploads(uid)
                    _isEditing.value = false
                    _updateState.value = EntryUpdateState.Saved
                },
                onFailure = { e -> _updateState.value = EntryUpdateState.Error(e.message ?: "Could not save changes.") },
            )
        }
    }

    private fun enqueuePendingPhotoUploads(userId: String) {
        val cloudName = getApplication<Application>().getString(R.string.cloudinary_cloud_name)
        val pending = _newPhotos.value.filter { it.state !is PhotoUploadState.Success }

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
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(getApplication()).enqueue(request)
        }
    }

    fun resetUpdateState() {
        _updateState.value = EntryUpdateState.Idle
    }
}