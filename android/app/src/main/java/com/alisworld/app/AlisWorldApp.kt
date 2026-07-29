package com.alisworld.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AlisWorldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tpChannel = NotificationChannel(
                CHANNEL_TP_HITS,
                "TP Hits",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when Take Profit is hit"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(tpChannel)
        }
    }

    companion object {
        const val CHANNEL_TP_HITS = "tp_hits"
    }
}
