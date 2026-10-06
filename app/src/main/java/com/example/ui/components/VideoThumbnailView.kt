package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.util.VideoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoThumbnailView(
    thumbnailUrl: String,
    videoUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var resolvedThumbnail by remember(thumbnailUrl, videoUrl) {
        mutableStateOf(thumbnailUrl.ifBlank { null })
    }

    LaunchedEffect(thumbnailUrl, videoUrl) {
        if (resolvedThumbnail.isNullOrBlank() || resolvedThumbnail?.startsWith("file://") == true) {
            val extracted = withContext(Dispatchers.IO) {
                VideoUtils.getOrExtractThumbnail(context, videoUrl, thumbnailUrl)
            }
            resolvedThumbnail = extracted
        }
    }

    Box(modifier = modifier) {
        if (!resolvedThumbnail.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(resolvedThumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant spiritual emerald gradient fallback while thumbnail loads
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0C5A42),
                                Color(0xFF063B2B),
                                Color(0xFF031E16)
                            )
                        )
                    )
            )
        }
    }
}
