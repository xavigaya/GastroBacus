package cat.gastrobacus.consum.dades

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

val CATALA: Locale = Locale.forLanguageTag("ca-ES")
val ZONA: ZoneId = ZoneId.of("Europe/Madrid")

fun nouId(): String = UUID.randomUUID().toString()

/** 130 -> "1,30 €" */
fun euros(cents: Long): String {
    val signe = if (cents < 0) "-" else ""
    val a = abs(cents)
    val enters = (a / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    return "$signe$enters,${(a % 100).toString().padStart(2, '0')} €"
}

/** "1,3" o "1.30" -> 130; null si no és un import vàlid */
fun llegirEuros(text: String): Long? {
    val t = text.trim().replace("€", "").replace(" ", "").replace(',', '.')
    if (t.isEmpty()) return null
    val d = t.toBigDecimalOrNull() ?: return null
    if (d.signum() < 0) return null
    return d.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
}

fun mesDe(ts: Long): String = YearMonth.from(Instant.ofEpochMilli(ts).atZone(ZONA)).toString()
fun mesDeData(data: String): String = data.take(7)
fun mesActual(): String = YearMonth.now(ZONA).toString()
fun mesAnterior(mes: String): String = YearMonth.parse(mes).minusMonths(1).toString()
fun mesSeguent(mes: String): String = YearMonth.parse(mes).plusMonths(1).toString()
fun avui(): String = LocalDate.now(ZONA).toString()

fun nomMes(mes: String): String {
    val ym = YearMonth.parse(mes)
    val n = ym.month.getDisplayName(TextStyle.FULL_STANDALONE, CATALA)
    return n.replaceFirstChar { it.uppercase() } + " " + ym.year
}

/** "d'octubre de 2026", "de març de 2026" */
fun deMes(mes: String): String {
    val ym = YearMonth.parse(mes)
    val n = ym.month.getDisplayName(TextStyle.FULL_STANDALONE, CATALA).lowercase(CATALA)
    val prep = if (n.first() in "aeiou") "d'" else "de "
    return "$prep$n de ${ym.year}"
}

fun hora(ts: Long): String = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(ts).atZone(ZONA))
fun diaCurt(ts: Long): String = DateTimeFormatter.ofPattern("d MMM", CATALA).format(Instant.ofEpochMilli(ts).atZone(ZONA)).replace('’', '\'')
fun diaMes(ts: Long): String = DateTimeFormatter.ofPattern("dd/MM").format(Instant.ofEpochMilli(ts).atZone(ZONA))
fun dataLlarga(data: String): String =
    DateTimeFormatter.ofPattern("EEEE d MMMM", CATALA).format(LocalDate.parse(data)).replace('’', '\'').replaceFirstChar { it.uppercase() }
fun dataHora(ts: Long): String = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", CATALA).format(Instant.ofEpochMilli(ts).atZone(ZONA))
fun dataPlena(ts: Long): String = DateTimeFormatter.ofPattern("d MMMM", CATALA).format(Instant.ofEpochMilli(ts).atZone(ZONA)).replace('’', '\'')
fun tsDe(data: String, horaDelDia: Int = 21): Long =
    LocalDateTime.of(LocalDate.parse(data), java.time.LocalTime.of(horaDelDia, 0)).atZone(ZONA).toInstant().toEpochMilli()

object Pin {
    fun novaSal(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    fun hash(pin: String, sal: String): String =
        MessageDigest.getInstance("SHA-256").digest("$sal:$pin".toByteArray()).joinToString("") { "%02x".format(it) }
    fun valid(pin: String) = pin.length == 4 && pin.all { it.isDigit() }
}
