package cat.gastrobacus.consum.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.Apat
import cat.gastrobacus.consum.dades.Assistent
import cat.gastrobacus.consum.dades.DespesaApat
import cat.gastrobacus.consum.dades.EstatApat
import cat.gastrobacus.consum.dades.EstatBizum
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.Extern
import cat.gastrobacus.consum.dades.Franja
import cat.gastrobacus.consum.dades.ModeRepartiment
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.TOT_EL_LOCAL
import cat.gastrobacus.consum.dades.TipusActivitat
import cat.gastrobacus.consum.dades.avui
import cat.gastrobacus.consum.dades.dataLlarga
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.dades.llegirEuros
import cat.gastrobacus.consum.dades.nouId
import cat.gastrobacus.consum.dades.repartiment

private fun num(d: Double) = d.toString().removeSuffix(".0").replace('.', ',')
private fun textEuros(c: Long) = if (c == 0L) "" else euros(c).removeSuffix(" €")

@Composable
fun Apats(model: AppModel, estat: Estat, soci: Soci) {
    var editant by remember { mutableStateOf<Apat?>(null) }
    editant?.let { a -> EditorApat(model, estat, soci, a) { editant = null }; return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Titol("Àpats i activitats", Modifier.weight(1f))
            Boto("Nova activitat", {
                val pc = estat.config.preusCadira[TipusActivitat.PRIVAT]
                editant = Apat(
                    nouId(), "", avui(), soci.id, ModeRepartiment.IGUALS, listOf(Assistent(soci.id)),
                    cadiraSoci = pc?.soci ?: 0, cadiraNoSoci = pc?.noSoci ?: 0,
                )
            }, icona = Icons.Default.Add)
        }
        Suau("Apunta qui ve i què ha costat la compra. Qui ha avançat diners se'ls descompta; a la resta se'ls carrega la seva part i la cadira al compte del mes.")
        if (estat.apats.isEmpty()) Suau("Encara no hi ha cap activitat. Crea'n una per repartir-ne les despeses.")
        estat.apats.sortedByDescending { it.data }.forEach { a ->
            val r = repartiment(a)
            val meu = r.quotes.firstOrNull { it.sociId == soci.id }
            val potApuntarse = a.estat == EstatApat.OBERT && a.tipus != TipusActivitat.PRIVAT && meu == null && a.data >= avui()
            Bloc(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { editant = a }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(a.titol.ifBlank { "Activitat sense títol" }, style = MaterialTheme.typography.titleLarge)
                            Etiqueta(a.tipus.nom, if (a.tipus == TipusActivitat.OBERT) ToEtiqueta.MOST else ToEtiqueta.NEUTRE)
                        }
                        Suau("${dataLlarga(a.data)}, ${a.franja.nom.lowercase()}. ${a.espai}. ${a.assistents.sumOf { 1 + it.convidats } + a.externs.size} persones, organitza ${estat.soci(a.organitzadorId)?.nom ?: "—"}")
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Etiqueta(if (a.estat == EstatApat.OBERT) "Obert" else "Passat al compte", if (a.estat == EstatApat.OBERT) ToEtiqueta.AMBRE else ToEtiqueta.VERD)
                        XifraText(euros(r.total), 20.sp)
                        if (meu != null) Text(
                            if (meu.saldo < 0) "Se't torna ${euros(-meu.saldo)}" else "Et toca ${euros(meu.saldo)}",
                            style = MaterialTheme.typography.labelMedium, color = if (meu.saldo < 0) C.pampol else C.most,
                        )
                        if (potApuntarse) Boto("M'hi apunto", {
                            model.accio("T'hi has apuntat") { model.repo.desaApat(a.copy(assistents = a.assistents + Assistent(soci.id)), soci.id); model.enviaEnSegonPla() }
                        }, petit = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorApat(model: AppModel, estat: Estat, soci: Soci, inicial: Apat, onFet: () -> Unit) {
    var a by remember { mutableStateOf(inicial) }
    // Imports en edició com a text, per poder escriure comes
    val importsText = remember { mutableStateOf(inicial.despeses.associate { it.id to textEuros(it.import) }) }
    var cadSoci by remember { mutableStateOf(textEuros(inicial.cadiraSoci)) }
    var cadNoSoci by remember { mutableStateOf(textEuros(inicial.cadiraNoSoci)) }
    var nomExtern by remember { mutableStateOf("") }
    val existeix = estat.apats.any { it.id == a.id }
    val organitza = a.organitzadorId == soci.id || soci.esJunta || !existeix
    val potEditar = a.estat == EstatApat.OBERT && (organitza || a.assistents.any { it.sociId == soci.id })
    val potTancar = a.estat == EstatApat.OBERT && (a.organitzadorId == soci.id || soci.esJunta)
    var confirmaTancar by remember { mutableStateOf(false) }
    var confirmaEliminar by remember { mutableStateOf(false) }
    val r = repartiment(a)
    fun nom(id: String) = estat.soci(id)?.nomComplet ?: "—"
    val disponibles = estat.socisActius.filter { s -> a.assistents.none { it.sociId == s.id } }
    fun canviaTipus(t: TipusActivitat) {
        val pc = estat.config.preusCadira[t]
        a = a.copy(tipus = t, cadiraSoci = pc?.soci ?: a.cadiraSoci, cadiraNoSoci = pc?.noSoci ?: a.cadiraNoSoci, externs = if (t == TipusActivitat.OBERT) a.externs else emptyList())
        cadSoci = textEuros(a.cadiraSoci); cadNoSoci = textEuros(a.cadiraNoSoci)
    }
    val pendentsExterns = a.externs.filter { !it.pagat }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Boto("Àpats", onFet, tipus = Tipus.FANTASMA, petit = true, icona = Icons.AutoMirrored.Filled.ArrowBack)
        Titol(if (existeix) a.titol.ifBlank { "Activitat" } else "Nova activitat")
        if (a.estat == EstatApat.TANCAT) Avis("Aquesta activitat ja s'ha passat als comptes del mes.", ToAvis.OK)
        else if (!potEditar) Avis(
            if (a.tipus != TipusActivitat.PRIVAT) "Encara no hi estàs apuntat. Ho pots fer més avall, a «Socis que hi vénen»."
            else "És una activitat privada: només la poden modificar qui l'organitza i la junta.",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Camp("Títol", a.titol, { if (potEditar) a = a.copy(titol = it) }, Modifier.weight(2f), placeholder = "Per exemple, Sopar de caragols")
            CampData("Data", a.data, { a = a.copy(data = it) }, Modifier.weight(1f), organitza && potEditar)
            Desplegable(Franja.entries, a.franja, { it.nom }, { a = a.copy(franja = it) }, Modifier.weight(1f), "Quan", organitza && potEditar)
        }

        Bloc(Modifier.fillMaxWidth()) {
            Subtitol("Tipus i espai")
            Interruptor(TipusActivitat.entries.map { it.nom }, a.tipus.ordinal, { canviaTipus(TipusActivitat.entries[it]) }, organitza && potEditar)
            Suau(a.tipus.descripcio + ".")
            Desplegable(
                listOf(TOT_EL_LOCAL) + estat.config.espais, a.espai,
                { if (it == TOT_EL_LOCAL) "Tot el local (no es comparteix)" else "$it (es pot compartir)" },
                { a = a.copy(espai = it) }, Modifier.width(420.dp), "Espai", organitza && potEditar,
            )
            Suau(
                if (a.espai == TOT_EL_LOCAL) "Es reserva tot el local: aquell dia i franja no s'hi podrà fer cap altra reserva."
                else "Es reserva només aquest espai; la resta del local es pot fer servir.", mida = 14.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
                if (soci.esJunta && potEditar) {
                    Camp("Cadira soci (€)", cadSoci, { cadSoci = it; a = a.copy(cadiraSoci = llegirEuros(it) ?: 0) }, Modifier.width(180.dp), teclat = KeyboardType.Decimal)
                    Camp("Cadira no soci (€)", cadNoSoci, { cadNoSoci = it; a = a.copy(cadiraNoSoci = llegirEuros(it) ?: 0) }, Modifier.width(180.dp), teclat = KeyboardType.Decimal)
                    Suau("Preus per a aquesta activitat. Els preus per defecte es canvien a Gestió > Configuració.", Modifier.weight(1f).padding(bottom = 8.dp), 14.sp)
                } else Text("Cadira: ${euros(a.cadiraSoci)} per soci i ${euros(a.cadiraNoSoci)} per convidat o persona de fora.", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Bloc(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Subtitol("Com es reparteix el menjar", Modifier.weight(1f))
                Interruptor(listOf("A parts iguals", "Segons el que ha pres"), a.mode.ordinal, { a = a.copy(mode = ModeRepartiment.entries[it]) }, potEditar)
            }
            Suau(
                if (a.mode == ModeRepartiment.IGUALS) "Cada persona paga el mateix. Els convidats compten com una persona més i van al compte del soci que els porta."
                else "Dona a cadascú les parts que ha pres: 1 és una ració normal, 0,5 mitja, 2 el doble. Si porta convidats, suma'ls a les seves parts.",
            )
        }

        Subtitol("Socis que hi vénen")
        Taula {
            FilaTaula(listOf({ Cap("Soci") }, { Cap("Convidats") }, { Cap(if (a.mode == ModeRepartiment.IGUALS) "" else "Parts") }, { Cap("") }), listOf(3f, 2f, 2f, 0.8f), capcalera = true)
            a.assistents.forEachIndexed { i, x ->
                fun upd(n: Assistent) { a = a.copy(assistents = a.assistents.toMutableList().also { l -> l[i] = n }) }
                val potAquest = potEditar && (organitza || x.sociId == soci.id)
                FilaTaula(
                    listOf(
                        { Cel(nom(x.sociId)) },
                        { Pas(x.convidats.toString(), potAquest, { upd(x.copy(convidats = maxOf(0, x.convidats - 1))) }, { upd(x.copy(convidats = x.convidats + 1)) }) },
                        {
                            if (a.mode == ModeRepartiment.PROPORCIONAL) Pas(num(x.parts), potAquest, { upd(x.copy(parts = maxOf(0.0, x.parts - 0.5))) }, { upd(x.copy(parts = x.parts + 0.5)) })
                        },
                        { if (potAquest) BotoRodo(Icons.Default.Close, "Treu ${nom(x.sociId)}") { a = a.copy(assistents = a.assistents.filterIndexed { j, _ -> j != i }) } },
                    ),
                    listOf(3f, 2f, 2f, 0.8f),
                )
            }
        }
        if (potEditar && disponibles.isNotEmpty()) {
            if (organitza) Desplegable(listOf<Soci?>(null) + disponibles, null, { it?.nomComplet ?: "+ Afegir un soci…" }, { s -> if (s != null) a = a.copy(assistents = a.assistents + Assistent(s.id)) }, Modifier.width(360.dp))
        } else if (a.estat == EstatApat.OBERT && a.tipus != TipusActivitat.PRIVAT && a.assistents.none { it.sociId == soci.id }) {
            Boto("M'hi apunto", { a = a.copy(assistents = a.assistents + Assistent(soci.id)); model.accio("T'hi has apuntat") { model.repo.desaApat(a, soci.id) } }, icona = Icons.Default.Add)
        }

        if (a.tipus == TipusActivitat.OBERT) {
            Subtitol("Persones de fora")
            Suau("Paguen la seva part i la cadira a preu de no soci, per Bizum o en efectiu. No va al compte de cap soci.")
            if (a.externs.isNotEmpty()) Taula {
                FilaTaula(listOf({ Cap("Nom") }, { Cap(if (a.mode == ModeRepartiment.IGUALS) "" else "Parts") }, { Cap("Ha de pagar", true) }, { Cap("Estat") }, { Cap("") }), listOf(3f, 2f, 1.4f, 2.6f, 0.8f), capcalera = true)
                a.externs.forEachIndexed { i, e ->
                    val q = r.quotes.firstOrNull { it.sociId == "ext:${e.id}" }
                    val bizum = estat.bizums.firstOrNull { it.externId == e.id && it.estat == EstatBizum.PENDENT }
                    fun upd(n: Extern) { a = a.copy(externs = a.externs.toMutableList().also { l -> l[i] = n }) }
                    FilaTaula(
                        listOf(
                            { Cel(e.nom) },
                            { if (a.mode == ModeRepartiment.PROPORCIONAL) Pas(num(e.parts), organitza && potEditar, { upd(e.copy(parts = maxOf(0.0, e.parts - 0.5))) }, { upd(e.copy(parts = e.parts + 0.5)) }) },
                            { Cel(euros(q?.quota ?: 0), dreta = true, negreta = true) },
                            {
                                when {
                                    e.pagat -> Etiqueta("Pagat", ToEtiqueta.VERD)
                                    bizum != null -> Etiqueta("Bizum pendent de confirmar", ToEtiqueta.AMBRE)
                                    existeix && potEditar -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (estat.config.telefonBizum.isNotBlank()) Boto("Bizum", {
                                            model.accio("Bizum de ${e.nom}: ${euros(q?.quota ?: 0)} al ${estat.config.telefonBizum}") { model.repo.desaApat(a, soci.id); model.repo.bizumExtern(a, e, q?.quota ?: 0) }
                                        }, tipus = Tipus.FANTASMA, petit = true)
                                        if (soci.esJunta) Boto("Efectiu", { upd(e.copy(pagat = true)) }, tipus = Tipus.FANTASMA, petit = true)
                                    }
                                    else -> Etiqueta("Pendent", ToEtiqueta.AMBRE)
                                }
                            },
                            { if (organitza && potEditar && !e.pagat) BotoRodo(Icons.Default.Close, "Treu ${e.nom}") { a = a.copy(externs = a.externs.filterIndexed { j, _ -> j != i }) } },
                        ),
                        listOf(3f, 2f, 1.4f, 2.6f, 0.8f),
                    )
                }
            }
            if (organitza && potEditar) Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                Camp("Nom", nomExtern, { nomExtern = it }, Modifier.width(320.dp), placeholder = "Persona o família")
                Boto("Afegir", { a = a.copy(externs = a.externs + Extern(nouId(), nomExtern.trim())); nomExtern = "" }, tipus = Tipus.SECUNDARI, enabled = nomExtern.isNotBlank(), icona = Icons.Default.Add)
            }
        }

        Subtitol("Despeses")
        Suau("El que algú ha pagat de la seva butxaca per a l'activitat. Se li descomptarà del que li toca.")
        a.despeses.forEachIndexed { i, d ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                Desplegable(estat.socisActius, estat.soci(d.pagadorId) ?: soci, { it.nomComplet }, { s ->
                    a = a.copy(despeses = a.despeses.toMutableList().also { l -> l[i] = d.copy(pagadorId = s.id) })
                }, Modifier.weight(1.3f), "Ho ha pagat", potEditar)
                Camp("Concepte", d.concepte, { t -> if (potEditar) a = a.copy(despeses = a.despeses.toMutableList().also { l -> l[i] = d.copy(concepte = t) }) }, Modifier.weight(1.6f), placeholder = "Carn, verdura…")
                Camp("Import (€)", importsText.value[d.id] ?: "", { t ->
                    if (potEditar) {
                        importsText.value = importsText.value + (d.id to t)
                        a = a.copy(despeses = a.despeses.toMutableList().also { l -> l[i] = d.copy(import = llegirEuros(t) ?: 0) })
                    }
                }, Modifier.weight(0.9f), teclat = KeyboardType.Decimal)
                if (potEditar) Column(Modifier.padding(bottom = 8.dp)) { BotoRodo(Icons.Default.Close, "Treu la despesa") { a = a.copy(despeses = a.despeses.filterIndexed { j, _ -> j != i }) } }
            }
        }
        if (potEditar) Boto("Afegir una despesa", { a = a.copy(despeses = a.despeses + DespesaApat(nouId(), soci.id, "", 0)) }, tipus = Tipus.SECUNDARI, petit = true, icona = Icons.Default.Add)

        Bloc(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Subtitol("Repartiment", Modifier.weight(1f))
                Suau(
                    "Menjar ${euros(r.total)}" + (if (r.perUnitat > 0) ", ${euros(Math.round(r.perUnitat))} per ${if (a.mode == ModeRepartiment.IGUALS) "persona" else "part"}" else "") +
                        ". Cadires ${euros(r.totalCadires)}",
                )
            }
            val pesos = listOf(3f, 1f, 1.2f, 1.2f, 2f)
            FilaTaula(listOf({ Cap("Qui") }, { Cap(if (a.mode == ModeRepartiment.IGUALS) "Persones" else "Parts", true) }, { Cap("Menjar", true) }, { Cap("Cadires", true) }, { Cap("Resultat", true) }), pesos, capcalera = true)
            r.quotes.forEach { q ->
                FilaTaula(
                    listOf(
                        { Column { Cel(q.nomExtern ?: nom(q.sociId)); if (q.pagat > 0) Suau("Ha pagat ${euros(q.pagat)}", mida = 13.sp); if (q.esExtern) Suau("De fora", mida = 13.sp) } },
                        { Cel(num(q.unitats), dreta = true) },
                        { Cel(euros(q.menjar), dreta = true) },
                        { Cel(if (q.cadires > 0) euros(q.cadires) else "", dreta = true) },
                        { Cel(if (q.saldo < 0) "se li torna ${euros(-q.saldo)}" else "paga ${euros(q.saldo)}", dreta = true, negreta = true, color = if (q.saldo < 0) C.pampol else C.most) },
                    ),
                    pesos,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (potEditar) Boto("Desar", { model.accio("Activitat desada", onFet) { model.repo.desaApat(a, soci.id); model.enviaEnSegonPla() } })
            if (potTancar) Boto("Passar als comptes del mes", { confirmaTancar = true }, tipus = Tipus.VERD, enabled = (r.total > 0 || r.totalCadires > 0) && a.assistents.isNotEmpty())
            if (soci.esJunta && a.estat == EstatApat.TANCAT) Boto("Reobrir", { model.accio("Activitat reoberta", onFet) { model.repo.desaApat(a.copy(estat = EstatApat.OBERT), soci.id) } }, tipus = Tipus.FANTASMA)
            if (existeix && a.estat == EstatApat.OBERT && (a.organitzadorId == soci.id || soci.esJunta)) Boto("Eliminar", { confirmaEliminar = true }, tipus = Tipus.FANTASMA)
        }
    }

    if (confirmaTancar) Confirmacio(
        "Passar als comptes",
        "Les quotes i les cadires sortiran a l'informe del mes de cada soci. Després ja no la podreu modificar." +
            if (pendentsExterns.isNotEmpty()) "\n\nAtenció: ${pendentsExterns.joinToString { it.nom }} encara no ${if (pendentsExterns.size == 1) "ha" else "han"} pagat." else "",
        "Passar als comptes",
        onConfirma = { confirmaTancar = false; model.accio("Activitat passada als comptes", onFet) { model.repo.desaApat(a.copy(estat = EstatApat.TANCAT), soci.id) } },
        onCancela = { confirmaTancar = false },
    )
    if (confirmaEliminar) Confirmacio(
        "Eliminar l'activitat", "S'esborrarà «${a.titol}» amb els assistents i les despeses, i s'anul·larà la reserva de l'espai.", "Eliminar",
        onConfirma = { confirmaEliminar = false; model.accio("Activitat eliminada", onFet) { model.repo.eliminaApat(a.id, soci.id); model.enviaEnSegonPla() } },
        onCancela = { confirmaEliminar = false }, perill = true,
    )
}

/** Control de menys/més amb el valor al mig. */
@Composable
fun Pas(valor: String, enabled: Boolean, menys: () -> Unit, mes: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (enabled) BotoRodo(Icons.Default.Remove, "Menys", menys)
        XifraText(valor, 18.sp, modifier = Modifier.width(40.dp))
        if (enabled) BotoRodo(Icons.Default.Add, "Més", mes)
    }
}
