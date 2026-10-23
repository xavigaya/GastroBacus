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
        toca("1"); toca("2")
        captura("02-pin")
        toca("3"); toca("4")
        toca("Cervesa Moritz 33 cl"); toca("Cervesa Moritz 33 cl"); toca("Estrella Damm 33 cl")
        captura("03-apuntar")
        toca("Apuntar al meu compte")
        captura("04-apuntat")
        toca("Apuntar més")
        captura("05-avui")
        toca("El meu consum")
        captura("06-consum")
        toca("Reserves")
        captura("07-reserves")
        toca("Àpats")
        captura("08-apats")
        toca("Sopar de caragols")
        captura("09-apat")
    }

    @Test fun recorregutJunta() {
        entraCom("Jordi")
        toca("Gestió")
        captura("10-gestio-resum")
        listOf("Consumicions", "Productes", "Estoc", "Socis", "Tancament de mes", "Còpies", "Configuració").forEachIndexed { i, s ->
            regla.onAllNodesWithText(s, substring = true)[0].performClick()
            captura("%02d-gestio-%s".format(11 + i, s.lowercase().replace(" ", "-").replace("ò", "o").replace("ó", "o")))
        }
    }
}
