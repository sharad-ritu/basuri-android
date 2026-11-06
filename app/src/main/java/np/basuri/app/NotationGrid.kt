package np.basuri.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Each vibhag gets a row. Equal-width columns preserve one column per beat at every font scale. */
@Composable internal fun NotationTaalGrid(
    taal: Taal,
    beats: List<NotationBeat>,
    script: NotationScript = NotationScript.Symbols,
    currentBeat: Int = -1,
) {
    val density = LocalDensity.current
    val hasPair = beats.any { it is NotationBeat.Notes && it.following.isNotEmpty() }
    val minimumCell = with(density) {
        (if (hasPair) {
            if (script == NotationScript.Names) 112 else 88
        } else {
            if (script == NotationScript.Names) 76 else 60
        }).sp.toDp()
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var firstBeat = 0
        taal.vibhags.forEach { vibhag ->
            val start = firstBeat
            val end = start + vibhag.bols.size
            Text("Beats ${start + 1}–$end", color = Muted, style = MaterialTheme.typography.labelMedium)
            BoxWithConstraints(Modifier.fillMaxWidth().background(Mist, RoundedCornerShape(12.dp))) {
                val availableWidth = maxWidth
                val cellWidth = maxOf(minimumCell, (availableWidth - 16.dp) / vibhag.bols.size)
                val scroll = rememberScrollState()
                Column {
                    Row(Modifier.horizontalScroll(scroll).height(IntrinsicSize.Min).padding(vertical = 8.dp)) {
                        vibhag.bols.forEachIndexed { index, bol ->
                            val beatIndex = start + index
                            val mark = taal.markAt(beatIndex)
                            val current = beatIndex == currentBeat
                            val highlight = if (current) Modifier.background(Bamboo.copy(alpha = .32f), RoundedCornerShape(10.dp))
                                .border(2.dp, Indigo, RoundedCornerShape(10.dp)) else Modifier
                            Column(Modifier.width(cellWidth).then(highlight).padding(vertical = 4.dp).semantics {
                                if (current) stateDescription = "Sounding beat"
                            }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(mark?.symbol ?: " ", color = Indigo, style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.clearAndSetSemantics {
                                        contentDescription = if (mark == null) "" else when (mark.mark) {
                                            TaalMark.Sam -> "Sam, first beat of the cycle"
                                            TaalMark.Khali -> "Khali, wave at section start"
                                            TaalMark.Tali -> "Tali ${mark.taliNumber}, clap at section start"
                                        }
                                    })
                                Text("${beatIndex + 1}", color = Muted, style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.semantics { contentDescription = "Beat ${beatIndex + 1}" })
                                Box(Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                                    NotationBeatGlyph(beats[beatIndex], script)
                                }
                                Text(bol, color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp).semantics { contentDescription = "Tabla bol $bol" })
                            }
                        }
                        Canvas(Modifier.width(16.dp).fillMaxHeight().clearAndSetSemantics { contentDescription = "End of vibhag" }) {
                            drawLine(Indigo.copy(alpha = .45f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1.dp.toPx())
                        }
                    }
                    if (cellWidth * vibhag.bols.size + 16.dp > availableWidth) {
                        Text("Swipe across this section to read every beat.", color = Muted,
                            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
            firstBeat = end
        }
    }
}

@Composable internal fun NotationBeatGlyph(beat: NotationBeat, script: NotationScript) {
    when (beat) {
        NotationBeat.Hold -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NotationHoldGlyph(script)
            Spacer(Modifier.height(12.dp))
        }
        is NotationBeat.Notes -> {
            val grouped = beat.following.isNotEmpty()
            Column(Modifier.width(IntrinsicSize.Max).then(if (grouped) Modifier.semantics {
                contentDescription = "${beat.tones.size} notes sharing one beat equally"
            } else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.Center) {
                    beat.tones.forEach { tone -> NotationGlyph(tone, script = script) }
                }
                Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                    if (grouped) {
                        val inset = 6.dp.toPx()
                        val curve = Path().apply {
                            moveTo(inset, 1.dp.toPx())
                            cubicTo(inset, size.height, size.width - inset, size.height, size.width - inset, 1.dp.toPx())
                        }
                        drawPath(curve, Indigo, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}
