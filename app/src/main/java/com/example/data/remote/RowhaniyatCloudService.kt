package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.Comment
import com.example.data.model.ShortItem
import com.example.data.model.UserProfile
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

object RowhaniyatCloudService {

    private const val TAG = "RowhaniyatCloudService"
    const val BASE_URL = "https://ais-pre-4angobnrimj6sryymqfpgm-149853408349.europe-west2.run.app"

    data class CloudFeedResult(
        val success: Boolean,
        val videos: List<VideoItem> = emptyList(),
        val shorts: List<ShortItem> = emptyList(),
        val users: Map<String, UserProfile> = emptyMap(),
        val comments: Map<String, List<Comment>> = emptyMap(),
        val error: String? = null
    )

    /**
     * 1. Fetch Central Online Cloud Feed (shared between all users)
     */
    suspend fun fetchFeed(): CloudFeedResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/api/feed")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                val videosList = mutableListOf<VideoItem>()
                val videosArray = json.optJSONArray("videos") ?: JSONArray()
                for (i in 0 until videosArray.length()) {
                    val v = videosArray.getJSONObject(i)
                    videosList.add(
                        VideoItem(
                            id = v.optString("id", "vid_$i"),
                            title = v.optString("title", ""),
                            description = v.optString("description", ""),
                            authorName = v.optString("authorName", "روحانيات"),
                            authorHandle = v.optString("authorHandle", "rowhaniyat"),
                            authorAvatarUrl = v.optString("authorAvatarUrl", ""),
                            thumbnailUrl = v.optString("thumbnailUrl", ""),
                            videoUrl = v.optString("videoUrl", ""),
                            viewsCount = v.optLong("viewsCount", 0L),
                            likesCount = v.optLong("likesCount", 0L),
                            commentsCount = v.optLong("commentsCount", 0L),
                            repostsCount = v.optLong("repostsCount", 0L),
                            category = v.optString("category", "تلاوات"),
                            timeAgo = v.optString("timeAgo", "مؤخراً"),
                            durationFormatted = v.optString("durationFormatted", "05:00")
                        )
                    )
                }

                val shortsList = mutableListOf<ShortItem>()
                val shortsArray = json.optJSONArray("shorts") ?: JSONArray()
                for (i in 0 until shortsArray.length()) {
                    val s = shortsArray.getJSONObject(i)
                    shortsList.add(
                        ShortItem(
                            id = s.optString("id", "short_$i"),
                            title = s.optString("title", ""),
                            authorName = s.optString("authorName", "روحانيات"),
                            authorHandle = s.optString("authorHandle", "rowhaniyat"),
                            authorAvatarUrl = s.optString("authorAvatarUrl", ""),
                            videoUrl = s.optString("videoUrl", ""),
                            thumbnailUrl = s.optString("thumbnailUrl", ""),
                            likesCount = s.optLong("likesCount", 0L),
                            commentsCount = s.optLong("commentsCount", 0L),
                            repostsCount = s.optLong("repostsCount", 0L),
                            sharesCount = s.optLong("sharesCount", 0L),
                            category = s.optString("category", "تلاوات"),
                            viewsCount = s.optLong("viewsCount", 0L),
                            soundTitle = s.optString("soundTitle", "تلاوة خاشعة"),
                            soundAuthor = s.optString("soundAuthor", "روحانيات"),
                            durationSeconds = s.optInt("durationSeconds", 30)
                        )
                    )
                }

                val usersMap = mutableMapOf<String, UserProfile>()
                val usersObj = json.optJSONObject("users")
                if (usersObj != null) {
                    val keys = usersObj.keys()
                    while (keys.hasNext()) {
                        val handle = keys.next()
                        val u = usersObj.getJSONObject(handle)
                        usersMap[handle] = UserProfile(
                            id = u.optString("id", handle),
                            name = u.optString("name", handle),
                            username = u.optString("username", handle),
                            bio = u.optString("bio", ""),
                            avatarUrl = u.optString("avatarUrl", ""),
                            coverUrl = u.optString("coverUrl", "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=1200&q=80"),
                            followersCount = u.optLong("followersCount", 0L),
                            followingCount = u.optLong("followingCount", 0L),
                            likesCount = u.optLong("likesCount", 0L),
                            viewsCount = u.optLong("viewsCount", 0L),
                            videosCount = u.optLong("videosCount", 0L)
                        )
                    }
                }

                val commentsMap = mutableMapOf<String, List<Comment>>()
                val commentsObj = json.optJSONObject("comments")
                if (commentsObj != null) {
                    val keys = commentsObj.keys()
                    while (keys.hasNext()) {
                        val contentId = keys.next()
                        val arr = commentsObj.getJSONArray(contentId)
                        val cList = mutableListOf<Comment>()
                        for (j in 0 until arr.length()) {
                            val c = arr.getJSONObject(j)
                            cList.add(
                                Comment(
                                    id = c.optString("id", "c_$j"),
                                    contentId = contentId,
                                    authorName = c.optString("authorName", ""),
                                    authorHandle = c.optString("authorHandle", ""),
                                    authorAvatarUrl = c.optString("authorAvatarUrl", ""),
                                    text = c.optString("text", ""),
                                    timeAgo = c.optString("timeAgo", "الآن"),
                                    likesCount = c.optLong("likesCount", 0L)
                                )
                            )
                        }
                        commentsMap[contentId] = cList
                    }
                }

                CloudFeedResult(
                    success = true,
                    videos = videosList,
                    shorts = shortsList,
                    users = usersMap,
                    comments = commentsMap
                )
            } else {
                CloudFeedResult(success = false, error = "HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchFeed error: ${e.message}")
            CloudFeedResult(success = false, error = e.localizedMessage)
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 2. Real Cloud Media Upload (Stream video to server storage)
     */
    suspend fun uploadVideo(
        context: Context,
        videoUri: Uri,
        onProgress: (Int) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/api/upload")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
                setRequestProperty("Content-Type", "video/mp4")
            }

            val inputStream: InputStream? = if (videoUri.scheme == "file") {
                File(videoUri.path ?: "").inputStream()
            } else {
                context.contentResolver.openInputStream(videoUri)
            }

            if (inputStream == null) return@withContext null

            val totalBytes = inputStream.available().coerceAtLeast(1)
            var uploadedBytes = 0L

            conn.outputStream.use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    uploadedBytes += bytesRead
                    val progress = ((uploadedBytes.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 99)
                    onProgress(progress)
                }
                outputStream.flush()
            }
            onProgress(100)

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                json.optString("url", null)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadVideo error: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 3. Publish Video / Short Record to Cloud Database
     */
    suspend fun publishVideoRecord(videoJson: JSONObject): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/api/videos")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connectTimeout = 10000
                readTimeout = 10000
            }
            conn.outputStream.bufferedWriter().use { it.write(videoJson.toString()) }
            conn.responseCode in 200..204
        } catch (e: Exception) {
            Log.e(TAG, "publishVideoRecord error: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 4. Delete Video / Short from Cloud Database (Owner Verification)
     */
    suspend fun deleteVideo(videoId: String, userHandle: String): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/api/videos/$videoId")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("x-user-handle", userHandle)
                connectTimeout = 8000
                readTimeout = 8000
            }
            conn.responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "deleteVideo error: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 5. Record Like on Cloud Database
     */
    suspend fun sendLike(videoId: String, isLiked: Boolean) = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/videos/$videoId/like")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
            }
            val json = JSONObject().put("isLiked", isLiked)
            conn.outputStream.bufferedWriter().use { it.write(json.toString()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }

    /**
     * 6. Add Comment to Cloud Database
     */
    suspend fun addComment(
        videoId: String,
        authorName: String,
        authorHandle: String,
        avatarUrl: String,
        text: String
    ) = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/videos/$videoId/comments")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
            }
            val json = JSONObject().apply {
                put("authorName", authorName)
                put("authorHandle", authorHandle)
                put("authorAvatarUrl", avatarUrl)
                put("text", text)
            }
            conn.outputStream.bufferedWriter().use { it.write(json.toString()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }

    /**
     * 7. Delete Comment from Cloud Database
     */
    suspend fun deleteComment(videoId: String, commentId: String, userHandle: String) = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/videos/$videoId/comments/$commentId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("x-user-handle", userHandle)
                connectTimeout = 5000
                readTimeout = 5000
            }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }

    /**
     * 8. Add Reply to Comment in Cloud Database
     */
    suspend fun addCommentReply(
        videoId: String,
        commentId: String,
        authorName: String,
        authorHandle: String,
        avatarUrl: String,
        text: String
    ) = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/videos/$videoId/comments/$commentId/reply")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
            }
            val json = JSONObject().apply {
                put("authorName", authorName)
                put("authorHandle", authorHandle)
                put("authorAvatarUrl", avatarUrl)
                put("text", text)
            }
            conn.outputStream.bufferedWriter().use { it.write(json.toString()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }

    /**
     * 9. Send Comment Like to Cloud Database
     */
    suspend fun sendCommentLike(videoId: String, commentId: String, isLiked: Boolean) = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/videos/$videoId/comments/$commentId/like")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
            }
            val json = JSONObject().put("isLiked", isLiked)
            conn.outputStream.bufferedWriter().use { it.write(json.toString()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }

    /**
     * 10. Edit Short or Video in Cloud Database (Owner Verification)
     */
    suspend fun editVideo(
        videoId: String,
        title: String,
        description: String,
        hashtags: List<String>,
        thumbnailUrl: String,
        authorHandle: String
    ): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/api/videos/$videoId")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-user-handle", authorHandle)
                connectTimeout = 8000
                readTimeout = 8000
            }
            val json = JSONObject().apply {
                put("title", title)
                put("description", description)
                put("hashtags", JSONArray(hashtags))
                if (thumbnailUrl.isNotBlank()) put("thumbnailUrl", thumbnailUrl)
            }
            conn.outputStream.bufferedWriter().use { it.write(json.toString()) }
            conn.responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "editVideo error: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 11. Save User Profile to Cloud Database
     */
    suspend fun saveUserProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/api/users")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }
            val json = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("username", profile.username)
                put("bio", profile.bio)
                put("avatarUrl", profile.avatarUrl)
                put("coverUrl", profile.coverUrl)
                put("followersCount", profile.followersCount)
                put("followingCount", profile.followingCount)
                put("likesCount", profile.likesCount)
                put("viewsCount", profile.viewsCount)
                put("videosCount", profile.videosCount)
                put("country", profile.country)
                put("email", profile.email)
            }
            conn.outputStream.bufferedWriter().use { it.write(json.toString()) }
            conn.responseCode in 200..204
        } catch (e: Exception) {
            Log.e(TAG, "saveUserProfile error: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }
}
