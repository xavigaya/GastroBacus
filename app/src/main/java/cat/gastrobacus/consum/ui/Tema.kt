package cat.gastrobacus.consum.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cat.gastrobacus.consum.R
import cat.gastrobacus.consum.dades.Categoria

/* Paleta: granat de most, verd de pàmpol, ambre de cervesa, roure fosc i pedra verdosa. */
@Immutable
data class Colors(
    val most: Color, val mostSuau: Color, val pampol: Color, val pampolSuau: Color,
    val ambre: Color, val ambreSuau: Color, val roure: Color, val roure2: Color,
    val pedra: Color, val superficie: Color, val vora: Color, val error: Color, val errorSuau: Color,
    val capcalera: Color, val sobreMost: Color,
)

val ColorsClars = Colors(
    most = Color(0xFF5E1A3A), mostSuau = Color(0xFFF3E3EA), pampol = Color(0xFF4E6B2E), pampolSuau = Color(0xFFE3EBD3),
    ambre = Color(0xFFD99A1E), ambreSuau = Color(0xFFFBEFD2), roure = Color(0xFF2B2118), roure2 = Color(0xFF5C5046),
    pedra = Color(0xFFEEF0E8), superficie = Color(0xFFFFFFFF), vora = Color(0xFFD8DACF), error = Color(0xFFB3261E),
    errorSuau = Color(0xFFFBE4E2), capcalera = Color(0xFF5E1A3A), sobreMost = Color.White,
)
val ColorsFoscos = Colors(
    most = Color(0xFFD98FB0), mostSuau = Color(0xFF3A1A29), pampol = Color(0xFFA9C47E), pampolSuau = Color(0xFF26301A),
    ambre = Color(0xFFF0B84A), ambreSuau = Color(0xFF3A2C10), roure = Color(0xFFF1ECE6), roure2 = Color(0xFFBDB3A9),
    pedra = Color(0xFF17140F), superficie = Color(0xFF221E18), vora = Color(0xFF3B352C), error = Color(0xFFFF8A80),
    errorSuau = Color(0xFF3D1A17), capcalera = Color(0xFF2E1020), sobreMost = Color(0xFF1B0F15),
)

val LocalColors = staticCompositionLocalOf { ColorsClars }
val C: Colors @Composable get() = LocalColors.current

fun colorCategoria(c: Categoria): Color = when (c) {
    Categoria.CERVESA -> Color(0xFFD99A1E)
    Categoria.VI -> Color(0xFF7D2348)
    Categoria.REFRESCOS -> Color(0xFFC4472D)
    Categoria.AIGUA -> Color(0xFF3B7EA1)
    Categoria.CAFE -> Color(0xFF6B4A2F)
    Categoria.PICAR -> Color(0xFF4E6B2E)
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Bricolage = FontFamily(
    Font(R.font.bricolage, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.Setting("opsz", 24f))),
    Font(R.font.bricolage, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.Setting("opsz", 36f))),
    Font(R.font.bricolage, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800), FontVariation.Setting("opsz", 48f))),
)
val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

private val Tipografia = Typography(
    displayLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, lineHeight = 60.sp),
    headlineLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Atkinson, fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Atkinson, fontSize = 16.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = Atkinson, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    labelMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 14.sp),
)

/** Estil per a imports i xifres que s'alineen en columnes. */
val Xifra = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")

@Composable
fun TemaGastroBacus(fosc: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (fosc) ColorsFoscos else ColorsClars
    val esquema = if (fosc) darkColorScheme(
        primary = c.most, onPrimary = c.sobreMost, secondary = c.pampol, background = c.pedra, surface = c.superficie,
        onBackground = c.roure, onSurface = c.roure, error = c.error, outline = c.vora, surfaceVariant = c.pedra, onSurfaceVariant = c.roure2,
    ) else lightColorScheme(
        primary = c.most, onPrimary = c.sobreMost, secondary = c.pampol, background = c.pedra, surface = c.superficie,
        onBackground = c.roure, onSurface = c.roure, error = c.error, outline = c.vora, surfaceVariant = c.pedra, onSurfaceVariant = c.roure2,
    )
    CompositionLocalProvider(LocalColors provides c) {
        MaterialTheme(colorScheme = esquema, typography = Tipografia, content = content)
    }
}
