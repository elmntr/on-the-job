package com.example.onthejob.util

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridges a Google Play Services Task into a suspend call, without pulling in
 * kotlinx-coroutines-play-services as a new dependency. Used for both
 * FirebaseAuth.getIdToken() and Firestore write calls.
 */
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
    addOnCanceledListener {
        cont.cancel()
    }
}