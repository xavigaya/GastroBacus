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
fun ElMeuConsum(estat: Estat, soci: Soci) {
    var mes by remember { mutableStateOf(mesActual()) }
    val inf = estat.informe(soci, mes)
    val estatMes = estat.estatMes(mes)
    val perCat = estat.consums.filter { it.sociId == soci.id && !it.anulat && mesDe(it.ts) == mes }
        .groupBy { estat.producte(it.producteId)?.categoria }
        .mapValues { (_, l) -> l.sumOf { it.import } }.entries.sortedByDescending { it.value }
    val maxim = (perCat.maxOfOrNull { it.value } ?: 1L).coerceAtLeast(1L)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Titol("El meu consum", Modifier.weight(1f))
                SelectorMes(mes, darrersMesos(), { mes = it })
            }
        }
        item {
            Bloc(Modifier.fillMaxWidth()) {
                Suau(
                    when (estatMes) {
                        EstatMes.OBERT -> "Portes gastat ${deMes(mes)}"
                        EstatMes.INFORMAT -> "Pendent de passar al banc"
                        EstatMes.TANCAT -> "Carregat al teu compte"
                    }, mida = 18.sp,
                )
                XifraText(euros(inf.total), 56.sp, C.most)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Etiqueta("Consumicions ${euros(inf.totalConsum)}")
                    if (inf.totalApats != 0L) Etiqueta("Àpats ${euros(inf.totalApats)}")
                    if (inf.totalAbonaments != 0L) Etiqueta("Despeses avançades ${euros(inf.totalAbonaments)}", ToEtiqueta.VERD)
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

