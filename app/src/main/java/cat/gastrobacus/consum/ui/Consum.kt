package cat.gastrobacus.consum.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.EstatMes
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.deMes
import cat.gastrobacus.consum.dades.diaCurt
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.dades.mesActual
import cat.gastrobacus.consum.dades.mesAnterior
import cat.gastrobacus.consum.dades.mesDe

fun darrersMesos(n: Int = 12): List<String> = generateSequence(mesActual()) { mesAnterior(it) }.take(n).toList()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ElMeuConsum(model: AppModel, estat: Estat, soci: Soci) {
    var mes by remember { mutableStateOf(mesActual()) }
    var pagant by remember { mutableStateOf(false) }
    val compte = estat.compte(soci)
    val meves = estat.transferencies.filter { it.sociId == soci.id }
    val inf = estat.informe(soci, mes)
    val estatMes = estat.estatMes(mes)
    val perCat = estat.consums.filter { it.sociId == soci.id && !it.anulat && mesDe(it.ts) == mes }
        .groupBy { estat.producte(it.producteId)?.categoria }
        .mapValues { (_, l) -> l.sumOf { it.import } }.entries.sortedByDescending { it.value }
    val maxim = (perCat.maxOfOrNull { it.value } ?: 1L).coerceAtLeast(1L)

    if (pagant) DialegPagar(model, estat, soci) { pagant = false }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Titol("El meu compte", Modifier.weight(1f))
            }
        }
        if (!estat.config.rebutBancari || compte.perPagar > 0) item {
            val antic = estat.deuteAntic(soci)
            Bloc(Modifier.fillMaxWidth(), vora = if (antic) C.error else C.vora) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Suau(if (compte.perPagar > 0) "Pendent de pagar" else "Estàs al dia", mida = 18.sp)
                        XifraText(euros(maxOf(0, compte.perPagar)), 48.sp, if (antic) C.error else C.roure)
                        if (compte.perPagar > 0 && compte.deuteDesDe != null) Text(
                            "Des del ${cat.gastrobacus.consum.dades.dataPlena(compte.deuteDesDe)}" + if (antic) ". Fa més de ${estat.config.diesAvisDeute} dies: paga-ho tan aviat com puguis." else ".",
                            style = MaterialTheme.typography.bodyMedium, color = if (antic) C.error else C.roure2,
                        )
                        if (compte.pendentConfirmar > 0) Etiqueta("Transferència de ${euros(compte.pendentConfirmar)} pendent de comprovar", ToEtiqueta.AMBRE)
                    }
                    if (compte.perPagar > 0) Boto("Pagar", { pagant = true }, gran = true)
                }
                if (meves.isNotEmpty()) {
                    Text("Els meus pagaments", style = MaterialTheme.typography.titleMedium)
                    meves.take(5).forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${cat.gastrobacus.consum.dades.dataLlarga(t.data)}: ${euros(t.import)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            when (t.estat) {
                                cat.gastrobacus.consum.dades.EstatTransferencia.CONFIRMADA -> Etiqueta("Rebuda", ToEtiqueta.VERD)
                                cat.gastrobacus.consum.dades.EstatTransferencia.PENDENT -> Etiqueta("Pendent de comprovar", ToEtiqueta.AMBRE)
                                cat.gastrobacus.consum.dades.EstatTransferencia.REBUTJADA -> Etiqueta("No ha arribat", ToEtiqueta.VERMELL)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Subtitol("Consum del mes", Modifier.weight(1f))
                SelectorMes(mes, darrersMesos(), { mes = it })
            }
        }
        item {
            Bloc(Modifier.fillMaxWidth()) {
                Suau(
                    when (estatMes) {
                        EstatMes.OBERT -> "Portes gastat ${deMes(mes)}"
                        EstatMes.INFORMAT -> "Consum ${deMes(mes)} (informe enviat)"
                        EstatMes.TANCAT -> if (estat.config.rebutBancari) "Carregat al teu compte" else "Consum ${deMes(mes)} (mes tancat)"
                    }, mida = 18.sp,
                )
                XifraText(euros(inf.total), 56.sp, C.most)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Etiqueta("Consumicions ${euros(inf.totalConsum)}")
                    if (inf.totalApats != 0L) Etiqueta("Àpats ${euros(inf.totalApats)}")
                    if (inf.totalAbonaments != 0L) Etiqueta("Despeses avançades ${euros(inf.totalAbonaments)}", ToEtiqueta.VERD)
                    val bizum = estat.consums.filter { it.sociId == soci.id && !it.anulat && it.pagament == cat.gastrobacus.consum.dades.Pagament.BIZUM && mesDe(it.ts) == mes }.sumOf { it.import }
                    if (bizum > 0) Etiqueta("Pagat per Bizum ${euros(bizum)} (no hi suma)", ToEtiqueta.MOST)
                }
                perCat.forEach { (c, v) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(c?.nom ?: "Altres", Modifier.width(140.dp), style = MaterialTheme.typography.bodyMedium)
                        Barra(v.toFloat() / maxim, c?.let { colorCategoria(it) } ?: C.vora, Modifier.weight(1f))
                        XifraText(euros(v), 16.sp, modifier = Modifier.width(90.dp))
                    }
                }
            }
        }
        item { Subtitol("Detall") }
        if (inf.linies.isEmpty()) item { Suau("Encara no hi ha res apuntat aquest mes.") }
        else item {
            Taula {
                FilaTaula(listOf({ Cap("Data") }, { Cap("Concepte") }, { Cap("Import", true) }), listOf(1f, 4f, 1.3f), capcalera = true)
                inf.linies.reversed().forEach { l ->
                    FilaTaula(
                        listOf(
                            { Cel(diaCurt(l.ts)) },
                            {
                                Column {
                                    Cel((if (l.quantitat > 1) "${l.quantitat} × " else "") + l.concepte)
                                    if (l.detall.isNotBlank()) Suau(l.detall, mida = 13.sp)
                                }
                            },
                            { Cel(euros(l.import), dreta = true, color = if (l.import < 0) C.pampol else C.roure) },
                        ),
                        listOf(1f, 4f, 1.3f),
                    )
                }
            }
        }
    }
}

/** El soci registra que ha fet la transferència del total que deu. */
@Composable
fun DialegPagar(model: AppModel, estat: Estat, soci: Soci, onTanca: () -> Unit) {
    val compte = estat.compte(soci)
    var data by remember { mutableStateOf(cat.gastrobacus.consum.dades.avui()) }
    var concepte by remember { mutableStateOf("${estat.config.nomSocietat} ${soci.nomComplet}") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onTanca,
        title = { Text("Pagar el que deus", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Import total pendent", style = MaterialTheme.typography.bodyLarge)
                XifraText(euros(compte.perPagar), 44.sp, C.most)
                if (estat.config.compteTransferencia.isNotBlank()) {
                    Text("Fes la transferència al compte de la societat:", style = MaterialTheme.typography.bodyLarge)
                    XifraText(estat.config.compteTransferencia, 24.sp)
                }
                CampData("Dia que has fet la transferència", data, { data = it })
                Camp("Concepte que hi has posat", concepte, { concepte = it })
                Suau("Tresoreria comprovarà que ha arribat. Mentrestant, surt com a pendent de comprovar.")
            }
        },
        confirmButton = {
            Boto("Registrar el pagament", {
                model.accio("Pagament registrat: ${euros(compte.perPagar)}. Gràcies!", onTanca) { model.repo.registraTransferencia(soci.id, data, concepte) }
            }, enabled = concepte.isNotBlank() && compte.perPagar > 0)
        },
        dismissButton = { Boto("Cancel·lar", onTanca, tipus = Tipus.FANTASMA) },
        containerColor = C.superficie,
    )
}
