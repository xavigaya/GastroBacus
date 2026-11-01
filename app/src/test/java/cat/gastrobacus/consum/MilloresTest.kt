package cat.gastrobacus.consum

import cat.gastrobacus.consum.dades.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Proves de la lògica de les millores: cadires, persones de fora, Bizum, favorits, convidats i CSV. */
class MilloresTest {
    private val anna = Soci("a", "Anna", "Bosch", "anna@x.cat", Rol.SOCI, "", "")

    @Test fun cadiresSociIConvidatsAPreusDiferents() {
        val apat = Apat(
            "1", "Sopar", "2026-10-10", "a", ModeRepartiment.IGUALS,
            listOf(Assistent("a", convidats = 2), Assistent("b")),
            listOf(DespesaApat("d", "b", "Compra", 4000)),
            tipus = TipusActivitat.SOCIS, cadiraSoci = 200, cadiraNoSoci = 300,
        )
        val r = repartiment(apat)
        val a = r.quotes.first { it.sociId == "a" }
        assertEquals(3000, a.menjar) // 3 de 4 persones
        assertEquals(200 + 2 * 300L, a.cadires)
        assertEquals(3, a.numCadires)
        val b = r.quotes.first { it.sociId == "b" }
        assertEquals(1000 + 200 - 4000L, b.saldo)
        assertEquals(1000L, r.totalCadires)
    }

    @Test fun personesDeForaPaguenLaSevaPartIPreuNoSoci() {
        val apat = Apat(
            "1", "Arrossada", "2026-11-10", "a", ModeRepartiment.IGUALS,
            listOf(Assistent("a")), listOf(DespesaApat("d", "a", "Arròs", 3000)),
            tipus = TipusActivitat.OBERT, externs = listOf(Extern("e1", "Família Roca"), Extern("e2", "Sílvia")),
            cadiraSoci = 300, cadiraNoSoci = 500,
        )
        val r = repartiment(apat)
        val e = r.quotes.first { it.sociId == "ext:e1" }
        assertTrue(e.esExtern)
        assertEquals(1000 + 500L, e.quota)
        assertEquals(3000, r.quotes.sumOf { it.menjar })
        assertEquals(3000L, r.totalExterns)
        // A l'informe de l'Anna només hi surt la seva part
        val inf = informeSoci(anna, "2026-11", emptyList(), listOf(apat.copy(estat = EstatApat.TANCAT)))
        assertEquals(1000 + 300 - 3000L, inf.total)
    }

    @Test fun elQueEsPagaPerBizumNoVaALaFactura() {
        val ts = tsDe("2026-10-05", 20)
        val consums = listOf(
            Consum("c1", "a", "p", "Cervesa", 2, 130, null, ts),
            Consum("c2", "a", "p", "Cervesa", 1, 130, null, ts, pagament = Pagament.BIZUM, bizumId = "b1"),
        )
        assertEquals(260, informeSoci(anna, "2026-10", consums, emptyList()).total)
    }

    @Test fun favoritsFixosIHabituals() {
        val ara = tsDe("2026-10-20", 12)
        val ps = listOf(
            Producte("p1", "Cafè", Categoria.CAFE, 60, favorit = true),
            Producte("p2", "Moritz", Categoria.CERVESA, 130),
            Producte("p3", "Olives", Categoria.PICAR, 120),
            Producte("p4", "Vi", Categoria.VI, 650),
        )
        val consums = listOf(
            Consum("1", "a", "p2", "Moritz", 5, 130, null, ara - 86_400_000L),
            Consum("2", "a", "p3", "Olives", 2, 120, null, ara - 86_400_000L),
            Consum("3", "a", "p4", "Vi", 9, 650, null, ara - 200L * 86_400_000L), // fa massa temps
            Consum("4", "b", "p4", "Vi", 9, 650, null, ara - 86_400_000L), // d'un altre soci
        )
        assertEquals(listOf("p1", "p2", "p3"), favorits(ps, consums, "a", 8, ara).map { it.id })
        assertEquals(listOf("p1", "p2"), favorits(ps, consums, "a", 1, ara).map { it.id })
    }

    @Test fun elsConvidatsCaduquenALesSetDelMati() {
        val vespre = java.time.LocalDateTime.of(2026, 11, 7, 22, 30).atZone(ZONA).toInstant().toEpochMilli()
        assertEquals("2026-11-08T07:00", Instant.ofEpochMilli(caducaConvidat(vespre, 7)).atZone(ZONA).toLocalDateTime().toString())
        val matinada = java.time.LocalDateTime.of(2026, 11, 8, 2, 0).atZone(ZONA).toInstant().toEpochMilli()
        assertEquals("2026-11-08T07:00", Instant.ofEpochMilli(caducaConvidat(matinada, 7)).atZone(ZONA).toLocalDateTime().toString())
    }

    @Test fun csvDeProductes() {
        val r = Csv.llegeixProductes(
            "﻿Nom;Categoria;Preu;Preu convidat;Estoc;Estoc mínim;Avís consum;Favorit\r\n" +
                "Cervesa Moritz 33 cl;cervesa;1,30;1,60;96;24;48;Sí\r\n" +
                "\"Vi negre; reserva\";Vi;6.50;;14;6;;no\r\n" +
                "Sense preu;Vi;;;;;;\r\n" +
                "Raret;Embotits;1;;;;;\r\n",
        )
        assertEquals(2, r.files.size)
        assertEquals(FilaProducte("Cervesa Moritz 33 cl", Categoria.CERVESA, 130, 160, 96, 24, 48, true), r.files[0])
        assertEquals("Vi negre; reserva", r.files[1].nom)
        assertEquals(650L, r.files[1].preu)
        assertEquals(2, r.errors.size)
    }

    @Test fun csvDeSocisAmbComesIDates() {
        val r = Csv.llegeixSocis(
            "nom,cognoms,correu,ref_banc,pin,junta,data_alta\n" +
                "Anna,Bosch Serra,anna@x.cat,GB-0004,1234,no,01/03/2019\n" +
                "Jordi,Puig,jordi@x.cat,,5678,si,2015-1-10\n" +
                "Pere,Roca,pere@x.cat,,12,no,\n",
        )
        assertEquals(2, r.files.size)
        assertEquals("2019-03-01", r.files[0].dataAlta)
        assertTrue(r.files[1].junta)
        assertEquals("2015-01-10", r.files[1].dataAlta)
        assertEquals(1, r.errors.size)
    }

    @Test fun plantillesEsPodenLlegir() {
        assertEquals(3, Csv.llegeixProductes(Csv.PLANTILLA_PRODUCTES).files.size)
        assertEquals(2, Csv.llegeixSocis(Csv.PLANTILLA_SOCIS).files.size)
    }
}
