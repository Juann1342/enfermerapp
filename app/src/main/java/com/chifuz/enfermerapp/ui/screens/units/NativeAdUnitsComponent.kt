package com.chifuz.enfermerapp.ui.screens.units

import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chifuz.enfermerapp.BuildConfig
import com.chifuz.enfermerapp.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdUnitsComponent() {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }


    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_ID_UNITS) // O _NOTAS
            .forNativeAd { ad ->
                // 1. IMPORTANTE: Si ya había un anuncio cargándose, lo destruimos antes de poner el nuevo
                nativeAd?.destroy()
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdMob", "Error cargando anuncio: ${error.message}")
                }
            }).build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            Log.d("AdMob", "Limpiando recursos al salir")
            // 2. IMPORTANTE: Destruimos el anuncio y ponemos la variable en null
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    nativeAd?.let { ad ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx).inflate(R.layout.ad_unified_small, null) as NativeAdView
                    view.headlineView = view.findViewById(R.id.ad_headline)
                    view.bodyView = view.findViewById(R.id.ad_body)
                    view.callToActionView = view.findViewById(R.id.ad_call_to_action)
                    view.iconView = view.findViewById(R.id.ad_app_icon)

                    (view.headlineView as TextView).text = ad.headline
                    (view.bodyView as TextView).text = ad.body
                    (view.callToActionView as Button).text = ad.callToAction
                    ad.icon?.let { (view.iconView as ImageView).setImageDrawable(it.drawable) }

                    view.setNativeAd(ad)
                    view
                },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}