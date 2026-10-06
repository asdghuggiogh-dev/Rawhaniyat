package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationType
import com.example.data.repository.IslamicDataRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SoftEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit = {},
    onNavigateToChannel: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val notifications by IslamicDataRepository.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإشعارات والتنبيهات 🔔",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        IslamicDataRepository.markAllNotificationsRead()
                        Toast.makeText(context, "تم تحديد جميع الإشعارات كمقروءة ✓", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = SoftEmerald
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تحديد الكل كمقروء",
                            color = SoftEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔔", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد إشعارات جديدة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ستظهر هنا الإشعارات المتعلقة بتفاعلاتك ومتابعيك ومحتواك",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            } else {
                items(notifications, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                IslamicDataRepository.markNotificationRead(item.id)
                                if (item.targetType == "video" || item.targetType == "short") {
                                    if (item.targetId.isNotBlank()) onNavigateToVideo(item.targetId)
                                } else if (item.targetType == "channel") {
                                    if (item.targetId.isNotBlank()) onNavigateToChannel(item.targetId)
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!item.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val emoji = when (item.type) {
                                NotificationType.NEW_FOLLOWER -> "👤"
                                NotificationType.LIKE -> "❤️"
                                NotificationType.COMMENT -> "💬"
                                NotificationType.NEW_VIDEO -> "📹"
                                NotificationType.UPLOAD_COMPLETE -> "✅"
                                NotificationType.ADMIN_ANNOUNCEMENT -> "📢"
                                NotificationType.SPIRITUAL_REMINDER -> "🌙"
                                NotificationType.REPOST -> "🔁"
                                NotificationType.QURAN_REMINDER -> "📖"
                                NotificationType.MORNING_AZKAR -> "☀️"
                                NotificationType.EVENING_AZKAR -> "🌙"
                                NotificationType.AYAH_OF_THE_DAY -> "✨"
                            }
                            Surface(
                                shape = CircleShape,
                                color = SoftEmerald.copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 22.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.timeAgo,
                                    style = MaterialTheme.typography.labelSmall.copy(color = SoftEmerald, fontSize = 10.sp)
                                )
                            }
                            if (!item.isRead) {
                                Surface(
                                    shape = CircleShape,
                                    color = SoftEmerald,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                        }
                    }
                }
            }
        }
    }
}
