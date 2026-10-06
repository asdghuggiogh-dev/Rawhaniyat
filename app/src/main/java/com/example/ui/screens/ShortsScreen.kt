package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.Comment
import com.example.data.model.NotificationType
import com.example.data.model.ShortItem
import com.example.data.repository.IslamicDataRepository
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.VideoThumbnailView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.SoftEmerald
import com.example.util.RowhaniyatNotificationManager
import com.example.util.VideoUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    onNavigateToChannel: (String) -> Unit = {}
) {
    val shorts by IslamicDataRepository.shortsFeed.collectAsState()
    val pagerState = rememberPagerState(pageCount = { shorts.size })
    var activeCommentsShortId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val shortItem = shorts[page]
            val isCurrentPage = pagerState.currentPage == page

            ShortItemPage(
                shortItem = shortItem,
                isCurrentPage = isCurrentPage,
                onOpenComments = { activeCommentsShortId = shortItem.id },
                onNavigateToChannel = onNavigateToChannel
            )
        }

        // Top Header overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "شورتز روحانيات 🌙",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${shorts.size}",
                    color = GoldSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Comments Bottom Sheet
        activeCommentsShortId?.let { contentId ->
            ShortCommentsBottomSheet(
                contentId = contentId,
                onDismiss = { activeCommentsShortId = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortItemPage(
    shortItem: ShortItem,
    isCurrentPage: Boolean,
    onOpenComments: () -> Unit,
    onNavigateToChannel: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isGuest by IslamicDataRepository.isGuestMode.collectAsState()
    val currentUser by IslamicDataRepository.currentUser.collectAsState()

    var isPlaying by remember(isCurrentPage) { mutableStateOf(isCurrentPage) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var likesCount by remember(shortItem.id) { mutableLongStateOf(shortItem.likesCount) }
    var isLiked by remember(shortItem.id) { mutableStateOf(shortItem.isLiked) }
    var isSaved by remember(shortItem.id) { mutableStateOf(shortItem.isSaved) }
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    var showEditShortDialog by remember { mutableStateOf(false) }

    // Spring animation for Like button
    val heartScale by animateFloatAsState(
        targetValue = if (isLiked) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "heartScale"
    )

    val creatorsMap by IslamicDataRepository.creatorsMap.collectAsState()
    val authorProfile = remember(shortItem.authorHandle, creatorsMap) {
        IslamicDataRepository.getUser(shortItem.authorHandle)
    }
    val isAuthorVerified = authorProfile.isVerified
    val isFollowing = authorProfile.isFollowing

    // Background view counter tracking
    LaunchedEffect(shortItem.id) {
        IslamicDataRepository.recordVideoView(shortItem.id)
    }

    // Playback progress loop
    LaunchedEffect(isPlaying, isCurrentPage) {
        if (isPlaying && isCurrentPage) {
            val totalSeconds = shortItem.durationSeconds.coerceAtLeast(15)
            val stepMs = 100L
            val stepFraction = stepMs.toFloat() / (totalSeconds * 1000f)
            while (isPlaying && isCurrentPage) {
                delay(stepMs)
                playbackProgress = (playbackProgress + stepFraction)
                if (playbackProgress >= 1f) {
                    playbackProgress = 0f
                }
            }
        } else {
            playbackProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(shortItem.id) {
                detectTapGestures(
                    onTap = {
                        isPlaying = !isPlaying
                    },
                    onDoubleTap = {
                        showDoubleTapHeart = true
                        if (!isLiked) {
                            isLiked = true
                            likesCount++
                            IslamicDataRepository.toggleShortLike(shortItem.id)
                            if (shortItem.authorHandle != currentUser.username) {
                                RowhaniyatNotificationManager.notify(
                                    context = context,
                                    title = "إعجاب جديد ❤️",
                                    body = "أعجب ${currentUser.name} بمقطع Short: ${shortItem.title}",
                                    type = NotificationType.LIKE,
                                    targetId = shortItem.id,
                                    targetType = "short"
                                )
                            }
                        }
                    }
                )
            }
    ) {
        // Video View or High-res Thumbnail
        val isLocalUri = shortItem.videoUrl.startsWith("content://") || shortItem.videoUrl.startsWith("file://")
        val isRemoteVideo = shortItem.videoUrl.endsWith(".mp4") || shortItem.videoUrl.contains("gtv-videos-bucket")

        if (isLocalUri || isRemoteVideo) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val videoModifier = if (shortItem.isHorizontal) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                } else {
                    Modifier.fillMaxSize()
                }

                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            VideoUtils.bindVideoView(
                                videoView = this,
                                videoUrl = shortItem.videoUrl,
                                onPrepared = { mp ->
                                    mp.isLooping = true
                                    if (isCurrentPage && isPlaying) {
                                        start()
                                    }
                                }
                            )
                        }
                    },
                    update = { view ->
                        if (isCurrentPage && isPlaying) {
                            if (!view.isPlaying) view.start()
                        } else {
                            if (view.isPlaying) view.pause()
                        }
                    },
                    modifier = videoModifier
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                VideoThumbnailView(
                    thumbnailUrl = shortItem.thumbnailUrl,
                    videoUrl = shortItem.videoUrl,
                    contentDescription = shortItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top and bottom gradients for maximum readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        // Center Pause Indicator on Tap
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        // 7. Side Action Buttons (❤️ إعجاب | 💬 تعليقات | 🔖 حفظ | ↗️ مشاركة)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Like Button ❤️
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        if (isGuest) {
                            Toast.makeText(context, "يرجى تسجيل الدخول أو إنشاء حساب للإعجاب بالمحتوى 🌙", Toast.LENGTH_SHORT).show()
                        } else {
                            isLiked = !isLiked
                            if (isLiked) {
                                likesCount++
                                RowhaniyatNotificationManager.notify(
                                    context = context,
                                    title = "إعجاب جديد ❤️",
                                    body = "أعجب شخص بمقطع Short: ${shortItem.title}",
                                    type = NotificationType.LIKE,
                                    targetId = shortItem.id,
                                    targetType = "short"
                                )
                            } else {
                                likesCount = (likesCount - 1).coerceAtLeast(0)
                            }
                            IslamicDataRepository.toggleShortLike(shortItem.id)
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .scale(heartScale)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "إعجاب",
                        tint = if (isLiked) Color(0xFFE53935) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$likesCount",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Comments Button 💬
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onOpenComments,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ModeComment,
                        contentDescription = "تعليقات",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${shortItem.commentsCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3. Save / Bookmark Button 🔖
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        isSaved = !isSaved
                        IslamicDataRepository.toggleShortSave(shortItem.id)
                        Toast.makeText(
                            context,
                            if (isSaved) "تم حفظ الـShort في المحفوظات 🔖" else "تمت الإزالة من المحفوظات",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "حفظ",
                        tint = if (isSaved) GoldSecondary else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "حفظ",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 4. Share & Options Button ↗️
            var showShareMenuSheet by remember { mutableStateOf(false) }
            var showDeleteConfirmDialog by remember { mutableStateOf(false) }
            val isOwner = remember(shortItem.authorHandle, currentUser.username) {
                shortItem.authorHandle == currentUser.username || currentUser.username == "admin"
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { showShareMenuSheet = true },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "مشاركة",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "مشاركة",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Share & Management Bottom Sheet
            if (showShareMenuSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showShareMenuSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "خيارات المقطع 🌙",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // 1. Share via System
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showShareMenuSheet = false
                                    val shortUrl = "https://rowhaniyat.app/short/${shortItem.id}"
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "شاهد هذا المقطع الإيماني: ${shortItem.title}\n$shortUrl\nعبر تطبيق روحانيات 🌙"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الـ Short ↗️"))
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, tint = SoftEmerald)
                            Text("مشاركة المقطع عبر التطبيقات ↗️", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        // 2. Copy Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showShareMenuSheet = false
                                    val shortUrl = "https://rowhaniyat.app/short/${shortItem.id}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Short Link", shortUrl)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ رابط المقطع بنجاح 📋", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = GoldSecondary)
                            Text("نسخ رابط الفيديو 📋", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        // 3. Delete Short (Strictly Owner Only)
                        if (isOwner) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showShareMenuSheet = false
                                        showDeleteConfirmDialog = true
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFE53935))
                                Text("حذف الفيديو نهائيًا 🗑️", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE53935))
                            }
                        }
                    }
                }
            }

            // Confirm Delete Dialog
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = {
                        Text(
                            text = "تأكيد حذف الفيديو",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Text("هل تريد حذف هذا الفيديو نهائيًا؟ لن يتمكن أحد من مشاهدته وسيتم حذفه من قاعدة البيانات السحابية.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = false
                                IslamicDataRepository.deleteVideoOrShort(shortItem.id) { success ->
                                    val msg = if (success) "تم حذف الفيديو بنجاح من السيرفر والتطبيق ✓" else "تم حذف الفيديو بنجاح ✓"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Text("حذف", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }

        // 6. Publisher Information Area (Avatar + Name + Verified, Category/Title, Hashtags, Audio)
        // Clean layout with NO views counter next to the username
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 26.dp)
                .fillMaxWidth()
        ) {
            // Row 1: Publisher Avatar + Name + Verified Badge + Follow Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Author Avatar
                AsyncImage(
                    model = shortItem.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200" },
                    contentDescription = shortItem.authorName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, SoftEmerald, CircleShape)
                        .clickable { onNavigateToChannel(shortItem.authorHandle) },
                    contentScale = ContentScale.Crop
                )

                // Author Name + Verified
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onNavigateToChannel(shortItem.authorHandle) }
                ) {
                    Text(
                        text = shortItem.authorName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (isAuthorVerified) {
                        VerifiedBadge(size = 14.dp)
                    }
                }

                // Follow Button
                Surface(
                    onClick = {
                        if (isGuest) {
                            Toast.makeText(context, "يرجى تسجيل الدخول أو إنشاء حساب للمتابعة 🌙", Toast.LENGTH_SHORT).show()
                        } else {
                            val isNow = IslamicDataRepository.toggleFollowCreator(shortItem.authorHandle, context)
                            val msg = if (isNow) "تمت متابعة ${authorProfile.name} ✓" else "تم إلغاء المتابعة"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (isNow) {
                                RowhaniyatNotificationManager.notify(
                                    context = context,
                                    title = "متابع جديد 👤",
                                    body = "بدأ ${currentUser.name} بمتابعة حساب ${authorProfile.name}",
                                    type = NotificationType.NEW_FOLLOWER,
                                    targetId = shortItem.authorHandle,
                                    targetType = "channel"
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isFollowing) Color.White.copy(alpha = 0.22f) else SoftEmerald
                ) {
                    Text(
                        text = if (isFollowing) "مُتابع" else "متابعة",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Short Category / Title (e.g. قرآن)
            Text(
                text = shortItem.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Row 3: Hashtags (#روحانيات #قرآن)
            val displayHashtags = if (shortItem.hashtags.isNotEmpty()) {
                shortItem.hashtags.joinToString(" ")
            } else {
                "#روحانيات #قرآن #خواطر_إيمانية"
            }
            Text(
                text = displayHashtags,
                color = GoldSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Row 4: Audio Track / Sound Source
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${shortItem.soundTitle} - ${shortItem.soundAuthor}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Live Playback Progress Bar at the very bottom
        LinearProgressIndicator(
            progress = { playbackProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter),
            color = SoftEmerald,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}

/**
 * 2. Professional Comments Bottom Sheet with immediate posting and deletion of own comments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortCommentsBottomSheet(
    contentId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val commentsMap by IslamicDataRepository.comments.collectAsState()
    val comments = commentsMap[contentId] ?: emptyList()
    val currentUser by IslamicDataRepository.currentUser.collectAsState()
    val isGuest by IslamicDataRepository.isGuestMode.collectAsState()

    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التعليقات (${comments.size}) 💬",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Comments List
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💬✨", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد تعليقات بعد",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "كن أول من يعلق بذكر الله أو كلمة طيبة ✨",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        val isMyComment = comment.authorHandle == currentUser.username

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = comment.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" },
                                contentDescription = comment.authorName,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = comment.authorName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = comment.timeAgo,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Delete Own Comment Button
                                    if (isMyComment) {
                                        IconButton(
                                            onClick = {
                                                IslamicDataRepository.deleteComment(contentId, comment.id)
                                                Toast.makeText(context, "تم حذف تعليقك ✓", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "حذف تعليقي",
                                                tint = Color(0xFFE53935).copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = comment.text,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add Comment Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = currentUser.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" },
                    contentDescription = currentUser.name,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("اكتب تعليقاً طيباً...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    singleLine = true
                )

                FilledIconButton(
                    onClick = {
                        if (isGuest) {
                            Toast.makeText(context, "يرجى تسجيل الدخول أو إنشاء حساب لكتابة تعليق 🌙", Toast.LENGTH_SHORT).show()
                        } else if (commentText.isNotBlank()) {
                            val textToSend = commentText.trim()
                            IslamicDataRepository.addComment(contentId, textToSend)
                            RowhaniyatNotificationManager.notify(
                                context = context,
                                title = "تعليق جديد 💬",
                                body = "${currentUser.name}: $textToSend",
                                type = NotificationType.COMMENT,
                                targetId = contentId,
                                targetType = "short"
                            )
                            commentText = ""
                            Toast.makeText(context, "تم نشر تعليقك بنجاح ✨", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = SoftEmerald),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "نشر التعليق",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
