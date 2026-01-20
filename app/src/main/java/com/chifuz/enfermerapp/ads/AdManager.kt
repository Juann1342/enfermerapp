package com.chifuz.enfermerapp.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.chifuz.enfermerapp.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback


object AdsManager {
    private var mInterstitialAd: InterstitialAd? = null
    private const val TAG = "AdsManager"

    // --- LÓGICA DE CONTROL ---
    private const val MAX_ADS_PER_HOUR = 5
    private const val COOLDOWN_MILLIS = 2 * 60 * 1000L // 2 minutos en milisegundos
    private const val HOUR_MILLIS = 60 * 60 * 1000L    // 1 hora en milisegundos

    private val adTimestamps = mutableListOf<Long>()
    private var lastAdShownTime: Long = 0

    fun loadInterstitial(context: Context) {
        // Usamos applicationContext para evitar fugas de memoria si la Activity se destruye
        val appContext = context.applicationContext
        val adRequest = AdRequest.Builder().build()
        val adId = BuildConfig.ADMOB_INTERSTITIAL_ID

        if (adId.isEmpty() || adId == "\"\"") {
            Log.e(TAG, "ID de anuncio no encontrado")
            return
        }

        InterstitialAd.load(appContext, adId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Error al cargar: ${adError.message}")
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Anuncio cargado con éxito.")
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun canShowAd(): Boolean {
        val now = System.currentTimeMillis()

        // 1. Limpiar historial: eliminamos marcas de más de una hora
        adTimestamps.removeAll { it < (now - HOUR_MILLIS) }

        // 2. Verificar límite de 4 por hora
        if (adTimestamps.size >= MAX_ADS_PER_HOUR) {
            Log.d(TAG, "Límite por hora alcanzado (${adTimestamps.size})")
            return false
        }

        // 3. Verificar descanso de 5 minutos entre anuncios
        if (now - lastAdShownTime < COOLDOWN_MILLIS) {
            Log.d(TAG, "En periodo de enfriamiento (cooldown)")
            return false
        }

        return true
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        if (mInterstitialAd != null && canShowAd()) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Anuncio cerrado.")
                    val now = System.currentTimeMillis()
                    lastAdShownTime = now
                    adTimestamps.add(now)

                    mInterstitialAd = null
                    loadInterstitial(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Log.e(TAG, "Fallo al mostrar: ${error.message}")
                    mInterstitialAd = null
                    onAdDismissed()
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Anuncio omitido por reglas de frecuencia o no cargado.")
            onAdDismissed()
        }
    }
}