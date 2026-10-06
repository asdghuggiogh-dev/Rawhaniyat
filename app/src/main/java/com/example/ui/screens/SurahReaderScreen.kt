package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ayah
import com.example.data.model.Surah
import com.example.data.repository.IslamicDataRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import kotlinx.coroutines.launch

enum class QuranTranquilTheme(
    val title: String,
    val backgroundColor: Color,
    val cardColor: Color,
    val textColor: Color,
    val accentColor: Color
) {
    WARM_PARCHMENT(
        title = "ورقي دافئ",
        backgroundColor = Color(0xFFFBF8F1),
        cardColor = Color(0xFFF4ECE1),
        textColor = Color(0xFF221A0F),
        accentColor = Color(0xFF996515)
    ),
    MIDNIGHT_DARK(
        title = "ليلي هادئ",
        backgroundColor = Color(0xFF081412),
        cardColor = Color(0xFF0E221E),
        textColor = Color(0xFFE2EFEA),
        accentColor = Color(0xFF10B981)
    ),
    EMERALD_MINT(
        title = "أخضر روحاني",
        backgroundColor = Color(0xFF031E18),
        cardColor = Color(0xFF062C24),
        textColor = Color(0xFFF4FAF7),
        accentColor = Color(0xFFD4AF37)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderScreen(
    surahId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val surah = IslamicDataRepository.surahsList.find { it.id == surahId } ?: IslamicDataRepository.surahsList.first()
    var selectedAyahForDialog by remember { mutableStateOf<Ayah?>(null) }
    var showJumpDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Tranquil Mode States
    var isTranquilMode by remember { mutableStateOf(false) }
    var textSizeSp by remember { mutableFloatStateOf(24f) }
    var tranquilTheme by remember { mutableStateOf(QuranTranquilTheme.WARM_PARCHMENT) }
    var showFontControls by remember { mutableStateOf(false) }

    val currentBgColor = if (isTranquilMode) tranquilTheme.backgroundColor else MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = currentBgColor,
        topBar = {
            if (isTranquilMode) {
                // Minimal Zen Top Bar
                Surface(
                    color = tranquilTheme.backgroundColor.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isTranquilMode = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "الخروج من الوضع الهادئ",
                                    tint = tranquilTheme.textColor
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "سورة ${surah.nameArabic} • الوضع الهادئ",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = tranquilTheme.textColor
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Toggle Font Controls
                            IconButton(onClick = { showFontControls = !showFontControls }) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = "التحكم في حجم الخط",
                                    tint = tranquilTheme.accentColor
                                )
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "سورة ${surah.nameArabic}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${surah.revelationType} • ${surah.versesCount} آية",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        // Tranquil Mode Button
                        FilledTonalButton(
                            onClick = {
                                isTranquilMode = true
                                Toast.makeText(context, "تم تفعيل الوضع الهادئ لقراءة مريحة 🌙", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = GoldSecondary.copy(alpha = 0.2f),
                                contentColor = GoldSecondary
                            )
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الوضع الهادئ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Jump to ayah icon
                        IconButton(onClick = { showJumpDialog = true }) {
                            Icon(Icons.Default.FindInPage, contentDescription = "الانتقال إلى آية")
                        }
                        // Save bookmark
                        IconButton(
                            onClick = {
                                IslamicDataRepository.saveReadingBookmark(surah.nameArabic, 1)
                                Toast.makeText(context, "تم حفظ موضع القراءة 🔖", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = "حفظ موضع القراءة", tint = GoldSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            // Floating Tranquil Controls Panel
            AnimatedVisibility(
                visible = isTranquilMode && showFontControls,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = tranquilTheme.cardColor,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header with close control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إعدادات القراءة الهادئة والخط العثماني",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = tranquilTheme.textColor
                                )
                            )
                            IconButton(onClick = { showFontControls = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = tranquilTheme.textColor)
                            }
                        }

                        // Font size stepper and preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "حجم الخط: ${textSizeSp.toInt()} sp",
                                style = MaterialTheme.typography.bodyMedium.copy(color = tranquilTheme.textColor)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Decrease font button (A-)
                                OutlinedIconButton(
                                    onClick = { if (textSizeSp > 18f) textSizeSp -= 2f },
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Text("A-", fontWeight = FontWeight.Bold, color = tranquilTheme.textColor)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Increase font button (A+)
                                OutlinedIconButton(
                                    onClick = { if (textSizeSp < 40f) textSizeSp += 2f },
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Text("A+", fontWeight = FontWeight.Bold, color = tranquilTheme.textColor)
                                }
                            }
                        }

                        // Font size slider for precision
                        Slider(
                            value = textSizeSp,
                            onValueChange = { textSizeSp = it },
                            valueRange = 18f..40f,
                            colors = SliderDefaults.colors(
                                thumbColor = tranquilTheme.accentColor,
                                activeTrackColor = tranquilTheme.accentColor
                            )
                        )

                        // Tranquil Theme Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuranTranquilTheme.entries.forEach { theme ->
                                val isSelected = tranquilTheme == theme
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { tranquilTheme = theme },
                                    label = { Text(theme.title, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = tranquilTheme.accentColor.copy(alpha = 0.2f),
                                        selectedLabelColor = tranquilTheme.accentColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = if (isTranquilMode) 12.dp else 16.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = if (isTranquilMode) 80.dp else 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isTranquilMode) 10.dp else 16.dp)
        ) {
            // Surah Header Banner (Simplified in tranquil mode)
            item {
                if (isTranquilMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "سورة ${surah.nameArabic}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = tranquilTheme.accentColor,
                                fontFamily = FontFamily.Serif
                            )
                        )
                        if (surah.id != 9) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = tranquilTheme.textColor,
                                    fontFamily = FontFamily.Serif
                                )
                            )
                        }
                        Divider(
                            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                            color = tranquilTheme.accentColor.copy(alpha = 0.3f)
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "سورة ${surah.nameArabic}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${surah.nameEnglish} • ترتيبها: ${surah.id}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            if (surah.id != 9) { // Except At-Tawbah
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary,
                                        fontFamily = FontFamily.Serif
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Surah Ayahs List
            if (surah.sampleAyahs.isNotEmpty()) {
                items(surah.sampleAyahs) { ayah ->
                    AyahItemCard(
                        ayah = ayah,
                        surahName = surah.nameArabic,
                        isTranquilMode = isTranquilMode,
                        textSizeSp = textSizeSp,
                        tranquilTheme = tranquilTheme,
                        onClick = { selectedAyahForDialog = ayah }
                    )
                }
            } else {
                items(surah.versesCount) { index ->
                    val ayahNum = index + 1
                    AyahItemCard(
                        ayah = Ayah(
                            number = ayahNum,
                            surahNumber = surah.id,
                            textArabic = "آية كريمة رقم ($ayahNum) من سورة ${surah.nameArabic} المباركة يقرأها المسلم بتدبر وخشوع",
                            translation = "Verse $ayahNum of Surah ${surah.nameEnglish}."
                        ),
                        surahName = surah.nameArabic,
                        isTranquilMode = isTranquilMode,
                        textSizeSp = textSizeSp,
                        tranquilTheme = tranquilTheme,
                        onClick = {
                            selectedAyahForDialog = Ayah(
                                number = ayahNum,
                                surahNumber = surah.id,
                                textArabic = "آية كريمة رقم ($ayahNum) من سورة ${surah.nameArabic}"
                            )
                        }
                    )
                }
            }
        }
    }

    // Ayah Actions Bottom Sheet
    selectedAyahForDialog?.let { ayah ->
        ModalBottomSheet(
            onDismissRequest = { selectedAyahForDialog = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "﴿ ${ayah.textArabic} ﴾",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        lineHeight = 36.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "سورة ${surah.nameArabic} - آية رقم ${ayah.number}",
                    style = MaterialTheme.typography.labelMedium.copy(color = GoldSecondary)
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Copy Ayah
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Ayah", "${ayah.textArabic} [سورة ${surah.nameArabic}: ${ayah.number}]")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ الآية الكريمة 📋", Toast.LENGTH_SHORT).show()
                            selectedAyahForDialog = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ")
                    }

                    // Share Ayah
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "﴿ ${ayah.textArabic} ﴾\n[سورة ${surah.nameArabic} - آية ${ayah.number}]\nمن تطبيق روحانيات 🌙")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الآية الكريمة"))
                            selectedAyahForDialog = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة")
                    }
                }

                // Bookmark as last read
                Button(
                    onClick = {
                        IslamicDataRepository.saveReadingBookmark(surah.nameArabic, ayah.number)
                        Toast.makeText(context, "تم حفظ الآية كآخر موضع قراءة 🔖", Toast.LENGTH_SHORT).show()
                        selectedAyahForDialog = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ كآخر موضع قراءة")
                }
            }
        }
    }

    // Jump to Ayah Dialog
    if (showJumpDialog) {
        var inputNumber by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("الانتقال إلى رقم آية") },
            text = {
                OutlinedTextField(
                    value = inputNumber,
                    onValueChange = { inputNumber = it },
                    placeholder = { Text("أدخل رقم الآية (1 - ${surah.versesCount})") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ayahNum = inputNumber.trim().toIntOrNull()
                        if (ayahNum != null && ayahNum in 1..surah.versesCount) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(ayahNum)
                            }
                            Toast.makeText(context, "تم الانتقال إلى الآية $ayahNum 📖", Toast.LENGTH_SHORT).show()
                            showJumpDialog = false
                        } else {
                            Toast.makeText(context, "يرجى إدخال رقم آية صحيح بين 1 و ${surah.versesCount}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("انتقال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun AyahItemCard(
    ayah: Ayah,
    surahName: String,
    isTranquilMode: Boolean = false,
    textSizeSp: Float = 24f,
    tranquilTheme: QuranTranquilTheme = QuranTranquilTheme.WARM_PARCHMENT,
    onClick: () -> Unit
) {
    if (isTranquilMode) {
        // Pure Classical Uthmanic Tranquil Layout without borders or cards
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 4.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                // Classical Uthmanic Text with Golden Ayah Rosette
                Text(
                    text = "${ayah.textArabic}  \u06DD${ayah.number}\u06DE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * 1.85f).sp,
                        fontWeight = FontWeight.Medium,
                        color = tranquilTheme.textColor
                    ),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                if (ayah.tafseer.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "﴿ التفسير ﴾: ${ayah.tafseer}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = tranquilTheme.textColor.copy(alpha = 0.7f),
                            fontSize = (textSizeSp * 0.5f).coerceAtLeast(11f).sp,
                            lineHeight = (textSizeSp * 0.8f).coerceAtLeast(16f).sp
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Divider(
                    color = tranquilTheme.textColor.copy(alpha = 0.08f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    } else {
        // Standard Modern Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "${ayah.textArabic} ﴿${ayah.number}﴾",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                if (ayah.tafseer.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "التفسير الميسر: ${ayah.tafseer}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
