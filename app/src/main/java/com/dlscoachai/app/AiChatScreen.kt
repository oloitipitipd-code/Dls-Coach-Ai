package com.dlscoachai.app

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class AiChatMessage(val role: String, val content: String)

@Composable
fun AiChatScreen(
    prefs: SharedPreferences,
    players: List<Player>,
    matches: List<Match>,
    formation: String,
    style: String,
    modifier: Modifier = Modifier
) {
    var endpoint by remember { mutableStateOf(prefs.getString("cloud_ai_endpoint", "") ?: "") }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<AiChatMessage>() }
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("DLS COACH AI · ONLINE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Cloud AI uses your saved squad and match records. No screenshot is required for chat. This does not connect to or control your DLS account.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it.trim(); prefs.edit().putString("cloud_ai_endpoint", endpoint).apply() },
            label = { Text("Cloud AI HTTPS endpoint") },
            placeholder = { Text("https://your-worker.example.workers.dev") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Saved players: ${players.size}", style = MaterialTheme.typography.labelMedium)
            Text("Recorded matches: ${matches.size}", style = MaterialTheme.typography.labelMedium)
        }
        HorizontalDivider()
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (messages.isEmpty()) {
                item { Text("Ask me to select your strongest XI, compare formations, analyse your recorded results, or create a development plan.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(messages) { message ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(if (message.role == "assistant") "DLS COACH AI" else "YOU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(message.content)
                    }
                }
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message your AI manager") },
            minLines = 2,
            maxLines = 4
        )
        Button(
            enabled = !busy && draft.isNotBlank() && endpoint.startsWith("https://"),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val prompt = draft.trim()
                draft = ""
                error = ""
                messages.add(AiChatMessage("user", prompt))
                busy = true
                scope.launch {
                    try {
                        val answer = withContext(Dispatchers.IO) {
                            requestCloudCoach(endpoint, messages.toList(), players, matches, formation, style)
                        }
                        messages.add(AiChatMessage("assistant", answer))
                    } catch (e: Exception) {
                        error = e.message ?: "Could not reach the cloud AI. Check the endpoint and internet connection."
                    } finally {
                        busy = false
                    }
                }
            }
        ) { Text(if (busy) "Thinking…" else "Send to AI") }
        Text("Cloud AI calls may incur provider charges. Only connect a service and share data if you accept its pricing and privacy terms.", style = MaterialTheme.typography.labelSmall)
    }
}

private fun requestCloudCoach(
    endpoint: String,
    messages: List<AiChatMessage>,
    players: List<Player>,
    matches: List<Match>,
    formation: String,
    style: String
): String {
    val playerArray = JSONArray()
    players.forEach { p ->
        playerArray.put(JSONObject()
            .put("name", p.name).put("position", p.position).put("rating", p.rating)
            .put("speed", p.spe).put("acceleration", p.acc).put("stamina", p.sta)
            .put("strength", p.str).put("control", p.con).put("passing", p.pas)
            .put("shooting", p.sho).put("tackling", p.tac)
            .put("goalkeeping_reflexes", p.gkr).put("goalkeeping_handling", p.gkh)
            .put("attributes_confirmed", p.statsKnown)
        )
    }
    val matchArray = JSONArray()
    matches.take(30).forEach { m ->
        matchArray.put(JSONObject().put("result", m.result).put("goals_for", m.scored)
            .put("goals_against", m.conceded).put("notes", m.note)
            .put("scorers", JSONArray(m.scorers)).put("assists", JSONArray(m.assists)))
    }
    val messageArray = JSONArray()
    messages.takeLast(20).forEach { m ->
        messageArray.put(JSONObject().put("role", m.role).put("content", m.content))
    }
    val payload = JSONObject()
        .put("messages", messageArray)
        .put("context", JSONObject()
            .put("game", "Dream League Soccer 2026")
            .put("formation", formation).put("playing_style", style)
            .put("players", playerArray).put("matches", matchArray)
        )

    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15000
        readTimeout = 45000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
    }
    try {
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        if (status !in 200..299) error(json.optString("error", "Cloud AI request failed (HTTP $status)."))
        return json.optString("answer").takeIf { it.isNotBlank() }
            ?: error("Cloud AI returned an empty response.")
    } finally {
        connection.disconnect()
    }
}
