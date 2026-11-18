package cat.gastrobacus.consum

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import cat.gastrobacus.consum.dades.*
import cat.gastrobacus.consum.exportar.resumActivitat
import cat.gastrobacus.consum.exportar.resumEstoc
import cat.gastrobacus.consum.exportar.resumTresoreria
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Versió 0.3: pagaments per transferència, sopar en efectiu, neteja, gent de fora i resums. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PagamentsTest {
    @get:Rule val migracions = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), BaseDades::class.java)
    private val anna = Soci("a", "Anna", "Bosch", "anna@x.cat", Rol.SOCI, "", "")
    private val dia = 86_400_000L

    @Test fun elsPagamentsCobreixenPrimerElDeuteMesAntic() {
        val ara = tsDe("2026-11-18", 12)
        val consums = listOf(
            Consum("1", "a", "p", "Cervesa", 1, 1000, null, ara - 60 * dia),
            Consum("2", "a", "p", "Cervesa", 1, 500, null, ara - 50 * dia),
            Consum("3", "a", "p", "Cervesa", 1, 300, null, ara - 5 * dia),
        )
        var c = estatCompte(anna, consums, emptyList(), emptyList())
        assertEquals(1800, c.perPagar); assertEquals(60, c.diesDeute(ara))
        // Paga 1.000: el deute més antic ja és el de fa 50 dies
        val t1 = Transferencia("t1", "a", 1000, "2026-10-01", "x", ara, EstatTransferencia.CONFIRMADA)
        c = estatCompte(anna, consums, emptyList(), listOf(t1))
        assertEquals(800, c.perPagar); assertEquals(50, c.diesDeute(ara))
        // Una transferència pendent també compta per no molestar el soci
        val t2 = Transferencia("t2", "a", 800, "2026-11-17", "x", ara)
        c = estatCompte(anna, consums, emptyList(), listOf(t1, t2))
        assertEquals(0, c.perPagar); assertEquals(800, c.pendentConfirmar); assertNull(c.deuteDesDe)
        // Si no arriba, el deute torna
        c = estatCompte(anna, consums, emptyList(), listOf(t1, t2.copy(estat = EstatTransferencia.REBUTJADA)))
        assertEquals(800, c.perPagar)
        // Amb rebut bancari, els mesos tancats ja estan cobrats (els dos primers consums són de setembre)
        assertEquals("2026-09", mesDe(ara - 60 * dia)); assertEquals("2026-09", mesDe(ara - 50 * dia))
        assertEquals(300, estatCompte(anna, consums, emptyList(), emptyList(), setOf("2026-09")).perPagar)
    }

    @Test fun soparDeSocisEnEfectiuINetejaAlCompte() {
        val apat = Apat(
            "1", "Sopar de socis", "2026-11-10", "c", ModeRepartiment.IGUALS,
            listOf(Assistent("a"), Assistent("c")), listOf(DespesaApat("d", "c", "Tiquet carnisseria", 4000)), EstatApat.TANCAT,
            tipus = TipusActivitat.SOCIS, cadiraSoci = 200, cadiraNoSoci = 300, cobramentEfectiu = true, neteja = Neteja.NORMAL, preuNeteja = 2000,
        )
        val r = repartiment(apat)
        assertEquals(2000 + 200L, r.quotes.first { it.sociId == "a" }.efectiu) // l'Anna paga en mà
        assertEquals(2000 + 200 - 4000L, r.quotes.first { it.sociId == "c" }.efectiu) // el cuiner en rep
        // Al compte de l'Anna no hi va res; al de l'organitzador, només la neteja
        assertEquals(0, informeSoci(anna, "2026-11", emptyList(), listOf(apat)).total)
        val cuiner = Soci("c", "Pere", "Cuiner", "", Rol.SOCI, "", "")
        val inf = informeSoci(cuiner, "2026-11", emptyList(), listOf(apat))
        assertEquals(listOf(TipusLinia.NETEJA), inf.linies.map { it.tipus })
        assertEquals(2000, inf.total)
    }

    @Test fun laGentDeForaVaAlCompteDelSociResponsable() {
        val apat = Apat(
            "1", "Arrossada", "2026-11-10", "c", ModeRepartiment.IGUALS,
            listOf(Assistent("c")), listOf(DespesaApat("d", "c", "Arròs", 2000)), EstatApat.TANCAT,
            tipus = TipusActivitat.OBERT, cadiraSoci = 300, cadiraNoSoci = 500, externs = listOf(Extern("e1", "Família Roca", responsableId = "a")),
        )
        val inf = informeSoci(anna, "2026-11", emptyList(), listOf(apat))
        assertEquals(1000 + 500L, inf.total)
        assertTrue(inf.linies.single().detall.contains("Família Roca"))
    }

    @Test fun registrarIConfirmarTransferencies() = runBlocking {
        val bd = BaseDades.enMemoria(ApplicationProvider.getApplicationContext())
        val r = Repositori(bd, CoroutineScope(Dispatchers.Unconfined))
        r.desaSoci(anna, "1234")
        bd.dao().desaProducte(Producte("p1", "Moritz", Categoria.CERVESA, 130, controlaEstoc = false))
        r.apuntar("a", listOf(ItemApunt("p1", 3)))
        val t = r.registraTransferencia("a", avui(), "GastroBacus Anna")
        assertEquals(390L, t.import)
        assertTrue(runCatching { r.registraTransferencia("a", avui(), "x") }.exceptionOrNull() is ErrorApp) // ja no deu res
        r.confirmaTransferencia(t.id, "j", rebuda = false)
        assertEquals(390L, r.compteDe("a").perPagar)
        r.reobreTransferencia(t.id); r.confirmaTransferencia(t.id, "j", rebuda = true)
        assertEquals(0L, r.compteDe("a").perPagar)
        bd.close()
    }

    @Test fun laGentDeForaSurtComAConvidatElDiaDeLActivitat() = runBlocking {
        val bd = BaseDades.enMemoria(ApplicationProvider.getApplicationContext())
        val r = Repositori(bd, CoroutineScope(Dispatchers.Unconfined))
        bd.dao().desaApat(Apat("x", "Arrossada", avui(), "c", tipus = TipusActivitat.OBERT, externs = listOf(Extern("e1", "Sílvia", responsableId = "a"))))
        r.activaConvidatsActivitats(); r.activaConvidatsActivitats()
        val cv = bd.dao().totsConvidats().single()
        assertEquals("Sílvia", cv.nom); assertEquals("a", cv.sociId)
        bd.close()
    }

    @Test fun elsResumsEsGeneren() {
        val c = DadesProva.crea()
        val estat = Estat(
            carregat = true, config = c.config, socis = c.socis, productes = c.productes, consums = c.consums, reposicions = c.reposicions,
            apats = c.apats, reserves = c.reserves, tancaments = c.tancaments, bizums = c.bizums, transferencies = c.transferencies, peticions = c.peticions,
        )
        val e = resumEstoc(estat)
        assertTrue(e.cos.contains("Cal reposar")); assertTrue(e.adjunt!!.dades.size > 1000)
        val t = resumTresoreria(estat, System.currentTimeMillis() - 15 * dia)
        assertTrue(t.cos.contains("Pendent de pagar entre tots els socis")); assertTrue(t.adjunt!!.dades.isNotEmpty())
        val a = resumActivitat(estat, c.apats.first { it.cobramentEfectiu })
        assertTrue(a.cos.contains("Pagament en efectiu")); assertTrue(a.cos.contains("Neteja: Neteja"))
        // Hi ha socis amb deute de fa més de 45 dies a les dades de prova
        assertTrue(c.socis.any { estat.deuteAntic(it) })
    }

    @Test fun migracioDeLaVersio2ALa3() {
        migracions.createDatabase("prova-v2", 2).apply {
            execSQL("INSERT INTO apats (id, titol, data, organitzadorId, mode, assistents, despeses, estat, tipus, franja, espai, externs, cadiraSoci, cadiraNoSoci) VALUES ('a1','Sopar','2026-10-01','s1','IGUALS','[{\"sociId\":\"s1\"}]','[]','OBERT','SOCIS','SOPAR','Tot el local','[{\"id\":\"e1\",\"nom\":\"Marc\"}]',200,300)")
            close()
        }
        val bd = migracions.runMigrationsAndValidate("prova-v2", 3, true)
        bd.query("SELECT neteja, preuNeteja, cobramentEfectiu, resumEnviat FROM apats").use { c ->
            c.moveToFirst(); assertEquals("SENSE", c.getString(0)); assertEquals(0, c.getInt(1)); assertEquals(0, c.getInt(2)); assertEquals(0, c.getInt(3))
        }
        bd.query("SELECT COUNT(*) FROM transferencies").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        bd.close()
        // Els JSON antics (sense responsable ni pagatEfectiu) es continuen llegint
        val conv = Conversors()
        assertEquals("", conv.aExterns("[{\"id\":\"e1\",\"nom\":\"Marc\"}]").single().responsableId)
        assertTrue(!conv.aAssistents("[{\"sociId\":\"s1\"}]").single().pagatEfectiu)
    }
}
