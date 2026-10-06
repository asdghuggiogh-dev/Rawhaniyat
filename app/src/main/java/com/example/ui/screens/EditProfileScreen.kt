package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.IslamicDataRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.util.VideoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val currentUser by IslamicDataRepository.currentUser.collectAsState()

    var name by remember { mutableStateOf(currentUser.name) }
    var username by remember { mutableStateOf(currentUser.username) }
    var bio by remember { mutableStateOf(currentUser.bio) }
    var websiteUrl by remember { mutableStateOf(currentUser.websiteUrl) }
    var country by remember { mutableStateOf(currentUser.country) }
    var currentAvatarUri by remember { mutableStateOf(currentUser.avatarUrl) }
    var currentCoverUri by remember { mutableStateOf(currentUser.coverUrl) }

    var showAvatarOptionsSheet by remember { mutableStateOf(false) }
    var showCoverOptionsSheet by remember { mutableStateOf(false) }

    // Modern Photo Picker for Avatar
    val pickAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentUri = VideoUtils.persistProfileImage(context, uri, isAvatar = true)
            currentAvatarUri = permanentUri.toString()
            Toast.makeText(context, "تم اختيار الصورة الشخصية ✓", Toast.LENGTH_SHORT).show()
        }
    }

    // Modern Photo Picker for Cover
    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentUri = VideoUtils.persistProfileImage(context, uri, isAvatar = false)
            currentCoverUri = permanentUri.toString()
            Toast.makeText(context, "تم اختيار صورة الغلاف ✓", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تعديل الملف الشخصي",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "الرجاء إدخال الاسم الكريم", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            IslamicDataRepository.updateFullProfile(
                                context = context,
                                name = name.trim(),
                                username = username.trim().removePrefix("@"),
                                bio = bio.trim(),
                                website = websiteUrl.trim(),
                                country = country.trim(),
                                avatarUrl = currentAvatarUri,
                                coverUrl = currentCoverUri
                            )
                            Toast.makeText(context, "تم حفظ التغييرات بنجاح 🌙", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    ) {
                        Text(
                            text = "حفظ",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            // Cover and Avatar Edit Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                // Cover Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(EmeraldPrimary)
                        .clickable { showCoverOptionsSheet = true }
                ) {
                    if (currentCoverUri.isNotBlank()) {
                        AsyncImage(
                            model = currentCoverUri,
                            contentDescription = "صورة الغلاف",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Edit cover button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تغيير الغلاف", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Avatar Photo
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(110.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showAvatarOptionsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentAvatarUri.isNotBlank()) {
                            AsyncImage(
                                model = currentAvatarUri,
                                contentDescription = "الصورة الشخصية",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("🌙", fontSize = 42.sp)
                        }
                    }

                    // Edit badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                            .clickable { showAvatarOptionsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "تغيير الصورة",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكريم *") },
                    placeholder = { Text("مثال: عبد الرحمن الشامي") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم (المعرّف) *") },
                    placeholder = { Text("مثال: abdurrahman_sh") },
                    prefix = { Text("@", fontWeight = FontWeight.Bold, color = EmeraldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("النبذة التعريفية (Bio)") },
                    placeholder = { Text("اكتب نبذة عن اهتماماتك وتلاواتك...") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = { Text("رابط الموقع / القناة (اختياري)") },
                    placeholder = { Text("https://rowhaniyat.app") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = GoldSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("البلد (اختياري)") },
                    placeholder = { Text("المملكة العربية السعودية") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "الرجاء إدخال الاسم الكريم", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        IslamicDataRepository.updateFullProfile(
                            context = context,
                            name = name.trim(),
                            username = username.trim().removePrefix("@"),
                            bio = bio.trim(),
                            website = websiteUrl.trim(),
                            country = country.trim(),
                            avatarUrl = currentAvatarUri,
                            coverUrl = currentCoverUri
                        )
                        Toast.makeText(context, "تم حفظ التغييرات بنجاح 🌙", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ التغييرات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    // Avatar Bottom Sheet Options
    if (showAvatarOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarOptionsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "تعديل الصورة الشخصية",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                ListItem(
                    headlineContent = { Text("اختيار صورة من الهاتف") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showAvatarOptionsSheet = false
                            pickAvatarLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                )

                ListItem(
                    headlineContent = { Text("التقاط صورة بالكاميرا") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showAvatarOptionsSheet = false
                            pickAvatarLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                )

                if (currentAvatarUri.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("إزالة الصورة الشخصية", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                currentAvatarUri = ""
                                showAvatarOptionsSheet = false
                                Toast.makeText(context, "تمت إزالة الصورة", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
            }
        }
    }

    // Cover Bottom Sheet Options
    if (showCoverOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCoverOptionsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "تعديل صورة الغلاف",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                ListItem(
                    headlineContent = { Text("اختيار من المعرض") },
                    leadingContent = { Icon(Icons.Default.Image, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showCoverOptionsSheet = false
                            pickCoverLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                )

                ListItem(
                    headlineContent = { Text("التقاط صورة") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showCoverOptionsSheet = false
                            pickCoverLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                )

                if (currentCoverUri.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("إزالة صورة الغلاف", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                currentCoverUri = ""
                                showCoverOptionsSheet = false
                                Toast.makeText(context, "تمت إزالة صورة الغلاف", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
            }
        }
    }
}
