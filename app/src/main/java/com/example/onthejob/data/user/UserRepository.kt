package com.example.onthejob.data.user

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Manages user settings and profile preferences locally using SharedPreferences.
 * Avoids direct reads/writes to users/{userId} document which are restricted by
 * Firestore security rules (only users/{userId}/entries/{entryId} is allowed).
 */
class UserRepository(
    context: Context,
) {
    companion object {
        const val DEFAULT_REQUIRED_HOURS = 486.0
        private const val PREFS_NAME = "on_the_job_user_prefs"
        private const val KEY_HOURS_REQUIRED = "hours_required"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _requiredHours = MutableStateFlow(
        prefs.getFloat(KEY_HOURS_REQUIRED, DEFAULT_REQUIRED_HOURS.toFloat()).toDouble()
    )

    fun getRequiredHours(): Flow<Double> = _requiredHours.asStateFlow()

    suspend fun updateRequiredHours(hours: Double): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                prefs.edit().putFloat(KEY_HOURS_REQUIRED, hours.toFloat()).apply()
                _requiredHours.value = hours
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
