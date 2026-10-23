package cat.gastrobacus.consum.dades

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class Conversors {
    @TypeConverter fun assistentsA(v: List<Assistent>): String = JSON.encodeToString(v)
    @TypeConverter fun aAssistents(v: String): List<Assistent> = JSON.decodeFromString(v)
    @TypeConverter fun despesesA(v: List<DespesaApat>): String = JSON.encodeToString(v)
    @TypeConverter fun aDespeses(v: String): List<DespesaApat> = JSON.decodeFromString(v)
}

@Dao
interface Dao_ {
    // Socis
    @Query("SELECT * FROM socis") fun socis(): Flow<List<Soci>>
    @Query("SELECT * FROM socis") suspend fun totsSocis(): List<Soci>
    @Query("SELECT * FROM socis WHERE id = :id") suspend fun soci(id: String): Soci?
    @Upsert suspend fun desaSoci(s: Soci)

    // Productes
    @Query("SELECT * FROM productes") fun productes(): Flow<List<Producte>>
    @Query("SELECT * FROM productes") suspend fun totsProductes(): List<Producte>
    @Query("SELECT * FROM productes WHERE id = :id") suspend fun producte(id: String): Producte?
    @Upsert suspend fun desaProducte(p: Producte)

    // Consums
    @Query("SELECT * FROM consums ORDER BY ts DESC") fun consums(): Flow<List<Consum>>
    @Query("SELECT * FROM consums") suspend fun totsConsums(): List<Consum>
    @Query("SELECT * FROM consums WHERE id = :id") suspend fun consum(id: String): Consum?
    @Upsert suspend fun desaConsum(c: Consum)
    @Upsert suspend fun desaConsums(c: List<Consum>)

    // Reposicions
    @Query("SELECT * FROM reposicions ORDER BY ts DESC") fun reposicions(): Flow<List<Reposicio>>
    @Query("SELECT * FROM reposicions") suspend fun totesReposicions(): List<Reposicio>
    @Upsert suspend fun desaReposicio(r: Reposicio)

    // Àpats
    @Query("SELECT * FROM apats ORDER BY data DESC") fun apats(): Flow<List<Apat>>
    @Query("SELECT * FROM apats") suspend fun totsApats(): List<Apat>
    @Query("SELECT * FROM apats WHERE id = :id") suspend fun apat(id: String): Apat?
    @Upsert suspend fun desaApat(a: Apat)
    @Query("DELETE FROM apats WHERE id = :id") suspend fun eliminaApat(id: String)

    // Reserves
    @Query("SELECT * FROM reserves ORDER BY data, franja") fun reserves(): Flow<List<Reserva>>
    @Query("SELECT * FROM reserves") suspend fun totesReserves(): List<Reserva>
    @Query("SELECT * FROM reserves WHERE data = :data") suspend fun reservesDe(data: String): List<Reserva>
    @Upsert suspend fun desaReserva(r: Reserva)
    @Query("DELETE FROM reserves WHERE id = :id") suspend fun eliminaReserva(id: String)

    // Tancaments
    @Query("SELECT * FROM tancaments") fun tancaments(): Flow<List<Tancament>>
    @Query("SELECT * FROM tancaments") suspend fun totsTancaments(): List<Tancament>
    @Query("SELECT * FROM tancaments WHERE mes = :mes") suspend fun tancament(mes: String): Tancament?
    @Upsert suspend fun desaTancament(t: Tancament)
    @Query("DELETE FROM tancaments WHERE mes = :mes") suspend fun eliminaTancament(mes: String)

    // Correus
    @Query("SELECT * FROM correus ORDER BY ts DESC") fun correus(): Flow<List<Correu>>
    @Query("SELECT * FROM correus") suspend fun totsCorreus(): List<Correu>
    @Query("SELECT * FROM correus WHERE estat != 'ENVIAT'") suspend fun correusPendents(): List<Correu>
    @Upsert suspend fun desaCorreu(c: Correu)
    @Upsert suspend fun desaCorreus(c: List<Correu>)

    // Configuració
    @Query("SELECT * FROM config WHERE id = 1") fun config(): Flow<ConfigFila?>
    @Query("SELECT * FROM config WHERE id = 1") suspend fun configActual(): ConfigFila?
    @Upsert suspend fun desaConfig(c: ConfigFila)

    // Esborrar-ho tot (restaurar còpies)
    @Query("DELETE FROM socis") suspend fun buidaSocis()
    @Query("DELETE FROM productes") suspend fun buidaProductes()
    @Query("DELETE FROM consums") suspend fun buidaConsums()
    @Query("DELETE FROM reposicions") suspend fun buidaReposicions()
    @Query("DELETE FROM apats") suspend fun buidaApats()
    @Query("DELETE FROM reserves") suspend fun buidaReserves()
    @Query("DELETE FROM tancaments") suspend fun buidaTancaments()
    @Query("DELETE FROM correus") suspend fun buidaCorreus()
    @Query("DELETE FROM config") suspend fun buidaConfig()

    @Delete suspend fun eliminaSoci(s: Soci)
}

@Database(
    entities = [Soci::class, Producte::class, Consum::class, Reposicio::class, Apat::class, Reserva::class, Tancament::class, Correu::class, ConfigFila::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Conversors::class)
abstract class BaseDades : RoomDatabase() {
    abstract fun dao(): Dao_

    companion object {
        @Volatile private var instancia: BaseDades? = null
        fun de(context: Context): BaseDades = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(context.applicationContext, BaseDades::class.java, "gastrobacus.db")
                .build().also { instancia = it }
        }
        fun enMemoria(context: Context): BaseDades =
            Room.inMemoryDatabaseBuilder(context, BaseDades::class.java).allowMainThreadQueries().build()
    }
}
