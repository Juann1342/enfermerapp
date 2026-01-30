package com.chifuz.enfermerapp.utils

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.chifuz.enfermerapp.R

class VibrationManager(private val context: Context) : DefaultLifecycleObserver {

    private val TAG = "VibrationManager"

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val soundPool: SoundPool
    private var soundId: Int = 0
    private var isSoundLoaded: Boolean = false

    init {
        val attributes = AudioAttributes.Builder()
            // CAMBIO: Usamos USAGE_MEDIA para saltar el bloqueo de "No molestar"
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attributes)
            .build()

        // El resto del cargado se mantiene igual
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                isSoundLoaded = true
            }
        }
        soundId = soundPool.load(context, R.raw.click_sound, 1)
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun doHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en vibración: ${e.message}")
        }
    }

    fun playClickSound() {
        if (isSoundLoaded && soundId != 0) {
            // Los parámetros 1f, 1f son volumen izquierdo y derecho (0.0 a 1.0)
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        soundPool.release()
    }
}