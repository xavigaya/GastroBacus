package cat.gastrobacus.consum.exportar

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import cat.gastrobacus.consum.R
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.InformeSoci
import cat.gastrobacus.consum.dades.TipusLinia
import cat.gastrobacus.consum.dades.deMes
import cat.gastrobacus.consum.dades.diaMes
import cat.gastrobacus.consum.dades.euros
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class Fitxer(val nom: String, val tipus: String, val dades: ByteArray)

const val TIPUS_PDF = "application/pdf"
const val TIPUS_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
const val TIPUS_CSV = "text/csv"

/* ======================= PDF ======================= */

private class Pdf(context: Context) {
    private val doc = PdfDocument()
    private val normal = ResourcesCompat.getFont(context, R.font.atkinson_regular) ?: Typeface.SANS_SERIF
    private val negreta = ResourcesCompat.getFont(context, R.font.atkinson_bold) ?: Typeface.DEFAULT_BOLD
    private val most = 0xFF5E1A3A.toInt()
    private val roure = 0xFF2B2118.toInt()
    private val pedra = 0xFFEEF0E8.toInt()
    private val ample = 595
    private val alt = 842
    private val marge = 40f
    private var pagina: PdfDocument.Page? = null
    private var n = 0
    var y = 0f

    private fun paint(mida: Float, bold: Boolean = false, color: Int = roure, dreta: Boolean = false) = Paint().apply {
        isAntiAlias = true; textSize = mida; typeface = if (bold) negreta else normal; this.color = color
        textAlign = if (dreta) Paint.Align.RIGHT else Paint.Align.LEFT
    }

    fun novaPagina(titol: String, subtitol: String) {
        pagina?.let { doc.finishPage(it) }
        n++
        val p = doc.startPage(PdfDocument.PageInfo.Builder(ample, alt, n).create())
        pagina = p
        val c = p.canvas
        c.drawRect(0f, 0f, ample.toFloat(), 70f, Paint().apply { color = most })
        c.drawText("GastroBacus", marge, 38f, paint(20f, true, 0xFFFFFFFF.toInt()))
        c.drawText("Societat gastronòmica", marge, 56f, paint(11f, false, 0xFFFFFFFF.toInt()))
        c.drawText(titol, marge, 102f, paint(16f, true))
        c.drawText(subtitol, marge, 120f, paint(11f))
        c.drawText("Pàgina $n", ample - marge, alt - 24f, paint(9f, dreta = true))
        y = 146f
    }

    /** Taula simple: columnes amb amplada relativa i alineació. */
    fun taula(cap: List<String>, files: List<List<String>>, amples: List<Float>, dreta: Set<Int>, peu: List<String>?, titol: String, subtitol: String) {
        val total = ample - 2 * marge
        val xs = amples.runningFold(marge) { acc, w -> acc + w * total }
        fun fila(valors: List<String>, bold: Boolean, fons: Int?, colorText: Int = roure) {
            if (y > alt - 60) { novaPagina(titol, subtitol); fila(cap, true, most, 0xFFFFFFFF.toInt()) }
            val c = pagina!!.canvas
            if (fons != null) c.drawRect(marge, y - 13f, ample - marge, y + 6f, Paint().apply { color = fons })
            valors.forEachIndexed { i, v ->
                val p = paint(9.5f, bold, colorText, i in dreta)
                val x = if (i in dreta) xs[i + 1] - 4f else xs[i] + 4f
                val maxW = xs[i + 1] - xs[i] - 8f
                var t = v
                while (p.measureText(t) > maxW && t.length > 3) t = t.dropLast(2) + "…"
                c.drawText(t, x, y, p)
            }
            y += 19f
        }
        fila(cap, true, most, 0xFFFFFFFF.toInt())
        files.forEachIndexed { i, f -> fila(f, false, if (i % 2 == 1) pedra else null) }
        if (peu != null) fila(peu, true, pedra)
    }

    fun text(t: String, mida: Float = 10f, bold: Boolean = false) {
        pagina!!.canvas.drawText(t, marge, y, paint(mida, bold)); y += mida + 8f
    }

    fun bytes(): ByteArray {
        pagina?.let { doc.finishPage(it) }
        return ByteArrayOutputStream().also { doc.writeTo(it); doc.close() }.toByteArray()
    }
}

fun pdfSoci(context: Context, inf: InformeSoci, mes: String): Fitxer {
    val pdf = Pdf(context)
    val titol = "Consum ${deMes(mes)}"
    val sub = "${inf.soci.nomComplet} · Ref. ${inf.soci.refBanc}"
    pdf.novaPagina(titol, sub)
    pdf.taula(
        listOf("Data", "Concepte", "Quant.", "Preu", "Import"),
        inf.linies.map { listOf(diaMes(it.ts), it.concepte + if (it.detall.isNotBlank()) " (${it.detall})" else "", it.quantitat.toString(), euros(it.preu), euros(it.import)) },
        listOf(0.12f, 0.52f, 0.1f, 0.13f, 0.13f), setOf(2, 3, 4),
        listOf("", if (inf.total >= 0) "Total a carregar" else "Saldo a favor", "", "", euros(inf.total)), titol, sub,
    )
    return Fitxer("GastroBacus-$mes-${inf.soci.nom}-${inf.soci.cognoms.substringBefore(' ')}.pdf".replace(" ", "_"), TIPUS_PDF, pdf.bytes())
}

fun pdfJunta(context: Context, estat: Estat, mes: String): Fitxer {
    val informes = estat.informes(mes)
    val total = informes.sumOf { it.total }
    val pdf = Pdf(context)
    val titol = "Informe de consum ${deMes(mes)}"
    val sub = "${informes.size} socis · Total ${euros(total)}"
    pdf.novaPagina(titol, sub)
    pdf.taula(
        listOf("Soci", "Ref. banc", "Consum", "Àpats", "Abonaments", "Total"),
        informes.map { listOf(it.soci.nomComplet, it.soci.refBanc, euros(it.totalConsum), euros(it.totalApats), euros(it.totalAbonaments), euros(it.total)) },
        listOf(0.32f, 0.13f, 0.14f, 0.13f, 0.14f, 0.14f), setOf(2, 3, 4, 5),
        listOf("Total", "", "", "", "", euros(total)), titol, sub,
    )
    return Fitxer("GastroBacus-informe-$mes.pdf", TIPUS_PDF, pdf.bytes())
}

/* ======================= Excel (XLSX mínim) ======================= */

private sealed class Cel {
    data class T(val v: String, val negreta: Boolean = false) : Cel()
    data class E(val cents: Long, val negreta: Boolean = false) : Cel() // import en euros
    data class N(val v: Number) : Cel()
    data class D(val ts: Long) : Cel()
}

private class Full(val nom: String, val amples: List<Int>, val files: List<List<Cel>>)

private fun esc(t: String) = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private fun col(i: Int): String { var n = i; var s = ""; do { s = ('A' + n % 26) + s; n = n / 26 - 1 } while (n >= 0); return s }

private fun xlsx(fulls: List<Full>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { z ->
        fun put(nom: String, cont: String) { z.putNextEntry(ZipEntry(nom)); z.write(cont.toByteArray()); z.closeEntry() }
        put("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
            fulls.indices.joinToString("") { """<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" } + "</Types>")
        put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
        put("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""" +
            fulls.mapIndexed { i, f -> """<sheet name="${esc(f.nom)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""" }.joinToString("") + "</sheets></workbook>")
        put("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            fulls.indices.joinToString("") { """<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""" } +
            """<Relationship Id="rId${fulls.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""")
        // Estils: 0 normal, 1 negreta, 2 euros, 3 euros negreta, 4 data, 5 capçalera
        put("xl/styles.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="2"><numFmt numFmtId="164" formatCode="#,##0.00\ &quot;€&quot;"/><numFmt numFmtId="165" formatCode="dd/mm/yyyy"/></numFmts><fonts count="3"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF5E1A3A"/></patternFill></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf/></cellStyleXfs><cellXfs count="6"><xf/><xf fontId="1" applyFont="1"/><xf numFmtId="164" applyNumberFormat="1"/><xf numFmtId="164" fontId="1" applyNumberFormat="1" applyFont="1"/><xf numFmtId="165" applyNumberFormat="1"/><xf fontId="2" fillId="2" applyFont="1" applyFill="1"/></cellXfs></styleSheet>""")
        fulls.forEachIndexed { fi, f ->
            val sb = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews><cols>""")
            f.amples.forEachIndexed { i, w -> sb.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
            sb.append("</cols><sheetData>")
            f.files.forEachIndexed { ri, fila ->
                sb.append("""<row r="${ri + 1}">""")
                fila.forEachIndexed { ci, c ->
                    val ref = col(ci) + (ri + 1)
                    when (c) {
                        is Cel.T -> sb.append("""<c r="$ref" t="inlineStr" s="${if (ri == 0) 5 else if (c.negreta) 1 else 0}"><is><t xml:space="preserve">${esc(c.v)}</t></is></c>""")
                        is Cel.E -> sb.append("""<c r="$ref" s="${if (c.negreta) 3 else 2}"><v>${c.cents / 100.0}</v></c>""")
                        is Cel.N -> sb.append("""<c r="$ref"><v>${c.v}</v></c>""")
                        is Cel.D -> sb.append("""<c r="$ref" s="4"><v>${25569.0 + (c.ts + java.util.TimeZone.getTimeZone("Europe/Madrid").getOffset(c.ts)) / 86_400_000.0}</v></c>""")
                    }
                }
                sb.append("</row>")
            }
            sb.append("</sheetData></worksheet>")
            put("xl/worksheets/sheet${fi + 1}.xml", sb.toString())
        }
    }
    return out.toByteArray()
}

fun excelJunta(estat: Estat, mes: String): Fitxer {
    val informes = estat.informes(mes)
    val resum = listOf(listOf<Cel>(Cel.T("Soci"), Cel.T("Referència banc"), Cel.T("Correu"), Cel.T("Consumicions"), Cel.T("Àpats"), Cel.T("Abonaments"), Cel.T("Total"))) +
        informes.map { listOf(Cel.T(it.soci.nomComplet), Cel.T(it.soci.refBanc), Cel.T(it.soci.email), Cel.E(it.totalConsum), Cel.E(it.totalApats), Cel.E(it.totalAbonaments), Cel.E(it.total)) } +
        listOf(listOf(Cel.T("Total", true), Cel.T(""), Cel.T(""), Cel.E(informes.sumOf { it.totalConsum }, true), Cel.E(informes.sumOf { it.totalApats }, true), Cel.E(informes.sumOf { it.totalAbonaments }, true), Cel.E(informes.sumOf { it.total }, true)))
    val detall = listOf(listOf<Cel>(Cel.T("Data"), Cel.T("Soci"), Cel.T("Concepte"), Cel.T("Detall"), Cel.T("Quantitat"), Cel.T("Preu"), Cel.T("Import"))) +
        informes.flatMap { inf -> inf.linies.map { l -> listOf(Cel.D(l.ts), Cel.T(inf.soci.nomComplet), Cel.T(l.concepte), Cel.T(l.detall), Cel.N(l.quantitat), Cel.E(l.preu), Cel.E(l.import)) } }
    val perProducte = informes.flatMap { it.linies }.filter { it.tipus == TipusLinia.CONSUM }
        .groupBy { it.concepte }.map { (p, ls) -> Triple(p, ls.sumOf { it.quantitat }, ls.sumOf { it.import }) }.sortedByDescending { it.third }
    val prod = listOf(listOf<Cel>(Cel.T("Producte"), Cel.T("Unitats"), Cel.T("Import"))) + perProducte.map { listOf(Cel.T(it.first), Cel.N(it.second), Cel.E(it.third)) }
    return Fitxer(
        "GastroBacus-consum-$mes.xlsx", TIPUS_XLSX,
        xlsx(listOf(Full("Resum per soci", listOf(30, 16, 32, 14, 12, 13, 12), resum), Full("Detall", listOf(12, 30, 32, 24, 10, 10, 11), detall), Full("Per producte", listOf(32, 10, 12), prod))),
    )
}

/* ======================= Fitxer per al banc (provisional) ======================= */

fun csvRemesa(estat: Estat, mes: String): Fitxer {
    val files = listOf(listOf("referencia_banc", "nom", "import", "concepte")) +
        estat.informes(mes).filter { it.total > 0 }.map {
            listOf(it.soci.refBanc, it.soci.nomComplet, "%d.%02d".format(it.total / 100, it.total % 100), "Consum GastroBacus $mes")
        }
    val csv = files.joinToString("\r\n") { f -> f.joinToString(";") { "\"" + it.replace("\"", "\"\"") + "\"" } }
    return Fitxer("GastroBacus-remesa-$mes.csv", TIPUS_CSV, ("﻿" + csv).toByteArray())
}

/* ======================= Compartir ======================= */

fun compartir(context: Context, f: Fitxer) {
    val dir = File(context.cacheDir, "exportacions").apply { mkdirs() }
    val fitxer = File(dir, f.nom).apply { writeBytes(f.dades) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fitxers", fitxer)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = f.tipus
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, f.nom)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    cat.gastrobacus.consum.Quiosc.desfixa()
    context.startActivity(Intent.createChooser(intent, "Compartir ${f.nom}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
