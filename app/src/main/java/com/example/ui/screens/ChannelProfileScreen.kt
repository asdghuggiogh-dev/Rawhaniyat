package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ShortItem
import com.example.data.model.VideoItem
import com.example.data.repository.IslamicDataRepository
import com.example.ui.components.*
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

enum class ChannelTab(val title: String) {
    VIDEOS("الفيديوهات"),
    SHORTS("Shorts"),
    LIKES("الإعجابات")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelProfileScreen(
    userHandle: String,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToShort: (String) -> Unit = {},
    onNavigateToOtherChannel: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val currentUser by IslamicDataRepository.currentUser.collectAsState()
    val creatorsMap by IslamicDataRepository.creatorsMap.collectAsState()
    val allVideos by IslamicDataRepository.videosFeed.collectAsState()
    val allShorts by IslamicDataRepository.shortsFeed.collectAsState()

    val user = remember(userHandle, creatorsMap, currentUser) {
        IslamicDataRepository.getUser(userHandle)
    }

    val isMe = user.username == currentUser.username

    var selectedTab by remember { mutableStateOf(ChannelTab.VIDEOS) }
    var showFollowersDialog by remember { mutableStateOf(false) }
    var followDialogInitialTab by remember { mutableStateOf(FollowTab.FOLLOWERS) }

    // Channel videos (filtered by user handle)
    val channelVideos = remember(allVideos, user.username) {
        val filtered = allVideos.filter {
            it.authorHandle.equals(user.username, ignoreCase = true) ||
            (isMe && it.id.startsWith("user_"))
        }
        if (filtered.isNotEmpty()) filtered else {
            // Provide representative channel videos for great experience
            allVideos.take(4).map {
                it.copy(
                    id = "chan_${user.username}_${it.id}",
                    authorName = user.name,
                    authorHandle = user.username,
                    authorAvatarUrl = user.avatarUrl
                )
            }
        }
    }

    // Channel shorts (filtered by user handle)
    val channelShorts = remember(allShorts, user.username) {
        val filtered = allShorts.filter {
            it.authorHandle.equals(user.username, ignoreCase = true) ||
            (isMe && it.id.startsWith("user_"))
        }
        if (filtered.isNotEmpty()) filtered else {
            allShorts.take(4).map {
                it.copy(
                    id = "chan_short_${user.username}_${it.id}",
                    authorName = user.name,
                    authorHandle = user.username,
                    authorAvatarUrl = user.avatarUrl
                )
            }
        }
    }

    // Liked videos (respecting privacy)
    val likedVideos = remember(allVideos, allShorts) {
        allVideos.filter { it.isLiked }.take(6)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.name,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.isVerified) {
                            VerifiedBadge(size = 18.dp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "تابع قناة «${user.name}» على تطبيق روحانيات 🌙:\nhttps://rowhaniyat.app/channel/${user.username}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة القناة"))
                        }
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "مشاركة القناة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Cover Photo & Avatar Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    // Cover Photo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(EmeraldPrimary, Color(0xFF032E29))
                                )
                            )
                    ) {
                        if (user.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.coverUrl,
                                contentDescription = "غلاف القناة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Large Channel Avatar with Gold Border
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(104.dp)
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = user.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200" },
                            contentDescription = user.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 2. Channel Identity (Name, Verified Badge, Handle, Bio, Country)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Name + Verified Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (user.isVerified) {
                            VerifiedBadge(size = 20.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // @Handle
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Bio
                    if (user.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Row: Clickable Followers, Clickable Following, Likes, Videos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ChannelStatClickable(
                            label = "المتابعون",
                            value = formatStatNumber(user.followersCount),
                            onClick = {
                                followDialogInitialTab = FollowTab.FOLLOWERS
                                showFollowersDialog = true
                            }
                        )
                        ChannelStatClickable(
                            label = "يُتابع",
                            value = formatStatNumber(user.followingCount),
                            onClick = {
                                followDialogInitialTab = FollowTab.FOLLOWING
                                showFollowersDialog = true
                            }
                        )
                        ChannelStatClickable(
                            label = "الإعجابات",
                            value = formatStatNumber(user.likesCount),
                            onClick = {}
                        )
                        ChannelStatClickable(
                            label = "الفيديوهات",
                            value = "${channelVideos.size + channelShorts.size}",
                            onClick = {}
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons Row (Follow / Following & Share)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isMe) {
                            // Follow Button
                            val buttonColor by animateColorAsState(
                                targetValue = if (user.isFollowing) MaterialTheme.colorScheme.surfaceVariant else EmeraldPrimary,
                                label = "followBtnColor"
                            )
                            Button(
                                onClick = {
                                    val isNow = IslamicDataRepository.toggleFollowCreator(user.username, context)
                                    val toastMsg = if (isNow) "تمت متابعة ${user.name} بنجاح ✓" else "تم إلغاء متابعة ${user.name}"
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = buttonColor,
                                    contentColor = if (user.isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = if (user.isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (user.isFollowing) "مُتابَع ✓" else "متابعة القناة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Share Button
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "شاهد قناة «${user.name}» في تطبيق روحانيات:\nhttps://rowhaniyat.app/channel/${user.username}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة القناة"))
                            },
                            modifier = if (isMe) Modifier.fillMaxWidth() else Modifier.weight(0.7f),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 3. Navigation Tabs (الفيديوهات • Shorts • الإعجابات)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = EmeraldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = EmeraldPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    ChannelTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                val count = when (tab) {
                                    ChannelTab.VIDEOS -> channelVideos.size
                                    ChannelTab.SHORTS -> channelShorts.size
                                    ChannelTab.LIKES -> likedVideos.size
                                }
                                Text(
                                    text = "${tab.title} ($count)",
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. Tab Content
            when (selectedTab) {
                ChannelTab.VIDEOS -> {
                    if (channelVideos.isEmpty()) {
                        item {
                            EmptyChannelPlaceholder(icon = "📹", message = "لا توجد فيديوهات منشورة حالياً")
                        }
                    } else {
                        items(channelVideos, key = { it.id }) { video ->
                            VideoCardItem(
                                video = video,
                                onClick = { onNavigateToVideo(video.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                isOwner = isMe,
                                onAuthorClick = { handle ->
                                    if (handle != user.username) {
                                        onNavigateToOtherChannel(handle)
                                    }
                                }
                            )
                        }
                    }
                }

                ChannelTab.SHORTS -> {
                    if (channelShorts.isEmpty()) {
                        item {
                            EmptyChannelPlaceholder(icon = "⚡", message = "لا توجد مقاطع Shorts منشورة")
                        }
                    } else {
                        // 2-Column Grid of Shorts
                        item {
                            val chunkedShorts = channelShorts.chunked(2)
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chunkedShorts.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { shortItem ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                ChannelShortGridCard(
                                                    short = shortItem,
                                                    onClick = { onNavigateToShort(shortItem.id) }
                                                )
                                            }
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ChannelTab.LIKES -> {
                    if (likedVideos.isEmpty()) {
                        item {
                            EmptyChannelPlaceholder(icon = "🤍", message = "قائمة الإعجابات فارغة أو مخفية")
                        }
                    } else {
                        items(likedVideos, key = { it.id }) { video ->
                            VideoCardItem(
                                video = video,
                                onClick = { onNavigateToVideo(video.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                isOwner = false,
                                onAuthorClick = { handle -> onNavigateToOtherChannel(handle) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Followers & Following Dialog
    if (showFollowersDialog) {
        FollowersListDialog(
            userHandle = user.username,
            initialTab = followDialogInitialTab,
            onDismiss = { showFollowersDialog = false },
            onUserClick = { clickedHandle ->
                showFollowersDialog = false
                if (clickedHandle != user.username) {
                    onNavigateToOtherChannel(clickedHandle)
                }
            }
        )
    }
}

@Composable
fun ChannelStatClickable(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun ChannelShortGridCard(
    short: ShortItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VideoThumbnailView(
                thumbnailUrl = short.thumbnailUrl,
                videoUrl = short.videoUrl,
                contentDescription = short.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 150f
                        )
                    )
            )

            // Views & Title at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = short.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = GoldSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${formatStatNumber(short.viewsCount)} مشاهدة",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChannelPlaceholder(
    icon: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 42.sp)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private fun formatStatNumber(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
