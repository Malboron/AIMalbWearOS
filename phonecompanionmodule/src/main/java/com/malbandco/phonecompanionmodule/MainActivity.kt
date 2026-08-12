package com.malbandco.phonecompanionmodule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malbandco.phonecompanionmodule.presentation.CompanionViewModel
import com.malbandco.phonecompanionmodule.presentation.SyncStatus
import com.malbandco.phonecompanionmodule.ui.theme.AIMalbTheme
import android.content.Context
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val prefs = com.malbandco.phonecompanionmodule.data.local.CompanionPrefs(newBase)
        val context = applyLocale(newBase, prefs.appLanguage)
        super.attachBaseContext(context)
    }

    private fun applyLocale(context: Context, language: String): Context {
        if (language == "system") return context
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

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
    var showPromptEditor by remember { mutableStateOf(false) }
    val syncStatus = viewModel.syncStatus.value
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val neonCyan = Color(0xFF00E5FF)

    // v1.2.5: Полноэкранный редактор промпта в отдельном окне
    if (showPromptEditor) {
        Dialog(
            onDismissRequest = { showPromptEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                var localPrompt by remember { mutableStateOf(viewModel.getSystemPrompt()) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .statusBarsPadding()
                ) {
                    Text(
                        stringResource(R.string.prompt_editor_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    Box(modifier = Modifier.weight(1f)) {
                        SimpleInputField(
                            value = localPrompt, 
                            onValueChange = { localPrompt = it }, 
                            placeholder = "", 
                            singleLine = false
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { 
                            viewModel.resetSystemPrompt()
                            localPrompt = viewModel.getSystemPrompt()
                        }) {
                            Text(stringResource(R.string.reset_default), color = Color.Gray)
                        }
                        Button(
                            onClick = { 
                                viewModel.setSystemPrompt(localPrompt)
                                showPromptEditor = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
                        ) {
                            Text(stringResource(R.string.save_changes), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

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
            text = stringResource(R.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = stringResource(R.string.companion_app),
            fontSize = 14.sp,
            color = neonCyan,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(R.string.groq_key_hint), color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { keyVisible = !keyVisible }) {
                    Icon(
                        imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(R.string.show_key),
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { viewModel.verifyKey(apiKey) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                enabled = syncStatus !is SyncStatus.Verifying && syncStatus !is SyncStatus.Syncing
            ) {
                Text(stringResource(R.string.verify_key), color = Color.White)
            }

            Button(
                onClick = { viewModel.syncToWatch(apiKey) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                enabled = syncStatus !is SyncStatus.Verifying && syncStatus !is SyncStatus.Syncing
            ) {
                Text(stringResource(R.string.sync_to_watch), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showPromptEditor = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors()
        ) {
            Text(stringResource(R.string.edit_sys_prompt), color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Selector
        var lang by remember { mutableStateOf(viewModel.getAppLanguage()) }
        val langOptions = listOf("system", "ru", "en")
        val langNames = listOf(stringResource(R.string.lang_system), stringResource(R.string.lang_ru), stringResource(R.string.lang_en))
        
        TextButton(
            onClick = {
                val nextIndex = (langOptions.indexOf(lang) + 1) % langOptions.size
                val newLang = langOptions[nextIndex]
                viewModel.setAppLanguage(newLang)
                lang = newLang
                (context as? ComponentActivity)?.recreate()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("${stringResource(R.string.language_label)}: ${langNames[langOptions.indexOf(lang)]}", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (syncStatus) {
            SyncStatus.Idle -> {
                Spacer(modifier = Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "github.com/Malboron/AIMalbWearOS", 
                        color = neonCyan, 
                        fontSize = 11.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { 
                            uriHandler.openUri("https://github.com/Malboron/AIMalbWearOS") 
                        }
                    )
                    Text("v1.1.1-beta (Build 13)", color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            SyncStatus.Verifying -> {
                CircularProgressIndicator(color = neonCyan, modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.verifying_key), color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            SyncStatus.Syncing -> {
                CircularProgressIndicator(color = Color.Magenta, modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.syncing_data), color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            SyncStatus.Success -> {
                Text(stringResource(R.string.sync_success), color = neonCyan, fontWeight = FontWeight.Bold)
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.resetStatus()
                }
            }
            is SyncStatus.Error -> {
                Text("🔴 ${syncStatus.message}", color = Color.Red, fontSize = 11.sp, textAlign = TextAlign.Center)
                TextButton(onClick = { viewModel.resetStatus() }) {
                    Text(stringResource(R.string.dismiss), color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SimpleInputField(value: String, onValueChange: (String) -> Unit, placeholder: String, singleLine: Boolean = true) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (placeholder.isNotEmpty()) {
            Text(placeholder, style = TextStyle(fontSize = 10.sp, color = Color.Gray))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray, shape = MaterialTheme.shapes.small)
                .padding(8.dp)
        )
    }
}
