package np.basuri.app

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF222D50)
val Indigo = Color(0xFF4C5FB5)
val Bamboo = Color(0xFFE7BB72)
val Paper = Color(0xFFF7F8FC)
val Mist = Color(0xFFEBEEF8)
val Muted = Color(0xFF586580)
val Green = Color(0xFF236957)
val GreenWash = Color(0xFFE4F2EB)
val Rounded = FontFamily(Typeface.create("sans-serif-rounded", Typeface.BOLD))

private val type = Typography(
    displayLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1.4).sp),
    headlineLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-.6).sp),
    headlineMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
)

@Composable fun BasuriTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        // Every Material role is mapped to a Basuri token so default chips, sliders, and sheets never fall back to Material lavender.
        colorScheme = lightColorScheme(primary = Ink, onPrimary = Color.White, primaryContainer = Mist, onPrimaryContainer = Ink,
            inversePrimary = Bamboo, secondary = Indigo, onSecondary = Color.White, secondaryContainer = Mist, onSecondaryContainer = Ink,
            tertiary = Green, onTertiary = Color.White, tertiaryContainer = GreenWash, onTertiaryContainer = Green,
            background = Paper, onBackground = Ink, surface = Color.White, onSurface = Ink, surfaceVariant = Mist, onSurfaceVariant = Muted,
            surfaceTint = Indigo, inverseSurface = Ink, inverseOnSurface = Color.White, outline = Color(0xFFBAC3D6), outlineVariant = Color(0xFFDDE2EE),
            surfaceBright = Color.White, surfaceDim = Mist, surfaceContainerLowest = Color.White, surfaceContainerLow = Paper,
            surfaceContainer = Paper, surfaceContainerHigh = Mist, surfaceContainerHighest = Mist,
            error = Color(0xFFAC3543)), typography = type) {
        CompositionLocalProvider(LocalContentColor provides Ink, content = content)
    }
}

@Composable fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, gold: Boolean = false) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled,
        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (gold) Bamboo else Indigo, contentColor = if (gold) Ink else Color.White)) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** A bamboo flute with a separate blowing hole and exactly six finger holes. */
@Composable fun FluteArt(modifier: Modifier = Modifier, diagonal: Boolean = false) {
    Canvas(modifier.clipToBounds().semantics { contentDescription = "Bamboo basuri with a blowing hole and six finger holes" }) {
        val w = size.width
        val h = size.height
        if (diagonal) {
            drawCircle(Color.White.copy(.04f), w * .38f, Offset(w * .52f, h * .48f))
            drawCircle(Bamboo.copy(.16f), w * .3f, Offset(w * .52f, h * .48f), style = Stroke(1.dp.toPx()))
            drawCircle(Bamboo.copy(.09f), w * .42f, Offset(w * .52f, h * .48f), style = Stroke(1.dp.toPx()))
        }
        val wave = Path().apply {
            moveTo(w * .03f, h * .68f)
            cubicTo(w * .25f, h * .27f, w * .7f, h * .9f, w * .98f, h * .28f)
        }
        drawPath(wave, Bamboo.copy(.45f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        rotate(if (diagonal) -38f else -12f) {
            val left = w * .08f
            val top = h * .44f
            val length = w * .84f
            val thickness = h * .11f
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFF1D5A0), Bamboo, Color(0xFFB7813D)), startY = top, endY = top + thickness),
                Offset(left, top), androidx.compose.ui.geometry.Size(length, thickness), CornerRadius(thickness / 2))
            listOf(.035f, .08f, .9f, .94f).forEach { x ->
                drawLine(Ink.copy(.8f), Offset(left + length * x, top), Offset(left + length * x, top + thickness), thickness * .075f)
            }
            drawOval(Ink, Offset(left + length * .16f, top + thickness * .27f), androidx.compose.ui.geometry.Size(thickness * .52f, thickness * .45f))
            repeat(6) { i -> drawCircle(Ink, thickness * .19f, Offset(left + length * (.36f + i * .082f), top + thickness * .5f)) }
            drawLine(Color.White.copy(.4f), Offset(left + length * .09f, top + thickness * .15f), Offset(left + length * .88f, top + thickness * .15f), 1.dp.toPx())
        }
        listOf(Offset(w * .2f, h * .18f), Offset(w * .84f, h * .72f), Offset(w * .72f, h * .14f)).forEach { p ->
            drawLine(Bamboo, p - Offset(4.dp.toPx(), 0f), p + Offset(4.dp.toPx(), 0f), 1.dp.toPx())
            drawLine(Bamboo, p - Offset(0f, 4.dp.toPx()), p + Offset(0f, 4.dp.toPx()), 1.dp.toPx())
        }
    }
}

/** A swar name with its octave dot: below for mandra, above for taar, as in Bhatkhande notation. */
@Composable fun SwarLabel(swar: Swar, style: TextStyle, color: Color = Ink, modifier: Modifier = Modifier) {
    Text(swar.label, style = style, color = color, modifier = modifier
        .semantics { contentDescription = swar.spoken }
        .drawBehind {
            if (swar.octave == Octave.Madhya) return@drawBehind
            val r = (style.fontSize.toPx() * .07f).coerceAtLeast(2.dp.toPx())
            val x = size.width * (if (swar.label.length > 2) .32f else .26f)
            drawCircle(color, r, Offset(x, if (swar.octave == Octave.Taar) r * 1.2f else size.height - r * 1.2f))
        })
}

@Composable fun Fingering(swar: Swar, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Color(0xFFFBF2E2), RoundedCornerShape(16.dp)).padding(14.dp)
        .semantics(mergeDescendants = true) {
            contentDescription = "${swar.spoken}. ${fingeringCue(swar)}. All remaining holes open. Count from the blowing hole on the left." +
                if (swar.overblown) " Overblow: use a faster, narrower stream of air." else ""
        }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("From the blowing hole", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f).padding(end = 10.dp))
            Text("● Closed  ○ Open", color = Ink, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(12.dp))
        Box {
            Canvas(Modifier.fillMaxWidth().height(30.dp)) {
                drawRoundRect(Bamboo.copy(.55f), Offset(0f, 4.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 22.dp.toPx()), CornerRadius(11.dp.toPx()))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(6) { index ->
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Canvas(Modifier.size(30.dp)) {
                            drawCircle(if (index < swar.closed) Ink else Color(0xFFFFFCF5))
                            drawCircle(Ink, style = Stroke(1.5.dp.toPx()))
                            if (index < swar.closed) drawCircle(Bamboo, radius = 3.dp.toPx())
                        }
                        Text("${index + 1}", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        if (swar.overblown || swar.octave == Octave.Mandra) Row(Modifier.padding(top = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(Modifier.background(if (swar.overblown) Indigo else Bamboo, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(if (swar.overblown) "Overblow" else "Low octave", color = if (swar.overblown) Color.White else Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Text(if (swar.overblown) "Same fingers, faster and narrower air" else "Slower, warmer air; seal every hole",
                color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable fun BreathArt(modifier: Modifier = Modifier) {
    Canvas(modifier.clipToBounds().semantics { contentDescription = "A gentle air stream moves across the far edge of the blowing hole." }) {
        val w = size.width
        val h = size.height
        val hole = Offset(w * .63f, h * .62f)
        drawRoundRect(Bamboo, Offset(w * .32f, h * .48f), androidx.compose.ui.geometry.Size(w * .62f, h * .3f), CornerRadius(h * .15f))
        drawOval(Ink, hole - Offset(w * .075f, h * .065f), androidx.compose.ui.geometry.Size(w * .15f, h * .13f))
        drawOval(Color(0xFFFFFCF5), hole - Offset(w * .06f, h * .045f), androidx.compose.ui.geometry.Size(w * .12f, h * .09f))
        repeat(3) { line ->
            val y = h * (.19f + line * .055f)
            val path = Path().apply {
                moveTo(w * .13f, y)
                cubicTo(w * .36f, y - h * .04f, w * .48f, h * .38f, w * .73f, h * .54f)
            }
            drawPath(path, Indigo.copy(1f - line * .18f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        val lip = Path().apply {
            moveTo(w * .03f, h * .12f)
            quadraticTo(w * .2f, h * .14f, w * .12f, h * .23f)
            quadraticTo(w * .2f, h * .32f, w * .03f, h * .36f)
        }
        drawPath(lip, Ink, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Views currently held awake, counted so two tools playing together don't release each other's hold. */
private val screenOnHolds = java.util.WeakHashMap<android.view.View, Int>()

/**
 * Keeps the display on while [active], so a learner holding the flute doesn't lose the metronome,
 * tanpura, or microphone to the screen timeout. Audio still stops when the app leaves the foreground.
 */
@Composable fun KeepScreenOn(active: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view, active) {
        if (!active) return@DisposableEffect onDispose {}
        screenOnHolds[view] = (screenOnHolds[view] ?: 0) + 1
        view.keepScreenOn = true
        onDispose {
            val left = (screenOnHolds[view] ?: 1) - 1
            if (left <= 0) { screenOnHolds.remove(view); view.keepScreenOn = false } else screenOnHolds[view] = left
        }
    }
}
