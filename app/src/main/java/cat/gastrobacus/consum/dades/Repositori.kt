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
    val convidats: List<ConvidatDia> = emptyList(),
    val bizums: List<Bizum> = emptyList(),
    val avisos: List<AvisEstoc> = emptyList(),
    val peticions: List<Peticio> = emptyList(),
) {
    fun soci(id: String?) = socis.firstOrNull { it.id == id }
    fun producte(id: String?) = productes.firstOrNull { it.id == id }
    fun convidat(id: String?) = convidats.firstOrNull { it.id == id }
    fun estatMes(mes: String) = tancaments.firstOrNull { it.mes == mes }?.estat ?: EstatMes.OBERT
    fun tancament(mes: String) = tancaments.firstOrNull { it.mes == mes }
    fun informe(soci: Soci, mes: String) = informeSoci(soci, mes, consums, apats)
    fun informes(mes: String) = informeMes(socis, mes, consums, apats)
    val socisActius get() = socis.filter { it.actiu }.sortedWith(compareBy(java.text.Collator.getInstance(CATALA)) { it.nom })
    val esBuida get() = carregat && socis.isEmpty()
    fun convidatsActius(ara: Long = System.currentTimeMillis()) = convidats.filter { it.caduca > ara }.sortedBy { it.nom }
    val bizumsPendents get() = bizums.filter { it.estat == EstatBizum.PENDENT }
    val avisosNous get() = avisos.filter { !it.llegit }
    val peticionsPendents get() = peticions.filter { it.estat == EstatPeticio.PENDENT }
    fun favoritsDe(sociId: String?) = favorits(productes, consums, sociId, config.favoritsPerSoci)
}

data class ItemApunt(val producteId: String, val quantitat: Int, val convidat: String? = null)

@Serializable
data class Copia(
    val versio: Int = 2,
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
    val convidats: List<ConvidatDia> = emptyList(),
    val bizums: List<Bizum> = emptyList(),
    val avisos: List<AvisEstoc> = emptyList(),
    val peticions: List<Peticio> = emptyList(),
)

data class ResultatImportacio(val nous: Int, val actualitzats: Int, val errors: List<String>)

class Repositori(private val bd: BaseDades, ambit: CoroutineScope) {
    private val dao = bd.dao()

    val estat: StateFlow<Estat> = combine(
        combine(dao.socis(), dao.productes(), dao.consums(), dao.reposicions()) { s, p, c, r -> listOf(s, p, c, r) },
        combine(dao.apats(), dao.reserves(), dao.tancaments(), dao.correus(), dao.config()) { a, re, t, co, cf -> listOf(a, re, t, co, cf) },
        combine(dao.convidats(), dao.bizums(), dao.avisos(), dao.peticions()) { cv, b, av, pe -> listOf(cv, b, av, pe) },
    ) { x, y, z ->
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
            convidats = z[0] as List<ConvidatDia>,
            bizums = z[1] as List<Bizum>,
            avisos = z[2] as List<AvisEstoc>,
            peticions = z[3] as List<Peticio>,
        )
    }.stateIn(ambit, SharingStarted.Eagerly, Estat())

    suspend fun config(): Config = dao.configActual()?.let { JSON.decodeFromString<Config>(it.json) } ?: Config()
    suspend fun desaConfig(c: Config) = dao.desaConfig(ConfigFila(json = JSON.encodeToString(c)))
    suspend fun actualitzaConfig(f: (Config) -> Config) = desaConfig(f(config()))

    private suspend fun mesBloquejat(mes: String) = dao.tancament(mes)?.estat == EstatMes.TANCAT

    /** Deixa un correu a la cua d'enviament (en mode de proves va a l'adreça de proves). */
    private suspend fun encua(per: String, assumpte: String, cos: String) {
        if (per.isBlank()) return
        val cfg = config()
        val desti = if (cfg.modeProves) cfg.correuProves else per
        dao.desaCorreu(Correu(nouId(), System.currentTimeMillis(), desti, per, "${cfg.nomSocietat}: $assumpte", cos))
    }

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

    /** Apunta les consumicions al compte del soci (o les registra com a pagades per Bizum). Retorna el total. */
    suspend fun apuntar(
        sociId: String, items: List<ItemApunt>, origen: Origen = Origen.TAULETA, ts: Long = System.currentTimeMillis(),
        bizumId: String? = null,
    ): Long {
        if (mesBloquejat(mesDe(ts))) throw ErrorApp("Aquest mes ja està tancat.")
        var total = 0L
        bd.withTransaction {
            for (it in items) {
                val p = dao.producte(it.producteId) ?: continue
                if (it.quantitat <= 0) continue
                val preu = p.preuPer(it.convidat != null)
                dao.desaConsum(
                    Consum(
                        nouId(), sociId, p.id, p.nom, it.quantitat, preu, it.convidat, ts, origen,
                        pagament = if (bizumId != null) Pagament.BIZUM else Pagament.FACTURA, bizumId = bizumId,
                    ),
                )
                desaAmbAvisos(
                    p.copy(
                        estoc = if (p.controlaEstoc) maxOf(0, p.estoc - it.quantitat) else p.estoc,
                        comptador = p.comptador + it.quantitat,
                    ),
                )
                total += preu * it.quantitat
            }
        }
        return total
    }

    /** Desa el producte i, si ha arribat a l'estoc mínim o a la quantitat consumida, crea l'avís i el correu. */
    private suspend fun desaAmbAvisos(p: Producte) {
        var q = p
        val ara = System.currentTimeMillis()
        val correuAvisos = config().correuAvisos
        if (q.controlaEstoc && q.estocMinim > 0 && q.estoc <= q.estocMinim && !q.avisMinimFet) {
            val m = "${q.nom}: en queden ${q.estoc} (el mínim és ${q.estocMinim})."
            dao.desaAvis(AvisEstoc(nouId(), q.id, TipusAvis.ESTOC_MINIM, m, ara))
            encua(correuAvisos, "cal reposar ${q.nom}", "$m\n\nAvís automàtic de la tauleta.")
            q = q.copy(avisMinimFet = true)
        }
        if (q.avisConsum > 0 && q.comptador >= q.avisConsum && !q.avisConsumFet) {
            val m = "${q.nom}: se n'han consumit ${q.comptador} des de la darrera comanda (avís a partir de ${q.avisConsum})."
            dao.desaAvis(AvisEstoc(nouId(), q.id, TipusAvis.CONSUM, m, ara))
            encua(correuAvisos, "${q.nom}, ${q.comptador} consumits", "$m\n\nAvís automàtic de la tauleta.")
            q = q.copy(avisConsumFet = true)
        }
        dao.desaProducte(q)
    }

    suspend fun anular(consumId: String, perId: String, esJunta: Boolean) {
        val c = dao.consum(consumId) ?: return
        if (c.anulat) return
        if (mesBloquejat(mesDe(c.ts))) throw ErrorApp("Aquest mes ja està tancat i no es pot modificar.")
        val minuts = config().minutsDesfer
        if (!esJunta) {
            if (c.sociId != perId) throw ErrorApp("Només pots desfer les teves consumicions.")
            if (c.pagament == Pagament.BIZUM) throw ErrorApp("Aquesta consumició s'ha pagat per Bizum. Demana-ho a la junta.")
            if (System.currentTimeMillis() - c.ts > minuts * 60_000L)
                throw ErrorApp("Han passat més de $minuts minuts. Demana-ho a la junta.")
        }
        bd.withTransaction {
            dao.desaConsum(c.copy(anulatTs = System.currentTimeMillis(), anulatPer = perId))
            dao.producte(c.producteId)?.let { p ->
                dao.desaProducte(
                    p.copy(estoc = if (p.controlaEstoc) p.estoc + c.quantitat else p.estoc, comptador = maxOf(0, p.comptador - c.quantitat)),
                )
            }
            // Si era part d'un Bizum, se'n descompta l'import
            c.bizumId?.let { id -> dao.bizum(id)?.let { b -> dao.desaBizum(b.copy(import = maxOf(0, b.import - c.import))) } }
        }
    }

    // ---------- Convidats del dia ----------

    suspend fun afegeixConvidat(sociId: String, nom: String): ConvidatDia {
        val n = nom.trim()
        if (n.isBlank()) throw ErrorApp("Escriu el nom del convidat.")
        val ara = System.currentTimeMillis()
        dao.totsConvidats().firstOrNull { it.sociId == sociId && it.nom.equals(n, true) && it.caduca > ara }?.let { return it }
        val c = ConvidatDia(nouId(), n, sociId, ara, caducaConvidat(ara, config().horaFiDia))
        dao.desaConvidat(c)
        return c
    }

    suspend fun treuConvidat(id: String) {
        dao.totsConvidats().firstOrNull { it.id == id }?.let { dao.desaConvidat(it.copy(caduca = System.currentTimeMillis() - 1)) }
    }

    suspend fun netejaConvidats() = dao.netejaConvidats(System.currentTimeMillis() - 30L * 86_400_000L)

    // ---------- Bizum ----------

    /** Registra les consumicions com a pagades per Bizum. Queden pendents fins que la junta confirma que han arribat. */
    suspend fun pagaAmbBizum(sociId: String, nomPagador: String, items: List<ItemApunt>, concepte: String): Bizum {
        val id = nouId()
        var b: Bizum? = null
        bd.withTransaction {
            val total = apuntar(sociId, items, Origen.TAULETA, System.currentTimeMillis(), bizumId = id)
            b = Bizum(id, sociId, nomPagador, concepte, total, System.currentTimeMillis())
            dao.desaBizum(b!!)
        }
        return b!!
    }

    suspend fun confirmaBizum(id: String, perId: String, rebut: Boolean) {
        val b = dao.bizum(id) ?: return
        bd.withTransaction {
            dao.desaBizum(b.copy(estat = if (rebut) EstatBizum.REBUT else EstatBizum.NO_ARRIBAT, confirmatPer = perId, confirmatTs = System.currentTimeMillis()))
            if (!rebut) {
                // No ha arribat: les consumicions passen a la factura del mes
                val consums = dao.consumsDeBizum(id)
                if (consums.any { mesBloquejat(mesDe(it.ts)) }) throw ErrorApp("El mes d'aquestes consumicions ja està tancat. Reobre'l primer.")
                dao.desaConsums(consums.map { it.copy(pagament = Pagament.FACTURA) })
            }
            // Bizum d'una persona de fora en una activitat oberta
            if (b.apatId != null && b.externId != null) dao.apat(b.apatId)?.let { a ->
                dao.desaApat(a.copy(externs = a.externs.map { e -> if (e.id == b.externId) e.copy(pagat = rebut) else e }))
            }
        }
    }

    suspend fun reobreBizum(id: String) {
        dao.bizum(id)?.let { dao.desaBizum(it.copy(estat = EstatBizum.PENDENT, confirmatPer = null, confirmatTs = null)) }
    }

    // ---------- Catàleg, socis i estoc ----------

    suspend fun desaProducte(p: Producte) {
        if (p.nom.isBlank()) throw ErrorApp("Posa un nom al producte.")
        if (dao.totsProductes().any { it.id != p.id && it.nom.equals(p.nom.trim(), ignoreCase = true) })
            throw ErrorApp("Ja hi ha un producte amb aquest nom.")
        // Si es canvia el mínim o el llindar, es tornen a activar els avisos
        val anterior = dao.producte(p.id)
        var nou = p.copy(nom = p.nom.trim())
        if (anterior != null) {
            if (nou.estoc > nou.estocMinim) nou = nou.copy(avisMinimFet = false)
            if (nou.avisConsum != anterior.avisConsum) nou = nou.copy(avisConsumFet = nou.avisConsum in 1..nou.comptador)
        }
        dao.desaProducte(nou)
    }

    suspend fun desaSoci(s: Soci, pinNou: String? = null) {
        if (s.nom.isBlank()) throw ErrorApp("Posa el nom del soci.")
        if (s.email.isNotBlank() && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(s.email.trim()))
            throw ErrorApp("El correu no té un format vàlid.")
        if (dao.totsSocis().any { it.id != s.id && it.email.isNotBlank() && it.email.equals(s.email.trim(), ignoreCase = true) })
            throw ErrorApp("Ja hi ha un soci amb aquest correu.")
        var nou = s.copy(nom = s.nom.trim(), cognoms = s.cognoms.trim(), email = s.email.trim())
        if (nou.periodes.isEmpty()) nou = nou.copy(periodes = listOf(Periode(avui())))
        if (pinNou != null) {
            if (!Pin.valid(pinNou)) throw ErrorApp("El PIN ha de tenir 4 xifres.")
            val sal = Pin.novaSal()
            nou = nou.copy(pinSal = sal, pinHash = Pin.hash(pinNou, sal))
        } else if (nou.pinHash.isBlank()) throw ErrorApp("Posa un PIN de 4 xifres.")
        dao.desaSoci(nou)
    }

    /** Consum dels mesos encara no tancats (el que el soci deu i encara no s'ha passat al banc). */
    suspend fun pendentDe(sociId: String): Pair<List<String>, Long> {
        val s = dao.soci(sociId) ?: return emptyList<String>() to 0L
        val consums = dao.totsConsums(); val apats = dao.totsApats()
        val mesos = (consums.filter { it.sociId == sociId }.map { mesDe(it.ts) } + apats.map { mesDeData(it.data) })
            .distinct().filter { !mesBloquejat(it) }.sorted()
        val amb = mesos.filter { informeSoci(s, it, consums, apats).linies.isNotEmpty() }
        return amb to amb.sumOf { informeSoci(s, it, consums, apats).total }
    }

    /** Dona de baixa el soci. Si [informeFinal], li envia el detall de tot el que té pendent i n'avisa tresoreria. */
    suspend fun donaDeBaixa(sociId: String, data: String, informeFinal: Boolean) {
        val s = dao.soci(sociId) ?: return
        if (s.esJunta && dao.totsSocis().count { it.esJunta && it.actiu } <= 1) throw ErrorApp("És l'únic membre de la junta: abans cal que n'hi hagi un altre.")
        val periodes = s.periodes.ifEmpty { listOf(Periode(data)) }
        val tancats = periodes.dropLast(1) + periodes.last().copy(baixa = data)
        if (informeFinal) {
            val (mesos, total) = pendentDe(sociId)
            if (mesos.isNotEmpty()) {
                val consums = dao.totsConsums(); val apats = dao.totsApats()
                val cfg = config()
                val cos = buildString {
                    append("Hola, ${s.nom}:\n\nT'enviem la liquidació del teu consum pendent a ${cfg.nomSocietat} en donar-te de baixa.\n\n")
                    mesos.forEach { m ->
                        val inf = informeSoci(s, m, consums, apats)
                        append("${nomMes(m)}\n")
                        inf.linies.forEach { l -> append("  ${diaMes(l.ts)}  ${l.concepte}${if (l.detall.isNotBlank()) " (${l.detall})" else ""}  ${euros(l.import)}\n") }
                        append("  Subtotal: ${euros(inf.total)}\n\n")
                    }
                    append("Total pendent: ${euros(total)}\n\nGràcies per haver format part de la societat.\nJunta de ${cfg.nomSocietat}")
                }
                encua(s.email, "liquidació en donar-te de baixa", cos)
                encua(cfg.correuTresoreria.ifBlank { cfg.compteGoogle ?: "" }, "baixa de ${s.nomComplet}",
                    "${s.nomComplet} (ref. ${s.refBanc}) es dona de baixa el ${dataLlarga(data)}.\nTé pendent ${euros(total)} de ${mesos.joinToString { nomMes(it) }}.\n\n$cos")
            }
        }
        dao.desaSoci(s.copy(actiu = false, periodes = tancats))
    }

    suspend fun donaDAlta(sociId: String, data: String) {
        val s = dao.soci(sociId) ?: return
        dao.desaSoci(s.copy(actiu = true, periodes = s.periodes + Periode(data)))
    }

    /** Arriba una comanda: ENTRADA suma unitats; RECOMPTE fixa l'estoc real. Totes dues posen el comptador a zero. */
    suspend fun registraComanda(producteId: String, quantitat: Int, tipus: TipusReposicio, perId: String) {
        if (tipus == TipusReposicio.ENTRADA && quantitat <= 0) throw ErrorApp("Indica quantes unitats han arribat.")
        if (quantitat < 0) throw ErrorApp("L'estoc no pot ser negatiu.")
        bd.withTransaction {
            val p = dao.producte(producteId) ?: return@withTransaction
            val nouEstoc = if (tipus == TipusReposicio.ENTRADA) p.estoc + quantitat else quantitat
            dao.desaProducte(
                p.copy(estoc = nouEstoc, comptador = 0, darreraComandaTs = System.currentTimeMillis(), avisMinimFet = nouEstoc <= p.estocMinim && p.avisMinimFet, avisConsumFet = false),
            )
            dao.desaReposicio(Reposicio(nouId(), producteId, quantitat, System.currentTimeMillis(), perId, tipus, p.estoc, p.comptador))
        }
    }

    suspend fun marcaAvisosLlegits() = dao.marcaAvisosLlegits()

    // ---------- Peticions i comandes ----------

    suspend fun creaPeticio(sociId: String, tipus: TipusPeticio, producteId: String?, text: String) {
        val p = producteId?.let { dao.producte(it) }
        val t = text.trim().ifBlank { p?.nom ?: "" }
        if (t.isBlank()) throw ErrorApp(if (tipus == TipusPeticio.FALTA) "Tria el producte que falta." else "Escriu quin producte proposes.")
        dao.desaPeticio(Peticio(nouId(), sociId, tipus, producteId, t, System.currentTimeMillis()))
        val qui = dao.soci(sociId)?.nomComplet ?: ""
        encua(
            config().correuAvisos,
            if (tipus == TipusPeticio.FALTA) "falta $t" else "proposta: $t",
            (if (tipus == TipusPeticio.FALTA) "$qui avisa que falta: $t" else "$qui proposa tenir: $t") + "\n\nEs pot veure a Gestió > Comandes de la tauleta.",
        )
    }

    suspend fun resolPeticio(p: Peticio, estat: EstatPeticio) = dao.desaPeticio(p.copy(estat = estat))

    /** Prepara el correu de comanda al proveïdor. */
    suspend fun preparaComanda(linies: List<Pair<Producte, Int>>, notes: String, per: String) {
        val l = linies.filter { it.second > 0 }
        if (l.isEmpty()) throw ErrorApp("La comanda no té cap producte.")
        if (!per.contains("@")) throw ErrorApp("Indica el correu del proveïdor.")
        val cfg = config()
        val cos = buildString {
            append("Bon dia,\n\nUs fem la comanda següent per a la societat ${cfg.nomSocietat}:\n\n")
            l.forEach { (p, q) -> append("  $q × ${p.nom}\n") }
            if (notes.isNotBlank()) append("\n${notes.trim()}\n")
            append("\nGràcies.\nJunta de ${cfg.nomSocietat}")
        }
        encua(per, "comanda", cos)
    }

    // ---------- Àpats i activitats ----------

    suspend fun desaApat(a: Apat, perId: String) {
        if (a.titol.isBlank()) throw ErrorApp("Posa un títol a l'activitat.")
        if (mesBloquejat(mesDeData(a.data))) throw ErrorApp("El mes d'aquesta activitat ja està tancat.")
        val anterior = dao.reservaDApat(a.id)
        bd.withTransaction {
            dao.desaApat(a.copy(titol = a.titol.trim()))
            // L'activitat reserva l'espai (si encara no ha passat)
            if (a.data >= avui()) {
                val persones = a.assistents.sumOf { 1 + it.convidats } + a.externs.size
                comprovaConflicte(a.data, a.franja, a.espai, a.espai == TOT_EL_LOCAL, excloent = anterior?.id)
                val r = Reserva(
                    anterior?.id ?: nouId(), a.organitzadorId, a.data, a.franja, a.espai, maxOf(1, persones),
                    "Activitat ${a.tipus.nom.lowercase()}: ${a.titol.trim()}", anterior?.creada ?: System.currentTimeMillis(),
                    exclusiu = a.espai == TOT_EL_LOCAL, apatId = a.id,
                )
                dao.desaReserva(r)
                val canvi = anterior == null || anterior.data != r.data || anterior.franja != r.franja || anterior.espai != r.espai
                if (canvi) avisaReserva(r, if (anterior == null) "nova reserva" else "reserva modificada", perId)
            }
        }
    }

    suspend fun eliminaApat(id: String, perId: String) {
        val a = dao.apat(id) ?: return
        if (mesBloquejat(mesDeData(a.data))) throw ErrorApp("El mes d'aquesta activitat ja està tancat.")
        bd.withTransaction {
            dao.eliminaApat(id)
            dao.reservaDApat(id)?.let { dao.eliminaReserva(it.id); avisaReserva(it, "reserva anul·lada", perId) }
        }
    }

    /** Persona de fora: es crea el Bizum pendent amb la seva part (menjar + cadira). */
    suspend fun bizumExtern(apat: Apat, extern: Extern, import: Long) {
        dao.totsBizums().firstOrNull { it.externId == extern.id && it.estat == EstatBizum.PENDENT }?.let { return }
        dao.desaBizum(Bizum(nouId(), null, extern.nom, "Activitat: ${apat.titol}", import, System.currentTimeMillis(), apatId = apat.id, externId = extern.id))
    }

    // ---------- Reserves ----------

    private suspend fun comprovaConflicte(data: String, franja: Franja, espai: String, exclusiu: Boolean, excloent: String? = null) {
        val conflicte = dao.reservesDe(data).filter { it.id != excloent }.firstOrNull {
            val mateixaFranja = it.franja == franja || it.franja == Franja.TOT_EL_DIA || franja == Franja.TOT_EL_DIA
            mateixaFranja && (exclusiu || it.exclusiu || it.espai == espai)
        }
        if (conflicte != null) {
            val qui = dao.soci(conflicte.sociId)?.nomComplet ?: "un altre soci"
            val que = if (conflicte.exclusiu) "Tot el local" else conflicte.espai
            throw ErrorApp("$que ja està reservat aquell dia (${conflicte.franja.nom.lowercase()}) per $qui.")
        }
    }

    private suspend fun avisaReserva(r: Reserva, que: String, perId: String) {
        val qui = dao.soci(r.sociId)?.nomComplet ?: ""
        val perQui = dao.soci(perId)?.nomComplet ?: qui
        encua(
            config().correuReserves, "$que ${dataLlarga(r.data).lowercase()}",
            "${que.replaceFirstChar { it.uppercase() }}:\n\n" +
                "  Dia: ${dataLlarga(r.data)}, ${r.franja.nom.lowercase()}\n" +
                "  Espai: ${if (r.exclusiu) "Tot el local" else r.espai}\n" +
                "  Persones: ${r.persones}\n  A nom de: $qui\n" +
                (if (r.notes.isNotBlank()) "  Notes: ${r.notes}\n" else "") +
                "\nFet per $perQui des de la tauleta.",
        )
    }

    suspend fun creaReserva(sociId: String, data: String, franja: Franja, espai: String, persones: Int, notes: String) {
        if (persones < 1) throw ErrorApp("Indica quantes persones sereu.")
        if (data < avui()) throw ErrorApp("No es pot reservar un dia que ja ha passat.")
        val exclusiu = espai == TOT_EL_LOCAL
        comprovaConflicte(data, franja, espai, exclusiu)
        val r = Reserva(nouId(), sociId, data, franja, espai, persones, notes.trim(), System.currentTimeMillis(), exclusiu)
        bd.withTransaction {
            dao.desaReserva(r)
            avisaReserva(r, "nova reserva", sociId)
        }
    }

    suspend fun eliminaReserva(id: String, perId: String) {
        val r = dao.totesReserves().firstOrNull { it.id == id } ?: return
        if (r.apatId != null) throw ErrorApp("Aquesta reserva és d'una activitat. Anul·la-la des d'Àpats.")
        bd.withTransaction {
            dao.eliminaReserva(id)
            avisaReserva(r, "reserva anul·lada", perId)
        }
    }

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
                if (inf.totalApats != 0L) append("Àpats i activitats: ${euros(inf.totalApats)}\n")
                if (inf.totalAbonaments != 0L) append("Despeses que vas avançar: ${euros(inf.totalAbonaments)}\n")
                if (inf.total >= 0) append("\nTotal a carregar al teu compte: ${euros(inf.total)}\n\n")
                else append("\nSaldo a favor teu: ${euros(-inf.total)}. La junta te'l tornarà.\n\n")
                append("El que vas pagar per Bizum no hi surt.\n")
                append("Si hi veus algun error, digues-ho a la junta abans del $limit. Després es passarà el rebut al banc.\n\n")
                append("Junta de ${cfg.nomSocietat}")
            }
            correus += Correu(nouId(), ara, desti(inf.soci.email), inf.soci.email, "${cfg.nomSocietat}: el teu consum ${deMes(mes)}", cos, inf.soci.id, mes)
        }
        val total = informes.sumOf { it.total }
        val tresoreria = cfg.correuTresoreria.ifBlank { cfg.compteGoogle ?: "" }
        if (tresoreria.isNotBlank()) {
            val bizumsMes = dao.totsBizums().filter { mesDe(it.ts) == mes }
            val cos = buildString {
                append("Resum ${deMes(mes)}\n\n")
                informes.forEach { append("  ${it.soci.nomComplet.padEnd(30)} ${it.soci.refBanc.padEnd(10)} ${euros(it.total)}\n") }
                append("\nTotal a facturar: ${euros(total)} (${informes.size} socis)\n")
                append("Cobrat per Bizum: ${euros(bizumsMes.filter { it.estat == EstatBizum.REBUT }.sumOf { it.import })}\n")
                val pend = bizumsMes.filter { it.estat == EstatBizum.PENDENT }
                if (pend.isNotEmpty()) append("Bizums pendents de confirmar: ${pend.size} (${euros(pend.sumOf { it.import })})\n")
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

    // ---------- Importació des de CSV ----------

    suspend fun importaProductes(files: List<FilaProducte>): ResultatImportacio {
        var nous = 0; var act = 0
        bd.withTransaction {
            val existents = dao.totsProductes()
            files.forEach { f ->
                val e = existents.firstOrNull { it.nom.equals(f.nom, ignoreCase = true) }
                if (e == null) {
                    dao.desaProducte(Producte(nouId(), f.nom, f.categoria, f.preu, f.preuConvidat, f.estoc ?: 0, f.estocMinim ?: 0, (f.estocMinim ?: 0) > 0 || (f.estoc ?: 0) > 0, true, f.favorit ?: false, avisConsum = f.avisConsum ?: 0))
                    nous++
                } else {
                    dao.desaProducte(
                        e.copy(
                            categoria = f.categoria, preu = f.preu, preuConvidat = f.preuConvidat, estoc = f.estoc ?: e.estoc,
                            estocMinim = f.estocMinim ?: e.estocMinim, favorit = f.favorit ?: e.favorit, avisConsum = f.avisConsum ?: e.avisConsum,
                            controlaEstoc = e.controlaEstoc || (f.estocMinim ?: 0) > 0, actiu = true,
                        ),
                    )
                    act++
                }
            }
        }
        return ResultatImportacio(nous, act, emptyList())
    }

    suspend fun importaSocis(files: List<FilaSoci>): ResultatImportacio {
        var nous = 0; var act = 0
        val errors = mutableListOf<String>()
        bd.withTransaction {
            val existents = dao.totsSocis()
            var seguent = existents.size + 1
            files.forEach { f ->
                val e = existents.firstOrNull { (f.email.isNotBlank() && it.email.equals(f.email, true)) || (it.nom.equals(f.nom, true) && it.cognoms.equals(f.cognoms, true)) }
                if (e == null) {
                    if (f.pin == null) { errors += "${f.nom} ${f.cognoms}: falta el PIN"; return@forEach }
                    val sal = Pin.novaSal()
                    dao.desaSoci(
                        Soci(
                            nouId(), f.nom, f.cognoms, f.email, if (f.junta) Rol.JUNTA else Rol.SOCI, Pin.hash(f.pin, sal), sal,
                            f.refBanc.ifBlank { "GB-%04d".format(seguent) }, true, listOf(Periode(f.dataAlta ?: avui())),
                        ),
                    )
                    seguent++; nous++
                } else {
                    var s = e.copy(email = f.email.ifBlank { e.email }, refBanc = f.refBanc.ifBlank { e.refBanc }, rol = if (f.junta) Rol.JUNTA else e.rol)
                    if (f.pin != null) { val sal = Pin.novaSal(); s = s.copy(pinSal = sal, pinHash = Pin.hash(f.pin, sal)) }
                    dao.desaSoci(s)
                    act++
                }
            }
        }
        return ResultatImportacio(nous, act, errors)
    }

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
        convidats = dao.totsConvidats(),
        bizums = dao.totsBizums(),
        avisos = dao.totsAvisos(),
        peticions = dao.totesPeticions(),
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
            c.convidats.forEach { dao.desaConvidat(it) }
            c.bizums.forEach { dao.desaBizum(it) }
            c.avisos.forEach { dao.desaAvis(it) }
            c.peticions.forEach { dao.desaPeticio(it) }
            val cfg = if (mantenirCompteGoogle && compte != null) c.config.copy(compteGoogle = compte) else c.config
            desaConfig(cfg)
        }
    }

    private suspend fun esborraTotIntern() {
        dao.buidaSocis(); dao.buidaProductes(); dao.buidaConsums(); dao.buidaReposicions()
        dao.buidaApats(); dao.buidaReserves(); dao.buidaTancaments(); dao.buidaCorreus(); dao.buidaConfig()
        dao.buidaConvidats(); dao.buidaBizums(); dao.buidaAvisos(); dao.buidaPeticions()
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
