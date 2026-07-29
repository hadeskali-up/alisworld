package com.alisworld.app.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alisworld.app.AlisWorldApp
import com.alisworld.app.MainActivity
import com.alisworld.app.R
import com.alisworld.app.data.ApiClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlisWorldFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")
        
        // Register token with backend
        CoroutineScope(Dispatchers.IO).launch {
            ApiClient.instance.registerFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Message received: ${message.data}")

        // Only handle TP-hit notifications
        if (message.data["type"] == "tp_hit") {
            val symbol = message.data["symbol"] ?: "Unknown"
            val profit = message.data["profit"]?.toDoubleOrNull() ?: 0.0
            val closePrice = message.data["close_price"]?.toDoubleOrNull() ?: 0.0

            showTpNotification(symbol, profit, closePrice)
        }
    }

    private fun showTpNotification(symbol: String, profit: Double, closePrice: Double) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, AlisWorldApp.CHANNEL_TP_HITS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🎯 TP Hit: $symbol")
            .setContentText("Closed at ${String.format("%.5f", closePrice)} • Profit: $${String.format("%.2f", profit)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val TAG = "AlisWorldFcm"
    }
}
