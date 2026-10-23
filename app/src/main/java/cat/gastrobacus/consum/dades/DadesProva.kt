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
            )
        }
        val productes = PRODUCTES.mapIndexed { i, p ->
            Producte("p${i + 1}", p.nom, p.cat, p.preu, p.conv, p.estoc, p.minim, controlaEstoc = p.minim > 0)
        }

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
            ),
            Apat(
                nouId(), "Sopar de caragols", mes.atDay(maxOf(1, minOf(avui.dayOfMonth - 1, 17))).toString(), "s3", ModeRepartiment.PROPORCIONAL,
                listOf(Assistent("s3"), Assistent("s6"), Assistent("s8", 2, 3.0), Assistent("s10", 0, 0.5), Assistent("s13")),
                listOf(DespesaApat(nouId(), "s3", "Caragols (3 kg)", 5400), DespesaApat(nouId(), "s13", "Allioli, pa i amanida", 1840)),
            ),
        )

        val cfg = Config()
        val reserves = listOf(
            Reserva(nouId(), "s4", avui.plusDays(2).toString(), Franja.SOPAR, cfg.espais[0], 12, "Aniversari", ara - 3 * 86_400_000L),
            Reserva(nouId(), "s11", avui.plusDays(3).toString(), Franja.DINAR, cfg.espais[1], 6, "", ara - 2 * 86_400_000L),
            Reserva(nouId(), "s7", avui.plusDays(9).toString(), Franja.TOT_EL_DIA, cfg.espais[0], 20, "Arrossada popular", ara - 86_400_000L),
        )
        val tancaments = listOf(Tancament(mesPassat.minusMonths(1).toString(), EstatMes.TANCAT, ara - 50 * 86_400_000L, ara - 44 * 86_400_000L))

        return Copia(
            ts = ara, config = cfg, socis = socis, productes = productes, consums = consums,
            reposicions = emptyList(), apats = apats, reserves = reserves, tancaments = tancaments, correus = emptyList(),
        )
    }
}
