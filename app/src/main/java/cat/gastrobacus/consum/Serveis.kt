package cat.gastrobacus.consum

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cat.gastrobacus.consum.dades.BaseDades
import cat.gastrobacus.consum.dades.Copia
import cat.gastrobacus.consum.dades.Correu
import cat.gastrobacus.consum.dades.EstatCorreu
import cat.gastrobacus.consum.dades.JSON
import cat.gastrobacus.consum.dades.Repositori
import cat.gastrobacus.consum.dades.ZONA
import cat.gastrobacus.consum.dades.informeSoci
import cat.gastrobacus.consum.exportar.Fitxer
import cat.gastrobacus.consum.exportar.pdfSoci
import cat.gastrobacus.consum.google.ErrorGoogle
import cat.gastrobacus.consum.google.Google
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.encodeToString
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/** Punt únic d'accés a les dades i serveis de l'app. */
class Serveis private constructor(context: Context) {
    val context: Context = context.applicationContext
    val ambit = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repo = Repositori(BaseDades.de(this.context), ambit)

    companion object {
        @Volatile private var instancia: Serveis? = null
        fun de(context: Context) = instancia ?: synchronized(this) { instancia ?: Serveis(context).also { instancia = it } }
        const val COPIES_A_GUARDAR = 90
    }

    // ---------- Compte de Google ----------

    suspend fun iniciaSessioGoogle(): String {
        val token = Google.token(context, interactiu = true)
        val correu = Google.correuDelCompte(token)
        repo.actualitzaConfig { it.copy(compteGoogle = correu) }
        programaCopiaDiaria(context, reprograma = true)
        return correu
    }

    suspend fun tancaSessioGoogle() = repo.actualitzaConfig { it.copy(compteGoogle = null) }

    // ---------- Correus ----------

    /** Envia els correus pendents (amb el PDF del soci adjunt). Retorna quants s'han enviat. */
    suspend fun enviaPendents(interactiu: Boolean = true): Int {
        val cfg = repo.config()
        val compte = cfg.compteGoogle ?: throw ErrorGoogle("Primer cal iniciar la sessió amb Google a Gestió > Configuració.", true)
        val pendents = repo.correusPendents()
        if (pendents.isEmpty()) return 0
        val token = Google.token(context, interactiu)
        val estat = repo.estat.value
        var enviats = 0
        for (c in pendents) {
            try {
                envia(token, compte, cfg.nomSocietat, c, estat)
                repo.marcaCorreu(c.id, EstatCorreu.ENVIAT)
                enviats++
            } catch (e: ErrorGoogle) {
                repo.marcaCorreu(c.id, EstatCorreu.ERROR, e.message)
                if (e.calIniciarSessio) throw e
            }
        }
        return enviats
    }

    private suspend fun envia(token: String, compte: String, nom: String, c: Correu, estat: cat.gastrobacus.consum.dades.Estat) {
        val adjunt = if (c.adjuntSociId != null && c.mes != null) {
            estat.soci(c.adjuntSociId)?.let { s -> pdfSoci(context, informeSoci(s, c.mes, estat.consums, estat.apats), c.mes) }
        } else null
        Google.enviaCorreu(token, compte, nom, c.per, c.assumpte, c.cos, adjunt?.let { Google.Adjunt(it.nom, it.tipus, it.dades) })
    }

    /** Envia un fitxer (Excel, PDF, remesa) per correu des del Gmail de la tauleta. */
    suspend fun enviaFitxer(per: String, assumpte: String, cos: String, f: Fitxer) {
        val cfg = repo.config()
        val compte = cfg.compteGoogle ?: throw ErrorGoogle("Primer cal iniciar la sessió amb Google a Gestió > Configuració.", true)
        val desti = if (cfg.modeProves) cfg.correuProves else per
        Google.enviaCorreu(Google.token(context, true), compte, cfg.nomSocietat, desti, assumpte, cos, Google.Adjunt(f.nom, f.tipus, f.dades))
    }

    // ---------- Còpies de seguretat ----------

    suspend fun ferCopia(interactiu: Boolean): String {
        try {
            val token = Google.token(context, interactiu)
            val nom = "gastrobacus-" + DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm").format(LocalDateTime.now(ZONA)) + ".json.gz"
            Google.pujaCopia(token, nom, JSON.encodeToString(repo.copia()))
            // Conserva les darreres còpies i esborra les més antigues
            Google.llistaCopies(token).drop(COPIES_A_GUARDAR).forEach { runCatching { Google.eliminaCopia(token, it.id) } }
            repo.actualitzaConfig { it.copy(darreraCopiaTs = System.currentTimeMillis(), darreraCopiaError = null) }
            return nom
        } catch (e: Exception) {
            repo.actualitzaConfig { it.copy(darreraCopiaError = e.message ?: "Error desconegut") }
            throw e
        }
    }

    suspend fun llistaCopies(): List<Google.FitxerCopia> = Google.llistaCopies(Google.token(context, true))

    suspend fun restauraCopia(id: String) {
        val text = Google.baixaCopia(Google.token(context, true), id)
        val c = JSON.decodeFromString<Copia>(text)
        repo.restaura(c, mantenirCompteGoogle = true)
    }
}

class TreballCopia(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val s = Serveis.de(applicationContext)
        if (s.repo.config().compteGoogle == null) return Result.success()
        return try {
            s.ferCopia(interactiu = false)
            runCatching { s.enviaPendents(interactiu = false) }
            Result.success()
        } catch (e: ErrorGoogle) {
            if (e.calIniciarSessio) Result.failure() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

/** Programa la còpia cada dia a les 4 de la matinada (amb connexió a internet). */
fun programaCopiaDiaria(context: Context, reprograma: Boolean = false) {
    val ara = LocalDateTime.now(ZONA)
    var properes4 = ara.toLocalDate().atTime(4, 0)
    if (!properes4.isAfter(ara)) properes4 = properes4.plusDays(1)
    val espera = Duration.between(ara, properes4).toMinutes()
    val peticio = PeriodicWorkRequestBuilder<TreballCopia>(24, TimeUnit.HOURS)
        .setInitialDelay(espera, TimeUnit.MINUTES)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork("copia-diaria", if (reprograma) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP, peticio)
}
