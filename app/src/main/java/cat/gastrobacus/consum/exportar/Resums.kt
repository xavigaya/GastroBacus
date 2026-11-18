package cat.gastrobacus.consum.exportar

import cat.gastrobacus.consum.dades.Apat
import cat.gastrobacus.consum.dades.EstatBizum
import cat.gastrobacus.consum.dades.EstatTransferencia
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.Pagament
import cat.gastrobacus.consum.dades.TipusReposicio
import cat.gastrobacus.consum.dades.avui
import cat.gastrobacus.consum.dades.dataLlarga
import cat.gastrobacus.consum.dades.dataPlena
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.dades.repartiment

/* Resums que s'envien automàticament per correu: estoc, tresoreria i activitats. */

data class Resum(val assumpte: String, val cos: String, val adjunt: Fitxer?)

private const val DIA = 86_400_000L

/** Estoc: què hi ha, què cal reposar i què s'ha consumit els darrers 7 dies. */
fun resumEstoc(estat: Estat, ara: Long = System.currentTimeMillis()): Resum {
    val desDe = ara - 7 * DIA
    val productes = estat.productes.filter { it.actiu }.sortedWith(compareBy({ it.categoria.ordinal }, { it.nom }))
    val setmana = estat.consums.filter { !it.anulat && it.ts >= desDe }.groupBy { it.producteId }.mapValues { (_, l) -> l.sumOf { it.quantitat } }
    val reposar = productes.filter { it.calReposar }
    val full1 = listOf(listOf<Cel>(Cel.T("Producte"), Cel.T("Categoria"), Cel.T("Queden"), Cel.T("Mínim"), Cel.T("Cal reposar"), Cel.T("Consumits 7 dies"), Cel.T("Des de la darrera comanda"), Cel.T("Darrera comanda"))) +
        productes.map { p ->
            listOf(
                Cel.T(p.nom), Cel.T(p.categoria.nom), if (p.controlaEstoc) Cel.N(p.estoc) else Cel.T("—"), Cel.N(p.estocMinim),
                Cel.T(if (p.calReposar) "Sí" else ""), Cel.N(setmana[p.id] ?: 0), Cel.N(p.comptador),
                p.darreraComandaTs?.let { Cel.D(it) } ?: Cel.T(""),
            )
        }
    val full2 = listOf(listOf<Cel>(Cel.T("Data"), Cel.T("Producte"), Cel.T("Tipus"), Cel.T("Quantitat"), Cel.T("N'hi havia"), Cel.T("Consumits des de l'anterior"))) +
        estat.reposicions.filter { it.ts >= desDe }.map { r ->
            listOf(Cel.D(r.ts), Cel.T(estat.producte(r.producteId)?.nom ?: ""), Cel.T(if (r.tipus == TipusReposicio.ENTRADA) "Entrada" else "Recompte"), Cel.N(r.quantitat), Cel.N(r.estocAnterior), Cel.N(r.consumitPeriode))
        }
    val cos = buildString {
        append("Resum d'estoc del ${dataPlena(ara)}.\n\n")
        if (reposar.isEmpty()) append("No cal reposar res.\n")
        else { append("Cal reposar:\n"); reposar.forEach { append("  ${it.nom}: en queden ${it.estoc} (mínim ${it.estocMinim})\n") } }
        val top = setmana.entries.sortedByDescending { it.value }.take(8)
        if (top.isNotEmpty()) { append("\nEl més consumit els darrers 7 dies:\n"); top.forEach { (id, n) -> append("  $n × ${estat.producte(id)?.nom ?: ""}\n") } }
        val peticions = estat.peticionsPendents
        if (peticions.isNotEmpty()) { append("\nPeticions dels socis:\n"); peticions.forEach { append("  ${it.tipus.nom}: ${it.text}\n") } }
        append("\nEl detall és a l'Excel adjunt.\n")
    }
    val nom = "GastroBacus-estoc-${avui()}.xlsx"
    return Resum("resum d'estoc", cos, Fitxer(nom, TIPUS_XLSX, xlsx(listOf(Full("Estoc", listOf(30, 14, 9, 9, 11, 14, 22, 14), full1), Full("Comandes de la setmana", listOf(12, 30, 11, 10, 11, 22), full2)))))
}

/** Tresoreria: consumicions i pagaments des de l'últim resum, i el saldo de cada soci. */
fun resumTresoreria(estat: Estat, desDe: Long, ara: Long = System.currentTimeMillis()): Resum {
    val consums = estat.consums.filter { !it.anulat && it.ts in desDe..ara }
    val transf = estat.transferencies.filter { it.ts in desDe..ara }
    val bizums = estat.bizums.filter { it.ts in desDe..ara }
    val saldos = estat.socis.map { it to estat.compte(it) }.filter { it.second.perPagar != 0L || it.second.pendentConfirmar > 0 }.sortedByDescending { it.second.perPagar }
    val fSaldos = listOf(listOf<Cel>(Cel.T("Soci"), Cel.T("Pendent de pagar"), Cel.T("Transferència per comprovar"), Cel.T("Deu des de"), Cel.T("Dies"))) +
        saldos.map { (s, c) -> listOf(Cel.T(s.nomComplet), Cel.E(c.perPagar), Cel.E(c.pendentConfirmar), c.deuteDesDe?.let { Cel.D(it) } ?: Cel.T(""), Cel.N(c.diesDeute(ara))) }
    val fTransf = listOf(listOf<Cel>(Cel.T("Registrada"), Cel.T("Soci"), Cel.T("Dia transferència"), Cel.T("Concepte"), Cel.T("Import"), Cel.T("Estat"))) +
        transf.map { t -> listOf(Cel.D(t.ts), Cel.T(estat.soci(t.sociId)?.nomComplet ?: ""), Cel.T(t.data), Cel.T(t.concepte), Cel.E(t.import), Cel.T(when (t.estat) { EstatTransferencia.PENDENT -> "Per comprovar"; EstatTransferencia.CONFIRMADA -> "Rebuda"; EstatTransferencia.REBUTJADA -> "No ha arribat" })) }
    val fBizums = listOf(listOf<Cel>(Cel.T("Data"), Cel.T("Qui"), Cel.T("Concepte"), Cel.T("Import"), Cel.T("Estat"))) +
        bizums.map { b -> listOf(Cel.D(b.ts), Cel.T(b.nom), Cel.T(b.concepte), Cel.E(b.import), Cel.T(when (b.estat) { EstatBizum.PENDENT -> "Per comprovar"; EstatBizum.REBUT -> "Rebut"; EstatBizum.NO_ARRIBAT -> "No ha arribat" })) }
    val fConsums = listOf(listOf<Cel>(Cel.T("Data"), Cel.T("Soci"), Cel.T("Producte"), Cel.T("Quantitat"), Cel.T("Import"), Cel.T("Pagament"))) +
        consums.sortedBy { it.ts }.map { c -> listOf(Cel.D(c.ts), Cel.T((estat.soci(c.sociId)?.nomComplet ?: "") + (c.convidat?.let { " (convidat: $it)" } ?: "")), Cel.T(c.nomProducte), Cel.N(c.quantitat), Cel.E(c.import), Cel.T(if (c.pagament == Pagament.BIZUM) "Bizum" else "Compte")) }
    val cos = buildString {
        append("Resum de tresoreria del ${dataPlena(desDe)} al ${dataPlena(ara)}.\n\n")
        append("Consumicions: ${euros(consums.sumOf { it.import })} (${consums.size} apunts)\n")
        append("Transferències registrades: ${euros(transf.sumOf { it.import })} (${transf.count { it.estat == EstatTransferencia.PENDENT }} per comprovar)\n")
        append("Bizums: ${euros(bizums.sumOf { it.import })} (${bizums.count { it.estat == EstatBizum.PENDENT }} per comprovar)\n")
        append("\nPendent de pagar entre tots els socis: ${euros(saldos.sumOf { maxOf(0, it.second.perPagar) })}\n")
        val antics = saldos.filter { it.second.perPagar > 0 && it.second.diesDeute(ara) > estat.config.diesAvisDeute }
        if (antics.isNotEmpty()) { append("Amb deute de fa més de ${estat.config.diesAvisDeute} dies:\n"); antics.forEach { (s, c) -> append("  ${s.nomComplet}: ${euros(c.perPagar)} (${c.diesDeute(ara)} dies)\n") } }
        append("\nEl detall és a l'Excel adjunt.\n")
    }
    return Resum(
        "resum de tresoreria", cos,
        Fitxer(
            "GastroBacus-tresoreria-${avui()}.xlsx", TIPUS_XLSX,
            xlsx(listOf(Full("Saldos", listOf(30, 16, 22, 14, 8), fSaldos), Full("Transferències", listOf(14, 28, 16, 30, 11, 14), fTransf), Full("Bizums", listOf(14, 28, 30, 11, 14), fBizums), Full("Consumicions", listOf(14, 34, 30, 10, 11, 10), fConsums))),
        ),
    )
}

/** Activitat: qui hi va anar, com es reparteix, efectiu i neteja. S'envia l'endemà. */
fun resumActivitat(estat: Estat, a: Apat): Resum {
    val r = repartiment(a)
    fun nom(id: String?) = estat.soci(id)?.nomComplet ?: "—"
    val cos = buildString {
        append("${a.titol}\n${dataLlarga(a.data)}, ${a.franja.nom.lowercase()}. Activitat ${a.tipus.nom.lowercase()}. Espai: ${a.espai}.\n")
        append("Organitza: ${nom(a.organitzadorId)}. ${a.assistents.sumOf { 1 + it.convidats } + a.externs.size} persones.\n")
        append("Neteja: ${a.neteja.nom}${if (a.preuNeteja > 0) " (${euros(a.preuNeteja)} al compte de qui organitza)" else ""}.\n\n")
        append(if (a.cobramentEfectiu) "Tiquets dels cuiners:\n" else "Despeses:\n")
        a.despeses.forEach { append("  ${nom(it.pagadorId)}: ${it.concepte} ${euros(it.import)}\n") }
        append("  Total: ${euros(r.total)}. Cadires: ${euros(r.totalCadires)}.\n\n")
        append(if (a.cobramentEfectiu) "Pagament en efectiu:\n" else "Repartiment (va al compte de cada soci quan es passa als comptes):\n")
        r.quotes.forEach { q ->
            val qui = q.nomExtern?.let { "$it (de fora, a compte de ${nom(q.responsableId)})" } ?: nom(q.sociId)
            val import = if (a.cobramentEfectiu) q.efectiu else q.saldo
            val marca = if (a.cobramentEfectiu && a.assistents.any { it.sociId == q.sociId && it.pagatEfectiu }) " ✓ pagat" else ""
            append("  $qui: ${if (import < 0) "rep ${euros(-import)}" else "paga ${euros(import)}"}$marca\n")
        }
        if (a.cobramentEfectiu) {
            val pendents = r.quotes.filter { q -> q.efectiu > 0 && !q.esExtern && a.assistents.none { it.sociId == q.sociId && it.pagatEfectiu } }
            if (pendents.isNotEmpty()) append("\nFalta marcar com a pagat: ${pendents.joinToString { nom(it.sociId) }}.\n")
            append("Cadires per a la caixa de la junta: ${euros(r.totalCadires)}.\n")
        }
        append("\nEstat a l'app: ${if (a.estat == cat.gastrobacus.consum.dades.EstatApat.TANCAT) "passada als comptes" else "encara oberta (cal passar-la als comptes)"}.\n")
    }
    return Resum("activitat ${a.titol} (${dataPlena(cat.gastrobacus.consum.dades.tsDe(a.data))})", cos, null)
}
