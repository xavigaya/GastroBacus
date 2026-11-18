package cat.gastrobacus.consum.dades

import kotlin.math.roundToLong

/** Lògica pura (sense Android) per als àpats i els informes mensuals. */

data class Quota(
    val sociId: String, // per a persones de fora: "ext:<id>"
    val unitats: Double, // persones (a parts iguals) o parts (proporcional)
    val menjar: Long, // part de les despeses de l'àpat
    val cadires: Long, // preu de les cadires (ingrés de la societat)
    val pagat: Long, // el que ha avançat en despeses
    val numCadires: Int = 0,
    val nomExtern: String? = null,
    val neteja: Long = 0, // només per a qui organitza
    val responsableId: String? = null, // soci que paga per la persona de fora
) {
    val esExtern get() = nomExtern != null
    val quota get() = menjar + cadires + neteja
    /** El que es paga a l'activitat mateixa (en efectiu, si és el cas): menjar i cadires. */
    val efectiu get() = menjar + cadires - pagat
    val saldo get() = quota - pagat // positiu: paga; negatiu: se li torna
}

data class Repartiment(val total: Long, val perUnitat: Double, val quotes: List<Quota>) {
    val totalCadires get() = quotes.sumOf { it.cadires }
    val totalNeteja get() = quotes.sumOf { it.neteja }
    val totalExterns get() = quotes.filter { it.esExtern }.sumOf { it.quota }
}

fun repartiment(apat: Apat): Repartiment {
    val total = apat.despeses.sumOf { it.import }
    fun unitats(a: Assistent) = if (apat.mode == ModeRepartiment.IGUALS) 1.0 + a.convidats else maxOf(0.0, a.parts)
    fun unitatsExtern(e: Extern) = if (apat.mode == ModeRepartiment.IGUALS) 1.0 else maxOf(0.0, e.parts)
    val totalUnitats = apat.assistents.sumOf { unitats(it) } + apat.externs.sumOf { unitatsExtern(it) }
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
            menjar = (perUnitat * u).roundToLong(),
            // El soci paga la seva cadira a preu de soci i la dels seus convidats a preu de no soci
            cadires = if (a != null) apat.cadiraSoci + a.convidats * apat.cadiraNoSoci else 0,
            pagat = apat.despeses.filter { it.pagadorId == id }.sumOf { it.import },
            numCadires = if (a != null) 1 + a.convidats else 0,
        )
    }.toMutableList()
    apat.externs.forEach { e ->
        val u = unitatsExtern(e)
        quotes += Quota("ext:${e.id}", u, (perUnitat * u).roundToLong(), apat.cadiraNoSoci, 0, 1, e.nom, responsableId = e.responsableId.ifBlank { null })
    }
    // La neteja la paga qui organitza
    if (apat.preuNeteja > 0) {
        val i = quotes.indexOfFirst { it.sociId == apat.organitzadorId }
        if (i >= 0) quotes[i] = quotes[i].copy(neteja = apat.preuNeteja)
        else quotes += Quota(apat.organitzadorId, 0.0, 0, 0, 0, neteja = apat.preuNeteja)
    }
    // Ajust de cèntims perquè la suma de les parts de menjar sigui exactament el total
    val dif = total - quotes.sumOf { it.menjar }
    if (dif != 0L && totalUnitats > 0) {
        val i = quotes.indexOfFirst { it.unitats > 0 }
        if (i >= 0) quotes[i] = quotes[i].copy(menjar = quotes[i].menjar + dif)
    }
    return Repartiment(total, perUnitat, quotes)
}

enum class TipusLinia { CONSUM, APAT, CADIRA, NETEJA, ABONAMENT }

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
    val totalApats get() = suma(TipusLinia.APAT) + suma(TipusLinia.CADIRA) + suma(TipusLinia.NETEJA)
    val totalAbonaments get() = suma(TipusLinia.ABONAMENT)
    val total get() = linies.sumOf { it.import }
}

private fun num(d: Double) = d.toString().removeSuffix(".0").replace('.', ',')

/**
 * Tot el que s'ha de carregar al compte del soci (de sempre). No hi entra el que s'ha pagat per Bizum
 * ni el menjar i les cadires de les activitats que es cobren en efectiu.
 */
fun liniesSoci(soci: Soci, consums: List<Consum>, apats: List<Apat>): List<LiniaInforme> {
    val linies = mutableListOf<LiniaInforme>()
    consums.filter { it.sociId == soci.id && !it.anulat && it.pagament == Pagament.FACTURA }.forEach {
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
    apats.filter { it.estat == EstatApat.TANCAT }.forEach { a ->
        val r = repartiment(a)
        val ts = tsDe(a.data)
        val q = r.quotes.firstOrNull { it.sociId == soci.id }
        if (q != null && !a.cobramentEfectiu) {
            if (q.menjar > 0) linies += LiniaInforme(
                ts, "Àpat: ${a.titol}",
                if (a.mode == ModeRepartiment.IGUALS) "${num(q.unitats)} ${if (q.unitats == 1.0) "persona" else "persones"}" else "${num(q.unitats)} parts",
                1, q.menjar, q.menjar, TipusLinia.APAT,
            )
            if (q.cadires > 0) linies += LiniaInforme(
                ts, "Cadires: ${a.titol}", "${q.numCadires} ${if (q.numCadires == 1) "cadira" else "cadires"}",
                q.numCadires, q.cadires / maxOf(1, q.numCadires), q.cadires, TipusLinia.CADIRA,
            )
            if (q.pagat > 0) linies += LiniaInforme(ts, "Àpat: ${a.titol}", "Despeses avançades", 1, -q.pagat, -q.pagat, TipusLinia.ABONAMENT)
        }
        if (q != null && q.neteja > 0) linies += LiniaInforme(ts, "Neteja: ${a.titol}", a.neteja.nom, 1, q.neteja, q.neteja, TipusLinia.NETEJA)
        // Persones de fora de les quals és responsable
        if (!a.cobramentEfectiu) r.quotes.filter { it.esExtern && it.responsableId == soci.id }.forEach { e ->
            linies += LiniaInforme(ts, "Àpat: ${a.titol}", "${e.nomExtern} (menjar i cadira)", 1, e.quota, e.quota, TipusLinia.APAT)
        }
    }
    return linies.sortedBy { it.ts }
}

fun informeSoci(soci: Soci, mes: String, consums: List<Consum>, apats: List<Apat>): InformeSoci =
    InformeSoci(soci, liniesSoci(soci, consums, apats).filter { mesDe(it.ts) == mes })

/** Situació del compte d'un soci respecte als pagaments per transferència. */
data class EstatCompte(
    val totalCarrecs: Long,
    val pagatConfirmat: Long,
    val pendentConfirmar: Long,
    /** Data del càrrec més antic que encara no està cobert per cap pagament. */
    val deuteDesDe: Long?,
) {
    /** El que encara ha de pagar (descomptant també les transferències pendents de confirmar). */
    val perPagar get() = totalCarrecs - pagatConfirmat - pendentConfirmar
    fun diesDeute(ara: Long = System.currentTimeMillis()) = deuteDesDe?.let { ((ara - it) / 86_400_000L).toInt() } ?: 0
}

/**
 * Calcula el deute aplicant els pagaments als càrrecs més antics primer.
 * [mesosPagatsPerRebut]: mesos tancats que ja s'han cobrat amb el rebut bancari (si es fa servir).
 */
fun estatCompte(
    soci: Soci, consums: List<Consum>, apats: List<Apat>, transferencies: List<Transferencia>,
    mesosPagatsPerRebut: Set<String> = emptySet(),
): EstatCompte {
    val linies = liniesSoci(soci, consums, apats).filter { mesDe(it.ts) !in mesosPagatsPerRebut }
    val total = linies.sumOf { it.import }
    val meves = transferencies.filter { it.sociId == soci.id }
    val confirmat = meves.filter { it.estat == EstatTransferencia.CONFIRMADA }.sumOf { it.import }
    val pendent = meves.filter { it.estat == EstatTransferencia.PENDENT }.sumOf { it.import }
    var acumulat = 0L
    var desDe: Long? = null
    for (l in linies) {
        acumulat += l.import
        if (acumulat > confirmat + pendent) { desDe = l.ts; break }
    }
    return EstatCompte(total, confirmat, pendent, if (total - confirmat - pendent > 0) desDe else null)
}

fun informeMes(socis: List<Soci>, mes: String, consums: List<Consum>, apats: List<Apat>): List<InformeSoci> =
    socis.map { informeSoci(it, mes, consums, apats) }
        .filter { it.linies.isNotEmpty() }
        .sortedWith(compareBy(java.text.Collator.getInstance(CATALA)) { it.soci.nomComplet })

/** Favorits d'un soci: els fixats per la junta i els que més pren els darrers 90 dies. */
fun favorits(productes: List<Producte>, consums: List<Consum>, sociId: String?, quants: Int, ara: Long = System.currentTimeMillis()): List<Producte> {
    val actius = productes.filter { it.actiu }
    val fixos = actius.filter { it.favorit }
    if (sociId == null) return fixos
    val desDe = ara - 90L * 86_400_000L
    val habituals = consums.asSequence()
        .filter { it.sociId == sociId && !it.anulat && it.ts >= desDe }
        .groupBy { it.producteId }.mapValues { (_, l) -> l.sumOf { it.quantitat } }
        .entries.sortedByDescending { it.value }
        .mapNotNull { e -> actius.firstOrNull { it.id == e.key } }
        .filter { it !in fixos }
        .take(quants)
    return fixos + habituals
}

/** Hora en què caduca un convidat creat a [ts]: les [hora] del matí següent (o d'avui si encara no han tocat). */
fun caducaConvidat(ts: Long, hora: Int): Long {
    val z = java.time.Instant.ofEpochMilli(ts).atZone(ZONA)
    var fi = z.toLocalDate().atTime(hora, 0).atZone(ZONA)
    if (!fi.isAfter(z)) fi = fi.plusDays(1)
    return fi.toInstant().toEpochMilli()
}
