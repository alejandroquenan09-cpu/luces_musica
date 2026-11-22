package com.alejandro.lucesmusica

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder

class LightService : Service() {

    companion object {
        var instance: LightService? = null
        var onBass: ((Float) -> Unit)? = null
        var onColor: ((Int) -> Unit)? = null
        var onStatus: ((String) -> Unit)? = null
        var currentColor: Int = Color.rgb(90, 90, 160)

        fun start(context: Context) {
            val i = Intent(context, LightService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LightService::class.java))
        }
    }

    private var strip: LedStrip? = null
    private var analyzer: AudioAnalyzer? = null
    private var cover: CoverListener? = null
    private var smooth = 0f
    private var silentFrames = 0

    override fun onCreate() {
        super.onCreate()
        instance = this
        startAsForeground()

        strip = LedStrip(this) { msg -> onStatus?.invoke(msg) }
        strip?.connect()

        cover = CoverListener(this) { c ->
            currentColor = c
            onColor?.invoke(c)
        }
        cover?.start()

        analyzer = AudioAnalyzer { bass, overall -> handleLevel(bass, overall) }
        analyzer?.start()
    }

    private fun handleLevel(bass: Float, overall: Float) {
        onBass?.invoke(bass)

        // silencio: apagar las luces
        if (overall < 0.02f && bass < 0.02f) silentFrames++ else silentFrames = 0
        if (silentFrames > 20) {
            strip?.setColor(0, 0, 0)
            return
        }

        // el brillo sigue a los graves, con subida rápida y bajada suave
        smooth = if (bass > smooth) bass else smooth * 0.82f + bass * 0.18f
        val level = (0.06f + smooth * 0.94f).coerceIn(0f, 1f)

        val c = currentColor
        strip?.setColor(
            (Color.red(c) * level).toInt(),
            (Color.green(c) * level).toInt(),
            (Color.blue(c) * level).toInt()
        )
    }

    private fun startAsForeground() {
        val channelId = "luces_musica"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Luces Música", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Luces Música activo")
            .setContentText("Las luces siguen tu música")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(open)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        analyzer?.stop()
        cover?.stop()
        strip?.setColor(0, 0, 0)
        strip?.disconnect()
        instance = null
        super.onDestroy()
    }
}
