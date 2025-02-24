package com.example.citamedicacl.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.citamedicacl.R
import com.example.citamedicacl.patient.PatientActivity

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "appointment_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Citas Médicas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de citas médicas"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showAppointmentNotification(doctorName: String, date: String, time: String) {
        val intent = Intent(context, PatientActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Nueva Cita Médica")
            .setContentText("Cita agendada con $doctorName para el $date a las $time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun requestNotificationPermission(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    companion object {
        const val NOTIFICATION_PERMISSION_CODE = 123
    }
} 