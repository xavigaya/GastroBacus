package cat.gastrobacus.consum.ui.gestio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.gastrobacus.consum.dades.EstatTransferencia
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.dataHora
import cat.gastrobacus.consum.dades.dataLlarga
import cat.gastrobacus.consum.dades.dataPlena
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.ui.AppModel
import cat.gastrobacus.consum.ui.Boto
import cat.gastrobacus.consum.ui.C
import cat.gastrobacus.consum.ui.Cap
import cat.gastrobacus.consum.ui.Cel
import cat.gastrobacus.consum.ui.Etiqueta
import cat.gastrobacus.consum.ui.FilaTaula
import cat.gastrobacus.consum.ui.Subtitol
import cat.gastrobacus.consum.ui.Suau
import cat.gastrobacus.consum.ui.Taula
import cat.gastrobacus.consum.ui.Tipus
import cat.gastrobacus.consum.ui.ToEtiqueta

/** Tresoreria: transferències per comprovar, Bizums i socis amb deute. */
@Composable
fun GestioCobraments(model: AppModel, estat: Estat, junta: Soci) {
    val pendents = estat.transferenciesPendents
    val deutors = estat.socis.map { it to estat.compte(it) }.filter { it.second.perPagar > 0 }.sortedByDescending { it.second.diesDeute() }
    val pesosT = listOf(2.4f, 2f, 2.6f, 1.2f, 2.6f)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Subtitol("Transferències per comprovar")
        Suau("Comprova al compte de la societat que han arribat amb aquest concepte i aquest import.")
        if (pendents.isEmpty()) Suau("Cap transferència pendent.")
        else Taula {
            FilaTaula(listOf({ Cap("Soci") }, { Cap("Dia de la transferència") }, { Cap("Concepte") }, { Cap("Import", true) }, { Cap("") }), pesosT, capcalera = true)
            pendents.forEach { t ->
                FilaTaula(
                    listOf(
                        { Cel(estat.soci(t.sociId)?.nomComplet ?: "—") },
                        { Cel(dataLlarga(t.data)) },
                        { Cel(t.concepte) },
                        { Cel(euros(t.import), dreta = true, negreta = true) },
                        {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Boto("Ha arribat", { model.accio("Transferència confirmada") { model.repo.confirmaTransferencia(t.id, junta.id, true) } }, tipus = Tipus.VERD, petit = true)
                                Boto("No ha arribat", { model.accio("El deute torna a estar pendent") { model.repo.confirmaTransferencia(t.id, junta.id, false) } }, tipus = Tipus.FANTASMA, petit = true)
                            }
                        },
                    ),
                    pesosT,
                )
            }
        }

        Subtitol("Socis amb deute")
        Suau("En vermell, els que fa més de ${estat.config.diesAvisDeute} dies que deuen: a la tauleta els surt el nom en vermell.")
        if (deutors.isEmpty()) Suau("Tothom està al dia.")
        else Taula {
            val pesos = listOf(3f, 1.4f, 2f, 1.2f)
            FilaTaula(listOf({ Cap("Soci") }, { Cap("Deu", true) }, { Cap("Des de") }, { Cap("Dies", true) }), pesos, capcalera = true)
            deutors.forEach { (s, c) ->
                val antic = c.diesDeute() > estat.config.diesAvisDeute
                FilaTaula(
                    listOf(
                        { Cel(s.nomComplet + if (!s.actiu) " (de baixa)" else "", color = if (antic) C.error else C.roure, negreta = antic) },
                        { Cel(euros(c.perPagar), dreta = true, negreta = true) },
                        { Cel(c.deuteDesDe?.let { dataPlena(it) } ?: "—") },
                        { Cel(c.diesDeute().toString(), dreta = true, color = if (antic) C.error else C.roure) },
                    ),
                    pesos,
                )
            }
            FilaTaula(listOf({ Cel("Total", negreta = true) }, { Cel(euros(deutors.sumOf { it.second.perPagar }), dreta = true, negreta = true) }, {}, {}), listOf(3f, 1.4f, 2f, 1.2f), capcalera = true)
        }

        GestioBizums(model, estat, junta)

        val fetes = estat.transferencies.filter { it.estat != EstatTransferencia.PENDENT }.take(30)
        if (fetes.isNotEmpty()) {
            Subtitol("Darreres transferències comprovades")
            Taula {
                fetes.forEach { t ->
                    FilaTaula(
                        listOf(
                            { Cel(estat.soci(t.sociId)?.nomComplet ?: "—") }, { Cel(dataLlarga(t.data)) }, { Cel(t.concepte) },
                            { Cel(euros(t.import), dreta = true) },
                            {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Etiqueta(if (t.estat == EstatTransferencia.CONFIRMADA) "Rebuda" else "No ha arribat", if (t.estat == EstatTransferencia.CONFIRMADA) ToEtiqueta.VERD else ToEtiqueta.VERMELL)
                                    Boto("Desfer", { model.accio("Torna a estar pendent") { model.repo.reobreTransferencia(t.id) } }, tipus = Tipus.FANTASMA, petit = true)
                                }
                            },
                        ),
                        pesosT,
                    )
                }
            }
            Suau("Comprovades per la junta. Darrera: ${fetes.firstOrNull()?.confirmatTs?.let { dataHora(it) } ?: "—"}.")
        }
    }
}
