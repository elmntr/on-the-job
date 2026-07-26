package com.example.onthejob.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onthejob.ui.theme.Paper

@Composable
private fun PlaceholderScreen(title: String, content: @Composable ColumnScope.() -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title)
        content()
    }
}


@Composable
fun TimeTrackingScreen() {
    PlaceholderScreen("Time Tracking")
}


@Composable
fun EntryDetailScreen(entryId: String, onBack: () -> Unit) {
    PlaceholderScreen("Entry: $entryId") {
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun PdfExportScreen(onBack: () -> Unit) {
    PlaceholderScreen("PDF Export") {
        Button(onClick = onBack) { Text("Back") }
    }
}