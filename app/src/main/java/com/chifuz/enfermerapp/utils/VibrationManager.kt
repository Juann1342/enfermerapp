package com.chifuz.enfermerapp.utils


import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

// Para cargar recursos 'R' debemos usar un nombre de paquete dummy ya que el IDE no lo conoce
// En un proyecto real, R se importaría automáticamente
// Usaremos la reflexión para acceder a R.raw.click_sound si existe, o un valor de fallback (0)
private val CLICK_SOUND_ID: Int = try {
    val packageName = "com.chifuz.enfermerapp" // Usamos el nombre de paquete base
    val resId = Class.forName("$packageName.R\$raw")
        .getField("click_sound")
        .getInt(null)
    resId
} catch (e: Exception) {
    0 // Fallback si no se encuentra R.raw.click_sound
}

// Esta clase maneja la vibración y el sonido (simularemos un sonido de 'click' simple).
// Implementa DefaultLifecycleObserver para liberar recursos al destruir el componente.
class VibrationManager(private val context: Context) : DefaultLifecycleObserver {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Usaremos SoundPool para latencia baja (ideal para este tipo de feedback)
    private val soundPool: SoundPool
    private var soundId: Int = 0 // ID del sonido de 'click'

    init {
        // Configuramos SoundPool
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1) // Solo necesitamos un stream para el click
            .setAudioAttributes(attributes)
            .build()

        // *** CAMBIO APLICADO: Cargar el recurso de sonido ***
        if (CLICK_SOUND_ID != 0) {
            soundId = soundPool.load(context, CLICK_SOUND_ID, 1)
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun doHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Vibración simple y corta (ideal para un click)
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    fun playClickSound() {
        // *** CAMBIO APLICADO: Reproducir el sonido ***
        if (soundId != 0) {
            // Reproducir sonido (volume 1.0, priority 0, loop 0 (no loop), rate 1.0)
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    // --- LifecycleObserver methods ---

    override fun onDestroy(owner: LifecycleOwner) {
        // Liberar SoundPool para evitar fugas de memoria
        soundPool.release()
    }
}