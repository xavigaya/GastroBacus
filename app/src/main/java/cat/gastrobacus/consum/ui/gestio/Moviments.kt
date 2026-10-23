package cat.gastrobacus.consum.ui.gestio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.EstatApat
import cat.gastrobacus.consum.dades.EstatMes
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.ItemApunt
import cat.gastrobacus.consum.dades.Origen
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.avui
import cat.gastrobacus.consum.dades.dataPlena
import cat.gastrobacus.consum.dades.diaCurt
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.dades.hora
import cat.gastrobacus.consum.dades.mesActual
import cat.gastrobacus.consum.dades.mesAnterior
import cat.gastrobacus.consum.dades.mesDe
import cat.gastrobacus.consum.dades.mesDeData
import cat.gastrobacus.consum.dades.nomMes
import cat.gastrobacus.consum.dades.tsDe
import cat.gastrobacus.consum.exportar.Fitxer
import cat.gastrobacus.consum.exportar.compartir
import cat.gastrobacus.consum.exportar.csvRemesa
import cat.gastrobacus.consum.exportar.excelJunta
import cat.gastrobacus.consum.exportar.pdfJunta
import cat.gastrobacus.consum.ui.AppModel
import cat.gastrobacus.consum.ui.Avis
import cat.gastrobacus.consum.ui.Bloc
import cat.gastrobacus.consum.ui.Boto
import cat.gastrobacus.consum.ui.C
import cat.gastrobacus.consum.ui.Camp
import cat.gastrobacus.consum.ui.CampData
import cat.gastrobacus.consum.ui.Cap
import cat.gastrobacus.consum.ui.Cel
import cat.gastrobacus.consum.ui.Confirmacio
import cat.gastrobacus.consum.ui.Desplegable
import cat.gastrobacus.consum.ui.Etiqueta
import cat.gastrobacus.consum.ui.FilaTaula
import cat.gastrobacus.consum.ui.SelectorMes
import cat.gastrobacus.consum.ui.Subtitol
import cat.gastrobacus.consum.ui.Suau
import cat.gastrobacus.consum.ui.Taula
import cat.gastrobacus.consum.ui.Tipus
import cat.gastrobacus.consum.ui.ToAvis
import cat.gastrobacus.consum.ui.ToEtiqueta
import cat.gastrobacus.consum.ui.darrersMesos

/* ---------------- Consumicions ---------------- */

@Composable
fun GestioConsumicions(model: AppModel, estat: Estat, junta: Soci) {
    var mes by remember { mutableStateOf(mesActual()) }
    var filtre by remember { mutableStateOf<Soci?>(null) }
    var afegint by remember { mutableStateOf(false) }
    val tancat = estat.estatMes(mes) == EstatMes.TANCAT
    val llista = estat.consums.filter { mesDe(it.ts) == mes && (filtre == null || it.sociId == filtre!!.id) }.sortedByDescending { it.ts }
    val valides = llista.filter { !it.anulat }
    val pesos = listOf(1.4f, 2.4f, 3f, 1.1f, 1.2f, 1.3f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectorMes(mes, darrersMesos(), { mes = it })
            Desplegable(listOf<Soci?>(null) + estat.socisActius, filtre, { it?.nomComplet ?: "Tots els socis" }, { filtre = it }, Modifier.width(280.dp))
            Box(Modifier.weight(1f))
            Boto("Apuntar a un soci", { afegint = true }, icona = Icons.Default.Add, enabled = !tancat)
        }
        if (tancat) Avis("Aquest mes està tancat. Per corregir-hi alguna cosa, reobre'l des de Tancament de mes.")
        Suau("${valides.sumOf { it.quantitat }} consumicions, ${euros(valides.sumOf { it.import })}.")
        Taula(Modifier.weight(1f)) {
            FilaTaula(listOf({ Cap("Quan") }, { Cap("Soci") }, { Cap("Producte") }, { Cap("Des de") }, { Cap("Import", true) }, { Cap("") }), pesos, capcalera = true)
            LazyColumn {
                items(llista, key = { it.id }) { c ->
                    FilaTaula(
                        listOf(
                            { Cel("${diaCurt(c.ts)} ${hora(c.ts)}", ratllat = c.anulat) },
                            { Cel(estat.soci(c.sociId)?.nomComplet ?: "—", ratllat = c.anulat) },
                            { Cel((if (c.quantitat > 1) "${c.quantitat} × " else "") + c.nomProducte + (c.convidat?.let { " (convidat: $it)" } ?: ""), ratllat = c.anulat) },
                            { Cel(if (c.origen == Origen.JUNTA) "Junta" else "Tauleta", color = C.roure2) },
                            { Cel(euros(c.import), dreta = true, ratllat = c.anulat, color = if (c.anulat) C.roure2 else C.roure) },
                            {
                                if (c.anulat) Suau("Anul·lada", mida = 13.sp)
                                else if (!tancat) Boto("Anul·lar", { model.accio("Consumició anul·lada") { model.repo.anular(c.id, junta.id, true) } }, tipus = Tipus.FANTASMA, petit = true)
                            },
                        ),
                        pesos,
                    )
                }
            }
        }
    }
    if (afegint) ApuntarJunta(model, estat) { afegint = false }
}

@Composable
private fun ApuntarJunta(model: AppModel, estat: Estat, onTanca: () -> Unit) {
    var soci by remember { mutableStateOf(estat.socisActius.firstOrNull()) }
    val productes = estat.productes.filter { it.actiu }.sortedBy { it.nom }
    var producte by remember { mutableStateOf(productes.firstOrNull()) }
    var quantitat by remember { mutableStateOf("1") }
    var convidat by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(avui()) }
    AlertDialog(
        onDismissRequest = onTanca,
        title = { Text("Apuntar a un soci", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Suau("Per a consumicions que algú es va oblidar d'apuntar.")
                Desplegable(estat.socisActius, soci, { it?.nomComplet ?: "" }, { soci = it }, Modifier.fillMaxWidth(), "Soci")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Desplegable(productes, producte, { it?.let { p -> "${p.nom} (${euros(p.preu)})" } ?: "" }, { producte = it }, Modifier.weight(2f), "Producte")
                    Camp("Quantitat", quantitat, { if (it.length <= 3 && it.all(Char::isDigit)) quantitat = it }, Modifier.weight(1f), teclat = KeyboardType.Number)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampData("Dia", data, { data = it }, Modifier.weight(1f))
                    Camp("Convidat", convidat, { convidat = it }, Modifier.weight(1f), placeholder = "Buit si és per al soci")
                }
            }
        },
        confirmButton = {
            Boto("Apuntar", {
                val s = soci ?: return@Boto
                val p = producte ?: return@Boto
                model.accio("Consumició apuntada", onTanca) {
                    model.repo.apuntar(s.id, listOf(ItemApunt(p.id, quantitat.toIntOrNull() ?: 1, convidat.trim().ifBlank { null })), Origen.JUNTA, tsDe(data, 20))
                }
            }, enabled = soci != null && producte != null && (quantitat.toIntOrNull() ?: 0) > 0)
        },
        dismissButton = { Boto("Cancel·lar", onTanca, tipus = Tipus.FANTASMA) },
        containerColor = C.superficie,
    )
}

/* ---------------- Tancament de mes ---------------- */

@Composable
fun Tancament(model: AppModel, estat: Estat) {
    val context = LocalContext.current
    val mesos = darrersMesos()
    var mes by remember {
        mutableStateOf(mesos.drop(1).reversed().firstOrNull { m -> estat.estatMes(m) != EstatMes.TANCAT && estat.consums.any { mesDe(it.ts) == m } } ?: mesAnterior(mesActual()))
    }
    var confirmaEnviar by remember { mutableStateOf(false) }
    var confirmaTancar by remember { mutableStateOf(false) }
    var enviantFitxer by remember { mutableStateOf<Fitxer?>(null) }

    val estatMes = estat.estatMes(mes)
    val t = estat.tancament(mes)
    val informes = estat.informes(mes)
    val total = informes.sumOf { it.total }
    val oberts = estat.apats.filter { it.estat == EstatApat.OBERT && mesDeData(it.data) == mes }
    val limit = t?.let { it.informatTs + estat.config.diesReclamacio * 86_400_000L } ?: 0L
    val aFavor = informes.filter { it.total < 0 }
    val senseCorreu = informes.filter { it.soci.email.isBlank() }
    val pesos = listOf(3f, 1.2f, 1.2f, 1.1f, 1.3f, 1.2f)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SelectorMes(mes, mesos, { mes = it })
            Etiqueta(
                when (estatMes) { EstatMes.OBERT -> "Obert"; EstatMes.INFORMAT -> "Informes enviats, termini de reclamacions"; EstatMes.TANCAT -> "Tancat" },
                when (estatMes) { EstatMes.OBERT -> ToEtiqueta.NEUTRE; EstatMes.INFORMAT -> ToEtiqueta.AMBRE; EstatMes.TANCAT -> ToEtiqueta.VERD },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Pas(1, "Revisar", fet = estatMes != EstatMes.OBERT, actual = estatMes == EstatMes.OBERT, Modifier.weight(1f)) {
                Text("${informes.size} socis amb consum, total ${euros(total)}.", style = MaterialTheme.typography.bodyLarge)
                if (mes == mesActual() && estatMes == EstatMes.OBERT) Avis("El mes encara no s'ha acabat.")
                if (oberts.isNotEmpty()) Avis("Hi ha ${if (oberts.size == 1) "un àpat obert" else "${oberts.size} àpats oberts"} (${oberts.joinToString { it.titol }}). No sortiran a l'informe fins que no es passin als comptes.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Boto("Excel", { enviantFitxer = excelJunta(estat, mes) }, tipus = Tipus.FANTASMA, petit = true, enabled = informes.isNotEmpty(), icona = Icons.Default.Email)
                    Boto("PDF", { enviantFitxer = pdfJunta(context, estat, mes) }, tipus = Tipus.FANTASMA, petit = true, enabled = informes.isNotEmpty(), icona = Icons.Default.Email)
                }
            }
            Pas(2, "Enviar informes", fet = estatMes != EstatMes.OBERT, actual = estatMes == EstatMes.INFORMAT, Modifier.weight(1f)) {
                Text("Cada soci rep el seu detall per correu, amb el PDF, i tresoreria el resum. S'obren ${estat.config.diesReclamacio} dies per reclamar.", style = MaterialTheme.typography.bodyMedium)
                if (estat.config.modeProves) Suau("Mode de proves: tots els correus van a ${estat.config.correuProves}.", mida = 14.sp)
                if (senseCorreu.isNotEmpty()) Suau("Sense correu: ${senseCorreu.joinToString { it.soci.nomComplet }}.", mida = 14.sp)
                if (t != null) Suau("Enviats el ${dataPlena(t.informatTs)}. Reclamacions fins al ${dataPlena(limit)}.", mida = 14.sp)
                if (estatMes != EstatMes.TANCAT) Boto(
                    if (estatMes == EstatMes.INFORMAT) "Tornar a enviar" else "Enviar informes", { confirmaEnviar = true },
                    tipus = if (estatMes == EstatMes.INFORMAT) Tipus.FANTASMA else Tipus.PRINCIPAL, enabled = informes.isNotEmpty() && !model.ocupat,
                )
            }
            Pas(3, "Tancar i passar al banc", fet = estatMes == EstatMes.TANCAT, actual = estatMes == EstatMes.INFORMAT && System.currentTimeMillis() >= limit, Modifier.weight(1f)) {
                Text("Un cop tancat, el mes ja no es pot modificar i es genera el fitxer de rebuts per al banc.", style = MaterialTheme.typography.bodyMedium)
                if (estatMes == EstatMes.TANCAT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Boto("Fitxer per al banc", { enviantFitxer = csvRemesa(estat, mes) }, petit = true, icona = Icons.Default.Email)
                        Boto("Reobrir el mes", { model.accio("${nomMes(mes)} reobert") { model.repo.reobrirMes(mes) } }, tipus = Tipus.FANTASMA, petit = true)
                    }
                    if (aFavor.isNotEmpty()) Avis("${aFavor.joinToString { "${it.soci.nomComplet} (${euros(-it.total)})" }} ${if (aFavor.size > 1) "tenen" else "té"} saldo a favor per despeses avançades. No surt al fitxer del banc: cal tornar-li els diners a part.")
                    Suau("De moment és un CSV amb la referència i l'import de cada soci. El format definitiu de la remesa es farà quan sapiguem què demana el banc.", mida = 13.sp)
                } else Boto("Tancar el mes", { confirmaTancar = true }, tipus = Tipus.VERD, enabled = estatMes == EstatMes.INFORMAT)
            }
        }

        Subtitol("Per soci")
        Taula {
            FilaTaula(listOf({ Cap("Soci") }, { Cap("Ref. banc") }, { Cap("Consum", true) }, { Cap("Àpats", true) }, { Cap("Abonaments", true) }, { Cap("Total", true) }), pesos, capcalera = true)
            informes.forEach { i ->
                FilaTaula(
                    listOf(
                        { Cel(i.soci.nomComplet) }, { Cel(i.soci.refBanc) }, { Cel(euros(i.totalConsum), dreta = true) },
                        { Cel(if (i.totalApats != 0L) euros(i.totalApats) else "", dreta = true) },
                        { Cel(if (i.totalAbonaments != 0L) euros(i.totalAbonaments) else "", dreta = true, color = C.pampol) },
                        { Cel(euros(i.total), dreta = true, negreta = true) },
                    ),
                    pesos,
                )
            }
            FilaTaula(listOf({ Cel("Total", negreta = true) }, {}, {}, {}, {}, { Cel(euros(total), dreta = true, negreta = true) }), pesos, capcalera = true)
        }
    }

    if (confirmaEnviar) Confirmacio(
        "Enviar els informes de ${nomMes(mes).lowercase()}",
        "S'enviarà un correu a ${informes.count { it.soci.email.isNotBlank() }} socis i el resum a tresoreria" +
            (if (estat.config.modeProves) ". Mode de proves: tot anirà a ${estat.config.correuProves}." else "."),
        "Enviar",
        onConfirma = {
            confirmaEnviar = false
            model.accio("Informes enviats") {
                model.repo.prepararInformes(mes)
                model.serveis.enviaPendents()
            }
        },
        onCancela = { confirmaEnviar = false },
    )
    if (confirmaTancar) Confirmacio(
        "Tancar ${nomMes(mes).lowercase()}",
        (if (System.currentTimeMillis() < limit) "El termini de reclamacions acaba el ${dataPlena(limit)}. Pots tancar igualment si ja està tot revisat.\n\n" else "") +
            "Es carregaran ${euros(informes.filter { it.total > 0 }.sumOf { it.total })} a ${informes.count { it.total > 0 }} socis." +
            (if (aFavor.isNotEmpty()) " A ${aFavor.size} ${if (aFavor.size == 1) "soci" else "socis"} se'ls ha de tornar ${euros(-aFavor.sumOf { it.total })}." else "") +
            " Després no es podran canviar consumicions ni àpats d'aquest mes, tret que el reobris.",
        "Tancar el mes",
        onConfirma = {
            confirmaTancar = false
            model.accio("${nomMes(mes)} tancat") {
                model.repo.tancarMes(mes)
                runCatching { model.serveis.ferCopia(interactiu = false) }
            }
        },
        onCancela = { confirmaTancar = false },
    )
    enviantFitxer?.let { f -> EnviarFitxer(model, estat, f, "${estat.config.nomSocietat}: ${f.nom}") { enviantFitxer = null } }
}

@Composable
private fun Pas(n: Int, titol: String, fet: Boolean, actual: Boolean, modifier: Modifier, contingut: @Composable () -> Unit) {
    Bloc(modifier, vora = if (actual) C.most else if (fet) C.pampol else C.vora) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            cat.gastrobacus.consum.ui.Avatar(if (fet && !actual) "✓" else n.toString(), 34.dp, if (actual) C.most else if (fet) C.pampol else C.pedra, if (actual || fet) C.sobreMost else C.roure)
            Text(titol, style = MaterialTheme.typography.titleLarge)
        }
        contingut()
    }
}

/** Envia un fitxer per correu des del Gmail de la tauleta, o el comparteix amb una altra app. */
@Composable
fun EnviarFitxer(model: AppModel, estat: Estat, f: Fitxer, assumpte: String, onTanca: () -> Unit) {
    val context = LocalContext.current
    var per by remember { mutableStateOf(estat.config.correuTresoreria.ifBlank { estat.config.compteGoogle ?: "" }) }
    AlertDialog(
        onDismissRequest = onTanca,
        title = { Text(f.nom, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Camp("Enviar per correu a", per, { per = it }, teclat = KeyboardType.Email)
                if (estat.config.modeProves) Suau("Mode de proves: s'enviarà a ${estat.config.correuProves}.", mida = 14.sp)
                if (estat.config.compteGoogle == null) Avis("Cal connectar el compte de Google a Configuració per enviar correus.", ToAvis.AVIS)
            }
        },
        confirmButton = {
            Boto("Enviar", { model.accio("Fitxer enviat", onTanca) { model.serveis.enviaFitxer(per.trim(), assumpte, "Adjunt: ${f.nom}\n\nEnviat des de la tauleta de ${estat.config.nomSocietat}.", f) } },
                enabled = per.contains("@") && estat.config.compteGoogle != null, icona = Icons.Default.Email)
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Boto("Compartir", { compartir(context, f); onTanca() }, tipus = Tipus.FANTASMA, icona = Icons.Default.Share)
                Boto("Cancel·lar", onTanca, tipus = Tipus.FANTASMA)
            }
        },
        containerColor = C.superficie,
    )
}

