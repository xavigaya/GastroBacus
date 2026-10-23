package cat.gastrobacus.consum.dades

import kotlin.math.roundToLong

/** Lògica pura (sense Android) per als àpats i els informes mensuals. */

data class Quota(
    val sociId: String,
    val unitats: Double, // persones (a parts iguals) o parts (proporcional)
    val quota: Long, // el que li toca pagar
    val pagat: Long, // el que ha avançat en despeses
) {
    val saldo get() = quota - pagat // positiu: paga; negatiu: se li torna
}

data class Repartiment(val total: Long, val perUnitat: Double, val quotes: List<Quota>)

fun repartiment(apat: Apat): Repartiment {
    val total = apat.despeses.sumOf { it.import }
    fun unitats(a: Assistent) = if (apat.mode == ModeRepartiment.IGUALS) 1.0 + a.convidats else maxOf(0.0, a.parts)
    val totalUnitats = apat.assistents.sumOf { unitats(it) }
    val perUnitat = if (totalUnitats > 0) total / totalUnitats else 0.0

    val ids = LinkedHashSet<String>().apply {
        apat.assistents.forEach { add(it.sociId) }
        apat.despeses.forEach { add(it.pagadorId) }
    }
    val quotes = ids.map { id ->
        val a = apat.assistents.firstOrNull { it.sociId == id }
        val u = a?.let { unitats(it) } ?: 0.0
        Quota(
            sociId = id,
            unitats = u,
            quota = (perUnitat * u).roundToLong(),
            pagat = apat.despeses.filter { it.pagadorId == id }.sumOf { it.import },
        )
    }.toMutableList()
    // Ajust de cèntims perquè la suma de quotes sigui exactament el total
    val dif = total - quotes.sumOf { it.quota }
    if (dif != 0L && totalUnitats > 0) {
        val i = quotes.indexOfFirst { it.unitats > 0 }
        if (i >= 0) quotes[i] = quotes[i].copy(quota = quotes[i].quota + dif)
    }
    return Repartiment(total, perUnitat, quotes)
}

enum class TipusLinia { CONSUM, APAT, ABONAMENT }

data class LiniaInforme(
    val ts: Long,
    val concepte: String,
    val detall: String,
    val quantitat: Int,
    val preu: Long,
    val import: Long,
    val tipus: TipusLinia,
)

data class InformeSoci(val soci: Soci, val linies: List<LiniaInforme>) {
    private fun suma(t: TipusLinia) = linies.filter { it.tipus == t }.sumOf { it.import }
    val totalConsum get() = suma(TipusLinia.CONSUM)
    val totalApats get() = suma(TipusLinia.APAT)
    val totalAbonaments get() = suma(TipusLinia.ABONAMENT)
    val total get() = linies.sumOf { it.import }
}

fun informeSoci(soci: Soci, mes: String, consums: List<Consum>, apats: List<Apat>): InformeSoci {
    val linies = mutableListOf<LiniaInforme>()
    consums.filter { it.sociId == soci.id && !it.anulat && mesDe(it.ts) == mes }.forEach {
        linies += LiniaInforme(
            ts = it.ts,
            concepte = it.nomProducte,
            detall = it.convidat?.let { c -> "Convidat: $c" } ?: "",
            quantitat = it.quantitat,
            preu = it.preuUnitari,
            import = it.import,
            tipus = TipusLinia.CONSUM,
        )
    }
    apats.filter { it.estat == EstatApat.TANCAT && mesDeData(it.data) == mes }.forEach { a ->
        val q = repartiment(a).quotes.firstOrNull { it.sociId == soci.id } ?: return@forEach
        val ts = tsDe(a.data)
        if (q.quota > 0) {
            val u = q.unitats.toString().removeSuffix(".0").replace('.', ',')
            linies += LiniaInforme(
                ts, "Àpat: ${a.titol}",
                if (a.mode == ModeRepartiment.IGUALS) "$u ${if (q.unitats == 1.0) "persona" else "persones"}" else "$u parts",
                1, q.quota, q.quota, TipusLinia.APAT,
            )
        }
        if (q.pagat > 0) linies += LiniaInforme(ts, "Àpat: ${a.titol}", "Despeses avançades", 1, -q.pagat, -q.pagat, TipusLinia.ABONAMENT)
    }
    return InformeSoci(soci, linies.sortedBy { it.ts })
}

fun informeMes(socis: List<Soci>, mes: String, consums: List<Consum>, apats: List<Apat>): List<InformeSoci> =
    socis.map { informeSoci(it, mes, consums, apats) }
        .filter { it.linies.isNotEmpty() }
        .sortedWith(compareBy(java.text.Collator.getInstance(CATALA)) { it.soci.nomComplet })
