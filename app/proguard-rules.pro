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
