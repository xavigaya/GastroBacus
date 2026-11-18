package cat.gastrobacus.consum.dades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/* Tots els imports són en cèntims d'euro (Long) per evitar errors d'arrodoniment. */

enum class Rol { SOCI, JUNTA }

/** Un període com a soci: data d'alta i, si n'hi ha, de baixa (AAAA-MM-DD). */
@Serializable
data class Periode(val alta: String, val baixa: String? = null)

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
    @ColumnInfo(defaultValue = "[]") val periodes: List<Periode> = emptyList(),
) {
    val nomComplet get() = "$nom $cognoms".trim()
    val inicials get() = (nom.take(1) + cognoms.take(1)).uppercase()
    val esJunta get() = rol == Rol.JUNTA
    val dataAlta get() = periodes.firstOrNull()?.alta
    val dataBaixa get() = if (actiu) null else periodes.lastOrNull()?.baixa
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
    /** La junta el fixa com a favorit per a tothom. */
    @ColumnInfo(defaultValue = "0") val favorit: Boolean = false,
    /** Unitats consumides des de la darrera comanda (es posa a zero en rebre'n una). */
    @ColumnInfo(defaultValue = "0") val comptador: Int = 0,
    /** Avisa quan el comptador arriba a aquesta quantitat (0 = sense avís). */
    @ColumnInfo(defaultValue = "0") val avisConsum: Int = 0,
    val darreraComandaTs: Long? = null,
    @ColumnInfo(defaultValue = "0") val avisMinimFet: Boolean = false,
    @ColumnInfo(defaultValue = "0") val avisConsumFet: Boolean = false,
) {
    fun preuPer(convidat: Boolean) = if (convidat && preuConvidat != null) preuConvidat else preu
    val calReposar get() = actiu && controlaEstoc && estoc <= estocMinim
}

enum class Origen { TAULETA, JUNTA }
enum class Pagament { FACTURA, BIZUM }

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
    /** BIZUM: s'ha pagat al moment i no va a la factura mensual (si el Bizum es confirma). */
    @ColumnInfo(defaultValue = "FACTURA") val pagament: Pagament = Pagament.FACTURA,
    val bizumId: String? = null,
) {
    val import get() = quantitat * preuUnitari
    val anulat get() = anulatTs != null
}

enum class TipusReposicio { ENTRADA, RECOMPTE }

@Serializable
@Entity(tableName = "reposicions")
data class Reposicio(
    @PrimaryKey val id: String,
    val producteId: String,
    val quantitat: Int, // ENTRADA: unitats que entren; RECOMPTE: estoc comptat
    val ts: Long,
    val perId: String,
    @ColumnInfo(defaultValue = "ENTRADA") val tipus: TipusReposicio = TipusReposicio.ENTRADA,
    @ColumnInfo(defaultValue = "0") val estocAnterior: Int = 0,
    /** Unitats consumides des de la comanda anterior (el comptador abans del reset). */
    @ColumnInfo(defaultValue = "0") val consumitPeriode: Int = 0,
)

@Serializable
data class Assistent(
    val sociId: String,
    val convidats: Int = 0,
    val parts: Double = 1.0,
    /** Activitats que es cobren en efectiu (sopar de socis): ja ha pagat la seva part. */
    val pagatEfectiu: Boolean = false,
)

/** Persona de fora en una activitat oberta. Tot el que fa va al compte del soci responsable. */
@Serializable
data class Extern(
    val id: String,
    val nom: String,
    val parts: Double = 1.0,
    val pagat: Boolean = false,
    val responsableId: String = "",
)

@Serializable
data class DespesaApat(val id: String, val pagadorId: String, val concepte: String, val import: Long)

enum class ModeRepartiment { IGUALS, PROPORCIONAL }
enum class EstatApat { OBERT, TANCAT }

enum class TipusActivitat(val nom: String, val descripcio: String) {
    PRIVAT("Privat", "Un soci organitza per a ell i els seus convidats"),
    SOCIS("Socis", "S'hi pot apuntar qualsevol soci"),
    OBERT("Obert", "Socis i també persones de fora"),
}

const val TOT_EL_LOCAL = "Tot el local"

enum class Neteja(val nom: String) { SENSE("Sense neteja"), NORMAL("Neteja"), AMPLIADA("Neteja ampliada") }

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
    @ColumnInfo(defaultValue = "PRIVAT") val tipus: TipusActivitat = TipusActivitat.PRIVAT,
    @ColumnInfo(defaultValue = "SOPAR") val franja: Franja = Franja.SOPAR,
    /** TOT_EL_LOCAL (bloqueja el dia) o el nom d'un espai que es comparteix. */
    @ColumnInfo(defaultValue = "Tot el local") val espai: String = TOT_EL_LOCAL,
    @ColumnInfo(defaultValue = "[]") val externs: List<Extern> = emptyList(),
    /** Preus de la cadira en el moment de crear l'activitat (la junta els pot canviar). */
    @ColumnInfo(defaultValue = "0") val cadiraSoci: Long = 0,
    @ColumnInfo(defaultValue = "0") val cadiraNoSoci: Long = 0,
    // Versió 3
    @ColumnInfo(defaultValue = "SENSE") val neteja: Neteja = Neteja.SENSE,
    /** Preu de la neteja en el moment de triar-la; es carrega a qui organitza. */
    @ColumnInfo(defaultValue = "0") val preuNeteja: Long = 0,
    /** El menjar i la cadira es paguen en efectiu a l'activitat (sopar de socis), no van al compte. */
    @ColumnInfo(defaultValue = "0") val cobramentEfectiu: Boolean = false,
    /** Ja s'ha enviat el resum de l'endemà. */
    @ColumnInfo(defaultValue = "0") val resumEnviat: Boolean = false,
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
    /** Reservat tot el local: no s'hi pot fer cap altra reserva aquella franja. */
    @ColumnInfo(defaultValue = "0") val exclusiu: Boolean = false,
    val apatId: String? = null,
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

/** Convidat que surt a «Qui ets?» fins a les 7 del matí següent. Apunta al compte del soci que el porta. */
@Serializable
@Entity(tableName = "convidats")
data class ConvidatDia(
    @PrimaryKey val id: String,
    val nom: String,
    val sociId: String,
    val creat: Long,
    val caduca: Long,
)

enum class EstatBizum { PENDENT, REBUT, NO_ARRIBAT }

@Serializable
@Entity(tableName = "bizums")
data class Bizum(
    @PrimaryKey val id: String,
    val sociId: String?, // null si paga algú de fora (activitat oberta)
    val nom: String, // qui l'ha de fer
    val concepte: String,
    val import: Long,
    val ts: Long,
    val estat: EstatBizum = EstatBizum.PENDENT,
    val confirmatPer: String? = null,
    val confirmatTs: Long? = null,
    val apatId: String? = null,
    val externId: String? = null,
)

enum class EstatTransferencia { PENDENT, CONFIRMADA, REBUTJADA }

/** Pagament per transferència que declara el soci. Queda pendent fins que tresoreria el confirma. */
@Serializable
@Entity(tableName = "transferencies", indices = [Index("sociId")])
data class Transferencia(
    @PrimaryKey val id: String,
    val sociId: String,
    val import: Long,
    val data: String, // dia de la transferència, AAAA-MM-DD
    val concepte: String,
    val ts: Long,
    val estat: EstatTransferencia = EstatTransferencia.PENDENT,
    val confirmatPer: String? = null,
    val confirmatTs: Long? = null,
)

enum class TipusAvis { ESTOC_MINIM, CONSUM }

@Serializable
@Entity(tableName = "avisos")
data class AvisEstoc(
    @PrimaryKey val id: String,
    val producteId: String,
    val tipus: TipusAvis,
    val missatge: String,
    val ts: Long,
    val llegit: Boolean = false,
)

enum class TipusPeticio(val nom: String) { FALTA("Falta"), PROPOSTA("Proposta") }
enum class EstatPeticio { PENDENT, FETA, DESCARTADA }

@Serializable
@Entity(tableName = "peticions")
data class Peticio(
    @PrimaryKey val id: String,
    val sociId: String,
    val tipus: TipusPeticio,
    val producteId: String? = null,
    val text: String,
    val ts: Long,
    val estat: EstatPeticio = EstatPeticio.PENDENT,
)

@Serializable
data class PreuCadira(val soci: Long = 0, val noSoci: Long = 0)

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
    // Versió 2
    val preusCadira: Map<TipusActivitat, PreuCadira> = mapOf(
        TipusActivitat.PRIVAT to PreuCadira(200, 300),
        TipusActivitat.SOCIS to PreuCadira(200, 300),
        TipusActivitat.OBERT to PreuCadira(300, 500),
    ),
    val telefonBizum: String = "",
    val correuReserves: String = "",
    val correuAvisos: String = "",
    val correuProveidor: String = "",
    val horaFiDia: Int = 7, // els convidats desapareixen a aquesta hora
    val favoritsPerSoci: Int = 8,
    // Versió 3: pagaments
    /** Si és fals, no es genera el fitxer de rebuts per al banc: es paga per transferència o Bizum. */
    val rebutBancari: Boolean = false,
    val compteTransferencia: String = "", // IBAN de la societat
    val diesAvisDeute: Int = 45,
    // Neteja i sopar de socis
    val preusNeteja: Map<Neteja, Long> = mapOf(Neteja.SENSE to 0, Neteja.NORMAL to 2000, Neteja.AMPLIADA to 4000),
    val soparSocisEfectiu: Boolean = true,
    // Resums automàtics (correus separats per comes)
    val resumEstocPer: String = "",
    val resumEstocDia: Int = 1, // 1 = dilluns … 7 = diumenge
    val resumTresoreriaPer: String = "",
    val resumTresoreriaDies: Int = 15,
    val resumActivitatsPer: String = "",
    val resumActivitatsOrganitzador: Boolean = true,
    val darrerResumEstoc: Long? = null,
    val darrerResumTresoreria: Long? = null,
)

@Entity(tableName = "config")
data class ConfigFila(@PrimaryKey val id: Int = 1, val json: String)
