package cat.gastrobacus.consum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.Franja
import cat.gastrobacus.consum.dades.Reserva
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.avui
import cat.gastrobacus.consum.dades.dataLlarga
import cat.gastrobacus.consum.dades.mesActual
import cat.gastrobacus.consum.dades.mesAnterior
import cat.gastrobacus.consum.dades.mesSeguent
import cat.gastrobacus.consum.dades.nomMes
import java.time.YearMonth

@Composable
fun Reserves(model: AppModel, estat: Estat, soci: Soci) {
    val avui = avui()
    var mes by remember { mutableStateOf(mesActual()) }
    var dia by remember { mutableStateOf(avui) }
    var franja by remember { mutableStateOf(Franja.SOPAR) }
    var espai by remember { mutableStateOf(estat.config.espais.firstOrNull() ?: "") }
    var persones by remember { mutableStateOf("8") }
    var notes by remember { mutableStateOf("") }
    var anulant by remember { mutableStateOf<Reserva?>(null) }

    val delDia = estat.reserves.filter { it.data == dia }.sortedBy { it.franja.ordinal }
    val properes = estat.reserves.filter { it.data >= avui && (soci.esJunta || it.sociId == soci.id) }.sortedBy { it.data }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Titol("Reserves")
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // Calendari
            Bloc(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotoRodo(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior") { mes = mesAnterior(mes) }
                    Text(nomMes(mes), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    BotoRodo(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes següent") { mes = mesSeguent(mes) }
                }
                Calendari(mes, dia, avui, estat) { dia = it }
            }
            // Dia triat
            Bloc(Modifier.weight(1f)) {
                Subtitol(dataLlarga(dia))
                if (delDia.isEmpty()) Suau("No hi ha cap reserva aquest dia.")
                delDia.forEach { r -> FilaReserva(estat, r, soci) { anulant = r } }
                HorizontalDivider(color = C.vora)
                Text("Fer una reserva", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Desplegable(Franja.entries, franja, { it.nom }, { franja = it }, Modifier.weight(1f), "Quan")
                    Camp("Persones", persones, { if (it.length <= 3 && it.all(Char::isDigit)) persones = it }, Modifier.weight(1f), teclat = KeyboardType.Number)
                }
                Desplegable(estat.config.espais, espai, { it }, { espai = it }, Modifier.fillMaxWidth(), "Espai")
                Camp("Notes", notes, { notes = it }, placeholder = "Opcional: motiu, necessitats…")
                Boto(
                    "Reservar",
                    { model.accio("Reserva feta", { notes = "" }) { model.repo.creaReserva(soci.id, dia, franja, espai, persones.toIntOrNull() ?: 0, notes) } },
                    enabled = dia >= avui && espai.isNotBlank() && (persones.toIntOrNull() ?: 0) > 0,
                )
            }
        }
        Subtitol(if (soci.esJunta) "Properes reserves" else "Les meves reserves")
        if (properes.isEmpty()) Suau("Cap reserva pendent.")
        else Bloc(Modifier.fillMaxWidth()) { properes.forEach { r -> FilaReserva(estat, r, soci, mostraData = true) { anulant = r } } }
    }

    anulant?.let { r ->
        Confirmacio(
            "Anul·lar la reserva", "${dataLlarga(r.data)}, ${r.franja.nom.lowercase()}: ${r.espai}.", "Anul·lar la reserva",
            onConfirma = { anulant = null; model.accio("Reserva anul·lada") { model.repo.eliminaReserva(r.id) } },
            onCancela = { anulant = null }, perill = true,
        )
    }
}

@Composable
private fun FilaReserva(estat: Estat, r: Reserva, soci: Soci, mostraData: Boolean = false, onAnula: () -> Unit) {
    val qui = estat.soci(r.sociId)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                (if (mostraData) dataLlarga(r.data) + ", " else "") + r.franja.nom.lowercase().replaceFirstChar { if (mostraData) it else it.uppercaseChar() },
                style = MaterialTheme.typography.titleMedium,
            )
            Suau("${r.espai}, ${r.persones} persones" + (if (qui != null && r.sociId != soci.id) ", ${qui.nomComplet}" else "") + (if (r.notes.isNotBlank()) ". ${r.notes}" else ""), mida = 14.sp)
        }
        if (r.sociId == soci.id || soci.esJunta) Boto("Anul·lar", onAnula, tipus = Tipus.FANTASMA, petit = true)
    }
}

@Composable
private fun Calendari(mes: String, triat: String, avui: String, estat: Estat, onTria: (String) -> Unit) {
    val ym = YearMonth.parse(mes)
    val primer = ym.atDay(1)
    val offset = primer.dayOfWeek.value - 1
    val n = ((offset + ym.lengthOfMonth() + 6) / 7) * 7
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("dl", "dt", "dc", "dj", "dv", "ds", "dg").forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = C.roure2, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        (0 until n step 7).forEach { setmana ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { d ->
                    val data = primer.plusDays((setmana + d - offset).toLong())
                    val iso = data.toString()
                    val fora = data.monthValue != ym.monthValue
                    val passat = iso < avui
                    val sel = iso == triat
                    val nRes = estat.reserves.count { it.data == iso }
                    Box(
                        Modifier.weight(1f).aspectRatio(1.15f).clip(RoundedCornerShape(8.dp))
                            .background(if (sel) C.most else C.superficie)
                            .border(2.dp, if (sel) C.most else if (iso == avui) C.roure else C.vora, RoundedCornerShape(8.dp))
                            .alpha(if (fora || passat) .35f else 1f)
                            .clickable(enabled = !passat) { onTria(iso) }.padding(6.dp),
                    ) {
                        Text(data.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, color = if (sel) C.sobreMost else C.roure)
                        if (nRes > 0) Row(Modifier.align(Alignment.BottomStart), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(minOf(nRes, 3)) { Box(Modifier.size(8.dp).clip(CircleShape).background(C.ambre)) }
                        }
                    }
                }
            }
        }
    }
}
