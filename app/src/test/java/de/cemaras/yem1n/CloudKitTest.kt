package de.cemaras.yem1n

import de.cemaras.yem1n.logik.CloudAdresse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CloudKitTest {
    @Test fun adresseHatApiTokenUndUmgebung() {
        val u = CloudAdresse.url("records/query", "TOKEN")
        assertEquals(
            "https://api.apple-cloudkit.com/database/1/iCloud.de.cemaras.yem1n/development/public/records/query?ckAPIToken=TOKEN",
            u,
        )
    }

    @Test fun produktionWirdUebernommen() {
        val u = CloudAdresse.url("records/lookup", "T", "production")
        assertEquals(
            "https://api.apple-cloudkit.com/database/1/iCloud.de.cemaras.yem1n/production/public/records/lookup?ckAPIToken=T",
            u,
        )
    }

    @Test fun keineAnmeldungImLink() {
        assertFalse(CloudAdresse.url("records/query", "T", "production").contains("ckWebAuthToken"))
    }
}
