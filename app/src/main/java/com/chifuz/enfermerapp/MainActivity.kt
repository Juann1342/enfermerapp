package com.chifuz.enfermerapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chifuz.enfermerapp.ui.navigation.AppNavHost
import com.chifuz.enfermerapp.ui.theme.EnfermerAppTheme
import com.chifuz.enfermerapp.ads.AdsManager
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class MainActivity : ComponentActivity() {

    // Esta variable le dirá a tu Menú si debe mostrar el botón de privacidad o no
    var isPrivacyOptionsRequired by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EnfermerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pasamos el estado de privacidad al NavHost o lo manejamos desde aquí
                    AppNavHost()
                }
            }
        }

        consentimientoAds()

    }

    private fun consentimientoAds(){
        // 1. Configuración de UMP
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)

        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError ->
                    if (formError != null) {
                        Log.e("UMP", "${formError.errorCode}: ${formError.message}")
                    }

                    // DETERMINAMOS SI EL BOTÓN DEBE MOSTRARSE
                    // Si el estado es REQUIRED, el usuario está en Europa/EEUU y el botón aparecerá
                    isPrivacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

                    // 4. Verificamos si ya podemos cargar anuncios
                    if (consentInformation.canRequestAds()) {
                        initializeAds()
                    }
                }
            },
            { requestConsentError ->
                Log.e("UMP", "${requestConsentError.errorCode}: ${requestConsentError.message}")
                initializeAds()
            }
        )
    }

    private fun initializeAds() {
        MobileAds.initialize(this) {}
        AdsManager.loadInterstitial(this)
    }
}