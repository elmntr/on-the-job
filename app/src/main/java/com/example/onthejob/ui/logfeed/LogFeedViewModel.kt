package com.example.onthejob.ui.logfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onthejob.data.entry.Entry
import com.example.onthejob.data.entry.EntryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Placeholder until CLAUDE.md's open question ("is hours-required user
// input, or preset by course/program?") is actually resolved. Matches the
// mockup's example value — NOT a real settings-backed number yet.
private const val PLACEHOLDER_REQUIRED_HOURS = 486.0

class LogFeedViewModel(
    private val entryRepository: EntryRepository = EntryRepository(),
) : ViewModel() {

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    val entries: StateFlow<List<Entry>> =
        (userId?.let { entryRepository.getEntries(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hoursRendered: StateFlow<Double> =
        entries.map { list -> list.sumOf { it.hours } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val hoursRequired: Double = PLACEHOLDER_REQUIRED_HOURS
}