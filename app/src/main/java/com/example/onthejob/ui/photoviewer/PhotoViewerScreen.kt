package com.example.onthejob.ui.photoviewer

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.example.onthejob.ui.theme.MonoFontFamily

@Composable
fun PhotoViewerScreen(
    imageUrls: List<String>,
    startIndex: Int,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: PhotoViewerViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PhotoViewerViewModel(context.applicationContext as android.app.Application) }
        },
    )

    val saveState by viewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (imageUrls.size - 1).coerceAtLeast(0)),
        pageCount = { imageUrls.size },
    )

    LaunchedEffect(saveState) {
        when (val state = saveState) {
            is SaveState.Saved -> {
                snackbarHostState.showSnackbar("Saved to gallery")
                viewModel.resetSaveState()
            }
            is SaveState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }

            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }

            if (imageUrls.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                    color = Color.White,
                    fontFamily = MonoFontFamily,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                IconButton(
                    onClick = { viewModel.saveCurrentPhoto(imageUrls[pagerState.currentPage]) },
                    enabled = saveState !is SaveState.Saving,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                ) {
                    if (saveState is SaveState.Saving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        SaveIcon()
                    }
                }
            }
        }
    }
}

/** Placeholder custom icon to avoid depending on material-icons-extended for
 *  a single icon — swap for Icons.Filled.Download if that dependency is
 *  already present in the project. */
@Composable
private fun SaveIcon() {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w / 2, 0f), androidx.compose.ui.geometry.Offset(w / 2, h * 0.6f), strokeWidth)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.35f), androidx.compose.ui.geometry.Offset(w / 2, h * 0.65f), strokeWidth)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.35f), androidx.compose.ui.geometry.Offset(w / 2, h * 0.65f), strokeWidth)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, h * 0.85f), androidx.compose.ui.geometry.Offset(w, h * 0.85f), strokeWidth)
    }
}
