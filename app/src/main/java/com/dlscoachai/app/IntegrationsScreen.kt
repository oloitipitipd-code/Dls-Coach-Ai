package com.dlscoachai.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class GithubBuildStatus(
    val title: String,
    val status: String,
    val conclusion: String,
    val commit: String,
    val url: String
)

private suspend fun fetchLatestGithubBuild(): GithubBuildStatus = withContext(Dispatchers.IO) {
    val connection = (URL("https://api.github.com/repos/oloitipitipd-code/Dls-Coach-Ai/actions/runs?per_page=1")
        .openConnection() as HttpURLConnection)
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.setRequestProperty("User-Agent", "DLS-Coach-AI-Android")
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("GitHub returned HTTP $code")
        val runs = JSONObject(body).optJSONArray("workflow_runs")
            ?: error("GitHub did not return workflow information.")
        if (runs.length() == 0) error("No GitHub Actions builds were found.")
        val run = runs.getJSONObject(0)
        GithubBuildStatus(
            title = run.optString("display_title", "Latest workflow"),
            status = run.optString("status", "unknown"),
            conclusion = run.optString("conclusion", ""),
            commit = run.optString("head_sha", "").take(7),
            url = run.optString("html_url", "https://github.com/oloitipitipd-code/Dls-Coach-Ai/actions")
        )
    } finally {
        connection.disconnect()
    }
}

@Composable
fun IntegrationsScreen(
    onShareReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var buildMessage by remember { mutableStateOf("Not checked yet. This check is optional and uses the public GitHub API.") }
    var buildUrl by remember { mutableStateOf("https://github.com/oloitipitipd-code/Dls-Coach-Ai/actions") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Optional connections", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("DLS Coach AI works without signing in to GitHub or ChatGPT. Connect only when you choose.")

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GITHUB", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Read-only project connection")
                Text("Check the latest public build status and open the source repository. No GitHub password or token is requested.")
                Text(buildMessage, style = MaterialTheme.typography.bodySmall)
                Button(
                    enabled = !checking,
                    onClick = {
                        checking = true
                        buildMessage = "Checking the latest public workflow…"
                        scope.launch {
                            try {
                                val build = fetchLatestGithubBuild()
                                buildUrl = build.url
                                val result = if (build.status == "completed") build.conclusion else build.status
                                buildMessage = "${build.title}\nStatus: $result\nCommit: ${build.commit}"
                            } catch (error: Exception) {
                                buildMessage = "Could not check GitHub: ${error.localizedMessage ?: "network unavailable"}"
                            } finally {
                                checking = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (checking) "Checking…" else "Check latest GitHub build") }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/oloitipitipd-code/Dls-Coach-Ai")))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open DLS Coach AI repository") }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildUrl)))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open latest build details") }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CHATGPT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Optional, user-controlled report sharing")
                Text("Create a coaching report and choose ChatGPT from Android's share menu if it is installed. Nothing is sent until you select an app and confirm the share.")
                Button(onClick = onShareReport, modifier = Modifier.fillMaxWidth()) {
                    Text("Share coaching report with an app")
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("INDEPENDENCE & PRIVACY", fontWeight = FontWeight.Bold)
                Text("Squad, match and development records remain on this phone by default.")
                Text("GitHub checking is read-only and only runs when you tap the check button. Report sharing is manual. No API key, subscription or account is required for core coaching features.")
                Text("The app does not read private DLS game files, control gameplay or access hidden game data.")
            }
        }
    }
}
