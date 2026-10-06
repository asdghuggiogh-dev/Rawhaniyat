package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.data.repository.IslamicDataRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

/**
 * YouTube-style Edge-to-Edge Full-Width 16:9 Video Item for Rowhaniyat.
 * Stretches across the screen with zero side padding on thumbnail, clean metadata row,
 * verified publisher badge, views & duration, and seamless spacing.
 */
@Composable
fun VideoCardItem(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOwner: Boolean = false,
    onDeleteSuccess: () -> Unit = {},
    onAuthorClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val isVerified = remember(video.authorHandle) {
        IslamicDataRepository.isAuthorVerified(video.authorHandle)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 1. Full-Width Edge-to-Edge 16:9 Video Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .clickable { onClick() }
        ) {
            VideoThumbnailView(
                thumbnailUrl = video.thumbnailUrl,
                videoUrl = video.videoUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Center subtle play indicator
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Duration Pill at bottom-end (e.g. 19:22)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Text(
                    text = video.durationFormatted.ifBlank { "00:00" },
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Quality Badge (e.g. 1080p FHD) at top-start
            if (video.quality.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = EmeraldPrimary.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = video.quality,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 2. Video Info Row immediately below thumbnail without white gaps
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Small circular publisher avatar
            AsyncImage(
                model = video.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200" },
                contentDescription = video.authorName,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .then(
                        if (onAuthorClick != null) Modifier.clickable { onAuthorClick(video.authorHandle) } else Modifier
                    ),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column: Title + Author + Verified + Views & Time
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title (max 2 lines + Ellipsis)
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Publisher Name + Verified Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .then(
                            if (onAuthorClick != null) Modifier.clickable { onAuthorClick(video.authorHandle) } else Modifier
                        )
                ) {
                    Text(
                        text = video.authorName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isVerified) {
                        VerifiedBadge(size = 14.dp)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Views • Time Ago (e.g. 1.2 مليون مشاهدة • منذ 3 أيام)
                Text(
                    text = "${formatArabicViews(video.viewsCount)} مشاهدة • ${video.timeAgo}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Options Button ⋮
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات الفيديو",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    // Save video
                    DropdownMenuItem(
                        text = { Text(if (video.isSaved) "إزالة من المحفوظات" else "حفظ في قائمة التشغيل") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (video.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = if (video.isSaved) GoldSecondary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showMenu = false
                            IslamicDataRepository.toggleVideoSave(video.id)
                            Toast.makeText(
                                context,
                                if (video.isSaved) "تمت الإزالة من المحفوظات" else "تم حفظ الفيديو بنجاح 🔖",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    // Watch later
                    DropdownMenuItem(
                        text = { Text("إضافة إلى المشاهدة لاحقاً") },
                        leadingIcon = {
                            Icon(Icons.Outlined.WatchLater, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            IslamicDataRepository.toggleVideoSave(video.id)
                            Toast.makeText(context, "تمت الإضافة لقائمة المشاهدة لاحقاً ⏱️", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Share
                    DropdownMenuItem(
                        text = { Text("مشاركة") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Share, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "شاهد «${video.title}» على تطبيق روحانيات 🌙\nhttps://rowhaniyat.app/video/${video.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو"))
                        }
                    )

                    // Copy link
                    DropdownMenuItem(
                        text = { Text("نسخ الرابط") },
                        leadingIcon = {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("رابط الفيديو", "https://rowhaniyat.app/video/${video.id}"))
                            Toast.makeText(context, "تم نسخ رابط الفيديو إلى الحافظة 📋", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Don't recommend
                    DropdownMenuItem(
                        text = { Text("عدم اقتراح هذا المحتوى") },
                        leadingIcon = {
                            Icon(Icons.Outlined.NotInterested, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            Toast.makeText(context, "لن نقترح هذا المحتوى مجدداً", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Report
                    DropdownMenuItem(
                        text = { Text("الإبلاغ") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Report, contentDescription = null, tint = Color(0xFFD32F2F))
                        },
                        onClick = {
                            showMenu = false
                            Toast.makeText(context, "شكراً لإبلاغك، سيتم مراجعة المحتوى من قبل الإدارة", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Author options
                    if (isOwner) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("تعديل البيانات") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Edit, contentDescription = null, tint = EmeraldPrimary)
                            },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف الفيديو", color = Color(0xFFD32F2F)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFD32F2F))
                            },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }

        // 3. Clean subtle divider between videos for seamless YouTube feed
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Edit Video Dialog
    if (showEditDialog) {
        var editTitle by remember { mutableStateOf(video.title) }
        var editDescription by remember { mutableStateOf(video.description) }
        var editThumbnailUrl by remember { mutableStateOf(video.thumbnailUrl) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("تعديل بيانات الفيديو ✏️", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("عنوان الفيديو") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("وصف الفيديو") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    OutlinedTextField(
                        value = editThumbnailUrl,
                        onValueChange = { editThumbnailUrl = it },
                        label = { Text("رابط الصورة المصغرة (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            IslamicDataRepository.updateVideo(
                                id = video.id,
                                title = editTitle.trim(),
                                description = editDescription.trim(),
                                thumbnailUrl = editThumbnailUrl.trim()
                            )
                            Toast.makeText(context, "تم حفظ التعديلات بنجاح ✨", Toast.LENGTH_SHORT).show()
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text("تأكيد حذف الفيديو 🗑️", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف هذا الفيديو؟ لا يمكن التراجع عن هذا الإجراء وسيتم حذفه نهائياً من قاعدة البيانات.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        IslamicDataRepository.deleteVideo(video.id)
                        Toast.makeText(context, "تم حذف الفيديو بنجاح", Toast.LENGTH_SHORT).show()
                        showDeleteConfirmDialog = false
                        onDeleteSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("نعم، حذف الفيديو", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Format Arabic Views count gracefully (e.g. 295 ألف, 1.2 مليون).
 */
fun formatArabicViews(count: Long): String {
    return when {
        count >= 1_000_000 -> {
            val millions = count / 1_000_000.0
            String.format(java.util.Locale.US, "%.1f مليون", millions)
        }
        count >= 1_000 -> {
            val thousands = count / 1_000
            "$thousands ألف"
        }
        else -> "$count"
    }
}
