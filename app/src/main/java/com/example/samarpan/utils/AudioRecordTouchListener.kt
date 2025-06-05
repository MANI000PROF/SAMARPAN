package com.example.samarpan.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import com.example.samarpan.WaveformView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AudioRecordTouchListener(
    private val context: Context,
    private val waveformView: WaveformView,
    private val recordingPopup: View,
    private val micAnimation: LottieAnimationView,
    private val timerTextView: TextView,
    private val swipeHint: TextView,
    private val onAudioRecorded: (String) -> Unit
) : View.OnTouchListener {


    private var recorder: MediaRecorder? = null
    private var recordingFilePath: String? = null
    private var startX = 0f
    private var isRecording = false
    private val cancelThreshold = 150 // px
    private val amplitudeHandler = Handler(Looper.getMainLooper())
    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            try {
                val amplitude = recorder?.maxAmplitude ?: 0
                waveformView.addAmplitude(amplitude)
            } catch (e: IllegalStateException) {
                // Skip if MediaRecorder is not in a valid state
            }

            if (isRecording) {
                amplitudeHandler.postDelayed(this, 100)
            }
        }
    }


    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startRecording()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val distanceX = startX - event.x
                if (isRecording && distanceX > cancelThreshold) {
                    cancelRecording()
                    Toast.makeText(context, "Recording canceled", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isRecording) {
                    stopRecording()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isRecording) {
                    cancelRecording()
                }
                return true
            }
        }
        return false
    }

    private fun startRecording() {
        try {
            val outputDir = File(context.cacheDir, "audio")
            if (!outputDir.exists()) outputDir.mkdirs()

            val fileName = "AUD_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.m4a"
            recordingFilePath = "${outputDir.absolutePath}/$fileName"

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(recordingFilePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                prepare()
                start()
            }

            isRecording = true
            recordingPopup.visibility = View.VISIBLE
            micAnimation.playAnimation()
            timerTextView.text = "00:00"
            swipeHint.visibility = View.VISIBLE
            startTimerForRecording()
            waveformView.reset()
            amplitudeHandler.post(amplitudeRunnable)
            Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
            isRecording = false
            recordingFilePath = null
        }
    }

    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            amplitudeHandler.removeCallbacks(amplitudeRunnable)
            stopTimer()
            recordingPopup.visibility = View.GONE
            micAnimation.cancelAnimation()
            recordingFilePath?.let { onAudioRecorded(it) }

        } catch (e: Exception) {
            Toast.makeText(context, "Error stopping recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Silently ignore if already stopped
        } finally {
            recorder = null
            isRecording = false
            amplitudeHandler.removeCallbacks(amplitudeRunnable)
            stopTimer()
            recordingPopup.visibility = View.GONE
            micAnimation.cancelAnimation()
            waveformView.reset()
            recordingFilePath?.let { File(it).delete() }
            recordingFilePath = null
        }
    }

    private var timer: CountDownTimer? = null
    private var elapsedSeconds = 0

    private fun startTimerForRecording() {
        elapsedSeconds = 0
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds++
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                timerTextView.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {}
        }.start()
    }

    private fun stopTimer() {
        timer?.cancel()
        timerTextView.text = "00:00"
    }

}
