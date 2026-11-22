package com.alejandro.lucesmusica

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import androidx.palette.graphics.Palette

class NotificationAccess : NotificationListenerService()

class CoverListener(private val context: Context, private val onColor: (Int) -> Unit) {

    private val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val component = ComponentName(context, NotificationAccess::class.java)
    private var controller: MediaController? = null
    private var lastTitle: String? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            readCover(metadata)
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        attach(list)
    }

    fun start() {
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, component)
            attach(manager.getActiveSessions(component))
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun attach(list: List<MediaController>?) {
        controller?.unregisterCallback(callback)
        val chosen = list?.firstOrNull { it.packageName.contains("metrolist", ignoreCase = true) }
            ?: list?.firstOrNull()
        controller = chosen
        chosen?.registerCallback(callback)
        readCover(chosen?.metadata)
    }

    private fun readCover(m: MediaMetadata?) {
        if (m == null) return
        val title = m.getString(MediaMetadata.METADATA_KEY_TITLE)
        if (title == lastTitle) return
        lastTitle = title
        val bmp: Bitmap = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: return
        Palette.from(bmp).maximumColorCount(16).generate { p ->
            val color = p?.vibrantSwatch?.rgb
                ?: p?.lightVibrantSwatch?.rgb
                ?: p?.darkVibrantSwatch?.rgb
                ?: p?.dominantSwatch?.rgb
                ?: return@generate
            onColor(color)
        }
    }

    fun stop() {
        controller?.unregisterCallback(callback)
        manager.removeOnActiveSessionsChangedListener(sessionsListener)
    }
}
