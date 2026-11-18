package cat.gastrobacus.consum.dades

import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth

/** Dades fictícies per provar l'app. PIN de tots els socis: 1234. */
object DadesProva {
    const val PIN = "1234"

    private val SOCIS = listOf(
        Triple("Jordi", "Puig Farré", Rol.JUNTA), Triple("Montse", "Solé Vidal", Rol.JUNTA), Triple("Pere", "Capdevila Roca", Rol.JUNTA),
        Triple("Anna", "Bosch Serra", Rol.SOCI), Triple("Ramon", "Gené Cases", Rol.SOCI), Triple("Núria", "Florensa Pla", Rol.SOCI),
        Triple("Josep", "Marsol Bonet", Rol.SOCI), Triple("Carme", "Riu Sabaté", Rol.SOCI), Triple("Xavier", "Torrent Mas", Rol.SOCI),
        Triple("Teresa", "Llobet Ibars", Rol.SOCI), Triple("Joan", "Pujol Arqué", Rol.SOCI), Triple("Mercè", "Badia Gort", Rol.SOCI),
        Triple("Antoni", "Segura Coll", Rol.SOCI), Triple("Rosa", "Vilanova Pons", Rol.SOCI), Triple("Francesc", "Escolà Mir", Rol.SOCI),
        Triple("Laia", "Roig Tarragó", Rol.SOCI), Triple("Miquel", "Balcells Font", Rol.SOCI), Triple("Pilar", "Companys Ros", Rol.SOCI),
        Triple("Enric", "Sanuy Prats", Rol.SOCI), Triple("Glòria", "Fabregat Nadal", Rol.SOCI), Triple("Lluís", "Oliva Camps", Rol.SOCI),
        Triple("Marta", "Jové Agustí", Rol.SOCI), Triple("Ferran", "Gilabert Sala", Rol.SOCI), Triple("Dolors", "Sendra Mor", Rol.SOCI),
    )

    private data class P(val nom: String, val cat: Categoria, val preu: Long, val conv: Long?, val estoc: Int, val minim: Int)

    private val PRODUCTES = listOf(
        P("Cervesa Moritz 33 cl", Categoria.CERVESA, 130, 160, 96, 24),
        P("Estrella Damm 33 cl", Categoria.CERVESA, 130, 160, 72, 24),
        P("Cervesa sense alcohol", Categoria.CERVESA, 120, 150, 30, 12),
        P("Quinto Damm 20 cl", Categoria.CERVESA, 90, 110, 48, 24),
        P("Cervesa artesana Lleida", Categoria.CERVESA, 250, 300, 18, 6),
        P("Vi negre Costers del Segre", Categoria.VI, 650, 800, 14, 6),
        P("Vi blanc Costers del Segre", Categoria.VI, 600, 750, 9, 6),
        P("Vi rosat", Categoria.VI, 550, 700, 6, 4),
        P("Copa de vi de la casa", Categoria.VI, 100, 150, 0, 0),
        P("Cava brut", Categoria.VI, 700, 900, 8, 4),
        P("Vermut de la casa", Categoria.VI, 150, 200, 0, 0),
        P("Coca-Cola", Categoria.REFRESCOS, 120, 150, 40, 12),
        P("Coca-Cola Zero", Categoria.REFRESCOS, 120, 150, 36, 12),
        P("Fanta de taronja", Categoria.REFRESCOS, 120, 150, 20, 12),
        P("Fanta de llimona", Categoria.REFRESCOS, 120, 150, 10, 12),
        P("Tònica", Categoria.REFRESCOS, 120, 150, 24, 12),
        P("Bitter Kas", Categoria.REFRESCOS, 140, 170, 12, 6),
        P("Suc de préssec", Categoria.REFRESCOS, 110, 140, 15, 6),
        P("Aigua 50 cl", Categoria.AIGUA, 50, 80, 60, 24),
        P("Aigua 1,5 l", Categoria.AIGUA, 80, 100, 24, 12),
        P("Aigua amb gas", Categoria.AIGUA, 90, 120, 18, 6),
        P("Cafè", Categoria.CAFE, 60, 100, 0, 0),
        P("Tallat", Categoria.CAFE, 70, 110, 0, 0),
        P("Ratafia", Categoria.CAFE, 150, 200, 0, 0),
        P("Gin-tònic", Categoria.CAFE, 400, 500, 0, 0),
        P("Patates fregides", Categoria.PICAR, 100, 130, 30, 10),
        P("Olives", Categoria.PICAR, 120, 150, 14, 6),
        P("Fruits secs", Categoria.PICAR, 150, 180, 12, 6),
        P("Llonganissa (ració)", Categoria.PICAR, 300, 350, 8, 4),
        P("Formatge (ració)", Categoria.PICAR, 350, 400, 4, 4),
    )

    private fun senseAccents(t: String) =
        Normalizer.normalize(t, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

    fun crea(ara: Long = System.currentTimeMillis()): Copia {
        val r = java.util.Random(42)
        val socis = SOCIS.mapIndexed { i, (nom, cognoms, rol) ->
            val sal = "prova-$i"
            Soci(
                id = "s${i + 1}", nom = nom, cognoms = cognoms,
                email = "${senseAccents(nom)}.${senseAccents(cognoms.substringBefore(' '))}@exemple.cat",
                rol = rol, pinHash = Pin.hash(PIN, sal), pinSal = sal, refBanc = "GB-%04d".format(i + 1),
                periodes = listOf(Periode("%d-%02d-01".format(2012 + i % 13, 1 + i % 12))),
            )
        }
        val productes = PRODUCTES.mapIndexed { i, p ->
            Producte(
                "p${i + 1}", p.nom, p.cat, p.preu, p.conv, p.estoc, p.minim, controlaEstoc = p.minim > 0,
                favorit = i == 0 || i == 21, avisConsum = if (i < 2) 48 else 0,
            )
        }.toMutableList()

        // Consumicions des de l'1 del mes passat fins fa una hora
        val avui = LocalDate.now(ZONA)
        val inici = avui.minusMonths(1).withDayOfMonth(1)
        val populars = listOf(0, 0, 1, 1, 3, 8, 10, 11, 18, 21, 22, 25, 26)
        val convidats = listOf("Marc", "Sílvia", "el cunyat", "Albert", "Elena")
        val consums = mutableListOf<Consum>()
        var dia = inici
        while (!dia.isAfter(avui)) {
            val visites = when (dia.dayOfWeek.value) { 5, 6 -> 12; 7 -> 7; else -> 4 }
            repeat(visites) {
                val soci = socis[r.nextInt(socis.size)]
                repeat(1 + r.nextInt(3)) {
                    val p = productes[if (r.nextDouble() < 0.75) populars[r.nextInt(populars.size)] else r.nextInt(productes.size)]
                    val conv = if (r.nextDouble() < 0.08) convidats[r.nextInt(convidats.size)] else null
                    val ts = dia.atTime(12 + r.nextInt(10), r.nextInt(59)).atZone(ZONA).toInstant().toEpochMilli()
                    if (ts < ara - 3_600_000L)
                        consums += Consum(nouId(), soci.id, p.id, p.nom, 1, p.preuPer(conv != null), conv, ts, Origen.TAULETA)
                }
            }
            dia = dia.plusDays(1)
        }

        // Tres socis amb un consum de fa 50 dies que encara no han pagat (surten en vermell a «Qui ets?»)
        listOf("s5", "s9", "s14").forEach { id ->
            val p = productes[0]
            consums += Consum(nouId(), id, p.id, p.nom, 2, p.preu, null, ara - 50 * 86_400_000L, Origen.TAULETA)
        }

        // Uns quants pagaments per Bizum dels darrers dies (dos confirmats i un pendent)
        val bizums = mutableListOf<Bizum>()
        consums.filter { it.ts > ara - 4 * 86_400_000L && it.convidat == null }.take(3).forEachIndexed { i, c ->
            val b = Bizum(nouId(), c.sociId, socis.first { it.id == c.sociId }.nomComplet, c.nomProducte, c.import, c.ts,
                if (i < 2) EstatBizum.REBUT else EstatBizum.PENDENT, if (i < 2) "s1" else null, if (i < 2) c.ts + 3_600_000L else null)
            bizums += b
            consums[consums.indexOf(c)] = c.copy(pagament = Pagament.BIZUM, bizumId = b.id)
        }

        // Comptador de consum des de la darrera comanda (l'1 d'aquest mes) i avisos dels productes amb poc estoc
        val iniciMes = avui.withDayOfMonth(1).atStartOfDay(ZONA).toInstant().toEpochMilli()
        val avisos = mutableListOf<AvisEstoc>()
        for (i in productes.indices) {
            val p = productes[i]
            val n = consums.filter { it.producteId == p.id && it.ts >= iniciMes }.sumOf { it.quantitat }
            var q = p.copy(comptador = n, darreraComandaTs = iniciMes)
            if (q.calReposar) {
                avisos += AvisEstoc(nouId(), q.id, TipusAvis.ESTOC_MINIM, "${q.nom}: en queden ${q.estoc} (el mínim és ${q.estocMinim}).", ara - 7_200_000L)
                q = q.copy(avisMinimFet = true)
            }
            if (q.avisConsum in 1..q.comptador) {
                avisos += AvisEstoc(nouId(), q.id, TipusAvis.CONSUM, "${q.nom}: se n'han consumit ${q.comptador} des de la darrera comanda.", ara - 3_600_000L)
                q = q.copy(avisConsumFet = true)
            }
            productes[i] = q
        }

        val mes = YearMonth.from(avui)
        val mesPassat = mes.minusMonths(1)
        val apats = listOf(
            Apat(
                nouId(), "Calçotada de la colla", mesPassat.atDay(20).toString(), "s2", ModeRepartiment.IGUALS,
                listOf("s1", "s2", "s4", "s5", "s7", "s9", "s12", "s15").mapIndexed { i, id -> Assistent(id, if (i == 3) 1 else 0) },
                listOf(
                    DespesaApat(nouId(), "s2", "Calçots i salsa", 6450),
                    DespesaApat(nouId(), "s5", "Carn a la brasa", 9230),
                    DespesaApat(nouId(), "s1", "Pa i postres", 2120),
                ),
                EstatApat.TANCAT,
                tipus = TipusActivitat.SOCIS, franja = Franja.DINAR, espai = TOT_EL_LOCAL, cadiraSoci = 200, cadiraNoSoci = 300,
                cobramentEfectiu = true, neteja = Neteja.NORMAL, preuNeteja = 2000, resumEnviat = true,
            ),
            Apat(
                nouId(), "Sopar de caragols", mes.atDay(maxOf(1, minOf(avui.dayOfMonth - 1, 17))).toString(), "s3", ModeRepartiment.PROPORCIONAL,
                listOf(Assistent("s3"), Assistent("s6"), Assistent("s8", 2, 3.0), Assistent("s10", 0, 0.5), Assistent("s13")),
                listOf(DespesaApat(nouId(), "s3", "Caragols (3 kg)", 5400), DespesaApat(nouId(), "s13", "Allioli, pa i amanida", 1840)),
                tipus = TipusActivitat.PRIVAT, franja = Franja.SOPAR, espai = "Cuina i menjador gran", cadiraSoci = 200, cadiraNoSoci = 300,
            ),
            Apat(
                "arrossada", "Arrossada popular", avui.plusDays(9).toString(), "s7", ModeRepartiment.IGUALS,
                listOf(Assistent("s7"), Assistent("s1", 1), Assistent("s11"), Assistent("s16", 2), Assistent("s20")),
                listOf(DespesaApat(nouId(), "s7", "Arròs, marisc i verdura", 9800)),
                tipus = TipusActivitat.OBERT, franja = Franja.TOT_EL_DIA, espai = TOT_EL_LOCAL, cadiraSoci = 300, cadiraNoSoci = 500,
                externs = listOf(Extern("e1", "Família Roca (veïns)", responsableId = "s7"), Extern("e2", "Sílvia Mas", responsableId = "s16")),
                neteja = Neteja.AMPLIADA, preuNeteja = 4000,
            ),
        )

        val cfg = Config(
            telefonBizum = "600 123 456",
            correuReserves = "reserves@exemple.cat",
            correuAvisos = "magatzem@exemple.cat",
            correuProveidor = "comandes@distribucions-exemple.cat",
            compteTransferencia = "ES00 1234 5678 9012 3456 7890",
            resumEstocPer = "magatzem@exemple.cat",
            resumTresoreriaPer = "tresoreria@exemple.cat",
            resumActivitatsPer = "tresoreria@exemple.cat",
        )
        val reserves = listOf(
            Reserva(nouId(), "s4", avui.plusDays(2).toString(), Franja.SOPAR, cfg.espais[0], 12, "Aniversari", ara - 3 * 86_400_000L),
            Reserva(nouId(), "s11", avui.plusDays(3).toString(), Franja.DINAR, cfg.espais[1], 6, "", ara - 2 * 86_400_000L),
            Reserva(nouId(), "s7", avui.plusDays(9).toString(), Franja.TOT_EL_DIA, TOT_EL_LOCAL, 13, "Activitat oberta: Arrossada popular", ara - 86_400_000L, exclusiu = true, apatId = "arrossada"),
        )
        val tancaments = listOf(Tancament(mesPassat.minusMonths(1).toString(), EstatMes.TANCAT, ara - 50 * 86_400_000L, ara - 44 * 86_400_000L))

        // Pagaments per transferència: quasi tothom ha pagat el mes passat; uns quants no (deute antic) i un està pendent
        val transferencies = socis.filter { it.id !in setOf("s5", "s9", "s14") }.mapNotNull { soci ->
            val deute = liniesSoci(soci, consums, apats).filter { mesDe(it.ts) < mes.toString() }.sumOf { it.import }
            if (deute <= 0) return@mapNotNull null
            val pendent = soci.id == "s12"
            Transferencia(
                nouId(), soci.id, deute, mes.atDay(minOf(5, avui.dayOfMonth)).toString(), "GastroBacus ${soci.nomComplet}",
                ara - (if (pendent) 1 else 10) * 3_600_000L,
                if (pendent) EstatTransferencia.PENDENT else EstatTransferencia.CONFIRMADA, if (pendent) null else "s1",
            )
        }

        return Copia(
            ts = ara, config = cfg, socis = socis, productes = productes, consums = consums,
            reposicions = emptyList(), apats = apats, reserves = reserves, tancaments = tancaments, correus = emptyList(),
            convidats = listOf(ConvidatDia("c1", "Marc", "s5", ara - 1_800_000L, caducaConvidat(ara, cfg.horaFiDia))),
            bizums = bizums, avisos = avisos, transferencies = transferencies,
            peticions = listOf(
                Peticio(nouId(), "s9", TipusPeticio.FALTA, "p15", "Fanta de llimona", ara - 5_400_000L),
                Peticio(nouId(), "s12", TipusPeticio.PROPOSTA, null, "Vermut negre de Reus", ara - 2 * 86_400_000L),
            ),
        )
    }
}
