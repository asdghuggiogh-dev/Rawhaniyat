package com.example.data.repository

import android.content.Context
import com.example.data.local.RowhaniyatDatabase
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.FollowEntity
import com.example.data.local.entity.StoryEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VideoEntity
import com.example.data.model.*
import com.example.data.remote.RowhaniyatCloudService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object IslamicDataRepository {

    private var database: RowhaniyatDatabase? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessionViewedVideos = mutableSetOf<String>()

    private val _featuredVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val featuredVideos: StateFlow<List<VideoItem>> = _featuredVideos.asStateFlow()

    // Default Creators Map with realistic follower counts and bio
    private val defaultCreatorsMap: Map<String, UserProfile> = listOf(
        UserProfile(
            id = "creator_othman",
            name = "الشيخ عثمان الخميس",
            username = "othmanalkhamees",
            bio = "إمام وخطيب وداعية إسلامي • فتاوى، تلاوات، وتفسير القرآن الكريم 📖",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
            coverUrl = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=800",
            followersCount = 1250000,
            followingCount = 18,
            likesCount = 8900000,
            viewsCount = 35000000,
            videosCount = 48,
            isFollowing = false
        ),
        UserProfile(
            id = "creator_omarabdelkafy",
            name = "د. عمر عبد الكافي",
            username = "omarabdelkafy",
            bio = "داعية إسلامي مهتم بالإعجاز العلمي في القرآن ومكارم الأخلاق 🌿",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
            coverUrl = "https://images.unsplash.com/photo-1519817650390-64a93db51149?w=800",
            followersCount = 980000,
            followingCount = 24,
            likesCount = 5600000,
            viewsCount = 22000000,
            videosCount = 36,
            isFollowing = false
        ),
        UserProfile(
            id = "creator_alafasy",
            name = "مشاري العفاسي",
            username = "alafasy",
            bio = "قارئ القرآن الكريم وإمام المسجد الكبير بدولة الكويت 🎙️",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            coverUrl = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=800",
            followersCount = 2400000,
            followingCount = 12,
            likesCount = 14500000,
            viewsCount = 68000000,
            videosCount = 64,
            isFollowing = false
        ),
        UserProfile(
            id = "creator_haramain",
            name = "تسجيلات الحرمين الشريفين",
            username = "haramain_recitations",
            bio = "تلاوات وخطب المسجد الحرام والمسجد النبوي الشريف بجودة عالية 🕋",
            avatarUrl = "https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=200",
            coverUrl = "https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=800",
            followersCount = 520000,
            followingCount = 5,
            likesCount = 3800000,
            viewsCount = 19000000,
            videosCount = 82,
            isFollowing = false
        ),
        UserProfile(
            id = "creator_rowhaniyat",
            name = "روحانيات ومواعظ",
            username = "rowhaniyat_official",
            bio = "القناة الرسمية لمنصة روحانيات • نفحات إيمانية، تدبر، وقصص القرآن 🌙",
            avatarUrl = "https://images.unsplash.com/photo-1519817650390-64a93db51149?w=200",
            coverUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800",
            followersCount = 350000,
            followingCount = 42,
            likesCount = 2100000,
            viewsCount = 9800000,
            videosCount = 52,
            isFollowing = false
        ),
        UserProfile(
            id = "creator_qabasat",
            name = "قبسات من نور",
            username = "qabasat_noor",
            bio = "خواطر وتأملات قرآنية ترتقي بالروح وتنير الدرب ✨",
            avatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200",
            coverUrl = "https://images.unsplash.com/photo-1470115636492-6d2b56f9146d?w=800",
            followersCount = 85000,
            followingCount = 31,
            likesCount = 740000,
            viewsCount = 3200000,
            videosCount = 28,
            isFollowing = false
        ),
        UserProfile(
            id = "creator_khaled",
            name = "م. خالد البلوشي",
            username = "khaled_balushi",
            bio = "تأملات في آيات الله الكونية وتفسير ميسر لكتاب الله 🌌",
            avatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=200",
            coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800",
            followersCount = 12400,
            followingCount = 65,
            likesCount = 150000,
            viewsCount = 680000,
            videosCount = 19,
            isFollowing = false
        ),
        UserProfile(
            id = "creator_dhikr",
            name = "ذاكرون وذاكرات",
            username = "dhikr_community",
            bio = "مجتمع الذاكرين • أذكار الصباح والمساء، أدعية وأوراد نبوية 📿",
            avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200",
            coverUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800",
            followersCount = 45000,
            followingCount = 80,
            likesCount = 410000,
            viewsCount = 1800000,
            videosCount = 31,
            isFollowing = false
        ),
        UserProfile(
            id = "creator_talib",
            name = "طالب علم مبتدئ",
            username = "talib_elm",
            bio = "أسعى في طلب العلم الشرعي وحفظ كتاب الله 🌸 (حساب غير موثق حالياً - دون 1,000 متابع)",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
            followersCount = 999, // Perfect for testing the dynamic 1000 verification rule!
            followingCount = 120,
            likesCount = 8400,
            viewsCount = 45000,
            videosCount = 7,
            isFollowing = false
        )
    ).associateBy { it.username }

    private val _creatorsMap = MutableStateFlow<Map<String, UserProfile>>(defaultCreatorsMap)
    val creatorsMap: StateFlow<Map<String, UserProfile>> = _creatorsMap.asStateFlow()

    // Current User State
    private val _currentUser = MutableStateFlow(
        UserProfile(
            id = "current_user_1",
            name = "عبد الرحمن الشامي",
            username = "abdurrahman_sh",
            bio = "طالب علم ومحب للقرآن الكريم والتدبر 🌙 | ناشر للخير والمواعظ الطيبة",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
            coverUrl = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=800&auto=format&fit=crop&q=80",
            followersCount = 24800,
            followingCount = 132,
            likesCount = 580000,
            viewsCount = 2100000,
            videosCount = 16,
            websiteUrl = "https://rowhaniyat.app",
            country = "المملكة العربية السعودية"
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    fun setGuestMode(isGuest: Boolean) {
        _isGuestMode.value = isGuest
    }

    fun init(context: android.content.Context) {
        initStorage(context)
        if (database != null) return

        val db = RowhaniyatDatabase.getInstance(context)
        database = db

        // Sync with Central Cloud Backend
        syncWithCloud()

        // Calculate initial featured from defaults
        recalculateFeatured(_videosFeed.value)

        repositoryScope.launch {
            try {
                val count = db.videoDao().getVideosCount()
                if (count == 0) {
                    // Seed initial long videos
                    val initialLongEntities = _videosFeed.value.map { item ->
                        VideoEntity(
                            id = item.id,
                            title = item.title,
                            authorName = item.authorName,
                            authorHandle = item.authorHandle,
                            authorAvatarUrl = item.authorAvatarUrl,
                            authorUserId = "system_author",
                            thumbnailUrl = item.thumbnailUrl,
                            videoUrl = item.videoUrl,
                            viewsCount = item.viewsCount,
                            timestamp = System.currentTimeMillis() - 86400000L * 2,
                            timeAgo = item.timeAgo,
                            durationFormatted = item.durationFormatted,
                            durationSeconds = 600,
                            likesCount = item.likesCount,
                            commentsCount = item.commentsCount,
                            repostsCount = item.repostsCount,
                            sharesCount = 1200,
                            isLiked = item.isLiked,
                            isSaved = item.isSaved,
                            isReposted = item.isReposted,
                            hashtags = item.hashtags.joinToString(","),
                            description = item.description,
                            isShort = false,
                            isVertical = item.isVertical,
                            isHorizontal = true,
                            fileSizeBytes = item.fileSizeBytes,
                            formattedSize = item.formattedSize,
                            lastWatchedPositionMs = item.lastWatchedPositionMs
                        )
                    }

                    // Seed initial shorts
                    val initialShortEntities = _shortsFeed.value.map { item ->
                        VideoEntity(
                            id = item.id,
                            title = item.title,
                            authorName = item.authorName,
                            authorHandle = item.authorHandle,
                            authorAvatarUrl = item.authorAvatarUrl,
                            authorUserId = "system_author",
                            thumbnailUrl = item.thumbnailUrl,
                            videoUrl = item.videoUrl,
                            viewsCount = item.viewsCount,
                            timestamp = System.currentTimeMillis() - 86400000L,
                            timeAgo = "أمس",
                            durationFormatted = "00:${item.durationSeconds}",
                            durationSeconds = item.durationSeconds,
                            likesCount = item.likesCount,
                            commentsCount = item.commentsCount,
                            repostsCount = item.repostsCount,
                            sharesCount = item.sharesCount,
                            isLiked = item.isLiked,
                            isSaved = item.isSaved,
                            isReposted = item.isReposted,
                            hashtags = item.hashtags.joinToString(","),
                            description = item.description,
                            isShort = true,
                            isVertical = true,
                            isHorizontal = item.isHorizontal,
                            fileSizeBytes = item.fileSizeBytes,
                            formattedSize = item.formattedSize
                        )
                    }

                    db.videoDao().insertVideosIfAbsent(initialLongEntities + initialShortEntities)

                    // Seed initial stories
                    val initialStoryEntities = _storiesFeed.value.map { item ->
                        StoryEntity(
                            id = item.id,
                            authorName = item.authorName,
                            authorHandle = item.authorHandle,
                            authorAvatarUrl = item.authorAvatarUrl,
                            authorUserId = "system_author",
                            mediaUri = item.mediaUri,
                            isVideo = item.isVideo,
                            textCaption = item.textCaption,
                            isViewed = item.isViewed,
                            isMyStory = item.isMyStory,
                            durationSeconds = item.durationSeconds,
                            timestamp = item.timestamp
                        )
                    }
                    db.storyDao().insertStoriesIfAbsent(initialStoryEntities)

                    // Seed initial comments
                    val initialCommentEntities = mutableListOf<CommentEntity>()
                    _comments.value.forEach { (contentId, list) ->
                        list.forEach { c ->
                            initialCommentEntities.add(
                                CommentEntity(
                                    id = c.id,
                                    contentId = contentId,
                                    authorName = c.authorName,
                                    authorHandle = c.authorHandle,
                                    authorAvatarUrl = c.authorAvatarUrl,
                                    authorUserId = "system_author",
                                    text = c.text,
                                    timestamp = System.currentTimeMillis(),
                                    timeAgo = c.timeAgo,
                                    likesCount = c.likesCount,
                                    isLiked = c.isLiked
                                )
                            )
                        }
                    }
                    db.commentDao().insertCommentsIfAbsent(initialCommentEntities)
                }

                // Observe Long Videos from Room continuously
                launch {
                    db.videoDao().getVideosByType(isShort = false).collect { entities ->
                        if (entities.isNotEmpty()) {
                            val items = entities.map { it.toVideoItem() }
                            _videosFeed.value = items
                            recalculateFeatured(items)
                        }
                    }
                }

                // Observe Shorts from Room continuously
                launch {
                    db.videoDao().getVideosByType(isShort = true).collect { entities ->
                        if (entities.isNotEmpty()) {
                            _shortsFeed.value = entities.map { it.toShortItem() }
                        }
                    }
                }

                // Observe Stories from Room continuously
                launch {
                    db.storyDao().getAllStories().collect { entities ->
                        if (entities.isNotEmpty()) {
                            _storiesFeed.value = entities.map { it.toStoryItem() }
                        }
                    }
                }

                // Seed users in Room if empty
                val usersCount = db.userDao().getUsersCount()
                if (usersCount == 0) {
                    val initialUserEntities = defaultCreatorsMap.values.map { user ->
                        UserEntity(
                            handle = user.username,
                            id = user.id,
                            name = user.name,
                            bio = user.bio,
                            avatarUrl = user.avatarUrl,
                            coverUrl = user.coverUrl,
                            followersCount = user.followersCount,
                            followingCount = user.followingCount,
                            likesCount = user.likesCount,
                            viewsCount = user.viewsCount,
                            isFollowing = user.isFollowing,
                            country = user.country,
                            websiteUrl = user.websiteUrl,
                            email = user.email,
                            isCurrentUser = false
                        )
                    }
                    db.userDao().insertUsers(initialUserEntities)
                }

                // Observe users from Room continuously
                launch {
                    db.userDao().getAllUsers().collect { entities ->
                        if (entities.isNotEmpty()) {
                            val map = entities.associate { entity ->
                                entity.handle to UserProfile(
                                    id = entity.id,
                                    name = entity.name,
                                    username = entity.handle,
                                    bio = entity.bio,
                                    avatarUrl = entity.avatarUrl,
                                    coverUrl = entity.coverUrl,
                                    followersCount = entity.followersCount,
                                    followingCount = entity.followingCount,
                                    likesCount = entity.likesCount,
                                    viewsCount = entity.viewsCount,
                                    videosCount = 12,
                                    isFollowing = entity.isFollowing,
                                    country = entity.country,
                                    websiteUrl = entity.websiteUrl,
                                    email = entity.email
                                )
                            }
                            _creatorsMap.value = map
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun recalculateFeatured(videoList: List<VideoItem> = _videosFeed.value) {
        _featuredVideos.value = calculateFeaturedVideos(videoList)
    }

    /**
     * Smart sorting algorithm for «فيديوهات طويلة مميزة»:
     * Combines views, likes, comments, shares, duration, and freshness.
     * Prevents views from being the sole factor.
     */
    fun calculateFeaturedVideos(videos: List<VideoItem>): List<VideoItem> {
        if (videos.isEmpty()) return emptyList()

        // Prioritize horizontal long videos, fallback to all videos if none
        val longVideos = videos.filter { !it.isVertical }
        val candidates = if (longVideos.isNotEmpty()) longVideos else videos

        return candidates.sortedByDescending { video ->
            val views = video.viewsCount.toDouble()
            val likes = video.likesCount.toDouble()
            val comments = video.commentsCount.toDouble()
            val reposts = video.repostsCount.toDouble()

            // Weighted engagement:
            // Views: 0.15
            // Likes: 3.5 (high user appreciation)
            // Comments: 6.0 (deep conversation)
            // Reposts/Shares: 8.0 (viral quality)
            val engagement = (views * 0.15) + (likes * 3.5) + (comments * 6.0) + (reposts * 8.0)

            // Freshness bonus: recent uploads get high boost
            val recencyBonus = when {
                video.timeAgo.contains("الآن") -> 2000.0
                video.timeAgo.contains("دقيق") || video.timeAgo.contains("ساعة") -> 1200.0
                video.timeAgo.contains("يوم") -> 500.0
                else -> 100.0
            }

            engagement + recencyBonus
        }
    }

    fun initStorage(context: android.content.Context) {
        try {
            val prefs = context.getSharedPreferences("rowhaniyat_user_prefs", android.content.Context.MODE_PRIVATE)
            val loggedIn = prefs.getBoolean("is_logged_in", false)
            _isLoggedIn.value = loggedIn

            val name = prefs.getString("user_name", null)
            val username = prefs.getString("user_username", null)
            val userId = prefs.getString("user_id", "user_${System.currentTimeMillis()}")
            val bio = prefs.getString("user_bio", "مستخدم تطبيق روحانيات 🌙")
            val avatar = prefs.getString("user_avatar", "")
            val cover = prefs.getString("user_cover", "")
            val website = prefs.getString("user_website", "")
            val country = prefs.getString("user_country", "المملكة العربية السعودية")
            val isFollowing = prefs.getBoolean("user_is_following", false)
            val followers = prefs.getLong("user_followers", 0L)
            val following = prefs.getLong("user_following", 0L)
            val likes = prefs.getLong("user_likes", 0L)
            val views = prefs.getLong("user_views", 0L)
            val videos = prefs.getLong("user_videos", 0L)

            if (name != null && username != null) {
                val restored = UserProfile(
                    id = userId ?: "user_1",
                    name = name,
                    username = username,
                    bio = bio ?: "مستخدم تطبيق روحانيات 🌙",
                    avatarUrl = avatar ?: "",
                    coverUrl = cover ?: "",
                    followersCount = followers,
                    followingCount = following,
                    likesCount = likes,
                    viewsCount = views,
                    videosCount = videos,
                    websiteUrl = website ?: "",
                    country = country ?: "المملكة العربية السعودية",
                    isFollowing = isFollowing
                )
                _currentUser.value = restored
                _creatorsMap.update { it + (username to restored) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveToStorage(context: android.content.Context) {
        try {
            val prefs = context.getSharedPreferences("rowhaniyat_user_prefs", android.content.Context.MODE_PRIVATE)
            val user = _currentUser.value
            prefs.edit()
                .putBoolean("is_logged_in", _isLoggedIn.value)
                .putString("user_id", user.id)
                .putString("user_name", user.name)
                .putString("user_username", user.username)
                .putString("user_bio", user.bio)
                .putString("user_avatar", user.avatarUrl)
                .putString("user_cover", user.coverUrl)
                .putString("user_website", user.websiteUrl)
                .putString("user_country", user.country)
                .putBoolean("user_is_following", user.isFollowing)
                .putLong("user_followers", user.followersCount)
                .putLong("user_following", user.followingCount)
                .putLong("user_likes", user.likesCount)
                .putLong("user_views", user.viewsCount)
                .putLong("user_videos", user.videosCount)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateProfile(name: String, username: String, bio: String) {
        _currentUser.update {
            it.copy(
                name = name,
                username = username,
                bio = bio
            )
        }
    }

    fun updateAvatar(context: android.content.Context, newAvatarUri: String) {
        _currentUser.update { it.copy(avatarUrl = newAvatarUri) }
        saveToStorage(context)
        repositoryScope.launch {
            RowhaniyatCloudService.saveUserProfile(_currentUser.value)
        }
    }

    fun updateCover(context: android.content.Context, newCoverUri: String) {
        _currentUser.update { it.copy(coverUrl = newCoverUri) }
        saveToStorage(context)
        repositoryScope.launch {
            RowhaniyatCloudService.saveUserProfile(_currentUser.value)
        }
    }

    fun updateFullProfile(
        context: android.content.Context,
        name: String,
        username: String,
        bio: String,
        website: String,
        country: String,
        avatarUrl: String? = null,
        coverUrl: String? = null
    ) {
        val updated = _currentUser.value.copy(
            name = name,
            username = username,
            bio = bio,
            websiteUrl = website,
            country = country,
            avatarUrl = avatarUrl ?: _currentUser.value.avatarUrl,
            coverUrl = coverUrl ?: _currentUser.value.coverUrl
        )
        _currentUser.value = updated
        _creatorsMap.update { it + (username to updated) }
        saveToStorage(context)
        repositoryScope.launch {
            RowhaniyatCloudService.saveUserProfile(updated)
        }
    }

    fun registerNewAccount(
        name: String,
        username: String,
        avatarUriString: String,
        coverUriString: String,
        bio: String = "مستخدم تطبيق روحانيات 🌙",
        context: android.content.Context
    ): UserProfile {
        setGuestMode(false)
        val cleanHandle = username.trim().replace("@", "").replace(" ", "_").lowercase()
        val userId = "user_${cleanHandle}_${System.currentTimeMillis()}"
        val newUser = UserProfile(
            id = userId,
            name = name.trim(),
            username = cleanHandle,
            avatarUrl = avatarUriString,
            coverUrl = coverUriString,
            bio = bio,
            followersCount = 0L,
            followingCount = 0L,
            likesCount = 0L,
            viewsCount = 0L,
            videosCount = 0L,
            isFollowing = false
        )
        _currentUser.value = newUser
        _creatorsMap.update { it + (cleanHandle to newUser) }
        _isLoggedIn.value = true
        saveToStorage(context)

        repositoryScope.launch {
            try {
                RowhaniyatCloudService.saveUserProfile(newUser)
                database?.userDao()?.insertUser(
                    com.example.data.local.entity.UserEntity(
                        handle = cleanHandle,
                        id = userId,
                        name = newUser.name,
                        bio = newUser.bio,
                        avatarUrl = newUser.avatarUrl,
                        coverUrl = newUser.coverUrl,
                        followersCount = 0L,
                        followingCount = 0L,
                        likesCount = 0L,
                        viewsCount = 0L,
                        isFollowing = false,
                        country = newUser.country,
                        websiteUrl = newUser.websiteUrl,
                        email = newUser.email,
                        isCurrentUser = true
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return newUser
    }

    fun signInWithGoogle(
        name: String,
        email: String,
        photoUrl: String,
        context: android.content.Context
    ): UserProfile {
        setGuestMode(false)
        val cleanUsername = email.substringBefore("@").replace(".", "_").lowercase()
        val existingCloudUser = _creatorsMap.value[cleanUsername]
        val userId = existingCloudUser?.id ?: "google_${email.hashCode().toUInt()}"
        val updatedUser = existingCloudUser?.copy(
            name = if (existingCloudUser.name.isNotBlank()) existingCloudUser.name else name,
            avatarUrl = if (existingCloudUser.avatarUrl.isNotBlank()) existingCloudUser.avatarUrl else photoUrl
        ) ?: UserProfile(
            id = userId,
            name = name,
            username = cleanUsername,
            email = email,
            avatarUrl = photoUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" },
            bio = "مستخدم تطبيق روحانيات عبر Google 🌙",
            followersCount = 0L,
            followingCount = 0L,
            likesCount = 0L,
            viewsCount = 0L,
            videosCount = 0L
        )
        _currentUser.value = updatedUser
        _creatorsMap.update { it + (cleanUsername to updatedUser) }
        _isLoggedIn.value = true
        saveToStorage(context)
        repositoryScope.launch {
            RowhaniyatCloudService.saveUserProfile(updatedUser)
        }
        return updatedUser
    }

    fun signInWithApple(
        name: String,
        email: String,
        context: android.content.Context
    ): UserProfile {
        setGuestMode(false)
        val cleanUsername = if (email.isNotBlank()) email.substringBefore("@") else "apple_user_${System.currentTimeMillis() % 10000}"
        val existingCloudUser = _creatorsMap.value[cleanUsername]
        val userId = existingCloudUser?.id ?: "apple_${cleanUsername.hashCode().toUInt()}"
        val updatedUser = existingCloudUser ?: UserProfile(
            id = userId,
            name = name.ifBlank { "مستخدم Apple" },
            username = cleanUsername,
            email = email,
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
            bio = "مستخدم تطبيق روحانيات عبر Apple ",
            followersCount = 0L,
            followingCount = 0L,
            likesCount = 0L,
            viewsCount = 0L,
            videosCount = 0L
        )
        _currentUser.value = updatedUser
        _creatorsMap.update { it + (cleanUsername to updatedUser) }
        _isLoggedIn.value = true
        saveToStorage(context)
        repositoryScope.launch {
            RowhaniyatCloudService.saveUserProfile(updatedUser)
        }
        return updatedUser
    }

    fun logout(context: android.content.Context) {
        _isLoggedIn.value = false
        val prefs = context.getSharedPreferences("rowhaniyat_user_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_logged_in", false).apply()
    }

    fun deleteAccount(context: android.content.Context) {
        val user = _currentUser.value
        _isLoggedIn.value = false
        val prefs = context.getSharedPreferences("rowhaniyat_user_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        // remove user's videos from feeds
        _videosFeed.update { it.filter { v -> v.authorHandle != user.username } }
        _shortsFeed.update { it.filter { s -> s.authorHandle != user.username } }
        _creatorsMap.update { it - user.username }
        repositoryScope.launch {
            try {
                database?.userDao()?.deleteUserByHandle(user.username)
            } catch (_: Exception) {}
        }
    }

    fun signInWithPhone(
        phoneNumber: String,
        context: android.content.Context
    ): UserProfile {
        setGuestMode(false)
        val cleanPhone = phoneNumber.replace("+", "").replace(" ", "").trim()
        val cleanUsername = "user_${cleanPhone.takeLast(6)}"
        val userId = "phone_${cleanPhone.hashCode().toUInt()}"
        val updatedUser = _currentUser.value.copy(
            id = userId,
            name = "مستخدم ($cleanPhone)",
            username = cleanUsername,
            bio = "مستخدم مسجل برقم الهاتف في تطبيق روحانيات 🌙"
        )
        _currentUser.value = updatedUser
        saveToStorage(context)
        repositoryScope.launch {
            try {
                database?.userDao()?.insertUser(
                    com.example.data.local.entity.UserEntity(
                        handle = cleanUsername,
                        id = userId,
                        name = updatedUser.name,
                        bio = updatedUser.bio,
                        avatarUrl = updatedUser.avatarUrl,
                        coverUrl = updatedUser.coverUrl,
                        followersCount = updatedUser.followersCount,
                        followingCount = updatedUser.followingCount,
                        likesCount = updatedUser.likesCount,
                        viewsCount = updatedUser.viewsCount,
                        isFollowing = false,
                        country = updatedUser.country,
                        websiteUrl = updatedUser.websiteUrl,
                        email = "",
                        isCurrentUser = true
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return updatedUser
    }

    fun toggleFollowUser(context: android.content.Context) {
        _currentUser.update {
            val nextState = !it.isFollowing
            val nextFollowers = if (nextState) it.followersCount + 1 else (it.followersCount - 1).coerceAtLeast(0)
            it.copy(isFollowing = nextState, followersCount = nextFollowers)
        }
        saveToStorage(context)
    }

    fun getUser(handle: String): UserProfile {
        val cleanHandle = handle.removePrefix("@").trim()
        if (cleanHandle == _currentUser.value.username || cleanHandle == "me" || cleanHandle.isBlank()) {
            return _currentUser.value
        }
        return _creatorsMap.value[cleanHandle] ?: UserProfile(
            id = "user_$cleanHandle",
            name = cleanHandle,
            username = cleanHandle,
            bio = "ناشر ومحب للمحتوى الإيماني في تطبيق روحانيات 🌙",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
            coverUrl = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=800",
            followersCount = 1200,
            followingCount = 45,
            likesCount = 15000,
            viewsCount = 60000,
            videosCount = 5,
            isFollowing = false
        )
    }

    fun isAuthorVerified(handle: String): Boolean {
        return getUser(handle).isVerified
    }

    fun toggleFollowCreator(handle: String, context: android.content.Context): Boolean {
        val cleanHandle = handle.removePrefix("@").trim()
        if (cleanHandle == _currentUser.value.username) {
            toggleFollowUser(context)
            return _currentUser.value.isFollowing
        }

        val user = getUser(cleanHandle)
        val nextIsFollowing = !user.isFollowing
        val nextFollowers = if (nextIsFollowing) user.followersCount + 1 else (user.followersCount - 1).coerceAtLeast(0)

        // Update in state
        val updatedUser = user.copy(
            isFollowing = nextIsFollowing,
            followersCount = nextFollowers
        )
        _creatorsMap.update { current ->
            current + (cleanHandle to updatedUser)
        }

        // Update current user following count
        _currentUser.update { current ->
            val newFollowingCount = if (nextIsFollowing) current.followingCount + 1 else (current.followingCount - 1).coerceAtLeast(0)
            current.copy(followingCount = newFollowingCount)
        }
        saveToStorage(context)

        // Persist in Room database
        repositoryScope.launch {
            try {
                database?.userDao()?.updateFollowState(cleanHandle, nextFollowers, nextIsFollowing)
                if (nextIsFollowing) {
                    database?.followDao()?.insertFollow(
                        FollowEntity(
                            followerHandle = _currentUser.value.username,
                            followingHandle = cleanHandle
                        )
                    )
                } else {
                    database?.followDao()?.deleteFollow(_currentUser.value.username, cleanHandle)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return nextIsFollowing
    }

    fun getFollowers(handle: String): List<UserProfile> {
        val cleanHandle = handle.removePrefix("@").trim()
        val targetUser = getUser(cleanHandle)
        val followersList = mutableListOf<UserProfile>()
        val currentUserVal = _currentUser.value

        // If current user is following this creator, current user appears in their followers list
        if (targetUser.isFollowing) {
            followersList.add(currentUserVal)
        }

        // Add other active community creators as followers for realistic display
        _creatorsMap.value.values.filter { it.username != cleanHandle }.take(7).forEach {
            followersList.add(it)
        }

        return followersList.distinctBy { it.username }
    }

    fun getFollowing(handle: String): List<UserProfile> {
        val cleanHandle = handle.removePrefix("@").trim()
        if (cleanHandle == _currentUser.value.username) {
            // Return accounts currently followed by user
            val followedCreators = _creatorsMap.value.values.filter { it.isFollowing }
            if (followedCreators.isNotEmpty()) return followedCreators
        }
        // General list of accounts this creator follows
        return _creatorsMap.value.values.filter { it.username != cleanHandle }.take(6)
    }

    fun resetAccount(context: android.content.Context) {
        val prefs = context.getSharedPreferences("rowhaniyat_user_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _currentUser.value = UserProfile(
            id = "current_user_1",
            name = "مستخدم روحانيات",
            username = "user_${System.currentTimeMillis() % 10000}",
            bio = "حساب جديد في تطبيق روحانيات 🌙",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
            coverUrl = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=800&auto=format&fit=crop&q=80",
            followersCount = 0,
            followingCount = 0,
            likesCount = 0,
            viewsCount = 0
        )
    }

    // User Posts
    data class UserPost(
        val id: String,
        val text: String,
        val timestamp: Long = System.currentTimeMillis(),
        val formattedTime: String = "الآن",
        val likesCount: Long = 0,
        val isLiked: Boolean = false
    )

    private val _userPosts = MutableStateFlow<List<UserPost>>(
        listOf(
            UserPost(
                id = "post_1",
                text = "«ألا بذكر الله تطمئن القلوب».. اللهم اجعل ألسنتنا عامرة بذكرك، وقلوبنا بخشيتك، وأرواحنا بطاعتك. 🤍",
                formattedTime = "قبل ساعتين",
                likesCount = 42
            ),
            UserPost(
                id = "post_2",
                text = "نصيحة لنفسي وإخواني: لا تفوتوا قراءة سورة الكهف اليوم ونور ما بين الجمعتين. 📖✨",
                formattedTime = "أمس",
                likesCount = 89
            )
        )
    )
    val userPosts: StateFlow<List<UserPost>> = _userPosts.asStateFlow()

    fun publishPost(text: String) {
        val newPost = UserPost(
            id = "post_${System.currentTimeMillis()}",
            text = text,
            formattedTime = "الآن",
            likesCount = 0
        )
        _userPosts.update { listOf(newPost) + it }
    }

    // Daily Ayah
    val dailyAyah = Ayah(
        number = 286,
        surahNumber = 2,
        textArabic = "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا ۚ لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ",
        translation = "Allah does not burden a soul beyond that it can bear.",
        tafseer = "تذكير يومي بأن الله أرحم بك من نفسك، ولن يحملك فوق طاقتك، فاستبشر وتوكل عليه.",
        audioUrl = "https://everyayah.com/data/Alafasy_128kbps/002286.mp3"
    )

    // Reciters
    val recitersList = listOf(
        Reciter(
            id = "afs",
            name = "مشاري راشد العفاسي",
            style = "مرتل بقراءة حفص",
            nationality = "دولة الكويت",
            bio = "قارئ القرآن الكريم وإمام المسجد الكبير بدولة الكويت، يتميز بصوت عذب وشجي."
        ),
        Reciter(
            id = "basit",
            name = "عبد الباسط عبد الصمد",
            style = "تلاوة مجودة ومرتلة",
            nationality = "جمهورية مصر العربية",
            bio = "صوت مكة الخالد وأحد أشهر قراء العالم الإسلامي في القرن العشرين."
        ),
        Reciter(
            id = "minshawi",
            name = "محمد صديق المنشاوي",
            style = "مرتل بخشوع استثنائي",
            nationality = "جمهورية مصر العربية",
            bio = "الملقب بـ 'الصوت الباكي'، من أعلام دولة التلاوة المصرية وأكثرهم تأثيراً."
        ),
        Reciter(
            id = "husary",
            name = "محمود خليل الحصري",
            style = "المرتل المتقن لأحكام التجويد",
            nationality = "جمهورية مصر العربية",
            bio = "شيخ عموم المقارئ المصرية الأسبق، رائد تسجيل المصحف المرتل بروايات متعددة."
        ),
        Reciter(
            id = "sudais",
            name = "عبد الرحمن السديس",
            style = "مرتل بنبرة مكية مميزة",
            nationality = "المملكة العربية السعودية",
            bio = "إمام وخطيب المسجد الحرام ورئيس الشؤون الدينية بالمسجد الحرام والمسجد النبوي."
        ),
        Reciter(
            id = "ghamdi",
            name = "سعد الغامدي",
            style = "مرتل هادئ ومريح",
            nationality = "المملكة العربية السعودية",
            bio = "إمام وقارئ سعودي بارز يتميز بصوته الهادئ المتدبر."
        )
    )

    // Audio Recitations Library
    private val _audioRecitations = MutableStateFlow(
        listOf(
            AudioRecitation(
                id = "rec_1",
                title = "سورة الفاتحة - تلاوة خاشعة",
                surahNumber = 1,
                surahName = "الفاتحة",
                reciterName = "مشاري راشد العفاسي",
                reciterId = "afs",
                audioUrl = "https://server8.mp3quran.net/afs/001.mp3",
                durationFormatted = "0:45",
                durationMs = 45000,
                isFavorite = true,
                playsCount = 384000
            ),
            AudioRecitation(
                id = "rec_2",
                title = "سورة الملك - تلاوة هادئة قبل النوم",
                surahNumber = 67,
                surahName = "الملك",
                reciterName = "محمد صديق المنشاوي",
                reciterId = "minshawi",
                audioUrl = "https://server10.mp3quran.net/minsh/067.mp3",
                durationFormatted = "9:30",
                durationMs = 570000,
                isFavorite = true,
                playsCount = 512000
            ),
            AudioRecitation(
                id = "rec_3",
                title = "سورة الكهف - نور ما بين الجمعتين",
                surahNumber = 18,
                surahName = "الكهف",
                reciterName = "عبد الرحمن السديس",
                reciterId = "sudais",
                audioUrl = "https://server11.mp3quran.net/sds/018.mp3",
                durationFormatted = "24:15",
                durationMs = 1455000,
                isFavorite = false,
                playsCount = 890000
            ),
            AudioRecitation(
                id = "rec_4",
                title = "سورة يس - قلب القرآن الكريم",
                surahNumber = 36,
                surahName = "يس",
                reciterName = "عبد الباسط عبد الصمد",
                reciterId = "basit",
                audioUrl = "https://server7.mp3quran.net/basit/036.mp3",
                durationFormatted = "18:40",
                durationMs = 1120000,
                isFavorite = true,
                playsCount = 420000
            ),
            AudioRecitation(
                id = "rec_5",
                title = "سورة الرحمن - عروس القرآن",
                surahNumber = 55,
                surahName = "الرحمن",
                reciterName = "مشاري راشد العفاسي",
                reciterId = "afs",
                audioUrl = "https://server8.mp3quran.net/afs/055.mp3",
                durationFormatted = "14:20",
                durationMs = 860000,
                isFavorite = false,
                playsCount = 670000
            ),
            AudioRecitation(
                id = "rec_6",
                title = "سورة الإخلاص والمعوذتين",
                surahNumber = 112,
                surahName = "الإخلاص والتحصين",
                reciterName = "سعد الغامدي",
                reciterId = "ghamdi",
                audioUrl = "https://server7.mp3quran.net/s_gmd/112.mp3",
                durationFormatted = "2:10",
                durationMs = 130000,
                isFavorite = true,
                playsCount = 295000
            )
        )
    )
    val audioRecitations: StateFlow<List<AudioRecitation>> = _audioRecitations.asStateFlow()

    fun toggleRecitationFavorite(id: String) {
        _audioRecitations.update { list ->
            list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it }
        }
    }

    // 114 Surahs Data
    val surahsList = listOf(
        Surah(1, "الفاتحة", "Al-Fatihah", "مكية", 7, 1, "أم الكتاب والسبع المثاني", listOf(
            Ayah(1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "أبدأ قراءتي مستعيناً بالله"),
            Ayah(2, 1, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "All praise is due to Allah, Lord of the worlds.", "الثناء التام لله تعالى مالك كل المخلوقات"),
            Ayah(3, 1, "الرَّحْمَٰنِ الرَّحِيمِ", "The Entirely Merciful, the Especially Merciful.", "ذو الرحمة الواسعة العظيمة"),
            Ayah(4, 1, "مَالِكِ يَوْمِ الدِّينِ", "Sovereign of the Day of Recompense.", "المتصرف وحده في يوم الجزاء والحساب"),
            Ayah(5, 1, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help.", "نخصك وحدك بالعبادة والاستعانة"),
            Ayah(6, 1, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Guide us to the straight path.", "وفقنا وأرشدنا إلى الطريق القويم"),
            Ayah(7, 1, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "The path of those upon whom You have bestowed favor, not of those who have evoked anger or of those who are astray.", "طريق الأنبياء والصديقين والشهداء والصالحين")
        )),
        Surah(2, "البقرة", "Al-Baqarah", "مدنية", 286, 2, "أطول سور القرآن الكريم وفيها آية الكرسي", listOf(
            Ayah(1, 2, "الم", "Alif, Lam, Meem.", "حروف مقطعة تدل على إعجاز القرآن"),
            Ayah(2, 2, "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ", "This is the Book about which there is no doubt, a guidance for those conscious of Allah.", "هذا القرآن حق لا شك فيه يهدي القلوب النقية"),
            Ayah(3, 2, "الَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ وَيُقِيمُونَ الصَّلَاةَ وَمِمَّا رَزَقْنَاهُمْ يُنفِقُونَ", "Who believe in the unseen, establish prayer, and spend out of what We have provided for them.", "المؤمنون بالغيبيات المقيمون لفرائض الله"),
            Ayah(255, 2, "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ", "Allah - there is no deity except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep.", "آية الكرسي أعظم آية في كتاب الله")
        )),
        Surah(3, "آل عمران", "Ali 'Imran", "مدنية", 200, 50, "سورة مباركة تحاج عن صاحبها يوم القيامة"),
        Surah(4, "النساء", "An-Nisa", "مدنية", 176, 77, "سورة تنظم الحقوق الأسرية والمجتمعية"),
        Surah(5, "المائدة", "Al-Ma'idah", "مدنية", 120, 106, "سورة العقود والأحكام"),
        Surah(6, "الأنعام", "Al-An'am", "مكية", 165, 128, "سورة التوحيد والدلائل الكونية"),
        Surah(7, "الأعراف", "Al-A'raf", "مكية", 206, 151, "سورة قصص الأنبياء مع أممهم"),
        Surah(8, "الأنفال", "Al-Anfal", "مدنية", 75, 177, "سورة معركة بدر والتوكل على الله"),
        Surah(9, "التوبة", "At-Tawbah", "مدنية", 129, 187, "سورة البراءة وتوبة الله على المؤمنين"),
        Surah(10, "يونس", "Yunus", "مكية", 109, 208, "سورة نبي الله يونس وقوة الإيمان"),
        Surah(11, "هود", "Hud", "مكية", 123, 221, "سورة استقامة المؤمنين ومواقف الرسل"),
        Surah(12, "يوسف", "Yusuf", "مكية", 111, 235, "أحسن القصص في الصبر والفرج", listOf(
            Ayah(1, 12, "الر ۚ تِلْكَ آيَاتُ الْكِتَابِ الْمُبِينِ", "Alif, Lam, Ra. These are the verses of the clear Book.", "آيات القرآن الواضحة الدالة على الحق"),
            Ayah(86, 12, "قَالَ إِنَّمَا أَشْكُو بَثِّي وَحُزْنِي إِلَى اللَّهِ وَأَعْلَمُ مِنَ اللَّهِ مَا لَا تَعْلَمُونَ", "He said, 'I only complain of my suffering and my grief to Allah, and I know from Allah that which you do not know.'", "مناجاة يعقوب عليه السلام وتوكله على الله")
        )),
        Surah(13, "الرعد", "Ar-Ra'd", "مدنية", 43, 249, "سورة التسبيح وطمأنينة القلوب بذكر الله"),
        Surah(14, "إبراهيم", "Ibrahim", "مكية", 52, 255, "سورة دعاء الخليل ونعمة الشكر"),
        Surah(15, "الحجر", "Al-Hijr", "مكية", 99, 262, "حفظ الذكر والقرآن الكريم"),
        Surah(16, "النحل", "An-Nahl", "مكية", 128, 267, "سورة النعم المتعددة وشكر المنعم"),
        Surah(17, "الإسراء", "Al-Isra", "مكية", 111, 282, "سورة معجزة الإسراء والمعراج وقيمة الوالدين"),
        Surah(18, "الكهف", "Al-Kahf", "مكية", 110, 293, "عصمة من فتنة الدجال ونور يوم الجمعة", listOf(
            Ayah(1, 18, "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجًا", "Praise be to Allah, who has sent down upon His Servant the Book and has not made therein any deviance.", "الثناء لله على نعمة إنزال القرآن"),
            Ayah(10, 18, "إِذْ أَوَى الْفِتْيَةُ إِلَى الْكَهْفِ فَقَالُوا رَبَّنَا آتِنَا مِن لَّدُنكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا", "When the youths retreated to the cave and said, 'Our Lord, grant us from Yourself mercy and prepare for us from our affair right guidance.'", "دعاء شباب الكهف في الالتجاء إلى الله")
        )),
        Surah(19, "مريم", "Maryam", "مكية", 98, 305, "سورة رحمة الله بعباده الصالحين"),
        Surah(20, "طه", "Taha", "مكية", 135, 312, "ما أنزلنا عليك القرآن لتشقى"),
        Surah(21, "الأنبياء", "Al-Anbiya", "مكية", 112, 322, "سورة استجابة دعوات الأنبياء"),
        Surah(22, "الحج", "Al-Hajj", "مدنية", 78, 332, "سورة الركن الخامس والتعظيم لشعائر الله"),
        Surah(23, "المؤمنون", "Al-Mu'minun", "مكية", 118, 342, "قد أفلح المؤمنون وصفات الفلاح"),
        Surah(24, "النور", "An-Nur", "مدنية", 64, 350, "الله نور السماوات والأرض"),
        Surah(25, "الفرقان", "Al-Furqan", "مكية", 77, 359, "صفات عباد الرحمن الذين يمشون هونا"),
        Surah(26, "الشعراء", "Ash-Shu'ara", "مكية", 227, 367, "سورة التنزيل وحجج الأنبياء"),
        Surah(27, "النمل", "An-Naml", "مكية", 93, 377, "سليمان والهدية والتوحيد"),
        Surah(28, "القصص", "Al-Qasas", "مكية", 88, 385, "قصة موسى عليه السلام وربط القلوب"),
        Surah(29, "العنكبوت", "Al-'Ankabut", "مكية", 69, 396, "أحسب الناس أن يتركوا أن يقولوا آمنا"),
        Surah(30, "الروم", "Ar-Rum", "مكية", 60, 404, "آيات الآفاق والنفس والزواج السكينة"),
        Surah(36, "يس", "Ya-Sin", "مكية", 83, 440, "سورة يس والبعث والنشور"),
        Surah(55, "الرحمن", "Ar-Rahman", "مدنية", 78, 531, "فبأي آلاء ربكما تكذبان"),
        Surah(56, "الواقعة", "Al-Waqi'ah", "مكية", 96, 534, "سورة الغنى ودفع الفاقة"),
        Surah(67, "الملك", "Al-Mulk", "مكية", 30, 562, "المنجية من عذاب القبر", listOf(
            Ayah(1, 67, "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ", "Blessed is He in whose hand is dominion, and He is over all things competent.", "تعاظم شأن الله وتبارك ملكه"),
            Ayah(2, 67, "الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا ۚ وَهُوَ الْعَزِيزُ الْغَفُورُ", "He who created death and life to test you as to which of you is best in deed - and He is the Exalted in Might, the Forgiving.", "الغاية من الحياة والابتلاء بالعمل الصالح")
        )),
        Surah(112, "الإخلاص", "Al-Ikhlas", "مكية", 4, 604, "تعدل ثلث القرآن الكريم", listOf(
            Ayah(1, 112, "قُلْ هُوَ اللَّهُ أَحَدٌ", "Say, 'He is Allah, [who is] One.'", "قل يا محمد: الله واحد لا شريك له"),
            Ayah(2, 112, "اللَّهُ الصَّمَدُ", "Allah, the Eternal Refuge.", "السيد الذي تصمد إليه الخلائق في حوائجها"),
            Ayah(3, 112, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "He neither begets nor is born.", "لم يصدر عنه ولد ولم ينشأ عن والد"),
            Ayah(4, 112, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "Nor is there to Him any equivalent.", "ليس له شبيه ولا نظير ولا مثيل")
        )),
        Surah(113, "الفلق", "Al-Falaq", "مكية", 5, 604, "الاستعاذة برب الصبح من شرور المخلوقات", listOf(
            Ayah(1, 113, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "Say, 'I seek refuge in the Lord of daybreak.'", "استعيذ برب الفلق والصباح"),
            Ayah(2, 113, "مِن شَرِّ مَا خَلَقَ", "From the evil of that which He created.", "من شر جميع ما خلق الله"),
            Ayah(3, 113, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "And from the evil of darkness when it settles.", "من شر الليل إذا دخل بظلامه"),
            Ayah(4, 113, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "And from the evil of the blowers in knots.", "من شر السواحر والحاسدات"),
            Ayah(5, 113, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "And from the evil of an envier when he envies.", "من شر كل عين وحسد عند صدوره")
        )),
        Surah(114, "الناس", "An-Nas", "مكية", 6, 604, "الاستعاذة بملك الناس من شر الوسواس الخناس", listOf(
            Ayah(1, 114, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "Say, 'I seek refuge in the Lord of mankind.'", "الالتجاء إلى مدبر أمور الخلق"),
            Ayah(2, 114, "مَلِكِ النَّاسِ", "The Sovereign of mankind.", "المالك الحقيقي لجميع عباده"),
            Ayah(3, 114, "إِلَٰهِ النَّاسِ", "The God of mankind.", "المعبود الحق لا إله غيره"),
            Ayah(4, 114, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "From the evil of the retreating whisperer.", "من وسوسة الشيطان الذي يختفي عند ذكر الله"),
            Ayah(5, 114, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "Who whispers into the breasts of mankind.", "يبث الأوهام والشكوك في القلوب"),
            Ayah(6, 114, "مِنَ الْجِنَّةِ وَالنَّاسِ", "From among the jinn and mankind.", "سواء كان وسواساً من شياطين الإنس أو الجن")
        ))
    )

    // Last reading bookmark
    private val _lastReadBookmark = MutableStateFlow("سورة البقرة - آية ٢٥٥ (آية الكرسي)")
    val lastReadBookmark: StateFlow<String> = _lastReadBookmark.asStateFlow()

    fun saveReadingBookmark(surahName: String, ayahNumber: Int) {
        _lastReadBookmark.value = "سورة $surahName - آية $ayahNumber"
    }

    // Azkar List
    val azkarList = listOf(
        ZikrItem(
            id = "zk_1",
            text = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَـهَ إِلاَّ اللهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            countTarget = 1,
            virtue = "من قالها حين يصبح وحين يمسي كفته من كل سوء وأثنى على ربه.",
            reference = "رواه مسلم",
            category = ZikrCategory.MORNING
        ),
        ZikrItem(
            id = "zk_2",
            text = "اللَّهُمَّ أَنْتَ رَبِّي لا إِلَهَ إِلا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لا يَغْفِرُ الذُّنُوبَ إِلا أَنْتَ.",
            countTarget = 1,
            virtue = "سيد الاستغفار: من قالها موقناً بها فمات من يومه أو ليلته دخل الجنة.",
            reference = "صحيح البخاري",
            category = ZikrCategory.MORNING
        ),
        ZikrItem(
            id = "zk_3",
            text = "بِسْمِ اللَّهِ الَّذِي لاَ يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الأَرْضِ وَلاَ فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
            countTarget = 3,
            virtue = "لم يضره شيء حتى يمسي أو يصبح.",
            reference = "رواه الترمذي وأبو داود",
            category = ZikrCategory.MORNING
        ),
        ZikrItem(
            id = "zk_4",
            text = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَـهَ إِلاَّ اللهُ وَحْدَهُ لاَ شَرِيكَ لَهُ.",
            countTarget = 1,
            virtue = "تحصين المساء وتفويض الأمر لخالق الأكوان.",
            reference = "صحيح مسلم",
            category = ZikrCategory.EVENING
        ),
        ZikrItem(
            id = "zk_5",
            text = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.",
            countTarget = 3,
            virtue = "لم تصبه حمة ولا ضرر في تلك الليلة.",
            reference = "صحيح مسلم",
            category = ZikrCategory.EVENING
        ),
        ZikrItem(
            id = "zk_6",
            text = "بِاسْمِكَ رَبِّي وَضَعْتُ جَنْبِي، وَبِكَ أَرْفَعُهُ، فَإِنْ أَمْسَكْتَ نَفْسِي فَارْحَمْهَا، وَإِنْ أَرْسَلْتَهَا فَاحْفَظْهَا بِمَا تَحْفَظُ بِهِ عِبَادَكَ الصَّالِحِينَ.",
            countTarget = 1,
            virtue = "دعاء النوم المبارك وحفظ النفس عند النوم.",
            reference = "متفق عليه",
            category = ZikrCategory.SLEEP
        ),
        ZikrItem(
            id = "zk_7",
            text = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ.",
            countTarget = 1,
            virtue = "أول ما يلهج به اللسان عند الاستيقاظ شكراً للواهب.",
            reference = "صحيح البخاري",
            category = ZikrCategory.WAKING
        ),
        ZikrItem(
            id = "zk_8",
            text = "أَسْتَغْفِرُ اللَّهَ (ثلاثاً)، اللَّهُمَّ أَنْتَ السَّلاَمُ، وَمِنْكَ السَّلاَمُ، تَبَارَكْتَ يَا ذَا الْجَلاَلِ وَالإِكْرَامِ.",
            countTarget = 1,
            virtue = "سنة المصطفى ﷺ دبر كل صلاة مكتوبة.",
            reference = "صحيح مسلم",
            category = ZikrCategory.AFTER_PRAYER
        ),
        ZikrItem(
            id = "zk_9",
            text = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَـٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنقَلِبُونَ.",
            countTarget = 1,
            virtue = "دعاء ركوب الدابة والمركبة والسفر.",
            reference = "سورة الزخرف / رواه مسلم",
            category = ZikrCategory.TRAVEL
        ),
        ZikrItem(
            id = "zk_10",
            text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ.",
            countTarget = 100,
            virtue = "كلمتان خفيفتان على اللسان، ثقيلتان في الميزان، حبيبتان إلى الرحمن.",
            reference = "صحيح البخاري",
            category = ZikrCategory.MISC
        )
    )

    // Duas List
    private val _duasList = MutableStateFlow(
        listOf(
            DuaItem(
                id = "dua_1",
                title = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً",
                textArabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
                source = "سورة البقرة: ٢٠١ (أكثر دعاء النبي ﷺ)",
                category = DuaCategory.QURANIC,
                isFavorite = true
            ),
            DuaItem(
                id = "dua_2",
                title = "رَبِّ اشْرَحْ لِي صَدْرِي",
                textArabic = "رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي وَاحْلُلْ عُقْدَةً مِّن لِّسَانِي يَفْقَهُوا قَوْلِي",
                source = "سورة طه: ٢٥-٢٨ (دعاء موسى عليه السلام)",
                category = DuaCategory.PROPHETIC,
                isFavorite = true
            ),
            DuaItem(
                id = "dua_3",
                title = "اللَّهُمَّ إِنِّي أَسْأَلُكَ رِزْقًا طَيِّبًا",
                textArabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا",
                source = "سنن ابن ماجه (يقال بعد صلاة الفجر)",
                category = DuaCategory.PROVISION,
                isFavorite = false
            ),
            DuaItem(
                id = "dua_4",
                title = "دعاء تفريج الهم والكرب",
                textArabic = "لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
                source = "دعوة ذي النون يونس عليه السلام في بطن الحوت",
                category = DuaCategory.RELIEF,
                isFavorite = true
            ),
            DuaItem(
                id = "dua_5",
                title = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ",
                textArabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْجُبْنِ وَالْبُخْلِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
                source = "صحيح البخاري",
                category = DuaCategory.RELIEF,
                isFavorite = true
            ),
            DuaItem(
                id = "dua_6",
                title = "دعاء السفر والمسير",
                textArabic = "اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى، وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَذَا وَاطْوِ عَنَّا بُعْدَهُ",
                source = "صحيح مسلم",
                category = DuaCategory.TRAVEL,
                isFavorite = false
            ),
            DuaItem(
                id = "dua_7",
                title = "اللَّهُمَّ عَافِنِي فِي بَدَنِي",
                textArabic = "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَهَ إِلَّا أَنْتَ",
                source = "سنن أبي داود (أدعية الصباح والمساء)",
                category = DuaCategory.MORNING_EVENING,
                isFavorite = true
            )
        )
    )
    val duasList: StateFlow<List<DuaItem>> = _duasList.asStateFlow()

    fun toggleDuaFavorite(id: String) {
        _duasList.update { list ->
            list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it }
        }
    }

    // Videos Feed (16:9)
    private val _videosFeed = MutableStateFlow(
        listOf(
            VideoItem(
                id = "vid_1",
                title = "كيف تجعل قلبك معلقاً بالقرآن وتتلذذ بالصلاة؟ | خواطر إيمانية",
                authorName = "د. عمر عبد الكافي",
                authorHandle = "omarabdelkafy",
                authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
                thumbnailUrl = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=800&auto=format&fit=crop&q=80",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                viewsCount = 184000,
                timeAgo = "منذ يومين",
                durationFormatted = "14:20",
                likesCount = 14200,
                commentsCount = 680,
                repostsCount = 1200,
                isLiked = false,
                isSaved = true,
                isReposted = false,
                hashtags = listOf("#خواطر", "#تدبر", "#صلاة", "#خشوع"),
                description = "جلسة إيمانية هادئة نتحدث فيها عن أسباب جفاء القلب وكيف نصل إلى الخشوع الحقيقي في الصلاة وتدبر آيات كتاب الله العزيز."
            ),
            VideoItem(
                id = "vid_2",
                title = "تفسير سورة الفلق والتحصين النبوي من العين والحسد",
                authorName = "الشيخ عثمان الخميس",
                authorHandle = "othmanalkhamees",
                authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
                thumbnailUrl = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=800&auto=format&fit=crop&q=80",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                viewsCount = 295000,
                timeAgo = "منذ ٤ أيام",
                durationFormatted = "22:15",
                likesCount = 28900,
                commentsCount = 1450,
                repostsCount = 3400,
                isLiked = true,
                isSaved = false,
                isReposted = true,
                hashtags = listOf("#فقه", "#تفسير", "#التحصين", "#المعوذات"),
                description = "شرح وافٍ لمعاني سورة الفلق وكيف كان النبي ﷺ يرقي نفسه وأهل بيته ويحصنهم من شرور الخلق والوسواس."
            ),
            VideoItem(
                id = "vid_3",
                title = "تلاوة تبكي القلوب لسورة الكهف من رحاب الحرم المكي",
                authorName = "تسجيلات الحرمين الشريفين",
                authorHandle = "haramain_recitations",
                authorAvatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&auto=format&fit=crop&q=80",
                thumbnailUrl = "https://images.unsplash.com/photo-1584551246679-0daf3d275d0f?w=800&auto=format&fit=crop&q=80",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                viewsCount = 512000,
                timeAgo = "منذ أسبوع",
                durationFormatted = "31:40",
                likesCount = 64000,
                commentsCount = 3100,
                repostsCount = 8900,
                isLiked = true,
                isSaved = true,
                isReposted = false,
                hashtags = listOf("#تلاوة", "#الحرم_المكي", "#سورة_الكهف", "#قرآن"),
                description = "تلاوة مهيبة من صلاة الجمعة بالمسجد الحرام بصوت الشيخ عبد الرحمن السديس."
            )
        )
    )
    val videosFeed: StateFlow<List<VideoItem>> = _videosFeed.asStateFlow()

    fun toggleVideoLike(id: String) {
        var updatedLiked = false
        var delta = 0L
        _videosFeed.update { list ->
            list.map {
                if (it.id == id) {
                    val newLiked = !it.isLiked
                    updatedLiked = newLiked
                    delta = if (newLiked) 1L else -1L
                    it.copy(
                        isLiked = newLiked,
                        likesCount = it.likesCount + delta
                    )
                } else it
            }
        }
        recalculateFeatured()
        repositoryScope.launch {
            database?.videoDao()?.updateLike(id, delta, updatedLiked)
        }
    }

    fun toggleVideoSave(id: String) {
        var updatedSaved = false
        _videosFeed.update { list ->
            list.map {
                if (it.id == id) {
                    updatedSaved = !it.isSaved
                    it.copy(isSaved = updatedSaved)
                } else it
            }
        }
        repositoryScope.launch {
            database?.videoDao()?.updateSave(id, updatedSaved)
        }
    }

    fun toggleVideoRepost(id: String) {
        var updatedRepost = false
        var delta = 0L
        _videosFeed.update { list ->
            list.map {
                if (it.id == id) {
                    updatedRepost = !it.isReposted
                    delta = if (updatedRepost) 1L else -1L
                    it.copy(
                        isReposted = updatedRepost,
                        repostsCount = it.repostsCount + delta
                    )
                } else it
            }
        }
        recalculateFeatured()
        repositoryScope.launch {
            database?.videoDao()?.updateRepost(id, delta, updatedRepost)
        }
    }

    fun recordVideoView(id: String) {
        if (sessionViewedVideos.add(id)) {
            _videosFeed.update { list ->
                list.map { if (it.id == id) it.copy(viewsCount = it.viewsCount + 1) else it }
            }
            _shortsFeed.update { list ->
                list.map { if (it.id == id) it.copy(viewsCount = it.viewsCount + 1) else it }
            }
            recalculateFeatured()
            repositoryScope.launch {
                database?.videoDao()?.incrementViews(id)
            }
        }
    }

    fun saveWatchPosition(id: String, positionMs: Long) {
        repositoryScope.launch {
            database?.videoDao()?.updateWatchPosition(id, positionMs)
        }
    }

    fun updateVideo(id: String, title: String, description: String, thumbnailUrl: String = "") {
        _videosFeed.update { list ->
            list.map {
                if (it.id == id) it.copy(
                    title = title,
                    description = description,
                    thumbnailUrl = if (thumbnailUrl.isNotBlank()) thumbnailUrl else it.thumbnailUrl
                ) else it
            }
        }
        recalculateFeatured()
        repositoryScope.launch {
            database?.videoDao()?.updateVideoMetadata(id, title, description, thumbnailUrl)
        }
    }

    fun deleteVideo(id: String) {
        _videosFeed.update { list -> list.filter { it.id != id } }
        _shortsFeed.update { list -> list.filter { it.id != id } }
        recalculateFeatured()
        repositoryScope.launch {
            database?.videoDao()?.deleteVideo(id)
        }
    }

    // Shorts Feed (9:16 vertical TikTok/YouTube Shorts style)
    private val _shortsFeed = MutableStateFlow(
        listOf(
            ShortItem(
                id = "short_1",
                title = "«وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ» 🤍 رسالة لكل قلب متعب اليوم",
                authorName = "روحانيات ومواعظ",
                authorHandle = "rowhaniyat_official",
                authorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=800&auto=format&fit=crop&q=80",
                likesCount = 89400,
                commentsCount = 2100,
                repostsCount = 9400,
                sharesCount = 14200,
                isLiked = true,
                isSaved = true,
                isReposted = false,
                isFollowing = true,
                hashtags = listOf("#طمأنينة", "#قرآن", "#Shorts", "#روحانيات"),
                soundTitle = "تلاوة هادئة ولسوف يعطيك ربك فترضى",
                soundAuthor = "مشاري العفاسي",
                viewsCount = 420000
            ),
            ShortItem(
                id = "short_2",
                title = "دعاء يزيل عنك هموم الدنيا في دقيقة واحدة ✨ ردده الآن",
                authorName = "قبسات من نور",
                authorHandle = "qabasat_noor",
                authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=800&auto=format&fit=crop&q=80",
                likesCount = 45600,
                commentsCount = 1320,
                repostsCount = 4800,
                sharesCount = 6700,
                isLiked = false,
                isSaved = false,
                isReposted = false,
                isFollowing = false,
                hashtags = listOf("#أدعية", "#تفريج_الهم", "#دعاء", "#استغفار"),
                soundTitle = "مؤثرات هادئة - قبسات الإيمان",
                soundAuthor = "قبسات من نور",
                viewsCount = 198000
            ),
            ShortItem(
                id = "short_3",
                title = "كيف تحافظ على صلاة الفجر في وقتها؟ ٣ خطوات عملية ستغير حياتك 🌅",
                authorName = "م. خالد البلوشي",
                authorHandle = "khaled_balushi",
                authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&auto=format&fit=crop&q=80",
                likesCount = 71200,
                commentsCount = 3400,
                repostsCount = 8100,
                sharesCount = 11900,
                isLiked = true,
                isSaved = true,
                isReposted = true,
                isFollowing = true,
                hashtags = listOf("#صلاة_الفجر", "#نصائح", "#بركة_الصباح"),
                soundTitle = "أذان الفجر بصوت الشيخ علي ملا",
                soundAuthor = "مؤذنو الحرم المكي",
                viewsCount = 340000
            ),
            ShortItem(
                id = "short_4",
                title = "من أعظم أسرار التسبيح: مئة تسبيحة تغفر ذنوبك وإن كانت مثل زبد البحر 📿",
                authorName = "ذاكرون وذاكرات",
                authorHandle = "dhikr_community",
                authorAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1519817650390-64a93db51149?w=800&auto=format&fit=crop&q=80",
                likesCount = 112000,
                commentsCount = 5600,
                repostsCount = 19500,
                sharesCount = 28000,
                isLiked = false,
                isSaved = false,
                isReposted = false,
                isFollowing = false,
                hashtags = listOf("#تسبيح", "#مسبحة", "#أذكار", "#أجر_عظيم"),
                soundTitle = "تسبيحات خاشعة في محراب الفجر",
                soundAuthor = "ذاكرون",
                viewsCount = 890000
            )
        )
    )
    val shortsFeed: StateFlow<List<ShortItem>> = _shortsFeed.asStateFlow()

    fun toggleShortLike(id: String) {
        var updatedLiked = false
        var delta = 0L
        _shortsFeed.update { list ->
            list.map {
                if (it.id == id) {
                    val newLiked = !it.isLiked
                    updatedLiked = newLiked
                    delta = if (newLiked) 1L else -1L
                    it.copy(
                        isLiked = newLiked,
                        likesCount = (it.likesCount + delta).coerceAtLeast(0)
                    )
                } else it
            }
        }
        repositoryScope.launch {
            database?.videoDao()?.updateLike(id, delta, updatedLiked)
            com.example.data.remote.RowhaniyatCloudService.sendLike(id, updatedLiked)
        }
    }

    fun toggleShortSave(id: String) {
        var updatedSaved = false
        _shortsFeed.update { list ->
            list.map {
                if (it.id == id) {
                    updatedSaved = !it.isSaved
                    it.copy(isSaved = updatedSaved)
                } else it
            }
        }
        repositoryScope.launch {
            database?.videoDao()?.updateSave(id, updatedSaved)
        }
    }

    fun toggleShortRepost(id: String) {
        var updatedRepost = false
        var delta = 0L
        _shortsFeed.update { list ->
            list.map {
                if (it.id == id) {
                    updatedRepost = !it.isReposted
                    delta = if (updatedRepost) 1L else -1L
                    it.copy(
                        isReposted = updatedRepost,
                        repostsCount = it.repostsCount + delta
                    )
                } else it
            }
        }
        repositoryScope.launch {
            database?.videoDao()?.updateRepost(id, delta, updatedRepost)
        }
    }

    fun toggleShortFollow(id: String) {
        _shortsFeed.update { list ->
            list.map {
                if (it.id == id) it.copy(isFollowing = !it.isFollowing) else it
            }
        }
    }

    // Stories Feed
    private val _storiesFeed = MutableStateFlow(
        listOf(
            StoryItem(
                id = "story_1",
                authorName = "مشاري العفاسي",
                authorHandle = "alafasy",
                authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
                mediaUri = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=800&auto=format&fit=crop&q=80",
                isVideo = false,
                textCaption = "﴿وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ﴾ 🤍 نسأل الله الإجابة والقبول",
                isViewed = false,
                isMyStory = false,
                durationSeconds = 10
            ),
            StoryItem(
                id = "story_2",
                authorName = "الشيخ عثمان الخميس",
                authorHandle = "othmanalkhamees",
                authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
                mediaUri = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=800&auto=format&fit=crop&q=80",
                isVideo = false,
                textCaption = "لا تنسوا قراءة أذكار المساء وحفظ بيوتكم بالمعوذتين ✨",
                isViewed = false,
                isMyStory = false,
                durationSeconds = 12
            ),
            StoryItem(
                id = "story_3",
                authorName = "ذاكرون وذاكرات",
                authorHandle = "dhikr_community",
                authorAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                mediaUri = "https://images.unsplash.com/photo-1519817650390-64a93db51149?w=800&auto=format&fit=crop&q=80",
                isVideo = false,
                textCaption = "سبحان الله وبحمده سبحان الله العظيم 📿",
                isViewed = true,
                isMyStory = false,
                durationSeconds = 8
            )
        )
    )
    val storiesFeed: StateFlow<List<StoryItem>> = _storiesFeed.asStateFlow()

    fun publishStory(
        mediaUri: String,
        isVideo: Boolean = true,
        caption: String = ""
    ) {
        val user = _currentUser.value
        val newStory = StoryItem(
            id = "user_story_${System.currentTimeMillis()}",
            authorName = user.name,
            authorHandle = user.username,
            authorAvatarUrl = user.avatarUrl,
            mediaUri = mediaUri,
            isVideo = isVideo,
            textCaption = caption,
            isViewed = false,
            isMyStory = true,
            durationSeconds = if (isVideo) 20 else 8
        )
        _storiesFeed.update { listOf(newStory) + it }
        repositoryScope.launch {
            val entity = StoryEntity(
                id = newStory.id,
                authorName = newStory.authorName,
                authorHandle = newStory.authorHandle,
                authorAvatarUrl = newStory.authorAvatarUrl,
                authorUserId = user.id,
                mediaUri = newStory.mediaUri,
                isVideo = newStory.isVideo,
                textCaption = newStory.textCaption,
                isViewed = newStory.isViewed,
                isMyStory = true,
                durationSeconds = newStory.durationSeconds,
                timestamp = newStory.timestamp
            )
            database?.storyDao()?.insertStory(entity)
        }
    }

    fun deleteStory(storyId: String) {
        _storiesFeed.update { list -> list.filter { it.id != storyId } }
        repositoryScope.launch {
            database?.storyDao()?.deleteStory(storyId)
        }
    }

    fun markStoryViewed(storyId: String) {
        _storiesFeed.update { list ->
            list.map { if (it.id == storyId) it.copy(isViewed = true) else it }
        }
        repositoryScope.launch {
            database?.storyDao()?.markStoryViewed(storyId)
        }
    }

    // Add New Video or Short with metadata
    fun publishContent(
        title: String,
        description: String,
        hashtags: List<String>,
        isShort: Boolean,
        videoUrl: String = "",
        thumbnailUrl: String = "",
        durationFormatted: String = "",
        durationSeconds: Int = 45,
        isHorizontal: Boolean = false,
        isVertical: Boolean = false,
        fileSizeBytes: Long = 0L,
        formattedSize: String = "",
        category: String = "تلاوات"
    ) {
        val user = _currentUser.value
        val entityId = if (isShort) "user_short_${System.currentTimeMillis()}" else "user_vid_${System.currentTimeMillis()}"
        val effectiveThumb = if (thumbnailUrl.isNotBlank()) thumbnailUrl else "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=800&auto=format&fit=crop&q=80"
        val effectiveVideo = if (videoUrl.isNotBlank()) videoUrl else "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        val effectiveDuration = if (durationFormatted.isNotBlank()) durationFormatted else if (isShort) "00:${durationSeconds.coerceAtLeast(15)}" else "05:00"

        val entity = VideoEntity(
            id = entityId,
            title = title,
            authorName = user.name,
            authorHandle = user.username,
            authorAvatarUrl = user.avatarUrl,
            authorUserId = user.id,
            thumbnailUrl = effectiveThumb,
            videoUrl = effectiveVideo,
            viewsCount = 1,
            timestamp = System.currentTimeMillis(),
            timeAgo = "الآن",
            durationFormatted = effectiveDuration,
            durationSeconds = durationSeconds,
            likesCount = 0,
            commentsCount = 0,
            repostsCount = 0,
            sharesCount = 0,
            isLiked = false,
            isSaved = false,
            isReposted = false,
            hashtags = hashtags.joinToString(","),
            description = description,
            isShort = isShort,
            isVertical = isVertical,
            isHorizontal = isHorizontal,
            fileSizeBytes = fileSizeBytes,
            formattedSize = formattedSize,
            category = category
        )

        if (isShort) {
            _shortsFeed.update { listOf(entity.toShortItem()) + it }
        } else {
            _videosFeed.update { listOf(entity.toVideoItem()) + it }
            recalculateFeatured()
        }

        repositoryScope.launch {
            database?.videoDao()?.insertVideo(entity)
            try {
                val json = org.json.JSONObject().apply {
                    put("id", entityId)
                    put("title", title)
                    put("description", description)
                    put("authorName", user.name)
                    put("authorHandle", user.username)
                    put("authorAvatarUrl", user.avatarUrl)
                    put("thumbnailUrl", effectiveThumb)
                    put("videoUrl", effectiveVideo)
                    put("durationFormatted", effectiveDuration)
                    put("durationSeconds", durationSeconds)
                    put("isShort", isShort)
                    put("isVertical", isVertical)
                    put("isHorizontal", isHorizontal)
                    put("viewsCount", 1)
                    put("likesCount", 0)
                    put("commentsCount", 0)
                    put("category", category)
                }
                com.example.data.remote.RowhaniyatCloudService.publishVideoRecord(json)
            } catch (_: Exception) {}
        }
    }

    fun syncWithCloud() {
        repositoryScope.launch {
            try {
                val result = com.example.data.remote.RowhaniyatCloudService.fetchFeed()
                if (result.success) {
                    if (result.videos.isNotEmpty()) {
                        _videosFeed.value = result.videos
                        recalculateFeatured(result.videos)
                    }
                    if (result.shorts.isNotEmpty()) {
                        _shortsFeed.value = result.shorts
                    }
                    if (result.users.isNotEmpty()) {
                        _creatorsMap.update { it + result.users }
                    }
                    if (result.comments.isNotEmpty()) {
                        _comments.update { it + result.comments }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteVideoOrShort(id: String, onComplete: (Boolean) -> Unit = {}) {
        val user = _currentUser.value
        val isOwnerInVideos = _videosFeed.value.any { it.id == id && it.authorHandle == user.username }
        val isOwnerInShorts = _shortsFeed.value.any { it.id == id && it.authorHandle == user.username }

        if (!isOwnerInVideos && !isOwnerInShorts && user.username != "admin") {
            onComplete(false)
            return
        }

        _videosFeed.update { it.filter { v -> v.id != id } }
        _shortsFeed.update { it.filter { s -> s.id != id } }
        _comments.update { it - id }
        recalculateFeatured()

        repositoryScope.launch {
            database?.videoDao()?.deleteVideo(id)
            val success = com.example.data.remote.RowhaniyatCloudService.deleteVideo(id, user.username)
            onComplete(success)
        }
    }

    // Comments Store
    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(
        mapOf(
            "short_1" to listOf(
                Comment(
                    id = "c1",
                    contentId = "short_1",
                    authorName = "محمد العتيبي",
                    authorHandle = "m_otaibi",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
                    text = "سبحان الله، الآية جاءت في وقتها تماماً كأنها رسالة خاصة. جزاكم الله خيراً ونفع بكم.",
                    timeAgo = "منذ ساعتين",
                    likesCount = 342,
                    isLiked = true
                ),
                Comment(
                    id = "c2",
                    contentId = "short_1",
                    authorName = "فاطمة الزهراء",
                    authorHandle = "fatima_z",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80",
                    text = "اللهم إنا نسألك الرضا بعد القضاء، وبرد العيش بعد الموت ولذة النظر إلى وجهك الكريم.",
                    timeAgo = "منذ ٤ ساعات",
                    likesCount = 188,
                    isLiked = false
                )
            ),
            "vid_1" to listOf(
                Comment(
                    id = "c3",
                    contentId = "vid_1",
                    authorName = "أحمد رضوان",
                    authorHandle = "ahmed_radwan",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
                    text = "كلام يلامس شغاف القلب والله.. حفظ الله الشيخ عمر ونفعنا بعلمه.",
                    timeAgo = "منذ يوم",
                    likesCount = 520,
                    isLiked = true
                )
            )
        )
    )
    val comments: StateFlow<Map<String, List<Comment>>> = _comments.asStateFlow()

    fun addComment(contentId: String, text: String) {
        val user = _currentUser.value
        val newComment = Comment(
            id = "comment_${System.currentTimeMillis()}",
            contentId = contentId,
            authorName = user.name,
            authorHandle = user.username,
            authorAvatarUrl = user.avatarUrl,
            text = text,
            timeAgo = "الآن",
            likesCount = 0,
            isLiked = false
        )
        _comments.update { map ->
            val existing = map[contentId] ?: emptyList()
            map + (contentId to (listOf(newComment) + existing))
        }
        _videosFeed.update { list ->
            list.map { if (it.id == contentId) it.copy(commentsCount = it.commentsCount + 1) else it }
        }
        _shortsFeed.update { list ->
            list.map { if (it.id == contentId) it.copy(commentsCount = it.commentsCount + 1) else it }
        }
        recalculateFeatured()

        repositoryScope.launch {
            val entity = CommentEntity.fromComment(newComment, authorUserId = user.id)
            database?.commentDao()?.insertComment(entity)
            com.example.data.remote.RowhaniyatCloudService.addComment(
                videoId = contentId,
                authorName = user.name,
                authorHandle = user.username,
                avatarUrl = user.avatarUrl,
                text = text
            )
        }
    }

    fun toggleCommentLike(
        contentId: String,
        commentId: String,
        replyId: String? = null,
        context: android.content.Context? = null
    ) {
        val user = _currentUser.value
        var targetAuthor = ""
        var isNowLiked = false

        _comments.update { map ->
            val list = map[contentId] ?: return@update map
            val updated = list.map { parent ->
                if (replyId == null && parent.id == commentId) {
                    val liked = !parent.isLiked
                    isNowLiked = liked
                    targetAuthor = parent.authorHandle
                    parent.copy(
                        isLiked = liked,
                        likesCount = if (liked) parent.likesCount + 1 else (parent.likesCount - 1).coerceAtLeast(0)
                    )
                } else if (replyId != null && parent.id == commentId) {
                    val updatedReplies = parent.replies.map { rep ->
                        if (rep.id == replyId) {
                            val liked = !rep.isLiked
                            isNowLiked = liked
                            targetAuthor = rep.authorHandle
                            rep.copy(
                                isLiked = liked,
                                likesCount = if (liked) rep.likesCount + 1 else (rep.likesCount - 1).coerceAtLeast(0)
                            )
                        } else rep
                    }
                    parent.copy(replies = updatedReplies)
                } else parent
            }
            map + (contentId to updated)
        }

        if (isNowLiked && targetAuthor.isNotBlank() && targetAuthor != user.username && context != null) {
            com.example.util.RowhaniyatNotificationManager.notify(
                context = context,
                title = "إعجاب بتعليقك ❤️",
                body = "أعجب ${user.name} بتعليقك الإيماني المبارك",
                type = NotificationType.LIKE,
                targetId = contentId,
                targetType = "short"
            )
        }

        repositoryScope.launch {
            com.example.data.remote.RowhaniyatCloudService.sendCommentLike(
                contentId,
                replyId ?: commentId,
                isNowLiked
            )
        }
    }

    fun addCommentReply(
        contentId: String,
        commentId: String,
        text: String,
        context: android.content.Context? = null
    ) {
        val user = _currentUser.value
        val newReply = Comment(
            id = "rep_${System.currentTimeMillis()}",
            contentId = contentId,
            authorName = user.name,
            authorHandle = user.username,
            authorAvatarUrl = user.avatarUrl,
            text = text.trim(),
            timeAgo = "الآن",
            likesCount = 0L,
            isLiked = false
        )

        _comments.update { map ->
            val list = map[contentId] ?: emptyList()
            val updated = list.map { parent ->
                if (parent.id == commentId) {
                    parent.copy(replies = parent.replies + newReply)
                } else parent
            }
            map + (contentId to updated)
        }

        _videosFeed.update { list ->
            list.map { if (it.id == contentId) it.copy(commentsCount = it.commentsCount + 1) else it }
        }
        _shortsFeed.update { list ->
            list.map { if (it.id == contentId) it.copy(commentsCount = it.commentsCount + 1) else it }
        }

        val parent = _comments.value[contentId]?.find { it.id == commentId }
        if (parent != null && parent.authorHandle != user.username && context != null) {
            com.example.util.RowhaniyatNotificationManager.notify(
                context = context,
                title = "رد جديد على تعليقك 💬",
                body = "رد ${user.name} على تعليقك: \"$text\"",
                type = NotificationType.COMMENT,
                targetId = contentId,
                targetType = "short"
            )
        }

        repositoryScope.launch {
            com.example.data.remote.RowhaniyatCloudService.addCommentReply(
                videoId = contentId,
                commentId = commentId,
                authorName = user.name,
                authorHandle = user.username,
                avatarUrl = user.avatarUrl,
                text = text
            )
        }
    }

    fun editVideoContent(
        videoId: String,
        newTitle: String,
        newDescription: String,
        newHashtags: List<String>,
        newThumbnailUrl: String
    ): Boolean {
        val user = _currentUser.value
        val isShort = _shortsFeed.value.any { it.id == videoId }
        val isLong = _videosFeed.value.any { it.id == videoId }
        if (!isShort && !isLong) return false

        if (isShort) {
            _shortsFeed.update { list ->
                list.map { s ->
                    if (s.id == videoId) {
                        s.copy(
                            title = newTitle.trim(),
                            description = newDescription.trim(),
                            hashtags = newHashtags,
                            thumbnailUrl = newThumbnailUrl.ifBlank { s.thumbnailUrl }
                        )
                    } else s
                }
            }
        } else {
            _videosFeed.update { list ->
                list.map { v ->
                    if (v.id == videoId) {
                        v.copy(
                            title = newTitle.trim(),
                            description = newDescription.trim(),
                            hashtags = newHashtags,
                            thumbnailUrl = newThumbnailUrl.ifBlank { v.thumbnailUrl }
                        )
                    } else v
                }
            }
        }

        repositoryScope.launch {
            com.example.data.remote.RowhaniyatCloudService.editVideo(
                videoId = videoId,
                title = newTitle,
                description = newDescription,
                hashtags = newHashtags,
                thumbnailUrl = newThumbnailUrl,
                authorHandle = user.username
            )
        }
        return true
    }

    fun deleteComment(contentId: String, commentId: String) {
        val user = _currentUser.value
        _comments.update { map ->
            val list = map[contentId] ?: return@update map
            map + (contentId to list.filter { it.id != commentId })
        }
        _videosFeed.update { list ->
            list.map { if (it.id == contentId) it.copy(commentsCount = (it.commentsCount - 1).coerceAtLeast(0)) else it }
        }
        _shortsFeed.update { list ->
            list.map { if (it.id == contentId) it.copy(commentsCount = (it.commentsCount - 1).coerceAtLeast(0)) else it }
        }
        recalculateFeatured()

        repositoryScope.launch {
            database?.commentDao()?.deleteCommentByUser(commentId, user.id)
            com.example.data.remote.RowhaniyatCloudService.deleteComment(contentId, commentId, user.username)
        }
    }

    // Notifications
    private val _notifications = MutableStateFlow(
        listOf(
            NotificationItem(
                id = "notif_1",
                title = "متابع جديد",
                body = "بدأ الشيخ عبد الله المحمود في متابعتك.",
                timeAgo = "منذ 10 دقائق",
                type = NotificationType.NEW_FOLLOWER,
                isRead = false
            ),
            NotificationItem(
                id = "notif_2",
                title = "إعجاب جديد على الـ Short الخاص بك",
                body = "أعجب أكثر من ٥٠٠ شخص بمقطعك الأخير «وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ».",
                timeAgo = "منذ ساعة",
                type = NotificationType.LIKE,
                isRead = false
            ),
            NotificationItem(
                id = "notif_3",
                title = "تذكير: أذكار المساء 🌙",
                body = "حان الآن وقت أذكار المساء لتحصين نفسك وذكر الرحمن.",
                timeAgo = "منذ ٣ ساعات",
                type = NotificationType.EVENING_AZKAR,
                isRead = true
            ),
            NotificationItem(
                id = "notif_4",
                title = "آية اليوم المقترحة",
                body = "«لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا» - سورة البقرة.",
                timeAgo = "اليوم، ٠٨:٠٠ ص",
                type = NotificationType.AYAH_OF_THE_DAY,
                isRead = true
            ),
            NotificationItem(
                id = "notif_5",
                title = "إعادة نشر محتواك 🔁",
                body = "قام حساب «قبسات من نور» بإعادة نشر مقطعك التوعوي.",
                timeAgo = "أمس",
                type = NotificationType.REPOST,
                isRead = true
            )
        )
    )
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun addNotification(item: NotificationItem) {
        _notifications.update { list -> listOf(item) + list }
    }

    fun markNotificationRead(id: String) {
        _notifications.update { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
    }

    fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
    }

    // Electronic Tasbeeh state
    private val _tasbeehCount = MutableStateFlow(0)
    val tasbeehCount: StateFlow<Int> = _tasbeehCount.asStateFlow()

    private val _tasbeehTarget = MutableStateFlow(33)
    val tasbeehTarget: StateFlow<Int> = _tasbeehTarget.asStateFlow()

    private val _currentZikrPhrase = MutableStateFlow("سُبْحَانَ اللَّهِ")
    val currentZikrPhrase: StateFlow<String> = _currentZikrPhrase.asStateFlow()

    private val _totalTasbeehSessions = MutableStateFlow(1420)
    val totalTasbeehSessions: StateFlow<Int> = _totalTasbeehSessions.asStateFlow()

    fun incrementTasbeeh() {
        _tasbeehCount.update { it + 1 }
        _totalTasbeehSessions.update { it + 1 }
    }

    fun resetTasbeeh() {
        _tasbeehCount.value = 0
    }

    fun setTasbeehTarget(target: Int) {
        _tasbeehTarget.value = target
    }

    fun setTasbeehPhrase(phrase: String) {
        _currentZikrPhrase.value = phrase
        _tasbeehCount.value = 0
    }
}
