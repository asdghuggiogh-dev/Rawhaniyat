package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerManager
import com.example.data.model.*
import com.example.data.repository.IslamicDataRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

enum class BrowseCategory(val title: String, val emoji: String) {
    QURAN("القرآن الكريم", "📖"),
    RECITATIONS("التلاوات", "🎙️"),
    AZKAR("الأذكار", "☀️"),
    DUAS("الأدعية", "🤲"),
    TASBEEH("المسبحة", "📿")
}

@Composable
fun BrowseScreen(
    initialCategory: BrowseCategory = BrowseCategory.QURAN,
    onNavigateToSurah: (Int) -> Unit
) {
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Category Switcher Chips
        ScrollableTabRow(
            selectedTabIndex = selectedCategory.ordinal,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary,
            divider = {}
        ) {
            BrowseCategory.values().forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category.emoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = category.title,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Content body based on category
        when (selectedCategory) {
            BrowseCategory.QURAN -> QuranSurahsView(onNavigateToSurah = onNavigateToSurah)
            BrowseCategory.RECITATIONS -> RecitationsLibraryView()
            BrowseCategory.AZKAR -> AzkarSectionView()
            BrowseCategory.DUAS -> DuasSectionView()
            BrowseCategory.TASBEEH -> TasbeehSectionView()
        }
    }
}

// 1. Holy Quran Surahs View
@Composable
fun QuranSurahsView(onNavigateToSurah: (Int) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val surahs = IslamicDataRepository.surahsList.filter {
        it.nameArabic.contains(searchQuery.trim(), ignoreCase = true) ||
                it.nameEnglish.contains(searchQuery.trim(), ignoreCase = true) ||
                it.id.toString() == searchQuery.trim()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث عن سورة بالاسم أو الرقم...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }

        items(surahs) { surah ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToSurah(surah.id) },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Surah Number inside Islamic Star Badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${surah.id}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Surah details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "سورة ${surah.nameArabic}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${surah.nameEnglish} • ${surah.revelationType} • ${surah.versesCount} آية",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Reading icon
                    FilledTonalIconButton(
                        onClick = { onNavigateToSurah(surah.id) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "قراءة السورة",
                            tint = EmeraldPrimary
                        )
                    }
                }
            }
        }
    }
}

// 2. Audio Recitations Library View
@Composable
fun RecitationsLibraryView() {
    val context = LocalContext.current
    val recitations by IslamicDataRepository.audioRecitations.collectAsState()
    var filterReciter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecitations = recitations.filter {
        (filterReciter == null || it.reciterId == filterReciter) &&
                (searchQuery.isEmpty() || it.title.contains(searchQuery) || it.reciterName.contains(searchQuery))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Reciters filter row
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث عن تلاوة أو قارئ...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterReciter == null,
                        onClick = { filterReciter = null },
                        label = { Text("جميع القراء") }
                    )
                }
                items(IslamicDataRepository.recitersList) { reciter ->
                    FilterChip(
                        selected = filterReciter == reciter.id,
                        onClick = { filterReciter = if (filterReciter == reciter.id) null else reciter.id },
                        label = { Text(reciter.name) }
                    )
                }
            }
        }

        items(filteredRecitations) { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        AudioPlayerManager.playRecitation(context, item)
                    },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎙️", fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.reciterName} • ${item.durationFormatted}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = item.licenseInfo,
                            style = MaterialTheme.typography.labelSmall.copy(color = GoldSecondary, fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            IslamicDataRepository.toggleRecitationFavorite(item.id)
                            Toast.makeText(context, if (item.isFavorite) "تمت الإزالة من المفضلة" else "تمت الإضافة للمفضلة", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (item.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// 3. Azkar Section View with Counter and Tranquil Mode
@Composable
fun AzkarSectionView() {
    var selectedCategory by remember { mutableStateOf(ZikrCategory.MORNING) }
    val azkar = IslamicDataRepository.azkarList.filter { it.category == selectedCategory }
    val context = LocalContext.current

    // Tranquil Mode States for Azkar
    var isTranquilMode by remember { mutableStateOf(false) }
    var zikrTextSizeSp by remember { mutableFloatStateOf(24f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Tranquil Mode Toggle Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isTranquilMode) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = if (isTranquilMode) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTranquilMode) Icons.Default.CheckCircle else Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = if (isTranquilMode) EmeraldPrimary else GoldSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTranquilMode) "الوضع الهادئ للأذكار (مُفعل) 🌙" else "تفعيل الوضع الهادئ للأذكار",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isTranquilMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Switch(
                    checked = isTranquilMode,
                    onCheckedChange = {
                        isTranquilMode = it
                        if (it) {
                            Toast.makeText(context, "تم تفعيل الوضع الهادئ للأذكار مع الخط العثماني 🌿", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldPrimary,
                        checkedTrackColor = EmeraldPrimary.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Font Size Controls (Shown when Tranquil Mode is active)
        if (isTranquilMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حجم الخط العثماني: ${zikrTextSizeSp.toInt()} sp",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedIconButton(
                            onClick = { if (zikrTextSizeSp > 18f) zikrTextSizeSp -= 2f },
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape
                        ) {
                            Text("A-", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedIconButton(
                            onClick = { if (zikrTextSizeSp < 38f) zikrTextSizeSp += 2f },
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape
                        ) {
                            Text("A+", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Categories Scrollable Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ZikrCategory.values()) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.titleArabic) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = EmeraldPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(azkar) { zikr ->
                ZikrCounterCard(
                    zikr = zikr,
                    isTranquilMode = isTranquilMode,
                    textSizeSp = zikrTextSizeSp
                )
            }
        }
    }
}

@Composable
fun ZikrCounterCard(
    zikr: ZikrItem,
    isTranquilMode: Boolean = false,
    textSizeSp: Float = 24f
) {
    var count by remember(zikr.id) { mutableIntStateOf(0) }
    val isFinished = count >= zikr.countTarget
    val context = LocalContext.current

    if (isTranquilMode) {
        // Tranquil Clean Zen Card with Pure Uthmanic Font
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (count < zikr.countTarget) {
                        count++
                        try {
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                            }
                        } catch (_: Exception) {}
                    }
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFinished) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sacred Text in Classical Uthmanic Serif Font
                Text(
                    text = zikr.text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * 1.8f).sp,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (zikr.virtue.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "﴿ الفضل ﴾: ${zikr.virtue}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = (textSizeSp * 0.5f).coerceAtLeast(11f).sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large Soothing Circular Tap Area
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = if (isFinished) EmeraldPrimary else EmeraldPrimary.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFinished) "تم إتمام الذكر المبارك ✓ ($count / ${zikr.countTarget})" else "انقر هنا للتسبيح ($count / ${zikr.countTarget})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFinished) Color.White else EmeraldPrimary
                            )
                        )
                    }
                }
            }
        }
    } else {
        // Standard Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFinished) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = zikr.text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (zikr.virtue.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "فضل الذكر: ${zikr.virtue} (${zikr.reference})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Counter Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button
                    IconButton(onClick = { count = 0 }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "إعادة ضبط", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Progress Indicator
                    Text(
                        text = "التكرار: $count / ${zikr.countTarget}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = if (isFinished) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Large Tap to Count Button
                    Button(
                        onClick = {
                            if (count < zikr.countTarget) {
                                count++
                                try {
                                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                                    }
                                } catch (_: Exception) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFinished) Color.Gray else EmeraldPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (isFinished) "تم الإنجاز ✓" else "تسبيح +1")
                    }
                }
            }
        }
    }
}

// 4. Duas Section View
@Composable
fun DuasSectionView() {
    var selectedCategory by remember { mutableStateOf(DuaCategory.QURANIC) }
    val duas by IslamicDataRepository.duasList.collectAsState()
    val filteredDuas = duas.filter { it.category == selectedCategory }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DuaCategory.values()) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.titleArabic) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredDuas) { dua ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = dua.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "« ${dua.textArabic} »",
                            style = MaterialTheme.typography.titleMedium.copy(
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "المصدر: ${dua.source}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                IslamicDataRepository.toggleDuaFavorite(dua.id)
                                Toast.makeText(context, if (dua.isFavorite) "تمت الإزالة من المفضلة" else "تمت الإضافة للمفضلة", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = if (dua.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "المفضلة",
                                    tint = if (dua.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                TextButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Dua", "${dua.textArabic}\n(${dua.source})"))
                                    Toast.makeText(context, "تم نسخ الدعاء المبارك", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نسخ")
                                }

                                TextButton(onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, "« ${dua.textArabic} »\n${dua.source}\nتطبيق روحانيات 🌙")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(intent, "مشاركة الدعاء"))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مشاركة")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. Electronic Tasbeeh Section View
@Composable
fun TasbeehSectionView() {
    val count by IslamicDataRepository.tasbeehCount.collectAsState()
    val target by IslamicDataRepository.tasbeehTarget.collectAsState()
    val currentPhrase by IslamicDataRepository.currentZikrPhrase.collectAsState()
    val totalSessions by IslamicDataRepository.totalTasbeehSessions.collectAsState()
    val context = LocalContext.current

    val phrases = listOf(
        "سُبْحَانَ اللَّهِ",
        "الْحَمْدُ لِلَّهِ",
        "لَا إِلَهَ إِلَّا اللَّهُ",
        "اللَّهُ أَكْبَرُ",
        "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ",
        "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
        "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Phrase selector
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(phrases) { phrase ->
                FilterChip(
                    selected = currentPhrase == phrase,
                    onClick = { IslamicDataRepository.setTasbeehPhrase(phrase) },
                    label = { Text(phrase) }
                )
            }
        }

        // Target Selector & Total counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "إجمالي التسبيحات: $totalSessions 📿",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(33, 100, 1000).forEach { tgt ->
                    FilterChip(
                        selected = target == tgt,
                        onClick = { IslamicDataRepository.setTasbeehTarget(tgt) },
                        label = { Text("$tgt") }
                    )
                }
            }
        }

        // Current Zikr Display
        Text(
            text = currentPhrase,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            ),
            textAlign = TextAlign.Center
        )

        // Grand Interactive Tasbeeh Circular Button
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(EmeraldPrimary, Color(0xFF03442C), Color(0xFF012115))
                    )
                )
                .clickable {
                    IslamicDataRepository.incrementTasbeeh()
                    try {
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                    } catch (_: Exception) {}
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "من أصل $target",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = GoldSecondary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "اضغط للتسبيح",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.7f))
                )
            }
        }

        // Reset Button
        OutlinedButton(
            onClick = { IslamicDataRepository.resetTasbeeh() },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("إعادة ضبط العداد")
        }

        Spacer(modifier = Modifier.height(70.dp))
    }
}
