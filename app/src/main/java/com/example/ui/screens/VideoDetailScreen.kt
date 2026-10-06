package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.data.model.VideoItem
import com.example.data.repository.IslamicDataRepository
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.VideoCardItem
import com.example.ui.components.VideoThumbnailView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.util.VideoUtils
import java.io.File
import kotlinx.coroutines.delay

data class SimilarVideo(
    val id: String,
    val title: String,
    val author: String,
    val views: String,
    val time: String,
    val duration: String,
    val thumbnail: String,
    val videoUrl: String = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    videoId: String,
    onBack: () -> Unit,
    onNavigateToChannel: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val videos by IslamicDataRepository.videosFeed.collectAsState()
    var currentVideoId by remember(videoId) { mutableStateOf(videoId) }
    val currentVideo = videos.find { it.id == currentVideoId }
        ?: videos.firstOrNull()
        ?: VideoItem(
            id = currentVideoId,
            title = "مقطع إيماني من روحانيات",
            authorName = "روحانيات",
            authorHandle = "rowhaniyat",
            authorAvatarUrl = "",
            thumbnailUrl = "",
            videoUrl = "",
            viewsCount = 1,
            timeAgo = "الآن",
            durationFormatted = "05:00",
            likesCount = 0,
            commentsCount = 0,
            repostsCount = 0
        )

    val creatorsMap by IslamicDataRepository.creatorsMap.collectAsState()
    val authorProfile = remember(currentVideo.authorHandle, creatorsMap) {
        IslamicDataRepository.getUser(currentVideo.authorHandle)
    }
    val isAuthorVerified = authorProfile.isVerified
    val isFollowingAuthor = authorProfile.isFollowing

    val commentsMap by IslamicDataRepository.comments.collectAsState()
    val comments = commentsMap[currentVideo.id] ?: emptyList()

    // Player states
    var isPlaying by remember(currentVideo.id) { mutableStateOf(true) }
    var playbackError by remember(currentVideo.id) { mutableStateOf(false) }
    var isPrepared by remember(currentVideo.id) { mutableStateOf(false) }
    var isMuted by remember(currentVideo.id) { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var retryCount by remember(currentVideo.id) { mutableIntStateOf(0) }
    var playbackProgress by remember(currentVideo.id) { mutableFloatStateOf(0f) }
    var mediaPlayerRef by remember(currentVideo.id) { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRef by remember(currentVideo.id) { mutableStateOf<VideoView?>(null) }

    // User interactions
    val currentUser by IslamicDataRepository.currentUser.collectAsState()
    var likesCount by remember(currentVideo.id) { mutableLongStateOf(currentVideo.likesCount) }
    var isLiked by remember(currentVideo.id) { mutableStateOf(currentVideo.isLiked) }
    var isDisliked by remember(currentVideo.id) { mutableStateOf(false) }
    var isSaved by remember(currentVideo.id) { mutableStateOf(currentVideo.isSaved) }
    var isFollowing by remember(currentVideo.id) { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var commentText by remember { mutableStateOf("") }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf("1080p FHD (الأعلى)") }
    var showQualityDialog by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Description expanded state (independent per video, always starts collapsed on new video)
    var isDescriptionExpanded by remember(currentVideo.id) { mutableStateOf(false) }

    // Record view in database and StateFlow
    LaunchedEffect(currentVideo.id) {
        IslamicDataRepository.recordVideoView(currentVideo.id)
    }

    // Save watch position on departure
    DisposableEffect(currentVideo.id) {
        onDispose {
            videoViewRef?.let { view ->
                try {
                    val pos = view.currentPosition
                    if (pos > 1000) {
                        IslamicDataRepository.saveWatchPosition(currentVideo.id, pos.toLong())
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Playback progress loop
    LaunchedEffect(isPlaying, isPrepared, currentVideo.id) {
        if (isPlaying) {
            while (isPlaying) {
                delay(250)
                videoViewRef?.let { view ->
                    try {
                        val cur = view.currentPosition
                        val dur = view.duration
                        if (dur > 0) {
                            playbackProgress = (cur.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                        } else {
                            playbackProgress = (playbackProgress + 0.015f)
                            if (playbackProgress >= 1f) playbackProgress = 0f
                        }
                    } catch (_: Exception) {
                        playbackProgress = (playbackProgress + 0.015f)
                        if (playbackProgress >= 1f) playbackProgress = 0f
                    }
                } ?: run {
                    playbackProgress = (playbackProgress + 0.015f)
                    if (playbackProgress >= 1f) playbackProgress = 0f
                }
            }
        }
    }

    // Dynamic similar / suggested videos based on real data
    val otherVideos = remember(videos, currentVideo.id) {
        videos.filter { it.id != currentVideo.id }
    }
    val similarVideos = remember(otherVideos) {
        if (otherVideos.isNotEmpty()) {
            otherVideos.map { item ->
                SimilarVideo(
                    id = item.id,
                    title = item.title,
                    author = item.authorName,
                    views = "${item.viewsCount} مشاهدة",
                    time = item.timeAgo,
                    duration = item.durationFormatted,
                    thumbnail = item.thumbnailUrl,
                    videoUrl = item.videoUrl
                )
            }
        } else {
            listOf(
                SimilarVideo(
                    id = "sim_1",
                    title = "تلاوات خاشعة بصوت القارئ محمد اللحيدان من سورة مريم والكهف",
                    author = "الشيخ محمد اللحيدان",
                    views = "2.2 مليون مشاهدة",
                    time = "قبل 11 شهراً",
                    duration = "30:20",
                    thumbnail = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=800&auto=format&fit=crop&q=80"
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مشاهدة الفيديو 🌙",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showMoreOptionsSheet = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات إضافية")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. مشغل الفيديو المتقدم (يدعم العمودي 9:16 والأفقي 16:9 دون أي تشويه مع مشغل حقيقي)
            item {
                val isVertical = currentVideo.isVertical
                val containerModifier = if (isFullscreen) {
                    Modifier
                        .fillMaxWidth()
                        .height(600.dp)
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(if (isVertical) 480.dp else 230.dp)
                        .background(Color.Black)
                }

                Box(
                    modifier = containerModifier,
                    contentAlignment = Alignment.Center
                ) {
                    // Center video frame with exact aspect ratio (9:16 for vertical, 16:9 for horizontal)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(if (isVertical) 9f / 16f else 16f / 9f)
                            .background(Color.Black)
                            .clickable { isPlaying = !isPlaying },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!playbackError && currentVideo.videoUrl.isNotBlank()) {
                            key(currentVideo.id, retryCount) {
                                AndroidView(
                                    factory = { ctx ->
                                        VideoView(ctx).apply {
                                            videoViewRef = this
                                            VideoUtils.bindVideoView(
                                                videoView = this,
                                                videoUrl = currentVideo.videoUrl,
                                                onPrepared = { mp ->
                                                    mediaPlayerRef = mp
                                                    mp.isLooping = true
                                                    if (isMuted) mp.setVolume(0f, 0f) else mp.setVolume(1f, 1f)
                                                    isPrepared = true
                                                    playbackError = false
                                                    if (currentVideo.lastWatchedPositionMs > 1000L && currentVideo.lastWatchedPositionMs < duration) {
                                                        seekTo(currentVideo.lastWatchedPositionMs.toInt())
                                                        Toast.makeText(ctx, "تمت استعادة موضع المشاهدة السابق ⏱️", Toast.LENGTH_SHORT).show()
                                                    }
                                                    if (isPlaying) start()
                                                },
                                                onError = {
                                                    val uri = Uri.parse(currentVideo.videoUrl)
                                                    if (uri.scheme == "file") {
                                                        val path = uri.path ?: currentVideo.videoUrl.removePrefix("file://")
                                                        val file = File(path)
                                                        if (!file.exists()) {
                                                            playbackError = true
                                                        }
                                                    } else {
                                                        playbackError = true
                                                    }
                                                    isPrepared = false
                                                }
                                            )
                                        }
                                    },
                                    update = { view ->
                                        try {
                                            if (isPlaying) {
                                                if (!view.isPlaying && isPrepared) view.start()
                                            } else {
                                                if (view.isPlaying) view.pause()
                                            }
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Real Thumbnail Poster while loading or preparing (Zero black screen glitch!)
                        if (!isPrepared && !playbackError) {
                            VideoThumbnailView(
                                thumbnailUrl = currentVideo.thumbnailUrl,
                                videoUrl = currentVideo.videoUrl,
                                contentDescription = currentVideo.title,
                                contentScale = if (isVertical) ContentScale.Crop else ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.Center),
                                color = GoldSecondary,
                                strokeWidth = 3.dp
                            )
                        }

                        // Playback Error Overlay if file is deleted or unreadable
                        if (playbackError) {
                            VideoThumbnailView(
                                thumbnailUrl = currentVideo.thumbnailUrl,
                                videoUrl = currentVideo.videoUrl,
                                contentDescription = currentVideo.title,
                                contentScale = if (isVertical) ContentScale.Crop else ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFE74C3C),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "عذراً، تعذر تشغيل الفيديو أو تم نقل الملف من هاتفك",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            playbackError = false
                                            isPrepared = false
                                            retryCount++
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إعادة المحاولة", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Play / Pause center indicator on tap
                        AnimatedVisibility(
                            visible = !isPlaying && !playbackError,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "تشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        // Top player overlay: orientation badge & mute toggle & fullscreen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = if (isVertical) "عمودي 9:16 📱" else "أفقي 16:9 📺",
                                    color = GoldSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Quality Selector Button
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showQualityDialog = true },
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = "الجودة",
                                            tint = GoldSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = selectedQuality.take(5),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Playback Speed Selector Button
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showSpeedDialog = true },
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "السرعة",
                                            tint = GoldSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${playbackSpeed}x",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Mute / Unmute
                                IconButton(
                                    onClick = {
                                        isMuted = !isMuted
                                        mediaPlayerRef?.let { mp ->
                                            if (isMuted) mp.setVolume(0f, 0f) else mp.setVolume(1f, 1f)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = "كتم الصوت",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Fullscreen toggle
                                IconButton(
                                    onClick = { isFullscreen = !isFullscreen },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "ملء الشاشة",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Bottom duration pill & scrubber bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = currentVideo.durationFormatted,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            LinearProgressIndicator(
                                progress = { playbackProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .align(Alignment.BottomCenter),
                                color = GoldSecondary,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // 2. عنوان وبيانات والوصف المرتب (بالتسلسل المطلوب بدقة)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // أولاً: عنوان الفيديو
                    Text(
                        text = currentVideo.title.ifBlank { "تلاوة وموعظة من روحانيات" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // ثانياً: بيانات الفيديو (اسم الناشر • عدد المشاهدات • مدة الفيديو • تاريخ النشر)
                    Text(
                        text = "${currentVideo.authorName} • ${currentVideo.viewsCount} مشاهدة • ${currentVideo.durationFormatted} • ${currentVideo.timeAgo}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // ثالثاً: الوصف بنظام YouTube (مختصر بسطر أو سطرين مع زر «عرض المزيد» وتوسيع سلس)
                    if (currentVideo.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                                    .animateContentSize()
                            ) {
                                Text(
                                    text = currentVideo.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isDescriptionExpanded && currentVideo.hashtags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentVideo.hashtags.joinToString(" "),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (isDescriptionExpanded) "عرض أقل ▴" else "عرض المزيد ▾",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // شريط الأزرار والتفاعل (Actions Bar)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // زر الإعجاب
                            val heartColor by animateColorAsState(
                                targetValue = if (isLiked) Color(0xFFE74C3C) else MaterialTheme.colorScheme.onSurface,
                                label = "heartColor"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        isLiked = !isLiked
                                        if (isLiked) {
                                            likesCount++
                                            isDisliked = false
                                        } else {
                                            likesCount = (likesCount - 1).coerceAtLeast(0)
                                        }
                                        IslamicDataRepository.toggleVideoLike(currentVideo.id)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "إعجاب",
                                    tint = heartColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$likesCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            // زر عدم الإعجاب
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        isDisliked = !isDisliked
                                        if (isDisliked && isLiked) {
                                            isLiked = false
                                            likesCount = (likesCount - 1).coerceAtLeast(0)
                                            IslamicDataRepository.toggleVideoLike(currentVideo.id)
                                        }
                                        Toast.makeText(context, if (isDisliked) "تم تسجيل عدم الإعجاب" else "تم إلغاء التفاعل", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown,
                                    contentDescription = "لم يعجبني",
                                    tint = if (isDisliked) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("لم يعجبني", style = MaterialTheme.typography.labelSmall)
                            }

                            // زر إعادة النشر / قصتي
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        Toast.makeText(context, "تمت الإضافة إلى قصتك بنجاح 🌙", Toast.LENGTH_SHORT).show()
                                        IslamicDataRepository.toggleVideoRepost(currentVideo.id)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "قصتي",
                                    tint = if (currentVideo.isReposted) GoldSecondary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("قصتي", style = MaterialTheme.typography.labelSmall)
                            }

                            // زر الحفظ
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        isSaved = !isSaved
                                        IslamicDataRepository.toggleVideoSave(currentVideo.id)
                                        Toast.makeText(
                                            context,
                                            if (isSaved) "تم الحفظ في المفضلة 🔖" else "تمت الإزالة من المحفوظات",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "حفظ",
                                    tint = if (isSaved) GoldSecondary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(if (isSaved) "محفوظ" else "حفظ", style = MaterialTheme.typography.labelSmall)
                            }

                            // زر المشاركة
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "شاهد هذا الفيديو المميز على تطبيق روحانيات: https://rowhaniyat.app/video/${currentVideo.id}\n${currentVideo.title}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو"))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "مشاركة",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("مشاركة", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // كارت الناشر / الشيخ
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToChannel(currentVideo.authorHandle) }
                            ) {
                                AsyncImage(
                                    model = currentVideo.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200" },
                                    contentDescription = currentVideo.authorName,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = currentVideo.authorName.ifBlank { "الشيخ عثمان الخميس" },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isAuthorVerified) {
                                            VerifiedBadge(size = 15.dp)
                                        }
                                    }
                                    Text(
                                        text = "@${currentVideo.authorHandle.ifBlank { "othmanalkhamees" }} • ${authorProfile.followersCount} متابع",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val isNow = IslamicDataRepository.toggleFollowCreator(currentVideo.authorHandle, context)
                                    val toastMsg = if (isNow) "تمت متابعة ${authorProfile.name} ✓" else "تم إلغاء المتابعة"
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowingAuthor) MaterialTheme.colorScheme.surfaceVariant else EmeraldPrimary,
                                    contentColor = if (isFollowingAuthor) MaterialTheme.colorScheme.onSurface else Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isFollowingAuthor) "مُتابع" else "متابعة",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. شرائح تصفية المحتوى المشابه (مثل يوتيوب)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterChips = listOf("الكل", "محتوى مشابه", "من نتائج البحث", "تلاوات خاشعة")
                    filterChips.forEach { chip ->
                        val isSelected = selectedFilter == chip
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = chip },
                            label = { Text(chip, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // 4. قائمة الفيديوهات المشابهة أسفل الوصف (بنفس تصميم فيديوهات الصفحة الرئيسية 16:9 الموحد)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "فيديوهات مقترحة 🎬",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            items(otherVideos) { item ->
                VideoCardItem(
                    video = item,
                    onClick = {
                        currentVideoId = item.id
                        isPlaying = true
                        isPrepared = false
                        playbackError = false
                        playbackProgress = 0f
                        retryCount = 0
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                        Toast.makeText(context, "جاري تشغيل: ${item.title}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isOwner = item.authorHandle == currentUser.username,
                    onAuthorClick = { handle -> onNavigateToChannel(handle) }
                )
            }

            // 5. قسم التعليقات (Comments Section - محفوظ بالكامل)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "التعليقات (${comments.size}) 💬",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("أضف تعليقاً على هذا الفيديو...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        FilledIconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    IslamicDataRepository.addComment(currentVideo.id, commentText.trim())
                                    commentText = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "إرسال")
                        }
                    }
                }
            }

            // Comments List
            items(comments) { comment ->
                val isCommenterVerified = remember(comment.authorHandle) {
                    IslamicDataRepository.isAuthorVerified(comment.authorHandle)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AsyncImage(
                        model = comment.authorAvatarUrl,
                        contentDescription = comment.authorName,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onNavigateToChannel(comment.authorHandle) },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { onNavigateToChannel(comment.authorHandle) }
                        ) {
                            Text(
                                text = comment.authorName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (isCommenterVerified) {
                                VerifiedBadge(size = 13.dp)
                            }
                        }
                        Text(
                            text = comment.text,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for More Options
    if (showMoreOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreOptionsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "خيارات إضافية ⚙️",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                ListItem(
                    headlineContent = { Text("حفظ في قائمة تشغيل جديدة") },
                    leadingContent = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "تم الحفظ في قائمة التشغيل 📋", Toast.LENGTH_SHORT).show()
                        showMoreOptionsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text("تنزيل للمشاهدة بدون إنترنت") },
                    leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "بدأ تنزيل الفيديو بدون إنترنت 📥", Toast.LENGTH_SHORT).show()
                        showMoreOptionsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text("نسخ رابط الفيديو") },
                    leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "تم نسخ الرابط إلى الحافظة 📋", Toast.LENGTH_SHORT).show()
                        showMoreOptionsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text("الإبلاغ عن محتوى غير لائق") },
                    leadingContent = { Icon(Icons.Default.Report, contentDescription = null, tint = Color.Red) },
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "شكراً لإبلاغك، سيتم فحص المحتوى فوراً", Toast.LENGTH_SHORT).show()
                        showMoreOptionsSheet = false
                    }
                )
            }
        }
    }

    // Dialog for Quality Selection
    if (showQualityDialog) {
        val qualityOptions = listOf(
            "1080p FHD (الأعلى)",
            "720p HD",
            "480p SD",
            "تلقائي (حسب سرعة الاتصال)"
        )
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = {
                Text(
                    text = "اختيار جودة تشغيل الفيديو ⚙️",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "يتم تقديم المحتوى بأعلى دقة متوفرة (${currentVideo.quality}) لضمان أفضل تجربة مشاهدة نقية بدون تشويش.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    qualityOptions.forEach { opt ->
                        val isSelected = selectedQuality == opt
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedQuality = opt
                                    Toast.makeText(context, "تم ضبط جودة المشاهدة: $opt", Toast.LENGTH_SHORT).show()
                                    showQualityDialog = false
                                },
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("إغلاق", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Dialog for Playback Speed Selection
    if (showSpeedDialog) {
        val speedOptions = listOf(
            0.5f to "0.5x (بطيء جداً)",
            0.75f to "0.75x (بطيء)",
            1.0f to "1.0x (سرعة عادية)",
            1.25f to "1.25x (سريع)",
            1.5f to "1.5x (أسرع)",
            2.0f to "2.0x (أقصى سرعة)"
        )
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = {
                Text(
                    text = "سرعة تشغيل الفيديو ⚡",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    speedOptions.forEach { (speed, label) ->
                        val isSelected = playbackSpeed == speed
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    playbackSpeed = speed
                                    mediaPlayerRef?.let { mp ->
                                        try {
                                            mp.playbackParams = mp.playbackParams.setSpeed(speed)
                                        } catch (_: Exception) {}
                                    }
                                    Toast.makeText(context, "تم ضبط السرعة: $label", Toast.LENGTH_SHORT).show()
                                    showSpeedDialog = false
                                },
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("إغلاق", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
