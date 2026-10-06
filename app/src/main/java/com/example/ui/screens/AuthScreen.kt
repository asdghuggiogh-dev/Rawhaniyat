package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
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
import coil.compose.AsyncImage
import com.example.data.repository.IslamicDataRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.SoftEmerald
import com.example.util.VideoUtils

enum class AuthViewMode {
    WELCOME_CHOICE,
    REGISTER_FORM,
    GOOGLE_ACCOUNT_CHOICE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var viewMode by remember { mutableStateOf(AuthViewMode.WELCOME_CHOICE) }

    // Registration Form State
    var accountName by remember { mutableStateOf("") }
    var accountHandle by remember { mutableStateOf("") }
    var accountBio by remember { mutableStateOf("مستخدم تطبيق روحانيات 🌙") }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Android Official Photo Pickers (Zero permission required)
    val pickAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentUri = VideoUtils.persistProfileImage(context, uri, true)
            selectedAvatarUri = permanentUri
            Toast.makeText(context, "تم اختيار صورة الحساب بنجاح ✓", Toast.LENGTH_SHORT).show()
        }
    }

    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentUri = VideoUtils.persistProfileImage(context, uri, false)
            selectedCoverUri = permanentUri
            Toast.makeText(context, "تم اختيار صورة الغلاف بنجاح ✓", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF03221C),
                        Color(0xFF053B32),
                        Color(0xFF021B16)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = viewMode,
            label = "AuthViewTransition"
        ) { mode ->
            when (mode) {
                AuthViewMode.WELCOME_CHOICE -> {
                    WelcomeChoiceView(
                        onSelectRegister = { viewMode = AuthViewMode.REGISTER_FORM },
                        onSelectGoogle = {
                            IslamicDataRepository.signInWithGoogle(
                                name = "عبد الرحمن الشامي",
                                email = "quranvideo62@gmail.com",
                                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                                context = context
                            )
                            Toast.makeText(context, "مرحباً بك مجدداً عبر Google 🌙", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        },
                        onSelectApple = {
                            IslamicDataRepository.signInWithApple(
                                name = "مستخدم Apple",
                                email = "apple_user@icloud.com",
                                context = context
                            )
                            Toast.makeText(context, "مرحباً بك عبر Apple ", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                    )
                }

                AuthViewMode.REGISTER_FORM -> {
                    RegisterFormView(
                        accountName = accountName,
                        onNameChange = { accountName = it },
                        accountHandle = accountHandle,
                        onHandleChange = { accountHandle = it },
                        accountBio = accountBio,
                        onBioChange = { accountBio = it },
                        selectedAvatarUri = selectedAvatarUri,
                        selectedCoverUri = selectedCoverUri,
                        onPickAvatar = {
                            pickAvatarLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onPickCover = {
                            pickCoverLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onBack = { viewMode = AuthViewMode.WELCOME_CHOICE },
                        isSubmitting = isSubmitting,
                        onSubmit = {
                            if (accountName.isBlank()) {
                                Toast.makeText(context, "يرجى كتابة اسم الحساب", Toast.LENGTH_SHORT).show()
                                return@RegisterFormView
                            }
                            if (accountHandle.isBlank()) {
                                Toast.makeText(context, "يرجى كتابة اسم المستخدم (المعرف)", Toast.LENGTH_SHORT).show()
                                return@RegisterFormView
                            }
                            if (selectedAvatarUri == null) {
                                Toast.makeText(context, "يرجى اختيار صورة للحساب من الهاتف 🖼️", Toast.LENGTH_SHORT).show()
                                return@RegisterFormView
                            }
                            if (selectedCoverUri == null) {
                                Toast.makeText(context, "يرجى اختيار صورة الغلاف للملف الشخصي 🌄", Toast.LENGTH_SHORT).show()
                                return@RegisterFormView
                            }

                            isSubmitting = true
                            IslamicDataRepository.registerNewAccount(
                                name = accountName,
                                username = accountHandle,
                                avatarUriString = selectedAvatarUri.toString(),
                                coverUriString = selectedCoverUri.toString(),
                                bio = accountBio,
                                context = context
                            )
                            isSubmitting = false
                            Toast.makeText(context, "أهلاً وسهلاً بك في روحانيات! تم إنشاء حسابك بنجاح ✨", Toast.LENGTH_LONG).show()
                            onLoginSuccess()
                        }
                    )
                }

                AuthViewMode.GOOGLE_ACCOUNT_CHOICE -> {
                    // Fallback
                    viewMode = AuthViewMode.WELCOME_CHOICE
                }
            }
        }
    }
}

@Composable
private fun WelcomeChoiceView(
    onSelectRegister: () -> Unit,
    onSelectGoogle: () -> Unit,
    onSelectApple: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Hero Brand & Logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(EmeraldPrimary, SoftEmerald, GoldSecondary)
                        )
                    )
                    .border(2.dp, GoldSecondary.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌙",
                    fontSize = 44.sp
                )
            }

            Text(
                text = "روحانيات",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "مَنصّة المرئيات والتلاوات الإيمانية المباركة",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = GoldSecondary,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Text(
                    text = "سجّل دخولك الآن للتفاعل مع مقاطع Shorts ونشر الخير ومتابعة القرّاء والمشايخ 🤍",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.88f),
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Action Buttons Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Google Sign-In Button
            Button(
                onClick = onSelectGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F2937)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "🌐", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "تسجيل الدخول باستخدام Google",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // 2. Apple Sign-In Button
            Button(
                onClick = onSelectApple,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111827),
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "تسجيل الدخول باستخدام Apple",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Divider Or
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                Text(
                    text = "أو",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
            }

            // 3. Create Account Button
            Button(
                onClick = onSelectRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftEmerald,
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldSecondary.copy(alpha = 0.5f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = GoldSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "إنشاء حساب جديد ✨",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Terms and Privacy Note
        Text(
            text = "بالتسجيل، أنت توافق على شروط الاستخدام وسياسة الخصوصية لتطبيق روحانيات",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun RegisterFormView(
    accountName: String,
    onNameChange: (String) -> Unit,
    accountHandle: String,
    onHandleChange: (String) -> Unit,
    accountBio: String,
    onBioChange: (String) -> Unit,
    selectedAvatarUri: Uri?,
    selectedCoverUri: Uri?,
    onPickAvatar: () -> Unit,
    onPickCover: () -> Unit,
    onBack: () -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "إنشاء حساب جديد ✨",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "خطوة واحدة لنشر محتواك الإيماني والبدء بـ 0 إحصائيات",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = GoldSecondary
                    )
                )
            }
        }

        // 1. Cover Image Picker (الغلاف)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clickable { onPickCover() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.08f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (selectedCoverUri != null) SoftEmerald else Color.White.copy(alpha = 0.25f)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (selectedCoverUri != null) {
                    AsyncImage(
                        model = selectedCoverUri,
                        contentDescription = "صورة الغلاف",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تغيير الغلاف", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Panorama, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("اضغط لاختيار صورة الغلاف 🌄 *", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("(مطلوب لإكمال الحساب)", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }
        }

        // 2. Avatar Image Picker (صورة الحساب الشخصي)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(
                        2.5.dp,
                        if (selectedAvatarUri != null) SoftEmerald else GoldSecondary,
                        CircleShape
                    )
                    .clickable { onPickAvatar() },
                contentAlignment = Alignment.Center
            ) {
                if (selectedAvatarUri != null) {
                    AsyncImage(
                        model = selectedAvatarUri,
                        contentDescription = "صورة الحساب",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("الصورة *", color = GoldSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (selectedAvatarUri != null) "✓ تم اختيار صورة الحساب" else "اضغط لاختيار صورة الحساب الشخصية (مطلوب)",
                color = if (selectedAvatarUri != null) Color(0xFF66BB6A) else Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 3. Name Input
        OutlinedTextField(
            value = accountName,
            onValueChange = onNameChange,
            label = { Text("اسم الحساب الكامل * (مثال: عبد الله القرني)") },
            placeholder = { Text("اكتب اسمك كما يظهر للمتابعين") },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = SoftEmerald) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = SoftEmerald,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = GoldSecondary,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
            )
        )

        // 4. Username / Handle Input
        OutlinedTextField(
            value = accountHandle,
            onValueChange = { onHandleChange(it.replace("@", "").trim()) },
            label = { Text("اسم المستخدم / المعرف * (مثال: abdullah_q)") },
            placeholder = { Text("معرف فريد بالإنجليزية بدون مسافات") },
            leadingIcon = { Text("@", color = SoftEmerald, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = SoftEmerald,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = GoldSecondary,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
            )
        )

        // 5. Bio Input
        OutlinedTextField(
            value = accountBio,
            onValueChange = onBioChange,
            label = { Text("نبذة عن الحساب (Bio)") },
            placeholder = { Text("اكتب نبذة طيبة أو دعاء تحبه...") },
            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = SoftEmerald) },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = SoftEmerald,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = GoldSecondary,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
            )
        )

        // Notice about default 0 stats
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.35f),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "📊 البيانات الأولية للحساب الجديد:",
                    color = GoldSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "• المتابعون: 0 | يتابع: 0 | الإعجابات: 0 | الفيديوهات: 0 | المشاهدات: 0",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
                Text(
                    text = "• لن يتم إضافة أي متابعين أو أرقام وهمية تلقائياً.",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Submit Button
        Button(
            onClick = onSubmit,
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SoftEmerald,
                contentColor = Color.White
            )
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إنشاء الحساب ودخول التطبيق 🚀",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
