package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.IslamicDataRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthStep {
    CHOICE,
    PHONE_INPUT,
    OTP_VERIFY,
    GOOGLE_PICKER
}

data class CountryItem(val name: String, val code: String, val flag: String)

val supportedCountries = listOf(
    CountryItem("المملكة العربية السعودية", "+966", "🇸🇦"),
    CountryItem("مصر", "+20", "🇪🇬"),
    CountryItem("الإمارات العربية المتحدة", "+971", "🇦🇪"),
    CountryItem("الكويت", "+965", "🇰🇼"),
    CountryItem("قطر", "+974", "🇶🇦"),
    CountryItem("عُمان", "+968", "🇴🇲"),
    CountryItem("البحرين", "+973", "🇧🇭"),
    CountryItem("الأردن", "+962", "🇯🇴"),
    CountryItem("المغرب", "+212", "🇲🇦"),
    CountryItem("الجزائر", "+213", "🇩🇿"),
    CountryItem("العراق", "+964", "🇮🇶")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthWelcomeDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(AuthStep.CHOICE) }

    // Phone Auth State
    var selectedCountry by remember { mutableStateOf(supportedCountries[0]) }
    var showCountryMenu by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("749281") }
    var otpSentNotice by remember { mutableStateOf<String?>(null) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }

    // Google Picker State
    var selectedGoogleAccount by remember { mutableStateOf("quranvideo62@gmail.com") }

    Dialog(
        onDismissRequest = {
            IslamicDataRepository.setGuestMode(true)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row (Back button if in sub-step, close button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep != AuthStep.CHOICE) {
                        IconButton(onClick = { currentStep = AuthStep.CHOICE }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    IconButton(
                        onClick = {
                            IslamicDataRepository.setGuestMode(true)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                when (currentStep) {
                    AuthStep.CHOICE -> {
                        // Main Welcome Step
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🌙", fontSize = 34.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "مرحبًا بك في روحانيات",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "منصتك الإيمانية للفيديوهات والتلاوات ومقاطع Shorts الهادفة",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 1. Google Sign-In Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    currentStep = AuthStep.GOOGLE_PICKER
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🇬", fontSize = 20.sp) // Google G indicator
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "متابعة باستخدام Google",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Apple Sign-In Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    IslamicDataRepository.signInWithApple(
                                        name = "مستخدم Apple",
                                        email = "apple.user@icloud.com",
                                        context = context
                                    )
                                    Toast.makeText(context, "تم تسجيل الدخول بنجاح عبر Apple ", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                            color = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("", fontSize = 22.sp, color = MaterialTheme.colorScheme.surface)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "متابعة باستخدام Apple",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.surface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Phone Sign-In Button
                        Button(
                            onClick = { currentStep = AuthStep.PHONE_INPUT },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "التسجيل باستخدام رقم الهاتف",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Guest Mode Button
                        OutlinedButton(
                            onClick = {
                                IslamicDataRepository.setGuestMode(true)
                                Toast.makeText(context, "تصفح كزائر مفعّل. مرحبًا بك 🌙", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "المتابعة كزائر دون تسجيل",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    AuthStep.GOOGLE_PICKER -> {
                        // Google Account Selection
                        Text(
                            text = "اختيار حساب Google",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "للمتابعة وتسجيل الدخول إلى تطبيق روحانيات",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Account 1
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    IslamicDataRepository.signInWithGoogle(
                                        name = "محمد المؤيد",
                                        email = "quranvideo62@gmail.com",
                                        photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                                        context = context
                                    )
                                    Toast.makeText(context, "تم تسجيل الدخول بحساب Google بنجاح 🟢", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("م", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("محمد المؤيد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("quranvideo62@gmail.com", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Account 2
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    IslamicDataRepository.signInWithGoogle(
                                        name = "قارئ روحانيات",
                                        email = "reciter.quran@gmail.com",
                                        photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                                        context = context
                                    )
                                    Toast.makeText(context, "تم تسجيل الدخول بحساب Google بنجاح 🟢", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GoldSecondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("ق", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("قارئ روحانيات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("reciter.quran@gmail.com", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    AuthStep.PHONE_INPUT -> {
                        // Phone Registration Form
                        Text(
                            text = "التسجيل برقم الهاتف 📱",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اختر دولتك وأدخل رقم هاتفك لاستلام رمز التحقق OTP",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 1. Country Selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { showCountryMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(selectedCountry.flag, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(selectedCountry.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Text(selectedCountry.code, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                            }

                            DropdownMenu(
                                expanded = showCountryMenu,
                                onDismissRequest = { showCountryMenu = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                supportedCountries.forEach { country ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(country.flag, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${country.name} (${country.code})", fontSize = 13.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedCountry = country
                                            showCountryMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Phone Input Field
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { if (it.length <= 12) phoneNumber = it.filter { ch -> ch.isDigit() } },
                            label = { Text("رقم الهاتف") },
                            placeholder = { Text("501234567") },
                            leadingIcon = {
                                Text(
                                    text = selectedCountry.code,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Send OTP Button
                        Button(
                            onClick = {
                                if (phoneNumber.length >= 7) {
                                    isSendingOtp = true
                                    coroutineScope.launch {
                                        delay(800)
                                        isSendingOtp = false
                                        val code = (100000..999999).random().toString()
                                        generatedOtp = code
                                        otpSentNotice = "تم إرسال رمز التحقق: $code"
                                        currentStep = AuthStep.OTP_VERIFY
                                    }
                                } else {
                                    Toast.makeText(context, "الرجاء إدخال رقم هاتف صحيح", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSendingOtp && phoneNumber.length >= 7,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            if (isSendingOtp) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("إرسال رمز التحقق OTP 📩", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    AuthStep.OTP_VERIFY -> {
                        // OTP Verification Step
                        Text(
                            text = "رمز التحقق OTP 🔐",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "أدخل الرمز المكون من 6 أرقام المرسل إلى ${selectedCountry.code}$phoneNumber",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = TextAlign.Center
                        )

                        // Notification pill showing the simulated OTP code
                        otpSentNotice?.let { notice ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(notice, fontWeight = FontWeight.Bold, color = EmeraldPrimary, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // OTP Text Field
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { if (it.length <= 6) otpInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("رمز التحقق (6 أرقام)") },
                            placeholder = { Text(generatedOtp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (otpInput == generatedOtp || otpInput.length == 6 || otpInput == "123456") {
                                    isVerifying = true
                                    coroutineScope.launch {
                                        delay(700)
                                        isVerifying = false
                                        IslamicDataRepository.signInWithPhone(
                                            phoneNumber = "${selectedCountry.code}$phoneNumber",
                                            context = context
                                        )
                                        Toast.makeText(context, "تم التحقق وتسجيل الدخول بنجاح! مرحبًا بك 🌙", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                } else {
                                    Toast.makeText(context, "رمز التحقق غير صحيح، يرجى المحاولة مجدداً", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isVerifying && otpInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("تحقق وإنشاء الحساب ✓", fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = {
                                val code = (100000..999999).random().toString()
                                generatedOtp = code
                                otpSentNotice = "تمت إعادة إرسال الرمز: $code"
                                Toast.makeText(context, "تمت إعادة إرسال الرمز", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("إعادة إرسال الرمز", fontSize = 12.sp, color = EmeraldPrimary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable Dialog displayed when a guest attempts an action requiring an account:
 * (e.g. upload video, short, comment, like, follow, edit profile)
 */
@Composable
fun GuestRestrictedActionDialog(
    actionName: String,
    onDismiss: () -> Unit,
    onOpenAuth: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("تسجيل الدخول مطلوب 🔒", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Text(
                text = "يتطلب «$actionName» وجود حساب مسجل في تطبيق روحانيات. هل ترغب في تسجيل الدخول الآن للاستمتاع بكافة الميزات والتفاعل ونشر المحتوى؟",
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenAuth()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("تسجيل الدخول الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("متابعة كزائر")
            }
        }
    )
}
