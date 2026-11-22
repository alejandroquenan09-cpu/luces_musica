package com.alejandro.lucesmusica

import android.media.audiofx.Visualizer
import kotlin.math.sqrt

class AudioAnalyzer(private val onLevel: (bass: Float, overall: Float) -> Unit) {

    private var visualizer: Visualizer? = null
    private var peak = 0.05f

    fun start() {
        stop()
        try {
            val v = Visualizer(0)
            v.enabled = false
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vis: Visualizer?, wf: ByteArray?, rate: Int) {}

                override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, rate: Int) {
                    if (fft == null) return
                    process(fft, rate)
                }
            }, Visualizer.getMaxCaptureRate(), false, true)
            v.enabled = true
            visualizer = v
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun process(fft: ByteArray, rateMilliHz: Int) {
        val n = fft.size / 2
        val hzPerBin = (rateMilliHz / 1000f) / 2f / n
        var bass = 0f
        var all = 0f
        var bassBins = 0
        for (i in 1 until n) {
            val re = fft[2 * i].toFloat()
            val im = fft[2 * i + 1].toFloat()
            val mag = sqrt(re * re + im * im)
            val hz = i * hzPerBin
            all += mag
            if (hz in 30f..150f) {
                bass += mag
                bassBins++
            }
        }
        if (bassBins > 0) bass /= bassBins
        all /= n

        peak = maxOf(peak * 0.995f, bass, 1f)
        val b = (bass / peak).coerceIn(0f, 1f)
        val o = (all / 40f).coerceIn(0f, 1f)
        onLevel(b, o)
    }

    fun stop() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
    }
}
