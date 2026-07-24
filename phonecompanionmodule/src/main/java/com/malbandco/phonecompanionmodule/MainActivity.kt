package com.malbandco.phonecompanionmodule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malbandco.phonecompanionmodule.presentation.CompanionViewModel
import com.malbandco.phonecompanionmodule.presentation.SyncStatus
import com.malbandco.phonecompanionmodule.ui.theme.AIMalbTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIMalbTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    CompanionScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionScreen(viewModel: CompanionViewModel = viewModel()) {
    var apiKey by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var showNetworkSettings by remember { mutableStateOf(false) }
    val syncStatus = viewModel.syncStatus.value

    val neonCyan = Color(0xFF00E5FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AIMalb",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Companion App",
            fontSize = 14.sp,
            color = neonCyan,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Groq API Key", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { keyVisible = !keyVisible }) {
                    Icon(
                        imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Visibility",
                        tint = neonCyan
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = neonCyan,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = neonCyan,
                cursorColor = neonCyan,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Split Actions as requested in v45
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { viewModel.verifyKey(apiKey) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                enabled = syncStatus !is SyncStatus.Verifying && syncStatus !is SyncStatus.Syncing
            ) {
                Text("Verify Key", color = Color.White)
            }

            Button(
                onClick = { viewModel.syncToWatch(apiKey) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                enabled = syncStatus !is SyncStatus.Verifying && syncStatus !is SyncStatus.Syncing
            ) {
                Text("Sync to Watch", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Network Settings Toggle
        TextButton(onClick = { showNetworkSettings = !showNetworkSettings }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Network Settings", color = Color.Gray, fontSize = 14.sp)
                Icon(
                    imageVector = if (showNetworkSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = showNetworkSettings) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = viewModel.getUseDoH(),
                        onCheckedChange = { viewModel.setUseDoH(it) },
                        colors = CheckboxDefaults.colors(checkedColor = neonCyan)
                    )
                    Text("Use DNS-over-HTTPS (Quad9)", color = Color.White, fontSize = 12.sp)
                }
                
                var pHost by remember { mutableStateOf(viewModel.getProxyHost()) }
                var pPort by remember { mutableStateOf(viewModel.getProxyPort()) }
                
                OutlinedTextField(
                    value = pHost,
                    onValueChange = { pHost = it; viewModel.setProxyHost(it) },
                    label = { Text("Proxy Host", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
                )
                OutlinedTextField(
                    value = pPort,
                    onValueChange = { pPort = it; viewModel.setProxyPort(it) },
                    label = { Text("Proxy Port", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Feedback
        when (syncStatus) {
            SyncStatus.Idle -> {}
            SyncStatus.Verifying -> {
                CircularProgressIndicator(color = neonCyan, modifier = Modifier.size(24.dp))
                Text("Verifying via Quad9...", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            SyncStatus.Syncing -> {
                CircularProgressIndicator(color = Color.Magenta, modifier = Modifier.size(24.dp))
                Text("Syncing to watch...", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            SyncStatus.Success -> {
                Text("🟢 Done!", color = neonCyan, fontWeight = FontWeight.Bold)
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.resetStatus()
                }
            }
            is SyncStatus.Error -> {
                Text("🔴 ${syncStatus.message}", color = Color.Red, fontSize = 11.sp, textAlign = TextAlign.Center)
                TextButton(onClick = { viewModel.resetStatus() }) {
                    Text("Dismiss", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}
