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
import cat.gastrobacus.consum.dades.Estat
import cat.gastrobacus.consum.dades.ModeRepartiment
import cat.gastrobacus.consum.dades.Soci
import cat.gastrobacus.consum.dades.avui
import cat.gastrobacus.consum.dades.dataLlarga
import cat.gastrobacus.consum.dades.euros
import cat.gastrobacus.consum.dades.llegirEuros
import cat.gastrobacus.consum.dades.nouId
import cat.gastrobacus.consum.dades.repartiment

@Composable
fun Apats(model: AppModel, estat: Estat, soci: Soci) {
    var editant by remember { mutableStateOf<Apat?>(null) }
    editant?.let { a -> EditorApat(model, estat, soci, a) { editant = null }; return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Titol("Àpats compartits", Modifier.weight(1f))
            Boto("Nou àpat", {
                editant = Apat(nouId(), "", avui(), soci.id, ModeRepartiment.IGUALS, listOf(Assistent(soci.id)))
            }, icona = Icons.Default.Add)
        }
        Suau("Apunta qui ve a dinar o sopar i què ha costat la compra. Qui ha avançat diners se'ls descompta, i a la resta se'ls carrega la seva part al compte del mes.")
        if (estat.apats.isEmpty()) Suau("Encara no hi ha cap àpat. Crea'n un per repartir-ne les despeses.")
        estat.apats.sortedByDescending { it.data }.forEach { a ->
            val r = repartiment(a)
            val meu = r.quotes.firstOrNull { it.sociId == soci.id }
            Bloc(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { editant = a }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(a.titol.ifBlank { "Àpat sense títol" }, style = MaterialTheme.typography.titleLarge)
                        Suau("${dataLlarga(a.data)}. ${a.assistents.size} socis, organitza ${estat.soci(a.organitzadorId)?.nom ?: "—"}")
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Etiqueta(if (a.estat == EstatApat.OBERT) "Obert" else "Passat al compte", if (a.estat == EstatApat.OBERT) ToEtiqueta.AMBRE else ToEtiqueta.VERD)
                        XifraText(euros(r.total), 20.sp)
                        if (meu != null) Text(
                            if (meu.saldo < 0) "Se't torna ${euros(-meu.saldo)}" else "Et toca ${euros(meu.saldo)}",
                            style = MaterialTheme.typography.labelMedium, color = if (meu.saldo < 0) C.pampol else C.most,
                        )
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
    val importsText = remember { mutableStateOf(inicial.despeses.associate { it.id to (if (it.import == 0L) "" else euros(it.import).removeSuffix(" €")) }) }
    val existeix = estat.apats.any { it.id == a.id }
    val potEditar = a.estat == EstatApat.OBERT && (a.organitzadorId == soci.id || soci.esJunta || a.assistents.any { it.sociId == soci.id } || !existeix)
    val potTancar = a.estat == EstatApat.OBERT && (a.organitzadorId == soci.id || soci.esJunta)
    var confirmaTancar by remember { mutableStateOf(false) }
    var confirmaEliminar by remember { mutableStateOf(false) }
    val r = repartiment(a)
    fun nom(id: String) = estat.soci(id)?.nomComplet ?: "—"
    val disponibles = estat.socisActius.filter { s -> a.assistents.none { it.sociId == s.id } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Boto("Àpats", onFet, tipus = Tipus.FANTASMA, petit = true, icona = Icons.AutoMirrored.Filled.ArrowBack)
        Titol(if (existeix) a.titol.ifBlank { "Àpat" } else "Nou àpat")
        if (a.estat == EstatApat.TANCAT) Avis("Aquest àpat ja s'ha passat als comptes del mes.", ToAvis.OK)
        else if (!potEditar) Avis("Només el poden modificar els assistents, qui l'organitza o la junta.")

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Camp("Títol", a.titol, { if (potEditar) a = a.copy(titol = it) }, Modifier.weight(2f), placeholder = "Per exemple, Sopar de caragols")
            CampData("Data", a.data, { a = a.copy(data = it) }, Modifier.weight(1f), potEditar)
        }

        Bloc(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Subtitol("Com es reparteix", Modifier.weight(1f))
                Interruptor(listOf("A parts iguals", "Segons el que ha pres"), a.mode.ordinal, { a = a.copy(mode = ModeRepartiment.entries[it]) }, potEditar)
            }
            Suau(
                if (a.mode == ModeRepartiment.IGUALS) "Cada persona paga el mateix. Els convidats compten com una persona més i van al compte del soci que els porta."
                else "Dona a cadascú les parts que ha pres: 1 és una ració normal, 0,5 mitja, 2 el doble. Si porta convidats, suma'ls a les seves parts.",
            )
        }

        Subtitol("Qui hi ve")
        Taula {
            FilaTaula(listOf({ Cap("Soci") }, { Cap(if (a.mode == ModeRepartiment.IGUALS) "Convidats" else "Parts") }, { Cap("") }), listOf(4f, 2f, 1f), capcalera = true)
            a.assistents.forEachIndexed { i, x ->
                FilaTaula(
                    listOf(
                        { Cel(nom(x.sociId)) },
                        {
                            if (a.mode == ModeRepartiment.IGUALS) Pas(x.convidats.toString(), potEditar,
                                { a = a.copy(assistents = a.assistents.toMutableList().also { l -> l[i] = x.copy(convidats = maxOf(0, x.convidats - 1)) }) },
                                { a = a.copy(assistents = a.assistents.toMutableList().also { l -> l[i] = x.copy(convidats = x.convidats + 1) }) })
                            else Pas(x.parts.toString().removeSuffix(".0").replace('.', ','), potEditar,
                                { a = a.copy(assistents = a.assistents.toMutableList().also { l -> l[i] = x.copy(parts = maxOf(0.0, x.parts - 0.5)) }) },
                                { a = a.copy(assistents = a.assistents.toMutableList().also { l -> l[i] = x.copy(parts = x.parts + 0.5) }) })
                        },
                        { if (potEditar) BotoRodo(Icons.Default.Close, "Treu ${nom(x.sociId)}") { a = a.copy(assistents = a.assistents.filterIndexed { j, _ -> j != i }) } },
                    ),
                    listOf(4f, 2f, 1f),
                )
            }
        }
        if (potEditar && disponibles.isNotEmpty()) {
            Desplegable(listOf<Soci?>(null) + disponibles, null, { it?.nomComplet ?: "+ Afegir un soci…" }, { s -> if (s != null) a = a.copy(assistents = a.assistents + Assistent(s.id)) }, Modifier.width(360.dp))
        }

        Subtitol("Despeses de l'àpat")
        Suau("El que algú ha pagat de la seva butxaca per a l'àpat. Se li descomptarà del que li toca.")
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
                Suau("Total ${euros(r.total)}" + if (r.perUnitat > 0) ", ${euros(Math.round(r.perUnitat))} per ${if (a.mode == ModeRepartiment.IGUALS) "persona" else "part"}" else "")
            }
            FilaTaula(listOf({ Cap("Soci") }, { Cap(if (a.mode == ModeRepartiment.IGUALS) "Persones" else "Parts", true) }, { Cap("Li toca", true) }, { Cap("Resultat", true) }), listOf(3f, 1f, 1.2f, 2f), capcalera = true)
            r.quotes.forEach { q ->
                FilaTaula(
                    listOf(
                        { Column { Cel(nom(q.sociId)); if (q.pagat > 0) Suau("Ha pagat ${euros(q.pagat)}", mida = 13.sp) } },
                        { Cel(q.unitats.toString().removeSuffix(".0").replace('.', ','), dreta = true) },
                        { Cel(euros(q.quota), dreta = true) },
                        { Cel(if (q.saldo < 0) "se li torna ${euros(-q.saldo)}" else "paga ${euros(q.saldo)}", dreta = true, negreta = true, color = if (q.saldo < 0) C.pampol else C.most) },
                    ),
                    listOf(3f, 1f, 1.2f, 2f),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (potEditar) Boto("Desar", { model.accio("Àpat desat", onFet) { model.repo.desaApat(a) } })
            if (potTancar) Boto("Passar als comptes del mes", { confirmaTancar = true }, tipus = Tipus.VERD, enabled = r.total > 0 && a.assistents.isNotEmpty())
            if (soci.esJunta && a.estat == EstatApat.TANCAT) Boto("Reobrir", { model.accio("Àpat reobert", onFet) { model.repo.desaApat(a.copy(estat = EstatApat.OBERT)) } }, tipus = Tipus.FANTASMA)
            if (existeix && a.estat == EstatApat.OBERT && (a.organitzadorId == soci.id || soci.esJunta)) Boto("Eliminar", { confirmaEliminar = true }, tipus = Tipus.FANTASMA)
        }
    }

    if (confirmaTancar) Confirmacio(
        "Passar als comptes", "Les quotes d'aquest àpat sortiran a l'informe del mes de cada soci. Després ja no el podreu modificar.", "Passar als comptes",
        onConfirma = { confirmaTancar = false; model.accio("Àpat passat als comptes", onFet) { model.repo.desaApat(a.copy(estat = EstatApat.TANCAT)) } },
        onCancela = { confirmaTancar = false },
    )
    if (confirmaEliminar) Confirmacio(
        "Eliminar l'àpat", "S'esborrarà «${a.titol}» amb els assistents i les despeses.", "Eliminar",
        onConfirma = { confirmaEliminar = false; model.accio("Àpat eliminat", onFet) { model.repo.eliminaApat(a.id) } },
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
