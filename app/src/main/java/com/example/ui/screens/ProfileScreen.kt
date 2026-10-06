package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.model.ShortItem
import com.example.data.model.VideoItem
import com.example.data.repository.IslamicDataRepository
import com.example.ui.components.FollowTab
import com.example.ui.components.FollowersListDialog
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.VideoThumbnailView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.util.VideoUtils

enum class ProfileTab(val title: String) {
    POSTS("المنشورات"),
    SHORTS("Shorts"),
    VIDEOS("الفيديوهات"),
    REPOSTS("إعادة النشر"),
    LIKES("الإعجابات"),
    SAVED("المحفوظات")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToShorts: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToChannel: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by IslamicDataRepository.currentUser.collectAsState()
    val isGuest by IslamicDataRepository.isGuestMode.collectAsState()
    val videos by IslamicDataRepository.videosFeed.collectAsState()
    val shorts by IslamicDataRepository.shortsFeed.collectAsState()
    val posts by IslamicDataRepository.userPosts.collectAsState()

    var selectedTab by remember { mutableStateOf(ProfileTab.POSTS) }
    var showAvatarOptionsSheet by remember { mutableStateOf(false) }
    var showCoverOptionsSheet by remember { mutableStateOf(false) }
    var showFollowersDialog by remember { mutableStateOf(false) }
    var followDialogInitialTab by remember { mutableStateOf(FollowTab.FOLLOWERS) }

    // Modern Photo Picker for direct Avatar change
    val pickAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentUri = VideoUtils.persistProfileImage(context, uri, isAvatar = true)
            IslamicDataRepository.updateAvatar(context, permanentUri.toString())
            Toast.makeText(context, "تم تحديث الصورة الشخصية بنجاح 🌙", Toast.LENGTH_SHORT).show()
        }
    }

    // Modern Photo Picker for direct Cover change
    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentUri = VideoUtils.persistProfileImage(context, uri, isAvatar = false)
            IslamicDataRepository.updateCover(context, permanentUri.toString())
            Toast.makeText(context, "تم تحديث صورة الغلاف بنجاح 🌙", Toast.LENGTH_SHORT).show()
        }
    }

    // Dynamic calculated numbers
    val userVideos = remember(videos, currentUser.username) {
        videos.filter { it.authorHandle == currentUser.username || it.id.startsWith("user_") }
    }
    val userShorts = remember(shorts, currentUser.username) {
        shorts.filter { it.authorHandle == currentUser.username || it.id.startsWith("user_") }
    }
    val repostedItems = remember(videos, shorts) {
        videos.filter { it.isReposted } + shorts.filter { it.isReposted }.map {
            VideoItem(
                id = it.id,
                title = it.title,
                authorName = it.authorName,
                authorHandle = it.authorHandle,
                authorAvatarUrl = it.authorAvatarUrl,
                thumbnailUrl = it.thumbnailUrl,
                videoUrl = it.videoUrl,
                viewsCount = it.viewsCount,
                timeAgo = "مُعاد نشره",
                durationFormatted = "Short",
                likesCount = it.likesCount,
                commentsCount = it.commentsCount,
                repostsCount = it.repostsCount,
                isReposted = true,
                isVertical = true
            )
        }
    }
    val likedVideos = remember(videos, shorts) {
        videos.filter { it.isLiked }
    }
    val savedVideos = remember(videos, shorts) {
        videos.filter { it.isSaved }
    }

    // Dynamic aggregated view and like counts
    val dynamicViewsCount = remember(userVideos, userShorts, currentUser.viewsCount) {
        val sum = userVideos.sumOf { it.viewsCount } + userShorts.sumOf { it.viewsCount }
        if (sum > 0) sum else currentUser.viewsCount
    }
    val dynamicLikesCount = remember(userVideos, userShorts, currentUser.likesCount) {
        val sum = userVideos.sumOf { it.likesCount } + userShorts.sumOf { it.likesCount }
        if (sum > 0) sum else currentUser.likesCount
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Cover Photo & Avatar Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Cover Image (Clickable to change)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(EmeraldPrimary)
                        .clickable { showCoverOptionsSheet = true }
                ) {
                    if (currentUser.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = currentUser.coverUrl,
                            contentDescription = "صورة الغلاف",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Settings Icon
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = Color.White)
                    }

                    // Camera Badge on Cover
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الغلاف", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Big Circular Avatar (Clickable to change directly)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(104.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showAvatarOptionsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUser.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = currentUser.avatarUrl,
                                contentDescription = currentUser.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("🌙", fontSize = 42.sp)
                        }
                    }

                    // Edit camera badge on avatar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable { showAvatarOptionsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "تعديل الصورة", tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // Profile Identity (Name, Username, Bio, Country)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isGuest) "زائر روحانيات" else currentUser.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    if (currentUser.isVerified && !isGuest) {
                        VerifiedBadge(size = 20.dp)
                    }
                }

                Text(
                    text = if (isGuest) "@guest_user" else "@${currentUser.username}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                if (currentUser.country.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                        Text(
                            text = currentUser.country,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isGuest) "تتصفح الآن في وضع الزائر. يمكنك تسجيل الدخول لإنشاء ونشر المقاطع ومتابعة الحسابات." else currentUser.bio,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row: Followers, Following, Likes, Views (All Dynamic!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatItem(
                        label = "المتابعون",
                        value = if (isGuest) "0" else formatNumber(currentUser.followersCount),
                        onClick = {
                            followDialogInitialTab = FollowTab.FOLLOWERS
                            showFollowersDialog = true
                        }
                    )
                    ProfileStatItem(
                        label = "المتابَعون",
                        value = if (isGuest) "0" else formatNumber(currentUser.followingCount),
                        onClick = {
                            followDialogInitialTab = FollowTab.FOLLOWING
                            showFollowersDialog = true
                        }
                    )
                    ProfileStatItem(label = "الإعجابات", value = if (isGuest) "0" else formatNumber(dynamicLikesCount))
                    ProfileStatItem(label = "المشاهدات", value = if (isGuest) "0" else formatNumber(dynamicViewsCount))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons (Edit Profile, Follow, Share)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Edit Profile / Login
                    Button(
                        onClick = {
                            if (isGuest) {
                                IslamicDataRepository.setGuestMode(false)
                                Toast.makeText(context, "تم تسجيل الدخول بنجاح 🌙", Toast.LENGTH_SHORT).show()
                            } else {
                                onNavigateToEditProfile()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isGuest) "تسجيل الدخول" else "تعديل الملف الشخصي", fontWeight = FontWeight.Bold)
                    }

                    // Follow / Unfollow Toggle (Interactive with debounce!)
                    OutlinedButton(
                        onClick = {
                            IslamicDataRepository.toggleFollowUser(context)
                            val statusMsg = if (!currentUser.isFollowing) "تمت متابعة الحساب بنجاح ✓" else "تم إلغاء المتابعة"
                            Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (currentUser.isFollowing) EmeraldPrimary.copy(alpha = 0.1f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (currentUser.isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentUser.isFollowing) "مُتابَع" else "متابعة",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Share Profile (Android Sharesheet)
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "تابع حساب ${currentUser.name} (@${currentUser.username}) على تطبيق روحانيات 🌙:\n${currentUser.bio}\nhttps://rowhaniyat.app/u/${currentUser.username}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الملف الشخصي"))
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // 6 Tabs Selector: Posts, Shorts, Videos, Reposts, Likes, Saved
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = EmeraldPrimary
                    )
                }
            ) {
                ProfileTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = when (tab) {
                                    ProfileTab.POSTS -> "${tab.title} (${posts.size})"
                                    ProfileTab.SHORTS -> "${tab.title} (${userShorts.size})"
                                    ProfileTab.VIDEOS -> "${tab.title} (${userVideos.size})"
                                    ProfileTab.REPOSTS -> "${tab.title} (${repostedItems.size})"
                                    ProfileTab.LIKES -> "${tab.title} (${likedVideos.size})"
                                    ProfileTab.SAVED -> "${tab.title} (${savedVideos.size})"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        // Tab Content Display
        when (selectedTab) {
            ProfileTab.POSTS -> {
                if (posts.isEmpty()) {
                    item {
                        EmptyTabContent(
                            icon = "✍️",
                            title = "لا توجد منشورات حتى الآن",
                            subtitle = "شارك خواطرك وتدبراتك الإيمانية مع مجتمع روحانيات"
                        )
                    }
                } else {
                    items(posts) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = currentUser.avatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(currentUser.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text(post.formattedTime, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(post.text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
                            }
                        }
                    }
                }
            }

            ProfileTab.SHORTS -> {
                if (userShorts.isEmpty()) {
                    item {
                        EmptyTabContent(
                            icon = "📱",
                            title = "لا توجد مقاطع Shorts حتى الآن",
                            subtitle = "انشر أول مقطع Short عمودي من ألبوم هاتفك"
                        )
                    }
                } else {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .heightIn(max = 600.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(userShorts) { short ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(9f / 16f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black)
                                        .clickable { onNavigateToShorts() }
                                ) {
                                    VideoThumbnailView(
                                        thumbnailUrl = short.thumbnailUrl,
                                        videoUrl = short.videoUrl,
                                        contentDescription = short.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = "❤️ ${formatNumber(short.likesCount)}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ProfileTab.VIDEOS -> {
                if (userVideos.isEmpty()) {
                    item {
                        EmptyTabContent(
                            icon = "📺",
                            title = "لا توجد فيديوهات طويلة حتى الآن",
                            subtitle = "ارفع محاضرات وتلاوات كاملة لتظهر في ملفك الشخصي"
                        )
                    }
                } else {
                    items(userVideos) { video ->
                        ProfileVideoRow(video = video, onClick = { onNavigateToVideo(video.id) })
                    }
                }
            }

            ProfileTab.REPOSTS -> {
                if (repostedItems.isEmpty()) {
                    item {
                        EmptyTabContent(
                            icon = "🔁",
                            title = "لا توجد منشورات مُعاد نشرها",
                            subtitle = "المقاطع التي تشاركها وتضيفها إلى قصتك ستظهر هنا"
                        )
                    }
                } else {
                    items(repostedItems) { item ->
                        ProfileVideoRow(video = item, onClick = { onNavigateToVideo(item.id) })
                    }
                }
            }

            ProfileTab.LIKES -> {
                if (likedVideos.isEmpty()) {
                    item {
                        EmptyTabContent(
                            icon = "❤️",
                            title = "قائمة الإعجابات فارغة",
                            subtitle = "اضغط على زر الإعجاب على الفيديوهات التي تلامس قلبك"
                        )
                    }
                } else {
                    items(likedVideos) { video ->
                        ProfileVideoRow(video = video, onClick = { onNavigateToVideo(video.id) })
                    }
                }
            }

            ProfileTab.SAVED -> {
                if (savedVideos.isEmpty()) {
                    item {
                        EmptyTabContent(
                            icon = "🔖",
                            title = "لا توجد محفوظات حتى الآن",
                            subtitle = "احفظ المقاطع والتلاوات المفضلة للرجوع إليها في أي وقت"
                        )
                    }
                } else {
                    items(savedVideos) { video ->
                        ProfileVideoRow(video = video, onClick = { onNavigateToVideo(video.id) })
                    }
                }
            }
        }
    }

    // Avatar Bottom Sheet Options
    if (showAvatarOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarOptionsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("تغيير الصورة الشخصية 👤", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                ListItem(
                    headlineContent = { Text("اختيار صورة من الهاتف") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showAvatarOptionsSheet = false
                            pickAvatarLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                )

                ListItem(
                    headlineContent = { Text("التقاط صورة بالكاميرا") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showAvatarOptionsSheet = false
                            pickAvatarLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                )

                if (currentUser.avatarUrl.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("إزالة الصورة", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                IslamicDataRepository.updateAvatar(context, "")
                                showAvatarOptionsSheet = false
                                Toast.makeText(context, "تمت إزالة الصورة", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
            }
        }
    }

    // Cover Bottom Sheet Options
    if (showCoverOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCoverOptionsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("تغيير صورة الغلاف 🖼️", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                ListItem(
                    headlineContent = { Text("اختيار من المعرض") },
                    leadingContent = { Icon(Icons.Default.Image, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showCoverOptionsSheet = false
                            pickCoverLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                )

                ListItem(
                    headlineContent = { Text("التقاط صورة") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showCoverOptionsSheet = false
                            pickCoverLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                )

                if (currentUser.coverUrl.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("إزالة صورة الغلاف", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                IslamicDataRepository.updateCover(context, "")
                                showCoverOptionsSheet = false
                                Toast.makeText(context, "تمت إزالة صورة الغلاف", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
            }
        }
    }

    if (showFollowersDialog) {
        FollowersListDialog(
            userHandle = currentUser.username,
            initialTab = followDialogInitialTab,
            onDismiss = { showFollowersDialog = false },
            onUserClick = { handle ->
                showFollowersDialog = false
                if (handle != currentUser.username) {
                    onNavigateToChannel(handle)
                }
            }
        )
    }
}

@Composable
fun ProfileStatItem(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
fun ProfileVideoRow(video: VideoItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 66.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                VideoThumbnailView(
                    thumbnailUrl = video.thumbnailUrl,
                    videoUrl = video.videoUrl,
                    contentDescription = video.title,
                    contentScale = if (video.isVertical) ContentScale.Fit else ContentScale.Crop,
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

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${video.authorName} • ${formatNumber(video.viewsCount)} مشاهدة",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
fun EmptyTabContent(icon: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center
        )
    }
}

fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1f مليون", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1f ألف", number / 1_000.0)
        else -> "$number"
    }
}
