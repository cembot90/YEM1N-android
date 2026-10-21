package de.cemaras.yem1n

import de.cemaras.yem1n.logik.CloudAdresse
import de.cemaras.yem1n.logik.CloudAuswertung
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudKitTest {
    @Test fun adresseOhneAnmeldung() {
        val u = CloudAdresse.url("users/current", "TOKEN")
        assertEquals(
            "https://api.apple-cloudkit.com/database/1/iCloud.de.cemaras.yem1n/development/public/users/current?ckAPIToken=TOKEN",
            u,
        )
    }

    @Test fun adresseMitAnmeldungKodiertSonderzeichen() {
        val u = CloudAdresse.url("records/query", "T", "a+b/c=", "production")
        assertTrue(u.contains("/production/public/records/query"))
        assertTrue(u.endsWith("&ckWebAuthToken=a%2Bb%2Fc%3D"))
    }

    @Test fun rueckkehrLiestSchluessel() {
        assertEquals("abc", CloudAdresse.tokenAusRueckkehr("yem1n://cloudkit?ckWebAuthToken=abc"))
    }

    @Test fun plusBleibtPlusUndProzentWirdEntkodiert() {
        assertEquals("a+b/c=", CloudAdresse.tokenAusRueckkehr("yem1n://cloudkit?ckWebAuthToken=a%2Bb%2Fc%3D"))
        assertEquals("a+b", CloudAdresse.tokenAusRueckkehr("yem1n://cloudkit?ckWebAuthToken=a+b"))
    }

    @Test fun rueckkehrOhneSchluesselIstNull() {
        assertNull(CloudAdresse.tokenAusRueckkehr("yem1n://cloudkit"))
        assertNull(CloudAdresse.tokenAusRueckkehr("yem1n://cloudkit?ckWebAuthToken="))
        assertNull(CloudAdresse.tokenAusRueckkehr("yem1n://cloudkit?anderes=1"))
    }

    @Test fun anmeldungNoetigWirdErkannt() {
        val a = CloudAuswertung.lies(
            """{"uuid":"x","serverErrorCode":"AUTHENTICATION_REQUIRED","reason":"request needs authorization","redirectURL":"https://apple.example/login"}""",
        )
        assertFalse(a.ok)
        assertTrue(a.anmeldungNoetig)
        assertEquals("https://apple.example/login", a.anmeldeUrl)
    }

    @Test fun erfolgBringtNutzerUndNeuenSchluessel() {
        val a = CloudAuswertung.lies("""{"userRecordName":"_abc123","ckWebAuthToken":"neu"}""")
        assertTrue(a.ok)
        assertEquals("_abc123", a.benutzer)
        assertEquals("neu", a.neuerToken)
    }

    @Test fun kaputteAntwortIstEinFehlerKeinAbsturz() {
        val a = CloudAuswertung.lies("<html>nein</html>")
        assertFalse(a.ok)
        assertEquals("UNGUELTIG", a.fehlerCode)
    }
}
