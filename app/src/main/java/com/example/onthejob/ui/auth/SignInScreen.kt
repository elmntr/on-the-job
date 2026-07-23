package com.example.onthejob.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.onthejob.R

@Composable
fun SignInScreen(
    uiState: SignInUiState,
    onSignInClick: () -> Unit,
) {
    val webClientId = stringResource(R.string.default_web_client_id)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ON THE JOB", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your OJT hours, documented daily.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onSignInClick,
            enabled = uiState !is SignInUiState.Loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState is SignInUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Continue with Google")
            }
        }

        if (uiState is SignInUiState.Error) {
            Spacer(Modifier.height(16.dp))
            Text(
                uiState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}