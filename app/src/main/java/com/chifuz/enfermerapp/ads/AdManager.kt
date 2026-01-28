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
    DOSIS, DOSIS_PESO, EDAD, PERFUSION, UNITS
}

object AdsManager {
    private var mInterstitialAd: InterstitialAd? = null
    private var mInterstitialAdEdad: InterstitialAd? = null
    private var mInterstitialAdPerfusion: InterstitialAd? = null
    private var mInterstitialAdUnits: InterstitialAd? = null
    private var mInterstitialAdDosisPeso: InterstitialAd? = null

    private var isRewardedLoading = false

    private const val TAG = "AdsManager"

    // --- CONFIGURACIÓN DE TIEMPOS (Refinado a 4 Horas) ---
    private const val MAX_ADS_PER_PERIOD = 5
    private const val COOLDOWN_MILLIS = 2 * 60 * 1000L // 2 minutos entre anuncios
    private const val FOUR_HOURS_MILLIS = 4 * 60 * 60 * 1000L // Ventana de 4 horas

    private val adTimestamps = mutableListOf<Long>()
    private var lastAdShownTime: Long = 0

    private var mRewardedAd: RewardedAd? = null
    private const val PREFS_NAME = "enfermerapp_prefs"
    private const val KEY_AD_FREE_UNTIL = "ad_free_until"

    fun isPremiumActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adFreeUntil = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)
        return System.currentTimeMillis() < adFreeUntil
    }

    /**
     * Calcula cuántas horas completas quedan de modo concentración.
     */
    fun getRemainingHours(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adFreeUntil = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)
        val diff = adFreeUntil - System.currentTimeMillis()
        // Usamos 60 minutos reales para el cálculo de "horas"
        return if (diff > 0) (diff / (1000 * 60 * 60)).toInt() else 0
    }

    /**
     * Activa el periodo libre de anuncios por 4 horas.
     */
    private fun setAdFreePeriod(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newTime = System.currentTimeMillis() + FOUR_HOURS_MILLIS
        prefs.edit().putLong(KEY_AD_FREE_UNTIL, newTime).apply()
        Log.d(TAG, "Modo concentración activado por 4 horas.")
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
            context.getString(R.string.tiempo_formato_horas_minutos, hours, minutes)
        } else {
            context.getString(R.string.tiempo_formato_minutos, minutes)
        }
    }

    // ... (loadRewarded y showRewarded se mantienen igual, llamando a setAdFreePeriod)
    fun loadRewarded(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, BuildConfig.ADMOB_REWARDED_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { mRewardedAd = null }
            override fun onAdLoaded(rewardedAd: RewardedAd) { mRewardedAd = rewardedAd }
        })
    }

    fun showRewarded(activity: Activity, onAdAvailable: (Boolean) -> Unit, onRewardEarned: () -> Unit) {
        if (isPremiumActive(activity)) {
            onRewardEarned()
            return
        }

        if (mRewardedAd != null) {
            // Si ya existe, lo mostramos directo
            mRewardedAd?.show(activity) {
                setAdFreePeriod(activity)
                onRewardEarned()
                mRewardedAd = null
                loadRewarded(activity)
            }
        } else {
            // Si no existe, avisamos que vamos a intentar cargar
            if (!isRewardedLoading) {
                onAdAvailable(false) // Indica a la UI que empiece a cargar (el loader)
                loadRewardedWithCallback(activity) { success ->
                    if (success) {
                        showRewarded(activity, onAdAvailable, onRewardEarned)
                    } else {
                        // Si después de cargar sigue nulo (timeout o error)
                        onAdAvailable(true) // Avisamos para ocultar loader y mostrar error
                    }
                }
            }
        }
    }

    private fun loadRewardedWithCallback(context: Context, onResult: (Boolean) -> Unit) {
        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()

        // Timeout de seguridad: si en 8 segundos no cargó, cancelamos
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (isRewardedLoading) {
                isRewardedLoading = false
                onResult(false)
            }
        }
        handler.postDelayed(timeoutRunnable, 8000)

        RewardedAd.load(context, BuildConfig.ADMOB_REWARDED_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isRewardedLoading = false
                handler.removeCallbacks(timeoutRunnable)
                mRewardedAd = null
                onResult(false)
            }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                isRewardedLoading = false
                handler.removeCallbacks(timeoutRunnable)
                mRewardedAd = rewardedAd
                onResult(true)
            }
        })
    }

    // --- LÓGICA DE INTERSTITIALS ---

    private fun getAdUnitId(location: AdLocation): String = when (location) {
        AdLocation.DOSIS -> BuildConfig.ADMOB_INTERSTITIAL_ID_DOSIS
        AdLocation.EDAD -> BuildConfig.ADMOB_INTERSTITIAL_ID_EDAD
        AdLocation.PERFUSION -> BuildConfig.ADMOB_INTERSTITIAL_ID_PERFUSION
        AdLocation.UNITS -> BuildConfig.ADMOB_INTERSTITIAL_ID_UNITS
        AdLocation.DOSIS_PESO -> BuildConfig.ADMOB_INTERSTITIAL_ID_DOSIS_PESO
    }

    private fun getAdVariable(location: AdLocation): InterstitialAd? = when (location) {
        AdLocation.DOSIS -> mInterstitialAd
        AdLocation.EDAD -> mInterstitialAdEdad
        AdLocation.PERFUSION -> mInterstitialAdPerfusion
        AdLocation.UNITS -> mInterstitialAdUnits
        AdLocation.DOSIS_PESO -> mInterstitialAdDosisPeso
    }

    private fun setAdVariable(location: AdLocation, ad: InterstitialAd?) {
        when (location) {
            AdLocation.DOSIS -> mInterstitialAd = ad
            AdLocation.EDAD -> mInterstitialAdEdad = ad
            AdLocation.PERFUSION -> mInterstitialAdPerfusion = ad
            AdLocation.UNITS -> mInterstitialAdUnits = ad
            AdLocation.DOSIS_PESO -> mInterstitialAdDosisPeso = ad
        }
    }

    fun loadInterstitial(context: Context, location: AdLocation = AdLocation.DOSIS) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context.applicationContext, getAdUnitId(location), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) { setAdVariable(location, null) }
                override fun onAdLoaded(interstitialAd: InterstitialAd) { setAdVariable(location, interstitialAd) }
            }
        )
    }

    private fun canShowAd(context: Context): Boolean {
        if (isPremiumActive(context)) return false

        val now = System.currentTimeMillis()
        // Limpiamos timestamps fuera de la ventana de 4 horas
        adTimestamps.removeAll { now - it > FOUR_HOURS_MILLIS }

        if (adTimestamps.size >= MAX_ADS_PER_PERIOD) {
            Log.d(TAG, "Límite de anuncios ($MAX_ADS_PER_PERIOD) en 4 horas alcanzado")
            return false
        }

        if (now - lastAdShownTime < COOLDOWN_MILLIS) {
            Log.d(TAG, "Periodo de enfriamiento activo")
            return false
        }

        return true
    }

    fun showInterstitial(activity: Activity, location: AdLocation = AdLocation.DOSIS, onAdDismissed: () -> Unit) {
        val adToShow = getAdVariable(location)

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