package cat.gastrobacus.consum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.ui.gestio.Gestio
import kotlinx.coroutines.delay

enum class Seccio(val titol: String, val icona: ImageVector) {
    APUNTAR("Apuntar", Icons.Default.LocalBar),
    CONSUM("El meu consum", Icons.AutoMirrored.Filled.ListAlt),
    RESERVES("Reserves", Icons.Default.CalendarMonth),
    APATS("Àpats", Icons.Default.SoupKitchen),
    PIN("El meu PIN", Icons.Default.Dialpad),
    GESTIO("Gestió", Icons.Default.AdminPanelSettings),
}

private const val INACTIVITAT_MS = 3 * 60_000L

@Composable
fun AreaSoci(model: AppModel, estat: Estat, soci: Soci) {
    var seccio by rememberSaveable { mutableStateOf(Seccio.APUNTAR) }
    var apuntat by remember { mutableStateOf<Long?>(null) }
    var darrerToc by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Si ningú toca la tauleta, es tanca la sessió
    LaunchedEffect(darrerToc) {
        delay(INACTIVITAT_MS)
        model.surt()
    }
    // Després d'apuntar, torna a «Qui ets?» al cap d'uns segons
    LaunchedEffect(apuntat) {
        if (apuntat != null) { delay(6000); model.surt() }
    }

    val seccions = Seccio.entries.filter { it != Seccio.GESTIO || soci.esJunta }

    Column(
        Modifier.fillMaxSize().background(C.pedra).pointerInput(Unit) {
            awaitPointerEventScope { while (true) { awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial); darrerToc = System.currentTimeMillis() } }
        },
    ) {
        BarraSuperior {
            Text(soci.nomComplet, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Boto("Acabar", { model.surt() }, tipus = Tipus.CAPCALERA, petit = true, icona = Icons.AutoMirrored.Filled.Logout)
        }
        Row(Modifier.fillMaxSize()) {
            NavigationRail(Modifier.fillMaxHeight().width(112.dp), containerColor = C.superficie) {
                seccions.forEach { s ->
                    NavigationRailItem(
                        selected = s == seccio, onClick = { seccio = s },
                        icon = { Icon(s.icona, null) },
                        label = { Text(s.titol, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                        colors = NavigationRailItemDefaults.colors(selectedIconColor = C.most, selectedTextColor = C.most, indicatorColor = C.mostSuau, unselectedIconColor = C.roure2, unselectedTextColor = C.roure2),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            VerticalDivider(color = C.vora)
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (seccio) {
                    Seccio.APUNTAR -> Apuntar(model, estat, soci) { apuntat = it }
                    Seccio.CONSUM -> ElMeuConsum(estat, soci)
                    Seccio.RESERVES -> Reserves(model, estat, soci)
                    Seccio.APATS -> Apats(model, estat, soci)
                    Seccio.PIN -> CanviPin(model, soci)
                    Seccio.GESTIO -> Gestio(model, estat, soci)
                }
            }
        }
    }

    apuntat?.let { total ->
        AlertDialog(
            onDismissRequest = { apuntat = null },
            icon = { Avatar("✓", 72.dp, C.pampolSuau, C.pampol) },
            title = { Text("Apuntat, ${soci.nom}", style = MaterialTheme.typography.headlineMedium) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                    XifraText(euros(total), 38.sp)
                    Suau("Tornem a la llista de socis en uns segons.")
                }
            },
            confirmButton = { Boto("Fet", { apuntat = null; model.surt() }) },
            dismissButton = { Boto("Apuntar més", { apuntat = null }, tipus = Tipus.FANTASMA) },
            containerColor = C.superficie,
        )
    }
}

@Composable
fun CanviPin(model: AppModel, soci: Soci) {
    var actual by remember { mutableStateOf("") }
    var nou by remember { mutableStateOf("") }
    var repeteix by remember { mutableStateOf("") }
    fun xifres(s: String) = s.length <= 4 && s.all(Char::isDigit)
    Column(Modifier.padding(28.dp).width(460.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Titol("El meu PIN")
        Suau("El PIN és el que escrius a la tauleta per entrar. Fes-ne un que recordis i que no sigui fàcil d'endevinar.")
        Camp("PIN actual", actual, { if (xifres(it)) actual = it }, teclat = androidx.compose.ui.text.input.KeyboardType.NumberPassword, secret = true)
        Camp("PIN nou", nou, { if (xifres(it)) nou = it }, teclat = androidx.compose.ui.text.input.KeyboardType.NumberPassword, secret = true)
        Camp("Repeteix el PIN nou", repeteix, { if (xifres(it)) repeteix = it }, teclat = androidx.compose.ui.text.input.KeyboardType.NumberPassword, secret = true,
            ajuda = if (repeteix.length == 4 && repeteix != nou) "Els dos PIN nous no coincideixen." else null)
        Boto(
            "Canviar el PIN",
            { model.accio("PIN canviat", { actual = ""; nou = ""; repeteix = "" }) { model.repo.canviarPin(soci.id, actual, nou) } },
            enabled = actual.length == 4 && nou.length == 4 && nou == repeteix,
        )
    }
}

