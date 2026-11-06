package np.basuri.app

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Marks follow the ink, not the text line box or the neighbouring beat number. */
@Composable internal fun NotationGlyph(
    tone: NotationTone,
    modifier: Modifier = Modifier,
    script: NotationScript = NotationScript.Symbols,
) {
    NotationSymbol(script.text(tone.pitch.swar), tone.description, script, tone, modifier)
}

@Composable internal fun NotationHoldGlyph(script: NotationScript) {
    NotationSymbol("-", "Continue the previous note for one more beat", script)
}

@Composable private fun NotationSymbol(
    text: String,
    description: String,
    script: NotationScript,
    tone: NotationTone? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // Resolve the same Rounded font token used by Compose, including Android's script fallback.
    val typeface = LocalFontFamilyResolver.current.resolve(Rounded).value as Typeface
    val textSize = with(density) { 24.sp.toPx() }
    val paint = remember(typeface, textSize) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSize
            color = Ink.toArgb()
        }
    }
    val bounds = remember(paint, text) { Rect().also { paint.getTextBounds(text, 0, text.length, it) } }
    // A common baseline and height for every swar in this script also aligns Hold cells.
    val scriptBounds = remember(paint, script) {
        (NotationSwar.entries.map(script::text) + "-").map { value ->
            Rect().also { paint.getTextBounds(value, 0, value.length, it) }
        }.let { Rect(0, it.minOf { b -> b.top }, 0, it.maxOf { b -> b.bottom }) }
    }
    val width = with(density) { maxOf(40.dp, bounds.width().toDp() + 12.dp) }
    val height = with(density) { (scriptBounds.bottom - scriptBounds.top).toDp() + 32.dp }
    Canvas(modifier.size(width, height).clearAndSetSemantics { contentDescription = description }) {
        val baseline = 16.dp.toPx() - scriptBounds.top
        val x = size.width / 2
        val top = baseline + bounds.top
        val bottom = baseline + bounds.bottom
        drawContext.canvas.nativeCanvas.drawText(text, x - (bounds.left + bounds.right) / 2f, baseline, paint)
        if (tone?.pitch?.alteration == NotationAlteration.Tivra) {
            drawLine(Indigo, Offset(x, top - 9.dp.toPx()), Offset(x, top - 3.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        }
        if (tone?.octave == NotationOctave.Taar) {
            val gap = if (tone.pitch.alteration == NotationAlteration.Tivra) 14.dp else 5.dp
            drawCircle(Indigo, 2.dp.toPx(), Offset(x, top - gap.toPx()))
        }
        if (tone?.pitch?.alteration == NotationAlteration.Komal) {
            val halfLine = bounds.width() * .38f
            drawLine(Indigo, Offset(x - halfLine, bottom + 3.dp.toPx()), Offset(x + halfLine, bottom + 3.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        }
        if (tone?.octave == NotationOctave.Mandra) {
            val gap = if (tone.pitch.alteration == NotationAlteration.Komal) 10.dp else 5.dp
            drawCircle(Indigo, 2.dp.toPx(), Offset(x, bottom + gap.toPx()))
        }
    }
}

@Composable internal fun NotationFingering(holes: List<NotationHole>, modifier: Modifier = Modifier) {
    val description = holes.mapIndexed { index, hole -> "hole ${index + 1} ${hole.description}" }.joinToString(", ")
    Column(modifier.fillMaxWidth().background(Bamboo.copy(alpha = .16f), RoundedCornerShape(16.dp))
        .padding(horizontal = 8.dp, vertical = 12.dp).clearAndSetSemantics {
            contentDescription = "Six finger holes, counted from the blowing hole on the left. $description."
        }) {
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            drawRoundRect(Bamboo.copy(alpha = .55f), Offset(0f, 7.dp.toPx()), Size(size.width, 22.dp.toPx()), CornerRadius(11.dp.toPx()))
            val cell = size.width / 6
            holes.forEachIndexed { index, hole ->
                drawNotationHole(hole, Offset(cell * (index + .5f), size.height / 2), minOf(15.dp.toPx(), cell * .36f))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            repeat(6) { index ->
                Text("${index + 1}", color = Muted, style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(top = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun NotationHoleLegend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NotationHole.entries.forEach { hole ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics {
                contentDescription = when (hole) {
                    NotationHole.Closed -> "Filled circle means closed"
                    NotationHole.Half -> "Half-filled circle means half covered"
                    NotationHole.Open -> "Empty circle means open"
                }
            }) {
                Canvas(Modifier.size(18.dp)) { drawNotationHole(hole, center, 8.dp.toPx()) }
                Text(when (hole) {
                    NotationHole.Closed -> "Closed"
                    NotationHole.Half -> "Half"
                    NotationHole.Open -> "Open"
                }, color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

private fun DrawScope.drawNotationHole(hole: NotationHole, at: Offset, radius: Float) {
    drawCircle(Paper, radius, at)
    when (hole) {
        NotationHole.Closed -> {
            drawCircle(Ink, radius, at)
            drawCircle(Bamboo, minOf(3.dp.toPx(), radius * .2f), at)
        }
        NotationHole.Half -> drawArc(Ink, 90f, 180f, true, at - Offset(radius, radius), Size(radius * 2, radius * 2))
        NotationHole.Open -> Unit
    }
    drawCircle(Ink, radius, at, style = Stroke(1.5.dp.toPx()))
}
