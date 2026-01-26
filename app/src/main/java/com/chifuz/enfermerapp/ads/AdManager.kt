package com.chifuz.enfermerapp.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.chifuz.enfermerapp.BuildConfig
import com.chifuz.enfermerapp.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

enum class AdLocation {
    DOSIS, EDAD, PERFUSION,UNITS
}

object AdsManager {
    // Mantenemos mInterstitialAd para DOSIS (retrocompatibilidad)
    private var mInterstitialAd: InterstitialAd? = null
    private var mInterstitialAdEdad: InterstitialAd? = null
    private var mInterstitialAdPerfusion: InterstitialAd? = null

    private var mInterstitialAdUnits: InterstitialAd? = null

    private const val TAG = "AdsManager"

    private const val MAX_ADS_PER_HOUR = 5
    private const val COOLDOWN_MILLIS = 2 * 60 * 1000L
    private const val HOUR_MILLIS = 40 * 60 * 1000L

    private val adTimestamps = mutableListOf<Long>()
    private var lastAdShownTime: Long = 0

    private var mRewardedAd: RewardedAd? = null
    private const val PREFS_NAME = "enfermerapp_prefs"
    private const val KEY_AD_FREE_UNTIL = "ad_free_until"


    // Verifica si el usuario está en periodo libre de anuncios
    fun isPremiumActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adFreeUntil = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)
        return System.currentTimeMillis() < adFreeUntil
    }

    // Añade esto a tu AdsManager.kt
    fun getRemainingHours(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adFreeUntil = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)
        val diff = adFreeUntil - System.currentTimeMillis()
        return if (diff > 0) (diff / (1000 * 40 * 60)).toInt() else 0
    }

    // Guarda el tiempo (12 horas desde ahora)
    private fun setAdFreePeriod(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val twelveHoursInMs = 4 * 60 * 60 * 1000L // 4 horas
        val newTime = System.currentTimeMillis() + twelveHoursInMs
        prefs.edit().putLong(KEY_AD_FREE_UNTIL, newTime).apply()
    }

    fun getRemainingTimeFormatted(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adFreeUntil = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)
        val diffMs = adFreeUntil - System.currentTimeMillis()

        if (diffMs <= 0) return "0 min"

        val totalMinutes = diffMs / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours > 0) {
            // Ejemplo: "5 h 45 min"
            context.getString(R.string.tiempo_formato_horas_minutos, hours, minutes)
        } else {
            // Ejemplo: "45 min"
            context.getString(R.string.tiempo_formato_minutos, minutes)
        }
    }

    fun loadRewarded(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, BuildConfig.ADMOB_REWARDED_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedAd = null
            }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
            }
        })
    }

    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit) {
        if (mRewardedAd != null) {
            mRewardedAd?.show(activity) {
                // El usuario vio el video completo
                setAdFreePeriod(activity)
                onRewardEarned()
                mRewardedAd = null
                loadRewarded(activity) // Recargar para la próxima
            }
        } else {
            // Si no hay anuncio cargado, intentamos cargar uno para la próxima
            loadRewarded(activity)
        }
    }


    // Función privada para no repetir lógica de IDs
    private fun getAdUnitId(location: AdLocation): String {
        return when (location) {
            AdLocation.DOSIS -> BuildConfig.ADMOB_INTERSTITIAL_ID_DOSIS
            AdLocation.EDAD -> BuildConfig.ADMOB_INTERSTITIAL_ID_EDAD
            AdLocation.PERFUSION -> BuildConfig.ADMOB_INTERSTITIAL_ID_PERFUSION
            AdLocation.UNITS -> BuildConfig.ADMOB_INTERSTITIAL_ID_UNITS
        }
    }

    private fun getAdVariable(location: AdLocation): InterstitialAd? {
        return when (location) {
            AdLocation.DOSIS -> mInterstitialAd
            AdLocation.EDAD -> mInterstitialAdEdad
            AdLocation.PERFUSION -> mInterstitialAdPerfusion
            AdLocation.UNITS -> mInterstitialAdUnits
        }
    }

    private fun setAdVariable(location: AdLocation, ad: InterstitialAd?) {
        when (location) {
            AdLocation.DOSIS -> mInterstitialAd = ad
            AdLocation.EDAD -> mInterstitialAdEdad = ad
            AdLocation.PERFUSION -> mInterstitialAdPerfusion = ad
            AdLocation.UNITS -> mInterstitialAdUnits = ad
        }
    }

    // MODIFICADO: location al final con default para no romper llamadas viejas
    fun loadInterstitial(context: Context, location: AdLocation = AdLocation.DOSIS) {
        val appContext = context.applicationContext
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            appContext,
            getAdUnitId(location),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    setAdVariable(location, null)
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    setAdVariable(location, interstitialAd)
                }
            }
        )
    }

    private fun canShowAd(context: Context): Boolean {
        // 2. Verificar primero si el modo concentración está activo
        if (isPremiumActive(context)) {
            Log.d(TAG, "Modo concentración activo: omitiendo anuncio.")
            return false
        }

        val now = System.currentTimeMillis()
        adTimestamps.removeAll { now - it > HOUR_MILLIS }

        if (adTimestamps.size >= MAX_ADS_PER_HOUR) {
            Log.d(TAG, "Límite global por hora alcanzado")
            return false
        }

        if (now - lastAdShownTime < COOLDOWN_MILLIS) {
            Log.d(TAG, "En periodo de enfriamiento global")
            return false
        }

        return true
    }

    /**
     * Versión compatible con el código existente.
     * Si llamas a AdsManager.showInterstitial(activity) { ... } funcionará para DOSIS.
     */
    fun showInterstitial(
        activity: Activity,
        location: AdLocation = AdLocation.DOSIS,
        onAdDismissed: () -> Unit
    ) {
        val adToShow = getAdVariable(location)

        // Aquí pasamos 'activity' como argumento
        if (adToShow != null && canShowAd(activity)) {
            adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    val now = System.currentTimeMillis()
                    lastAdShownTime = now
                    adTimestamps.add(now)

                    setAdVariable(location, null)
                    loadInterstitial(activity, location)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    setAdVariable(location, null)
                    onAdDismissed()
                }
            }
            adToShow.show(activity)
        } else {
            onAdDismissed()
        }
    }
}