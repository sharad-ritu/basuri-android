package np.basuri.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Both tools stay composed, so starting a pulse never interrupts the drone. */
@Composable internal fun ToolsScreen(flute: Flute, onRhythmPracticed: (Long) -> Unit) {
    val context = LocalContext.current
    val focus = remember(context) { PracticeAudioFocus(context) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.padding(top = 16.dp)) {
            Text("Tools", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text("A drone and a steady pulse for daily practice.", color = Muted,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
        }
        TanpuraTool(flute, focus)
        MetronomeTool(onRhythmPracticed, focus)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable internal fun PracticeToolPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color(0xFFE3E7F1))) {
        Column(Modifier.fillMaxWidth().padding(18.dp), content = content)
    }
}
