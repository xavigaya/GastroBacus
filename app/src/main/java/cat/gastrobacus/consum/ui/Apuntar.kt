package cat.gastrobacus.consum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.Categoria
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.ItemApunt
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.ZONA
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.dades.hora
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Apuntar(model: AppModel, estat: Estat, soci: Soci, onApuntat: (Long) -> Unit) {
    var cat by remember { mutableStateOf<Categoria?>(null) }
    val cistella = remember { mutableStateListOf<ItemApunt>() }
    var perA by remember { mutableStateOf<String?>(null) } // null = el soci
    val convidats = remember { mutableStateListOf<String>() }
    var demanaConvidat by remember { mutableStateOf(false) }
    var ara by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(15_000); ara = System.currentTimeMillis() } }

    val productes = estat.productes.filter { it.actiu && (cat == null || it.categoria == cat) }
        .sortedWith(compareBy({ it.categoria.ordinal }, { it.nom }))
    fun preuDe(i: ItemApunt) = estat.producte(i.producteId)?.preuPer(i.convidat != null) ?: 0
    val total = cistella.sumOf { it.quantitat * preuDe(it) }
    fun canvia(producteId: String, convidat: String?, delta: Int) {
        val i = cistella.indexOfFirst { it.producteId == producteId && it.convidat == convidat }
        if (i < 0) { if (delta > 0) cistella += ItemApunt(producteId, delta, convidat) }
        else {
            val q = cistella[i].quantitat + delta
            if (q <= 0) cistella.removeAt(i) else cistella[i] = cistella[i].copy(quantitat = q)
        }
    }
    val iniciDia = LocalDate.now(ZONA).atStartOfDay(ZONA).toInstant().toEpochMilli()
    val avui = estat.consums.filter { it.sociId == soci.id && it.ts >= iniciDia }.sortedByDescending { it.ts }
    val minuts = estat.config.minutsDesfer

    Row(Modifier.fillMaxSize().padding(start = 24.dp, top = 20.dp, end = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Titol("Què prens, ${soci.nom}?")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pindola("Tot", cat == null, { cat = null })
                Categoria.entries.forEach { c -> Pindola(c.nom, cat == c, { cat = c }, punt = colorCategoria(c)) }
            }
            LazyVerticalGrid(
                GridCells.Adaptive(170.dp), Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                items(productes, key = { it.id }) { p ->
                    val q = cistella.filter { it.producteId == p.id && it.convidat == perA }.sumOf { it.quantitat }
                    val sense = p.controlaEstoc && p.estoc <= 0
                    Box(
                        Modifier.fillMaxWidth().height(124.dp).clip(RoundedCornerShape(14.dp)).background(C.superficie)
                            .border(2.dp, if (q > 0) C.most else C.vora, RoundedCornerShape(14.dp))
                            .clickable { canvia(p.id, perA, 1) },
                    ) {
                        Box(Modifier.width(8.dp).fillMaxHeight().background(colorCategoria(p.categoria)))
                        Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(p.nom, style = MaterialTheme.typography.titleMedium, maxLines = 2, color = if (sense) C.roure2 else C.roure)
                            Column {
                                XifraText(euros(p.preuPer(perA != null)), 24.sp)
                                when {
                                    sense -> Text("Sense estoc", style = MaterialTheme.typography.bodySmall, color = C.error, fontWeight = FontWeight.Bold)
                                    p.calReposar -> Suau("En queden ${p.estoc}", mida = 13.sp)
                                }
                            }
                        }
                        if (q > 0) Box(
                            Modifier.align(Alignment.TopEnd).padding(8.dp).size(34.dp).clip(CircleShape).background(C.most),
                            contentAlignment = Alignment.Center,
                        ) { XifraText(q.toString(), 17.sp, C.sobreMost) }
                    }
                }
            }
        }

        // ---------- El compte ----------
        Column(Modifier.width(360.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
            Bloc(Modifier.fillMaxWidth()) {
                Text("Per a qui és?", style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pindola("Per a mi", perA == null, { perA = null }, ambre = true)
                    convidats.forEach { c -> Pindola(c, perA == c, { perA = c }, ambre = true) }
                    Pindola("+ Convidat", false, { demanaConvidat = true })
                }
                if (perA != null) Suau("Va al teu compte, a preu de convidat.")
                HorizontalDivider(color = C.vora)
                if (cistella.isEmpty()) Suau("Toca un producte per afegir-lo.")
                cistella.toList().forEach { i ->
                    val p = estat.producte(i.producteId) ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotoRodo(Icons.Default.Remove, "Treu un ${p.nom}") { canvia(i.producteId, i.convidat, -1) }
                        Column(Modifier.weight(1f)) {
                            Text("${i.quantitat} × ${p.nom}", style = MaterialTheme.typography.bodyMedium)
                            if (i.convidat != null) Suau(i.convidat, mida = 13.sp)
                        }
                        BotoRodo(Icons.Default.Add, "Afegeix un ${p.nom}") { canvia(i.producteId, i.convidat, 1) }
                        XifraText(euros(i.quantitat * preuDe(i)), 16.sp)
                    }
                }
                HorizontalDivider(color = C.roure, thickness = 2.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    XifraText(euros(total), 30.sp)
                }
                Boto(
                    "Apuntar al meu compte",
                    { val items = cistella.toList(); model.accioAmb({ model.repo.apuntar(soci.id, items) }) { t -> cistella.clear(); perA = null; ara = System.currentTimeMillis(); onApuntat(t) } },
                    Modifier.fillMaxWidth(), gran = true, enabled = cistella.isNotEmpty() && !model.ocupat,
                )
            }
            if (avui.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Bloc(Modifier.fillMaxWidth()) {
                    Text("Avui", style = MaterialTheme.typography.titleLarge)
                    avui.forEach { c ->
                        val potDesfer = !c.anulat && ara - c.ts < minuts * 60_000L
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${hora(c.ts)}  ${if (c.quantitat > 1) "${c.quantitat} × " else ""}${c.nomProducte}",
                                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (c.anulat) TextDecoration.LineThrough else null),
                                    color = if (c.anulat) C.roure2 else C.roure,
                                )
                                if (c.convidat != null) Suau(c.convidat, mida = 13.sp)
                            }
                            if (potDesfer) Boto("Desfer", { model.accio("Consumició desfeta") { model.repo.anular(c.id, soci.id, false) } }, tipus = Tipus.FANTASMA, petit = true, icona = Icons.AutoMirrored.Filled.Undo)
                            else XifraText(euros(c.import), 15.sp, if (c.anulat) C.roure2 else C.roure)
                        }
                    }
                    Suau("Pots desfer una consumició durant $minuts minuts. Després, parla amb la junta.", mida = 13.sp)
                }
            }
        }
    }

    if (demanaConvidat) {
        var nom by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { demanaConvidat = false },
            title = { Text("Afegir un convidat", style = MaterialTheme.typography.headlineMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Camp("Nom del convidat", nom, { nom = it }, placeholder = "Per exemple, Marc")
                    Suau("El que prengui el convidat s'apuntarà al teu compte.")
                }
            },
            confirmButton = {
                Boto("Afegir", {
                    val n = nom.trim()
                    if (n !in convidats) convidats += n
                    perA = n; demanaConvidat = false
                }, enabled = nom.isNotBlank())
            },
            dismissButton = { Boto("Cancel·lar", { demanaConvidat = false }, tipus = Tipus.FANTASMA) },
            containerColor = C.superficie,
        )
    }
}

@Composable
fun BotoRodo(icona: androidx.compose.ui.graphics.vector.ImageVector, descripcio: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).border(2.dp, C.vora, CircleShape).background(C.superficie).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icona, descripcio, Modifier.size(20.dp), tint = C.roure) }
}
