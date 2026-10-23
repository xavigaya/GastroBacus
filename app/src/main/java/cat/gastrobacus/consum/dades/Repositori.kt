package cat.gastrobacus.consum.dades

import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class ErrorApp(missatge: String) : Exception(missatge)

/** Tot l'estat de l'app. Amb menys de 50 socis cap a la memòria sense problemes. */
data class Estat(
    val carregat: Boolean = false,
    val config: Config = Config(),
    val socis: List<Soci> = emptyList(),
    val productes: List<Producte> = emptyList(),
    val consums: List<Consum> = emptyList(),
    val reposicions: List<Reposicio> = emptyList(),
    val apats: List<Apat> = emptyList(),
    val reserves: List<Reserva> = emptyList(),
    val tancaments: List<Tancament> = emptyList(),
    val correus: List<Correu> = emptyList(),
) {
    fun soci(id: String?) = socis.firstOrNull { it.id == id }
    fun producte(id: String) = productes.firstOrNull { it.id == id }
    fun estatMes(mes: String) = tancaments.firstOrNull { it.mes == mes }?.estat ?: EstatMes.OBERT
    fun tancament(mes: String) = tancaments.firstOrNull { it.mes == mes }
    fun informe(soci: Soci, mes: String) = informeSoci(soci, mes, consums, apats)
    fun informes(mes: String) = informeMes(socis, mes, consums, apats)
    val socisActius get() = socis.filter { it.actiu }.sortedWith(compareBy(java.text.Collator.getInstance(CATALA)) { it.nom })
    val esBuida get() = carregat && socis.isEmpty()
}

data class ItemApunt(val producteId: String, val quantitat: Int, val convidat: String? = null)

@Serializable
data class Copia(
    val versio: Int = 1,
    val ts: Long,
    val config: Config,
    val socis: List<Soci>,
    val productes: List<Producte>,
    val consums: List<Consum>,
    val reposicions: List<Reposicio>,
    val apats: List<Apat>,
    val reserves: List<Reserva>,
    val tancaments: List<Tancament>,
    val correus: List<Correu>,
)

class Repositori(private val bd: BaseDades, ambit: CoroutineScope) {
    private val dao = bd.dao()

    val estat: StateFlow<Estat> = combine(
        combine(dao.socis(), dao.productes(), dao.consums(), dao.reposicions()) { s, p, c, r -> listOf(s, p, c, r) },
        combine(dao.apats(), dao.reserves(), dao.tancaments(), dao.correus(), dao.config()) { a, re, t, co, cf -> listOf(a, re, t, co, cf) },
    ) { x, y ->
        @Suppress("UNCHECKED_CAST")
        Estat(
            carregat = true,
            socis = x[0] as List<Soci>,
            productes = x[1] as List<Producte>,
            consums = x[2] as List<Consum>,
            reposicions = x[3] as List<Reposicio>,
            apats = y[0] as List<Apat>,
            reserves = y[1] as List<Reserva>,
            tancaments = y[2] as List<Tancament>,
            correus = y[3] as List<Correu>,
            config = (y[4] as ConfigFila?)?.let { JSON.decodeFromString<Config>(it.json) } ?: Config(),
        )
    }.stateIn(ambit, SharingStarted.Eagerly, Estat())

    suspend fun config(): Config = dao.configActual()?.let { JSON.decodeFromString<Config>(it.json) } ?: Config()
    suspend fun desaConfig(c: Config) = dao.desaConfig(ConfigFila(json = JSON.encodeToString(c)))
    suspend fun actualitzaConfig(f: (Config) -> Config) = desaConfig(f(config()))

    private suspend fun mesBloquejat(mes: String) = dao.tancament(mes)?.estat == EstatMes.TANCAT

    // ---------- Identificació ----------

    suspend fun verificarPin(sociId: String, pin: String): Boolean {
        val s = dao.soci(sociId) ?: return false
        return s.actiu && s.pinHash == Pin.hash(pin, s.pinSal)
    }

    suspend fun canviarPin(sociId: String, actual: String, nou: String) {
        if (!verificarPin(sociId, actual)) throw ErrorApp("El PIN actual no és correcte.")
        if (!Pin.valid(nou)) throw ErrorApp("El PIN nou ha de tenir 4 xifres.")
        val s = dao.soci(sociId)!!
        val sal = Pin.novaSal()
        dao.desaSoci(s.copy(pinSal = sal, pinHash = Pin.hash(nou, sal)))
    }

    // ---------- Consumicions ----------

    suspend fun apuntar(sociId: String, items: List<ItemApunt>, origen: Origen = Origen.TAULETA, ts: Long = System.currentTimeMillis()): Long {
        if (mesBloquejat(mesDe(ts))) throw ErrorApp("Aquest mes ja està tancat.")
        var total = 0L
        bd.withTransaction {
            for (it in items) {
                val p = dao.producte(it.producteId) ?: continue
                if (it.quantitat <= 0) continue
                val preu = p.preuPer(it.convidat != null)
                dao.desaConsum(
                    Consum(nouId(), sociId, p.id, p.nom, it.quantitat, preu, it.convidat, ts, origen),
                )
                if (p.controlaEstoc) dao.desaProducte(p.copy(estoc = maxOf(0, p.estoc - it.quantitat)))
                total += preu * it.quantitat
            }
        }
        return total
    }

    suspend fun anular(consumId: String, perId: String, esJunta: Boolean) {
        val c = dao.consum(consumId) ?: return
        if (c.anulat) return
        if (mesBloquejat(mesDe(c.ts))) throw ErrorApp("Aquest mes ja està tancat i no es pot modificar.")
        val minuts = config().minutsDesfer
        if (!esJunta) {
            if (c.sociId != perId) throw ErrorApp("Només pots desfer les teves consumicions.")
            if (System.currentTimeMillis() - c.ts > minuts * 60_000L)
                throw ErrorApp("Han passat més de $minuts minuts. Demana-ho a la junta.")
        }
        bd.withTransaction {
            dao.desaConsum(c.copy(anulatTs = System.currentTimeMillis(), anulatPer = perId))
            dao.producte(c.producteId)?.let { p -> if (p.controlaEstoc) dao.desaProducte(p.copy(estoc = p.estoc + c.quantitat)) }
        }
    }

    // ---------- Catàleg, socis i estoc ----------

    suspend fun desaProducte(p: Producte) {
        if (p.nom.isBlank()) throw ErrorApp("Posa un nom al producte.")
        dao.desaProducte(p.copy(nom = p.nom.trim()))
    }

    suspend fun desaSoci(s: Soci, pinNou: String? = null) {
        if (s.nom.isBlank()) throw ErrorApp("Posa el nom del soci.")
        if (s.email.isNotBlank() && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(s.email.trim()))
            throw ErrorApp("El correu no té un format vàlid.")
        if (dao.totsSocis().any { it.id != s.id && it.email.isNotBlank() && it.email.equals(s.email.trim(), ignoreCase = true) })
            throw ErrorApp("Ja hi ha un soci amb aquest correu.")
        var nou = s.copy(nom = s.nom.trim(), cognoms = s.cognoms.trim(), email = s.email.trim())
        if (pinNou != null) {
            if (!Pin.valid(pinNou)) throw ErrorApp("El PIN ha de tenir 4 xifres.")
            val sal = Pin.novaSal()
            nou = nou.copy(pinSal = sal, pinHash = Pin.hash(pinNou, sal))
        } else if (nou.pinHash.isBlank()) throw ErrorApp("Posa un PIN de 4 xifres.")
        dao.desaSoci(nou)
    }

    suspend fun reposar(producteId: String, quantitat: Int, perId: String) {
        if (quantitat == 0) return
        bd.withTransaction {
            val p = dao.producte(producteId) ?: return@withTransaction
            dao.desaProducte(p.copy(estoc = maxOf(0, p.estoc + quantitat)))
            dao.desaReposicio(Reposicio(nouId(), producteId, quantitat, System.currentTimeMillis(), perId))
        }
    }

    // ---------- Àpats ----------

    suspend fun desaApat(a: Apat) {
        if (a.titol.isBlank()) throw ErrorApp("Posa un títol a l'àpat.")
        if (mesBloquejat(mesDeData(a.data))) throw ErrorApp("El mes d'aquest àpat ja està tancat.")
        dao.desaApat(a.copy(titol = a.titol.trim()))
    }

    suspend fun eliminaApat(id: String) {
        val a = dao.apat(id) ?: return
        if (mesBloquejat(mesDeData(a.data))) throw ErrorApp("El mes d'aquest àpat ja està tancat.")
        dao.eliminaApat(id)
    }

    // ---------- Reserves ----------

    suspend fun creaReserva(sociId: String, data: String, franja: Franja, espai: String, persones: Int, notes: String) {
        if (persones < 1) throw ErrorApp("Indica quantes persones sereu.")
        if (data < avui()) throw ErrorApp("No es pot reservar un dia que ja ha passat.")
        val conflicte = dao.reservesDe(data).firstOrNull {
            it.espai == espai && (it.franja == franja || it.franja == Franja.TOT_EL_DIA || franja == Franja.TOT_EL_DIA)
        }
        if (conflicte != null) {
            val qui = dao.soci(conflicte.sociId)?.nomComplet ?: "un altre soci"
            throw ErrorApp("$espai ja està reservat aquell dia (${conflicte.franja.nom.lowercase()}) per $qui.")
        }
        dao.desaReserva(Reserva(nouId(), sociId, data, franja, espai, persones, notes.trim(), System.currentTimeMillis()))
    }

    suspend fun eliminaReserva(id: String) = dao.eliminaReserva(id)

    // ---------- Tancament de mes ----------

    /** Pas 2: crea un correu per a cada soci i un per a tresoreria, i obre el termini de reclamacions. */
    suspend fun prepararInformes(mes: String): List<Correu> {
        val cfg = config()
        val informes = informeMes(dao.totsSocis(), mes, dao.totsConsums(), dao.totsApats())
        if (informes.isEmpty()) throw ErrorApp("Aquest mes no hi ha cap consum.")
        val ara = System.currentTimeMillis()
        val limit = dataPlena(ara + cfg.diesReclamacio * 86_400_000L)
        fun desti(real: String) = if (cfg.modeProves) cfg.correuProves else real

        val correus = mutableListOf<Correu>()
        informes.filter { it.soci.email.isNotBlank() }.forEach { inf ->
            val linies = inf.linies.joinToString("\n") { l ->
                "  ${diaMes(l.ts)}  ${if (l.quantitat > 1) "${l.quantitat} × " else ""}${l.concepte}${if (l.detall.isNotBlank()) " (${l.detall})" else ""}  ${euros(l.import)}"
            }
            val cos = buildString {
                append("Hola, ${inf.soci.nom}:\n\n")
                append("Aquest és el resum del teu consum a ${cfg.nomSocietat} ${deMes(mes)}.\n\n$linies\n\n")
                append("Consumicions: ${euros(inf.totalConsum)}\n")
                if (inf.totalApats != 0L) append("Àpats compartits: ${euros(inf.totalApats)}\n")
                if (inf.totalAbonaments != 0L) append("Despeses que vas avançar: ${euros(inf.totalAbonaments)}\n")
                if (inf.total >= 0) append("\nTotal a carregar al teu compte: ${euros(inf.total)}\n\n")
                else append("\nSaldo a favor teu: ${euros(-inf.total)}. La junta te'l tornarà.\n\n")
                append("Si hi veus algun error, digues-ho a la junta abans del $limit. Després es passarà el rebut al banc.\n\n")
                append("Junta de ${cfg.nomSocietat}")
            }
            correus += Correu(nouId(), ara, desti(inf.soci.email), inf.soci.email, "${cfg.nomSocietat}: el teu consum ${deMes(mes)}", cos, inf.soci.id, mes)
        }
        val total = informes.sumOf { it.total }
        val tresoreria = cfg.correuTresoreria.ifBlank { cfg.compteGoogle ?: "" }
        if (tresoreria.isNotBlank()) {
            val cos = buildString {
                append("Resum ${deMes(mes)}\n\n")
                informes.forEach { append("  ${it.soci.nomComplet.padEnd(30)} ${it.soci.refBanc.padEnd(10)} ${euros(it.total)}\n") }
                append("\nTotal: ${euros(total)} (${informes.size} socis)\n")
                val sense = informes.filter { it.soci.email.isBlank() }
                if (sense.isNotEmpty()) append("Sense correu (cal avisar-los a part): ${sense.joinToString { it.soci.nomComplet }}\n")
                append("Termini de reclamacions fins al $limit.\n")
            }
            correus += Correu(nouId(), ara, desti(tresoreria), tresoreria, "${cfg.nomSocietat}: informe de consum ${deMes(mes)} per a tresoreria", cos, null, mes)
        }
        bd.withTransaction {
            dao.desaCorreus(correus)
            dao.desaTancament(Tancament(mes, EstatMes.INFORMAT, ara, null))
        }
        return correus
    }

    suspend fun tancarMes(mes: String) {
        val t = dao.tancament(mes) ?: throw ErrorApp("Primer s'han d'enviar els informes.")
        dao.desaTancament(t.copy(estat = EstatMes.TANCAT, tancatTs = System.currentTimeMillis()))
    }

    suspend fun reobrirMes(mes: String) = dao.eliminaTancament(mes)

    suspend fun marcaCorreu(id: String, estat: EstatCorreu, error: String? = null) {
        val c = dao.totsCorreus().firstOrNull { it.id == id } ?: return
        dao.desaCorreu(c.copy(estat = estat, error = error))
    }

    suspend fun correusPendents() = dao.correusPendents()

    // ---------- Còpies ----------

    suspend fun copia(): Copia = Copia(
        ts = System.currentTimeMillis(),
        config = config(),
        socis = dao.totsSocis(),
        productes = dao.totsProductes(),
        consums = dao.totsConsums(),
        reposicions = dao.totesReposicions(),
        apats = dao.totsApats(),
        reserves = dao.totesReserves(),
        tancaments = dao.totsTancaments(),
        correus = dao.totsCorreus(),
    )

    suspend fun restaura(c: Copia, mantenirCompteGoogle: Boolean = true) {
        val compte = config().compteGoogle
        bd.withTransaction {
            esborraTotIntern()
            c.socis.forEach { dao.desaSoci(it) }
            c.productes.forEach { dao.desaProducte(it) }
            dao.desaConsums(c.consums)
            c.reposicions.forEach { dao.desaReposicio(it) }
            c.apats.forEach { dao.desaApat(it) }
            c.reserves.forEach { dao.desaReserva(it) }
            c.tancaments.forEach { dao.desaTancament(it) }
            dao.desaCorreus(c.correus)
            val cfg = if (mantenirCompteGoogle && compte != null) c.config.copy(compteGoogle = compte) else c.config
            desaConfig(cfg)
        }
    }

    private suspend fun esborraTotIntern() {
        dao.buidaSocis(); dao.buidaProductes(); dao.buidaConsums(); dao.buidaReposicions()
        dao.buidaApats(); dao.buidaReserves(); dao.buidaTancaments(); dao.buidaCorreus(); dao.buidaConfig()
    }

    suspend fun carregaDadesProva() {
        val c = DadesProva.crea()
        restaura(c, mantenirCompteGoogle = true)
    }

    /** Configuració inicial sense dades de prova: crea el primer membre de la junta. */
    suspend fun comencaDeZero(nom: String, cognoms: String, email: String, pin: String) {
        bd.withTransaction {
            esborraTotIntern()
            desaConfig(Config())
            desaSoci(Soci(nouId(), nom, cognoms, email, Rol.JUNTA, "", "", "GB-0001"), pin)
        }
    }
}
