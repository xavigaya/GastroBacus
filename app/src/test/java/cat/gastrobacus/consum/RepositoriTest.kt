package cat.gastrobacus.consum

import android.app.Application
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import cat.gastrobacus.consum.dades.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Proves amb base de dades real: migració de la versió 1, estoc i comandes, Bizum, reserves i baixes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RepositoriTest {
    @get:Rule val migracions = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), BaseDades::class.java)

    private fun repo(): Pair<Repositori, BaseDades> {
        val bd = BaseDades.enMemoria(ApplicationProvider.getApplicationContext())
        return Repositori(bd, CoroutineScope(Dispatchers.Unconfined)) to bd
    }

    @Test fun laMigracioConservaLesDadesDeLaVersio1() {
        migracions.createDatabase("prova-migracio", 1).apply {
            execSQL("INSERT INTO socis (id, nom, cognoms, email, rol, pinHash, pinSal, refBanc, actiu) VALUES ('s1','Anna','Bosch','a@x.cat','SOCI','h','s','GB-1',1)")
            execSQL("INSERT INTO productes (id, nom, categoria, preu, preuConvidat, estoc, estocMinim, controlaEstoc, actiu) VALUES ('p1','Moritz','CERVESA',130,160,10,4,1,1)")
            execSQL("INSERT INTO consums (id, sociId, producteId, nomProducte, quantitat, preuUnitari, convidat, ts, origen, anulatTs, anulatPer) VALUES ('c1','s1','p1','Moritz',2,130,NULL,1700000000000,'TAULETA',NULL,NULL)")
            execSQL("INSERT INTO apats (id, titol, data, organitzadorId, mode, assistents, despeses, estat) VALUES ('a1','Sopar','2026-10-01','s1','IGUALS','[]','[]','OBERT')")
            close()
        }
        val bd = migracions.runMigrationsAndValidate("prova-migracio", 2, true)
        bd.query("SELECT pagament, periodes FROM consums, socis").use { c -> c.moveToFirst(); assertEquals("FACTURA", c.getString(0)); assertEquals("[]", c.getString(1)) }
        bd.query("SELECT tipus, espai, externs FROM apats").use { c -> c.moveToFirst(); assertEquals("PRIVAT", c.getString(0)); assertEquals(TOT_EL_LOCAL, c.getString(1)); assertEquals("[]", c.getString(2)) }
        bd.query("SELECT comptador, favorit FROM productes").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)); assertEquals(0, c.getInt(1)) }
        bd.close()
    }

    @Test fun comandaRecompteIAvisos() = runBlocking {
        val (r, bd) = repo()
        r.desaConfig(Config(correuAvisos = "magatzem@x.cat", modeProves = false))
        bd.dao().desaProducte(Producte("p1", "Moritz", Categoria.CERVESA, 130, estoc = 10, estocMinim = 8, avisConsum = 3))
        r.apuntar("s1", listOf(ItemApunt("p1", 3)))
        var p = bd.dao().producte("p1")!!
        assertEquals(7, p.estoc); assertEquals(3, p.comptador)
        val avisos = bd.dao().totsAvisos()
        assertEquals(setOf(TipusAvis.ESTOC_MINIM, TipusAvis.CONSUM), avisos.map { it.tipus }.toSet())
        assertEquals(2, bd.dao().totsCorreus().count { it.per == "magatzem@x.cat" })
        // No es repeteix l'avís
        r.apuntar("s1", listOf(ItemApunt("p1", 1)))
        assertEquals(2, bd.dao().totsAvisos().size)
        // Arriba una comanda: entrada de 24 i el comptador torna a zero
        r.registraComanda("p1", 24, TipusReposicio.ENTRADA, "s1")
        p = bd.dao().producte("p1")!!
        assertEquals(30, p.estoc); assertEquals(0, p.comptador); assertTrue(!p.avisMinimFet && !p.avisConsumFet)
        assertEquals(4, bd.dao().totesReposicions().single().consumitPeriode)
        // Recompte: es fixa l'estoc real
        r.registraComanda("p1", 27, TipusReposicio.RECOMPTE, "s1")
        assertEquals(27, bd.dao().producte("p1")!!.estoc)
        bd.close()
    }

    @Test fun bizumNoArribatPassaALaFactura() = runBlocking {
        val (r, bd) = repo()
        bd.dao().desaProducte(Producte("p1", "Moritz", Categoria.CERVESA, 130, controlaEstoc = false))
        val b = r.pagaAmbBizum("s1", "Anna", listOf(ItemApunt("p1", 2)), "GastroBacus Anna")
        assertEquals(260L, b.import)
        assertTrue(bd.dao().totsConsums().all { it.pagament == Pagament.BIZUM && it.bizumId == b.id })
        r.confirmaBizum(b.id, "s2", rebut = false)
        assertTrue(bd.dao().totsConsums().all { it.pagament == Pagament.FACTURA })
        assertEquals(EstatBizum.NO_ARRIBAT, bd.dao().bizum(b.id)!!.estat)
        bd.close()
    }

    @Test fun totElLocalBloquejaLaFranjaIAvisaElResponsable() = runBlocking {
        val (r, bd) = repo()
        r.desaConfig(Config(correuReserves = "reserves@x.cat", modeProves = false))
        val dia = java.time.LocalDate.now(ZONA).plusDays(5).toString()
        r.creaReserva("s1", dia, Franja.SOPAR, "Taula del racó", 6, "")
        val apat = Apat("a1", "Sopar gran", dia, "s2", espai = TOT_EL_LOCAL, franja = Franja.SOPAR, assistents = listOf(Assistent("s2")))
        val error = runCatching { r.desaApat(apat, "s2") }.exceptionOrNull()
        assertTrue(error is ErrorApp)
        // A dinar sí que es pot
        r.desaApat(apat.copy(franja = Franja.DINAR), "s2")
        val res = bd.dao().reservaDApat("a1")!!
        assertTrue(res.exclusiu)
        assertEquals(2, bd.dao().totsCorreus().count { it.per == "reserves@x.cat" })
        // I ningú més pot reservar aquell dinar
        assertTrue(runCatching { r.creaReserva("s3", dia, Franja.DINAR, "Taula del racó", 4, "") }.exceptionOrNull() is ErrorApp)
        bd.close()
    }

    @Test fun baixaILiquidacio() = runBlocking {
        val (r, bd) = repo()
        r.desaConfig(Config(modeProves = false, correuTresoreria = "tresoreria@x.cat"))
        r.desaSoci(Soci("s1", "Anna", "Bosch", "anna@x.cat", Rol.SOCI, "", ""), "1234")
        r.desaSoci(Soci("s2", "Jordi", "Puig", "jordi@x.cat", Rol.JUNTA, "", ""), "1234")
        bd.dao().desaProducte(Producte("p1", "Moritz", Categoria.CERVESA, 130, controlaEstoc = false))
        r.apuntar("s1", listOf(ItemApunt("p1", 3)))
        assertEquals(390L, r.pendentDe("s1").second)
        r.donaDeBaixa("s1", "2026-11-30", informeFinal = true)
        val s = bd.dao().soci("s1")!!
        assertTrue(!s.actiu); assertEquals("2026-11-30", s.dataBaixa)
        assertEquals(setOf("anna@x.cat", "tresoreria@x.cat"), bd.dao().totsCorreus().map { it.per }.toSet())
        r.donaDAlta("s1", "2027-02-01")
        assertEquals(2, bd.dao().soci("s1")!!.periodes.size)
        // L'únic membre de la junta no es pot donar de baixa
        assertTrue(runCatching { r.donaDeBaixa("s2", "2026-11-30", false) }.exceptionOrNull() is ErrorApp)
        bd.close()
    }

    @Test fun importarActualitzaElsQueJaHiSon() = runBlocking {
        val (r, bd) = repo()
        bd.dao().desaProducte(Producte("p1", "Cervesa Moritz 33 cl", Categoria.CERVESA, 120, estoc = 5))
        val res = r.importaProductes(Csv.llegeixProductes(Csv.PLANTILLA_PRODUCTES).files)
        assertEquals(2, res.nous); assertEquals(1, res.actualitzats)
        val p = bd.dao().producte("p1")!!
        assertEquals(130L, p.preu); assertEquals(96, p.estoc); assertTrue(p.favorit)
        val rs = r.importaSocis(Csv.llegeixSocis(Csv.PLANTILLA_SOCIS).files)
        assertEquals(2, rs.nous)
        assertTrue(r.verificarPin(bd.dao().totsSocis().first { it.nom == "Jordi" }.id, "5678"))
        bd.close()
    }
}
