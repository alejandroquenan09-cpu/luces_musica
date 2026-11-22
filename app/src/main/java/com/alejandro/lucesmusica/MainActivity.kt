package com.alejandro.lucesmusica

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import android.service.notification.NotificationListenerService

class MainActivity : AppCompatActivity() {

    private lateinit var smoke: SmokeView
    private lateinit var status: TextView
    private lateinit var button: Button
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        smoke = SmokeView(this)
        status = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            text = "Listo para empezar"
        }
        val title = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            text = "Luces Música"
        }
        button = Button(this).apply {
            text = "Iniciar"
            setOnClickListener { toggle() }
        }
        val access = Button(this).apply {
            text = "Dar acceso a notificaciones"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            addView(title)
            addView(status)
            addView(button)
            addView(access)
        }

        val root = FrameLayout(this)
        root.addView(smoke, ViewGroup.LayoutParams(-1, -1))
        root.addView(column, ViewGroup.LayoutParams(-1, -1))
        setContentView(root)

        LightService.onBass = { b -> runOnUiThread { smoke.setBass(b) } }
        LightService.onColor = { c -> runOnUiThread { smoke.setTargetColor(c) } }
        LightService.onStatus = { s -> runOnUiThread { status.text = s } }
    }

    private fun toggle() {
        if (running) {
            LightService.stop(this)
            running = false
            button.text = "Iniciar"
            status.text = "Detenido"
        } else {
            askPermissions()
        }
    }

    private fun askPermissions() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 31) {
            needed += Manifest.permission.BLUETOOTH_SCAN
            needed += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.POST_NOTIFICATIONS

        val missing = needed.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) begin() else ActivityCompat.requestPermissions(this, missing.toTypedArray(), 10)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == 10) {
            if (results.all { it == PackageManager.PERMISSION_GRANTED }) begin()
            else status.text = "Faltan permisos, acepta todos"
        }
    }

    private fun begin() {
        LightService.start(this)
        running = true
        button.text = "Detener"
        status.text = "Iniciando..."
    }
}
