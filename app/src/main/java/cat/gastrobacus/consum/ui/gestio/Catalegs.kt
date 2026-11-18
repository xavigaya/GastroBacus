package cat.gastrobacus.consum.ui.gestio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.Categoria
import cat.gastrobacus.consum.dades.Csv
import cat.gastrobacus.consum.exportar.Fitxer
import cat.gastrobacus.consum.exportar.TIPUS_CSV
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.Producte
import cat.gastrobacus.consum.dades.Rol
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.TipusReposicio
import cat.gastrobacus.consum.dades.avui
import cat.gastrobacus.consum.dades.dataLlarga
import cat.gastrobacus.consum.dades.diaCurt
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.dades.llegirEuros
import cat.gastrobacus.consum.dades.nomMes
import cat.gastrobacus.consum.dades.nouId
import cat.gastrobacus.consum.ui.AppModel
import cat.gastrobacus.consum.ui.Avis
import cat.gastrobacus.consum.ui.Boto
import cat.gastrobacus.consum.ui.C
import cat.gastrobacus.consum.ui.Camp
import cat.gastrobacus.consum.ui.CampData
import cat.gastrobacus.consum.ui.Cap
import cat.gastrobacus.consum.ui.Casella
import cat.gastrobacus.consum.ui.Cel
import cat.gastrobacus.consum.ui.Desplegable
import cat.gastrobacus.consum.ui.Etiqueta
import cat.gastrobacus.consum.ui.FilaTaula
import cat.gastrobacus.consum.ui.Interruptor
import cat.gastrobacus.consum.ui.Pindola
import cat.gastrobacus.consum.ui.Suau
import cat.gastrobacus.consum.ui.Subtitol
import cat.gastrobacus.consum.ui.Taula
import cat.gastrobacus.consum.ui.Tipus
import cat.gastrobacus.consum.ui.ToEtiqueta
import cat.gastrobacus.consum.ui.colorCategoria

private fun textEuros(c: Long) = euros(c).removeSuffix(" €")

/* ---------------- Productes ---------------- */

@Composable
fun GestioProductes(model: AppModel, estat: Estat) {
    var editant by remember { mutableStateOf<Producte?>(null) }
    var cerca by remember { mutableStateOf("") }
    var important by remember { mutableStateOf(false) }
    var exportant by remember { mutableStateOf<Fitxer?>(null) }
    val llista = estat.productes.filter { it.nom.contains(cerca, ignoreCase = true) }.sortedWith(compareBy({ it.categoria.ordinal }, { it.nom }))
    val pesos = listOf(3f, 1.6f, 1.1f, 1.1f, 1f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Camp("Cerca", cerca, { cerca = it }, Modifier.width(320.dp), placeholder = "Nom del producte")
            Box(Modifier.weight(1f))
            Boto("Exportar", { exportant = Fitxer("GastroBacus-productes-${avui()}.csv", TIPUS_CSV, ("\uFEFF" + Csv.exportaProductes(estat.productes)).toByteArray()) }, tipus = Tipus.FANTASMA, icona = Icons.Default.FileDownload)
            Boto("Importar", { important = true }, tipus = Tipus.FANTASMA, icona = Icons.Default.FileUpload)
            Boto("Nou producte", { editant = Producte(nouId(), "", Categoria.CERVESA, 100) }, icona = Icons.Default.Add)
        }
        Taula(Modifier.weight(1f)) {
            FilaTaula(listOf({ Cap("Producte") }, { Cap("Categoria") }, { Cap("Soci", true) }, { Cap("Convidat", true) }, { Cap("Estoc", true) }, { Cap("") }), pesos, capcalera = true)
            LazyColumn {
                items(llista, key = { it.id }) { p ->
                    Box(Modifier.alpha(if (p.actiu) 1f else .5f)) {
                        FilaTaula(
                            listOf(
                                {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (p.favorit) Icon(Icons.Default.Star, "Favorit", Modifier.size(18.dp), tint = C.ambre)
                                        Cel(p.nom, negreta = true); if (!p.actiu) Etiqueta("Amagat")
                                    }
                                },
                                { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(10.dp).clip(CircleShape).background(colorCategoria(p.categoria))); Cel(p.categoria.nom) } },
                                { Cel(euros(p.preu), dreta = true) },
                                { Cel(p.preuConvidat?.let { euros(it) } ?: "igual", dreta = true, color = if (p.preuConvidat == null) C.roure2 else C.roure) },
                                { Cel(if (p.controlaEstoc) p.estoc.toString() else "—", dreta = true) },
                                { Boto("Editar", { editant = p }, tipus = Tipus.FANTASMA, petit = true) },
                            ),
                            pesos,
                        )
                    }
                }
            }
        }
    }
    editant?.let { EditorProducte(model, estat, it) { editant = null } }
    if (important) ImportarCsv(model, estat, perSocis = false) { important = false }
    exportant?.let { f -> EnviarFitxer(model, estat, f, "${estat.config.nomSocietat}: ${f.nom}") { exportant = null } }
}

@Composable
private fun EditorProducte(model: AppModel, estat: Estat, inicial: Producte, onTanca: () -> Unit) {
    var p by remember { mutableStateOf(inicial) }
    var preu by remember { mutableStateOf(textEuros(inicial.preu)) }
    var preuConv by remember { mutableStateOf(inicial.preuConvidat?.let { textEuros(it) } ?: "") }
    var ambPreuConv by remember { mutableStateOf(inicial.preuConvidat != null) }
    var estoc by remember { mutableStateOf(inicial.estoc.toString()) }
    var minim by remember { mutableStateOf(inicial.estocMinim.toString()) }
    var avisConsum by remember { mutableStateOf(if (inicial.avisConsum > 0) inicial.avisConsum.toString() else "") }
    val existeix = estat.productes.any { it.id == p.id }
    val preuValid = llegirEuros(preu) != null && (!ambPreuConv || llegirEuros(preuConv) != null)

    AlertDialog(
        onDismissRequest = onTanca,
        title = { Text(if (existeix) "Editar producte" else "Nou producte", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Camp("Nom", p.nom, { p = p.copy(nom = it) })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Desplegable(Categoria.entries, p.categoria, { it.nom }, { p = p.copy(categoria = it) }, Modifier.weight(1f), "Categoria")
                    Camp("Preu per al soci (€)", preu, { preu = it }, Modifier.weight(1f), teclat = KeyboardType.Decimal)
                }
                Casella("Els convidats paguen un preu diferent", ambPreuConv, { ambPreuConv = it })
                if (ambPreuConv) Camp("Preu per a convidats (€)", preuConv, { preuConv = it }, teclat = KeyboardType.Decimal)
                Casella("Favorit per a tothom (surt sempre a «Favorits»)", p.favorit, { p = p.copy(favorit = it) })
                Casella("Portar el control d'estoc", p.controlaEstoc, { p = p.copy(controlaEstoc = it) })
                if (p.controlaEstoc) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Camp("Unitats a la societat", estoc, { if (it.all(Char::isDigit)) estoc = it }, Modifier.weight(1f), teclat = KeyboardType.Number,
                        ajuda = if (existeix) "Per a comandes, millor des d'Estoc." else "Inventari inicial.")
                    Camp("Avisa quan en quedin", minim, { if (it.all(Char::isDigit)) minim = it }, Modifier.weight(1f), teclat = KeyboardType.Number)
                }
                Camp(
                    "Avisa quan se n'hagin consumit", avisConsum, { if (it.all(Char::isDigit)) avisConsum = it }, teclat = KeyboardType.Number,
                    placeholder = "Buit: sense avís", ajuda = "Unitats des de la darrera comanda. Ara en porta ${p.comptador}.",
                )
                Casella("Visible per apuntar", p.actiu, { p = p.copy(actiu = it) })
            }
        },
        confirmButton = {
            Boto("Desar", {
                val nou = p.copy(
                    preu = llegirEuros(preu) ?: 0, preuConvidat = if (ambPreuConv) llegirEuros(preuConv) else null,
                    estoc = estoc.toIntOrNull() ?: 0, estocMinim = minim.toIntOrNull() ?: 0, avisConsum = avisConsum.toIntOrNull() ?: 0,
                )
                model.accio("Producte desat", onTanca) { model.repo.desaProducte(nou) }
            }, enabled = p.nom.isNotBlank() && preuValid)
        },
        dismissButton = { Boto("Cancel·lar", onTanca, tipus = Tipus.FANTASMA) },
        containerColor = C.superficie,
    )
}

/* ---------------- Estoc ---------------- */

@Composable
fun GestioEstoc(model: AppModel, estat: Estat, soci: Soci) {
    var nomesBaix by remember { mutableStateOf(false) }
    var comanda by remember { mutableStateOf<Producte?>(null) }
    val llista = estat.productes.filter { it.actiu && (it.controlaEstoc || it.avisConsum > 0) && (!nomesBaix || it.calReposar) }.sortedBy { it.estoc - it.estocMinim }
    val pesos = listOf(3f, 0.9f, 0.9f, 1.3f, 1.3f, 1.5f)
    // En obrir l'estoc, els avisos es donen per vistos
    LaunchedEffect(Unit) { if (estat.avisosNous.isNotEmpty()) model.repo.marcaAvisosLlegits() }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.weight(2.2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Suau("Quan arribi una comanda, prem «Comanda»: pots sumar les unitats que entren o fer un recompte. Totes dues posen a zero el comptador de consum.", Modifier.weight(1f))
                Casella("Només el que cal reposar", nomesBaix, { nomesBaix = it })
            }
            Taula(Modifier.weight(1f)) {
                FilaTaula(listOf({ Cap("Producte") }, { Cap("Queden", true) }, { Cap("Mínim", true) }, { Cap("Consumits", true) }, { Cap("Darrera comanda") }, { Cap("") }), pesos, capcalera = true)
                LazyColumn {
                    items(llista, key = { it.id }) { p ->
                        FilaTaula(
                            listOf(
                                {
                                    Column {
                                        Cel(p.nom)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (p.controlaEstoc && p.estoc <= 0) Etiqueta("Esgotat", ToEtiqueta.VERMELL) else if (p.calReposar) Etiqueta("Cal reposar", ToEtiqueta.AMBRE)
                                            if (p.avisConsum in 1..p.comptador) Etiqueta("${p.comptador} consumits", ToEtiqueta.MOST)
                                        }
                                    }
                                },
                                { Cel(if (p.controlaEstoc) p.estoc.toString() else "—", dreta = true, negreta = true) },
                                { Cel(if (p.controlaEstoc) p.estocMinim.toString() else "—", dreta = true) },
                                { Cel(p.comptador.toString() + if (p.avisConsum > 0) " / ${p.avisConsum}" else "", dreta = true) },
                                { Cel(p.darreraComandaTs?.let { diaCurt(it) } ?: "—", color = C.roure2) },
                                { Boto("Comanda", { comanda = p }, petit = true) },
                            ),
                            pesos,
                        )
                    }
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Subtitol("Avisos")
            if (estat.avisos.isEmpty()) Suau("Cap avís.")
            estat.avisos.take(12).forEach { a ->
                Text("${diaCurt(a.ts)}: ${a.missatge}", style = MaterialTheme.typography.bodyMedium, color = if (a.llegit) C.roure2 else C.roure)
            }
            Subtitol("Darreres comandes")
            if (estat.reposicions.isEmpty()) Suau("Encara no s'ha registrat cap comanda.")
            estat.reposicions.take(20).forEach { r ->
                val nom = estat.producte(r.producteId)?.nom ?: ""
                Text(
                    "${diaCurt(r.ts)}: " + (if (r.tipus == TipusReposicio.ENTRADA) "+${r.quantitat} $nom" else "$nom, recompte ${r.quantitat} (n'hi havia ${r.estocAnterior})") +
                        (if (r.consumitPeriode > 0) ". Consumits des de l'anterior: ${r.consumitPeriode}" else "") + " (${estat.soci(r.perId)?.nom ?: ""})",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    comanda?.let { p -> DialegComanda(model, p, soci) { comanda = null } }
}

@Composable
private fun DialegComanda(model: AppModel, p: Producte, soci: Soci, onTanca: () -> Unit) {
    var tipus by remember { mutableStateOf(TipusReposicio.ENTRADA) }
    var q by remember { mutableStateOf("") }
    val n = q.toIntOrNull()
    AlertDialog(
        onDismissRequest = onTanca,
        title = { Text("Comanda: ${p.nom}", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Interruptor(listOf("Sumar el que entra", "Recompte"), tipus.ordinal, { tipus = TipusReposicio.entries[it] })
                Camp(
                    if (tipus == TipusReposicio.ENTRADA) "Unitats que han arribat" else "Unitats que hi ha ara (comptades)", q,
                    { if (it.length <= 4 && it.all(Char::isDigit)) q = it }, teclat = KeyboardType.Number,
                )
                Text(
                    "Ara n'hi ha ${p.estoc} segons l'app" + (if (n != null) ". Després en quedaran ${if (tipus == TipusReposicio.ENTRADA) p.estoc + n else n}." else "."),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Suau("El comptador de consum (${p.comptador} des de la darrera comanda) tornarà a zero.")
            }
        },
        confirmButton = {
            Boto("Registrar la comanda", { model.accio("Comanda de ${p.nom} registrada", onTanca) { model.repo.registraComanda(p.id, n ?: 0, tipus, soci.id) } },
                enabled = n != null && (tipus == TipusReposicio.RECOMPTE || n > 0))
        },
        dismissButton = { Boto("Cancel·lar", onTanca, tipus = Tipus.FANTASMA) },
        containerColor = C.superficie,
    )
}

/* ---------------- Socis ---------------- */

@Composable
fun GestioSocis(model: AppModel, estat: Estat) {
    var editant by remember { mutableStateOf<Soci?>(null) }
    var baixa by remember { mutableStateOf<Soci?>(null) }
    var alta by remember { mutableStateOf<Soci?>(null) }
    var cerca by remember { mutableStateOf("") }
    var filtre by remember { mutableStateOf(0) } // 0 actius, 1 de baixa, 2 tots
    var important by remember { mutableStateOf(false) }
    var exportant by remember { mutableStateOf<Fitxer?>(null) }
    val llista = estat.socis
        .filter { (it.nomComplet + " " + it.email).contains(cerca, ignoreCase = true) && (filtre == 2 || it.actiu == (filtre == 0)) }
        .sortedWith(compareBy(java.text.Collator.getInstance(cat.gastrobacus.consum.dades.CATALA)) { it.nom })
    val pesos = listOf(3f, 2.8f, 1.1f, 1.3f, 1f, 2.4f)
    val seguentRef = "GB-%04d".format(estat.socis.size + 1)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Camp("Cerca", cerca, { cerca = it }, Modifier.width(240.dp), placeholder = "Nom o correu")
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pindola("Actius ${estat.socis.count { it.actiu }}", filtre == 0, { filtre = 0 })
                Pindola("De baixa ${estat.socis.count { !it.actiu }}", filtre == 1, { filtre = 1 })
                Pindola("Tots", filtre == 2, { filtre = 2 })
            }
            Boto("Exportar", { exportant = Fitxer("GastroBacus-socis-${avui()}.csv", TIPUS_CSV, ("\uFEFF" + Csv.exportaSocis(estat.socis)).toByteArray()) }, tipus = Tipus.FANTASMA, icona = Icons.Default.FileDownload)
            Boto("Importar", { important = true }, tipus = Tipus.FANTASMA, icona = Icons.Default.FileUpload)
            Boto("Donar d'alta", { editant = Soci(nouId(), "", "", "", Rol.SOCI, "", "", seguentRef) }, icona = Icons.Default.Add)
        }
        Taula(Modifier.weight(1f)) {
            FilaTaula(listOf({ Cap("Nom") }, { Cap("Correu") }, { Cap("Ref. banc") }, { Cap("Alta") }, { Cap("Rol") }, { Cap("") }), pesos, capcalera = true)
            LazyColumn {
                items(llista, key = { it.id }) { s ->
                    FilaTaula(
                        listOf(
                            { Column { Cel(s.nomComplet, negreta = true); s.dataBaixa?.let { Suau("Baixa: ${dataCurta(it)}", mida = 13.sp) } } },
                            { Cel(s.email.ifBlank { "—" }) },
                            { Cel(s.refBanc) },
                            { Cel(s.dataAlta?.let { dataCurta(it) } ?: "—", color = C.roure2) },
                            { if (s.esJunta) Etiqueta("Junta", ToEtiqueta.MOST) else Cel("Soci") },
                            {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Boto("Editar", { editant = s }, tipus = Tipus.FANTASMA, petit = true)
                                    if (s.actiu) Boto("Baixa", { baixa = s }, tipus = Tipus.FANTASMA, petit = true)
                                    else Boto("Alta", { alta = s }, tipus = Tipus.SECUNDARI, petit = true)
                                }
                            },
                        ),
                        pesos,
                    )
                }
            }
        }
    }
    editant?.let { EditorSoci(model, estat, it) { editant = null } }
    baixa?.let { DialegBaixa(model, it) { baixa = null } }
    alta?.let { s ->
        var data by remember { mutableStateOf(avui()) }
        AlertDialog(
            onDismissRequest = { alta = null },
            title = { Text("Tornar a donar d'alta", style = MaterialTheme.typography.headlineMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${s.nomComplet} tornarà a ser soci. Conserva el PIN i tot l'historial.", style = MaterialTheme.typography.bodyLarge)
                    CampData("Data d'alta", data, { data = it })
                    if (s.periodes.isNotEmpty()) Suau("Períodes anteriors: " + s.periodes.joinToString { "${dataCurta(it.alta)} – ${it.baixa?.let(::dataCurta) ?: ""}" })
                }
            },
            confirmButton = { Boto("Donar d'alta", { model.accio("${s.nom} torna a ser soci") { model.repo.donaDAlta(s.id, data) }; alta = null }) },
            dismissButton = { Boto("Cancel·lar", { alta = null }, tipus = Tipus.FANTASMA) },
            containerColor = C.superficie,
        )
    }
    if (important) ImportarCsv(model, estat, perSocis = true) { important = false }
    exportant?.let { f -> EnviarFitxer(model, estat, f, "${estat.config.nomSocietat}: ${f.nom}") { exportant = null } }
}

private fun dataCurta(iso: String) = runCatching { java.time.LocalDate.parse(iso).let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) } }.getOrDefault(iso)

@Composable
private fun DialegBaixa(model: AppModel, s: Soci, onTanca: () -> Unit) {
    var data by remember { mutableStateOf(avui()) }
    var informe by remember { mutableStateOf(true) }
    var pendent by remember { mutableStateOf<Pair<List<String>, Long>?>(null) }
    LaunchedEffect(s.id) { pendent = model.repo.pendentDe(s.id) }
    AlertDialog(
        onDismissRequest = onTanca,
        title = { Text("Donar de baixa ${s.nomComplet}", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampData("Data de baixa", data, { data = it })
                when (val p = pendent) {
                    null -> Suau("Calculant el consum pendent…")
                    else -> if (p.first.isEmpty()) Avis("No té cap consum pendent de cobrar.", cat.gastrobacus.consum.ui.ToAvis.OK)
                    else {
                        Avis("Té ${euros(p.second)} pendents de ${p.first.joinToString { nomMes(it) }}. Es cobraran amb el tancament de cada mes.")
                        Casella("Enviar-li ara la liquidació per correu (i avisar tresoreria)", informe, { informe = it })
                    }
                }
                Suau("Deixarà de sortir a la tauleta. Es conserva l'historial i es pot tornar a donar d'alta.")
            }
        },
        confirmButton = {
            Boto("Donar de baixa", {
                model.accio("${s.nom} donat de baixa el ${dataLlarga(data).lowercase()}", onTanca) {
                    model.repo.donaDeBaixa(s.id, data, informe && (pendent?.first?.isNotEmpty() == true))
                    model.enviaEnSegonPla()
                }
            }, tipus = Tipus.PERILL, enabled = pendent != null)
        },
        dismissButton = { Boto("Cancel·lar", onTanca, tipus = Tipus.FANTASMA) },
        containerColor = C.superficie,
    )
}

@Composable
private fun EditorSoci(model: AppModel, estat: Estat, inicial: Soci, onTanca: () -> Unit) {
    var s by remember { mutableStateOf(inicial) }
    var pin by remember { mutableStateOf("") }
    var dataAlta by remember { mutableStateOf(inicial.dataAlta ?: avui()) }
    val existeix = estat.socis.any { it.id == s.id }
    val darrerJunta = existeix && inicial.esJunta && estat.socis.count { it.esJunta && it.actiu } == 1

    AlertDialog(
        onDismissRequest = onTanca,
        title = { Text(if (existeix) "Editar soci" else "Donar d'alta un soci", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Camp("Nom", s.nom, { s = s.copy(nom = it) }, Modifier.weight(1f))
                    Camp("Cognoms", s.cognoms, { s = s.copy(cognoms = it) }, Modifier.weight(1f))
                }
                Camp("Correu", s.email, { s = s.copy(email = it) }, teclat = KeyboardType.Email, ajuda = "Hi rebrà l'informe mensual.")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Camp("Referència al banc", s.refBanc, { s = s.copy(refBanc = it) }, Modifier.weight(1f), ajuda = "La del rebut del soci al banc.")
                    Camp(
                        if (existeix) "PIN nou (opcional)" else "PIN", pin, { if (it.length <= 4 && it.all(Char::isDigit)) pin = it }, Modifier.weight(1f),
                        teclat = KeyboardType.NumberPassword, secret = true, ajuda = if (existeix) "Deixa-ho buit per no canviar-lo." else "4 xifres.",
                    )
                }
                CampData("Data d'alta", dataAlta, { dataAlta = it })
                Casella("És de la junta (pot gestionar l'app)", s.esJunta, { if (!darrerJunta) s = s.copy(rol = if (it) Rol.JUNTA else Rol.SOCI) }, enabled = !darrerJunta)
                if (darrerJunta) Suau("És l'únic membre de la junta: no se li pot treure el rol.", mida = 14.sp)
            }
        },
        confirmButton = {
            Boto(if (existeix) "Desar" else "Donar d'alta", {
                val periodes = if (s.periodes.isEmpty()) listOf(cat.gastrobacus.consum.dades.Periode(dataAlta)) else s.periodes.toMutableList().also { it[0] = it[0].copy(alta = dataAlta) }
                model.accio(if (existeix) "Soci desat" else "${s.nom} donat d'alta", onTanca) { model.repo.desaSoci(s.copy(periodes = periodes), pin.ifBlank { null }) }
            }, enabled = s.nom.isNotBlank() && (if (existeix) pin.isEmpty() || pin.length == 4 else pin.length == 4))
        },
        dismissButton = { Boto("Cancel·lar", onTanca, tipus = Tipus.FANTASMA) },
        containerColor = C.superficie,
    )
}
