package de.cemaras.yem1n.logik

/**
 * Adressen der CloudKit-Web-Services. Dieselbe Datenbank wie in der iOS-App.
 * Die App liest hier nur noch öffentliche, unterschriebene Klassenpakete. Familien laufen über den Familien-Dienst.
 */
object CloudAdresse {
    const val CONTAINER = "iCloud.de.cemaras.yem1n"
    const val HOST = "https://api.apple-cloudkit.com"

    /**
     * Adresse für eine Operation, zum Beispiel "records/query".
     * [umgebung] ist "development" oder "production", [datenbank] meist "public".
     */
    fun url(
        operation: String, apiToken: String,
        umgebung: String = "development", datenbank: String = "public",
    ): String = "$HOST/database/1/$CONTAINER/$umgebung/$datenbank/$operation?ckAPIToken=$apiToken"
}
