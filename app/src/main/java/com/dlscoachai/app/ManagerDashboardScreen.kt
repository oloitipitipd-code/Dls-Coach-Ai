package com.dlscoachai.app

import android.content.SharedPreferences
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/**
 * Local management dashboard. Resource values are user-confirmed snapshots,
 * not a live read of the DLS account. Decisions are recommendations only;
 * the user performs any corresponding action inside DLS.
 */
@Composable
fun ManagerDashboardScreen(
    players: List<Player>,
    matches: List<Match>,
    formation: String,
    style: String,
    prefs: SharedPreferences,
    modifier: Modifier = Modifier
) {
    var opponentFormation by remember { mutableStateOf(prefs.getString("manager_opponent_formation", "Unknown") ?: "Unknown") }
    var opponentStyle by remember { mutableStateOf(prefs.getString("manager_opponent_style", "Unknown") ?: "Unknown") }
    var opponentFormationMenu by remember { mutableStateOf(false) }
    var opponentStyleMenu by remember { mutableStateOf(false) }
    val adaptiveRecommendation = remember(players, opponentFormation, opponentStyle) {
        AdaptiveTacticalManager.recommend(players, opponentFormation, opponentStyle)
    }
    val aiAssessment = adaptiveRecommendation?.assessment ?: remember(players) { chooseBestFormation(players) }
    val aiFormation = aiAssessment?.formation ?: "Not enough squad data"
    val aiLineup = aiAssessment?.lineup.orEmpty()
    val aiBench = aiAssessment?.let { benchRecommendations(players, it.formation) }.orEmpty()
    var coins by remember { mutableStateOf(prefs.getString("manager_coins", "") ?: "") }
    var gems by remember { mutableStateOf(prefs.getString("manager_gems", "") ?: "") }
    var coaches by remember { mutableStateOf(prefs.getString("manager_coaches", "") ?: "") }
    var gifts by remember { mutableStateOf(prefs.getString("manager_gifts", "") ?: "") }
    var snapshotTime by remember { mutableLongStateOf(prefs.getLong("manager_resources_time", 0L)) }
    var decisions by remember { mutableStateOf(loadManagerDecisions(prefs.getString("manager_decisions", "[]") ?: "[]")) }
    val recommendations = remember(players, matches, formation, style) {
        nextCoachingActions(players, matches, formation)
    }

    Page(modifier) {
        Text("MANAGEMENT DASHBOARD", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Your local football operations centre. Routine coaching work is prepared automatically; consequential actions remain approval-gated.")


        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("AUTONOMOUS MANAGER", fontWeight = FontWeight.Bold)
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                        Text("ON", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text("Operating mode: ${ManagerPolicy.MODE_LABEL} for local, reversible coaching work.", fontWeight = FontWeight.SemiBold)
                Text("The app can automatically refresh squad assessments, role-fit lineups, tactical plans, development priorities and summaries from information already saved on this device.", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("${ManagerPolicy.APPROVAL_LABEL} — ALWAYS REQUIRED", fontWeight = FontWeight.Bold)
                Text("Purchases, spending coins or gems, signing or releasing players, irreversible changes and account/profile actions must never be performed without your explicit approval.", style = MaterialTheme.typography.bodySmall)
                Text("Current boundary: this app has no authorised DLS control API. It cannot execute taps, gameplay, upgrades, purchases, reward claims or settings changes. You perform those actions inside DLS; the app records only outcomes you confirm.", style = MaterialTheme.typography.bodySmall)
                Text("No hidden automation, game-memory access, traffic interception or bypass of game protections.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OPPONENT-ADAPTIVE TACTICAL MANAGER", fontWeight = FontWeight.Bold)
                Text("The manager compares all six supported formations using your squad's role fit plus the opponent's observed shape and playing style.")
                Text("Opponent formation")
                Box {
                    OutlinedButton(onClick = { opponentFormationMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(opponentFormation)
                    }
                    DropdownMenu(expanded = opponentFormationMenu, onDismissRequest = { opponentFormationMenu = false }) {
                        (listOf("Unknown", "4-2-4") + AdaptiveTacticalManager.supportedFormations).distinct().forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                opponentFormation = option
                                prefs.edit().putString("manager_opponent_formation", option).apply()
                                opponentFormationMenu = false
                            })
                        }
                    }
                }
                Text("Opponent's observed playing style")
                Box {
                    OutlinedButton(onClick = { opponentStyleMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(opponentStyle)
                    }
                    DropdownMenu(expanded = opponentStyleMenu, onDismissRequest = { opponentStyleMenu = false }) {
                        AdaptiveTacticalManager.opponentStyles.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                opponentStyle = option
                                prefs.edit().putString("manager_opponent_style", option).apply()
                                opponentStyleMenu = false
                            })
                        }
                    }
                }
                Text("AI recommendation: ${adaptiveRecommendation?.assessment?.formation ?: "Not enough squad data"}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (adaptiveRecommendation != null) {
                    Text("Confidence: ${adaptiveRecommendation.confidence} · Tactical fit: ${adaptiveRecommendation.tacticalFit}/14")
                    Text(adaptiveRecommendation.reason, style = MaterialTheme.typography.bodySmall)
                    Text("Other leading options", fontWeight = FontWeight.SemiBold)
                    AdaptiveTacticalManager.rankedOptions(players, opponentFormation, opponentStyle).take(3).forEachIndexed { index, option ->
                        Text("${index + 1}. ${option.assessment.formation} · ${option.assessment.filledSlots}/11 positions · tactical fit ${option.tacticalFit}")
                    }
                } else {
                    Text("Add and confirm your squad before the manager can select a formation.")
                }
                Text("Important: opponent formation and style are currently entered from what you observe. Automatic live opponent recognition is not implemented yet. This is a tactical recommendation; it does not change DLS controls or the in-game formation.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI-SELECTED TEAM", fontWeight = FontWeight.Bold)
                Text("Formation selected for the current opponent profile: $aiFormation", fontWeight = FontWeight.SemiBold)
                Text("Confidence: ${aiAssessment?.confidence ?: "INSUFFICIENT DATA"} · Filled positions: ${aiAssessment?.filledSlots ?: 0}/11")
                if (players.isEmpty()) {
                    Text("No squad is registered yet. Import all player screens to let the AI build your team.")
                } else {
                    aiLineup.forEach { slot ->
                        Text("${slot.position}: ${slot.player?.let { "${it.name} · OVR ${it.rating}" } ?: "No suitable registered player"}")
                    }
                    HorizontalDivider()
                    Text("BENCH / ROTATION", fontWeight = FontWeight.SemiBold)
                    aiBench.forEach { player -> Text("• ${player.name} · ${player.position} · OVR ${player.rating}") }
                    if (aiAssessment?.confidence == "LOW") {
                        Text("Data quality is low. Confirm player positions and card attributes before relying on this XI.", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text("This is an in-app team plan. It does not change the lineup inside DLS; game setup must currently be completed manually.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ACCOUNT RESOURCE LEDGER", fontWeight = FontWeight.Bold)
                Text("Enter balances you can currently see in DLS. These are manual snapshots; the app is not connected to private account inventory.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(coins, { coins = it.filter(Char::isDigit) }, label = { Text("Coins") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(gems, { gems = it.filter(Char::isDigit) }, label = { Text("Gems") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(coaches, { coaches = it.filter(Char::isDigit) }, label = { Text("Available coaches (count)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(gifts, { gifts = it.filter(Char::isDigit) }, label = { Text("Unclaimed gifts (count)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = {
                    snapshotTime = System.currentTimeMillis()
                    prefs.edit().putString("manager_coins", coins).putString("manager_gems", gems)
                        .putString("manager_coaches", coaches).putString("manager_gifts", gifts)
                        .putLong("manager_resources_time", snapshotTime).apply()
                }, modifier = Modifier.fillMaxWidth()) { Text("Save confirmed resource snapshot") }
                if (snapshotTime > 0L) {
                    Text("Last confirmed: ${DateFormat.getDateTimeInstance().format(Date(snapshotTime))}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("COACHING APPROVAL QUEUE", fontWeight = FontWeight.Bold)
                Text("Recommendations are prepared from your saved squad and match records. Approving means you want to test the plan; it does not execute an action in DLS.", style = MaterialTheme.typography.bodySmall)
                if (recommendations.isEmpty()) {
                    Text("No recommendations are available yet.")
                } else recommendations.forEach { recommendation ->
                    val latestDecision = decisions.firstOrNull { it.text == recommendation }
                    val alreadyRecorded = latestDecision?.status == "APPROVED" || latestDecision?.status == "TESTED"
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(recommendation)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = !alreadyRecorded, onClick = {
                                decisions = listOf(ManagerDecision(recommendation, "APPROVED", System.currentTimeMillis())) + decisions
                                prefs.edit().putString("manager_decisions", saveManagerDecisions(decisions)).apply()
                            }) { Text(if (latestDecision?.status == "TESTED") "Tested" else if (alreadyRecorded) "Approved" else "Approve to test") }
                            if (latestDecision?.status == "APPROVED") {
                                TextButton(onClick = {
                                    decisions = listOf(ManagerDecision(recommendation, "TESTED", System.currentTimeMillis())) + decisions
                                    prefs.edit().putString("manager_decisions", saveManagerDecisions(decisions)).apply()
                                }) { Text("Mark tested") }
                            }
                            TextButton(onClick = {
                                decisions = listOf(ManagerDecision(recommendation, "DISMISSED", System.currentTimeMillis())) + decisions
                                prefs.edit().putString("manager_decisions", saveManagerDecisions(decisions)).apply()
                            }) { Text("Dismiss") }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("DECISION HISTORY", fontWeight = FontWeight.Bold)
                if (decisions.isEmpty()) Text("No decisions recorded yet.")
                decisions.take(12).forEach { decision ->
                    Text(decision.status + " · " + decision.text, fontWeight = FontWeight.Medium)
                    Text(DateFormat.getDateTimeInstance().format(Date(decision.time)), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("MANAGER STATUS", fontWeight = FontWeight.Bold)
                Text("Squad records: ${players.size} · Match records: ${matches.size}")
                Text("Current plan: $formation · $style")
                Text("Local management: available. Direct DLS account authentication, purchases, player upgrades, reward claims, setting changes and gameplay inputs are not enabled.")
                Text("Safe workflow: review visible information → confirm it → receive recommendation → approve or dismiss → perform any in-game action yourself → record the verified outcome.")
            }
        }
    }
}

private data class ManagerDecision(val text: String, val status: String, val time: Long)

private fun loadManagerDecisions(raw: String): List<ManagerDecision> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { i ->
        val item = array.optJSONObject(i) ?: return@mapNotNull null
        val text = item.optString("text").take(300)
        val status = item.optString("status")
        val time = item.optLong("time", 0L)
        if (text.isBlank() || status !in setOf("APPROVED", "DISMISSED", "TESTED")) null
        else ManagerDecision(text, status, time)
    }.take(100)
}.getOrDefault(emptyList())

private fun saveManagerDecisions(items: List<ManagerDecision>): String = JSONArray().apply {
    items.take(100).forEach { item ->
        put(JSONObject().put("text", item.text).put("status", item.status).put("time", item.time))
    }
}.toString()

/** Serialises the local manager ledger for the user's explicit JSON backup. */
fun exportManagerData(prefs: SharedPreferences): String {
    val decisions = sanitiseManagerDecisions(prefs.getString("manager_decisions", "[]") ?: "[]")
    return JSONObject()
        .put("coins", prefs.getString("manager_coins", "") ?: "")
        .put("gems", prefs.getString("manager_gems", "") ?: "")
        .put("coaches", prefs.getString("manager_coaches", "") ?: "")
        .put("gifts", prefs.getString("manager_gifts", "") ?: "")
        .put("resourcesTime", prefs.getLong("manager_resources_time", 0L).coerceAtLeast(0L))
        .put("decisions", decisions)
        .toString()
}

/** Restores only known manager fields; malformed optional fields fall back safely. */
fun restoreManagerData(prefs: SharedPreferences, raw: String) {
    runCatching {
        val data = JSONObject(raw)
        val decisions = sanitiseManagerDecisions(data.optJSONArray("decisions")?.toString() ?: "[]")
        prefs.edit()
            .putString("manager_coins", data.optString("coins", "").filter(Char::isDigit).take(12))
            .putString("manager_gems", data.optString("gems", "").filter(Char::isDigit).take(12))
            .putString("manager_coaches", data.optString("coaches", "").filter(Char::isDigit).take(6))
            .putString("manager_gifts", data.optString("gifts", "").filter(Char::isDigit).take(6))
            .putLong("manager_resources_time", data.optLong("resourcesTime", 0L).coerceAtLeast(0L))
            .putString("manager_decisions", decisions.toString())
            .apply()
    }
}

private fun sanitiseManagerDecisions(raw: String): JSONArray = runCatching {
    val source = JSONArray(raw)
    JSONArray().apply {
        for (i in 0 until minOf(source.length(), 100)) {
            val item = source.optJSONObject(i) ?: continue
            val text = item.optString("text").trim().take(300)
            val status = item.optString("status")
            if (text.isNotBlank() && status in setOf("APPROVED", "DISMISSED", "TESTED")) {
                put(JSONObject()
                    .put("text", text)
                    .put("status", status)
                    .put("time", item.optLong("time", 0L).coerceAtLeast(0L)))
            }
        }
    }
}.getOrDefault(JSONArray())
