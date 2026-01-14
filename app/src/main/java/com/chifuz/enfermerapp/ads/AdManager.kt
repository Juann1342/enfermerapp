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

    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()

        // Ahora sí reconocerá ADMOB_INTERSTITIAL_ID
        val adId = BuildConfig.ADMOB_INTERSTITIAL_ID

        if (adId.isEmpty() || adId == "\"\"") {
            Log.e(TAG, "ID de anuncio no encontrado en BuildConfig")
            return
        }

        InterstitialAd.load(context, adId, adRequest, object : InterstitialAdLoadCallback() {
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

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Anuncio cerrado por el usuario.")
                    mInterstitialAd = null
                    loadInterstitial(activity) // Pre-carga el siguiente
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Log.e(TAG, "Fallo al mostrar anuncio: ${error.message}")
                    mInterstitialAd = null
                    onAdDismissed()
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            Log.d(TAG, "El anuncio no estaba listo.")
            onAdDismissed()
        }
    }
}