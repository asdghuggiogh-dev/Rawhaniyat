package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AudioPlayerManager
import com.example.data.model.*
import com.example.data.repository.IslamicDataRepository
import com.example.ui.components.StoriesRow
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.VideoCardItem
import com.example.ui.components.VideoThumbnailView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToShorts: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToAzkar: () -> Unit,
    onNavigateToTasbeeh: () -> Unit,
    onNavigateToRecitations: () -> Unit = {},
    onNavigateToCreate: (ContentType) -> Unit = {},
    onNavigateToChannel: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by IslamicDataRepository.currentUser.collectAsState()
    val isGuest by IslamicDataRepository.isGuestMode.collectAsState()
    val audioRecitations by IslamicDataRepository.audioRecitations.collectAsState()
    val videos by IslamicDataRepository.videosFeed.collectAsState()
    val featuredVideos by IslamicDataRepository.featuredVideos.collectAsState()
    val shorts by IslamicDataRepository.shortsFeed.collectAsState()
    val stories by IslamicDataRepository.storiesFeed.collectAsState()
    val lastBookmark by IslamicDataRepository.lastReadBookmark.collectAsState()
    val currentTrack by AudioPlayerManager.currentTrack.collectAsState()
    val isAudioPlaying by AudioPlayerManager.isPlaying.collectAsState()

    var viewingStoryIndex by remember { mutableStateOf<Int?>(null) }
    var showAllFeaturedSheet by remember { mutableStateOf(false) }
    var selectedVideoCategory by remember { mutableStateOf("الكل") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Stories Section
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StoriesRow(
                        stories = stories,
                        onAddStoryClick = { onNavigateToCreate(ContentType.STORY) },
                        onStoryClick = { story ->
                            IslamicDataRepository.markStoryViewed(story.id)
                            val idx = stories.indexOf(story)
                            viewingStoryIndex = if (idx >= 0) idx else 0
                        }
                    )
                }
            }
        // Welcome Card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    tonalElevation = 2.dp
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGuest) "مرحباً بك يا ضيف روحانيات 🌙" else "السلام عليكم ورحمة الله، ${currentUser.name} 🌙",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اللهم اجعل القرآن ربيع قلوبنا ونور صدورنا وجلاء أحزاننا",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Quick Tasbeeh Mini Action
                    FilledTonalButton(
                        onClick = onNavigateToTasbeeh,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "المسبحة 📿", fontSize = 12.sp)
                    }
                }
            }
            }
        }

        // Daily Quran Ayah (آية قرآنية يومية)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                val dailyAyah = IslamicDataRepository.dailyAyah
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "✨", fontSize = 18.sp)
                            Text(
                                text = "آية اليوم المباركة",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoldSecondary
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "سورة البقرة: ٢٨٦",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "﴿ ${dailyAyah.textArabic} ﴾",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = dailyAyah.tafseer,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val recitation = audioRecitations.firstOrNull { it.surahNumber == 1 } ?: audioRecitations.first()
                                AudioPlayerManager.playRecitation(context, recitation)
                                Toast.makeText(context, "جاري تشغيل التلاوة الصوتية...", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "استماع", tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "استماع", color = EmeraldPrimary)
                        }

                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Ayah", "${dailyAyah.textArabic} - [سورة البقرة: 286]")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ الآية الكريمة", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ", tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "نسخ", color = MaterialTheme.colorScheme.onSurface)
                        }

                        TextButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "﴿ ${dailyAyah.textArabic} ﴾\nمن تطبيق روحانيات 🌙")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة الآية")
                                context.startActivity(shareIntent)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "مشاركة", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            }
        }

        // Daily Azkar & Last Played / Last Read Bookmark
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Azkar Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToAzkar() },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "☀️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أذكار الصباح والمساء",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "حصن المسلم اليومي وموجبات السكينة",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "فتح الأذكار ←",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Last Read Bookmark Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToSurah(2) },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔖", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "آخر موضع قراءة",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lastBookmark,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "متابعة التلاوة ←",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GoldSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            }
        }

        // Featured Quran Audio Recitations
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎙️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "التلاوات القرآنية المختارة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    TextButton(onClick = onNavigateToRecitations) {
                        Text(text = "عرض الكل", color = EmeraldPrimary)
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(audioRecitations) { recitation ->
                        Surface(
                            modifier = Modifier
                                .width(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    AudioPlayerManager.playRecitation(context, recitation)
                                },
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(EmeraldPrimary, Color(0xFF023624))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "📖", fontSize = 32.sp)
                                        Text(
                                            text = recitation.surahName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    // Play overlay button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(GoldSecondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val isThisPlaying = currentTrack?.id == recitation.id && isAudioPlaying
                                        Icon(
                                            imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "تشغيل",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = recitation.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = recitation.reciterName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "المدة: ${recitation.durationFormatted}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = GoldSecondary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Renowned Reciters (القراء)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👥", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "أشهر القراء في العالم الإسلامي",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(IslamicDataRepository.recitersList) { reciter ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(90.dp)
                                .clickable {
                                    val matched = audioRecitations.firstOrNull { it.reciterId == reciter.id }
                                    if (matched != null) {
                                        AudioPlayerManager.playRecitation(context, matched)
                                        Toast.makeText(context, "تلاوة بصوت ${reciter.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(EmeraldPrimary, Color(0xFF033825))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = reciter.name.take(1),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = reciter.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = reciter.style,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
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

        // Shorts Preview Row (فيديوهات قصيرة)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "أحدث مقاطع Shorts الروحانية",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    TextButton(onClick = onNavigateToShorts) {
                        Text(text = "عرض الكل", color = EmeraldPrimary)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shorts) { shortItem ->
                        Surface(
                            modifier = Modifier
                                .width(140.dp)
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigateToShorts() },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Real Thumbnail image with fallback
                                VideoThumbnailView(
                                    thumbnailUrl = shortItem.thumbnailUrl,
                                    videoUrl = shortItem.videoUrl,
                                    contentDescription = shortItem.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Dark gradient overlay at bottom for text contrast
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                                            )
                                        )
                                )

                                // Play icon indicator
                                Icon(
                                    imageVector = Icons.Default.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(36.dp)
                                )

                                // Details at bottom
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = shortItem.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "❤️ ${shortItem.likesCount}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = GoldSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Filter Chips & Long Videos Sections
        item {
            val categories = listOf("الكل", "مميزة ✨", "أحدث الفيديوهات", "تلاوات قرآنية", "محتوى روحاني", "قد يعجبك")

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Category Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedVideoCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedVideoCategory = cat },
                            label = {
                                Text(
                                    text = cat,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                // If "الكل" or "مميزة ✨" is selected, show Featured Section
                if (selectedVideoCategory == "الكل" || selectedVideoCategory == "مميزة ✨") {
                    val displayFeatured = if (featuredVideos.isNotEmpty()) featuredVideos else videos.sortedByDescending { it.viewsCount }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "فيديوهات مميزة ✨", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            TextButton(onClick = { showAllFeaturedSheet = true }) {
                                Text(text = "عرض الكل", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (displayFeatured.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(displayFeatured) { video ->
                                    Box(modifier = Modifier.width(300.dp)) {
                                        VideoCardItem(
                                            video = video,
                                            onClick = { onNavigateToVideo(video.id) },
                                            isOwner = video.authorHandle == currentUser.username,
                                            onAuthorClick = { handle -> onNavigateToChannel(handle) }
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "🎬✨", fontSize = 30.sp)
                                    Text(
                                        text = "لا توجد فيديوهات طويلة مميزة حالياً",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Button(
                                        onClick = { onNavigateToCreate(ContentType.LONG_VIDEO) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Text("انشر فيديو الآن 🎥")
                                    }
                                }
                            }
                        }
                    }
                }

                // If "الكل" or "أحدث الفيديوهات" is selected:
                if (selectedVideoCategory == "الكل" || selectedVideoCategory == "أحدث الفيديوهات") {
                    val latestVideos = videos.sortedByDescending { it.id }
                    val displayList = if (selectedVideoCategory == "أحدث الفيديوهات") latestVideos else latestVideos.take(3)

                    if (displayList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🕒", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "أحدث الفيديوهات",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            displayList.forEach { video ->
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

                // If "الكل" or "تلاوات قرآنية" is selected:
                if (selectedVideoCategory == "الكل" || selectedVideoCategory == "تلاوات قرآنية") {
                    val quranVideos = videos.filter {
                        it.title.contains("قرآن") || it.title.contains("تلاو") || it.title.contains("سورة") ||
                        it.hashtags.any { h -> h.contains("قرآن") || h.contains("تلاو") }
                    }.ifEmpty { videos.take(3) }
                    val displayList = if (selectedVideoCategory == "تلاوات قرآنية") quranVideos else quranVideos.take(3)

                    if (displayList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "📖", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تلاوات قرآنية",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            displayList.forEach { video ->
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

                // If "الكل" or "محتوى روحاني" is selected:
                if (selectedVideoCategory == "الكل" || selectedVideoCategory == "محتوى روحاني") {
                    val spiritualVideos = videos.filter {
                        it.title.contains("موعظ") || it.title.contains("درس") || it.title.contains("قلب") ||
                        it.title.contains("الشيخ") || it.title.contains("إيمان") || it.title.contains("رمضان") ||
                        it.hashtags.any { h -> h.contains("موعظ") || h.contains("روحان") }
                    }.ifEmpty { videos.reversed() }
                    val displayList = if (selectedVideoCategory == "محتوى روحاني") spiritualVideos else spiritualVideos.take(3)

                    if (displayList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🌙", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "محتوى روحاني",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            displayList.forEach { video ->
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

                // If "الكل" or "قد يعجبك" is selected:
                if (selectedVideoCategory == "الكل" || selectedVideoCategory == "قد يعجبك") {
                    val recommendedVideos = videos.filter { it.isLiked || it.isSaved || it.viewsCount > 1000 }.ifEmpty { videos }
                    val displayList = if (selectedVideoCategory == "قد يعجبك") recommendedVideos else recommendedVideos.take(3)

                    if (displayList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "💡", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "قد يعجبك",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            displayList.forEach { video ->
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
            }
        }
    }

    // Story Viewer Dialog
    viewingStoryIndex?.let { idx ->
        StoryViewerDialog(
            stories = stories,
            initialStoryIndex = idx,
            onDismiss = { viewingStoryIndex = null },
            onDeleteStory = { storyId ->
                IslamicDataRepository.deleteStory(storyId)
            }
        )
    }

    // All Featured Videos Bottom Sheet
    if (showAllFeaturedSheet) {
        val displayFeatured = if (featuredVideos.isNotEmpty()) featuredVideos else videos.filter { !it.isVertical }
        ModalBottomSheet(
            onDismissRequest = { showAllFeaturedSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "فيديوهات طويلة مميزة ✨",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "المقاطع الأكثر مشاهدة وتفاعلاً في روحانيات",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = { showAllFeaturedSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayFeatured) { video ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAllFeaturedSheet = false
                                    onNavigateToVideo(video.id)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black)
                                ) {
                                    VideoThumbnailView(
                                        thumbnailUrl = video.thumbnailUrl,
                                        videoUrl = video.videoUrl,
                                        contentDescription = video.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Black.copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = video.durationFormatted,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${video.authorName} • ${video.viewsCount} مشاهدة",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = "${video.quality} ✨ • ${video.timeAgo}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
