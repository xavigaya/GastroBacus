package cat.gastrobacus.consum.ui.gestio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.Quiosc
import cat.gastrobacus.consum.dades.Csv
import cat.gastrobacus.consum.dades.EstatBizum
import cat.gastrobacus.consum.dades.EstatPeticio
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.FilaProducte
import cat.gastrobacus.consum.dades.FilaSoci
import cat.gastrobacus.consum.dades.LecturaCsv
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.TipusPeticio
import cat.gastrobacus.consum.dades.dataHora
import cat.gastrobacus.consum.dades.diaCurt
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.exportar.Fitxer
import cat.gastrobacus.consum.exportar.TIPUS_CSV
import cat.gastrobacus.consum.ui.AppModel
import cat.gastrobacus.consum.ui.Avis
import cat.gastrobacus.consum.ui.Bloc
import cat.gastrobacus.consum.ui.Boto
import cat.gastrobacus.consum.ui.C
import cat.gastrobacus.consum.ui.Camp
import cat.gastrobacus.consum.ui.Cap
import cat.gastrobacus.consum.ui.Cel
import cat.gastrobacus.consum.ui.Etiqueta
import cat.gastrobacus.consum.ui.FilaTaula
import cat.gastrobacus.consum.ui.Subtitol
import cat.gastrobacus.consum.ui.Suau
import cat.gastrobacus.consum.ui.Taula
import cat.gastrobacus.consum.ui.Tipus
import cat.gastrobacus.consum.ui.ToAvis
import cat.gastrobacus.consum.ui.ToEtiqueta

/* ---------------- Peticions dels socis i comanda al proveïdor ---------------- */

@Composable
fun GestioComandes(model: AppModel, estat: Estat) {
    val pendents = estat.peticionsPendents
    // Proposta de comanda: el que està per sota del mínim i el que els socis diuen que falta
    val falten = pendents.filter { it.tipus == TipusPeticio.FALTA }.mapNotNull { it.producteId }.toSet()
    val candidats = estat.productes.filter { it.actiu && it.controlaEstoc }.sortedWith(compareBy({ !(it.calReposar || it.id in falten) }, { it.nom }))
    val quantitats = remember {
        mutableStateMapOf<String, String>().apply {
            candidats.filter { it.calReposar || it.id in falten }.forEach { p -> put(p.id, maxOf(p.estocMinim * 2 - p.estoc, p.estocMinim, 6).toString()) }
        }
    }
    var per by remember { mutableStateOf(estat.config.correuProveidor) }
    var notes by remember { mutableStateOf("") }
    var nomesProposta by remember { mutableStateOf(true) }
    val visibles = if (nomesProposta) candidats.filter { it.calReposar || it.id in falten || (quantitats[it.id]?.toIntOrNull() ?: 0) > 0 } else candidats
    val pesos = listOf(3f, 1f, 1f, 1.6f)

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.weight(1.6f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Subtitol("Comanda al proveïdor", Modifier.weight(1f))
                cat.gastrobacus.consum.ui.Casella("Només la proposta", nomesProposta, { nomesProposta = it })
            }
            Suau("Ja hi ha proposats els productes per sota del mínim i els que els socis han dit que falten. Ajusta les quantitats.", mida = 14.sp)
            Taula(Modifier.weight(1f)) {
                FilaTaula(listOf({ Cap("Producte") }, { Cap("Queden", true) }, { Cap("Mínim", true) }, { Cap("Demanar") }), pesos, capcalera = true)
                LazyColumn {
                    items(visibles, key = { it.id }) { p ->
                        FilaTaula(
                            listOf(
                                {
                                    Column {
                                        Cel(p.nom)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (p.calReposar) Etiqueta("Cal reposar", ToEtiqueta.AMBRE)
                                            if (p.id in falten) Etiqueta("Un soci diu que falta", ToEtiqueta.MOST)
                                        }
                                    }
                                },
                                { Cel(p.estoc.toString(), dreta = true) },
                                { Cel(p.estocMinim.toString(), dreta = true) },
                                { Camp("", quantitats[p.id] ?: "", { t -> if (t.length <= 4 && t.all(Char::isDigit)) quantitats[p.id] = t }, Modifier.width(110.dp), placeholder = "0", teclat = KeyboardType.Number) },
                            ),
                            pesos,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Camp("Correu del proveïdor", per, { per = it }, Modifier.weight(1f), teclat = KeyboardType.Email)
                Camp("Notes", notes, { notes = it }, Modifier.weight(1f), placeholder = "Dia d'entrega, contacte…")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f))
                Boto("Enviar la comanda", {
                    val linies = estat.productes.mapNotNull { p -> quantitats[p.id]?.toIntOrNull()?.takeIf { it > 0 }?.let { p to it } }
                    model.accio("Comanda enviada") {
                        model.repo.preparaComanda(linies, notes, per.trim())
                        model.serveis.enviaPendents()
                        // Les peticions de productes que falten queden resoltes
                        estat.peticionsPendents.filter { it.tipus == TipusPeticio.FALTA && (quantitats[it.producteId ?: ""]?.toIntOrNull() ?: 0) > 0 }
                            .forEach { model.repo.resolPeticio(it, EstatPeticio.FETA) }
                    }
                }, icona = Icons.Default.Send, enabled = per.contains("@") && quantitats.values.any { (it.toIntOrNull() ?: 0) > 0 } && estat.config.compteGoogle != null)
            }
            if (estat.config.compteGoogle == null) Suau("Cal connectar el compte de Google per enviar la comanda.", mida = 14.sp)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Subtitol("Peticions dels socis")
            if (pendents.isEmpty()) Suau("Cap petició pendent.")
            pendents.forEach { p ->
                Bloc(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Etiqueta(p.tipus.nom, if (p.tipus == TipusPeticio.FALTA) ToEtiqueta.AMBRE else ToEtiqueta.MOST)
                        Text(p.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                    Suau("${estat.soci(p.sociId)?.nomComplet ?: ""}, ${diaCurt(p.ts)}", mida = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Boto(if (p.tipus == TipusPeticio.FALTA) "Ja està" else "L'afegirem", { model.accio("Petició resolta") { model.repo.resolPeticio(p, EstatPeticio.FETA) } }, tipus = Tipus.FANTASMA, petit = true)
                        Boto("Descartar", { model.accio("Petició descartada") { model.repo.resolPeticio(p, EstatPeticio.DESCARTADA) } }, tipus = Tipus.FANTASMA, petit = true)
                    }
                }
            }
            val resoltes = estat.peticions.filter { it.estat != EstatPeticio.PENDENT }.take(10)
            if (resoltes.isNotEmpty()) {
                Subtitol("Resoltes")
                resoltes.forEach { Text("${diaCurt(it.ts)}: ${it.tipus.nom.lowercase()} ${it.text} (${if (it.estat == EstatPeticio.FETA) "fet" else "descartat"})", style = MaterialTheme.typography.bodyMedium, color = C.roure2) }
            }
        }
    }
}

/* ---------------- Bizums ---------------- */

@Composable
fun GestioBizums(model: AppModel, estat: Estat, junta: Soci) {
    val pendents = estat.bizumsPendents
    val altres = estat.bizums.filter { it.estat != EstatBizum.PENDENT }.take(40)
    val pesos = listOf(1.5f, 2.6f, 2.6f, 1.1f, 2.6f)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (estat.config.telefonBizum.isBlank()) Avis("Encara no hi ha cap telèfon de Bizum a Configuració: els socis no veuran l'opció de pagar per Bizum.")
        Subtitol("Bizums per comprovar")
        Suau("Si un Bizum no arriba, les consumicions passen al compte del soci.")
        if (pendents.isEmpty()) Suau("Cap Bizum pendent.")
        else Taula {
            FilaTaula(listOf({ Cap("Quan") }, { Cap("Qui") }, { Cap("Concepte") }, { Cap("Import", true) }, { Cap("") }), pesos, capcalera = true)
            pendents.forEach { b ->
                FilaTaula(
                    listOf(
                        { Cel(dataHora(b.ts)) },
                        { Cel(b.nom + if (b.sociId == null) " (de fora)" else "") },
                        { Cel(b.concepte) },
                        { Cel(euros(b.import), dreta = true, negreta = true) },
                        {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Boto("Ha arribat", { model.accio("Bizum confirmat") { model.repo.confirmaBizum(b.id, junta.id, true) } }, tipus = Tipus.VERD, petit = true)
                                Boto("No ha arribat", { model.accio(if (b.sociId != null) "Passa a la factura del mes" else "Marcat com a no pagat") { model.repo.confirmaBizum(b.id, junta.id, false) } }, tipus = Tipus.FANTASMA, petit = true)
                            }
                        },
                    ),
                    pesos,
                )
            }
            FilaTaula(listOf({ Cel("Total", negreta = true) }, {}, {}, { Cel(euros(pendents.sumOf { it.import }), dreta = true, negreta = true) }, {}), pesos, capcalera = true)
        }
        if (altres.isNotEmpty()) {
            Subtitol("Darrers Bizums comprovats")
            Taula {
                altres.forEach { b ->
                    FilaTaula(
                        listOf(
                            { Cel(dataHora(b.ts)) }, { Cel(b.nom) }, { Cel(b.concepte) }, { Cel(euros(b.import), dreta = true) },
                            {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Etiqueta(if (b.estat == EstatBizum.REBUT) "Rebut" else "No ha arribat", if (b.estat == EstatBizum.REBUT) ToEtiqueta.VERD else ToEtiqueta.VERMELL)
                                    if (b.estat == EstatBizum.REBUT) Boto("Desfer", { model.accio("Torna a estar pendent") { model.repo.reobreBizum(b.id) } }, tipus = Tipus.FANTASMA, petit = true)
                                }
                            },
                        ),
                        pesos,
                    )
                }
            }
        }
    }
}

/* ---------------- Importar CSV ---------------- */

private fun llegeixText(bytes: ByteArray): String {
    // Els CSV d'Excel solen anar en Windows-1252; si no és UTF-8 vàlid, es llegeix així
    val dec = Charsets.UTF_8.newDecoder()
    return try { dec.decode(java.nio.ByteBuffer.wrap(bytes)).toString() } catch (e: java.nio.charset.CharacterCodingException) { String(bytes, charset("windows-1252")) }
}

@Composable
fun ImportarCsv(model: AppModel, estat: Estat, perSocis: Boolean, onTanca: () -> Unit) {
    val context = LocalContext.current
    var lecturaP by remember { mutableStateOf<LecturaCsv<FilaProducte>?>(null) }
    var lecturaS by remember { mutableStateOf<LecturaCsv<FilaSoci>?>(null) }
    var nomFitxer by remember { mutableStateOf<String?>(null) }
    var plantilla by remember { mutableStateOf<Fitxer?>(null) }
    val obrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val text = llegeixText(context.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
            nomFitxer = uri.lastPathSegment?.substringAfterLast('/') ?: "fitxer"
            if (perSocis) lecturaS = Csv.llegeixSocis(text) else lecturaP = Csv.llegeixProductes(text)
        }.onFailure { model.avisa("No s'ha pogut llegir el fitxer: ${it.message}") }
    }
    val columnes = if (perSocis) Csv.COLUMNES_SOCIS else Csv.COLUMNES_PRODUCTES
    val files = if (perSocis) lecturaS?.files?.size else lecturaP?.files?.size
    val errors = (if (perSocis) lecturaS?.errors else lecturaP?.errors).orEmpty()

    AlertDialog(
        onDismissRequest = onTanca,
        title = { Text(if (perSocis) "Importar socis" else "Importar productes i estoc", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Fitxer CSV separat per punt i coma (com el que desa l'Excel). La primera fila ha de tenir aquestes columnes:", style = MaterialTheme.typography.bodyMedium)
                columnes.forEach { (c, d) ->
                    Row { Text(c, Modifier.width(130.dp), style = MaterialTheme.typography.labelLarge); Text(d, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium) }
                }
                Suau(if (perSocis) "Els socis que ja existeixen (mateix correu o nom) s'actualitzen." else "Els productes que ja existeixen (mateix nom) s'actualitzen.", mida = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Boto("Triar el fitxer", { Quiosc.desfixa(); obrir.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) })
                    Boto("Enviar la plantilla per correu", {
                        plantilla = Fitxer(if (perSocis) "plantilla-socis.csv" else "plantilla-productes.csv", TIPUS_CSV, ("﻿" + if (perSocis) Csv.PLANTILLA_SOCIS else Csv.PLANTILLA_PRODUCTES).toByteArray())
                    }, tipus = Tipus.FANTASMA)
                }
                if (files != null) {
                    Box(Modifier.padding(top = 4.dp))
                    Avis("$nomFitxer: $files files a punt per importar" + if (errors.isNotEmpty()) ", ${errors.size} amb errors." else ".", if (errors.isEmpty()) ToAvis.OK else ToAvis.AVIS)
                    errors.take(8).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = C.error) }
                    if (errors.size > 8) Suau("…i ${errors.size - 8} més.", mida = 13.sp)
                }
            }
        },
        confirmButton = {
            Boto("Importar", {
                model.accioAmb({
                    if (perSocis) model.repo.importaSocis(lecturaS!!.files) else model.repo.importaProductes(lecturaP!!.files)
                }) { r ->
                    model.avisa("Importats: ${r.nous} nous i ${r.actualitzats} actualitzats" + if (r.errors.isNotEmpty()) ". ${r.errors.joinToString()}" else "")
                    onTanca()
                }
            }, enabled = (files ?: 0) > 0)
        },
        dismissButton = { Boto("Tancar", onTanca, tipus = Tipus.FANTASMA) },
        containerColor = C.superficie,
    )
    plantilla?.let { f -> EnviarFitxer(model, estat, f, "${estat.config.nomSocietat}: ${f.nom}") { plantilla = null } }
}
