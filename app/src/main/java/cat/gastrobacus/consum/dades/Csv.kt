package cat.gastrobacus.consum.dades

import java.text.Normalizer

/*
 * Importació de productes i socis des de fitxers CSV (separats per punt i coma o per comes).
 * La primera fila ha de tenir els noms de les columnes; l'ordre no importa.
 */

data class FilaProducte(
    val nom: String,
    val categoria: Categoria,
    val preu: Long,
    val preuConvidat: Long?,
    val estoc: Int?,
    val estocMinim: Int?,
    val avisConsum: Int?,
    val favorit: Boolean?,
)

data class FilaSoci(
    val nom: String,
    val cognoms: String,
    val email: String,
    val refBanc: String,
    val pin: String?,
    val junta: Boolean,
    val dataAlta: String?,
)

data class LecturaCsv<T>(val files: List<T>, val errors: List<String>)

object Csv {
    const val PLANTILLA_PRODUCTES =
        "nom;categoria;preu;preu_convidat;estoc;estoc_minim;avis_consum;favorit\n" +
            "Cervesa Moritz 33 cl;Cervesa;1,30;1,60;96;24;48;si\n" +
            "Vi negre Costers del Segre;Vi;6,50;8,00;14;6;;no\n" +
            "Cafè;Cafè i licors;0,60;;;;;si\n"

    const val PLANTILLA_SOCIS =
        "nom;cognoms;correu;ref_banc;pin;junta;data_alta\n" +
            "Anna;Bosch Serra;anna.bosch@exemple.cat;GB-0004;1234;no;2019-03-01\n" +
            "Jordi;Puig Farré;jordi.puig@exemple.cat;GB-0001;5678;si;2015-01-10\n"

    val COLUMNES_PRODUCTES = listOf(
        "nom" to "Nom del producte (obligatori). Si ja existeix, s'actualitza.",
        "categoria" to "Cervesa, Vi, Refrescos, Aigua, Cafè i licors o Picar (obligatori).",
        "preu" to "Preu per al soci, amb coma o punt (obligatori).",
        "preu_convidat" to "Preu per a convidats. Buit si és el mateix.",
        "estoc" to "Unitats que hi ha ara (inventari inicial).",
        "estoc_minim" to "Avisa quan en quedin aquestes unitats.",
        "avis_consum" to "Avisa quan se n'hagin consumit aquestes des de la darrera comanda.",
        "favorit" to "si o no: favorit per a tothom.",
    )

    val COLUMNES_SOCIS = listOf(
        "nom" to "Nom (obligatori).",
        "cognoms" to "Cognoms.",
        "correu" to "Correu per a l'informe mensual. Si ja existeix, s'actualitza el soci.",
        "ref_banc" to "Referència del soci al banc. Buida: es posa GB-0001, GB-0002…",
        "pin" to "PIN de 4 xifres (obligatori per als socis nous).",
        "junta" to "si o no.",
        "data_alta" to "AAAA-MM-DD o DD/MM/AAAA. Buida: avui.",
    )

    // ---------- Exportació (mateix format que la importació, per poder-ho tornar a importar) ----------

    private fun camp(t: String) = if (t.any { it == ';' || it == '"' || it == '\n' }) "\"" + t.replace("\"", "\"\"") + "\"" else t
    private fun diners(c: Long) = "%d,%02d".format(c / 100, c % 100)
    private fun fila(v: List<String>) = v.joinToString(";") { camp(it) }

    /** Productes actius, amb l'estoc actual. */
    fun exportaProductes(productes: List<Producte>): String =
        (listOf(fila(COLUMNES_PRODUCTES.map { it.first })) + productes.filter { it.actiu }.sortedWith(compareBy({ it.categoria.ordinal }, { it.nom })).map { p ->
            fila(listOf(
                p.nom, p.categoria.nom, diners(p.preu), p.preuConvidat?.let { diners(it) } ?: "",
                if (p.controlaEstoc) p.estoc.toString() else "", if (p.estocMinim > 0) p.estocMinim.toString() else "",
                if (p.avisConsum > 0) p.avisConsum.toString() else "", if (p.favorit) "si" else "no",
            ))
        }).joinToString("\r\n", postfix = "\r\n")

    /** Socis actius. El PIN no s'exporta (només se'n guarda una empremta); en tornar a importar es conserva. */
    fun exportaSocis(socis: List<Soci>): String =
        (listOf(fila(COLUMNES_SOCIS.map { it.first })) + socis.filter { it.actiu }.sortedWith(compareBy({ it.nom }, { it.cognoms })).map { s ->
            fila(listOf(s.nom, s.cognoms, s.email, s.refBanc, "", if (s.esJunta) "si" else "no", s.periodes.lastOrNull()?.alta ?: ""))
        }).joinToString("\r\n", postfix = "\r\n")

    /** Divideix una línia CSV respectant les cometes. */
    fun divideix(linia: String, sep: Char): List<String> {
        val r = mutableListOf<String>()
        val sb = StringBuilder()
        var cometes = false
        var i = 0
        while (i < linia.length) {
            val c = linia[i]
            when {
                c == '"' && cometes && i + 1 < linia.length && linia[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> cometes = !cometes
                c == sep && !cometes -> { r += sb.toString().trim(); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        r += sb.toString().trim()
        return r
    }

    private fun clau(t: String) = Normalizer.normalize(t.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun taula(text: String): Pair<List<String>, List<Pair<Int, Map<String, String>>>> {
        val linies = text.removePrefix("﻿").lines().map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (linies.isEmpty()) return emptyList<String>() to emptyList()
        val sep = if (linies[0].count { it == ';' } >= linies[0].count { it == ',' }) ';' else ','
        val cap = divideix(linies[0], sep).map { clau(it) }
        val files = linies.drop(1).mapIndexed { i, l ->
            val v = divideix(l, sep)
            (i + 2) to cap.indices.associate { j -> cap[j] to (v.getOrNull(j) ?: "") }
        }
        return cap to files
    }

    private fun sino(t: String?): Boolean? = when (clau(t ?: "")) {
        "si", "s", "yes", "y", "1", "x", "true" -> true
        "no", "n", "0", "false" -> false
        else -> null
    }

    private fun enter(t: String?): Int? = t?.trim()?.takeIf { it.isNotEmpty() }?.toIntOrNull()

    fun categoria(t: String): Categoria? {
        val c = clau(t)
        return Categoria.entries.firstOrNull { clau(it.nom) == c || clau(it.name) == c }
            ?: when {
                c.startsWith("cervesa") || c == "birra" -> Categoria.CERVESA
                c.startsWith("vi") || c == "cava" -> Categoria.VI
                c.startsWith("refresc") -> Categoria.REFRESCOS
                c.startsWith("aigua") -> Categoria.AIGUA
                c.startsWith("cafe") || c.startsWith("licor") -> Categoria.CAFE
                c.startsWith("picar") || c == "tapes" -> Categoria.PICAR
                else -> null
            }
    }

    fun data(t: String?): String? {
        val s = t?.trim().orEmpty()
        if (s.isEmpty()) return null
        Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$").find(s)?.let { m ->
            return "%s-%02d-%02d".format(m.groupValues[1], m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
        Regex("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})$").find(s)?.let { m ->
            return "%s-%02d-%02d".format(m.groupValues[3], m.groupValues[2].toInt(), m.groupValues[1].toInt())
        }
        return ""
    }

    fun llegeixProductes(text: String): LecturaCsv<FilaProducte> {
        val (cap, files) = taula(text)
        val errors = mutableListOf<String>()
        listOf("nom", "categoria", "preu").filter { it !in cap }.let { if (it.isNotEmpty()) return LecturaCsv(emptyList(), listOf("Falten les columnes: ${it.joinToString()}.")) }
        val r = files.mapNotNull { (n, f) ->
            val nom = f["nom"].orEmpty().trim()
            if (nom.isEmpty()) { errors += "Fila $n: falta el nom."; return@mapNotNull null }
            val cat = categoria(f["categoria"].orEmpty()) ?: run { errors += "Fila $n ($nom): categoria «${f["categoria"]}» desconeguda."; return@mapNotNull null }
            val preu = llegirEuros(f["preu"].orEmpty()) ?: run { errors += "Fila $n ($nom): preu no vàlid."; return@mapNotNull null }
            val pc = f["preu_convidat"].orEmpty().takeIf { it.isNotBlank() }?.let { llegirEuros(it) ?: run { errors += "Fila $n ($nom): preu de convidat no vàlid."; return@mapNotNull null } }
            FilaProducte(nom, cat, preu, pc, enter(f["estoc"]), enter(f["estoc_minim"]), enter(f["avis_consum"]), sino(f["favorit"]))
        }
        return LecturaCsv(r, errors)
    }

    fun llegeixSocis(text: String): LecturaCsv<FilaSoci> {
        val (cap, files) = taula(text)
        val errors = mutableListOf<String>()
        if ("nom" !in cap) return LecturaCsv(emptyList(), listOf("Falta la columna nom."))
        val r = files.mapNotNull { (n, f) ->
            val nom = f["nom"].orEmpty().trim()
            if (nom.isEmpty()) { errors += "Fila $n: falta el nom."; return@mapNotNull null }
            val pin = f["pin"].orEmpty().trim().ifEmpty { null }
            if (pin != null && !Pin.valid(pin)) { errors += "Fila $n ($nom): el PIN ha de tenir 4 xifres."; return@mapNotNull null }
            val email = (f["correu"] ?: f["email"]).orEmpty().trim()
            if (email.isNotEmpty() && !email.contains("@")) { errors += "Fila $n ($nom): correu no vàlid."; return@mapNotNull null }
            val alta = data(f["data_alta"])
            if (alta == "") { errors += "Fila $n ($nom): data d'alta no vàlida."; return@mapNotNull null }
            FilaSoci(nom, f["cognoms"].orEmpty().trim(), email, f["ref_banc"].orEmpty().trim(), pin, sino(f["junta"]) == true, alta)
        }
        return LecturaCsv(r, errors)
    }
}
