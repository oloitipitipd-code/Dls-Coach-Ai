package com.dlscoachai.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class Player(
    val name: String,
    val position: String,
    val rating: Int,
    val spe: Int = 50,
    val acc: Int = 50,
    val sta: Int = 50,
    val str: Int = 50,
    val con: Int = 50,
    val pas: Int = 50,
    val sho: Int = 50,
    val tac: Int = 50,
    val gkr: Int = 50,
    val gkh: Int = 50,
    val statsKnown: Boolean = false
)
data class LineupSlot(val position: String, val player: Player?, val score: Int)
data class Match(val result: String, val scored: Int, val conceded: Int, val note: String, val scorers: List<String> = emptyList(), val assists: List<String> = emptyList())
data class PlayerProgress(val playerName: String, val rating: Int, val attribute: String, val attributeValue: Int, val recordedAt: Long, val note: String = "")
data class CoachBackup(val players: List<Player>, val matches: List<Match>, val formation: String, val style: String, val progress: List<PlayerProgress> = emptyList(), val managerData: String = "{}")
data class OcrPlayerDraft(val name: String, val rating: Int, val sourceLine: String)

fun parseLiveSquadCandidates(raw: String): List<LiveSquadCandidate> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val name = item.optString("name").trim()
        val rating = item.optInt("rating", -1)
        if (name.isBlank() || rating !in 1..100) null
        else LiveSquadCandidate(name, rating, item.optString("sourceLine"))
    }
}.getOrDefault(emptyList())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedImages = extractSharedImageUris(intent)
        setContent { DlsCoachApp(sharedImageUris = sharedImages) }
    }

    private fun extractSharedImageUris(source: Intent?): List<Uri> {
        if (source == null) return emptyList()
        return when (source.action) {
            Intent.ACTION_SEND -> {
                if (source.type?.startsWith("image/") != true) return emptyList()
                val uri = if (Build.VERSION.SDK_INT >= 33) {
                    source.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    source.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (source.type?.startsWith("image/") != true) return emptyList()
                val uris = if (Build.VERSION.SDK_INT >= 33) {
                    source.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    source.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }
                uris?.take(8).orEmpty()
            }
            else -> emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlsCoachApp(sharedImageUris: List<Uri> = emptyList()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dls_coach_data", 0) }
    var tab by remember { mutableIntStateOf(0) }
    var captureRequested by remember { mutableStateOf(prefs.getBoolean("capture_active", false)) }
    var captureStatus by remember { mutableStateOf(prefs.getString("capture_status", "Not started") ?: "Not started") }
    var captureFrames by remember { mutableLongStateOf(prefs.getLong("capture_frames", 0L)) }
    var captureFrameWidth by remember { mutableIntStateOf(prefs.getInt("capture_frame_width", 0)) }
    var captureFrameHeight by remember { mutableIntStateOf(prefs.getInt("capture_frame_height", 0)) }
    var captureBrightness by remember { mutableIntStateOf(prefs.getInt("capture_average_brightness", -1)) }
    var captureOcrText by remember { mutableStateOf(prefs.getString("capture_ocr_text", "") ?: "") }
    var liveScreenType by remember { mutableStateOf(prefs.getString("live_screen_type", "UNKNOWN") ?: "UNKNOWN") }
    var liveScreenConfidence by remember { mutableStateOf(prefs.getString("live_screen_confidence", "LOW") ?: "LOW") }
    var liveCandidatesJson by remember { mutableStateOf(prefs.getString("live_squad_candidates", "[]") ?: "[]") }

    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                captureRequested = prefs.getBoolean("capture_active", false)
                captureStatus = prefs.getString("capture_status", "Not started") ?: "Not started"
                captureFrames = prefs.getLong("capture_frames", 0L)
                captureFrameWidth = prefs.getInt("capture_frame_width", 0)
                captureFrameHeight = prefs.getInt("capture_frame_height", 0)
                captureBrightness = prefs.getInt("capture_average_brightness", -1)
                captureOcrText = prefs.getString("capture_ocr_text", "") ?: ""
                liveScreenType = prefs.getString("live_screen_type", "UNKNOWN") ?: "UNKNOWN"
                liveScreenConfidence = prefs.getString("live_screen_confidence", "LOW") ?: "LOW"
                liveCandidatesJson = prefs.getString("live_squad_candidates", "[]") ?: "[]"
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    val capturePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
            val serviceIntent = Intent(context, LiveCaptureService::class.java).apply {
                putExtra(LiveCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(LiveCaptureService.EXTRA_RESULT_DATA, data)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent)
                else context.startService(serviceIntent)
                captureRequested = true
                captureStatus = "Starting capture…"
            } catch (error: Exception) {
                captureRequested = false
                Toast.makeText(context, "Could not start capture: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            captureRequested = false
            Toast.makeText(context, "Screen capture was not started.", Toast.LENGTH_SHORT).show()
        }
    }

    val startCapture = {
        val manager = context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        capturePermissionLauncher.launch(manager.createScreenCaptureIntent())
    }
    val stopCapture = {
        context.startService(Intent(context, LiveCaptureService::class.java).setAction(LiveCaptureService.ACTION_STOP))
        captureRequested = false
        captureStatus = "Stopping capture…"
    }
    var players by remember {
        mutableStateOf(loadStoredPlayers(
            raw = prefs.getString("players", null),
            hasStoredValue = prefs.contains("players")
        ))
    }
    var matches by remember { mutableStateOf(loadMatches(prefs.getString("matches", "[]") ?: "[]")) }
    var progressEntries by remember { mutableStateOf(loadProgress(prefs.getString("player_progress", "[]") ?: "[]")) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var formation by remember { mutableStateOf(prefs.getString("formation", "5-3-2") ?: "5-3-2") }
    var style by remember { mutableStateOf(prefs.getString("style", "Aggressive attack + compact defence") ?: "Aggressive attack + compact defence") }
    var showPlayerDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var showMatchDialog by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var ocrBusy by remember { mutableStateOf(false) }
    var ocrText by remember { mutableStateOf(prefs.getString("last_squad_ocr", "") ?: "") }
    var ocrDrafts by remember { mutableStateOf<List<OcrPlayerDraft>>(emptyList()) }
    var ocrError by remember { mutableStateOf("") }

    LaunchedEffect(sharedImageUris) {
        if (sharedImageUris.isNotEmpty()) {
            ocrBusy = true
            ocrError = ""
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val scannedTexts = mutableListOf<String>()
            try {
                sharedImageUris.take(8).forEachIndexed { index, uri ->
                    val image = InputImage.fromFilePath(context, uri)
                    val result = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text> { continuation ->
                        recognizer.process(image)
                            .addOnSuccessListener { text ->
                                if (continuation.isActive) continuation.resume(text)
                            }
                            .addOnFailureListener { error ->
                                if (continuation.isActive) continuation.resumeWith(Result.failure(error))
                            }
                    }
                    scannedTexts.add("IMAGE ${index + 1}\n${result.text}")
                }
                ocrText = scannedTexts.joinToString("\n\n")
                ocrDrafts = parseOcrPlayerDrafts(ocrText)
                prefs.edit().putString("last_squad_ocr", ocrText).apply()
                showOcrDialog = true
            } catch (error: Exception) {
                ocrError = error.localizedMessage ?: "Could not read the shared image. Try sharing it again."
                Toast.makeText(context, ocrError, Toast.LENGTH_LONG).show()
            } finally {
                recognizer.close()
                ocrBusy = false
            }
        }
    }

    LaunchedEffect(players) { prefs.edit().putString("players", savePlayers(players)).apply() }
    LaunchedEffect(matches) { prefs.edit().putString("matches", saveMatches(matches)).apply() }
    LaunchedEffect(progressEntries) { prefs.edit().putString("player_progress", saveProgress(progressEntries)).apply() }
    LaunchedEffect(formation) { prefs.edit().putString("formation", formation).apply() }
    LaunchedEffect(style) { prefs.edit().putString("style", style).apply() }

    val exportReportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri != null) runCatching {
            val report = buildCoachReport(players, matches, formation, style, progressEntries)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(report.toByteArray(Charsets.UTF_8))
            } ?: error("Could not open the selected destination.")
        }.onSuccess {
            Toast.makeText(context, "Coaching report saved on your device.", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(context, "Report export failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val exportBackupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(encodeBackup(players, matches, formation, style, progressEntries, exportManagerData(prefs)).toByteArray(Charsets.UTF_8))
            } ?: error("Could not open the selected destination.")
        }.onSuccess {
            Toast.makeText(context, "Backup exported successfully.", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(context, "Backup export failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val importBackupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Could not read the selected backup.")
            decodeBackup(raw)
        }.onSuccess { backup ->
            players = backup.players
            matches = backup.matches
            progressEntries = backup.progress
            formation = backup.formation
            style = backup.style
            restoreManagerData(prefs, backup.managerData)
            Toast.makeText(context, "Backup restored successfully.", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(context, "Restore failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val squadImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            ocrBusy = true
            ocrError = ""
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        ocrText = result.text
                        ocrDrafts = parseOcrPlayerDrafts(ocrText)
                        prefs.edit().putString("last_squad_ocr", ocrText).apply()
                        showOcrDialog = true
                        ocrBusy = false
                        recognizer.close()
                    }
                    .addOnFailureListener { error ->
                        ocrError = error.localizedMessage ?: "Text recognition failed. Try a clearer screenshot."
                        ocrBusy = false
                        recognizer.close()
                    }
            } catch (error: Exception) {
                ocrError = error.localizedMessage ?: "Could not open this image."
                ocrBusy = false
            }
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF126B4A),
        secondary = androidx.compose.ui.graphics.Color(0xFF456B5B),
        background = androidx.compose.ui.graphics.Color(0xFFF5F7F5)
    )) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("DLS COACH AI", fontWeight = FontWeight.Bold)
                            Text("Your evidence-led DLS 26 companion", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf("Home", "Squad", "Tactics", "Matches", "Coach", "Manager", "Game Link", "Connect").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(listOf("⌂", "⚽", "♟", "▤", "✦", "✓", "▶", "↔")[index]) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(players, matches, formation, style,
                    onExport = { exportBackupPicker.launch("DLS-Coach-AI-backup.json") },
                    onImport = { importBackupPicker.launch(arrayOf("application/json", "text/*")) },
                    captureRequested = captureRequested,
                    captureStatus = captureStatus,
                    captureFrames = captureFrames,
                    captureFrameWidth = captureFrameWidth,
                    captureFrameHeight = captureFrameHeight,
                    captureBrightness = captureBrightness,
                    captureOcrText = captureOcrText,
                    onStartCapture = startCapture,
                    onStopCapture = stopCapture,
                    modifier = Modifier.padding(padding)
                )
                1 -> SquadScreen(
                    players,
                    onAdd = { showPlayerDialog = true },
                    onDelete = { players = players - it },
                    onEdit = { editingPlayer = it },
                    onUploadScreenshot = { squadImagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.padding(padding)
                )
                2 -> TacticsScreen(formation, style, { formation = it }, { style = it }, Modifier.padding(padding))
                3 -> MatchesScreen(matches, { showMatchDialog = true }, Modifier.padding(padding))
                4 -> CoachScreen(players, matches, formation, style, progressEntries, onLogProgress = { showProgressDialog = true }, onShare = {
                    exportReportPicker.launch("DLS-Coach-AI-coaching-report.txt")
                }, modifier = Modifier.padding(padding))
                5 -> ManagerDashboardScreen(players, matches, formation, style, prefs, Modifier.padding(padding))
                6 -> DlsConnectionScreen(
                    captureRequested = captureRequested,
                    captureStatus = captureStatus,
                    liveScreenType = liveScreenType,
                    liveScreenConfidence = liveScreenConfidence,
                    liveCandidates = parseLiveSquadCandidates(liveCandidatesJson),
                    onConfirmLiveCandidates = { candidates ->
                        val existingNames = players.map { it.name.trim().lowercase() }.toSet()
                        val additions = candidates
                            .filterNot { it.name.trim().lowercase() in existingNames }
                            .distinctBy { it.name.trim().lowercase() }
                            .map { Player(name = it.name, position = "UNK", rating = it.rating, statsKnown = false) }
                        players = players + additions
                        prefs.edit().putString("live_squad_candidates", "[]").apply()
                        liveCandidatesJson = "[]"
                        Toast.makeText(context, "Added ${additions.size} confirmed player record(s). Positions and attributes remain unverified.", Toast.LENGTH_LONG).show()
                    },
                    onStartCapture = startCapture,
                    onStopCapture = stopCapture,
                    modifier = Modifier.padding(padding)
                )
                7 -> IntegrationsScreen(
                    onShareReport = {
                        val report = buildCoachReport(players, matches, formation, style, progressEntries)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "DLS Coach AI coaching report")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(send, "Choose an app to share your report"))
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        if (showPlayerDialog) {
            AddPlayerDialog(
                initialPlayer = null,
                onDismiss = { showPlayerDialog = false },
                onAdd = { player -> players = players + player; showPlayerDialog = false }
            )
        }
        editingPlayer?.let { current ->
            AddPlayerDialog(
                initialPlayer = current,
                onDismiss = { editingPlayer = null },
                onAdd = { updated -> players = players.map { if (it == current) updated else it }; editingPlayer = null }
            )
        }
        if (showMatchDialog) {
            AddMatchDialog(
                players = players,
                onDismiss = { showMatchDialog = false },
                onAdd = { match -> matches = listOf(match) + matches; showMatchDialog = false }
            )
        }
        if (showProgressDialog) {
            PlayerProgressDialog(players, onDismiss = { showProgressDialog = false }) { entry ->
                progressEntries = listOf(entry) + progressEntries
                showProgressDialog = false
            }
        }
        if (showOcrDialog) {
            AlertDialog(
                onDismissRequest = { showOcrDialog = false },
                title = { Text("Screenshot text — review before entering players") },
                text = {
                    Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("On-device text recognition completed. Check every name and number against your screenshot. OCR can misread small text; this result has NOT been saved as player records.", style = MaterialTheme.typography.bodySmall)
                        Text("POSSIBLE PLAYER DRAFTS", fontWeight = FontWeight.Bold)
                        if (ocrDrafts.isEmpty()) {
                            Text("No clear 'player name + rating' lines were detected. Nothing will be added automatically. Check the extracted text below or enter players manually.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("These are suggestions only. Confirm each name and rating against the screenshot. Position and card attributes remain unknown until you edit the player.", style = MaterialTheme.typography.bodySmall)
                            ocrDrafts.forEach { draft ->
                                Card {
                                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(draft.name, fontWeight = FontWeight.SemiBold)
                                            Text("Possible OVR: ${draft.rating} · Position unknown", style = MaterialTheme.typography.bodySmall)
                                        }
                                        val alreadyExists = players.any { it.name.equals(draft.name, ignoreCase = true) }
                                        TextButton(
                                            enabled = !alreadyExists,
                                            onClick = {
                                                players = players + Player(draft.name, "UNK", draft.rating)
                                                ocrDrafts = ocrDrafts.filterNot { it == draft }
                                            }
                                        ) { Text(if (alreadyExists) "In squad" else "Add") }
                                    }
                                }
                            }
                        }
                        Divider()
                        Text("EXTRACTED TEXT", fontWeight = FontWeight.Bold)
                        Text(if (ocrText.isBlank()) "No readable text found. Try a sharper, uncropped screenshot." else ocrText)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "DLS Coach AI — extracted squad text")
                        putExtra(Intent.EXTRA_TEXT, ocrText)
                    }
                    context.startActivity(Intent.createChooser(send, "Share extracted text"))
                }) { Text("Share text") } },
                dismissButton = { TextButton(onClick = { showOcrDialog = false }) { Text("Close") } }
            )
        }
    }
}

@Composable
fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun HomeScreen(players: List<Player>, matches: List<Match>, formation: String, style: String, onExport: () -> Unit, onImport: () -> Unit, captureRequested: Boolean, captureStatus: String, captureFrames: Long, captureFrameWidth: Int, captureFrameHeight: Int, captureBrightness: Int, captureOcrText: String, onStartCapture: () -> Unit, onStopCapture: () -> Unit, modifier: Modifier = Modifier) {
    Page(modifier) {
        Text("Welcome, Coach", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Build your squad, record matches and turn your results into better decisions.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Players", players.size.toString(), Modifier.weight(1f))
            StatCard("Matches", matches.size.toString(), Modifier.weight(1f))
            StatCard("Goals conceded", matches.sumOf { it.conceded }.toString(), Modifier.weight(1f))
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("CURRENT PLAN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(formation, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(style)
            }
        }
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("First coaching principle", fontWeight = FontWeight.Bold)
                Text("Attack aggressively with structure: keep midfield cover behind the ball and avoid pulling centre-backs out of position.")
            }
        }
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LIVE MATCH CO-PILOT", fontWeight = FontWeight.Bold)
                Text(captureStatus, style = MaterialTheme.typography.bodySmall)
                Text("Diagnostic samples: $captureFrames", style = MaterialTheme.typography.labelSmall)
                if (captureFrameWidth > 0 && captureFrameHeight > 0) {
                    Text("Captured frame: ${captureFrameWidth} × ${captureFrameHeight}px", style = MaterialTheme.typography.labelSmall)
                }
                if (captureBrightness >= 0) {
                    Text("Average screen brightness: $captureBrightness / 255", style = MaterialTheme.typography.labelSmall)
                }
                Text("LATEST ON-SCREEN TEXT (ON-DEVICE OCR)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Text(captureOcrText.ifBlank { "No readable text detected yet. Keep the DLS screen visible during an authorised capture session." }, style = MaterialTheme.typography.bodySmall)
                if (captureOcrText.isNotBlank()) {
                    Text("OCR is raw text recognition only. It does not yet identify players, ball movement, score events or tactical situations.", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    if (captureRequested) "Capture is active. Use the persistent Android notification or the button below to stop it."
                    else "Start a user-authorised screen-capture session. If Android offers app-window sharing, choose DLS 26.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("DLS screen samples are processed on this phone using bundled text recognition. Frames are not saved or uploaded, and no audio is captured. This stage reads visible text only; tactical interpretation and gameplay control are not enabled.", style = MaterialTheme.typography.bodySmall)
                if (captureRequested) {
                    OutlinedButton(onClick = onStopCapture, modifier = Modifier.fillMaxWidth()) { Text("Stop capture session") }
                } else {
                    Button(onClick = onStartCapture, modifier = Modifier.fillMaxWidth()) { Text("Start screen capture") }
                }
            }
        }
        Text("DATA SAFETY", fontWeight = FontWeight.Bold)
        Text("Your squad and match history are stored on this device. Export a backup before changing phones or reinstalling the app.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Export backup") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Restore backup") }
        }
        Text("Recommendations become more useful as verified player attributes and match evidence are added.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SquadScreen(
    players: List<Player>,
    onAdd: () -> Unit,
    onDelete: (Player) -> Unit,
    onEdit: (Player) -> Unit,
    onUploadScreenshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Page(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Squad Lab", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Button(onClick = onAdd) { Text("Add player") }
        }
        OutlinedButton(onClick = onUploadScreenshot, modifier = Modifier.fillMaxWidth()) {
            Text("Upload squad screenshot — scan inside app")
        }
        val unknownPositions = players.count { it.position == "UNK" }
        val verifiedProfiles = players.count { it.position != "UNK" && it.statsKnown }
        Card {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SQUAD DATA QUALITY", fontWeight = FontWeight.Bold)
                Text("$verifiedProfiles / ${players.size} complete profiles")
                Text("$unknownPositions positions still need confirmation · ${players.count { !it.statsKnown }} attribute sets unverified")
                LinearProgressIndicator(
                    progress = { if (players.isEmpty()) 0f else verifiedProfiles.toFloat() / players.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Edit a player to confirm their natural position and enter exact card attributes. The coach avoids treating missing data as fact.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            "Choose a DLS 26 screenshot. The app reads visible text on your phone using on-device OCR, then shows the result for you to verify. It does not guess unreadable attributes or automatically create player records yet.",
            style = MaterialTheme.typography.bodySmall
        )
        if (players.isEmpty()) {
            Text("Your squad is empty. Add players with their real positions and ratings.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(players) { player ->
                    Card {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(player.name, fontWeight = FontWeight.Bold)
                                Text("${player.position}  •  OVR ${player.rating}", style = MaterialTheme.typography.bodySmall)
                                if (player.statsKnown) {
                                    Text("SPE ${player.spe} · ACC ${player.acc} · STA ${player.sta} · STR ${player.str}", style = MaterialTheme.typography.labelSmall)
                                    Text("CON ${player.con} · PAS ${player.pas} · SHO ${player.sho} · TAC ${player.tac}", style = MaterialTheme.typography.labelSmall)
                                    if (player.position == "GK") Text("GKR ${player.gkr} · GKH ${player.gkh}", style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Text("Attributes not verified yet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                TextButton(onClick = { onEdit(player) }) { Text("Edit") }
                                TextButton(onClick = { onDelete(player) }) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TacticsScreen(formation: String, style: String, onFormation: (String) -> Unit, onStyle: (String) -> Unit, modifier: Modifier = Modifier) {
    val formations = listOf("4-2-3-1", "4-3-3", "4-1-2-1-2", "3-5-2", "5-3-2", "4-4-2")
    val styles = listOf("Aggressive attack + compact defence", "Balanced possession", "Fast counter-attack", "High press")
    Page(modifier) {
        Text("Tactical Lab", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Choose a starting structure. These are planning notes, not automatic changes to the game.")
        Text("Formation", fontWeight = FontWeight.Bold)
        formations.forEach { item ->
            OutlinedButton(onClick = { onFormation(item) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (formation == item) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )) { Text(if (formation == item) "✓  $item" else item) }
        }
        Text("Playing style", fontWeight = FontWeight.Bold)
        styles.forEach { item ->
            OutlinedButton(onClick = { onStyle(item) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (style == item) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )) { Text(if (style == item) "✓  $item" else item) }
        }
    }
}

@Composable
fun MatchesScreen(matches: List<Match>, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Page(modifier) {
        Text("Match Analyst", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Log result") }
        Text("Recent results", fontWeight = FontWeight.Bold)
        if (matches.isEmpty()) Text("No matches logged yet.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches) { match ->
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text("${match.result}  •  ${match.scored}-${match.conceded}", fontWeight = FontWeight.Bold)
                        if (match.note.isNotBlank()) Text(match.note)
                        if (match.scorers.isNotEmpty()) Text("Goals: ${match.scorers.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                        if (match.assists.isNotEmpty()) Text("Assists: ${match.assists.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Text("Standalone operation: live screen text recognition runs locally inside DLS Coach AI after you grant Android screen-capture permission. Full match-video event analysis is still under development and does not require ChatGPT.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CoachScreen(players: List<Player>, matches: List<Match>, formation: String, style: String, progressEntries: List<PlayerProgress>, onLogProgress: () -> Unit, onShare: () -> Unit, modifier: Modifier = Modifier) {
    val conceded = matches.sumOf { it.conceded }
    val scored = matches.sumOf { it.scored }
    val losses = matches.count { it.result.equals("Loss", true) }
    val lineup = remember(players, formation) { optimiseLineup(players, formation) }
    val tips = remember(formation, style) { tacticalAdvice(formation, style) }
    Page(modifier) {
        Text("Performance Centre", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Evidence-led squad selection and tactical recommendations. Results depend on player data and how you execute in matches.")
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("TEAM PERFORMANCE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Players entered: ${players.size}")
                Text("Matches recorded: ${matches.size}")
                Text("Goals scored: $scored  |  Goals conceded: $conceded")
                Text("Recorded losses: $losses")
                if (matches.isNotEmpty()) {
                    val winRate = (matches.count { it.result.equals("Win", true) } * 100.0 / matches.size)
                    val averageScored = scored.toDouble() / matches.size
                    val averageConceded = conceded.toDouble() / matches.size
                    val cleanSheets = matches.count { it.conceded == 0 }
                    Text("Win rate: ${"%.1f".format(winRate)}%")
                    Text("Average goals per match: ${"%.1f".format(averageScored)} scored · ${"%.1f".format(averageConceded)} conceded")
                    Text("Clean sheets: $cleanSheets / ${matches.size}")
                    Text("Goal difference: ${scored - conceded}")
                    performanceInsights(matches).forEach { insight ->
                        Text("• $insight", color = MaterialTheme.colorScheme.tertiary)
                    }
                    compareRecentPeriods(matches).forEach { insight ->
                        Text("• $insight", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Text("PLAYER CONTRIBUTIONS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Based only on goal scorers and assists you enter when logging matches. Missing entries are not treated as zero performance.", style = MaterialTheme.typography.bodySmall)
        val contributions = playerContributionStats(matches)
        if (contributions.isEmpty()) {
            Text("No player contributions recorded yet. Add exact player names when logging a match.")
        } else {
            contributions.take(8).forEach { contribution ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(contribution.name, fontWeight = FontWeight.SemiBold)
                            Text("Recorded contribution matches: ${contribution.appearancesWithContribution}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("G ${contribution.goals} · A ${contribution.assists}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (matches.size >= 10) {
            Text("PLAYER FORM: LATEST FIVE VS PREVIOUS FIVE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            playerContributionTrends(matches).take(8).forEach { trend ->
                val recentTotal = trend.recentGoals + trend.recentAssists
                val previousTotal = trend.previousGoals + trend.previousAssists
                val change = recentTotal - previousTotal
                val direction = when {
                    change > 0 -> "+$change contributions"
                    change < 0 -> "$change contributions"
                    else -> "unchanged"
                }
                Text("${trend.name}: recent G${trend.recentGoals}/A${trend.recentAssists}; previous G${trend.previousGoals}/A${trend.previousAssists} ($direction)", style = MaterialTheme.typography.bodySmall)
            }
            Text("Form compares recorded goals and assists only; it is not a complete measure of a player's performance or minutes played.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Log ten matches to compare each player's recorded goal/assist contributions across two five-match periods.", style = MaterialTheme.typography.bodySmall)
        }
        Text("TACTICAL EVENT PATTERNS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("These counts use only the event tags you manually add when recording a match. They are not detected automatically.", style = MaterialTheme.typography.bodySmall)
        Card {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tacticalEventInsights(matches).forEach { insight -> Text("• $insight") }
            }
        }
        Text("RECOMMENDED STARTING XI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Role-fit scores are coaching estimates based on the entered attributes and position, not official DLS formulas.", style = MaterialTheme.typography.bodySmall)
        if (players.isEmpty()) {
            Text("Add your players and their actual card attributes in Squad to generate a role-based XI.")
        } else {
            lineup.forEach { slot ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(slot.position, fontWeight = FontWeight.Bold, modifier = Modifier.width(54.dp))
                        Column(Modifier.weight(1f)) {
                            Text(slot.player?.name ?: "No suitable player entered", fontWeight = FontWeight.SemiBold)
                            if (slot.player != null) Text("OVR ${slot.player.rating} · role fit ${slot.score}/100", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (players.any { it.position == "UNK" }) {
                Text("Some player positions are still unknown. Assign the positions shown on their DLS cards before relying on this XI.", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
            }
            if (players.size < 11) Text("You have entered ${players.size} players. Add at least 11 for a complete XI.")
            val bench = benchRecommendations(players, formation)
            if (bench.isNotEmpty()) {
                Text("SUGGESTED BENCH", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                bench.forEachIndexed { index, player ->
                    Text("${index + 1}. ${player.name} · ${player.position} · OVR ${player.rating}")
                }
            }
        }
        Text("TACTICAL GAME PLAN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        tips.forEach { tip -> Text("• $tip") }
        Text("INDIVIDUAL DEVELOPMENT TRACKER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Record visible player-card values before and after training. These are manual snapshots, not automatic game telemetry.", style = MaterialTheme.typography.bodySmall)
        if (players.isNotEmpty()) OutlinedButton(onClick = onLogProgress, modifier = Modifier.fillMaxWidth()) { Text("Log player development") }
        if (progressEntries.isEmpty()) {
            Text("No records yet. Save a baseline before training, then record the player again later.")
        } else {
            progressEntries.groupBy { it.playerName }.forEach { (name, entries) ->
                val ordered = entries.sortedBy { it.recordedAt }
                val first = ordered.first()
                val latest = ordered.last()
                val delta = latest.rating - first.rating
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name, fontWeight = FontWeight.Bold)
                        Text("OVR ${first.rating} → ${latest.rating} (${if (delta > 0) "+" else ""}$delta)")
                        Text("Latest ${latest.attribute}: ${latest.attributeValue} · ${entries.size} record(s)", style = MaterialTheme.typography.bodySmall)
                        Text("Updated ${java.text.DateFormat.getDateInstance().format(java.util.Date(latest.recordedAt))}", style = MaterialTheme.typography.labelSmall)
                        val previous = ordered.dropLast(1).lastOrNull { it.attribute == latest.attribute }
                        if (previous != null) {
                            val change = latest.attributeValue - previous.attributeValue
                            Text("${latest.attribute} change since previous matching record: ${if (change > 0) "+" else ""}$change")
                        }
                    }
                }
            }
        }
        Text("PLAYER DEVELOPMENT PRIORITIES", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("SMART PLAYER DEVELOPMENT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("These are personalised training priorities from the attributes you entered. They are coaching estimates, not guaranteed in-game rating gains.", style = MaterialTheme.typography.bodySmall)
        if (players.isEmpty()) {
            Text("Add player attributes to unlock individual development plans.")
        } else {
            players.sortedByDescending { it.rating }.forEach { player ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${player.name} · ${player.position} · OVR ${player.rating}", fontWeight = FontWeight.Bold)
                        developmentFocus(player).forEachIndexed { index, focus ->
                            Text("${index + 1}. $focus")
                        }
                    }
                }
            }
        }
        Text("SQUAD DEPTH & BALANCE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        squadBalanceAdvice(players).forEach { advice -> Text("• $advice") }
        Text("RECENT FORM", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (matches.isEmpty()) {
            Text("Log at least one match to begin tracking form.")
        } else {
            val recent = matches.take(5)
            val wins = recent.count { it.result.equals("Win", true) }
            val draws = recent.count { it.result.equals("Draw", true) }
            val recentConceded = recent.sumOf { it.conceded }
            val recentScored = recent.sumOf { it.scored }
            Text("Last ${recent.size}: $wins wins · $draws draws · ${recent.size - wins - draws} losses")
            Text("Goals: $recentScored scored · $recentConceded conceded")
            Text(if (recentConceded > recentScored) "Current signal: defensive stability is the first area to investigate." else "Current signal: review consistency and keep testing what works.")
            Text("This is a simple trend from logged results, not a full match-performance measurement.", style = MaterialTheme.typography.bodySmall)
        }
        Text("YOUR NEXT THREE ACTIONS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        nextCoachingActions(players, matches, formation).forEachIndexed { index, action ->
            Card { Text("${index + 1}. $action", Modifier.padding(12.dp)) }
        }
        Text("Defenders: build Tackling (TAC), Strength (STR), Speed (SPE) and Stamina (STA).")
        Text("Defensive midfielders: prioritise TAC, PAS, STA and STR to win the ball and protect the back line.")
        Text("Attacking midfielders and wingers: develop Control (CON), Passing (PAS), Acceleration (ACC) and Shooting (SHO).")
        Text("Strikers: prioritise SHO, CON, SPE and ACC for finishing and movement.")
        Text("Goalkeepers: compare Reactions (GKR) for shot-stopping and Handling (GKH) for catching and claiming.")
        Text("In DLS, use the appropriate in-game coaches to develop attributes. This app cannot directly upgrade a player's card.")
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your current setup", fontWeight = FontWeight.Bold)
                Text("Formation: $formation")
                Text("Style: $style")
            }
        }
        Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) { Text("Generate and save coaching report") }
        Text("Important: this app recommends selection and training priorities. It cannot directly change player attributes, control the game or guarantee wins.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PlayerProgressDialog(players: List<Player>, onDismiss: () -> Unit, onSave: (PlayerProgress) -> Unit) {
    var selectedName by remember { mutableStateOf(players.firstOrNull()?.name ?: "") }
    var rating by remember { mutableStateOf(players.firstOrNull()?.rating?.toString() ?: "") }
    var attribute by remember { mutableStateOf("SPE") }
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var playerMenu by remember { mutableStateOf(false) }
    var attributeMenu by remember { mutableStateOf(false) }
    val attributes = listOf("SPE", "ACC", "STA", "STR", "CON", "PAS", "SHO", "TAC", "GKR", "GKH")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log player development") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { playerMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedName) }
                    DropdownMenu(expanded = playerMenu, onDismissRequest = { playerMenu = false }) {
                        players.forEach { player ->
                            DropdownMenuItem(text = { Text("${player.name} · ${player.rating}") }, onClick = {
                                selectedName = player.name
                                rating = player.rating.toString()
                                playerMenu = false
                            })
                        }
                    }
                }
                OutlinedTextField(rating, { rating = it.filter(Char::isDigit).take(3) }, label = { Text("Current OVR (1–100)") }, singleLine = true)
                Box {
                    OutlinedButton(onClick = { attributeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Attribute: $attribute") }
                    DropdownMenu(expanded = attributeMenu, onDismissRequest = { attributeMenu = false }) {
                        attributes.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { attribute = item; attributeMenu = false }) }
                    }
                }
                OutlinedTextField(value, { value = it.filter(Char::isDigit).take(3) }, label = { Text("$attribute value (1–100)") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Optional note") })
                Text("Copy values from the actual in-game player card. Saving a record does not upgrade the player.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = selectedName.isNotBlank() && rating.toIntOrNull() in 1..100 && value.toIntOrNull() in 1..100, onClick = {
                onSave(PlayerProgress(selectedName, rating.toInt(), attribute, value.toInt(), System.currentTimeMillis(), note.trim()))
            }) { Text("Save record") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddPlayerDialog(initialPlayer: Player? = null, onDismiss: () -> Unit, onAdd: (Player) -> Unit) {
    var name by remember { mutableStateOf(initialPlayer?.name ?: "") }
    var position by remember { mutableStateOf(initialPlayer?.position?.takeIf { it != "UNK" } ?: "CM") }
    var rating by remember { mutableStateOf(initialPlayer?.rating?.toString() ?: "90") }
    val statNames = listOf("SPE", "ACC", "STA", "STR", "CON", "PAS", "SHO", "TAC", "GKR", "GKH")
    val stats = remember(initialPlayer) { mutableStateMapOf<String, String>().apply {
        statNames.forEachIndexed { index, key ->
            val value = initialPlayer?.let { listOf(it.spe, it.acc, it.sta, it.str, it.con, it.pas, it.sho, it.tac, it.gkr, it.gkh)[index] }
            put(key, if (initialPlayer?.statsKnown == true) value.toString() else "")
        }
    } }
    val positions = listOf("GK", "CB", "LB", "RB", "CDM", "CM", "CAM", "LM", "RM", "LW", "RW", "CF", "ST")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPlayer == null) "Add player and card stats" else "Edit player profile") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Player name") }, singleLine = true)
                OutlinedTextField(rating, { rating = it.filter(Char::isDigit).take(3) }, label = { Text("Overall rating") }, singleLine = true)
                Text("Natural position")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    positions.forEach { p -> FilterChip(selected = position == p, onClick = { position = p }, label = { Text(p) }) }
                }
                Text("Enter the numbers exactly as shown on the in-game player card (1–100). For a goalkeeper, prioritise GKR and GKH. All fields must be verified before saving.", style = MaterialTheme.typography.bodySmall)
                statNames.forEach { stat ->
                    OutlinedTextField(
                        value = stats[stat] ?: "70",
                        onValueChange = { stats[stat] = it.filter(Char::isDigit).take(3) },
                        label = { Text(stat) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            val value = rating.toIntOrNull()
            val numbers = statNames.map { stats[it]?.toIntOrNull() }
            if (name.isNotBlank() && value != null && value in 1..100 && numbers.all { it != null && it in 1..100 }) {
                onAdd(Player(name.trim(), position, value,
                    numbers[0]!!, numbers[1]!!, numbers[2]!!, numbers[3]!!, numbers[4]!!,
                    numbers[5]!!, numbers[6]!!, numbers[7]!!, numbers[8]!!, numbers[9]!!,
                    statsKnown = true
                ))
            }
        }) { Text("Save player") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddMatchDialog(players: List<Player>, onDismiss: () -> Unit, onAdd: (Match) -> Unit) {
    var result by remember { mutableStateOf("Loss") }
    var scored by remember { mutableStateOf("0") }
    var conceded by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    var scorersText by remember { mutableStateOf("") }
    var assistsText by remember { mutableStateOf("") }
    var contributionError by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record a match") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Win", "Draw", "Loss").forEach { r ->
                        FilterChip(selected = result == r, onClick = { result = r }, label = { Text(r) })
                    }
                }
                OutlinedTextField(scored, { scored = it.filter(Char::isDigit).take(2) }, label = { Text("Goals scored") })
                OutlinedTextField(conceded, { conceded = it.filter(Char::isDigit).take(2) }, label = { Text("Goals conceded") })
                Text("Optional player contributions — enter exact squad names, separated by commas. Repeat a name for multiple goals/assists.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(scorersText, { scorersText = it }, label = { Text("Goal scorers") })
                OutlinedTextField(assistsText, { assistsText = it }, label = { Text("Assists") })
                if (contributionError.isNotBlank()) Text(contributionError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Squad: " + players.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall)
                Text("Optional tactical event tags (tap only what actually happened):", style = MaterialTheme.typography.bodySmall)
                val eventTags = listOf("[COUNTER]", "[HIGH PRESS]", "[MIDFIELD LOST]", "[MISSED CHANCE]", "[GK ERROR]", "[DEFENSIVE ERROR]", "[SET PIECE]")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    eventTags.chunked(2).forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            group.forEach { tag ->
                                FilterChip(
                                    selected = note.contains(tag, ignoreCase = true),
                                    onClick = { note = if (note.contains(tag, ignoreCase = true)) note.replace(tag, "", ignoreCase = true).trim() else listOf(note.trim(), tag).filter { it.isNotBlank() }.joinToString(" ") },
                                    label = { Text(tag.removePrefix("[").removeSuffix("]")) }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Match notes / events") })
            }
        },
        confirmButton = { TextButton(onClick = {
            val a = scored.toIntOrNull()
            val b = conceded.toIntOrNull()
            if (a != null && b != null && a >= 0 && b >= 0) {
                val squadNames = players.map { it.name }
                fun resolveNames(raw: String): List<String> = raw.split(",").map { it.trim() }
                    .filter { entered -> entered.isNotBlank() && squadNames.any { it.equals(entered, ignoreCase = true) } }
                    .map { entered -> squadNames.first { it.equals(entered, ignoreCase = true) } }
                val enteredScorers = scorersText.split(",").map { it.trim() }.filter(String::isNotBlank)
                val enteredAssists = assistsText.split(",").map { it.trim() }.filter(String::isNotBlank)
                val scorers = resolveNames(scorersText)
                val assists = resolveNames(assistsText)
                if (scorers.size != enteredScorers.size || assists.size != enteredAssists.size) {
                    contributionError = "One or more names do not match your squad. Check spelling or leave that field blank."
                } else {
                    onAdd(Match(result, a, b, note.trim(), scorers, assists))
                }
            }
        }) { Text("Save match") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun parseOcrPlayerDrafts(raw: String): List<OcrPlayerDraft> {
    val ratingAtEnd = Regex("^(.{2,32}?)\\s+(100|[1-9][0-9]?)$")
    val ignored = setOf("overall rating", "rating", "speed", "acceleration", "stamina", "strength", "control", "passing", "shooting", "tackling", "goalkeeping", "height", "weight", "player")
    return raw.lineSequence()
        .map { it.trim().replace(Regex("\\s+"), " ") }
        .mapNotNull { line ->
            val match = ratingAtEnd.matchEntire(line) ?: return@mapNotNull null
            val name = match.groupValues[1].trim().trim('-', ':', '|', '•')
            val rating = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            if (name.lowercase() in ignored || name.count { it.isLetter() } < 2) return@mapNotNull null
            if (name.any { it.isDigit() }) return@mapNotNull null
            OcrPlayerDraft(name, rating, line)
        }
        .distinctBy { it.name.lowercase() }
        .take(30)
        .toList()
}

fun encodeBackup(players: List<Player>, matches: List<Match>, formation: String, style: String, progress: List<PlayerProgress> = emptyList(), managerData: String = "{}"): String =
    JSONObject()
        .put("app", "DLS Coach AI")
        .put("schemaVersion", 3)
        .put("exportedAt", System.currentTimeMillis())
        .put("players", JSONArray(savePlayers(players)))
        .put("matches", JSONArray(saveMatches(matches)))
        .put("progress", JSONArray(saveProgress(progress)))
        .put("formation", formation)
        .put("style", style)
        .put("managerData", JSONObject(managerData))
        .toString(2)

fun decodeBackup(raw: String): CoachBackup {
    val root = JSONObject(raw)
    require(root.optString("app") == "DLS Coach AI") { "This is not a DLS Coach AI backup." }
    require(root.optInt("schemaVersion", -1) in 1..3) { "Unsupported backup version." }
    val playerArray = root.optJSONArray("players")
        ?: error("The backup is missing its player list.")
    val matchArray = root.optJSONArray("matches")
        ?: error("The backup is missing its match list.")
    require(playerArray.length() <= 100) { "The backup contains too many player records." }
    require(matchArray.length() <= 10000) { "The backup contains too many match records." }
    require((0 until playerArray.length()).all { playerArray.optJSONObject(it) != null }) {
        "The backup contains an invalid player record."
    }
    require((0 until matchArray.length()).all { matchArray.optJSONObject(it) != null }) {
        "The backup contains an invalid match record."
    }
    val players = loadPlayers(playerArray.toString())
    val matches = loadMatches(matchArray.toString())
    require(players.size == playerArray.length()) { "One or more player records could not be read." }
    require(matches.size == matchArray.length()) { "One or more match records could not be read." }
    require(players.all { it.name.isNotBlank() && it.rating in 1..100 }) {
        "The backup contains invalid player names or ratings."
    }
    require(matches.all { match ->
        match.result in setOf("Win", "Draw", "Loss") && match.scored in 0..99 && match.conceded in 0..99 &&
            match.result == when {
                match.scored > match.conceded -> "Win"
                match.scored == match.conceded -> "Draw"
                else -> "Loss"
            }
    }) { "The backup contains a match with invalid or inconsistent result data." }
    val progress = loadProgress(root.optJSONArray("progress")?.toString() ?: "[]")
    val formation = root.optString("formation", "5-3-2")
    val supportedFormations = setOf("4-2-3-1", "4-3-3", "4-1-2-1-2", "3-5-2", "5-3-2", "4-4-2")
    require(formation in supportedFormations) { "The backup contains an unsupported formation." }
    return CoachBackup(
        players = players,
        matches = matches,
        progress = progress,
        formation = formation,
        style = root.optString("style", "Aggressive attack + compact defence"),
        managerData = root.optJSONObject("managerData")?.toString() ?: "{}"
    )
}

/** Preserves an intentionally empty squad instead of silently reseeding it on every launch. */
fun loadStoredPlayers(raw: String?, hasStoredValue: Boolean): List<Player> {
    if (!hasStoredValue) return initialSquad()
    return loadPlayers(raw ?: "[]")
}

fun initialSquad(): List<Player> = listOf(
    Player("Kylian Mbappé", "UNK", 96), Player("Erling Haaland", "UNK", 96),
    Player("Mohamed Salah", "UNK", 95), Player("Vinícius Jr", "UNK", 95),
    Player("Raphinha", "UNK", 95), Player("Michael Olise", "UNK", 96),
    Player("Ousmane Dembélé", "UNK", 96), Player("Gabriel Batistuta", "UNK", 95),
    Player("Jude Bellingham", "UNK", 95), Player("Rodri", "UNK", 95),
    Player("Vitinha", "UNK", 95), Player("Federico Valverde", "UNK", 95),
    Player("Pedri", "UNK", 94), Player("Kevin De Bruyne", "UNK", 93),
    Player("Lothar Matthäus", "UNK", 91), Player("Virgil van Dijk", "UNK", 95),
    Player("William Saliba", "UNK", 94), Player("Marcel Desailly", "UNK", 93),
    Player("Achraf Hakimi", "UNK", 95), Player("Gabriel", "UNK", 90),
    Player("Stefan de Vrij", "UNK", 90), Player("Andrew Robertson", "UNK", 90),
    Player("Reece James", "UNK", 86)
)

fun savePlayers(players: List<Player>): String = JSONArray().apply {
    players.forEach { p ->
        put(JSONObject()
            .put("name", p.name).put("position", p.position).put("rating", p.rating)
            .put("spe", p.spe).put("acc", p.acc).put("sta", p.sta).put("str", p.str)
            .put("con", p.con).put("pas", p.pas).put("sho", p.sho).put("tac", p.tac)
            .put("gkr", p.gkr).put("gkh", p.gkh).put("statsKnown", p.statsKnown))
    }
}.toString()

fun loadPlayers(raw: String): List<Player> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        val rating = o.optInt("rating", 50)
        Player(
            o.optString("name"), o.optString("position"), rating,
            o.optInt("spe", rating), o.optInt("acc", rating), o.optInt("sta", rating),
            o.optInt("str", rating), o.optInt("con", rating), o.optInt("pas", rating),
            o.optInt("sho", rating), o.optInt("tac", rating), o.optInt("gkr", rating), o.optInt("gkh", rating),
            o.optBoolean("statsKnown", false)
        )
    }
}.getOrDefault(emptyList())

fun saveProgress(entries: List<PlayerProgress>): String = JSONArray().apply {
    entries.forEach { entry ->
        put(JSONObject().put("playerName", entry.playerName).put("rating", entry.rating)
            .put("attribute", entry.attribute).put("attributeValue", entry.attributeValue)
            .put("recordedAt", entry.recordedAt).put("note", entry.note))
    }
}.toString()

fun loadProgress(raw: String): List<PlayerProgress> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        PlayerProgress(item.optString("playerName"), item.optInt("rating"),
            item.optString("attribute"), item.optInt("attributeValue"),
            item.optLong("recordedAt"), item.optString("note"))
    }.filter { it.playerName.isNotBlank() && it.rating in 1..100 && it.attributeValue in 1..100 }
}.getOrDefault(emptyList())

fun saveMatches(matches: List<Match>): String = JSONArray().apply {
    matches.forEach { match ->
        put(JSONObject()
            .put("result", match.result)
            .put("scored", match.scored)
            .put("conceded", match.conceded)
            .put("note", match.note)
            .put("scorers", JSONArray(match.scorers))
            .put("assists", JSONArray(match.assists)))
    }
}.toString()

fun loadMatches(raw: String): List<Match> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Match(
            o.optString("result"), o.optInt("scored"), o.optInt("conceded"), o.optString("note"),
            o.optJSONArray("scorers")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) } } ?: emptyList(),
            o.optJSONArray("assists")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) } } ?: emptyList()
        )
    }
}.getOrDefault(emptyList())

fun formationSlots(formation: String): List<String> = when (formation) {
    "5-3-2" -> listOf("GK", "LB", "CB", "CB", "CB", "RB", "CM", "CM", "CM", "ST", "ST")
    "3-5-2" -> listOf("GK", "CB", "CB", "CB", "LM", "CM", "CDM", "CM", "RM", "ST", "ST")
    "4-4-2" -> listOf("GK", "LB", "CB", "CB", "RB", "LM", "CM", "CM", "RM", "ST", "ST")
    "4-3-3" -> listOf("GK", "LB", "CB", "CB", "RB", "CM", "CDM", "CM", "LW", "ST", "RW")
    "4-1-2-1-2" -> listOf("GK", "LB", "CB", "CB", "RB", "CDM", "CM", "CM", "CAM", "ST", "ST")
    else -> listOf("GK", "LB", "CB", "CB", "RB", "CDM", "CDM", "LW", "CAM", "RW", "ST")
}

fun roleScore(p: Player, role: String): Int {
    if (p.position == "UNK") return -1000
    if (!p.statsKnown) {
        val positionFit = when {
            p.position == role -> 12
            role == "ST" && p.position == "CF" || role == "CF" && p.position == "ST" -> 7
            role in listOf("CM", "CDM", "CAM") && p.position in listOf("CM", "CDM", "CAM") -> 5
            role in listOf("LW", "RW") && p.position in listOf("LM", "RM", "LW", "RW") -> 4
            role in listOf("LB", "RB") && p.position in listOf("LB", "RB", "LM", "RM") -> 3
            else -> -15
        }
        return (p.rating + positionFit).coerceIn(0, 100)
    }
    val weighted = when (role) {
        "GK" -> p.gkr * 0.55 + p.gkh * 0.45
        "CB" -> p.tac * 0.30 + p.str * 0.25 + p.spe * 0.15 + p.sta * 0.12 + p.pas * 0.10 + p.acc * 0.08
        "LB", "RB" -> p.spe * 0.18 + p.acc * 0.15 + p.sta * 0.16 + p.tac * 0.20 + p.pas * 0.12 + p.str * 0.08 + p.con * 0.06 + p.sho * 0.05
        "CDM" -> p.tac * 0.25 + p.str * 0.15 + p.sta * 0.15 + p.pas * 0.18 + p.con * 0.10 + p.spe * 0.07 + p.acc * 0.05 + p.sho * 0.05
        "CM" -> p.pas * 0.22 + p.sta * 0.16 + p.con * 0.17 + p.tac * 0.12 + p.spe * 0.08 + p.acc * 0.08 + p.str * 0.07 + p.sho * 0.10
        "CAM" -> p.pas * 0.20 + p.con * 0.20 + p.sho * 0.18 + p.acc * 0.10 + p.spe * 0.10 + p.sta * 0.08 + p.str * 0.04 + p.tac * 0.10
        "LW", "RW", "LM", "RM" -> p.spe * 0.18 + p.acc * 0.17 + p.con * 0.18 + p.sho * 0.18 + p.pas * 0.12 + p.sta * 0.08 + p.tac * 0.03 + p.str * 0.03 + p.rating * 0.03
        "ST", "CF" -> p.sho * 0.28 + p.spe * 0.18 + p.acc * 0.15 + p.con * 0.15 + p.str * 0.10 + p.sta * 0.06 + p.pas * 0.08
        else -> p.rating.toDouble()
    }
    val natural = when {
        p.position == role -> 12
        role == "ST" && p.position == "CF" || role == "CF" && p.position == "ST" -> 8
        role in listOf("LB", "RB") && p.position in listOf("LM", "RM") -> 2
        role in listOf("LM", "RM") && p.position in listOf("LB", "RB", "LW", "RW") -> 5
        role in listOf("LW", "RW") && p.position in listOf("LM", "RM", "ST", "CF") -> 4
        role in listOf("CM", "CDM", "CAM") && p.position in listOf("CM", "CDM", "CAM") -> 5
        role == "CB" && p.position in listOf("LB", "RB", "CDM") -> 0
        else -> -18
    }
    val goalkeeperPenalty = if (p.position == "GK" && role != "GK") 60 else 0
    return (weighted + natural - goalkeeperPenalty).toInt().coerceIn(0, 100)
}

fun optimiseLineup(players: List<Player>, formation: String): List<LineupSlot> {
    val available = players.filter { it.position != "UNK" }.toMutableList()
    return formationSlots(formation).map { role ->
        val candidates = available.filter { role != "GK" || it.position == "GK" }
        val chosen = candidates.maxByOrNull { roleScore(it, role) }
            ?.takeIf { roleScore(it, role) >= 0 }
        if (chosen != null) available.remove(chosen)
        LineupSlot(role, chosen, chosen?.let { roleScore(it, role) } ?: 0)
    }
}

fun benchRecommendations(players: List<Player>, formation: String): List<Player> {
    val starters = optimiseLineup(players, formation).mapNotNull { it.player }.toSet()
    return players.filter { it !in starters && it.position != "UNK" }
        .sortedWith(compareByDescending<Player> { it.rating }.thenBy { it.name })
        .take(7)
}

fun tacticalAdvice(formation: String, style: String): List<String> {
    val base = mutableListOf<String>()
    when (formation) {
        "5-3-2" -> {
            base += "Use the three centre-backs as a compact central block; do not drag all three toward the ball."
            base += "Wing-backs provide width, but avoid sending both forward at the same time. Keep one side protected."
            base += "Use one central midfielder as the defensive screen and the other two to support progression and second balls."
            base += "Two strikers should offer a passing option and a forward run; avoid forcing a through ball when the lane is closed."
        }
        "4-3-3" -> {
            base += "Keep a holding midfielder behind the ball when full-backs advance."
            base += "Wingers stretch the defence; use cutbacks and low-risk passes rather than sprinting into every defender."
            base += "Press with the nearest forward while midfield blocks the central passing lane."
        }
        "3-5-2" -> {
            base += "Protect the wide channels: wing-backs must recover when possession is lost."
            base += "Keep one midfielder disciplined in front of the back three."
            base += "Use two forwards for combinations, not only direct long balls."
        }
        "4-4-2" -> {
            base += "Keep the two central midfielders close enough to protect the centre."
            base += "Wide midfielders track opposing full-backs and counter into space."
            base += "One striker can come short while the other attacks the space behind."
        }
        else -> {
            base += "Keep two midfielders connected to the defence; do not let both attack beyond the ball."
            base += "Use the wide attackers to stretch the pitch and the attacking midfielder between the lines."
            base += "When possession is lost, recover the central lane before chasing the ball."
        }
    }
    base += "Defending: jockey and contain first, use a standing tackle when the opponent exposes the ball, and slide only when the angle and timing are safe."
    base += "Attacking: scan before receiving, use short passes to draw pressure, then accelerate into space; choose shots when the shooting lane is clear."
    if (style.contains("Aggressive", true) || style.contains("High press", true)) {
        base += "Because your style is aggressive, avoid an uncontrolled all-out press: trigger pressure near the ball and keep cover behind the pressing player."
    }
    return base
}

fun developmentFocus(player: Player): List<String> {
    if (!player.statsKnown) return listOf("Enter verified card attributes before setting a development priority.")
    val priorities = when (player.position) {
        "GK" -> listOf("GKR" to player.gkr, "GKH" to player.gkh, "STA" to player.sta, "STR" to player.str)
        "CB", "LB", "RB" -> listOf("TAC" to player.tac, "STR" to player.str, "SPE" to player.spe, "STA" to player.sta, "PAS" to player.pas)
        "CDM" -> listOf("TAC" to player.tac, "PAS" to player.pas, "STA" to player.sta, "STR" to player.str, "CON" to player.con)
        "CM", "CAM", "LM", "RM" -> listOf("PAS" to player.pas, "CON" to player.con, "STA" to player.sta, "SHO" to player.sho, "ACC" to player.acc)
        "LW", "RW", "CF", "ST" -> listOf("SHO" to player.sho, "SPE" to player.spe, "ACC" to player.acc, "CON" to player.con, "PAS" to player.pas)
        else -> listOf("STA" to player.sta, "PAS" to player.pas, "CON" to player.con)
    }
    return priorities.sortedBy { it.second }.take(3).map { (attribute, value) ->
        "$attribute is currently $value — consider prioritising it if it matches your in-game coach options."
    }
}

fun squadBalanceAdvice(players: List<Player>): List<String> {
    if (players.isEmpty()) return listOf("Add your real squad to receive a squad-depth audit.")
    val advice = mutableListOf<String>()
    val count = players.size
    val goalkeepers = players.count { it.position == "GK" }
    val centreBacks = players.count { it.position == "CB" }
    val fullBacks = players.count { it.position == "LB" || it.position == "RB" }
    val midfielders = players.count { it.position in listOf("CDM", "CM", "CAM", "LM", "RM") }
    val attackers = players.count { it.position in listOf("LW", "RW", "CF", "ST") }
    advice += "Registered squad: $count players."
    if (goalkeepers < 2) advice += "Goalkeeper cover is thin: add or register another GK if available."
    if (centreBacks < 3) advice += "Centre-back depth is limited for a back three or five."
    if (fullBacks < 2) advice += "Wide defensive cover is limited; check LB/RB options."
    if (midfielders < 4) advice += "Midfield rotation is limited; prioritise reliable central cover."
    if (attackers < 3) advice += "Forward depth is limited; check striker and wide-attacker options."
    if (count < 16) advice += "Your entered squad is small for rotation; add more of your actual players before relying on depth recommendations."
    if (advice.size == 1) advice += "Your entered position mix has basic coverage. Compare individual attributes and role fit before choosing the XI."
    advice += "Depth counts use the natural positions you entered; they do not judge player quality or official DLS squad rules."
    return advice
}

data class PlayerContribution(val name: String, val goals: Int, val assists: Int, val appearancesWithContribution: Int)

fun playerContributionStats(matches: List<Match>): List<PlayerContribution> {
    val contributions = linkedMapOf<String, Triple<Int, Int, Int>>()
    matches.forEach { match ->
        (match.scorers + match.assists).distinctBy { it.lowercase() }.forEach { name ->
            val key = name.lowercase()
            val current = contributions[key] ?: Triple(0, 0, 0)
            val goals = match.scorers.count { it.equals(name, true) }
            val assists = match.assists.count { it.equals(name, true) }
            contributions[key] = Triple(current.first + goals, current.second + assists, current.third + 1)
        }
    }
    return contributions.map { (key, values) ->
        val displayName = matches.asSequence().flatMap { (it.scorers + it.assists).asSequence() }
            .firstOrNull { it.equals(key, true) } ?: key
        PlayerContribution(displayName, values.first, values.second, values.third)
    }.sortedWith(compareByDescending<PlayerContribution> { it.goals + it.assists }.thenByDescending { it.goals }.thenBy { it.name })
}

data class PlayerContributionTrend(
    val name: String,
    val recentGoals: Int,
    val recentAssists: Int,
    val previousGoals: Int,
    val previousAssists: Int
)

fun playerContributionTrends(matches: List<Match>): List<PlayerContributionTrend> {
    if (matches.size < 10) return emptyList()
    val recent = matches.take(5)
    val previous = matches.drop(5).take(5)
    val names = (recent + previous).flatMap { it.scorers + it.assists }.distinctBy { it.lowercase() }
    return names.map { name ->
        PlayerContributionTrend(
            name = name,
            recentGoals = recent.sumOf { match -> match.scorers.count { it.equals(name, true) } },
            recentAssists = recent.sumOf { match -> match.assists.count { it.equals(name, true) } },
            previousGoals = previous.sumOf { match -> match.scorers.count { it.equals(name, true) } },
            previousAssists = previous.sumOf { match -> match.assists.count { it.equals(name, true) } }
        )
    }.sortedWith(
        compareByDescending<PlayerContributionTrend> { it.recentGoals + it.recentAssists }
            .thenBy { it.name }
    )
}

fun tacticalEventInsights(matches: List<Match>): List<String> {
    val categories = linkedMapOf(
        "[COUNTER]" to "Opponent counter-attacks",
        "[HIGH PRESS]" to "High-press situations",
        "[MIDFIELD LOST]" to "Midfield possession losses",
        "[MISSED CHANCE]" to "Missed chances",
        "[GK ERROR]" to "Goalkeeper errors",
        "[DEFENSIVE ERROR]" to "Defensive errors",
        "[SET PIECE]" to "Set-piece situations"
    )
    if (matches.isEmpty()) return listOf("No matches logged yet. Record matches and tag observed events.")
    val recent = matches.take(5)
    val previous = if (matches.size >= 10) matches.drop(5).take(5) else emptyList()
    fun count(period: List<Match>, tag: String) = period.sumOf { match ->
        Regex(Regex.escape(tag), RegexOption.IGNORE_CASE).findAll(match.note).count()
    }
    val insights = categories.mapNotNull { (tag, label) ->
        val recentCount = count(recent, tag)
        val previousCount = count(previous, tag)
        if (recentCount == 0 && previousCount == 0) null
        else {
            val trend = when {
                previous.isEmpty() -> "no earlier five-match comparison"
                recentCount > previousCount -> "up from $previousCount in the previous five"
                recentCount < previousCount -> "down from $previousCount in the previous five"
                else -> "unchanged from the previous five"
            }
            Triple(label, recentCount, trend)
        }
    }.sortedWith(compareByDescending<Triple<String, Int, String>> { it.second }.thenBy { it.first })
    if (insights.isEmpty()) return listOf("No tagged tactical events in the latest five matches. Tag only events you personally observed.")
    val output = insights.take(4).map { (label, recentCount, trend) ->
        "$label: $recentCount in the latest ${recent.size} match(es); $trend. Review notes and footage before changing tactics."
    }.toMutableList()
    if (previous.isEmpty()) output += "Log at least ten matches to compare the latest five with a complete previous five-match sample."
    output += "These are counts of manually tagged notes, not automatic detection or proof of the cause of a result."
    return output
}

fun compareRecentPeriods(matches: List<Match>): List<String> {
    if (matches.size < 10) return listOf("Log at least ten matches to compare the latest five with the previous five.")
    val recent = matches.take(5)
    val previous = matches.drop(5).take(5)
    fun averageScored(period: List<Match>) = period.sumOf { it.scored }.toDouble() / period.size
    fun averageConceded(period: List<Match>) = period.sumOf { it.conceded }.toDouble() / period.size
    fun winRate(period: List<Match>) = period.count { it.result.equals("Win", true) } * 100.0 / period.size
    val scoredChange = averageScored(recent) - averageScored(previous)
    val concededChange = averageConceded(recent) - averageConceded(previous)
    val winChange = winRate(recent) - winRate(previous)
    fun signed(value: Double) = (if (value > 0) "+" else "") + String.format(java.util.Locale.US, "%.1f", value)
    val result = mutableListOf<String>()
    result += "Latest five vs previous five: goals scored per match ${signed(scoredChange)}, goals conceded per match ${signed(concededChange)}, win rate ${signed(winChange)} percentage points."
    when {
        concededChange <= -0.4 && scoredChange >= -0.2 -> result += "Trend signal: defensive results improved without a meaningful drop in scoring."
        scoredChange >= 0.4 && concededChange <= 0.2 -> result += "Trend signal: attacking output improved while goals conceded stayed broadly stable."
        concededChange >= 0.4 -> result += "Trend signal: defensive results worsened; inspect match evidence before changing personnel or formation."
        scoredChange <= -0.4 -> result += "Trend signal: attacking output fell; review chance creation and shot quality."
        else -> result += "Trend signal: no large change in these basic results. Continue logging and test one change at a time."
    }
    result += "This comparison describes results only; it cannot identify the cause of a change."
    return result
}

fun performanceInsights(matches: List<Match>): List<String> {
    if (matches.isEmpty()) return listOf("Log matches to generate evidence-based performance trends.")
    val recent = matches.take(5)
    val avgConceded = recent.sumOf { it.conceded }.toDouble() / recent.size
    val avgScored = recent.sumOf { it.scored }.toDouble() / recent.size
    val losses = recent.count { it.result.equals("Loss", true) }
    val cleanSheets = recent.count { it.conceded == 0 }
    val insights = mutableListOf<String>()
    if (avgConceded >= 2.0) insights += "Defensive alert: ${"%.1f".format(avgConceded)} goals conceded per match across the last ${recent.size}; inspect the footage to identify whether the cause is central space, wide channels, counters or goalkeeping."
    if (avgScored < 1.0) insights += "Attacking alert: fewer than one goal per match in this sample; review chance quality, shot selection and forward support."
    if (losses >= 3) insights += "Form alert: $losses losses in the last ${recent.size}; change one tactical variable at a time and compare the next results."
    if (recent.size >= 3 && cleanSheets == 0) insights += "No clean sheets in the last ${recent.size}; review defensive transitions and goalkeeper evidence rather than assuming the goalkeeper is the only cause."
    if (insights.isEmpty()) insights += "No major scoring alert in this small sample. Keep logging matches and avoid changing a working system without evidence."
    insights += "These are statistical signals from your logged results, not proof of the cause of a goal."
    return insights
}

fun nextCoachingActions(players: List<Player>, matches: List<Match>, formation: String): List<String> {
    val actions = mutableListOf<String>()
    if (players.size < 11) actions += "Enter the rest of your squad and accurate card attributes so the starting XI and depth audit are meaningful."
    if (players.none { it.position == "GK" }) actions += "Add your goalkeeper's real GKR and GKH values before assessing shot-stopping."
    if (matches.isEmpty()) actions += "Record your next three Dream League Live results, including goals scored, goals conceded and a short note about each match."
    else {
        val recent = matches.take(5)
        if (recent.sumOf { it.conceded } > recent.sumOf { it.scored }) actions += "Review the last match footage for the first cause of goals conceded: exposed centre, wide channel, counterattack or goalkeeper error."
        if (recent.count { it.result.equals("Loss", true) } >= 3) actions += "Test one tactical change at a time for the next two matches; do not change formation, style and XI simultaneously."
        if (recent.sumOf { it.scored } == 0) actions += "Prioritise chance creation and striker role fit; review whether shots are being taken from clear positions."
    }
    actions += "Use the recommended XI for $formation, then compare its role fit with your own match experience."
    actions += "Train only the attributes that are both weak for the player's role and available through your in-game coaches; record changes afterwards."
    return actions.distinct().take(3)
}

fun buildCoachReport(players: List<Player>, matches: List<Match>, formation: String, style: String, progress: List<PlayerProgress> = emptyList()): String {
    val squad = if (players.isEmpty()) "No players entered." else players.joinToString("\n") {
        "- ${it.name}: ${it.position}, OVR ${it.rating}; SPE ${it.spe}, ACC ${it.acc}, STA ${it.sta}, STR ${it.str}, CON ${it.con}, PAS ${it.pas}, SHO ${it.sho}, TAC ${it.tac}, GKR ${it.gkr}, GKH ${it.gkh}"
    }
    val games = if (matches.isEmpty()) "No match results entered." else matches.take(10).joinToString("\n") { match ->
        val scorers = if (match.scorers.isEmpty()) "Scorers not recorded" else "Scorers: ${match.scorers.joinToString(", ")}"
        val assists = if (match.assists.isEmpty()) "Assists not recorded" else "Assists: ${match.assists.joinToString(", ")}"
        "- ${match.result} ${match.scored}-${match.conceded}: ${match.note}\n  $scorers; $assists"
    }
    val contributions = playerContributionStats(matches).take(15).joinToString("\n") {
        "- ${it.name}: ${it.goals} goals, ${it.assists} assists (in ${it.appearancesWithContribution} recorded contribution matches)"
    }.ifBlank { "No player contributions recorded." }
    val development = if (progress.isEmpty()) "No player development snapshots recorded." else progress.take(30).joinToString("\n") { "- ${it.playerName}: OVR ${it.rating}, ${it.attribute} ${it.attributeValue}, ${java.text.DateFormat.getDateInstance().format(java.util.Date(it.recordedAt))} ${it.note}" }
    return """DLS COACH AI — COACHING REVIEW
Game: Dream League Soccer 2026
Mode: Dream League Live
Preferred style: Aggressive attacking with strong defence
Formation: $formation
Tactical approach: $style

SQUAD
$squad

RECENT MATCHES
$games

PLAYER DEVELOPMENT SNAPSHOTS
$development

PLAYER MATCH CONTRIBUTIONS
$contributions

Please analyse only the evidence provided. Identify the biggest tactical and squad weaknesses, recommend a suitable formation and starting XI from the listed players, explain defensive problems and attacking movement, and give me three specific changes to test. Do not invent player attributes or claim to have watched footage that is not attached."""
}
