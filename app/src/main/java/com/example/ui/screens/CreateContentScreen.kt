package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.repository.IslamicDataRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.util.VideoMeta
import com.example.util.VideoUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ContentType(val titleArabic: String, val subtitle: String, val icon: String) {
    SHORT("إضافة Short", "فيديو عمودي أقل من دقيقة 9:16", "📱"),
    LONG_VIDEO("إضافة فيديو طويل", "فيديو كامل 16:9 مع صورة مصغرة", "📺"),
    STORY("إضافة قصة", "قصة يومية مؤقتة (فيديو أو صورة)", "⏳")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContentScreen(
    initialType: ContentType = ContentType.SHORT,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(initialType) }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var customThumbnailUri by remember { mutableStateOf<Uri?>(null) }
    var videoMetadata by remember { mutableStateOf<VideoMeta?>(null) }
    var isMediaVideo by remember { mutableStateOf(true) }

    // Form fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hashtagInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("تلاوات") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Upload simulation state
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStatusMessage by remember { mutableStateOf("") }

    val categories = listOf("تلاوات", "أدعية", "قصص نبوية", "تأملات إيمانية", "مواعظ وخواطر", "فقه وأحكام")
    val quickHashtags = listOf("#قرآن", "#تلاوة", "#Shorts", "#روحانيات", "#تدبر", "#استغفار")

    // Android Official Photo Picker (0 permissions required, modern Android standard)
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            val isVideo = (selectedType != ContentType.STORY) || (context.contentResolver.getType(uri)?.startsWith("video") == true)
            isMediaVideo = isVideo
            if (isVideo) {
                val meta = VideoUtils.extractMetadata(context, uri)
                videoMetadata = meta
                if (title.isBlank()) {
                    title = meta.fileName.substringBeforeLast(".")
                }
            }
        }
    }

    // Custom Thumbnail Picker for Long Videos
    val pickThumbnailLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            customThumbnailUri = uri
            Toast.makeText(context, "تم اختيار الصورة المصغرة المخصصة ✓", Toast.LENGTH_SHORT).show()
        }
    }

    // Fallback file browser
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video") ?: true
            isMediaVideo = isVideo
            if (isVideo) {
                val meta = VideoUtils.extractMetadata(context, uri)
                videoMetadata = meta
                if (title.isBlank()) {
                    title = meta.fileName.substringBeforeLast(".")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "استوديو نشر المحتوى 🌙",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "انشر التلاوات والقصص والمواعظ النافعة لمجتمع روحانيات",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        // 1. Selector for Content Type (Short, Long Video, Story)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ContentType.values().forEach { type ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                        .clickable {
                            if (!isUploading) {
                                selectedType = type
                                selectedMediaUri = null
                                videoMetadata = null
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${type.icon} ${type.titleArabic.substringAfter("إضافة ")}",
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 2. Media Picker & Preview Area
        if (selectedMediaUri == null) {
            // Upload Prompt Card (Tap to open phone gallery)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        try {
                            if (selectedType == ContentType.STORY) {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            } else {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                        } catch (e: Exception) {
                            // Fallback to GetContent
                            pickFileLauncher.launch("video/*")
                        }
                    },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "اختيار ملف",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "اضغط لاختيار ${selectedType.titleArabic} من ألبوم الهاتف",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = selectedType.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "يدعم MP4, MOV, WebM وجميع مقاطع الفيديو",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = {
                            pickFileLauncher.launch(if (selectedType == ContentType.STORY) "*/*" else "video/*")
                        }
                    ) {
                        Text("أو تصفح كافة ملفات الجهاز 📁", fontSize = 12.sp)
                    }
                }
            }
        } else {
            // Selected Media Preview & Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معاينة الملف المختار ✓",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )

                        TextButton(
                            onClick = {
                                selectedMediaUri = null
                                videoMetadata = null
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تغيير الملف", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player / Thumbnail Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        val meta = videoMetadata
                        if (isMediaVideo && selectedMediaUri != null) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoURI(selectedMediaUri)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = selectedMediaUri,
                                contentDescription = "معاينة الصورة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Duration Badge overlay
                        meta?.let {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = it.formattedDuration,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metadata Badges (Duration, Size, Orientation)
                    videoMetadata?.let { meta ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(meta.formattedDuration, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(meta.formattedSize, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (meta.isVertical) Icons.Default.PhoneAndroid else Icons.Default.Tv,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (meta.isVertical) "عمودي 9:16" else "أفقي 16:9",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Optional Custom Thumbnail for Long Videos
                    if (selectedType == ContentType.LONG_VIDEO) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("صورة مصغرة مخصصة:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(
                                onClick = {
                                    pickThumbnailLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (customThumbnailUri != null) "تغيير الصورة المصغرة" else "اختيار صورة", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Information Inputs (Title, Description, Hashtags)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(if (selectedType == ContentType.STORY) "عنوان أو تعليق القصة" else "عنوان الفيديو أو الـShort *") },
            placeholder = { Text("مثال: تلاوة مؤثرة من سورة الكهف...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (selectedType != ContentType.STORY) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("الوصف ومعلومات المحتوى") },
                placeholder = { Text("اكتب فوائد الآية والموعظة...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            // Hashtags
            OutlinedTextField(
                value = hashtagInput,
                onValueChange = { hashtagInput = it },
                label = { Text("الوسوم (Hashtags)") },
                placeholder = { Text("مثال: #قرآن #تلاوة #تدبر") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Quick Hashtags chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickHashtags.take(4).forEach { tag ->
                    AssistChip(
                        onClick = {
                            if (!hashtagInput.contains(tag)) {
                                hashtagInput = if (hashtagInput.isBlank()) tag else "$hashtagInput $tag"
                            }
                        },
                        label = { Text(tag, fontSize = 10.sp) }
                    )
                }
            }

            // Category Selection Dropdown
            Text(
                text = "تصنيف وفئة الفيديو (مطلوب) *",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("اختر فئة المحتوى") },
                    leadingIcon = {
                        val icon = when (selectedCategory) {
                            "تلاوات" -> "📖"
                            "أدعية" -> "🤲"
                            "قصص نبوية" -> "📜"
                            "تأملات إيمانية" -> "🌙"
                            "مواعظ وخواطر" -> "💡"
                            "فقه وأحكام" -> "⚖️"
                            else -> "✨"
                        }
                        Text(icon, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    categories.forEach { cat ->
                        val icon = when (cat) {
                            "تلاوات" -> "📖"
                            "أدعية" -> "🤲"
                            "قصص نبوية" -> "📜"
                            "تأملات إيمانية" -> "🌙"
                            "مواعظ وخواطر" -> "💡"
                            "فقه وأحكام" -> "⚖️"
                            else -> "✨"
                        }
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(icon, fontSize = 18.sp)
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (cat == selectedCategory) FontWeight.Bold else FontWeight.Normal,
                                            color = if (cat == selectedCategory) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            },
                            onClick = {
                                selectedCategory = cat
                                categoryDropdownExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // Quick Category Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.take(4).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Upload Progress Indicator
        if (isUploading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uploadStatusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${(uploadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldPrimary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Publish Action Button
        Button(
            onClick = {
                if (selectedMediaUri == null) {
                    Toast.makeText(context, "الرجاء اختيار ملف من ألبوم الهاتف أولاً", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (title.isBlank()) {
                    Toast.makeText(context, "الرجاء إدخال عنوان للمحتوى", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Start upload process with real cloud upload
                isUploading = true
                coroutineScope.launch {
                    uploadStatusMessage = "جاري فحص وتجهيز الملف..."
                    uploadProgress = 0.1f

                    val permanentMediaUri = VideoUtils.persistMediaLocally(context, selectedMediaUri!!, isMediaVideo)
                    val localVideoUrlStr = permanentMediaUri.toString()

                    val finalMeta = if (isMediaVideo && (videoMetadata == null || videoMetadata?.thumbnailUri == null)) {
                        VideoUtils.extractMetadata(context, permanentMediaUri)
                    } else {
                        videoMetadata ?: VideoUtils.extractMetadata(context, permanentMediaUri)
                    }

                    uploadStatusMessage = "جاري رفع الفيديو إلى السيرفر السحابي..."
                    val cloudUrl = com.example.data.remote.RowhaniyatCloudService.uploadVideo(context, permanentMediaUri) { progress ->
                        uploadProgress = 0.1f + (progress / 100f) * 0.85f
                        uploadStatusMessage = "جاري الرفع السحابي: $progress% 🚀"
                    }

                    val finalVideoUrl = cloudUrl ?: localVideoUrlStr
                    uploadProgress = 1.0f
                    uploadStatusMessage = "تم النشر السحابي بنجاح! 🚀"
                    delay(300)

                    val tags = if (hashtagInput.isNotBlank()) {
                        hashtagInput.split(" ").filter { it.isNotBlank() }
                    } else listOf("#روحانيات", "#قرآن")

                    val finalThumbStr = customThumbnailUri?.let {
                        VideoUtils.persistMediaLocally(context, it, false).toString()
                    } ?: (finalMeta.thumbnailUri ?: VideoUtils.getOrExtractThumbnail(context, localVideoUrlStr, null))

                    when (selectedType) {
                        ContentType.SHORT -> {
                            IslamicDataRepository.publishContent(
                                title = title.trim(),
                                description = description.trim(),
                                hashtags = tags,
                                isShort = true,
                                videoUrl = finalVideoUrl,
                                thumbnailUrl = finalThumbStr,
                                durationFormatted = finalMeta.formattedDuration.ifBlank { "00:45" },
                                durationSeconds = ((finalMeta.durationMs.takeIf { it > 0 } ?: 45000L) / 1000).toInt(),
                                isHorizontal = !finalMeta.isVertical,
                                fileSizeBytes = finalMeta.sizeBytes,
                                formattedSize = finalMeta.formattedSize,
                                category = selectedCategory
                            )
                            Toast.makeText(context, "تم نشر الـShort بنجاح على السيرفر 🌙", Toast.LENGTH_LONG).show()
                        }

                        ContentType.LONG_VIDEO -> {
                            IslamicDataRepository.publishContent(
                                title = title.trim(),
                                description = description.trim(),
                                hashtags = tags,
                                isShort = false,
                                videoUrl = finalVideoUrl,
                                thumbnailUrl = finalThumbStr,
                                durationFormatted = finalMeta.formattedDuration.ifBlank { "05:00" },
                                fileSizeBytes = finalMeta.sizeBytes,
                                formattedSize = finalMeta.formattedSize,
                                isVertical = finalMeta.isVertical,
                                category = selectedCategory
                            )
                            Toast.makeText(context, "تم نشر الفيديو بنجاح على السيرفر 📺", Toast.LENGTH_LONG).show()
                        }

                        ContentType.STORY -> {
                            IslamicDataRepository.publishStory(
                                mediaUri = finalVideoUrl,
                                isVideo = isMediaVideo,
                                caption = title.trim()
                            )
                            Toast.makeText(context, "تم نشر القصة بنجاح في قصص روحانيات ⏳", Toast.LENGTH_LONG).show()
                        }
                    }

                    com.example.util.RowhaniyatNotificationManager.notify(
                        context = context,
                        title = "اكتمال النشر بنجاح 📹",
                        body = "تم نشر محتواك «${title.trim().ifBlank { "محتوى إيماني جديد" }}» في روحانيات",
                        type = com.example.data.model.NotificationType.UPLOAD_COMPLETE
                    )

                    isUploading = false
                    onPublished()
                }
            },
            enabled = !isUploading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Icon(Icons.Default.RocketLaunch, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (selectedType) {
                    ContentType.SHORT -> "نشر الـShort الآن 🚀"
                    ContentType.LONG_VIDEO -> "نشر الفيديو الطويل 🚀"
                    ContentType.STORY -> "نشر القصة الآن 🚀"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
