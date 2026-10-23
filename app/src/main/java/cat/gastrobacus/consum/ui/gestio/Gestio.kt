package cat.gastrobacus.consum.ui.gestio

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.EstatCorreu
import cat.gastrobacus.consum.dades.EstatMes
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.deMes
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.dades.mesActual
import cat.gastrobacus.consum.dades.mesDe
import cat.gastrobacus.consum.dades.nomMes
import cat.gastrobacus.consum.ui.AppModel
import cat.gastrobacus.consum.ui.Avis
import cat.gastrobacus.consum.ui.Barra
import cat.gastrobacus.consum.ui.Bloc
import cat.gastrobacus.consum.ui.Boto
import cat.gastrobacus.consum.ui.C
import cat.gastrobacus.consum.ui.Pindola
import cat.gastrobacus.consum.ui.Subtitol
import cat.gastrobacus.consum.ui.Suau
import cat.gastrobacus.consum.ui.Tipus
import cat.gastrobacus.consum.ui.Titol
import cat.gastrobacus.consum.ui.XifraText

enum class SeccioGestio(val titol: String) {
    RESUM("Resum"), CONSUMICIONS("Consumicions"), PRODUCTES("Productes"), ESTOC("Estoc"), SOCIS("Socis"),
    TANCAMENT("Tancament de mes"), CORREUS("Correus"), COPIES("Còpies"), CONFIGURACIO("Configuració"),
}

@Composable
fun Gestio(model: AppModel, estat: Estat, soci: Soci) {
    var s by rememberSaveable { mutableStateOf(SeccioGestio.RESUM) }
    val perReposar = estat.productes.count { it.calReposar }
    val pendents = estat.correus.count { it.estat != EstatCorreu.ENVIAT }

    Column(Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 24.dp)) {
        Titol("Gestió")
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SeccioGestio.entries.forEach { x ->
                val n = when (x) { SeccioGestio.ESTOC -> perReposar; SeccioGestio.CORREUS -> pendents; else -> 0 }
                Pindola(x.titol + if (n > 0) "  $n" else "", s == x, { s = x })
            }
        }
        Column(Modifier.weight(1f)) {
            when (s) {
                SeccioGestio.RESUM -> Resum(estat) { s = it }
                SeccioGestio.CONSUMICIONS -> GestioConsumicions(model, estat, soci)
                SeccioGestio.PRODUCTES -> GestioProductes(model, estat)
                SeccioGestio.ESTOC -> GestioEstoc(model, estat, soci)
                SeccioGestio.SOCIS -> GestioSocis(model, estat)
                SeccioGestio.TANCAMENT -> Tancament(model, estat)
                SeccioGestio.CORREUS -> Correus(model, estat)
                SeccioGestio.COPIES -> Copies(model, estat)
                SeccioGestio.CONFIGURACIO -> Configuracio(model, estat)
            }
        }
    }
}

@Composable
private fun Resum(estat: Estat, onVes: (SeccioGestio) -> Unit) {
    val mes = mesActual()
    val informes = estat.informes(mes)
    val consumsMes = estat.consums.filter { !it.anulat && mesDe(it.ts) == mes }
    val perReposar = estat.productes.filter { it.calReposar }
    val oberts = estat.apats.count { it.estat == cat.gastrobacus.consum.dades.EstatApat.OBERT }
    val pendentsTancar = estat.consums.map { mesDe(it.ts) }.distinct().filter { it < mes && estat.estatMes(it) != EstatMes.TANCAT }.sorted()
    val top = informes.sortedByDescending { it.total }.take(5)
    val topProd = consumsMes.groupBy { it.nomProducte }.mapValues { (_, l) -> l.sumOf { it.quantitat } }.entries.sortedByDescending { it.value }.take(6)
    val maxP = (topProd.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Xifra("Total ${deMes(mes)}", euros(informes.sumOf { it.total }), "fins avui", Modifier.weight(1f))
            Xifra("Consumicions", consumsMes.sumOf { it.quantitat }.toString(), "${consumsMes.map { it.sociId }.distinct().size} socis diferents", Modifier.weight(1f))
            Xifra("Per reposar", perReposar.size.toString(), "productes amb poc estoc", Modifier.weight(1f), alerta = perReposar.isNotEmpty())
            Xifra("Àpats oberts", oberts.toString(), "pendents de passar als comptes", Modifier.weight(1f))
        }
        if (pendentsTancar.isNotEmpty()) Avis(
            pendentsTancar.joinToString { nomMes(it) } + if (pendentsTancar.size > 1) " encara no estan tancats." else " encara no està tancat.",
        ) { Boto("Anar al tancament", { onVes(SeccioGestio.TANCAMENT) }, tipus = Tipus.FANTASMA, petit = true) }
        if (estat.config.compteGoogle == null) Avis("La tauleta no té cap compte de Google connectat: no s'envien correus ni es fan còpies de seguretat.") {
            Boto("Configurar", { onVes(SeccioGestio.CONFIGURACIO) }, tipus = Tipus.FANTASMA, petit = true)
        } else estat.config.darreraCopiaError?.let { Avis("La darrera còpia de seguretat ha fallat: $it", cat.gastrobacus.consum.ui.ToAvis.ERROR) }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Bloc(Modifier.weight(1f)) {
                Subtitol("Qui porta més aquest mes")
                top.forEachIndexed { i, inf ->
                    Row { Text("${i + 1}. ${inf.soci.nomComplet}", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge); XifraText(euros(inf.total)) }
                }
            }
            Bloc(Modifier.weight(1f)) {
                Subtitol("El més demanat")
                topProd.forEach { (p, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(p, Modifier.width(190.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Barra(n.toFloat() / maxP, C.most, Modifier.weight(1f))
                        XifraText(n.toString(), 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Xifra(titol: String, valor: String, peu: String, modifier: Modifier, alerta: Boolean = false) {
    Bloc(modifier) {
        Text(titol, style = MaterialTheme.typography.labelMedium, color = C.roure2)
        XifraText(valor, 32.sp, if (alerta) C.ambre else C.roure)
        Suau(peu, mida = 13.sp)
    }
}

