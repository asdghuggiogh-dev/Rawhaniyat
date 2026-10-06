package com.example

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.IslamicDataRepository
import com.example.ui.components.ExpandedAudioPlayerDialog
import com.example.ui.components.IslamicTopAppBar
import com.example.ui.components.MiniAudioPlayer
import com.example.ui.navigation.ActiveScreen
import com.example.ui.navigation.MainTab
import com.example.ui.screens.*
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.RowhaniyatTheme
import com.example.ui.theme.SoftEmerald
import com.example.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        IslamicDataRepository.init(this)
        com.example.util.RowhaniyatNotificationManager.init(this)
        val prefs = getSharedPreferences("rowhaniyat_prefs", Context.MODE_PRIVATE)

        setContent {
            val savedThemeModeName = remember { prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name }
            var themeMode by remember {
                mutableStateOf(
                    try {
                        ThemeMode.valueOf(savedThemeModeName)
                    } catch (_: Exception) {
                        ThemeMode.SYSTEM
                    }
                )
            }

            val systemDark = isSystemInDarkTheme()
            val isDarkMode = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            RowhaniyatTheme(darkTheme = isDarkMode) {
                // Enforce RTL for genuine full Arabic layout support
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    RowhaniyatAppRoot(
                        themeMode = themeMode,
                        isDarkMode = isDarkMode,
                        onThemeModeChange = { newMode ->
                            themeMode = newMode
                            prefs.edit().putString("theme_mode", newMode.name).apply()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowhaniyatAppRoot(
    themeMode: ThemeMode,
    isDarkMode: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rowhaniyat_prefs", Context.MODE_PRIVATE) }
    var activeTab by remember { mutableStateOf(MainTab.HOME) }
    var activeScreen by remember { mutableStateOf<ActiveScreen>(ActiveScreen.Main) }
    val screenStack = remember { mutableStateListOf<ActiveScreen>() }
    var showExpandedAudioPlayer by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    val isGuest by IslamicDataRepository.isGuestMode.collectAsState()

    fun navigateTo(screen: ActiveScreen) {
        screenStack.add(activeScreen)
        activeScreen = screen
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            activeScreen = screenStack.removeAt(screenStack.lastIndex)
        } else {
            activeScreen = ActiveScreen.Main
        }
    }

    val isLoggedIn by IslamicDataRepository.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        AuthScreen(
            onLoginSuccess = {
                // User is authenticated
            }
        )
        return
    }

    val hasSeenWelcome = remember { prefs.getBoolean("has_seen_welcome", false) }
    var showWelcomeDialog by remember { mutableStateOf(!hasSeenWelcome) }

    // Request Notification Permission on Android 13+ (Tiramisu)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* permission result handled */ }
        LaunchedEffect(Unit) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Central Online Cloud Sync
    LaunchedEffect(Unit) {
        IslamicDataRepository.syncWithCloud()
    }

    val notifications by IslamicDataRepository.notifications.collectAsState()
    val unreadNotificationsCount = notifications.count { !it.isRead }

    // Intercept back button gracefully
    BackHandler(enabled = activeScreen != ActiveScreen.Main || activeTab != MainTab.HOME || showExpandedAudioPlayer || screenStack.isNotEmpty()) {
        if (showExpandedAudioPlayer) {
            showExpandedAudioPlayer = false
        } else if (screenStack.isNotEmpty()) {
            navigateBack()
        } else if (activeScreen != ActiveScreen.Main) {
            activeScreen = ActiveScreen.Main
        } else if (activeTab != MainTab.HOME) {
            activeTab = MainTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (activeScreen == ActiveScreen.Main && activeTab != MainTab.SHORTS) {
                IslamicTopAppBar(
                    title = "روحانيات",
                    subtitle = when (activeTab) {
                        MainTab.HOME -> "الرئيسية • نفحات إيمانية 🌙"
                        MainTab.SHORTS -> "Shorts • مقاطع قصيرة ⚡"
                        MainTab.ADD -> "إضافة محتوى ＋"
                        MainTab.NOTIFICATIONS -> "الإشعارات • التنبيهات 🔔"
                        MainTab.PROFILE -> "الحساب • ملفي الشخصي 👤"
                    },
                    unreadNotificationsCount = unreadNotificationsCount,
                    onSearchClick = { navigateTo(ActiveScreen.Search) },
                    onNotificationsClick = { navigateTo(ActiveScreen.Notifications) },
                    onSettingsClick = { navigateTo(ActiveScreen.Settings) }
                )
            }
        },
        bottomBar = {
            if (activeScreen == ActiveScreen.Main) {
                Column {
                    // Floating Mini Player when recitation is loaded
                    MiniAudioPlayer(
                        onExpand = { showExpandedAudioPlayer = true }
                    )

                    // 5-Tab Modern Bottom Navigation (الرئيسية | Shorts | نشر | الإشعارات | الحساب)
                    NavigationBar(
                        windowInsets = NavigationBarDefaults.windowInsets,
                        containerColor = if (activeTab == MainTab.SHORTS) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MainTab.values().forEach { tab ->
                            if (tab == MainTab.ADD) {
                                // Prominent center Add button with "نشر" label
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (isGuest) {
                                                Toast.makeText(
                                                    context,
                                                    "يرجى تسجيل الدخول أو إنشاء حساب لنشر المحتوى 🌙",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                showWelcomeDialog = true
                                            } else {
                                                showAddSheet = true
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SoftEmerald,
                                        shadowElevation = 3.dp,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "نشر",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "نشر",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = SoftEmerald
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else {
                                val isSelected = activeTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { activeTab = tab },
                                    alwaysShowLabel = true,
                                    icon = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            if (tab == MainTab.NOTIFICATIONS && unreadNotificationsCount > 0) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(
                                                            containerColor = GoldSecondary,
                                                            contentColor = Color.White
                                                        ) {
                                                            Text(
                                                                text = "$unreadNotificationsCount",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.titleArabic,
                                                        tint = if (isSelected) SoftEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.titleArabic,
                                                    tint = if (isSelected) SoftEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .width(14.dp)
                                                        .height(2.5.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(SoftEmerald)
                                                )
                                            }
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tab.titleArabic,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp,
                                                color = if (isSelected) SoftEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = SoftEmerald.copy(alpha = 0.12f),
                                        selectedIconColor = SoftEmerald,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedTextColor = SoftEmerald,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (activeScreen == ActiveScreen.Main && activeTab == MainTab.SHORTS) 0.dp else paddingValues.calculateTopPadding(),
                    bottom = if (activeScreen == ActiveScreen.Main) paddingValues.calculateBottomPadding() else 0.dp
                )
        ) {
            when (val screen = activeScreen) {
                is ActiveScreen.Main -> {
                    when (activeTab) {
                        MainTab.HOME -> HomeScreen(
                            onNavigateToSurah = { surahId ->
                                navigateTo(ActiveScreen.SurahReader(surahId))
                            },
                            onNavigateToShorts = {
                                activeTab = MainTab.SHORTS
                            },
                            onNavigateToVideo = { videoId ->
                                navigateTo(ActiveScreen.VideoPlayer(videoId))
                            },
                            onNavigateToAzkar = {
                                navigateTo(ActiveScreen.Browse(BrowseCategory.AZKAR))
                            },
                            onNavigateToTasbeeh = {
                                navigateTo(ActiveScreen.Browse(BrowseCategory.TASBEEH))
                            },
                            onNavigateToRecitations = {
                                navigateTo(ActiveScreen.Browse(BrowseCategory.RECITATIONS))
                            },
                            onNavigateToCreate = { type ->
                                navigateTo(ActiveScreen.CreateContent(type))
                            },
                            onNavigateToChannel = { handle ->
                                navigateTo(ActiveScreen.ChannelProfile(handle))
                            }
                        )

                        MainTab.SHORTS -> ShortsScreen(
                            onNavigateToChannel = { handle ->
                                navigateTo(ActiveScreen.ChannelProfile(handle))
                            }
                        )

                        MainTab.ADD -> HomeScreen(
                            onNavigateToSurah = { surahId -> navigateTo(ActiveScreen.SurahReader(surahId)) },
                            onNavigateToShorts = { activeTab = MainTab.SHORTS },
                            onNavigateToVideo = { videoId -> navigateTo(ActiveScreen.VideoPlayer(videoId)) },
                            onNavigateToAzkar = { navigateTo(ActiveScreen.Browse(BrowseCategory.AZKAR)) },
                            onNavigateToTasbeeh = { navigateTo(ActiveScreen.Browse(BrowseCategory.TASBEEH)) },
                            onNavigateToRecitations = { navigateTo(ActiveScreen.Browse(BrowseCategory.RECITATIONS)) },
                            onNavigateToCreate = { type -> navigateTo(ActiveScreen.CreateContent(type)) },
                            onNavigateToChannel = { handle -> navigateTo(ActiveScreen.ChannelProfile(handle)) }
                        )

                        MainTab.NOTIFICATIONS -> NotificationsScreen(
                            onBack = { activeTab = MainTab.HOME },
                            onNavigateToVideo = { videoId ->
                                navigateTo(ActiveScreen.VideoPlayer(videoId))
                            },
                            onNavigateToChannel = { handle ->
                                navigateTo(ActiveScreen.ChannelProfile(handle))
                            }
                        )

                        MainTab.PROFILE -> ProfileScreen(
                            onNavigateToSettings = { navigateTo(ActiveScreen.Settings) },
                            onNavigateToShorts = { activeTab = MainTab.SHORTS },
                            onNavigateToVideo = { videoId ->
                                navigateTo(ActiveScreen.VideoPlayer(videoId))
                            },
                            onNavigateToEditProfile = {
                                navigateTo(ActiveScreen.EditProfile)
                            },
                            onNavigateToChannel = { handle ->
                                navigateTo(ActiveScreen.ChannelProfile(handle))
                            }
                        )
                    }
                }

                is ActiveScreen.EditProfile -> EditProfileScreen(
                    onBack = { navigateBack() }
                )

                is ActiveScreen.CreateContent -> CreateContentScreen(
                    initialType = screen.initialType,
                    onPublished = {
                        screenStack.clear()
                        activeScreen = ActiveScreen.Main
                        activeTab = MainTab.HOME
                    }
                )

                is ActiveScreen.Browse -> BrowseScreen(
                    initialCategory = screen.initialCategory,
                    onNavigateToSurah = { surahId ->
                        navigateTo(ActiveScreen.SurahReader(surahId))
                    }
                )

                is ActiveScreen.SurahReader -> SurahReaderScreen(
                    surahId = screen.surahId,
                    onBack = { navigateBack() }
                )

                is ActiveScreen.VideoPlayer -> VideoDetailScreen(
                    videoId = screen.videoId,
                    onBack = { navigateBack() },
                    onNavigateToChannel = { handle ->
                        navigateTo(ActiveScreen.ChannelProfile(handle))
                    }
                )

                is ActiveScreen.ChannelProfile -> ChannelProfileScreen(
                    userHandle = screen.userHandle,
                    onBack = { navigateBack() },
                    onNavigateToVideo = { videoId ->
                        navigateTo(ActiveScreen.VideoPlayer(videoId))
                    },
                    onNavigateToShort = { shortId ->
                        screenStack.clear()
                        activeScreen = ActiveScreen.Main
                        activeTab = MainTab.SHORTS
                    },
                    onNavigateToOtherChannel = { otherHandle ->
                        navigateTo(ActiveScreen.ChannelProfile(otherHandle))
                    }
                )

                is ActiveScreen.Search -> SearchScreen(
                    onBack = { navigateBack() },
                    onNavigateToSurah = { surahId ->
                        navigateTo(ActiveScreen.SurahReader(surahId))
                    },
                    onNavigateToVideo = { videoId ->
                        navigateTo(ActiveScreen.VideoPlayer(videoId))
                    },
                    onNavigateToChannel = { handle ->
                        navigateTo(ActiveScreen.ChannelProfile(handle))
                    }
                )

                is ActiveScreen.Notifications -> NotificationsScreen(
                    onBack = { navigateBack() },
                    onNavigateToVideo = { videoId ->
                        navigateTo(ActiveScreen.VideoPlayer(videoId))
                    },
                    onNavigateToChannel = { handle ->
                        navigateTo(ActiveScreen.ChannelProfile(handle))
                    }
                )

                is ActiveScreen.Settings -> SettingsScreen(
                    onBack = { navigateBack() },
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { dark ->
                        onThemeModeChange(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)
                    },
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onNavigateToEditProfile = {
                        navigateTo(ActiveScreen.EditProfile)
                    }
                )
            }

            // Expanded Audio Player Overlay
            if (showExpandedAudioPlayer) {
                ExpandedAudioPlayerDialog(
                    onDismiss = { showExpandedAudioPlayer = false }
                )
            }

            // Bottom Sheet for Add Options (🎬 إضافة Short | 🎥 إضافة فيديو طويل)
            if (showAddSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                    ) {
                        Text(
                            text = "إنشاء محتوى في روحانيات",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 1. إضافة Short
                        Surface(
                            onClick = {
                                showAddSheet = false
                                navigateTo(ActiveScreen.CreateContent(ContentType.SHORT))
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(SoftEmerald.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎬", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "إضافة مقطع Short",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "فيديو عمودي سريع وهادف (أقل من 60 ثانية)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. إضافة فيديو طويل
                        Surface(
                            onClick = {
                                showAddSheet = false
                                navigateTo(ActiveScreen.CreateContent(ContentType.LONG_VIDEO))
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(GoldSecondary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎥", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "إضافة فيديو طويل",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "فيديو كامل 16:9 بدقة عالية مع صورة مصغرة",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            // Welcome Dialog (Authentication Flow with Google, Apple, Phone & Guest Mode)
            if (showWelcomeDialog) {
                AuthWelcomeDialog(
                    onDismiss = {
                        showWelcomeDialog = false
                        prefs.edit().putBoolean("has_seen_welcome", true).apply()
                    }
                )
            }
        }
    }
}
