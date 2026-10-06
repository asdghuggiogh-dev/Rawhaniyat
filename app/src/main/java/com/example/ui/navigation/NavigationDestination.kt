package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.screens.BrowseCategory
import com.example.ui.screens.ContentType

enum class MainTab(
    val titleArabic: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isAction: Boolean = false
) {
    HOME("الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
    SHORTS("Shorts", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline),
    ADD("نشر", Icons.Filled.Add, Icons.Outlined.Add, isAction = true),
    NOTIFICATIONS("الإشعارات", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    PROFILE("الحساب", Icons.Filled.Person, Icons.Outlined.Person);

    companion object {
        // Backwards compatibility alias
        val SUBSCRIPTIONS = NOTIFICATIONS
        val QURAN = NOTIFICATIONS
    }
}

sealed class ActiveScreen {
    object Main : ActiveScreen()
    data class SurahReader(val surahId: Int) : ActiveScreen()
    data class VideoPlayer(val videoId: String) : ActiveScreen()
    object Search : ActiveScreen()
    object Notifications : ActiveScreen()
    object Settings : ActiveScreen()
    object EditProfile : ActiveScreen()
    data class CreateContent(val initialType: ContentType = ContentType.SHORT) : ActiveScreen()
    data class Browse(val initialCategory: BrowseCategory = BrowseCategory.QURAN) : ActiveScreen()
    data class ChannelProfile(val userHandle: String) : ActiveScreen()
}
