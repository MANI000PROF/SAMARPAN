package com.example.samarpan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val amplitudes = mutableListOf<Float>()
    private val path = Path()
    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    fun addAmplitude(amp: Int) {
        val normalized = (amp / 32767f) * height / 2 // Normalize based on max short value
        amplitudes.add(normalized)
        if (amplitudes.size > width) {
            amplitudes.removeAt(0) // Maintain fixed length for scrolling
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (amplitudes.isEmpty()) return

        path.reset()
        val midY = height / 2f
        val space = 1f // 1 pixel per amplitude

        path.moveTo(0f, midY)

        for (i in amplitudes.indices) {
            val x = i * space
            val y = midY - amplitudes[i]
            path.lineTo(x, y)
        }

        canvas.drawPath(path, paint)
    }

    fun reset() {
        amplitudes.clear()
        invalidate()
    }
}
