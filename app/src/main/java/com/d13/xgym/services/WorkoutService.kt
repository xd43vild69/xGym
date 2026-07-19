package com.d13.xgym.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.d13.xgym.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WorkoutService : Service() {

    companion object {
        const val ACTION_START_WORKOUT = "START_WORKOUT"
        const val ACTION_STOP_WORKOUT = "STOP_WORKOUT"
        const val ACTION_START_REST = "START_REST"
        const val ACTION_STOP_REST = "STOP_REST"

        const val EXTRA_SESSION_START_TS = "SESSION_START_TS"
        const val EXTRA_REST_START_TS = "REST_START_TS"
        const val EXTRA_REST_DURATION_MS = "REST_DURATION_MS"
    }

    private val CHANNEL_ID = "workout_channel"
    private val NOTIFICATION_ID = 1

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var restJob: Job? = null

    private var sessionStartTs: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_WORKOUT -> {
                sessionStartTs = intent.getLongExtra(EXTRA_SESSION_START_TS, System.currentTimeMillis())
                startForegroundService()
            }
            ACTION_STOP_WORKOUT -> {
                stopForegroundService()
            }
            ACTION_START_REST -> {
                val restStartTs = intent.getLongExtra(EXTRA_REST_START_TS, System.currentTimeMillis())
                val durationMs = intent.getLongExtra(EXTRA_REST_DURATION_MS, 0L)
                startRestTimer(restStartTs, durationMs)
            }
            ACTION_STOP_REST -> {
                cancelRestTimer()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        // Build initial notification
        val notification = buildNotification("00:00")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start timer loop to update notification
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - sessionStartTs
                updateNotification(formatHMS(elapsed))
                delay(1000)
            }
        }
    }

    private fun stopForegroundService() {
        timerJob?.cancel()
        cancelRestTimer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun startRestTimer(restStartTs: Long, durationMs: Long) {
        cancelRestTimer()
        restJob = serviceScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - restStartTs
                if (elapsed >= durationMs) {
                    vibrate()
                    // Dispara una sola vez
                    break
                }
                delay(200)
            }
        }
    }

    private fun cancelRestTimer() {
        restJob?.cancel()
        restJob = null
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(500)
    }

    private fun buildNotification(timeStr: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Entrenamiento en curso")
            .setContentText("Tiempo: $timeStr")
            // Reemplaza esto con un ícono real de tu mipmap/drawable si tienes uno
            .setSmallIcon(android.R.drawable.ic_menu_preferences) 
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // No hacer sonido cada vez que se actualiza el segundo
            .build()
    }

    private fun updateNotification(timeStr: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(timeStr))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cronómetro de Entrenamiento",
                NotificationManager.IMPORTANCE_LOW // Low = sin sonido para que el reloj corra silencioso
            ).apply {
                description = "Muestra el tiempo activo de tu sesión"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatHMS(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }
}
