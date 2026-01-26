import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
}


val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    // Usamos FileInputStream para ser más explícitos y evitar errores de stream
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.chifuz.enfermerapp"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    defaultConfig {
        applicationId = "com.chifuz.enfermerapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 19
        versionName = "1.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

// --- CONFIGURACIÓN DE ADMOB ---
        val appId = localProperties.getProperty("ADMOB_APP_ID") ?: ""

        // Leemos los 3 IDs de local.properties
        val adIdDosis = localProperties.getProperty("ADMOB_INTERSTITIAL_ID_DOSIS") ?: ""
        val adIdEdad = localProperties.getProperty("ADMOB_INTERSTITIAL_ID_EDAD") ?: ""
        val adIdPerfusion = localProperties.getProperty("ADMOB_INTERSTITIAL_ID_PERFUSION") ?: ""
        val adNativeIdEdad = localProperties.getProperty("ADMOB_NATIVE_ID_EDAD") ?: ""
        val rewardedId = localProperties.getProperty("ADMOB_REWARDED_ID") ?: ""

        val adNativeIdNotas = localProperties.getProperty("ADMOB_NATIVE_ID_NOTAS") ?: ""
        val adIdUnits = localProperties.getProperty("ADMOB_INTERSTITIAL_ID_UNITS") ?: ""
        val nativeIdUnits = localProperties.getProperty("ADMOB_NATIVE_ID_UNITS") ?: ""

        buildConfigField("String", "ADMOB_INTERSTITIAL_ID_UNITS", "\"$adIdUnits\"")
        buildConfigField("String", "ADMOB_NATIVE_ID_UNITS", "\"$nativeIdUnits\"")
        buildConfigField("String", "ADMOB_NATIVE_ID_NOTAS", "\"$adNativeIdNotas\"")

        // Los exponemos a Kotlin mediante BuildConfig
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID_DOSIS", "\"$adIdDosis\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID_EDAD", "\"$adIdEdad\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID_PERFUSION", "\"$adIdPerfusion\"")

        buildConfigField("String", "ADMOB_REWARDED_ID", "\"$rewardedId\"")
        // Mantenemos el ID original para no romper nada que use la variable vieja (opcional/seguridad)
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$adIdDosis\"")
        buildConfigField("String", "ADMOB_NATIVE_ID_EDAD", "\"$adNativeIdEdad\"")

        manifestPlaceholders["admobAppId"] = appId
    }

    // ... resto del archivo
    buildFeatures {
        compose = true
        buildConfig = true // Asegúrate de que esto esté en true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //viewmodel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    //iconos
    implementation("androidx.compose.material:material-icons-extended")

    // Google Play Services Ads (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
}