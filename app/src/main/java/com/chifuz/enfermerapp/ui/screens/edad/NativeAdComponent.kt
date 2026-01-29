package com.chifuz.enfermerapp.ui.screens.edad

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chifuz.enfermerapp.BuildConfig
import com.chifuz.enfermerapp.R
import com.chifuz.enfermerapp.ads.AdsManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdEdad(modifier: Modifier = Modifier) {
    val context = LocalContext.current
//    if (AdsManager.isPremiumActive(context)) return
    val adStatus = remember { mutableStateOf<NativeAd?>(null) }

    // Carga del anuncio nativo
    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_ID_EDAD)
            .forNativeAd { ad ->
                // Si ya había un anuncio (por una carga previa lenta), lo destruimos
                adStatus.value?.destroy()
                adStatus.value = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdsManager", "Error cargando nativo: ${error.message}")
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            // Al salir de la pantalla, destruimos el anuncio actual y limpiamos el estado
            adStatus.value?.destroy()
            adStatus.value = null
        }
    }

    adStatus.value?.let { ad ->
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            AndroidView(
                factory = { ctx ->
                    // Inflamos el layout directamente
                    val view = LayoutInflater.from(ctx).inflate(R.layout.ad_unified_small, null) as NativeAdView

                    // Vinculamos los componentes usando los IDs que pusimos en el XML
                    view.headlineView = view.findViewById(R.id.ad_headline)
                    view.bodyView = view.findViewById(R.id.ad_body)
                    view.callToActionView = view.findViewById(R.id.ad_call_to_action)
                    view.iconView = view.findViewById(R.id.ad_app_icon)

                    // Asignamos el contenido
                    (view.headlineView as TextView).text = ad.headline
                    (view.bodyView as TextView).text = ad.body
                    (view.callToActionView as Button).text = ad.callToAction

                    ad.icon?.let {
                        (view.iconView as ImageView).setImageDrawable(it.drawable)
                    }

                    // Muy importante: decirle al NativeAdView cuál es el anuncio
                    view.setNativeAd(ad)

                    view
                },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}