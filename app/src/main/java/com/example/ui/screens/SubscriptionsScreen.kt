package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.repository.IslamicDataRepository
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.VideoCardItem
import com.example.ui.components.VideoThumbnailView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onNavigateToVideo: (String) -> Unit,
    onNavigateToShort: (String) -> Unit,
    onNavigateToChannel: (String) -> Unit,
    onNavigateToSurah: (Int) -> Unit
) {
    val context = LocalContext.current
    val creatorsMap by IslamicDataRepository.creatorsMap.collectAsState()
    val allVideos by IslamicDataRepository.videosFeed.collectAsState()
    val allShorts by IslamicDataRepository.shortsFeed.collectAsState()
    val currentUser by IslamicDataRepository.currentUser.collectAsState()

    var selectedFilter by remember { mutableStateOf("الكل") }
    val filters = listOf("الكل", "اليوم", "فيديوهات", "Shorts", "تلاوات وقرآن")

    val followedCreators = remember(creatorsMap) {
        creatorsMap.values.filter { it.isFollowing }
    }

    val displayCreators = remember(creatorsMap, followedCreators) {
        if (followedCreators.isNotEmpty()) followedCreators else creatorsMap.values.take(6)
    }

    // Filter videos for subscribed channels
    val feedVideos = remember(allVideos, followedCreators, selectedFilter) {
        val followedHandles = followedCreators.map { it.username }.toSet()
        val candidateVideos = if (followedHandles.isNotEmpty()) {
            allVideos.filter { followedHandles.contains(it.authorHandle) }
        } else {
            allVideos
        }

        when (selectedFilter) {
            "اليوم" -> candidateVideos.filter { it.timeAgo.contains("الآن") || it.timeAgo.contains("ساعة") || it.timeAgo.contains("يوم") }
            "فيديوهات" -> candidateVideos.filter { !it.isVertical }
            "Shorts" -> emptyList()
            "تلاوات وقرآن" -> candidateVideos.filter { it.title.contains("قرآن") || it.title.contains("تلاو") || it.title.contains("سورة") }
            else -> candidateVideos
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Subscribed Channels Horizontal Bar (YouTube style)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (followedCreators.isNotEmpty()) "القنوات التي تتابعها 🔔" else "قنوات مقترحة للمتابعة ✨",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${displayCreators.size} قناة",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayCreators.toList(), key = { it.id }) { creator ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(68.dp)
                                .clickable { onNavigateToChannel(creator.username) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(
                                        width = 2.dp,
                                        color = if (creator.isFollowing) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .padding(3.dp)
                            ) {
                                AsyncImage(
                                    model = creator.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200" },
                                    contentDescription = creator.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = creator.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 2. Filter Chips Row
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(filters) { chip ->
                    val isSelected = selectedFilter == chip
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = chip },
                        label = { Text(chip, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // 3. Quick Quran Reader Banner (preserves Quran access)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateToSurah(1) },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.AutoStories, contentDescription = null, tint = EmeraldPrimary)
                        Column {
                            Text("القرآن الكريم كاملاً 📖", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("تصفح سور وآيات القرآن الكريم مع التفسير والتلاوة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("فتح ⬅️", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Shorts Row in Subscriptions (if Shorts filter or All)
        if (selectedFilter == "الكل" || selectedFilter == "Shorts") {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مقاطع Shorts للقنوات ⚡", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allShorts.take(6)) { short ->
                            Surface(
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onNavigateToShort(short.id) },
                                color = Color.Black
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    VideoThumbnailView(
                                        thumbnailUrl = short.thumbnailUrl,
                                        videoUrl = short.videoUrl,
                                        contentDescription = short.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f))
                                    )
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = short.title,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${short.viewsCount} مشاهدة",
                                            color = GoldSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)
                }
            }
        }

        // 5. Full-Width Videos Feed
        if (selectedFilter != "Shorts") {
            items(feedVideos, key = { it.id }) { video ->
                VideoCardItem(
                    video = video,
                    onClick = { onNavigateToVideo(video.id) },
                    isOwner = video.authorHandle == currentUser.username,
                    onAuthorClick = { handle -> onNavigateToChannel(handle) }
                )
            }
        }
    }
}
