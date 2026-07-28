package com.example.onthejob.ui.photoviewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Saved : SaveState
    data class Error(val message: String) : SaveState
}

class PhotoViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    fun saveCurrentPhoto(imageUrl: String) {
        if (_saveState.value is SaveState.Saving) return
        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            _saveState.value = when (val result = PhotoSaver.saveToGallery(getApplication(), imageUrl)) {
                is PhotoSaver.Result.Success -> SaveState.Saved
                is PhotoSaver.Result.Error -> SaveState.Error(result.message)
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}
