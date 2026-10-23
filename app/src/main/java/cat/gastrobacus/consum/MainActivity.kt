package cat.gastrobacus.consum

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.gastrobacus.consum.google.Google
import cat.gastrobacus.consum.ui.AppModel
import cat.gastrobacus.consum.ui.AreaSoci
import cat.gastrobacus.consum.ui.C
import cat.gastrobacus.consum.ui.ConfiguracioInicial
import cat.gastrobacus.consum.ui.Logo
import cat.gastrobacus.consum.ui.Pantalla
import cat.gastrobacus.consum.ui.PantallaPin
import cat.gastrobacus.consum.ui.QuiEts
import cat.gastrobacus.consum.ui.TemaGastroBacus

class Aplicacio : Application() {
    override fun onCreate() {
        super.onCreate()
        Serveis.de(this)
        programaCopiaDiaria(this)
    }
}

class MainActivity : ComponentActivity() {
    private val model: AppModel by viewModels()

    private val autoritzacio = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        Google.resultatAutoritzacio(this, r.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Quiosc.registra(this)
        Google.llancador = { autoritzacio.launch(it) }
        // Amaga les barres del sistema; es poden fer aparèixer lliscant des de la vora
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { TemaGastroBacus { App(model) } }
    }

    override fun onResume() {
        super.onResume()
        fixaSiCal()
    }

    fun fixaSiCal() {
        val cfg = model.estat.value
        if (cfg.carregat && cfg.config.fixarPantalla && !cfg.esBuida && !model.pantallaDesfixada) Quiosc.fixa()
    }
}

@Composable
fun App(model: AppModel) {
    val estat by model.estat.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { model.missatges.collect { avisos.showSnackbar(it) } }
    // Fixa la pantalla tan bon punt hi ha dades i la configuració ho demana
    LaunchedEffect(estat.carregat, estat.config.fixarPantalla, estat.esBuida, model.pantallaDesfixada) {
        if (estat.carregat && estat.config.fixarPantalla && !estat.esBuida && !model.pantallaDesfixada) Quiosc.fixa()
    }

    BackHandler(enabled = true) { if (model.pantalla !is Pantalla.QuiEts) model.surt() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(avisos) { d -> Snackbar(containerColor = C.roure, contentColor = C.pedra) { Text(d.visuals.message, style = androidx.compose.material3.MaterialTheme.typography.titleMedium) } }
        },
        containerColor = C.pedra,
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(C.pedra)) {
            when {
                !estat.carregat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Logo(96.dp) }
                estat.esBuida -> ConfiguracioInicial(model)
                else -> when (val p = model.pantalla) {
                    is Pantalla.QuiEts -> QuiEts(model, estat)
                    is Pantalla.Pin -> estat.soci(p.sociId)?.let { PantallaPin(model, it) } ?: QuiEts(model, estat)
                    is Pantalla.Area -> estat.soci(p.sociId)?.takeIf { it.actiu }?.let { AreaSoci(model, estat, it) } ?: QuiEts(model, estat)
                }
            }
            if (model.ocupat) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.most) }
        }
    }
}
