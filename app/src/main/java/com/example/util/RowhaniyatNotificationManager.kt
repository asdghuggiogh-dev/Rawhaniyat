package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.NotificationItem
import com.example.data.model.NotificationType
import com.example.data.repository.IslamicDataRepository

object RowhaniyatNotificationManager {

    const val CHANNEL_ID = "rowhaniyat_spiritual_channel"
    const val CHANNEL_NAME = "إشعارات روحانيات 🌙"

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.rowhaniyat_notification}")
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات التفاعلات والمحتوى القرآني والإيماني في روحانيات"
                enableLights(true)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Preview the serene custom notification sound directly (useful in settings).
     */
    fun playPreviewSound(context: Context) {
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.rowhaniyat_notification)
            mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Post a notification both in-app and as a system status-bar notification with custom sound.
     */
    fun notify(
        context: Context,
        title: String,
        body: String,
        type: NotificationType,
        targetId: String = "",
        targetType: String = ""
    ) {
        val prefs = context.getSharedPreferences("rowhaniyat_settings_prefs", Context.MODE_PRIVATE)

        // 1. Check user preferences
        val allEnabled = prefs.getBoolean("notif_all_enabled", true)
        if (!allEnabled) return

        val isTypeAllowed = when (type) {
            NotificationType.LIKE -> prefs.getBoolean("notif_likes", true)
            NotificationType.COMMENT -> prefs.getBoolean("notif_comments", true)
            NotificationType.NEW_FOLLOWER -> prefs.getBoolean("notif_followers", true)
            NotificationType.NEW_VIDEO -> prefs.getBoolean("notif_new_content", true)
            NotificationType.UPLOAD_COMPLETE -> true
            NotificationType.ADMIN_ANNOUNCEMENT -> prefs.getBoolean("notif_general", true)
            NotificationType.SPIRITUAL_REMINDER,
            NotificationType.QURAN_REMINDER,
            NotificationType.MORNING_AZKAR,
            NotificationType.EVENING_AZKAR,
            NotificationType.AYAH_OF_THE_DAY -> prefs.getBoolean("notif_spiritual_reminders", true)
            NotificationType.REPOST -> prefs.getBoolean("notif_likes", true)
        }

        if (!isTypeAllowed) return

        // 2. Add to in-app notification repository
        val notificationItem = NotificationItem(
            id = "notif_${System.currentTimeMillis()}_${(100..999).random()}",
            title = title,
            body = body,
            timeAgo = "الآن",
            type = type,
            isRead = false,
            targetId = targetId,
            targetType = targetType,
            timestamp = System.currentTimeMillis()
        )
        IslamicDataRepository.addNotification(notificationItem)

        // 3. Post System Notification with Custom Spiritual Sound
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_id", targetId)
                putExtra("target_type", targetType)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.rowhaniyat_notification}")

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(soundUri)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)

            NotificationManagerCompat.from(context).notify(
                notificationItem.id.hashCode(),
                builder.build()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
