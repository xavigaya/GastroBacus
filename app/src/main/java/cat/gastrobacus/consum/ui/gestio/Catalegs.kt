package cat.gastrobacus.consum.ui.gestio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.Categoria
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.Producte
import cat.gastrobacus.consum.dades.Rol
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.diaCurt
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.dades.llegirEuros
import cat.gastrobacus.consum.dades.nouId
import cat.gastrobacus.consum.ui.AppModel
import cat.gastrobacus.consum.ui.Boto
import cat.gastrobacus.consum.ui.C
import cat.gastrobacus.consum.ui.Camp
import cat.gastrobacus.consum.ui.Cap
import cat.gastrobacus.consum.ui.Casella
import cat.gastrobacus.consum.ui.Cel
import cat.gastrobacus.consum.ui.Desplegable
import cat.gastrobacus.consum.ui.Etiqueta
import cat.gastrobacus.consum.ui.FilaTaula
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
    val llista = estat.productes.filter { it.nom.contains(cerca, ignoreCase = true) }.sortedWith(compareBy({ it.categoria.ordinal }, { it.nom }))
    val pesos = listOf(3f, 1.6f, 1.1f, 1.1f, 1.1f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Camp("Cerca", cerca, { cerca = it }, Modifier.width(320.dp), placeholder = "Nom del producte")
            Box(Modifier.weight(1f))
            Boto("Nou producte", { editant = Producte(nouId(), "", Categoria.CERVESA, 100) }, icona = Icons.Default.Add)
        }
        Taula(Modifier.weight(1f)) {
            FilaTaula(listOf({ Cap("Producte") }, { Cap("Categoria") }, { Cap("Soci", true) }, { Cap("Convidat", true) }, { Cap("Estoc", true) }, { Cap("") }), pesos, capcalera = true)
            LazyColumn {
                items(llista, key = { it.id }) { p ->
                    Box(Modifier.alpha(if (p.actiu) 1f else .5f)) {
                        FilaTaula(
                            listOf(
                                { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Cel(p.nom, negreta = true); if (!p.actiu) Etiqueta("Amagat") } },
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
}

@Composable
private fun EditorProducte(model: AppModel, estat: Estat, inicial: Producte, onTanca: () -> Unit) {
    var p by remember { mutableStateOf(inicial) }
    var preu by remember { mutableStateOf(textEuros(inicial.preu)) }
    var preuConv by remember { mutableStateOf(inicial.preuConvidat?.let { textEuros(it) } ?: "") }
    var ambPreuConv by remember { mutableStateOf(inicial.preuConvidat != null) }
    var estoc by remember { mutableStateOf(inicial.estoc.toString()) }
    var minim by remember { mutableStateOf(inicial.estocMinim.toString()) }
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
                Casella("Portar el control d'estoc", p.controlaEstoc, { p = p.copy(controlaEstoc = it) })
                if (p.controlaEstoc) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Camp("Unitats a la societat", estoc, { if (it.all(Char::isDigit)) estoc = it }, Modifier.weight(1f), teclat = KeyboardType.Number)
                    Camp("Avisa quan en quedin", minim, { if (it.all(Char::isDigit)) minim = it }, Modifier.weight(1f), teclat = KeyboardType.Number)
                }
                Casella("Visible per apuntar", p.actiu, { p = p.copy(actiu = it) })
            }
        },
        confirmButton = {
            Boto("Desar", {
                val nou = p.copy(
                    preu = llegirEuros(preu) ?: 0, preuConvidat = if (ambPreuConv) llegirEuros(preuConv) else null,
                    estoc = estoc.toIntOrNull() ?: 0, estocMinim = minim.toIntOrNull() ?: 0,
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
    val entrades = remember { mutableStateMapOf<String, String>() }
    val llista = estat.productes.filter { it.actiu && it.controlaEstoc && (!nomesBaix || it.calReposar) }.sortedBy { it.estoc - it.estocMinim }
    val pesos = listOf(3f, 1f, 1f, 2.2f)

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Suau("Quan arribi una comanda, escriu quantes unitats entren. Per corregir un recompte, posa un número negatiu.", Modifier.weight(1f))
                Casella("Només el que cal reposar", nomesBaix, { nomesBaix = it })
            }
            Taula(Modifier.weight(1f)) {
                FilaTaula(listOf({ Cap("Producte") }, { Cap("Queden", true) }, { Cap("Mínim", true) }, { Cap("Entrada") }), pesos, capcalera = true)
                LazyColumn {
                    items(llista, key = { it.id }) { p ->
                        FilaTaula(
                            listOf(
                                {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Cel(p.nom)
                                        if (p.estoc <= 0) Etiqueta("Esgotat", ToEtiqueta.VERMELL) else if (p.calReposar) Etiqueta("Cal reposar", ToEtiqueta.AMBRE)
                                    }
                                },
                                { Cel(p.estoc.toString(), dreta = true, negreta = true) },
                                { Cel(p.estocMinim.toString(), dreta = true) },
                                {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Camp("", entrades[p.id] ?: "", { t -> if (t.isEmpty() || t == "-" || t.toIntOrNull() != null) entrades[p.id] = t }, Modifier.width(110.dp), placeholder = "+24", teclat = KeyboardType.Number)
                                        Boto("Afegir", {
                                            val n = entrades[p.id]?.toIntOrNull() ?: 0
                                            model.accio(if (n > 0) "Afegides $n unitats de ${p.nom}" else "Tretes ${-n} unitats de ${p.nom}", { entrades[p.id] = "" }) { model.repo.reposar(p.id, n, soci.id) }
                                        }, petit = true, enabled = (entrades[p.id]?.toIntOrNull() ?: 0) != 0)
                                    }
                                },
                            ),
                            pesos,
                        )
                    }
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Subtitol("Darreres entrades")
            if (estat.reposicions.isEmpty()) Suau("Encara no s'ha registrat cap entrada.")
            estat.reposicions.take(20).forEach { r ->
                Text(
                    "${diaCurt(r.ts)}: ${if (r.quantitat > 0) "+" else ""}${r.quantitat} ${estat.producte(r.producteId)?.nom ?: ""} (${estat.soci(r.perId)?.nom ?: ""})",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/* ---------------- Socis ---------------- */

@Composable
fun GestioSocis(model: AppModel, estat: Estat) {
    var editant by remember { mutableStateOf<Soci?>(null) }
    var cerca by remember { mutableStateOf("") }
    val llista = estat.socis.filter { (it.nomComplet + " " + it.email).contains(cerca, ignoreCase = true) }
        .sortedWith(compareBy(java.text.Collator.getInstance(cat.gastrobacus.consum.dades.CATALA)) { it.nom })
    val pesos = listOf(3f, 3f, 1.3f, 1.2f, 1f)
    val seguentRef = "GB-%04d".format(estat.socis.size + 1)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Camp("Cerca", cerca, { cerca = it }, Modifier.width(320.dp), placeholder = "Nom o correu")
            Suau("${estat.socis.count { it.actiu }} socis actius", Modifier.weight(1f))
            Boto("Nou soci", { editant = Soci(nouId(), "", "", "", Rol.SOCI, "", "", seguentRef) }, icona = Icons.Default.Add)
        }
        Taula(Modifier.weight(1f)) {
            FilaTaula(listOf({ Cap("Nom") }, { Cap("Correu") }, { Cap("Ref. banc") }, { Cap("Rol") }, { Cap("") }), pesos, capcalera = true)
            LazyColumn {
                items(llista, key = { it.id }) { s ->
                    Box(Modifier.alpha(if (s.actiu) 1f else .5f)) {
                        FilaTaula(
                            listOf(
                                { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Cel(s.nomComplet, negreta = true); if (!s.actiu) Etiqueta("De baixa") } },
                                { Cel(s.email.ifBlank { "—" }) },
                                { Cel(s.refBanc) },
                                { if (s.esJunta) Etiqueta("Junta", ToEtiqueta.MOST) else Cel("Soci") },
                                { Boto("Editar", { editant = s }, tipus = Tipus.FANTASMA, petit = true) },
                            ),
                            pesos,
                        )
                    }
                }
            }
        }
    }
    editant?.let { EditorSoci(model, estat, it) { editant = null } }
}

@Composable
private fun EditorSoci(model: AppModel, estat: Estat, inicial: Soci, onTanca: () -> Unit) {
    var s by remember { mutableStateOf(inicial) }
    var pin by remember { mutableStateOf("") }
    val existeix = estat.socis.any { it.id == s.id }
    val darrerJunta = existeix && inicial.esJunta && estat.socis.count { it.esJunta && it.actiu } == 1

    AlertDialog(
        onDismissRequest = onTanca,
        title = { Text(if (existeix) "Editar soci" else "Nou soci", style = MaterialTheme.typography.headlineMedium) },
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
                Casella("És de la junta (pot gestionar l'app)", s.esJunta, { if (!darrerJunta) s = s.copy(rol = if (it) Rol.JUNTA else Rol.SOCI) }, enabled = !darrerJunta)
                Casella("Soci actiu", s.actiu, { if (!darrerJunta) s = s.copy(actiu = it) }, enabled = !darrerJunta)
                if (darrerJunta) Suau("És l'únic membre de la junta: no se li pot treure el rol ni donar de baixa.", mida = 14.sp)
            }
        },
        confirmButton = {
            Boto("Desar", { model.accio("Soci desat", onTanca) { model.repo.desaSoci(s, pin.ifBlank { null }) } },
                enabled = s.nom.isNotBlank() && (if (existeix) pin.isEmpty() || pin.length == 4 else pin.length == 4))
        },
        dismissButton = { Boto("Cancel·lar", onTanca, tipus = Tipus.FANTASMA) },
        containerColor = C.superficie,
    )
}

