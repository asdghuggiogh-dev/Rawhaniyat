package com.example.data.model

data class Surah(
    val id: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val revelationType: String, // "مكية" or "مدنية"
    val versesCount: Int,
    val startPage: Int = 1,
    val description: String = "",
    val sampleAyahs: List<Ayah> = emptyList(),
    val audioUrl: String = ""
)

data class Ayah(
    val number: Int,
    val surahNumber: Int,
    val textArabic: String,
    val translation: String = "",
    val tafseer: String = "",
    val audioUrl: String = "",
    val isFavorite: Boolean = false
)

data class Reciter(
    val id: String,
    val name: String,
    val style: String = "مرتل", // مرتل أو مجود
    val nationality: String = "المملكة العربية السعودية",
    val photoUrl: String = "",
    val bio: String = ""
)

data class AudioRecitation(
    val id: String,
    val title: String,
    val surahNumber: Int,
    val surahName: String,
    val reciterName: String,
    val reciterId: String,
    val audioUrl: String,
    val durationFormatted: String,
    val durationMs: Long,
    val licenseInfo: String = "مرخص للاستخدام العام غير التجاري - المصدر: EveryAyah / أرشيف التلاوات الحرة",
    val isFavorite: Boolean = false,
    val playsCount: Int = 12500
)

data class ZikrItem(
    val id: String,
    val text: String,
    val countTarget: Int,
    val currentCount: Int = 0,
    val virtue: String = "",
    val reference: String = "",
    val category: ZikrCategory
)

enum class ZikrCategory(val titleArabic: String) {
    MORNING("أذكار الصباح"),
    EVENING("أذكار المساء"),
    SLEEP("أذكار النوم"),
    WAKING("أذكار الاستيقاظ"),
    AFTER_PRAYER("أذكار بعد الصلاة"),
    TRAVEL("أذكار السفر"),
    MISC("أذكار متنوعة")
}

data class DuaItem(
    val id: String,
    val title: String,
    val textArabic: String,
    val source: String,
    val category: DuaCategory,
    val isFavorite: Boolean = false
)

enum class DuaCategory(val titleArabic: String) {
    QURANIC("أدعية القرآن الكريم"),
    PROPHETIC("أدعية الأنبياء والمرسلين"),
    PROVISION("أدعية الرزق والبركة"),
    RELIEF("أدعية تفريج الهم والكرب"),
    TRAVEL("أدعية السفر"),
    MORNING_EVENING("أدعية الصباح والمساء"),
    GENERAL("أدعية عامة وشاملة")
}

data class VideoItem(
    val id: String,
    val title: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val viewsCount: Long,
    val timeAgo: String,
    val durationFormatted: String,
    val likesCount: Long,
    val commentsCount: Long,
    val repostsCount: Long = 0L,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isReposted: Boolean = false,
    val hashtags: List<String> = emptyList(),
    val description: String = "",
    val category: String = "تلاوات",
    val fileSizeBytes: Long = 0L,
    val formattedSize: String = "",
    val isVertical: Boolean = false,
    val quality: String = "1080p FHD",
    val lastWatchedPositionMs: Long = 0L
)

data class ShortItem(
    val id: String,
    val title: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String,
    val videoUrl: String,
    val likesCount: Long,
    val commentsCount: Long,
    val repostsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isReposted: Boolean = false,
    val isFollowing: Boolean = false,
    val hashtags: List<String> = emptyList(),
    val category: String = "تلاوات",
    val soundTitle: String = "تلاوة خاشعة بصوت هادئ",
    val soundAuthor: String = "روحانيات",
    val viewsCount: Long = 45000,
    val durationSeconds: Int = 45,
    val thumbnailUrl: String = "",
    val description: String = "",
    val isHorizontal: Boolean = false,
    val fileSizeBytes: Long = 0L,
    val formattedSize: String = ""
)

data class StoryItem(
    val id: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String,
    val mediaUri: String,
    val isVideo: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val textCaption: String = "",
    val isViewed: Boolean = false,
    val isMyStory: Boolean = false,
    val durationSeconds: Int = 15
)

data class Comment(
    val id: String,
    val contentId: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String,
    val text: String,
    val timeAgo: String,
    val likesCount: Long,
    val isLiked: Boolean = false,
    val replies: List<Comment> = emptyList()
)

data class UserProfile(
    val id: String,
    val name: String,
    val username: String,
    val bio: String = "مستخدم تطبيق روحانيات 🌙",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val followersCount: Long = 0,
    val followingCount: Long = 0,
    val likesCount: Long = 0,
    val viewsCount: Long = 0,
    val videosCount: Long = 0,
    val isFollowing: Boolean = false,
    val websiteUrl: String = "",
    val country: String = "المملكة العربية السعودية",
    val email: String = "user@rowhaniyat.app"
) {
    // Dynamic rule: verified if 1,000 followers or more
    val isVerified: Boolean
        get() = followersCount >= 1000
}

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val targetId: String = "",
    val targetType: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class NotificationType {
    NEW_FOLLOWER,
    LIKE,
    COMMENT,
    NEW_VIDEO,
    UPLOAD_COMPLETE,
    ADMIN_ANNOUNCEMENT,
    SPIRITUAL_REMINDER,
    REPOST,
    QURAN_REMINDER,
    MORNING_AZKAR,
    EVENING_AZKAR,
    AYAH_OF_THE_DAY
}
