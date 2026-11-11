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

data class Player(val name: String, val position: String, val rating: Int)
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
                1 -> SquadScreen(players, onAdd = { showPlayerDialog = true }, onDelete = { players = players - it }, modifier = Modifier.padding(padding))
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
fun SquadScreen(players: List<Player>, onAdd: () -> Unit, onDelete: (Player) -> Unit, modifier: Modifier = Modifier) {
    Page(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Squad Lab", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Button(onClick = onAdd) { Text("Add player") }
        }
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
        Text("Footage is shared through Android's share sheet. Choose ChatGPT if available; the app does not read or analyse game footage by itself.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CoachScreen(players: List<Player>, matches: List<Match>, formation: String, style: String, onShare: () -> Unit, modifier: Modifier = Modifier) {
    val conceded = matches.sumOf { it.conceded }
    val scored = matches.sumOf { it.scored }
    val losses = matches.count { it.result.equals("Loss", true) }
    Page(modifier) {
        Text("ChatGPT Coach", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Generate a coaching brief using the information you have entered.")
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your data snapshot", fontWeight = FontWeight.Bold)
                Text("Squad players: ${players.size}")
                Text("Matches recorded: ${matches.size}")
                Text("Goals scored: $scored  |  Goals conceded: $conceded")
                Text("Recorded losses: $losses")
                Text("Formation: $formation")
                Text("Style: $style")
            }
        }
        Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) { Text("Share coaching report") }
        Text("Tip: add the full squad, then share screenshots or match recordings separately for a more reliable review.")
    }
}

@Composable
fun AddPlayerDialog(onDismiss: () -> Unit, onAdd: (Player) -> Unit) {
    var name by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("CM") }
    var rating by remember { mutableStateOf("90") }
    val positions = listOf("GK", "CB", "LB", "RB", "CDM", "CM", "CAM", "LM", "RM", "LW", "RW", "CF", "ST")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add squad player") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Player name") }, singleLine = true)
                OutlinedTextField(rating, { rating = it.filter(Char::isDigit).take(3) }, label = { Text("Overall rating") }, singleLine = true)
                Text("Position")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    positions.forEach { p -> FilterChip(selected = position == p, onClick = { position = p }, label = { Text(p) }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            val value = rating.toIntOrNull()
            if (name.isNotBlank() && value != null && value in 1..100) onAdd(Player(name.trim(), position, value))
        }) { Text("Save") } },
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
    players.forEach { put(JSONObject().put("name", it.name).put("position", it.position).put("rating", it.rating)) }
}.toString()

fun loadPlayers(raw: String): List<Player> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Player(o.optString("name"), o.optString("position"), o.optInt("rating"))
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

fun buildCoachReport(players: List<Player>, matches: List<Match>, formation: String, style: String): String {
    val squad = if (players.isEmpty()) "No players entered." else players.joinToString("\n") { "- ${it.name}: ${it.position}, OVR ${it.rating}" }
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
