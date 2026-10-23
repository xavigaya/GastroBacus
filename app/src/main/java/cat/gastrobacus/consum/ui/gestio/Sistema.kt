package cat.gastrobacus.consum.ui.gestio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import cat.gastrobacus.consum.dades.Correu
import cat.gastrobacus.consum.dades.EstatCorreu
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.dataHora
import cat.gastrobacus.consum.google.Google
import cat.gastrobacus.consum.ui.AppModel
import cat.gastrobacus.consum.ui.Avis
import cat.gastrobacus.consum.ui.Bloc
import cat.gastrobacus.consum.ui.Boto
import cat.gastrobacus.consum.ui.C
import cat.gastrobacus.consum.ui.Camp
import cat.gastrobacus.consum.ui.Cap
import cat.gastrobacus.consum.ui.Casella
import cat.gastrobacus.consum.ui.Cel
import cat.gastrobacus.consum.ui.Confirmacio
import cat.gastrobacus.consum.ui.Etiqueta
import cat.gastrobacus.consum.ui.FilaTaula
import cat.gastrobacus.consum.ui.Subtitol
import cat.gastrobacus.consum.ui.Suau
import cat.gastrobacus.consum.ui.Taula
import cat.gastrobacus.consum.ui.Tipus
import cat.gastrobacus.consum.ui.ToAvis
import cat.gastrobacus.consum.ui.ToEtiqueta
import cat.gastrobacus.consum.ui.dataCopia

/* ---------------- Correus ---------------- */

@Composable
fun Correus(model: AppModel, estat: Estat) {
    var obert by remember { mutableStateOf<Correu?>(null) }
    val pendents = estat.correus.count { it.estat != EstatCorreu.ENVIAT }
    val pesos = listOf(1.4f, 3f, 4f, 1.1f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (estat.config.compteGoogle == null) Avis("Encara no hi ha cap compte de Google connectat. Els correus queden aquí fins que es connecti a Configuració.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Suau(if (pendents == 0) "Tots els correus s'han enviat." else "$pendents correus pendents d'enviar.", Modifier.weight(1f))
            if (pendents > 0) Boto("Enviar els pendents", { model.accio("Correus enviats") { model.serveis.enviaPendents() } }, icona = Icons.Default.Send, enabled = estat.config.compteGoogle != null && !model.ocupat)
        }
        if (estat.correus.isEmpty()) Suau("Encara no s'ha enviat cap correu. Els informes mensuals s'envien des de Tancament de mes.")
        else Taula(Modifier.weight(1f)) {
            FilaTaula(listOf({ Cap("Quan") }, { Cap("Per a") }, { Cap("Assumpte") }, { Cap("Estat") }), pesos, capcalera = true)
            LazyColumn {
                items(estat.correus, key = { it.id }) { c ->
                    FilaTaula(
                        listOf(
                            { Cel(dataHora(c.ts)) },
                            { Column { Cel(c.destinatariOriginal); if (c.per != c.destinatariOriginal) Suau("va a ${c.per}", mida = 13.sp) } },
                            { Cel(c.assumpte) },
                            {
                                when (c.estat) {
                                    EstatCorreu.ENVIAT -> Etiqueta("Enviat", ToEtiqueta.VERD)
                                    EstatCorreu.ERROR -> Etiqueta("Error", ToEtiqueta.VERMELL)
                                    EstatCorreu.PENDENT -> Etiqueta("Pendent", ToEtiqueta.AMBRE)
                                }
                            },
                        ),
                        pesos, onClick = { obert = c },
                    )
                }
            }
        }
    }
    obert?.let { c ->
        AlertDialog(
            onDismissRequest = { obert = null },
            title = { Text(c.assumpte, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Suau("Per a ${c.per}" + if (c.per != c.destinatariOriginal) " (en lloc de ${c.destinatariOriginal}, mode de proves)" else "")
                    if (c.adjuntSociId != null) Suau("Amb el PDF del consum adjunt.", mida = 14.sp)
                    Text(c.cos, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp))
                    c.error?.let { Avis(it, ToAvis.ERROR) }
                }
            },
            confirmButton = {
                if (c.estat != EstatCorreu.ENVIAT) Boto("Marcar com a enviat", { obert = null; model.accio("Marcat com a enviat") { model.repo.marcaCorreu(c.id, EstatCorreu.ENVIAT) } }, tipus = Tipus.FANTASMA)
                else Boto("Tancar", { obert = null })
            },
            dismissButton = { if (c.estat != EstatCorreu.ENVIAT) Boto("Tancar", { obert = null }) },
            containerColor = C.superficie,
        )
    }
}

/* ---------------- Còpies de seguretat ---------------- */

@Composable
fun Copies(model: AppModel, estat: Estat) {
    var copies by remember { mutableStateOf<List<Google.FitxerCopia>?>(null) }
    var restaurant by remember { mutableStateOf<Google.FitxerCopia?>(null) }
    val cfg = estat.config

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp).widthIn(max = 900.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Bloc(Modifier.fillMaxWidth()) {
            Subtitol("Còpia diària al Google Drive")
            Text(
                "Cada dia a les 4 de la matinada es desa una còpia de totes les dades a la carpeta «${Google.CARPETA}» del Drive del compte de la tauleta. Es guarden les darreres 90.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Suau("La tauleta ha d'estar engegada, amb wifi. Si a les 4 no hi ha connexió, la còpia es fa quan torni.", mida = 14.sp)
            if (cfg.compteGoogle == null) Avis("Cal connectar el compte de Google a Configuració.")
            else {
                Text("Compte: ${cfg.compteGoogle}", style = MaterialTheme.typography.bodyMedium)
                Text("Darrera còpia: ${cfg.darreraCopiaTs?.let { dataHora(it) } ?: "encara cap"}", style = MaterialTheme.typography.titleMedium)
                cfg.darreraCopiaError?.let { Avis("La darrera còpia ha fallat: $it", ToAvis.ERROR) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Boto("Fer una còpia ara", { model.accio("Còpia desada al Drive") { model.serveis.ferCopia(interactiu = true) } }, icona = Icons.Default.Backup, enabled = cfg.compteGoogle != null && !model.ocupat)
                Boto("Veure les còpies", { model.accioAmb({ model.serveis.llistaCopies() }) { copies = it } }, tipus = Tipus.FANTASMA, enabled = cfg.compteGoogle != null)
            }
        }
        copies?.let { l ->
            Subtitol("Còpies al Drive")
            if (l.isEmpty()) Suau("No hi ha cap còpia.")
            Taula {
                FilaTaula(listOf({ Cap("Fitxer") }, { Cap("Data") }, { Cap("Mida", true) }, { Cap("") }), listOf(3f, 2f, 1f, 1.4f), capcalera = true)
                l.forEach { f ->
                    FilaTaula(
                        listOf({ Cel(f.nom) }, { Cel(dataCopia(f.creat)) }, { Cel("%.0f KB".format(f.mida / 1024.0), dreta = true) }, { Boto("Recuperar", { restaurant = f }, tipus = Tipus.FANTASMA, petit = true) }),
                        listOf(3f, 2f, 1f, 1.4f),
                    )
                }
            }
        }
    }
    restaurant?.let { f ->
        Confirmacio(
            "Recuperar la còpia",
            "Totes les dades actuals de la tauleta se substituiran per les de la còpia del ${dataCopia(f.creat)}. Abans es farà una còpia de l'estat actual, per si de cas.",
            "Recuperar", perill = true,
            onConfirma = {
                restaurant = null
                model.accio("Còpia recuperada", { model.surt() }) {
                    model.serveis.ferCopia(interactiu = true)
                    model.serveis.restauraCopia(f.id)
                }
            },
            onCancela = { restaurant = null },
        )
    }
}

/* ---------------- Configuració ---------------- */

@Composable
fun Configuracio(model: AppModel, estat: Estat) {
    val context = LocalContext.current
    val cfg = estat.config
    var nom by remember(cfg) { mutableStateOf(cfg.nomSocietat) }
    var tresoreria by remember(cfg) { mutableStateOf(cfg.correuTresoreria) }
    var espais by remember(cfg) { mutableStateOf(cfg.espais.joinToString("\n")) }
    var minuts by remember(cfg) { mutableStateOf(cfg.minutsDesfer.toString()) }
    var dies by remember(cfg) { mutableStateOf(cfg.diesReclamacio.toString()) }
    var proves by remember(cfg) { mutableStateOf(cfg.modeProves) }
    var correuProves by remember(cfg) { mutableStateOf(cfg.correuProves) }
    var confirmaDadesProva by remember { mutableStateOf(false) }
    var refresc by remember { mutableIntStateOf(0) }
    val fixada = remember(refresc) { Quiosc.estaFixada(context) }
    val potSola = remember(refresc) { Quiosc.potObrirseSola(context) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(2000); refresc++ } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp).widthIn(max = 900.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Bloc(Modifier.fillMaxWidth()) {
            Subtitol("Compte de Google de la tauleta")
            Suau("S'utilitza per enviar els informes amb el Gmail i per desar les còpies de seguretat al Drive.")
            if (cfg.compteGoogle != null) {
                Text("Connectat: ${cfg.compteGoogle}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Boto("Tornar a iniciar la sessió", { model.accio("Sessió iniciada amb Google") { model.serveis.iniciaSessioGoogle() } }, tipus = Tipus.FANTASMA)
                    Boto("Desconnectar", { model.accio("Compte de Google desconnectat") { model.serveis.tancaSessioGoogle() } }, tipus = Tipus.FANTASMA)
                }
            } else Boto("Iniciar la sessió amb Google", { model.accio("Sessió iniciada amb Google") { model.serveis.iniciaSessioGoogle() } })
        }

        Bloc(Modifier.fillMaxWidth()) {
            Subtitol("Societat")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Camp("Nom", nom, { nom = it }, Modifier.weight(1f))
                Camp("Correu de tresoreria", tresoreria, { tresoreria = it }, Modifier.weight(1f), teclat = KeyboardType.Email, ajuda = "Hi arriba el resum mensual. Si és buit, va al Gmail de la tauleta.")
            }
            Camp("Espais que es poden reservar", espais, { espais = it }, linies = 3, ajuda = "Un per línia.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Camp("Minuts per desfer una consumició", minuts, { if (it.length <= 3 && it.all(Char::isDigit)) minuts = it }, Modifier.weight(1f), teclat = KeyboardType.Number)
                Camp("Dies per reclamar l'informe", dies, { if (it.length <= 2 && it.all(Char::isDigit)) dies = it }, Modifier.weight(1f), teclat = KeyboardType.Number)
            }
            Casella("Mode de proves: enviar tots els correus a una sola adreça", proves, { proves = it })
            if (proves) Camp("Adreça de proves", correuProves, { correuProves = it }, teclat = KeyboardType.Email)
            Boto("Desar la configuració", {
                model.accio("Configuració desada") {
                    model.repo.actualitzaConfig {
                        it.copy(
                            nomSocietat = nom.trim().ifBlank { "GastroBacus" }, correuTresoreria = tresoreria.trim(),
                            espais = espais.lines().map(String::trim).filter(String::isNotBlank).ifEmpty { it.espais },
                            minutsDesfer = minuts.toIntOrNull() ?: it.minutsDesfer, diesReclamacio = dies.toIntOrNull() ?: it.diesReclamacio,
                            modeProves = proves, correuProves = correuProves.trim(),
                        )
                    }
                }
            })
        }

        Bloc(Modifier.fillMaxWidth()) {
            Subtitol("Tauleta")
            Casella("Fixar la pantalla a l'app", cfg.fixarPantalla, { v ->
                model.accio(if (v) "La pantalla es fixarà a l'app" else "La pantalla ja no es fixarà") { model.repo.actualitzaConfig { it.copy(fixarPantalla = v) } }
                if (v) { model.pantallaDesfixada = false; Quiosc.fixa() } else Quiosc.desfixa()
            })
            Suau("Amb la pantalla fixada no es pot sortir de l'app sense passar per aquí. La primera vegada Android demana confirmació.", mida = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Etiqueta(if (fixada) "Ara està fixada" else "Ara no està fixada", if (fixada) ToEtiqueta.VERD else ToEtiqueta.AMBRE)
                if (fixada) Boto("Desfixar per fer manteniment", { model.pantallaDesfixada = true; Quiosc.desfixa(); refresc++ }, tipus = Tipus.FANTASMA, petit = true)
                else if (cfg.fixarPantalla) Boto("Tornar a fixar", { model.pantallaDesfixada = false; Quiosc.fixa(); refresc++ }, tipus = Tipus.FANTASMA, petit = true)
            }
            Box(Modifier.padding(top = 4.dp))
            Text("Obrir-se sola en engegar la tauleta", style = MaterialTheme.typography.titleMedium)
            if (potSola) Etiqueta("Activat", ToEtiqueta.VERD)
            else {
                Suau("Android demana el permís «Mostra sobre altres aplicacions» perquè l'app s'obri sola. En prémer el botó es desfixa la pantalla i s'obren els ajustos.", mida = 14.sp)
                Boto("Donar el permís", { model.pantallaDesfixada = true; Quiosc.obreAjustPermis(context) }, tipus = Tipus.FANTASMA, petit = true)
            }
            Suau("També la podeu posar com a pantalla d'inici de la tauleta: així el botó d'inici sempre torna a GastroBacus.", mida = 14.sp)
            Boto("Triar la pantalla d'inici", { model.pantallaDesfixada = true; Quiosc.obreAjustInici(context) }, tipus = Tipus.FANTASMA, petit = true)
        }

        Bloc(Modifier.fillMaxWidth(), vora = C.error) {
            Subtitol("Dades de prova")
            Text("Esborra totes les dades i torna a carregar els socis, productes i consumicions fictícies (PIN 1234). El compte de Google es manté.", style = MaterialTheme.typography.bodyMedium)
            Boto("Carregar les dades de prova", { confirmaDadesProva = true }, tipus = Tipus.PERILL)
        }
    }
    if (confirmaDadesProva) Confirmacio(
        "Carregar les dades de prova", "S'esborraran totes les consumicions, socis, àpats i reserves d'aquesta tauleta. Abans es farà una còpia al Drive si hi ha compte connectat.",
        "Esborrar i carregar", perill = true,
        onConfirma = {
            confirmaDadesProva = false
            model.accio("Dades de prova carregades", { model.surt() }) {
                if (cfg.compteGoogle != null) runCatching { model.serveis.ferCopia(interactiu = true) }
                model.repo.carregaDadesProva()
            }
        },
        onCancela = { confirmaDadesProva = false },
    )
}

