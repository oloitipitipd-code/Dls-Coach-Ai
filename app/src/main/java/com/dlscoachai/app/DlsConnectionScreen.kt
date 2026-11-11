package com.dlscoachai.app

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class InstalledDlsApp(val packageName: String, val className: String, val label: String)

private fun findInstalledDlsApp(packageManager: PackageManager): InstalledDlsApp? {
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    @Suppress("DEPRECATION")
    val candidates = packageManager.queryIntentActivities(launcherIntent, 0)
    val match = candidates.firstOrNull {
        it.loadLabel(packageManager).toString().contains("Dream League Soccer", ignoreCase = true)
    } ?: return null
    val activity = match.activityInfo
    return InstalledDlsApp(
        packageName = activity.packageName,
        className = activity.name,
        label = match.loadLabel(packageManager).toString()
    )
}

@Composable
fun DlsConnectionScreen(
    captureRequested: Boolean,
    captureStatus: String,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dls_coach_data", 0) }
    var cameraSetting by remember { mutableStateOf(prefs.getString("game_camera_setting", "") ?: "") }
    var graphicsSetting by remember { mutableStateOf(prefs.getString("game_graphics_setting", "") ?: "") }
    var controlsSetting by remember { mutableStateOf(prefs.getString("game_controls_setting", "") ?: "") }
    var soundSetting by remember { mutableStateOf(prefs.getString("game_sound_setting", "") ?: "") }
    var otherSetting by remember { mutableStateOf(prefs.getString("game_other_setting", "") ?: "") }
    var detectionRefresh by remember { mutableStateOf(0) }
    var accountConfirmed by remember { mutableStateOf(prefs.getBoolean("dls_account_confirmed", false)) }

    fun saveSetting(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("DLS GAME LINK", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Open the installed game, capture its visible screen with your permission, and keep a record of your preferred in-game settings.")

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GAME CONNECTION", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                val installedGame = remember(detectionRefresh) { findInstalledDlsApp(context.packageManager) }
                if (installedGame != null) {
                    Text("Detected: ${installedGame.label}")
                    Text("Package: ${installedGame.packageName}", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = {
                            runCatching {
                                val launch = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_LAUNCHER)
                                    component = ComponentName(installedGame.packageName, installedGame.className)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(launch)
                            }.onFailure {
                                Toast.makeText(context, "Could not open DLS: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open Dream League Soccer") }
                } else {
                    Text("Dream League Soccer was not detected among launchable apps. Confirm DLS is installed, then tap Retry detection.")
                }
                OutlinedButton(onClick = { detectionRefresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Retry game detection") }
                Text("This connection opens the game through Android's normal app launcher. It does not inspect private game files or change game data.")
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MY DLS ACCOUNT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = accountConfirmed,
                        onCheckedChange = {
                            accountConfirmed = it
                            prefs.edit().putBoolean("dls_account_confirmed", it).apply()
                        }
                    )
                    Text("I confirm that I have a DLS account on this device")
                }
                Text(if (accountConfirmed) "Account status: confirmed by you on this phone; not directly authenticated by DLS Coach AI." else "Account status: not yet confirmed in this app.")
                Text("First Touch Games currently documents profile linking inside DLS through Sign in with Google on Android (or Sign in with Apple on iOS). This protects/transfers your game profile; it does not grant DLS Coach AI access to your account data.")
                Text("Keep signing in inside DLS itself. Never enter your Google password, DLS password, profile-transfer code or verification code into DLS Coach AI.")
                Text("For squad analysis, we will read information you deliberately share through screenshots or authorised screen capture. No private account data is requested.")
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LIVE SCREEN READING", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Status: $captureStatus")
                Text("Screen capture is user-authorised. Android displays its own permission and app/window selection prompt.")
                if (captureRequested) {
                    Button(onClick = onStopCapture, modifier = Modifier.fillMaxWidth()) { Text("Stop live screen reading") }
                } else {
                    Button(onClick = onStartCapture, modifier = Modifier.fillMaxWidth()) { Text("Start authorised screen reading") }
                }
                Text("Current capability: local OCR reads visible text. It does not yet reliably recognise players, the ball, formations, match events or every game setting. No gameplay controls are sent.")
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MY DLS SETTINGS PROFILE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Record the values you choose inside DLS. These notes are saved on this phone; the app cannot apply changes inside DLS automatically.")
                OutlinedTextField(cameraSetting, { cameraSetting = it; saveSetting("game_camera_setting", it) }, label = { Text("Camera setting") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(graphicsSetting, { graphicsSetting = it; saveSetting("game_graphics_setting", it) }, label = { Text("Graphics / performance") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(controlsSetting, { controlsSetting = it; saveSetting("game_controls_setting", it) }, label = { Text("Controls / layout") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(soundSetting, { soundSetting = it; saveSetting("game_sound_setting", it) }, label = { Text("Sound settings") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(otherSetting, { otherSetting = it; saveSetting("game_other_setting", it) }, label = { Text("Other settings / notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedButton(
                    onClick = {
                        cameraSetting = ""; graphicsSetting = ""; controlsSetting = ""; soundSetting = ""; otherSetting = ""
                        prefs.edit().remove("game_camera_setting").remove("game_graphics_setting")
                            .remove("game_controls_setting").remove("game_sound_setting").remove("game_other_setting").apply()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Clear settings notes") }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("WHAT THE APP CAN MANAGE", fontWeight = FontWeight.Bold)
                Text("DLS Coach AI can manage its own squad records, tactical plans, match history, player-development records, reports and capture session. It can open DLS and analyse authorised visible information.")
                Text("It cannot currently change DLS graphics, controls, formations or account settings itself. Direct game control would require an official supported interface from First Touch Games; no such interface has been verified.")
            }
        }
    }
}
