package com.example.onthejob.ui.logfeed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.onthejob.data.entry.Entry
import com.example.onthejob.data.entry.EntryRepository
import com.example.onthejob.data.user.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogFeedViewModel @JvmOverloads constructor(
    application: Application,
    private val entryRepository: EntryRepository = EntryRepository(),
    private val userRepository: UserRepository = UserRepository(application),
) : AndroidViewModel(application) {

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    val entries: StateFlow<List<Entry>> =
        (userId?.let { entryRepository.getEntries(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hoursRendered: StateFlow<Double> =
        entries.map { list -> list.sumOf { it.hours } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val hoursRequired: StateFlow<Double> =
        userRepository.getRequiredHours()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserRepository.DEFAULT_REQUIRED_HOURS)

    fun updateHoursRequired(newGoal: Double) {
        viewModelScope.launch {
            userRepository.updateRequiredHours(newGoal)
        }
    }
}