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
import com.chifuz.enfermerapp.utils.PrefsManager
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform


class MainActivity : ComponentActivity() {

    var isPrivacyOptionsRequired by mutableStateOf(false)
        private set

    var darkModeState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Cargar preferencias y aplicar idioma ANTES de setContent
        darkModeState = PrefsManager.isDarkMode(this)
        updateLocale(PrefsManager.getLang(this))

        setContent {
            EnfermerAppTheme(darkTheme = darkModeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        onThemeChanged = { enabled ->
                            darkModeState = enabled
                            PrefsManager.setDarkMode(this, enabled)
                        },
                        onLangChanged = { lang ->
                            PrefsManager.setLang(this, lang)
                            // Recreamos para aplicar el nuevo Locale a nivel sistema
                            recreate()
                        }
                    )
                }
            }
        }

        consentimientoAds()
    }

    // Única función updateLocale, optimizada para Compose y Android moderno
    private fun updateLocale(lang: String) {
        val locale = if (lang.contains("-")) {
            val parts = lang.split("-")
            java.util.Locale(parts[0], parts[1]) // Esto creará pt_BR correctamente
        } else {
            java.util.Locale(lang)
        }

        java.util.Locale.setDefault(locale)

        val resources = resources
        val config = resources.configuration
        config.setLocale(locale)

        // Sincronización de contextos
        baseContext.resources.updateConfiguration(config, resources.displayMetrics)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun consentimientoAds() {
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
                    isPrivacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

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