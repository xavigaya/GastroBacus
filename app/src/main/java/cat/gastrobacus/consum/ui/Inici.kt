package cat.gastrobacus.consum.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.google.Google
import kotlinx.coroutines.launch

/* ---------------- Configuració inicial (primera vegada) ---------------- */

@Composable
fun ConfiguracioInicial(model: AppModel) {
    var pas by remember { mutableStateOf("tria") }
    var copies by remember { mutableStateOf<List<Google.FitxerCopia>>(emptyList()) }
    var copiaTriada by remember { mutableStateOf<Google.FitxerCopia?>(null) }

    Box(Modifier.fillMaxSize().background(C.pedra).verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Logo(88.dp)
                Column {
                    Text("GastroBacus", style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp), color = C.most)
                    Suau("El compte de la societat, sense monedes.", mida = 18.sp)
                }
            }
            when (pas) {
                "tria" -> {
                    Subtitol("Com vols començar?")
                    OpcioInici(Icons.Default.CloudDownload, "Recuperar una còpia del Drive", "Si l'app ja funcionava en una altra tauleta. Cal iniciar la sessió amb el Gmail de la societat.") {
                        model.accioAmb({ model.serveis.iniciaSessioGoogle(); model.serveis.llistaCopies() }) {
                            copies = it
                            pas = "copies"
                        }
                    }
                    OpcioInici(Icons.Default.PersonAdd, "Començar de zero", "Crea el primer membre de la junta. Després podràs afegir socis i productes.") { pas = "zero" }
                    OpcioInici(Icons.Default.Science, "Provar amb dades fictícies", "24 socis, 30 productes i dos mesos de consumicions inventades. PIN de tots els socis: 1234.") {
                        model.accio("Dades de prova carregades") { model.repo.carregaDadesProva() }
                    }
                }
                "copies" -> {
                    Subtitol("Tria la còpia a recuperar")
                    if (copies.isEmpty()) Avis("No hi ha cap còpia de GastroBacus en aquest compte de Google.")
                    copies.take(30).forEach { f ->
                        Bloc(Modifier.fillMaxWidth().clickable { copiaTriada = f }, padding = 16.dp) {
                            Text(f.nom, style = MaterialTheme.typography.titleMedium)
                            Suau(dataCopia(f.creat) + " · " + "%.0f KB".format(f.mida / 1024.0))
                        }
                    }
                    Boto("Tornar", { pas = "tria" }, tipus = Tipus.FANTASMA)
                }
                "zero" -> PrimerSoci(model) { pas = "tria" }
            }
        }
    }
    copiaTriada?.let { f ->
        Confirmacio(
            "Recuperar la còpia", "Es carregaran les dades de la còpia ${f.nom}.", "Recuperar",
            onConfirma = { copiaTriada = null; model.accio("Còpia recuperada") { model.serveis.restauraCopia(f.id) } },
            onCancela = { copiaTriada = null },
        )
    }
}

fun dataCopia(iso: String): String = runCatching {
    cat.gastrobacus.consum.dades.dataHora(java.time.Instant.parse(iso).toEpochMilli())
}.getOrDefault(iso)

@Composable
private fun OpcioInici(icona: ImageVector, titol: String, text: String, onClick: () -> Unit) {
    Bloc(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icona, null, Modifier.size(44.dp), tint = C.most)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titol, style = MaterialTheme.typography.titleLarge)
                Suau(text)
            }
        }
    }
}

@Composable
private fun PrimerSoci(model: AppModel, onTorna: () -> Unit) {
    var nom by remember { mutableStateOf("") }
    var cognoms by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    Bloc(Modifier.fillMaxWidth()) {
        Subtitol("Primer membre de la junta")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Camp("Nom", nom, { nom = it }, Modifier.weight(1f))
            Camp("Cognoms", cognoms, { cognoms = it }, Modifier.weight(1f))
        }
        Camp("Correu", email, { email = it }, teclat = KeyboardType.Email)
        Camp("PIN de 4 xifres", pin, { if (it.length <= 4 && it.all(Char::isDigit)) pin = it }, teclat = KeyboardType.NumberPassword, secret = true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Boto("Tornar", onTorna, tipus = Tipus.FANTASMA)
            Boto("Començar", { model.accio("Benvinguts a GastroBacus") { model.repo.comencaDeZero(nom, cognoms, email, pin) } }, enabled = nom.isNotBlank() && pin.length == 4)
        }
    }
}

/* ---------------- Qui ets? ---------------- */

@Composable
fun QuiEts(model: AppModel, estat: Estat) {
    var lletra by remember { mutableStateOf<Char?>(null) }
    val socis = estat.socisActius
    val lletres = socis.map { it.nom.first().uppercaseChar() }.distinct()
    val llista = if (lletra == null) socis else socis.filter { it.nom.first().uppercaseChar() == lletra }

    Column(Modifier.fillMaxSize().background(C.pedra)) {
        BarraSuperior()
        Column(Modifier.padding(horizontal = 28.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Titol("Qui ets?")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Lletra("Tots", lletra == null) { lletra = null }
                lletres.forEach { l -> Lletra(l.toString(), lletra == l) { lletra = l } }
            }
            LazyVerticalGrid(
                GridCells.Adaptive(200.dp), Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(llista, key = { it.id }) { s -> FitxaSoci(s) { model.triaSoci(s.id) } }
            }
        }
    }
}

@Composable
private fun Lletra(t: String, sel: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(50.dp).widthIn(min = 50.dp).clip(RoundedCornerShape(10.dp))
            .background(if (sel) C.most else C.superficie).border(2.dp, if (sel) C.most else C.vora, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(t, style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp), color = if (sel) C.sobreMost else C.roure) }
}

@Composable
private fun FitxaSoci(s: Soci, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(14.dp)).background(C.superficie)
            .border(2.dp, C.vora, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(s.inicials, 50.dp)
        Column {
            Text(s.nom, style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp), maxLines = 1)
            Suau(s.cognoms, mida = 14.sp)
        }
    }
}

/* ---------------- PIN ---------------- */

@Composable
fun PantallaPin(model: AppModel, soci: Soci) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var intents by remember { mutableIntStateOf(0) }
    val tremola = remember { Animatable(0f) }
    val ambit = rememberCoroutineScope()

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (model.repo.verificarPin(soci.id, pin)) model.entra(soci.id)
            else {
                error = true; intents++; pin = ""
                ambit.launch {
                    for (x in listOf(-14f, 14f, -9f, 9f, 0f)) tremola.animateTo(x, tween(55))
                }
            }
        }
    }
    fun prem(d: String) { error = false; if (pin.length < 4) pin += d }

    Column(Modifier.fillMaxSize().background(C.pedra)) {
        BarraSuperior { Boto("No sóc jo", { model.surt() }, tipus = Tipus.CAPCALERA, petit = true, icona = Icons.AutoMirrored.Filled.ArrowBack) }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Avatar(soci.inicials, 80.dp)
            Titol(soci.nomComplet)
            Text(
                if (error) "PIN incorrecte. Torna-ho a provar." else "Escriu el teu PIN",
                style = MaterialTheme.typography.bodyLarge, color = if (error) C.error else C.roure2,
            )
            Row(Modifier.offset { IntOffset(tremola.value.dp.roundToPx(), 0) }, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                repeat(4) { i ->
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).border(3.dp, C.most, CircleShape)
                            .background(if (i < pin.length) C.most else androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { fila.forEach { d -> Tecla(d) { prem(d) } } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(88.dp))
                    Tecla("0") { prem("0") }
                    Box(
                        Modifier.size(88.dp).clip(CircleShape).clickable { pin = pin.dropLast(1) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.Backspace, "Esborra", Modifier.size(30.dp), tint = C.roure) }
                }
            }
            if (intents >= 3) Suau("Si no recordes el PIN, demana a la junta que te'l canviï.")
        }
    }
}

@Composable
private fun Tecla(d: String, onClick: () -> Unit) {
    Box(
        Modifier.size(88.dp).clip(CircleShape).background(C.superficie).border(2.dp, C.vora, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(d, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold)) }
}

