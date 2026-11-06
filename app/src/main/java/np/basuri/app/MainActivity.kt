package np.basuri.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.*


class MainActivity : ComponentActivity() {
    private val vm: BasuriViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) vm.openFromNotification(intent?.getStringExtra(Reminders.OPEN))
        setContent {
            BasuriTheme {
                val screen = vm.screen.collectAsStateWithLifecycle().value
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = screen != Screen.Welcome
                        isAppearanceLightNavigationBars = screen != Screen.Welcome
                    }
                }
                BasuriApp(vm)
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.openFromNotification(intent.getStringExtra(Reminders.OPEN))
    }
}

@Composable private fun BasuriApp(vm: BasuriViewModel) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val audio by vm.audio.collectAsStateWithLifecycle()
    val detecting = vm.saDetection.collectAsStateWithLifecycle().value is SaDetection.Listening
    KeepScreenOn(audio is AudioStatus.Listening || audio is AudioStatus.Playing || detecting)
    // Roll the checklist, streak, and calendar over at midnight while the app stays open.
    LaunchedEffect(today) {
        val midnight = today.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        kotlinx.coroutines.delay((midnight - System.currentTimeMillis()).coerceAtLeast(0) + 1000)
        vm.refreshDay()
    }
    // Each tab keeps its own saved state (metronome tempo, tanpura string, notation section) while you switch away.
    val tabs = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val owner = LocalLifecycleOwner.current
    var exitLesson by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.stopAudio()
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshDay()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); vm.stopAudio() }
    }
    BackHandler(screen != Screen.Home && screen != Screen.Welcome) {
        when (screen) {
            Screen.Lesson -> if (vm.step.value == 0 && vm.guideIndex.value > 0) vm.previousGuide() else { vm.stopAudio(); exitLesson = true }
            Screen.Setup -> vm.navigate(Screen.Welcome)
            Screen.Settings -> vm.closeSettings()
            else -> vm.navigate(Screen.Home)
        }
    }
    Box(Modifier.fillMaxSize().background(if (screen == Screen.Welcome) Ink else Paper).safeDrawingPadding()) {
        when (screen) {
            Screen.Welcome -> WelcomeScreen { vm.navigate(Screen.Setup) }
            Screen.Setup -> Configuration(progress, true, { vm.navigate(Screen.Welcome) }, vm) { f, g, detected -> vm.configure(f, g, true, detected); vm.beginLesson(0) }
            Screen.Home, Screen.Practice, Screen.Metronome, Screen.Notation, Screen.Settings -> Scaffold(containerColor = Paper,
                bottomBar = { BottomNavigation(screen) { if (it == Screen.Settings) vm.openSettings() else vm.navigate(it) } }) { padding ->
                // My flute stays out of the tab cache: leaving it discards unsaved edits, as before.
                Box(Modifier.padding(padding)) { if (screen == Screen.Settings) Configuration(progress, false, vm::closeSettings, vm) { f, g, detected -> vm.configure(f, g, false, detected) }
                else tabs.SaveableStateProvider(screen.name) {
                    when (screen) {
                        Screen.Home -> HomeScreen(progress, vm::beginLesson, vm::openSettings, vm::startWarmup, { vm.navigate(Screen.Metronome) })
                        Screen.Practice -> StudioScreen(vm, progress, vm::openSettings)
                        Screen.Metronome -> ToolsScreen(progress.flute, vm::addRhythmPractice)
                        Screen.Notation -> NotationScreen(progress.flute)
                        else -> Unit
                    }
                } }
            }
            Screen.Lesson -> LessonScreen(vm, progress) { vm.stopAudio(); exitLesson = true }
            Screen.Complete -> {
                val summary by vm.summary.collectAsStateWithLifecycle()
                CompletionScreen(progress, vm.currentLesson, summary, { vm.navigate(Screen.Home) }, {
                    val index = vm.lessonIndex.value + 1
                    if (index < lessons.size) vm.beginLesson(index) else vm.navigate(Screen.Practice)
                }, vm.lessonIndex.value == lessons.lastIndex, vm::setReminder, vm::declineReminder, vm::openSettings, vm::dismissTunePrompt)
            }
        }
    }
    if (exitLesson) AlertDialog(onDismissRequest = { exitLesson = false },
        title = { Text("Leave this lesson?") }, text = { Text("This session will not be counted. Your completed lessons are saved." +
            if (progress.streakAtRisk) " Finish a lesson today to keep your ${progress.streak}-day streak." else "") },
        confirmButton = { TextButton({ exitLesson = false; vm.navigate(Screen.Home) }) { Text("Leave lesson") } },
        dismissButton = { TextButton({ exitLesson = false }) { Text("Keep practicing") } })
}

@Composable private fun BottomNavigation(screen: Screen, navigate: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp, windowInsets = WindowInsets(0), modifier = Modifier.height(72.dp)) {
        listOf(Triple(Screen.Home, "Learn", Icons.Rounded.Route), Triple(Screen.Practice, "Practice", Icons.Rounded.GraphicEq),
            Triple(Screen.Metronome, "Tools", Icons.Rounded.AvTimer), Triple(Screen.Notation, "Notation", Icons.Rounded.LibraryMusic),
            Triple(Screen.Settings, "My flute", Icons.Rounded.Tune)).forEach { (target, label, icon) ->
            NavigationBarItem(selected = screen == target, onClick = { navigate(target) },
                icon = { Icon(icon, null, Modifier.size(22.dp)) }, label = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Indigo, selectedTextColor = Indigo,
                    indicatorColor = Mist, unselectedIconColor = Muted, unselectedTextColor = Muted))
        }
    }
}

@Composable private fun WelcomeScreen(onBegin: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("basuri", fontFamily = Rounded, fontSize = 26.sp, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text("बाँसुरी", color = Bamboo, fontSize = 22.sp)
        }
        FluteArt(Modifier.fillMaxWidth().height(220.dp), diagonal = true)
        Text("Your first note\nstarts here.", style = MaterialTheme.typography.displayLarge, color = Color.White)
        Text("Learn the basuri in short, guided lessons. Just you, your flute, and a little practice.", style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFD3DAED), modifier = Modifier.padding(top = 14.dp, bottom = 24.dp))
        listOf(Icons.Rounded.Route to "Short guided lessons, from first sound to a full octave", Icons.Rounded.Hearing to "Hear a note, then find it with live pitch feedback",
            Icons.Rounded.LocalFireDepartment to "A daily routine with streaks and reminders", Icons.Rounded.Lock to "Offline, with no account needed").forEach { (icon, text) ->
            Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Bamboo, modifier = Modifier.size(22.dp))
                Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Get started", onBegin, gold = true)
        Text("For a six-hole Nepali basuri", color = Color(0xFFD3DAED), style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
    }
}

@Composable internal fun TopLine(title: String, onBack: () -> Unit, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(end = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun HelpSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Paper) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            content()
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Got it", onDismiss)
        }
    }
}

@Composable internal fun Choice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color by animateColorAsState(if (selected) Indigo else Color.White, label = "Choice")
    Surface(onClick = onClick, color = color, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (selected) Indigo else Color(0xFFD9DEEB)),
        modifier = modifier.heightIn(min = 48.dp).semantics { this.selected = selected }) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else Ink, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}
