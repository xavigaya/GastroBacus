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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import cat.gastrobacus.consum.dades.ConvidatDia
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.ItemApunt
import cat.gastrobacus.consum.dades.Pagament
import cat.gastrobacus.consum.dades.Producte
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.TipusPeticio
import cat.gastrobacus.consum.dades.ZONA
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.dades.hora
import kotlinx.coroutines.delay
import java.time.LocalDate

private const val FAVORITS = "fav"
private const val TOT = "tot"

/**
 * Pantalla d'apuntar. Amb [convidat], la fa servir un convidat del dia: tot va al compte del soci que el porta.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Apuntar(model: AppModel, estat: Estat, soci: Soci, convidat: ConvidatDia? = null, onApuntat: (Long) -> Unit) {
    val favorits = estat.favoritsDe(soci.id)
    var filtre by remember { mutableStateOf(if (favorits.isNotEmpty()) FAVORITS else TOT) }
    val cistella = remember { mutableStateListOf<ItemApunt>() }
    var perA by remember { mutableStateOf(convidat?.nom) } // null = el soci
    var demanaConvidat by remember { mutableStateOf(false) }
    var bizum by remember { mutableStateOf(false) }
    var peticio by remember { mutableStateOf(false) }
    var pagant by remember { mutableStateOf(false) }
    var ara by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(15_000); ara = System.currentTimeMillis() } }

    val convidatsSoci = estat.convidatsActius(ara).filter { it.sociId == soci.id }
    val productes: List<Producte> = when (filtre) {
        FAVORITS -> favorits
        TOT -> estat.productes.filter { it.actiu }.sortedWith(compareBy({ it.categoria.ordinal }, { it.nom }))
        else -> estat.productes.filter { it.actiu && it.categoria.name == filtre }.sortedBy { it.nom }
    }
    fun preuDe(i: ItemApunt) = estat.producte(i.producteId)?.preuPer(i.convidat != null) ?: 0
    val total = cistella.sumOf { it.quantitat * preuDe(it) }
    fun canvia(producteId: String, conv: String?, delta: Int) {
        val i = cistella.indexOfFirst { it.producteId == producteId && it.convidat == conv }
        if (i < 0) { if (delta > 0) cistella += ItemApunt(producteId, delta, conv) }
        else {
            val q = cistella[i].quantitat + delta
            if (q <= 0) cistella.removeAt(i) else cistella[i] = cistella[i].copy(quantitat = q)
        }
    }
    val iniciDia = LocalDate.now(ZONA).atStartOfDay(ZONA).toInstant().toEpochMilli()
    val avui = estat.consums.filter { it.sociId == soci.id && it.ts >= iniciDia && (convidat == null || it.convidat == convidat.nom) }.sortedByDescending { it.ts }
    val minuts = estat.config.minutsDesfer
    fun fet(t: Long) {
        cistella.clear(); if (convidat == null) perA = null; ara = System.currentTimeMillis()
        model.enviaEnSegonPla()
        onApuntat(t)
    }

    Row(Modifier.fillMaxSize().padding(start = 24.dp, top = 20.dp, end = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Titol("Què prens, ${convidat?.nom ?: soci.nom}?", Modifier.weight(1f))
                TextButton({ peticio = true }) { Text("Falta alguna cosa?", style = MaterialTheme.typography.labelLarge, color = C.most) }
            }
            if (convidat == null && estat.deuteAntic(soci, ara)) {
                val c = estat.compte(soci)
                Avis("Tens ${euros(c.perPagar)} pendents de pagar des de fa ${c.diesDeute(ara)} dies.", ToAvis.ERROR) {
                    Boto("Pagar", { pagant = true }, petit = true)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (favorits.isNotEmpty()) Pindola("★ Favorits", filtre == FAVORITS, { filtre = FAVORITS })
                Pindola("Tot", filtre == TOT, { filtre = TOT })
                Categoria.entries.forEach { c -> Pindola(c.nom, filtre == c.name, { filtre = c.name }, punt = colorCategoria(c)) }
            }
            // Cada filtre té el seu desplaçament, perquè en canviar es vegi des de dalt
            val estatGraella = remember(filtre) { androidx.compose.foundation.lazy.grid.LazyGridState() }
            LazyVerticalGrid(
                GridCells.Adaptive(170.dp), Modifier.fillMaxSize(), state = estatGraella,
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                val grups = if (filtre == TOT) productes.groupBy { it.categoria } else mapOf<Categoria?, List<Producte>>(null to productes)
                grups.forEach { (cat, llista) ->
                    if (cat != null) item(key = "cap-${cat.name}", span = { GridItemSpan(maxLineSpan) }) {
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(colorCategoria(cat)))
                            Text(cat.nom, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    items(llista, key = { it.id }) { p ->
                        FitxaProducte(p, perA != null, cistella.filter { it.producteId == p.id && it.convidat == perA }.sumOf { it.quantitat }, p.favorit || (filtre == FAVORITS)) {
                            canvia(p.id, perA, 1)
                        }
                    }
                }
            }
        }

        // ---------- El compte ----------
        Column(Modifier.width(360.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
            Bloc(Modifier.fillMaxWidth()) {
                if (convidat == null) {
                    Text("Per a qui és?", style = MaterialTheme.typography.titleLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pindola("Per a mi", perA == null, { perA = null }, ambre = true)
                        convidatsSoci.forEach { c -> Pindola(c.nom, perA == c.nom, { perA = c.nom }, ambre = true) }
                        Pindola("+ Convidat", false, { demanaConvidat = true })
                    }
                    if (perA != null) Suau("Va al teu compte, a preu de convidat. $perA surt a la pantalla d'inici fins a les ${estat.config.horaFiDia} del matí i es pot apuntar sol.")
                } else {
                    Text("Convidat de ${soci.nomComplet}", style = MaterialTheme.typography.titleLarge)
                    Suau("El que apuntis va al compte de ${soci.nom}, a preu de convidat. També ho pots pagar ara per Bizum.")
                }
                HorizontalDivider(color = C.vora)
                if (cistella.isEmpty()) Suau("Toca un producte per afegir-lo.")
                cistella.toList().forEach { i ->
                    val p = estat.producte(i.producteId) ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotoRodo(Icons.Default.Remove, "Treu un ${p.nom}") { canvia(i.producteId, i.convidat, -1) }
                        Column(Modifier.weight(1f)) {
                            Text("${i.quantitat} × ${p.nom}", style = MaterialTheme.typography.bodyMedium)
                            if (i.convidat != null && convidat == null) Suau(i.convidat, mida = 13.sp)
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
                    if (convidat == null) "Apuntar al meu compte" else "Apuntar al compte de ${soci.nom}",
                    { val items = cistella.toList(); model.accioAmb({ model.repo.apuntar(soci.id, items) }) { t -> fet(t) } },
                    Modifier.fillMaxWidth(), gran = true, enabled = cistella.isNotEmpty() && !model.ocupat,
                )
                if (estat.config.telefonBizum.isNotBlank()) Boto(
                    "Pagar ara per Bizum", { bizum = true }, Modifier.fillMaxWidth(), tipus = Tipus.SECUNDARI, enabled = cistella.isNotEmpty() && !model.ocupat,
                )
            }
            if (avui.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Bloc(Modifier.fillMaxWidth()) {
                    Text("Avui", style = MaterialTheme.typography.titleLarge)
                    avui.forEach { c ->
                        val potDesfer = !c.anulat && c.pagament == Pagament.FACTURA && ara - c.ts < minuts * 60_000L
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${hora(c.ts)}  ${if (c.quantitat > 1) "${c.quantitat} × " else ""}${c.nomProducte}",
                                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (c.anulat) TextDecoration.LineThrough else null),
                                    color = if (c.anulat) C.roure2 else C.roure,
                                )
                                val detall = listOfNotNull(c.convidat.takeIf { convidat == null }, if (c.pagament == Pagament.BIZUM) "Bizum" else null).joinToString(", ")
                                if (detall.isNotBlank()) Suau(detall, mida = 13.sp)
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
                    Suau("El que prengui s'apuntarà al teu compte. Sortirà a la pantalla d'inici fins a les ${estat.config.horaFiDia} del matí perquè es pugui apuntar sol, sense PIN.")
                }
            },
            confirmButton = {
                Boto("Afegir", {
                    model.accioAmb({ model.repo.afegeixConvidat(soci.id, nom) }) { perA = it.nom; demanaConvidat = false }
                }, enabled = nom.isNotBlank())
            },
            dismissButton = { Boto("Cancel·lar", { demanaConvidat = false }, tipus = Tipus.FANTASMA) },
            containerColor = C.superficie,
        )
    }

    if (bizum) {
        val qui = convidat?.nom ?: soci.nomComplet
        val concepte = "${estat.config.nomSocietat} ${convidat?.nom ?: soci.nom}"
        AlertDialog(
            onDismissRequest = { bizum = false },
            title = { Text("Pagar per Bizum", style = MaterialTheme.typography.headlineMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Fes un Bizum de", style = MaterialTheme.typography.bodyLarge)
                    XifraText(euros(total), 44.sp, C.most)
                    Text("al telèfon", style = MaterialTheme.typography.bodyLarge)
                    XifraText(estat.config.telefonBizum, 34.sp)
                    Text("amb el concepte «$concepte».", style = MaterialTheme.typography.bodyLarge)
                    Suau("Quan l'hagis fet, prem «Ja l'he fet». No anirà a la factura del mes. La junta comprovarà que ha arribat; si no, s'hi afegirà.")
                }
            },
            confirmButton = {
                Boto("Ja l'he fet", {
                    val items = cistella.toList()
                    model.accioAmb({ model.repo.pagaAmbBizum(soci.id, qui, items, concepte) }) { b -> bizum = false; fet(b.import) }
                })
            },
            dismissButton = { Boto("Cancel·lar", { bizum = false }, tipus = Tipus.FANTASMA) },
            containerColor = C.superficie,
        )
    }

    if (peticio) DialegPeticio(model, estat, soci) { peticio = false }
    if (pagant) DialegPagar(model, estat, soci) { pagant = false }
}

@Composable
private fun FitxaProducte(p: Producte, convidat: Boolean, q: Int, estrella: Boolean, onClick: () -> Unit) {
    val sense = p.controlaEstoc && p.estoc <= 0
    Box(
        Modifier.fillMaxWidth().height(124.dp).clip(RoundedCornerShape(14.dp)).background(C.superficie)
            .border(2.dp, if (q > 0) C.most else C.vora, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    ) {
        Box(Modifier.width(8.dp).fillMaxHeight().background(colorCategoria(p.categoria)))
        Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(p.nom, style = MaterialTheme.typography.titleMedium, maxLines = 2, color = if (sense) C.roure2 else C.roure, modifier = Modifier.padding(end = if (q > 0) 30.dp else 0.dp))
            Column {
                XifraText(euros(p.preuPer(convidat)), 24.sp)
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
        else if (estrella && p.favorit) Icon(Icons.Default.Star, "Favorit", Modifier.align(Alignment.TopEnd).padding(10.dp).size(18.dp), tint = C.ambre)
    }
}

/** El soci avisa que falta un producte o en proposa un de nou. */
@Composable
private fun DialegPeticio(model: AppModel, estat: Estat, soci: Soci, onTanca: () -> Unit) {
    var tipus by remember { mutableStateOf(TipusPeticio.FALTA) }
    val productes = estat.productes.filter { it.actiu }.sortedBy { it.nom }
    var producte by remember { mutableStateOf(productes.firstOrNull { it.controlaEstoc && it.estoc <= 0 } ?: productes.firstOrNull()) }
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onTanca,
        title = { Text("Falta alguna cosa?", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Interruptor(listOf("S'ha acabat un producte", "Proposar-ne un de nou"), tipus.ordinal, { tipus = TipusPeticio.entries[it] })
                if (tipus == TipusPeticio.FALTA) Desplegable(productes, producte, { it?.nom ?: "" }, { producte = it }, Modifier.fillMaxWidth(), "Quin producte?")
                else Camp("Quin producte us agradaria tenir?", text, { text = it }, placeholder = "Per exemple, vermut negre de Reus")
                Suau("La junta ho veurà a la tauleta i en rebrà un correu.")
            }
        },
        confirmButton = {
            Boto("Enviar", {
                model.accio("Gràcies, la junta ho veurà", onTanca) {
                    model.repo.creaPeticio(soci.id, tipus, if (tipus == TipusPeticio.FALTA) producte?.id else null, if (tipus == TipusPeticio.FALTA) "" else text)
                    model.enviaEnSegonPla()
                }
            }, enabled = if (tipus == TipusPeticio.FALTA) producte != null else text.isNotBlank())
        },
        dismissButton = { Boto("Cancel·lar", onTanca, tipus = Tipus.FANTASMA) },
        containerColor = C.superficie,
    )
}

@Composable
fun BotoRodo(icona: androidx.compose.ui.graphics.vector.ImageVector, descripcio: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).border(2.dp, C.vora, CircleShape).background(C.superficie).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icona, descripcio, Modifier.size(20.dp), tint = C.roure) }
}
