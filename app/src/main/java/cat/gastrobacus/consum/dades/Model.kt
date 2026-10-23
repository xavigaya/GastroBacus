package cat.gastrobacus.consum.dades

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/* Tots els imports són en cèntims d'euro (Long) per evitar errors d'arrodoniment. */

enum class Rol { SOCI, JUNTA }

@Serializable
@Entity(tableName = "socis")
data class Soci(
    @PrimaryKey val id: String,
    val nom: String,
    val cognoms: String,
    val email: String,
    val rol: Rol = Rol.SOCI,
    val pinHash: String,
    val pinSal: String,
    val refBanc: String = "",
    val actiu: Boolean = true,
) {
    val nomComplet get() = "$nom $cognoms".trim()
    val inicials get() = (nom.take(1) + cognoms.take(1)).uppercase()
    val esJunta get() = rol == Rol.JUNTA
}

enum class Categoria(val nom: String) {
    CERVESA("Cervesa"), VI("Vi"), REFRESCOS("Refrescos"), AIGUA("Aigua"), CAFE("Cafè i licors"), PICAR("Picar")
}

@Serializable
@Entity(tableName = "productes")
data class Producte(
    @PrimaryKey val id: String,
    val nom: String,
    val categoria: Categoria,
    val preu: Long,
    val preuConvidat: Long? = null, // null = mateix preu que el soci
    val estoc: Int = 0,
    val estocMinim: Int = 0,
    val controlaEstoc: Boolean = true,
    val actiu: Boolean = true,
) {
    fun preuPer(convidat: Boolean) = if (convidat && preuConvidat != null) preuConvidat else preu
    val calReposar get() = actiu && controlaEstoc && estoc <= estocMinim
}

enum class Origen { TAULETA, JUNTA }

@Serializable
@Entity(tableName = "consums", indices = [Index("sociId"), Index("ts")])
data class Consum(
    @PrimaryKey val id: String,
    val sociId: String,
    val producteId: String,
    val nomProducte: String,
    val quantitat: Int,
    val preuUnitari: Long,
    val convidat: String? = null,
    val ts: Long,
    val origen: Origen = Origen.TAULETA,
    val anulatTs: Long? = null,
    val anulatPer: String? = null,
) {
    val import get() = quantitat * preuUnitari
    val anulat get() = anulatTs != null
}

@Serializable
@Entity(tableName = "reposicions")
data class Reposicio(
    @PrimaryKey val id: String,
    val producteId: String,
    val quantitat: Int,
    val ts: Long,
    val perId: String,
)

@Serializable
data class Assistent(val sociId: String, val convidats: Int = 0, val parts: Double = 1.0)

@Serializable
data class DespesaApat(val id: String, val pagadorId: String, val concepte: String, val import: Long)

enum class ModeRepartiment { IGUALS, PROPORCIONAL }
enum class EstatApat { OBERT, TANCAT }

@Serializable
@Entity(tableName = "apats")
data class Apat(
    @PrimaryKey val id: String,
    val titol: String,
    val data: String, // AAAA-MM-DD
    val organitzadorId: String,
    val mode: ModeRepartiment = ModeRepartiment.IGUALS,
    val assistents: List<Assistent> = emptyList(),
    val despeses: List<DespesaApat> = emptyList(),
    val estat: EstatApat = EstatApat.OBERT,
)

enum class Franja(val nom: String) { DINAR("Dinar"), SOPAR("Sopar"), TOT_EL_DIA("Tot el dia") }

@Serializable
@Entity(tableName = "reserves", indices = [Index("data")])
data class Reserva(
    @PrimaryKey val id: String,
    val sociId: String,
    val data: String,
    val franja: Franja,
    val espai: String,
    val persones: Int,
    val notes: String = "",
    val creada: Long,
)

enum class EstatMes { OBERT, INFORMAT, TANCAT }

@Serializable
@Entity(tableName = "tancaments")
data class Tancament(
    @PrimaryKey val mes: String, // AAAA-MM
    val estat: EstatMes,
    val informatTs: Long,
    val tancatTs: Long? = null,
)

enum class EstatCorreu { PENDENT, ENVIAT, ERROR }

@Serializable
@Entity(tableName = "correus")
data class Correu(
    @PrimaryKey val id: String,
    val ts: Long,
    val per: String,
    val destinatariOriginal: String,
    val assumpte: String,
    val cos: String,
    val adjuntSociId: String? = null, // per regenerar el PDF del soci en reenviar
    val mes: String? = null,
    val estat: EstatCorreu = EstatCorreu.PENDENT,
    val error: String? = null,
)

@Serializable
data class Config(
    val nomSocietat: String = "GastroBacus",
    val correuTresoreria: String = "",
    val modeProves: Boolean = true,
    val correuProves: String = "xavigaya+gastrobacus@gmail.com",
    val minutsDesfer: Int = 10,
    val diesReclamacio: Int = 5,
    val espais: List<String> = listOf("Cuina i menjador gran", "Taula del racó"),
    val compteGoogle: String? = null,
    val fixarPantalla: Boolean = true,
    val darreraCopiaTs: Long? = null,
    val darreraCopiaError: String? = null,
)

@Entity(tableName = "config")
data class ConfigFila(@PrimaryKey val id: Int = 1, val json: String)
