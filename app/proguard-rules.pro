# Keep-Regeln für den Release-Bau mit Verkleinerung (R8).

# Sprachausgabe: Die Bibliothek ruft ihre Klassen aus nativem Code, deshalb darf nichts umbenannt oder entfernt werden.
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclasseswithmembernames class * { native <methods>; }

# Ed25519 für die Unterschrift der Klassenpakete
-keep class net.i2p.crypto.eddsa.** { *; }
-dontwarn sun.security.x509.X509Key

# kotlinx.serialization: Namen der Felder müssen stimmen, sonst passt das JSON nicht mehr
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class de.cemaras.yem1n.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class de.cemaras.yem1n.**$$serializer { *; }

# KI auf dem Gerät und Scanner: bringen ihre eigenen Regeln mit
-dontwarn com.google.mlkit.**

# WorkManager (kommt über ML Kit und FCM mit) legt seine Room-Datenbank per Namen an.
# Ohne diese Regeln stürzt die Release-App beim Start ab (WorkDatabase_Impl fehlt).
-keep class * extends androidx.room.RoomDatabase { <init>(...); }
-keep class androidx.work.impl.WorkDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class * extends androidx.startup.Initializer { <init>(); }
-dontwarn androidx.room.paging.**
