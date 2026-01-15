import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
        minSdk = 24
        targetSdk = 36
        versionCode = 13
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- CONFIGURACIÓN DE ADMOB ---
        // Leemos de local.properties. Si no existe, ponemos un valor vacío seguro.
        val interstitialId = localProperties.getProperty("ADMOB_INTERSTITIAL_ID") ?: ""
        val appId = localProperties.getProperty("ADMOB_APP_ID") ?: ""

        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$interstitialId\"")
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
}