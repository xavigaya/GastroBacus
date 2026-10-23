package cat.gastrobacus.consum.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.dades.nomMes

/** El logotip: un raïm que dibuixa una B. */
@Composable
fun Logo(mida: Dp = 40.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(mida)) {
        val k = size.width / 64f
        drawRoundRect(Color(0xFF5E1A3A), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(14 * k))
        val tija = Path().apply { moveTo(22 * k, 11 * k); cubicTo(22 * k, 7 * k, 25 * k, 5 * k, 28 * k, 4 * k) }
        drawPath(tija, Color(0xFFB7C98A), style = Stroke(2.4f * k, cap = StrokeCap.Round))
        val fulla = Path().apply {
            moveTo(28 * k, 4 * k); cubicTo(34 * k, 2 * k, 39 * k, 5 * k, 40 * k, 9 * k); cubicTo(35 * k, 11 * k, 30 * k, 9 * k, 28 * k, 4 * k); close()
        }
        drawPath(fulla, Color(0xFF7E9A4A))
        listOf(21f to 17f, 21f to 28.5f, 21f to 40f, 21f to 51.5f, 32.5f to 15.5f, 41.5f to 22f, 32.5f to 28.5f, 33.5f to 40f, 44f to 46f, 33.5f to 52f)
            .forEach { (x, y) -> drawCircle(Color(0xFFF3E3EA), 5.6f * k, Offset(x * k, y * k)) }
        listOf(19f to 15f, 30.5f to 13.5f, 31.5f to 38f).forEach { (x, y) -> drawCircle(Color.White.copy(alpha = .55f), 1.4f * k, Offset(x * k, y * k)) }
    }
}

@Composable
fun BarraSuperior(contingut: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().background(C.capcalera).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Logo(40.dp)
        Text("GastroBacus", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.weight(1f))
        contingut()
    }
}

enum class Tipus { PRINCIPAL, SECUNDARI, FANTASMA, PERILL, VERD, CAPCALERA }

@Composable
fun Boto(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tipus: Tipus = Tipus.PRINCIPAL,
    gran: Boolean = false,
    petit: Boolean = false,
    enabled: Boolean = true,
    icona: ImageVector? = null,
) {
    val alt = if (gran) 64.dp else if (petit) 40.dp else 52.dp
    val forma = RoundedCornerShape(if (gran) 18.dp else 12.dp)
    val estil = if (gran) MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp) else if (petit) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
    val pad = PaddingValues(horizontal = if (petit) 12.dp else 20.dp)
    val cos: @Composable RowScope.() -> Unit = {
        if (icona != null) { Icon(icona, null, Modifier.size(if (petit) 18.dp else 22.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = estil, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    when (tipus) {
        Tipus.SECUNDARI, Tipus.FANTASMA -> OutlinedButton(
            onClick, modifier.defaultMinSize(minHeight = alt), enabled, forma,
            ButtonDefaults.outlinedButtonColors(contentColor = if (tipus == Tipus.SECUNDARI) C.most else C.roure),
            border = BorderStroke(2.dp, if (!enabled) C.vora.copy(alpha = .5f) else if (tipus == Tipus.SECUNDARI) C.most else C.vora),
            contentPadding = pad, content = cos,
        )
        else -> Button(
            onClick, modifier.defaultMinSize(minHeight = alt), enabled, forma,
            ButtonDefaults.buttonColors(
                containerColor = when (tipus) { Tipus.PERILL -> C.error; Tipus.VERD -> C.pampol; Tipus.CAPCALERA -> Color.White.copy(alpha = .16f); else -> C.most },
                contentColor = if (tipus == Tipus.CAPCALERA) Color.White else C.sobreMost,
                disabledContainerColor = C.most.copy(alpha = .35f), disabledContentColor = C.sobreMost.copy(alpha = .8f),
            ),
            contentPadding = pad, content = cos,
        )
    }
}

@Composable
fun Bloc(modifier: Modifier = Modifier, padding: Dp = 20.dp, vora: Color = C.vora, contingut: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, RoundedCornerShape(18.dp), color = C.superficie, border = BorderStroke(1.dp, vora)) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp), content = contingut)
    }
}

enum class ToEtiqueta { NEUTRE, AMBRE, VERD, MOST, VERMELL }

@Composable
fun Etiqueta(text: String, to: ToEtiqueta = ToEtiqueta.NEUTRE) {
    val (fons, vora) = when (to) {
        ToEtiqueta.NEUTRE -> C.pedra to C.vora
        ToEtiqueta.AMBRE -> C.ambreSuau to C.ambre
        ToEtiqueta.VERD -> C.pampolSuau to C.pampol
        ToEtiqueta.MOST -> C.mostSuau to C.most
        ToEtiqueta.VERMELL -> C.errorSuau to C.error
    }
    Text(
        text, style = MaterialTheme.typography.labelMedium, color = C.roure,
        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(fons).border(1.dp, vora, RoundedCornerShape(99.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

enum class ToAvis { AVIS, ERROR, OK }

@Composable
fun Avis(text: String, to: ToAvis = ToAvis.AVIS, modifier: Modifier = Modifier, accio: (@Composable () -> Unit)? = null) {
    val (fons, vora) = when (to) { ToAvis.AVIS -> C.ambreSuau to C.ambre; ToAvis.ERROR -> C.errorSuau to C.error; ToAvis.OK -> C.pampolSuau to C.pampol }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(fons),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).height(56.dp).background(vora))
        Text(text, Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, color = C.roure)
        if (accio != null) Box(Modifier.padding(end = 10.dp)) { accio() }
    }
}

@Composable
fun Avatar(inicials: String, mida: Dp = 48.dp, fons: Color = C.mostSuau, color: Color = C.most) {
    Box(Modifier.size(mida).clip(CircleShape).background(fons), contentAlignment = Alignment.Center) {
        Text(inicials, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = (mida.value * 0.36f).sp), color = color)
    }
}

@Composable
fun Camp(
    etiqueta: String,
    valor: String,
    onCanvi: (String) -> Unit,
    modifier: Modifier = Modifier,
    ajuda: String? = null,
    placeholder: String? = null,
    teclat: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    linies: Int = 1,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = C.roure)
        OutlinedTextField(
            valor, onCanvi, Modifier.fillMaxWidth(),
            singleLine = linies == 1, minLines = linies,
            placeholder = placeholder?.let { { Text(it, color = C.roure2) } },
            keyboardOptions = KeyboardOptions(keyboardType = teclat),
            visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = C.vora, focusedBorderColor = C.most, unfocusedContainerColor = C.superficie, focusedContainerColor = C.superficie),
        )
        if (ajuda != null) Text(ajuda, style = MaterialTheme.typography.bodySmall, color = C.roure2)
    }
}

@Composable
fun Casella(text: String, marcada: Boolean, onCanvi: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled) { onCanvi(!marcada) }.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(marcada, onCanvi, enabled = enabled, colors = CheckboxDefaults.colors(checkedColor = C.most))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun <T> Desplegable(
    opcions: List<T>,
    seleccionat: T,
    text: (T) -> String,
    onTria: (T) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String? = null,
    enabled: Boolean = true,
) {
    var obert by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (etiqueta != null) Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                { obert = true }, Modifier.defaultMinSize(minHeight = 52.dp).fillMaxWidth(), enabled,
                RoundedCornerShape(10.dp), ButtonDefaults.outlinedButtonColors(containerColor = C.superficie, contentColor = C.roure),
                border = BorderStroke(2.dp, C.vora), contentPadding = PaddingValues(start = 14.dp, end = 8.dp),
            ) {
                Text(text(seleccionat), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(obert, { obert = false }) {
                opcions.forEach { o ->
                    DropdownMenuItem({ Text(text(o), style = MaterialTheme.typography.bodyLarge) }, { onTria(o); obert = false })
                }
            }
        }
    }
}

@Composable
fun SelectorMes(mes: String, mesos: List<String>, onCanvi: (String) -> Unit, modifier: Modifier = Modifier) =
    Desplegable(mesos, mes, { nomMes(it) }, onCanvi, modifier.width(240.dp))

@Composable
fun Confirmacio(
    titol: String,
    text: String,
    confirmar: String,
    onConfirma: () -> Unit,
    onCancela: () -> Unit,
    perill: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onCancela,
        title = { Text(titol, style = MaterialTheme.typography.headlineMedium) },
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { Boto(confirmar, onConfirma, tipus = if (perill) Tipus.PERILL else Tipus.PRINCIPAL) },
        dismissButton = { TextButton(onCancela) { Text("Cancel·lar", style = MaterialTheme.typography.labelLarge, color = C.roure) } },
        containerColor = C.superficie,
    )
}

@Composable
fun Titol(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.headlineLarge, color = C.roure)

@Composable
fun Subtitol(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.headlineMedium, color = C.roure)

@Composable
fun Suau(text: String, modifier: Modifier = Modifier, mida: TextUnit = 16.sp) =
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium.copy(fontSize = mida), color = C.roure2)

@Composable
fun XifraText(text: String, mida: TextUnit = 18.sp, color: Color = C.roure, modifier: Modifier = Modifier) =
    Text(text, modifier, style = Xifra.copy(fontSize = mida), color = color)

/** Fila de taula senzilla amb pesos per columna. */
@Composable
fun FilaTaula(
    cels: List<@Composable () -> Unit>,
    pesos: List<Float>,
    capcalera: Boolean = false,
    onClick: (() -> Unit)? = null,
    atenuada: Boolean = false,
) {
    Column {
        Row(
            Modifier.fillMaxWidth()
                .then(if (capcalera) Modifier.background(C.pedra) else Modifier)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = if (capcalera) 10.dp else 8.dp)
                .then(if (atenuada) Modifier.background(Color.Transparent) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            cels.forEachIndexed { i, c -> Box(Modifier.weight(pesos[i]), contentAlignment = Alignment.CenterStart) { c() } }
        }
        HorizontalDivider(color = C.vora)
    }
}

@Composable
fun Cap(text: String, dreta: Boolean = false) =
    Text(text, Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelMedium, color = C.roure2, textAlign = if (dreta) androidx.compose.ui.text.style.TextAlign.End else null)

@Composable
fun Cel(text: String, dreta: Boolean = false, negreta: Boolean = false, color: Color = C.roure, ratllat: Boolean = false) =
    Text(
        text, Modifier.fillMaxWidth(),
        style = (if (dreta) Xifra.copy(fontSize = 16.sp, fontWeight = if (negreta) FontWeight.ExtraBold else FontWeight.Bold) else MaterialTheme.typography.bodyMedium.copy(fontWeight = if (negreta) FontWeight.Bold else FontWeight.Normal))
            .copy(textDecoration = if (ratllat) androidx.compose.ui.text.style.TextDecoration.LineThrough else null),
        color = color, textAlign = if (dreta) androidx.compose.ui.text.style.TextAlign.End else null,
        maxLines = 2, overflow = TextOverflow.Ellipsis,
    )

@Composable
fun Taula(modifier: Modifier = Modifier, contingut: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), RoundedCornerShape(12.dp), color = C.superficie, border = BorderStroke(1.dp, C.vora)) {
        Column(content = contingut)
    }
}

/** Barra horitzontal proporcional (consum per categoria, productes més demanats). */
@Composable
fun Barra(fraccio: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.height(12.dp).clip(RoundedCornerShape(99.dp)).background(C.pedra)) {
        Box(Modifier.fillMaxWidth(fraccio.coerceIn(0.02f, 1f)).height(12.dp).clip(RoundedCornerShape(99.dp)).background(color))
    }
}

@Composable
fun Interruptor(opcions: List<String>, seleccionat: Int, onTria: (Int) -> Unit, enabled: Boolean = true) {
    Row(
        Modifier.clip(RoundedCornerShape(99.dp)).border(2.dp, C.vora, RoundedCornerShape(99.dp)).background(C.superficie).padding(3.dp),
    ) {
        opcions.forEachIndexed { i, o ->
            val sel = i == seleccionat
            Text(
                o, style = MaterialTheme.typography.labelLarge, color = if (sel) C.pedra else C.roure,
                modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(if (sel) C.roure else Color.Transparent)
                    .clickable(enabled) { onTria(i) }.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

/** Fitxa seleccionable en forma de píndola (categories, convidats, lletres). */
@Composable
fun Pindola(text: String, seleccionada: Boolean, onClick: () -> Unit, punt: Color? = null, ambre: Boolean = false) {
    val fons = when { seleccionada && ambre -> C.ambreSuau; seleccionada -> C.roure; else -> C.superficie }
    val vora = when { seleccionada && ambre -> C.ambre; seleccionada -> C.roure; else -> C.vora }
    Row(
        Modifier.clip(RoundedCornerShape(99.dp)).background(fons).border(2.dp, vora, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick).defaultMinSize(minHeight = 48.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (punt != null) { Box(Modifier.size(12.dp).clip(CircleShape).background(punt)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (seleccionada && !ambre) C.pedra else C.roure)
    }
}


/** Camp de data amb calendari desplegable. La data és AAAA-MM-DD. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CampData(etiqueta: String, data: String, onCanvi: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var obert by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(
            { obert = true }, Modifier.defaultMinSize(minHeight = 56.dp).fillMaxWidth(), enabled,
            RoundedCornerShape(10.dp), ButtonDefaults.outlinedButtonColors(containerColor = C.superficie, contentColor = C.roure),
            border = BorderStroke(2.dp, C.vora), contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            Text(runCatching { cat.gastrobacus.consum.dades.dataLlarga(data) }.getOrDefault(data), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        }
    }
    if (obert) {
        val inicial = runCatching { java.time.LocalDate.parse(data).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
        val estat = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = inicial)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { obert = false },
            confirmButton = {
                Boto("D'acord", {
                    estat.selectedDateMillis?.let { onCanvi(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()) }
                    obert = false
                })
            },
            dismissButton = { TextButton({ obert = false }) { Text("Cancel·lar", color = C.roure) } },
        ) { androidx.compose.material3.DatePicker(estat) }
    }
}
