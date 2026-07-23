package com.example.onthejob.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Failure(val message: String) : AuthResult()
    object Cancelled : AuthResult()
}

class AuthRepository(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val credentialManager = CredentialManager.create(context)

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun signInWithGoogle(webClientId: String): AuthResult {
        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(generateNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleIdTokenCredential.idToken, null
                )
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) AuthResult.Success(user)
                else AuthResult.Failure("Sign-in succeeded but no user was returned.")
            } else {
                AuthResult.Failure("Unexpected credential type.")
            }
        } catch (e: GoogleIdTokenParsingException) {
            AuthResult.Failure("Could not parse Google ID token: ${e.message}")
        } catch (e: GetCredentialException) {
            // Covers user cancellation, no accounts on device, etc.
            AuthResult.Cancelled
        } catch (e: Exception) {
            AuthResult.Failure(e.message ?: "Unknown sign-in error.")
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        credentialManager.clearCredentialState(
            ClearCredentialStateRequest()
        )
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}