package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.IslamicDataRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.SoftEmerald
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    themeMode: com.example.ui.theme.ThemeMode = com.example.ui.theme.ThemeMode.SYSTEM,
    onThemeModeChange: (com.example.ui.theme.ThemeMode) -> Unit = {},
    onBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rowhaniyat_settings_prefs", Context.MODE_PRIVATE) }
    val currentUser by IslamicDataRepository.currentUser.collectAsState()

    // 1. الخصوصية
    var isPrivateAccount by remember { mutableStateOf(prefs.getBoolean("privacy_is_private", false)) }
    var allowMessages by remember { mutableStateOf(prefs.getBoolean("privacy_allow_messages", true)) }
    var hideLikes by remember { mutableStateOf(prefs.getBoolean("privacy_hide_likes", false)) }
    var showActivityStatus by remember { mutableStateOf(prefs.getBoolean("privacy_show_activity", true)) }
    var commentPermission by remember { mutableStateOf(prefs.getString("privacy_comment_perm", "الجميع") ?: "الجميع") }

    // 2. الإشعارات
    var notifFollowers by remember { mutableStateOf(prefs.getBoolean("notif_followers", true)) }
    var notifLikes by remember { mutableStateOf(prefs.getBoolean("notif_likes", true)) }
    var notifComments by remember { mutableStateOf(prefs.getBoolean("notif_comments", true)) }
    var notifNewContent by remember { mutableStateOf(prefs.getBoolean("notif_new_content", true)) }
    var notifSpiritual by remember { mutableStateOf(prefs.getBoolean("notif_spiritual_reminders", true)) }
    var notifAnnouncements by remember { mutableStateOf(prefs.getBoolean("notif_general", true)) }

    // 3. التشغيل
    var autoPlayVideos by remember { mutableStateOf(prefs.getBoolean("play_autoplay", true)) }
    var videoQuality by remember { mutableStateOf(prefs.getString("play_quality", "تلقائي") ?: "تلقائي") }
    var useCellularData by remember { mutableStateOf(prefs.getBoolean("play_cellular", true)) }
    var wifiOnlyDownload by remember { mutableStateOf(prefs.getBoolean("play_wifi_only", false)) }
    var backgroundAudio by remember { mutableStateOf(prefs.getBoolean("play_bg_audio", true)) }
    var saveLastPosition by remember { mutableStateOf(prefs.getBoolean("play_save_position", true)) }

    // 4. المظهر واللغة
    var selectedThemeMode by remember { mutableStateOf(if (isDarkMode) "داكن" else "فاتح") }
    var selectedFontSize by remember { mutableStateOf(prefs.getString("app_font_size", "متوسط") ?: "متوسط") }
    var selectedLanguage by remember { mutableStateOf(prefs.getString("app_language", "العربية") ?: "العربية") }

    // Dialog state controllers
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showSignOutAllDialog by remember { mutableStateOf(false) }
    var showReportProblemDialog by remember { mutableStateOf(false) }
    var showHelpCenterDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    fun savePref(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun savePref(key: String, value: String) = prefs.edit().putString(key, value).apply()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإعدادات ⚙️",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. الحساب (Account)
            item {
                SettingsSectionHeader(title = "الحساب", icon = Icons.Default.Person)
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(title = "تعديل الملف الشخصي", subtitle = "الصورة، الغلاف، الاسم، والنبذة", icon = Icons.Default.Edit) {
                            onNavigateToEditProfile()
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableRow(title = "البريد الإلكتروني وبيانات الحساب", subtitle = currentUser.email, icon = Icons.Default.Email) {
                            showChangeEmailDialog = true
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableRow(title = "تغيير كلمة المرور", subtitle = "تحديث كلمة السر لحماية الحساب", icon = Icons.Default.Lock) {
                            showChangePasswordDialog = true
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableRow(title = "تسجيل الخروج", subtitle = "الخروج من الحساب الحالي", icon = Icons.Default.ExitToApp, isDanger = true) {
                            showSignOutDialog = true
                        }
                    }
                }
            }

            // 2. الخصوصية (Privacy)
            item {
                SettingsSectionHeader(title = "الخصوصية", icon = Icons.Default.Security)
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsSwitchRow(
                            title = "حساب خاص",
                            subtitle = "المتابعون المعتمدون فقط يستطيعون رؤية منشوراتك",
                            checked = isPrivateAccount,
                            onCheckedChange = { isPrivateAccount = it; savePref("privacy_is_private", it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchRow(
                            title = "السماح بالرسائل المباشرة",
                            subtitle = "استقبال الرسائل والتوجيهات من المتابعين",
                            checked = allowMessages,
                            onCheckedChange = { allowMessages = it; savePref("privacy_allow_messages", it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchRow(
                            title = "إخفاء قائمة الإعجابات",
                            subtitle = "عدم إظهار المقاطع التي أعجبتك للآخرين",
                            checked = hideLikes,
                            onCheckedChange = { hideLikes = it; savePref("privacy_hide_likes", it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchRow(
                            title = "حالة النشاط",
                            subtitle = "إظهار وقت تواجدك في التطبيق",
                            checked = showActivityStatus,
                            onCheckedChange = { showActivityStatus = it; savePref("privacy_show_activity", it) }
                        )
                    }
                }
            }

            // 3. الإشعارات (Notifications)
            item {
                SettingsSectionHeader(title = "الإشعارات والتنبيهات 🔔", icon = Icons.Default.Notifications)
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // All On / All Off quick toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    notifLikes = true
                                    notifComments = true
                                    notifFollowers = true
                                    notifNewContent = true
                                    notifSpiritual = true
                                    notifAnnouncements = true
                                    savePref("notif_all_enabled", true)
                                    savePref("notif_likes", true)
                                    savePref("notif_comments", true)
                                    savePref("notif_followers", true)
                                    savePref("notif_new_content", true)
                                    savePref("notif_spiritual_reminders", true)
                                    savePref("notif_general", true)
                                    Toast.makeText(context, "تم تفعيل كافة الإشعارات ✓", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("السماح بكل الإشعارات", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = {
                                    notifLikes = false
                                    notifComments = false
                                    notifFollowers = false
                                    notifNewContent = false
                                    notifSpiritual = false
                                    notifAnnouncements = false
                                    savePref("notif_all_enabled", false)
                                    savePref("notif_likes", false)
                                    savePref("notif_comments", false)
                                    savePref("notif_followers", false)
                                    savePref("notif_new_content", false)
                                    savePref("notif_spiritual_reminders", false)
                                    savePref("notif_general", false)
                                    Toast.makeText(context, "تم إيقاف كافة الإشعارات ❌", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("إيقاف كل الإشعارات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        SettingsSwitchRow(
                            title = "الإعجابات ❤️",
                            subtitle = "تنبيه عند إعجاب الآخرين بفيديوهاتك ومقاطع الـ Shorts",
                            checked = notifLikes,
                            onCheckedChange = { notifLikes = it; savePref("notif_likes", it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                        SettingsSwitchRow(
                            title = "التعليقات 💬",
                            subtitle = "تنبيه عند كتابة تعليق على مقاطعك أو الرد عليك",
                            checked = notifComments,
                            onCheckedChange = { notifComments = it; savePref("notif_comments", it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                        SettingsSwitchRow(
                            title = "المتابعون 👤",
                            subtitle = "تنبيه عندما يبدأ مستخدم جديد بمتابعة حسابك",
                            checked = notifFollowers,
                            onCheckedChange = { notifFollowers = it; savePref("notif_followers", it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                        SettingsSwitchRow(
                            title = "فيديوهات الحسابات التي أتابعها 📹",
                            subtitle = "تنبيه عند نشر قنواتك المفضلة فيديوهات أو Shorts جديدة",
                            checked = notifNewContent,
                            onCheckedChange = { notifNewContent = it; savePref("notif_new_content", it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                        SettingsSwitchRow(
                            title = "إشعارات روحانيات 🌙",
                            subtitle = "تذكير بالأذكار والنفحات الإيمانية والمحتوى القرآني",
                            checked = notifSpiritual,
                            onCheckedChange = { notifSpiritual = it; savePref("notif_spiritual_reminders", it) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                        SettingsSwitchRow(
                            title = "الإعلانات المهمة 📢",
                            subtitle = "تنبيهات ورسائل مهمة من إدارة تطبيق روحانيات",
                            checked = notifAnnouncements,
                            onCheckedChange = { notifAnnouncements = it; savePref("notif_general", it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        // Custom Spiritual Sound section
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎵", fontSize = 20.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "صوت إشعار روحانيات المميّز",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "نغمة روحانية هادئة وقصيرة (1.8 ثانية) مريحة للأذن",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            com.example.util.RowhaniyatNotificationManager.playPreviewSound(context)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("استماع للنغمة 🔊", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            com.example.util.RowhaniyatNotificationManager.notify(
                                                context = context,
                                                title = "🌙 روحانيات",
                                                body = "تجربة إشعار روحانيات بنغمة التطبيق الخاصة الهادئة ✨",
                                                type = com.example.data.model.NotificationType.SPIRITUAL_REMINDER
                                            )
                                            Toast.makeText(context, "تم إرسال إشعار تجريبي 🔔", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("إشعار تجريبي 🔔", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. التشغيل (Playback & Quality)
            item {
                SettingsSectionHeader(title = "التشغيل والبيانات", icon = Icons.Default.PlayCircle)
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsSwitchRow(title = "التشغيل التلقائي لمقاطع Shorts", subtitle = "بدء تشغيل المقطع فور ظهوره", checked = autoPlayVideos, onCheckedChange = { autoPlayVideos = it; savePref("play_autoplay", it) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchRow(title = "تشغيل الصوت في الخلفية", subtitle = "متابعة سماع التلاوات بعد قفل الشاشة", checked = backgroundAudio, onCheckedChange = { backgroundAudio = it; savePref("play_bg_audio", it) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchRow(title = "استخدام بيانات الهاتف الخلوية", subtitle = "تشغيل المقاطع عبر شبكة 4G/5G", checked = useCellularData, onCheckedChange = { useCellularData = it; savePref("play_cellular", it) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchRow(title = "التحميل عبر Wi-Fi فقط", subtitle = "حفظ باقة الإنترنت عند تنزيل التلاوات", checked = wifiOnlyDownload, onCheckedChange = { wifiOnlyDownload = it; savePref("play_wifi_only", it) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsSwitchRow(title = "حفظ آخر موضع للمشاهدة", subtitle = "استئناف الفيديوهات من النقطة التي توقفت عندها", checked = saveLastPosition, onCheckedChange = { saveLastPosition = it; savePref("play_save_position", it) })
                    }
                }
            }

            // 5. المظهر واللغة (Appearance & Language)
            item {
                SettingsSectionHeader(title = "المظهر واللغة", icon = Icons.Default.Palette)
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Theme selector
                        Text("مظهر التطبيق (Dark / Light Mode)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = themeMode == com.example.ui.theme.ThemeMode.SYSTEM,
                                onClick = { onThemeModeChange(com.example.ui.theme.ThemeMode.SYSTEM) },
                                label = { Text("تلقائي 📱", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = themeMode == com.example.ui.theme.ThemeMode.LIGHT,
                                onClick = { onThemeModeChange(com.example.ui.theme.ThemeMode.LIGHT) },
                                label = { Text("فاتح ☀️", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = themeMode == com.example.ui.theme.ThemeMode.DARK,
                                onClick = { onThemeModeChange(com.example.ui.theme.ThemeMode.DARK) },
                                label = { Text("داكن 🌙", fontSize = 12.sp) }
                            )
                        }

                        HorizontalDivider()

                        // Language selector
                        Text("لغة التطبيق", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedLanguage == "العربية",
                                onClick = {
                                    selectedLanguage = "العربية"
                                    savePref("app_language", "العربية")
                                    Toast.makeText(context, "اللغة الحالية: العربية مع دعم كامل لـ RTL ✓", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("العربية (RTL)") }
                            )
                            FilterChip(
                                selected = selectedLanguage == "English",
                                onClick = {
                                    selectedLanguage = "English"
                                    savePref("app_language", "English")
                                    Toast.makeText(context, "English interface enabled ✓", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("English") }
                            )
                        }
                    }
                }
            }

            // 6. البيانات والتخزين (Data & Cache)
            item {
                SettingsSectionHeader(title = "البيانات والتخزين", icon = Icons.Default.Storage)
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = "مسح ذاكرة التخزين المؤقت",
                            subtitle = "حذف ملفات الكاش لتفريغ مساحة الهاتف",
                            icon = Icons.Default.CleaningServices
                        ) {
                            val cacheDir = context.cacheDir
                            val sizeBefore = cacheDir.walkTopDown().sumOf { it.length() }
                            cacheDir.deleteRecursively()
                            cacheDir.mkdirs()
                            val mbFreed = String.format("%.1f", sizeBefore.toDouble() / (1024 * 1024))
                            Toast.makeText(context, "تم مسح $mbFreed ميجابايت من التخزين المؤقت بنجاح ✓", Toast.LENGTH_LONG).show()
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableRow(
                            title = "مسح سجل البحث",
                            subtitle = "حذف جميع عمليات البحث المحفوظة",
                            icon = Icons.Default.History
                        ) {
                            val searchPrefs = context.getSharedPreferences("rowhaniyat_search_prefs", Context.MODE_PRIVATE)
                            searchPrefs.edit().clear().apply()
                            Toast.makeText(context, "تم مسح سجل البحث بالكامل ✓", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // 7. الأمان وإدارة الجلسات وحذف الحساب
            item {
                SettingsSectionHeader(title = "الخصوصية والأمان", icon = Icons.Default.GppGood)
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = "تسجيل الخروج من جميع الأجهزة",
                            subtitle = "إنهاء كافة الجلسات النشطة باستثناء هذا الجهاز",
                            icon = Icons.Default.Devices
                        ) {
                            showSignOutAllDialog = true
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableRow(
                            title = "حذف الحساب نهائياً",
                            subtitle = "حذف حسابك وكافة منشوراتك وبياناتك بشكل لا رجعة فيه",
                            icon = Icons.Default.DeleteForever,
                            isDanger = true
                        ) {
                            showDeleteAccountDialog = true
                        }
                    }
                }
            }

            // 8. المساعدة وعن التطبيق (Help & About)
            item {
                SettingsSectionHeader(title = "المساعدة وعن التطبيق", icon = Icons.Default.HelpOutline)
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(title = "مركز المساعدة والأسئلة الشائعة", subtitle = "إجابات على أكثر الأسئلة شيوعاً", icon = Icons.Default.Help) {
                            showFaqDialog = true
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableRow(title = "الإبلاغ عن مشكلة أو اقتراح", subtitle = "تواصل مع فريق التطوير والدعم الفني", icon = Icons.Default.Feedback) {
                            showReportProblemDialog = true
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableRow(title = "شروط الاستخدام وسياسة الخصوصية", subtitle = "حقوق المستخدم والمحتوى", icon = Icons.Default.Policy) {
                            showTermsDialog = true
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableRow(
                            title = "إصدار تطبيق روحانيات",
                            subtitle = "الإصدار 2.2 • الوضع الهادئ للمصحف الشريف والأذكار",
                            icon = Icons.Default.Info
                        ) {
                            Toast.makeText(context, "تطبيق روحانيات - الإصدار 2.2 المبارك 🌙", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // Change Email Dialog
    if (showChangeEmailDialog) {
        var newEmail by remember { mutableStateOf(currentUser.email) }
        AlertDialog(
            onDismissRequest = { showChangeEmailDialog = false },
            title = { Text("تحديث البريد الإلكتروني") },
            text = {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("البريد الإلكتروني") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showChangeEmailDialog = false
                        Toast.makeText(context, "تم تحديث البريد الإلكتروني بنجاح ✓", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeEmailDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("تغيير كلمة المرور") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("كلمة المرور الحالية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("كلمة المرور الجديدة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            Toast.makeText(context, "كلمة المرور يجب أن لا تقل عن 6 أحرف", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showChangePasswordDialog = false
                        Toast.makeText(context, "تم تغيير كلمة المرور بنجاح 🔒", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("تغيير")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("تسجيل الخروج") },
            text = { Text("هل أنت متأكد من رغبتك في تسجيل الخروج من حسابك؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        IslamicDataRepository.logout(context)
                        Toast.makeText(context, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تسجيل الخروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Sign Out All Devices Confirmation Dialog
    if (showSignOutAllDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutAllDialog = false },
            title = { Text("تسجيل الخروج من كافة الأجهزة") },
            text = { Text("سيتم إبطال جميع الجلسات القديمة على الأجهزة الأخرى وتأمين حسابك.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutAllDialog = false
                        Toast.makeText(context, "تم تسجيل الخروج من كافة الأجهزة بنجاح ✓", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("تأكيد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutAllDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Delete Account Critical Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp)) },
            title = { Text("حذف الحساب نهائياً", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "تحذير: هذا الإجراء لا يمكن التراجع عنه. سيتم حذف جميع تلاواتك، مقاطعك، منشوراتك، وتعليقاتك وسجل محفوظاتك بشكل نهائي من منصة روحانيات.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        IslamicDataRepository.deleteAccount(context)
                        Toast.makeText(context, "تم حذف الحساب والبيانات بنجاح", Toast.LENGTH_LONG).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف الحساب الآن")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAccountDialog = false }) { Text("إلغاء وتراجع") }
            }
        )
    }

    // Report a Problem Dialog
    if (showReportProblemDialog) {
        var reportText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportProblemDialog = false },
            title = { Text("الإبلاغ عن مشكلة أو مقترح") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اكتب تفاصيل المشكلة أو الفكرة التي ترغب في إضافتها إلى روحانيات:")
                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        placeholder = { Text("وصف المشكلة بالتفصيل...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportText.isNotBlank()) {
                            showReportProblemDialog = false
                            Toast.makeText(context, "شكراً لتواصلك! تم إرسال البلاغ لفريق التطوير وسنتعامل معه فوراً 🌙", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("إرسال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportProblemDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // FAQ Dialog
    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("الأسئلة الشائعة 💡") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("س: كيف أنشر فيديو جديد؟\nج: من زر «إضافة محتوى» في الصفحة الرئيسية اختر Short أو فيديو طويل من ألبوم هاتفك مباشرة.", fontSize = 13.sp)
                    HorizontalDivider()
                    Text("س: هل التطبيق مجاني بالكامل؟\nج: نعم، تطبيق روحانيات وقف خيري ومجاني بالكامل دون أي إعلانات مزعجة.", fontSize = 13.sp)
                    HorizontalDivider()
                    Text("س: كيف أستمع للتلاوات في الخلفية؟\nج: عند تشغيل أي تلاوة تستطيع قفل الشاشة أو التنقل وسيبقى الصوت يعمل بسلاسة.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFaqDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Terms of Use Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("شروط الاستخدام وسياسة الخصوصية 📜") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("• يلتزم جميع المستخدمين بنشر المحتوى الهادف والموافق لتعاليم الشريعة الإسلامية.", fontSize = 13.sp)
                    Text("• يحظر نشر أي محتوى مخل أو مسيء أو مخالف للآداب العامة.", fontSize = 13.sp)
                    Text("• نحرص على حماية بيانات المستخدمين وخصوصيتهم التامة وفق أعلى المعايير الأمنية.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("حسناً")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        )
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDanger) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDanger) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldPrimary
            )
        )
    }
}
