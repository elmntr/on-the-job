package com.example.onthejob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.onthejob.ui.auth.AuthViewModel
import com.example.onthejob.ui.auth.SignInScreen
import com.example.onthejob.ui.auth.SignInUiState

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.safeDrawingPadding()) {
                    val uiState by authViewModel.uiState.collectAsState()
                    val webClientId = stringResource(R.string.default_web_client_id)

                    when (uiState) {
                        is SignInUiState.SignedIn -> {
                            // TODO: replace with real nav shell — home/log feed screen
                        }
                        else -> {
                            SignInScreen(
                                uiState = uiState,
                                onSignInClick = { authViewModel.signIn(webClientId) },
                            )
                        }
                    }
                }
            }
        }
    }
}