# --- REGLAS PARA SONIDO Y ATRIBUTOS ---
# Evita que se eliminen o cambien de nombre los recursos de sonido
-keep class com.chifuz.enfermerapp.R$raw { *; }
-keepclassmembers class **.R$raw {
    public static <fields>;
}

# Protege las clases de audio para que SoundPool y los atributos funcionen en Release
-keep class android.media.SoundPool { *; }
-keep class android.media.AudioAttributes* { *; }
-keep class android.media.AudioAttributes$Builder { *; }

# --- REGLAS PARA ROOM (Tus Notas) ---
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep class com.chifuz.enfermerapp.data.model.** { *; }
-keep interface com.chifuz.enfermerapp.data.dao.** { *; }

# --- REGLAS PARA ADMOB ---
-keep public class com.google.android.gms.ads.** { *; }
-keep public class com.google.ads.** { *; }

# --- REGLAS PARA CONFIGURACIÓN (BuildConfig) ---
-keep class **.BuildConfig { *; }
# --- REGLAS PARA VIEWMODELS ---
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}