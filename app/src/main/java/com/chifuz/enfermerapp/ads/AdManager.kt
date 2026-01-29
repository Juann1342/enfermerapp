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

    // --- NUEVA CONFIGURACIÓN DE TIEMPOS SEPARADA ---
    private const val MAX_INTERSTITIALS_PER_HOUR = 5
    private const val COOLDOWN_MILLIS = 2 * 60 * 1000L      // 2 minutos entre anuncios
    private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L    // Ventana de 1 hora para el límite de 5
    private const val FOUR_HOURS_MILLIS = 4 * 60 * 60 * 1000L // Duración del Modo Concentración

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

    fun getRemainingHours(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adFreeUntil = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)
        val diff = adFreeUntil - System.currentTimeMillis()
        return if (diff > 0) (diff / (1000 * 60 * 60)).toInt() else 0
    }

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
            context.getString(R.string.tiempo_formato_horas_minutos, hours.toInt(), minutes.toInt())
        } else {
            context.getString(R.string.tiempo_formato_minutos, minutes.toInt())
        }
    }

    // --- REWARDED LOGIC ---
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
            mRewardedAd?.show(activity) {
                setAdFreePeriod(activity)
                onRewardEarned()
                mRewardedAd = null
                loadRewarded(activity)
            }
        } else {
            if (!isRewardedLoading) {
                onAdAvailable(false)
                loadRewardedWithCallback(activity) { success ->
                    if (success) {
                        showRewarded(activity, onAdAvailable, onRewardEarned)
                    } else {
                        onAdAvailable(true)
                    }
                }
            }
        }
    }

    private fun loadRewardedWithCallback(context: Context, onResult: (Boolean) -> Unit) {
        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
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

    // --- INTERSTITIAL LOGIC ---
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
        // ESCENARIO 3: Si está el modo concentración (4 horas), no mostramos NADA.
        if (isPremiumActive(context)) {
            Log.d(TAG, "Modo concentración activo: No se muestra Interstitial.")
            return false
        }

        val now = System.currentTimeMillis()

        // ESCENARIO 2: Máximo 5 por HORA.
        // Limpiamos los registros que tengan más de 1 hora (no 4).
        adTimestamps.removeAll { now - it > ONE_HOUR_MILLIS }

        if (adTimestamps.size >= MAX_INTERSTITIALS_PER_HOUR) {
            Log.d(TAG, "Límite de 5 anuncios por hora alcanzado.")
            return false
        }

        // ESCENARIO 1: Uno cada 2 minutos (Cooldown).
        if (now - lastAdShownTime < COOLDOWN_MILLIS) {
            Log.d(TAG, "Periodo de enfriamiento de 2 min activo.")
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