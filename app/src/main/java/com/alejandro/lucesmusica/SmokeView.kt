package com.alejandro.lucesmusica

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class SmokeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Blob(val phase: Float, val speed: Float, val radius: Float, val orbit: Float)

    private val blobs = List(7) { i ->
        Blob(
            phase = i * 0.9f,
            speed = 0.15f + (i % 4) * 0.07f,
            radius = 0.45f + (i % 3) * 0.15f,
            orbit = 0.18f + (i % 4) * 0.08f
        )
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var time = 0f
    private var color = Color.rgb(90, 90, 160)
    private var shownColor = color
    private var bass = 0f
    private var smoothBass = 0f

    fun setTargetColor(c: Int) { color = c }
    fun setBass(b: Float) { bass = b }

    private fun mix(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
    )

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(Color.rgb(8, 8, 12))

        time += 0.016f
        shownColor = mix(shownColor, color, 0.04f)
        smoothBass += (bass - smoothBass) * 0.25f

        val base = maxOf(w, h)
        for ((i, b) in blobs.withIndex()) {
            val t = time * b.speed + b.phase
            val cx = w / 2f + cos(t * 1.3f) * w * b.orbit * 1.6f
            val cy = h / 2f + sin(t) * h * b.orbit * 1.4f
            val r = base * b.radius * (0.8f + smoothBass * 0.5f)

            val tint = if (i % 2 == 0) shownColor else mix(shownColor, Color.WHITE, 0.35f)
            val alpha = (60 + smoothBass * 110).toInt().coerceIn(0, 255)
            val inner = Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint))
            val outer = Color.argb(0, Color.red(tint), Color.green(tint), Color.blue(tint))

            paint.shader = RadialGradient(cx, cy, r, inner, outer, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r, paint)
        }
        paint.shader = null
        postInvalidateOnAnimation()
    }
}
