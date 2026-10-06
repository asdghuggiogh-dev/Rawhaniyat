package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.repository.IslamicDataRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

enum class FollowTab(val title: String) {
    FOLLOWERS("المتابعون"),
    FOLLOWING("يُتابع")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersListDialog(
    userHandle: String,
    initialTab: FollowTab = FollowTab.FOLLOWERS,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(initialTab) }
    val creatorsMap by IslamicDataRepository.creatorsMap.collectAsState()
    val targetUser = remember(userHandle, creatorsMap) {
        IslamicDataRepository.getUser(userHandle)
    }

    val followers = remember(userHandle, creatorsMap) {
        IslamicDataRepository.getFollowers(userHandle)
    }
    val following = remember(userHandle, creatorsMap) {
        IslamicDataRepository.getFollowing(userHandle)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with title and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = targetUser.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Follow Tabs (المتابعون • يتابع)
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
                    Tab(
                        selected = selectedTab == FollowTab.FOLLOWERS,
                        onClick = { selectedTab = FollowTab.FOLLOWERS },
                        text = {
                            Text(
                                text = "المتابعون (${followers.size})",
                                fontWeight = if (selectedTab == FollowTab.FOLLOWERS) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == FollowTab.FOLLOWING,
                        onClick = { selectedTab = FollowTab.FOLLOWING },
                        text = {
                            Text(
                                text = "يُتابع (${following.size})",
                                fontWeight = if (selectedTab == FollowTab.FOLLOWING) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Accounts List
                val currentList = if (selectedTab == FollowTab.FOLLOWERS) followers else following

                if (currentList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🌙",
                                fontSize = 42.sp
                            )
                            Text(
                                text = if (selectedTab == FollowTab.FOLLOWERS) "لا يوجد متابعون حالياً" else "لا يتابع أي حسابات حالياً",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        items(currentList, key = { it.username }) { user ->
                            FollowUserRowItem(
                                user = user,
                                onUserClick = {
                                    onDismiss()
                                    onUserClick(user.username)
                                },
                                onToggleFollow = {
                                    val isNowFollowing = IslamicDataRepository.toggleFollowCreator(user.username, context)
                                    val msg = if (isNowFollowing) "تمت متابعة ${user.name} ✓" else "تم إلغاء متابعة ${user.name}"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FollowUserRowItem(
    user: UserProfile,
    onUserClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val currentUser by IslamicDataRepository.currentUser.collectAsState()
    val isMe = user.username == currentUser.username

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Avatar
            AsyncImage(
                model = user.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" },
                contentDescription = user.name,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name + Verified Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.isVerified) {
                        VerifiedBadge(size = 14.dp)
                    }
                }

                // @handle
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Followers count
                Text(
                    text = "${formatArabicCount(user.followersCount)} متابع",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Follow / Following Button (if not current user)
        if (!isMe) {
            Button(
                onClick = onToggleFollow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (user.isFollowing) MaterialTheme.colorScheme.surfaceVariant else EmeraldPrimary,
                    contentColor = if (user.isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = if (user.isFollowing) "مُتابع" else "متابعة",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "أنت",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun formatArabicCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1f مليون", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1f ألف", count / 1_000.0)
        else -> count.toString()
    }
}
