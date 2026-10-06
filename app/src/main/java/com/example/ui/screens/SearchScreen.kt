package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AudioPlayerManager
import com.example.data.repository.IslamicDataRepository
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.VideoThumbnailView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

enum class SearchFilter(val title: String) {
    ALL("الكل"),
    QURAN("السور"),
    RECITATIONS("التلاوات"),
    VIDEOS("الفيديوهات"),
    SHORTS("Shorts"),
    USERS("المستخدمين")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToChannel: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val searchPrefs = remember { context.getSharedPreferences("rowhaniyat_search_prefs", Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }

    // Search History State
    var searchHistory by remember {
        val saved = searchPrefs.getStringSet("recent_queries", null) ?: emptySet()
        mutableStateOf(saved.toList())
    }

    fun addQueryToHistory(q: String) {
        if (q.isBlank()) return
        val updated = (listOf(q.trim()) + searchHistory.filter { it != q.trim() }).take(10)
        searchHistory = updated
        searchPrefs.edit().putStringSet("recent_queries", updated.toSet()).apply()
    }

    fun clearSearchHistory() {
        searchHistory = emptyList()
        searchPrefs.edit().remove("recent_queries").apply()
        Toast.makeText(context, "تم مسح سجل البحث بنجاح ✓", Toast.LENGTH_SHORT).show()
    }

    val popularSuggestions = listOf("سورة الكهف", "مشاري العفاسي", "أذكار الصباح", "سورة البقرة", "#طمأنينة", "دعاء تيسير الأمور")

    val surahs = IslamicDataRepository.surahsList
    val recitations by IslamicDataRepository.audioRecitations.collectAsState()
    val videos by IslamicDataRepository.videosFeed.collectAsState()
    val shorts by IslamicDataRepository.shortsFeed.collectAsState()
    val creatorsMap by IslamicDataRepository.creatorsMap.collectAsState()
    val currentUser by IslamicDataRepository.currentUser.collectAsState()

    // Filtered Results
    val cleanQuery = query.trim()
    val filteredCreators = remember(cleanQuery, creatorsMap) {
        if (cleanQuery.isBlank()) emptyList()
        else creatorsMap.values.filter {
            it.name.contains(cleanQuery, ignoreCase = true) ||
            it.username.contains(cleanQuery, ignoreCase = true) ||
            it.bio.contains(cleanQuery, ignoreCase = true)
        }
    }
    val filteredSurahs = remember(cleanQuery) {
        if (cleanQuery.isBlank()) emptyList()
        else surahs.filter { it.nameArabic.contains(cleanQuery, ignoreCase = true) || it.nameEnglish.contains(cleanQuery, ignoreCase = true) }
    }
    val filteredRecitations = remember(cleanQuery) {
        if (cleanQuery.isBlank()) emptyList()
        else recitations.filter { it.title.contains(cleanQuery, ignoreCase = true) || it.reciterName.contains(cleanQuery, ignoreCase = true) }
    }
    val filteredVideos = remember(cleanQuery) {
        if (cleanQuery.isBlank()) emptyList()
        else videos.filter { it.title.contains(cleanQuery, ignoreCase = true) || it.authorName.contains(cleanQuery, ignoreCase = true) || it.hashtags.any { h -> h.contains(cleanQuery) } }
    }
    val filteredShorts = remember(cleanQuery) {
        if (cleanQuery.isBlank()) emptyList()
        else shorts.filter { it.title.contains(cleanQuery, ignoreCase = true) || it.authorName.contains(cleanQuery, ignoreCase = true) || it.hashtags.any { h -> h.contains(cleanQuery) } }
    }
    val filteredUsers = remember(cleanQuery) {
        if (cleanQuery.isBlank()) emptyList()
        else {
            val list = mutableListOf(currentUser)
            list.filter { it.name.contains(cleanQuery, ignoreCase = true) || it.username.contains(cleanQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("ابحث في المصحف، التلاوات، المقاطع...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchFilter.values()) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.title, fontSize = 12.sp) }
                    )
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // If query is blank, show Recent Searches & Popular Suggestions
                if (query.isBlank()) {
                    // Recent Search History
                    if (searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سجل البحث الأخير 🕒",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                TextButton(onClick = { clearSearchHistory() }) {
                                    Text("مسح الكل", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        items(searchHistory) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { query = item }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.NorthWest, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                            }
                        }

                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                    }

                    // Popular Suggestions
                    item {
                        Text(
                            text = "عمليات بحث شائعة ✨",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            popularSuggestions.forEach { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        query = suggestion
                                        addQueryToHistory(suggestion)
                                    }
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Display Filtered Results
                    val hasResults = (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.QURAN) && filteredSurahs.isNotEmpty() ||
                            (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.VIDEOS) && filteredVideos.isNotEmpty() ||
                            (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.SHORTS) && filteredShorts.isNotEmpty() ||
                            (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.RECITATIONS) && filteredRecitations.isNotEmpty() ||
                            (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.USERS) && filteredUsers.isNotEmpty()

                    if (!hasResults) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔍", fontSize = 42.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لم يتم العثور على نتائج لـ «$cleanQuery»", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("جرب كلمات أخرى مثل اسم السورة أو القارئ", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                        }
                    }

                    // 1. Users Results
                    if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.USERS) && filteredUsers.isNotEmpty()) {
                        item {
                            Text("المستخدمين والقراء 👤", style = MaterialTheme.typography.labelLarge.copy(color = EmeraldPrimary, fontWeight = FontWeight.Bold))
                        }
                        items(filteredUsers) { user ->
                            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(model = user.avatarUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("@${user.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Surahs Results
                    if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.QURAN) && filteredSurahs.isNotEmpty()) {
                        item {
                            Text("سور القرآن الكريم 📖", style = MaterialTheme.typography.labelLarge.copy(color = EmeraldPrimary, fontWeight = FontWeight.Bold))
                        }
                        items(filteredSurahs) { surah ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        addQueryToHistory(cleanQuery)
                                        onNavigateToSurah(surah.id)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = CircleShape, color = EmeraldPrimary.copy(alpha = 0.1f), modifier = Modifier.size(34.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${surah.id}", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(surah.nameArabic, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("${surah.revelationType} • ${surah.versesCount} آية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // 2. Users / Creators Results
                    if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.USERS) && filteredCreators.isNotEmpty()) {
                        item {
                            Text("القنوات والمستخدمين 👤", style = MaterialTheme.typography.labelLarge.copy(color = EmeraldPrimary, fontWeight = FontWeight.Bold))
                        }
                        items(filteredCreators) { creator ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        addQueryToHistory(cleanQuery)
                                        onNavigateToChannel(creator.username)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = creator.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" },
                                        contentDescription = creator.name,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary.copy(alpha = 0.1f)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(creator.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (creator.isVerified) {
                                                VerifiedBadge(size = 14.dp)
                                            }
                                        }
                                        Text("@${creator.username} • ${creator.followersCount} متابع", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // 3. Videos Results
                    if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.VIDEOS) && filteredVideos.isNotEmpty()) {
                        item {
                            Text("الفيديوهات 📺", style = MaterialTheme.typography.labelLarge.copy(color = EmeraldPrimary, fontWeight = FontWeight.Bold))
                        }
                        items(filteredVideos) { video ->
                            val isVerified = remember(video.authorHandle) {
                                IslamicDataRepository.isAuthorVerified(video.authorHandle)
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        addQueryToHistory(cleanQuery)
                                        onNavigateToVideo(video.id)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(width = 90.dp, height = 55.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                                        VideoThumbnailView(
                                            thumbnailUrl = video.thumbnailUrl,
                                            videoUrl = video.videoUrl,
                                            contentDescription = video.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(video.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("${video.authorName} • ${video.durationFormatted}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (isVerified) {
                                                VerifiedBadge(size = 12.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Shorts Results
                    if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.SHORTS) && filteredShorts.isNotEmpty()) {
                        item {
                            Text("مقاطع Shorts ⚡", style = MaterialTheme.typography.labelLarge.copy(color = EmeraldPrimary, fontWeight = FontWeight.Bold))
                        }
                        items(filteredShorts) { short ->
                            val isVerified = remember(short.authorHandle) {
                                IslamicDataRepository.isAuthorVerified(short.authorHandle)
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        addQueryToHistory(cleanQuery)
                                        Toast.makeText(context, "جاري فتح: ${short.title}", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(width = 45.dp, height = 65.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                                        VideoThumbnailView(
                                            thumbnailUrl = short.thumbnailUrl,
                                            videoUrl = short.videoUrl,
                                            contentDescription = short.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(short.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("${short.authorName} • ${short.viewsCount} مشاهدة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (isVerified) {
                                                VerifiedBadge(size = 12.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Recitations Results
                    if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.RECITATIONS) && filteredRecitations.isNotEmpty()) {
                        item {
                            Text("التلاوات الصوتية 🎙️", style = MaterialTheme.typography.labelLarge.copy(color = EmeraldPrimary, fontWeight = FontWeight.Bold))
                        }
                        items(filteredRecitations) { rec ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        addQueryToHistory(cleanQuery)
                                        AudioPlayerManager.playRecitation(context, rec)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rec.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${rec.reciterName} • ${rec.durationFormatted}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.PlayCircle, contentDescription = "تشغيل", tint = EmeraldPrimary, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
