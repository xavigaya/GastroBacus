package cat.gastrobacus.consum

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import cat.gastrobacus.consum.dades.DadesProva
import cat.gastrobacus.consum.ui.AppModel
import cat.gastrobacus.consum.ui.TemaGastroBacus
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Recorre les pantalles principals amb dades de prova i en desa captures a build/captures.
 * Serveix per revisar el disseny sense tauleta ni emulador.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-land-mdpi", application = Application::class)
class PantallesTest {
    @get:Rule val regla = createAndroidComposeRule<ComponentActivity>()
    private lateinit var model: AppModel
    private val dir = File(System.getProperty("captures.dir") ?: "build/captures").apply { mkdirs() }

    @Before fun prepara() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        model = AppModel(app)
        runBlocking { model.repo.carregaDadesProva() }
        regla.setContent { TemaGastroBacus { App(model) } }
        espera()
    }

    private fun espera() {
        regla.waitForIdle()
        Thread.sleep(300)
        regla.waitForIdle()
    }

    private fun captura(nom: String) {
        espera()
        val v = regla.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        regla.runOnUiThread { v.draw(android.graphics.Canvas(bmp)) }
        File(dir, "$nom.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun toca(text: String) { regla.onNodeWithText(text, substring = false).performClick(); espera() }

    private fun entraCom(nom: String) {
        toca(nom)
        DadesProva.PIN.forEach { toca(it.toString()) }
    }

    @Test fun recorregutSoci() {
        captura("01-qui-ets")
        toca("Anna")
        toca("1"); toca("2"); toca("3"); toca("4")
        captura("02-apuntar-favorits")
        toca("Tot")
        captura("03-apuntar-per-tipus")
        toca("Cervesa Moritz 33 cl"); toca("Cervesa Moritz 33 cl")
        toca("Pagar ara per Bizum")
        regla.onNodeWithText("Ja l'he fet").assertExists()
        regla.onNodeWithText("600 123 456").assertExists()
        captura("04-bizum")
        toca("Cancel·lar")
        toca("Falta alguna cosa?")
        regla.onNodeWithText("Proposar-ne un de nou").assertExists()
        captura("05-falta")
        toca("Cancel·lar")
        toca("Àpats")
        captura("06-activitats")
        toca("Arrossada popular")
        captura("07-activitat-oberta")
        toca("Reserves")
        captura("08-reserves")
    }

    @Test fun recorregutPagament() {
        toca("Ramon")
        DadesProva.PIN.forEach { toca(it.toString()) }
        captura("20-avis-deute")
        toca("El meu compte")
        captura("21-el-meu-compte")
        regla.onAllNodesWithText("Pagar")[0].performClick(); espera()
        regla.onNodeWithText("Registrar el pagament").assertExists()
        regla.onNodeWithText("ES00 1234 5678 9012 3456 7890").assertExists()
        toca("Registrar el pagament")
        captura("22-pagament-registrat")
        toca("Àpats")
        toca("Calçotada de la colla")
        captura("23-sopar-socis-efectiu")
    }

    @Test fun recorregutConvidat() {
        toca("Marc")
        captura("09-convidat")
    }

    @Test fun recorregutJunta() {
        entraCom("Jordi")
        toca("Gestió")
        captura("10-gestio-resum")
        listOf("Estoc", "Comandes", "Cobraments", "Socis", "Configuració").forEachIndexed { i, s ->
            regla.onAllNodesWithText(s, substring = true)[0].performClick()
            captura("%02d-gestio-%s".format(11 + i, s.lowercase().replace("ó", "o")))
        }
        regla.onAllNodesWithText("Estoc", substring = true)[0].performClick(); espera()
        regla.onAllNodesWithText("Comanda", substring = false, useUnmergedTree = true)[0].performClick()
        espera()
        regla.onNodeWithText("Registrar la comanda").assertExists()
        regla.onNodeWithText("Recompte").assertExists()
    }
}
