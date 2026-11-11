package com.dlscoachai.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
    val gkh: Int = 50
)
data class LineupSlot(val position: String, val player: Player?, val score: Int)
data class Match(val result: String, val scored: Int, val conceded: Int, val note: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DlsCoachApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlsCoachApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dls_coach_data", 0) }
    var tab by remember { mutableIntStateOf(0) }
    var players by remember { mutableStateOf(loadPlayers(prefs.getString("players", "[]") ?: "[]")) }
    var matches by remember { mutableStateOf(loadMatches(prefs.getString("matches", "[]") ?: "[]")) }
    var formation by remember { mutableStateOf(prefs.getString("formation", "4-2-3-1") ?: "4-2-3-1") }
    var style by remember { mutableStateOf(prefs.getString("style", "Aggressive attack + compact defence") ?: "Aggressive attack + compact defence") }
    var showPlayerDialog by remember { mutableStateOf(false) }
    var showMatchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(players) { prefs.edit().putString("players", savePlayers(players)).apply() }
    LaunchedEffect(matches) { prefs.edit().putString("matches", saveMatches(matches)).apply() }
    LaunchedEffect(formation) { prefs.edit().putString("formation", formation).apply() }
    LaunchedEffect(style) { prefs.edit().putString("style", style).apply() }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = context.contentResolver.getType(uri) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(share, "Share match footage for analysis"))
        }
    }

    val squadImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = context.contentResolver.getType(uri) ?: "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(share, "Send squad screenshot to ChatGPT for review"))
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
                    listOf("Home", "Squad", "Tactics", "Matches", "Coach").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(listOf("⌂", "⚽", "♟", "▤", "✦")[index]) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(players, matches, formation, style, Modifier.padding(padding))
                1 -> SquadScreen(
                    players,
                    onAdd = { showPlayerDialog = true },
                    onDelete = { players = players - it },
                    onUploadScreenshot = { squadImagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.padding(padding)
                )
                2 -> TacticsScreen(formation, style, { formation = it }, { style = it }, Modifier.padding(padding))
                3 -> MatchesScreen(matches, { showMatchDialog = true }, { mediaPicker.launch(arrayOf("image/*", "video/*")) }, Modifier.padding(padding))
                else -> CoachScreen(players, matches, formation, style, onShare = {
                    val report = buildCoachReport(players, matches, formation, style)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "DLS Coach AI - Match & Squad Report")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(Intent.createChooser(send, "Send coaching report"))
                }, modifier = Modifier.padding(padding))
            }
        }

        if (showPlayerDialog) {
            AddPlayerDialog(
                onDismiss = { showPlayerDialog = false },
                onAdd = { player -> players = players + player; showPlayerDialog = false }
            )
        }
        if (showMatchDialog) {
            AddMatchDialog(
                onDismiss = { showMatchDialog = false },
                onAdd = { match -> matches = listOf(match) + matches; showMatchDialog = false }
            )
        }
    }
}

@Composable
fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun HomeScreen(players: List<Player>, matches: List<Match>, formation: String, style: String, modifier: Modifier = Modifier) {
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
        Text("Start by adding your actual players and logging match results. Recommendations become more useful as your data improves.", style = MaterialTheme.typography.bodySmall)
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
            Text("Upload squad screenshot for AI review")
        }
        Text(
            "Choose a screenshot of your DLS 26 team. Android will let you share it with ChatGPT for image-based analysis. This app does not analyse images by itself.",
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
                            }
                            TextButton(onClick = { onDelete(player) }) { Text("Remove") }
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
fun MatchesScreen(matches: List<Match>, onAdd: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
    Page(modifier) {
        Text("Match Analyst", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("Log result") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Import footage") }
        }
        Text("Recent results", fontWeight = FontWeight.Bold)
        if (matches.isEmpty()) Text("No matches logged yet.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches) { match ->
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text("${match.result}  •  ${match.scored}-${match.conceded}", fontWeight = FontWeight.Bold)
                        if (match.note.isNotBlank()) Text(match.note)
                    }
                }
            }
        }
        Text("Tap Import footage, choose a match recording or screenshot, then select ChatGPT in Android's share menu if available. This app passes the file to ChatGPT; it does not analyse video on its own.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CoachScreen(players: List<Player>, matches: List<Match>, formation: String, style: String, onShare: () -> Unit, modifier: Modifier = Modifier) {
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
                    Text("Goal difference: ${scored - conceded}")
                    Text(if (conceded > scored) "Priority: improve defensive protection and chance prevention." else "Review both chance creation and defensive consistency.")
                }
            }
        }
        Text("RECOMMENDED STARTING XI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            if (players.size < 11) Text("You have entered ${players.size} players. Add at least 11 for a complete XI.")
        }
        Text("TACTICAL GAME PLAN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        tips.forEach { tip -> Text("• $tip") }
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your current setup", fontWeight = FontWeight.Bold)
                Text("Formation: $formation")
                Text("Style: $style")
            }
        }
        Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) { Text("Generate and share full coaching report") }
        Text("Important: this app recommends selection and training priorities. It cannot directly change player attributes, control the game or guarantee wins.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AddPlayerDialog(onDismiss: () -> Unit, onAdd: (Player) -> Unit) {
    var name by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("CM") }
    var rating by remember { mutableStateOf("90") }
    val statNames = listOf("SPE", "ACC", "STA", "STR", "CON", "PAS", "SHO", "TAC", "GKR", "GKH")
    val stats = remember { mutableStateMapOf<String, String>().apply { statNames.forEach { put(it, "70") } } }
    val positions = listOf("GK", "CB", "LB", "RB", "CDM", "CM", "CAM", "LM", "RM", "LW", "RW", "CF", "ST")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add player and card stats") },
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
                Text("Enter the numbers exactly as shown on the in-game player card (1–100). For a goalkeeper, prioritise GKR and GKH.", style = MaterialTheme.typography.bodySmall)
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
                    numbers[5]!!, numbers[6]!!, numbers[7]!!, numbers[8]!!, numbers[9]!!
                ))
            }
        }) { Text("Save player") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddMatchDialog(onDismiss: () -> Unit, onAdd: (Match) -> Unit) {
    var result by remember { mutableStateOf("Loss") }
    var scored by remember { mutableStateOf("0") }
    var conceded by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
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
                OutlinedTextField(note, { note = it }, label = { Text("What went wrong / well?") })
            }
        },
        confirmButton = { TextButton(onClick = {
            val a = scored.toIntOrNull()
            val b = conceded.toIntOrNull()
            if (a != null && b != null) onAdd(Match(result, a, b, note.trim()))
        }) { Text("Save match") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun savePlayers(players: List<Player>): String = JSONArray().apply {
    players.forEach { p ->
        put(JSONObject()
            .put("name", p.name).put("position", p.position).put("rating", p.rating)
            .put("spe", p.spe).put("acc", p.acc).put("sta", p.sta).put("str", p.str)
            .put("con", p.con).put("pas", p.pas).put("sho", p.sho).put("tac", p.tac)
            .put("gkr", p.gkr).put("gkh", p.gkh))
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
            o.optInt("sho", rating), o.optInt("tac", rating), o.optInt("gkr", rating), o.optInt("gkh", rating)
        )
    }
}.getOrDefault(emptyList())

fun saveMatches(matches: List<Match>): String = JSONArray().apply {
    matches.forEach { put(JSONObject().put("result", it.result).put("scored", it.scored).put("conceded", it.conceded).put("note", it.note)) }
}.toString()

fun loadMatches(raw: String): List<Match> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Match(o.optString("result"), o.optInt("scored"), o.optInt("conceded"), o.optString("note"))
    }
}.getOrDefault(emptyList())

fun formationSlots(formation: String): List<String> = when (formation) {
    "5-3-2" -> listOf("GK", "LB", "CB", "CB", "CB", "RB", "CM", "CM", "CM", "ST", "ST")
    "3-5-2" -> listOf("GK", "CB", "CB", "CB", "LM", "CM", "CDM", "CM", "RM", "ST", "ST")
    "4-4-2" -> listOf("GK", "LB", "CB", "CB", "RB", "LM", "CM", "CM", "RM", "ST", "ST")
    "4-3-3" -> listOf("GK", "LB", "CB", "CB", "RB", "CM", "CDM", "CM", "LW", "ST", "RW")
    else -> listOf("GK", "LB", "CB", "CB", "RB", "CDM", "CDM", "LW", "CAM", "RW", "ST")
}

fun roleScore(p: Player, role: String): Int {
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
    val available = players.toMutableList()
    return formationSlots(formation).map { role ->
        val chosen = available.maxByOrNull { roleScore(it, role) }
        if (chosen != null) available.remove(chosen)
        LineupSlot(role, chosen, chosen?.let { roleScore(it, role) } ?: 0)
    }
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

fun buildCoachReport(players: List<Player>, matches: List<Match>, formation: String, style: String): String {
    val squad = if (players.isEmpty()) "No players entered." else players.joinToString("\n") {
        "- ${it.name}: ${it.position}, OVR ${it.rating}; SPE ${it.spe}, ACC ${it.acc}, STA ${it.sta}, STR ${it.str}, CON ${it.con}, PAS ${it.pas}, SHO ${it.sho}, TAC ${it.tac}, GKR ${it.gkr}, GKH ${it.gkh}"
    }
    val games = if (matches.isEmpty()) "No match results entered." else matches.take(10).joinToString("\n") { "- ${it.result} ${it.scored}-${it.conceded}: ${it.note}" }
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

Please analyse only the evidence provided. Identify the biggest tactical and squad weaknesses, recommend a suitable formation and starting XI from the listed players, explain defensive problems and attacking movement, and give me three specific changes to test. Do not invent player attributes or claim to have watched footage that is not attached."""
}
