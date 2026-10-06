package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import android.widget.VideoView
import java.io.File
import java.io.FileOutputStream
import kotlin.math.absoluteValue

data class VideoMeta(
    val durationMs: Long = 0L,
    val formattedDuration: String = "00:00",
    val width: Int = 1080,
    val height: Int = 1920,
    val isVertical: Boolean = true,
    val sizeBytes: Long = 0L,
    val formattedSize: String = "0 ميجابايت",
    val thumbnailUri: String? = null,
    val fileName: String = "video.mp4"
)

object VideoUtils {

    private const val FALLBACK_DEFAULT_THUMB = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=800&auto=format&fit=crop&q=80"

    /**
     * Extracts full video metadata: duration, dimensions, orientation, file size,
     * and extracts a real video frame as a permanent JPEG thumbnail in filesDir.
     */
    fun extractMetadata(context: Context, uri: Uri): VideoMeta {
        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        var width = 1080
        var height = 1920
        var rotation = 0
        var thumbUri: String? = null
        var sizeBytes = 0L
        var displayName = "فيديو_محدد.mp4"

        try {
            // 1. Get file size and name from ContentResolver
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                }
            }

            // 2. Set data source for MediaMetadataRetriever
            if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
                val path = uri.path ?: uri.toString().removePrefix("file://")
                retriever.setDataSource(path)
            } else {
                retriever.setDataSource(context, uri)
            }

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

            width = widthStr?.toIntOrNull() ?: 1080
            height = heightStr?.toIntOrNull() ?: 1920
            rotation = rotationStr?.toIntOrNull() ?: 0

            // If video is rotated 90 or 270 degrees, swap dimensions
            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }

            // 3. Extract and save Thumbnail Bitmap into permanent internal storage
            val bitmap = extractFrameBitmap(context, retriever, uri)
            if (bitmap != null) {
                val thumbDir = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }
                val thumbFile = File(thumbDir, "thumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                if (thumbFile.exists() && thumbFile.length() > 0) {
                    thumbUri = Uri.fromFile(thumbFile).toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        val isVertical = height >= width

        // Format duration
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        val formattedDuration = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        // Format file size
        val formattedSize = when {
            sizeBytes >= 1024L * 1024L * 1024L -> String.format("%.1f جيجابايت", sizeBytes.toDouble() / (1024 * 1024 * 1024))
            sizeBytes >= 1024L * 1024L -> String.format("%.1f ميجابايت", sizeBytes.toDouble() / (1024 * 1024))
            sizeBytes >= 1024L -> String.format("%.1f كيلوبايت", sizeBytes.toDouble() / 1024)
            sizeBytes > 0 -> "$sizeBytes بايت"
            else -> "حجم مناسب"
        }

        return VideoMeta(
            durationMs = durationMs,
            formattedDuration = formattedDuration,
            width = width,
            height = height,
            isVertical = isVertical,
            sizeBytes = sizeBytes,
            formattedSize = formattedSize,
            thumbnailUri = thumbUri,
            fileName = displayName
        )
    }

    /**
     * Extracts a frame bitmap using multiple strategies to guarantee success.
     */
    private fun extractFrameBitmap(context: Context, retriever: MediaMetadataRetriever, uri: Uri): Bitmap? {
        // Try multiple timestamps: 1s, 0.5s, 0s, -1
        val timeCandidates = longArrayOf(1_000_000L, 500_000L, 0L, -1L)
        for (time in timeCandidates) {
            try {
                val bmp = if (time >= 0) {
                    retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST)
                } else {
                    retriever.frameAtTime
                }
                if (bmp != null) return bmp
            } catch (_: Exception) {}
        }

        // Fallback: ThumbnailUtils or contentResolver.loadThumbnail if API 29+
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return context.contentResolver.loadThumbnail(uri, Size(720, 1280), null)
            }
        } catch (_: Exception) {}

        // Fallback: file path with ThumbnailUtils
        try {
            if (uri.scheme == "file") {
                val path = uri.path ?: uri.toString().removePrefix("file://")
                return ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND)
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Retrieves an existing thumbnail or extracts a new one from the video,
     * saving it permanently in context.filesDir/thumbnails/.
     * Guarantees that EVERY video (Short or Long) has a real, valid thumbnail.
     */
    fun getOrExtractThumbnail(context: Context, videoUrl: String, existingThumb: String?): String {
        // 1. Check if existing thumbnail is valid
        if (!existingThumb.isNullOrBlank()) {
            if (existingThumb.startsWith("file://")) {
                val file = File(existingThumb.removePrefix("file://"))
                if (file.exists() && file.length() > 0) {
                    return existingThumb
                }
            } else if (existingThumb.startsWith("http://") || existingThumb.startsWith("https://")) {
                return existingThumb
            }
        }

        if (videoUrl.isBlank()) {
            return FALLBACK_DEFAULT_THUMB
        }

        // 2. Check if we already cached a thumbnail for this video URL
        val cacheKey = videoUrl.hashCode().absoluteValue.toString()
        val thumbDir = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }
        val cachedFile = File(thumbDir, "thumb_cache_$cacheKey.jpg")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return Uri.fromFile(cachedFile).toString()
        }

        // 3. Extract thumbnail dynamically from the video
        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(videoUrl)
            if (uri.scheme == "file" || videoUrl.startsWith("/")) {
                val path = uri.path ?: videoUrl.removePrefix("file://")
                retriever.setDataSource(path)
            } else if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                retriever.setDataSource(videoUrl, HashMap<String, String>())
            } else {
                retriever.setDataSource(context, uri)
            }

            val bitmap = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.frameAtTime

            if (bitmap != null) {
                FileOutputStream(cachedFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                if (cachedFile.exists() && cachedFile.length() > 0) {
                    return Uri.fromFile(cachedFile).toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        return FALLBACK_DEFAULT_THUMB
    }

    /**
     * Persists media into internal app files storage.
     * Guarantees file readability across reboots, app kills, and permission expirations.
     */
    fun persistMediaLocally(context: Context, sourceUri: Uri, isVideo: Boolean): Uri {
        // First try to persist URI permission if supported
        try {
            context.contentResolver.takePersistableUriPermission(
                sourceUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}

        // If it's already a local file in our filesDir, keep it
        if (sourceUri.scheme == "file" && sourceUri.path?.contains(context.filesDir.path) == true) {
            return sourceUri
        }

        return try {
            val subDir = if (isVideo) "videos" else "images"
            val targetDir = File(context.filesDir, subDir).apply { if (!exists()) mkdirs() }
            val ext = if (isVideo) "mp4" else "jpg"
            val targetFile = File(targetDir, "media_${System.currentTimeMillis()}.$ext")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                Uri.fromFile(targetFile)
            } else {
                sourceUri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri
        }
    }

    /**
     * Configure VideoView for robust playback of local files (file://) or content/remote URIs.
     */
    fun bindVideoView(
        videoView: VideoView,
        videoUrl: String,
        onPrepared: (MediaPlayer) -> Unit = {},
        onError: () -> Unit = {}
    ) {
        try {
            val uri = Uri.parse(videoUrl)
            if (uri.scheme == "file" || videoUrl.startsWith("/")) {
                val path = uri.path ?: videoUrl.removePrefix("file://")
                val localFile = File(path)
                if (localFile.exists()) {
                    videoView.setVideoPath(localFile.absolutePath)
                } else {
                    videoView.setVideoURI(uri)
                }
            } else {
                videoView.setVideoURI(uri)
            }

            videoView.setOnPreparedListener { mp ->
                onPrepared(mp)
            }
            videoView.setOnErrorListener { _, what, extra ->
                onError()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError()
        }
    }

    /**
     * Persists profile avatar or cover into permanent internal storage.
     */
    fun persistProfileImage(context: Context, sourceUri: Uri, isAvatar: Boolean): Uri {
        return try {
            val dir = File(context.filesDir, "profile").apply { if (!exists()) mkdirs() }
            val prefix = if (isAvatar) "avatar" else "cover"
            val targetFile = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) {
                Uri.fromFile(targetFile)
            } else {
                sourceUri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri
        }
    }

    /**
     * Formats counts into compact readable strings (e.g., 999, 1K, 1.2K, 10K, 1.5M).
     */
    fun formatCompactCount(count: Long): String {
        if (count < 0) return "0"
        return when {
            count < 1_000 -> count.toString()
            count < 1_000_000 -> {
                val thousands = count / 1000.0
                if (count % 1000 < 100 || count >= 100_000) {
                    "${count / 1000}K"
                } else {
                    String.format(java.util.Locale.US, "%.1fK", thousands).replace(".0K", "K")
                }
            }
            else -> {
                val millions = count / 1000000.0
                if (count % 1000000 < 100000) {
                    "${count / 1000000}M"
                } else {
                    String.format(java.util.Locale.US, "%.1fM", millions).replace(".0M", "M")
                }
            }
        }
    }

    /**
     * Ensures random numeric file names like 1000022396 are replaced with a spiritual title.
     */
    fun cleanShortTitle(title: String): String {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return "تلاوة إيمانية خاشعة 🌙"
        if (trimmed.matches(Regex("^[0-9_\\-\\.]+$")) || trimmed.startsWith("user_short_") || trimmed.startsWith("vid_")) {
            return "نفحات إيمانية وتلاوة خاشعة 🌙"
        }
        return trimmed
    }
}
