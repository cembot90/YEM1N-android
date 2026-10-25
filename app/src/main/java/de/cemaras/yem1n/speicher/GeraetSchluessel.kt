package de.cemaras.yem1n.speicher

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import de.cemaras.yem1n.logik.FamilienKrypto
import de.cemaras.yem1n.logik.GeraetSignierer
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * Der Schlüssel dieses Geräts für die Familie. Er wird im Android Keystore erzeugt (wenn möglich im Sicherheitschip)
 * und verlässt das Gerät nie. Er kommt in keine Sicherung. Wer das Gerät wechselt, koppelt neu.
 */
object GeraetSchluessel : GeraetSignierer {
    private const val ALIAS = "yem1n-familie"
    private const val KEYSTORE = "AndroidKeyStore"

    private fun speicher(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    fun vorhanden(): Boolean = try { speicher().containsAlias(ALIAS) } catch (e: Exception) { false }

    /** Legt ein neues Schlüsselpaar an und gibt den öffentlichen Teil zurück. Ein altes Paar wird vorher gelöscht. */
    fun neuErzeugen(): ByteArray? {
        loeschen()
        return try {
            erzeugen(sicherheitschip = true)
        } catch (e: Exception) {
            try { erzeugen(sicherheitschip = false) } catch (e2: Exception) { null }
        }
    }

    private fun erzeugen(sicherheitschip: Boolean): ByteArray? {
        val b = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
        if (sicherheitschip && Build.VERSION.SDK_INT >= 28) b.setIsStrongBoxBacked(true)
        val gen = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
        gen.initialize(b.build())
        gen.generateKeyPair()
        return oeffentlich()
    }

    override fun oeffentlich(): ByteArray? = try {
        val zert = speicher().getCertificate(ALIAS)
        if (zert == null) null else FamilienKrypto.pubAusSpki(zert.publicKey.encoded)
    } catch (e: Exception) {
        null
    }

    override fun signieren(daten: ByteArray): ByteArray? = try {
        val schluessel = speicher().getKey(ALIAS, null) as? PrivateKey
        if (schluessel == null) null else Signature.getInstance("SHA256withECDSA").run {
            initSign(schluessel)
            update(daten)
            sign()
        }
    } catch (e: Exception) {
        null
    }

    fun loeschen() {
        try { speicher().deleteEntry(ALIAS) } catch (e: Exception) { /* war nichts da */ }
    }
}
