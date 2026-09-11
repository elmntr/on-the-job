package com.example.onthejob.data.ojt

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-scoped ViewModel (shared across screens via the Activity's ViewModelStore)
 * that manages:
 *   1. The migration — runs once on first sign-in after the update.
 *   2. The list of all instances for the current user.
 *   3. The currently-active instance (which scopes LogFeed, Calendar, HoursCard, PDF export).
 *   4. The "Format with AI" user preference (Feature 1).
 *
 * Lifetime: AndroidViewModel (lives until the app process dies), so the
 * active-instance selection persists through screen transitions.
 *
 * The active instance id is also saved to SharedPreferences so it survives
 * app restarts without requiring a fresh Firestore round-trip.
 */
class OjtInstanceViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "on_the_job_ojt_prefs"
        private const val KEY_ACTIVE_INSTANCE_ID = "active_instance_id"
        private const val KEY_USE_AI_FORMATTING = "use_ai_formatting"
        const val DEFAULT_HOURS_REQUIRED = 486.0
    }

    private val repo = OjtInstanceRepository()
    private val prefs: SharedPreferences =
        getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    // ── Feature 1: AI formatting preference ─────────────────────────────────

    private val _useAiFormatting = MutableStateFlow(
        prefs.getBoolean(KEY_USE_AI_FORMATTING, true) // default ON
    )
    /** Whether the "Format with AI" toggle is enabled. Persisted across sessions. */
    val useAiFormatting: StateFlow<Boolean> = _useAiFormatting.asStateFlow()

    fun setUseAiFormatting(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_AI_FORMATTING, enabled).apply()
        _useAiFormatting.value = enabled
    }

    // ── Feature 2: OJT instances ─────────────────────────────────────────────

    /** All instances for the user, live-updated from Firestore. */
    val instances: StateFlow<List<OjtInstance>> =
        (userId?.let { repo.getInstances(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The currently-selected OJT instance id.
     * Initialised from SharedPreferences; updated when the user picks an
     * instance or when the migration completes and returns the default.
     */
    private val _activeInstanceId = MutableStateFlow(
        prefs.getString(KEY_ACTIVE_INSTANCE_ID, "") ?: ""
    )
    val activeInstanceId: StateFlow<String> = _activeInstanceId.asStateFlow()

    /** Full object for the active instance (null while loading). */
    val activeInstance: StateFlow<OjtInstance?> =
        kotlinx.coroutines.flow.combine(instances, activeInstanceId) { list, id ->
            list.find { it.id == id } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Migration status — used by screens to show a brief loading state.
     * Transitions: Idle → Running → Done (or Error).
     */
    sealed class MigrationState {
        data object Idle : MigrationState()
        data object Running : MigrationState()
        data object Done : MigrationState()
        data class Error(val message: String) : MigrationState()
    }

    private val _migrationState = MutableStateFlow<MigrationState>(MigrationState.Idle)
    val migrationState: StateFlow<MigrationState> = _migrationState.asStateFlow()

    init {
        runMigrationIfNeeded()
    }

    /**
     * Runs the idempotent migration. On success, pins the active instance id
     * if it isn't already set (first launch) or validates that the saved id
     * still exists (handles deleted instance edge case).
     */
    private fun runMigrationIfNeeded() {
        val uid = userId ?: return
        _migrationState.value = MigrationState.Running

        viewModelScope.launch {
            // Legacy hoursRequired is kept in the old SharedPreferences key
            // so we can carry it forward as the default instance's target.
            val legacyPrefs = getApplication<Application>().getSharedPreferences(
                "on_the_job_user_prefs", Context.MODE_PRIVATE
            )
            val legacyHours = legacyPrefs.getFloat("hours_required", DEFAULT_HOURS_REQUIRED.toFloat()).toDouble()

            val result = repo.ensureDefaultInstance(uid, legacyHours)
            result.fold(
                onSuccess = { defaultInstanceId ->
                    // If no active instance was previously saved, use the
                    // migration-returned default.
                    if (_activeInstanceId.value.isBlank()) {
                        setActiveInstance(defaultInstanceId)
                    }
                    _migrationState.value = MigrationState.Done
                },
                onFailure = { e ->
                    if (_activeInstanceId.value.isBlank()) {
                        setActiveInstance("default")
                    }
                    _migrationState.value = MigrationState.Error(
                        e.message ?: "Could not set up your OJT data."
                    )
                },
            )
        }
    }

    fun setActiveInstance(instanceId: String) {
        prefs.edit().putString(KEY_ACTIVE_INSTANCE_ID, instanceId).apply()
        _activeInstanceId.value = instanceId
    }

    /** Creates a new instance and switches to it. */
    fun createInstance(name: String, hoursRequired: Double) {
        val uid = userId ?: return
        viewModelScope.launch {
            val result = repo.createInstance(
                userId = uid,
                instance = OjtInstance(name = name, hoursRequired = hoursRequired),
            )
            result.onSuccess { newId -> setActiveInstance(newId) }
        }
    }

    /** Updates an instance's name or target hours. */
    fun updateInstanceTarget(instanceId: String, name: String, hoursRequired: Double) {
        val uid = userId ?: return
        viewModelScope.launch {
            repo.updateInstance(uid, instanceId, name, hoursRequired)
        }
    }
}
