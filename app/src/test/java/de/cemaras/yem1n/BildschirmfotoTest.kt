package de.cemaras.yem1n

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import de.cemaras.yem1n.logik.VorschulArt
import de.cemaras.yem1n.ui.App
import de.cemaras.yem1n.ui.AppViewModel
import de.cemaras.yem1n.ui.theme.YEM1NTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Macht Bildschirmfotos der wichtigsten Seiten, damit sich das Design ohne Gerät prüfen lässt.
 * Läuft nur mit `./gradlew testDebugUnitTest -Pfotos`, die Fotos liegen danach in `app/build/fotos`.
 * Die App läuft dabei in Robolectric, es wird nichts gesendet und nichts auf einem Gerät verändert.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-420dpi")
class BildschirmfotoTest {
    @get:Rule
    val regel = createComposeRule()

    private val beispiel = """
        {"klasse":"Klasse 3","fach":"Mathe","arbeit":"Beispiel","uebungen":[
          {"titel":"Halbieren und Zehner","aufgaben":[
            {"art":"zahl","frage":"Halbiere die Zahl.","rechnung":"48","antwort":"24","erklaerung":"48 : 2 = 24"},
            {"art":"zahl","frage":"Welcher Zehner kommt davor?","rechnung":"67","antwort":"60"},
            {"art":"zahl","frage":"Rechne aus.","rechnung":"7 · 8","antwort":"56"}]},
          {"titel":"Lesen und wählen","aufgaben":[
            {"art":"wahl","frage":"Lies genau: Tim hat 24 Murmeln und schenkt seiner Schwester die Hälfte. Wie viele Murmeln hat Tim danach noch?","antwort":"12","optionen":["12","14","48"]},
            {"art":"vergleich","frage":"Welches Zeichen passt?","rechnung":"48 ? 84","antwort":"<","hinweis":"Schau zuerst auf die Zehner."},
            {"art":"text","frage":"Schreibe das Wort richtig.","rechnung":"der hunt","antwort":"Hund"}]}]}
    """.trimIndent()

    private fun foto(name: String) {
        regel.mainClock.advanceTimeBy(1200)
        regel.onRoot().captureRoboImage("build/fotos/$name.png")
    }

    private fun alleSeiten(vorne: String, rosa: Boolean = false) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = AppViewModel(app)
        regel.mainClock.autoAdvance = false
        regel.setContent { YEM1NTheme { App(vm) } }
        foto("$vorne-00-einrichtung")
        vm.einrichtungAbschliessen("kind", "Yemin", "3", false, rosa, 2, false)
        vm.setzeFarbwelt(rosa)
        vm.setzeVorlesen(true)
        vm.fuegeArbeitEin(beispiel)
        foto("$vorne-01-start")
        val nr = vm.arbeiten.indexOfFirst { it.titel == "Beispiel" }
        vm.oeffne(nr)
        foto("$vorne-02-uebungen")
        vm.startUebung(0)
        foto("$vorne-03-ueben")
        vm.antworte("25")
        vm.antworte("26")
        foto("$vorne-04-ueben-falsch")
        vm.weiter()
        vm.antworte("60")
        foto("$vorne-05-ueben-richtig")
        vm.weiter()
        vm.zeigeLoesung()
        foto("$vorne-06-ueben-loesung")
        vm.weiter()
        foto("$vorne-07-ergebnis")
        vm.zurueck()
        vm.startUebung(1)
        foto("$vorne-08-ueben-lang")
        vm.zeigeLoesung()
        vm.weiter()
        foto("$vorne-09-ueben-vergleich")
        vm.zeigeLoesung()
        vm.weiter()
        foto("$vorne-10-ueben-text")
        vm.zurueck()
        vm.zurueck()
        vm.oeffneVorschule()
        foto("$vorne-11-vorschule")
        vm.startVorschul(VorschulArt.ZAEHLEN)
        foto("$vorne-12-vorschulspiel")
        vm.zurueck()
        vm.zurueck()
        vm.oeffneLernwelt()
        foto("$vorne-13-lernwelt")
        vm.zurueck()
        vm.oeffneHaustier()
        foto("$vorne-14-haustier")
        vm.zurueck()
        vm.oeffneErfolge()
        foto("$vorne-15-erfolge")
        vm.zurueck()
        vm.oeffneEinstellungen()
        foto("$vorne-16-einstellungen")
    }

    @Test
    fun handy() = alleSeiten("handy")

    @Test
    fun handyRosa() = alleSeiten("rosa", rosa = true)

    @Test
    fun handyGrosseSchrift() {
        RuntimeEnvironment.setFontScale(1.5f)
        alleSeiten("gross")
    }

    @Test
    @Config(sdk = [35], qualifiers = "w800dp-h1280dp-240dpi")
    fun tablet() = alleSeiten("tablet")
}
