package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.StoryItem
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoriesRow(
    stories: List<StoryItem>,
    onAddStoryClick: () -> Unit,
    onStoryClick: (StoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        // "Add Story" Button / My Story
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onAddStoryClick() }
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EmeraldPrimary, Color(0xFF0F3238))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "إضافة قصة",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Mini badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(GoldSecondary)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "قصتك +",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        // Stories List
        items(stories) { story ->
            val ringBrush = if (!story.isViewed) {
                Brush.sweepGradient(
                    listOf(
                        GoldSecondary,
                        EmeraldPrimary,
                        Color(0xFFFFD700),
                        GoldSecondary
                    )
                )
            } else {
                Brush.sweepGradient(listOf(Color.Gray.copy(alpha = 0.5f), Color.LightGray.copy(alpha = 0.5f)))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onStoryClick(story) }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ringBrush)
                        .padding(2.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = story.authorAvatarUrl,
                        contentDescription = story.authorName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.authorName.split(" ").firstOrNull() ?: story.authorName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StoryViewerDialog(
    stories: List<StoryItem>,
    initialStoryIndex: Int = 0,
    onDismiss: () -> Unit,
    onDeleteStory: (String) -> Unit
) {
    if (stories.isEmpty()) {
        onDismiss()
        return
    }

    var currentIndex by remember { mutableIntStateOf(initialStoryIndex.coerceIn(0, stories.size - 1)) }
    val currentStory = stories[currentIndex]
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Timer effect for auto-advancing story
    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            val remainingDuration = ((1f - progress.value) * (currentStory.durationSeconds * 1000)).toInt()
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remainingDuration.coerceAtLeast(100),
                    easing = LinearEasing
                )
            )
            // Auto advance
            if (currentIndex < stories.size - 1) {
                progress.snapTo(0f)
                currentIndex++
            } else {
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Tap Left: Previous story
                                if (currentIndex > 0) {
                                    currentIndex--
                                    coroutineScope.launch { progress.snapTo(0f) }
                                }
                            } else {
                                // Tap Right: Next story
                                if (currentIndex < stories.size - 1) {
                                    currentIndex++
                                    coroutineScope.launch { progress.snapTo(0f) }
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Media View: Video or Image
            if (currentStory.isVideo && currentStory.mediaUri.startsWith("content://") || currentStory.mediaUri.endsWith(".mp4")) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(currentStory.mediaUri))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                        }
                    },
                    update = { view ->
                        if (isPaused) {
                            if (view.isPlaying) view.pause()
                        } else {
                            if (!view.isPlaying) view.start()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = currentStory.mediaUri,
                    contentDescription = currentStory.textCaption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dark Top and Bottom Gradients
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Top Progress Bars and Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Progress segments
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { idx, _ ->
                        val segmentProgress = when {
                            idx < currentIndex -> 1f
                            idx == currentIndex -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                // Author Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = currentStory.authorAvatarUrl,
                            contentDescription = currentStory.authorName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.dp, GoldSecondary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentStory.authorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "منذ ساعات قليلة • قصة يومية 🌙",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentStory.isMyStory) {
                            IconButton(onClick = {
                                onDeleteStory(currentStory.id)
                                Toast.makeText(context, "تم حذف القصة بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                                if (stories.size <= 1) {
                                    onDismiss()
                                } else {
                                    currentIndex = (currentIndex - 1).coerceAtLeast(0)
                                    coroutineScope.launch { progress.snapTo(0f) }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف القصة", tint = Color.Red)
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }
                }
            }

            // Caption Text at Bottom
            if (currentStory.textCaption.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 32.dp)
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = currentStory.textCaption,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
