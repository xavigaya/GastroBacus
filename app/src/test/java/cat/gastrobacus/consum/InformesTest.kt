package cat.gastrobacus.consum

import cat.gastrobacus.consum.dades.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InformesTest {
    private val soci = Soci("a", "Anna", "Bosch", "anna@x.cat", Rol.SOCI, "", "")

    @Test fun repartimentAPartsIgualsAmbConvidats() {
        val apat = Apat(
            "1", "Calçotada", "2026-09-20", "a", ModeRepartiment.IGUALS,
            listOf(Assistent("a"), Assistent("b", convidats = 1), Assistent("c")),
            listOf(DespesaApat("d1", "a", "Calçots", 4000)),
        )
        val r = repartiment(apat)
        assertEquals(4000, r.total)
        assertEquals(1000.0, r.perUnitat, 0.001)
        assertEquals(listOf(1000L, 2000L, 1000L), r.quotes.map { it.quota })
        assertEquals(-3000, r.quotes.first { it.sociId == "a" }.saldo) // ha pagat 40 i li toquen 10
        assertEquals(0, r.quotes.sumOf { it.saldo })
    }

    @Test fun repartimentProporcionalCuadraElsCentims() {
        val apat = Apat(
            "1", "Caragols", "2026-10-17", "a", ModeRepartiment.PROPORCIONAL,
            listOf(Assistent("a"), Assistent("b"), Assistent("c", parts = 3.0), Assistent("d", parts = 0.5), Assistent("e")),
            listOf(DespesaApat("d1", "a", "Caragols", 5400), DespesaApat("d2", "e", "Pa", 1840)),
        )
        val r = repartiment(apat)
        assertEquals(7240, r.total)
        assertEquals(7240, r.quotes.sumOf { it.quota })
        assertEquals(0, r.quotes.sumOf { it.saldo })
        assertEquals(3342, r.quotes.first { it.sociId == "c" }.quota)
    }

    @Test fun pagadorQueNoVeAlSoparRepIntegreElQueHaPagat() {
        val apat = Apat("1", "Sopar", "2026-10-01", "a", ModeRepartiment.IGUALS, listOf(Assistent("a"), Assistent("b")), listOf(DespesaApat("d", "z", "Compra", 3000)))
        val z = repartiment(apat).quotes.first { it.sociId == "z" }
        assertEquals(0, z.quota)
        assertEquals(-3000, z.saldo)
    }

    @Test fun informeInclouConsumsApatsIAbonaments() {
        val ts = tsDe("2026-10-05", 20)
        val consums = listOf(
            Consum("c1", "a", "p", "Cervesa", 2, 130, null, ts),
            Consum("c2", "a", "p", "Cervesa", 1, 160, "Marc", ts),
            Consum("c3", "a", "p", "Cervesa", 1, 130, null, ts, anulatTs = ts + 1),
            Consum("c4", "a", "p", "Cervesa", 1, 130, null, tsDe("2026-09-30", 20)),
        )
        val apats = listOf(
            Apat("1", "Sopar", "2026-10-10", "a", ModeRepartiment.IGUALS, listOf(Assistent("a"), Assistent("b")), listOf(DespesaApat("d", "a", "Compra", 3000)), EstatApat.TANCAT),
            Apat("2", "Obert", "2026-10-11", "a", ModeRepartiment.IGUALS, listOf(Assistent("a")), listOf(DespesaApat("d", "b", "Compra", 1000))),
        )
        val inf = informeSoci(soci, "2026-10", consums, apats)
        assertEquals(420, inf.totalConsum)
        assertEquals(1500, inf.totalApats)
        assertEquals(-3000, inf.totalAbonaments)
        assertEquals(-1080, inf.total)
    }

    @Test fun formatDEuros() {
        assertEquals("1,30 €", euros(130))
        assertEquals("1.234,05 €", euros(123405))
        assertEquals("-0,50 €", euros(-50))
        assertEquals(130L, llegirEuros("1,3"))
        assertEquals(1250L, llegirEuros("12.50 €"))
        assertNull(llegirEuros("abc"))
    }

    @Test fun nomsDeMes() {
        assertEquals("d'octubre de 2026", deMes("2026-10"))
        assertEquals("de març de 2026", deMes("2026-03"))
        assertEquals("Octubre 2026", nomMes("2026-10"))
        assertEquals("Dissabte 17 d'octubre", dataLlarga("2026-10-17"))
        assertEquals("Diumenge 1 de març", dataLlarga("2026-03-01"))
    }

    @Test fun dadesDeProvaConsistents() {
        val c = DadesProva.crea()
        assertEquals(24, c.socis.size)
        assertEquals(30, c.productes.size)
        assert(c.consums.size > 300)
        assert(c.socis.all { it.pinHash == Pin.hash(DadesProva.PIN, it.pinSal) })
    }
}
