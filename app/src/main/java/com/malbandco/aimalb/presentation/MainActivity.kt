package com.malbandco.aimalb.presentation

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.speech.RecognizerIntent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.malbandco.aimalb.presentation.components.JumpingDots
import com.malbandco.aimalb.presentation.components.QRCodeHelper
import com.malbandco.aimalb.presentation.theme.AIMalbTheme
import kotlin.math.abs

/**
 * Главная точка входа приложения. Управляет жизненным циклом и аппаратными кнопками.
 */
class MainActivity : ComponentActivity() {
    private var mainViewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            mainViewModel = vm
            WearApp(vm)
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("trigger_voice", false) == true) {
            mainViewModel?.triggerVoiceManually()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_STEM_2) {
            mainViewModel?.triggerVoiceManually()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}

object Routes {
    const val HOME = "home"
    const val LOADING = "loading"
    const val RESPONDING = "responding"
    const val SETTINGS_MENU = "settings_menu"
    const val AI_SETTINGS = "ai_settings"
    const val MODEL_SELECTION = "model_selection"
    const val PROMPT_EDITOR = "prompt_editor"
    const val ABOUT = "about"
    const val QR_CODE = "qr_code" // v1.2.5
}

/**
 * Корневой Composable. Управляет навигацией и системными флагами.
 */
@Composable
fun WearApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val navController = rememberSwipeDismissableNavController()
    val appState = viewModel.appState.value
    val isScreenLockActive = viewModel.isScreenLockActive.value
    val shouldTriggerVoice = viewModel.shouldTriggerVoice.value

    // Глобальный запуск микрофона на уровне приложения
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
            viewModel.onVoiceInputReceived(it)
        }
    }

    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
    }

    LaunchedEffect(shouldTriggerVoice) {
        if (shouldTriggerVoice) {
            voiceLauncher.launch(voiceIntent)
            viewModel.onVoiceTriggerConsumed()
        }
    }

    DisposableEffect(isScreenLockActive) {
        val activity = context as? ComponentActivity
        if (isScreenLockActive) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    LaunchedEffect(appState) {
        when (appState) {
            AppState.LOADING -> {
                if (navController.currentBackStackEntry?.destination?.route != Routes.LOADING) {
                    navController.navigate(Routes.LOADING)
                }
            }
            AppState.RESPONDING -> {
                if (navController.currentBackStackEntry?.destination?.route != Routes.RESPONDING) {
                    navController.navigate(Routes.RESPONDING) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            }
            AppState.IDLE -> {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute == Routes.RESPONDING || currentRoute == Routes.LOADING) {
                    navController.popBackStack(Routes.HOME, false)
                }
            }
        }
    }

    AIMalbTheme {
        AppScaffold {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = Routes.HOME
            ) {
                composable(Routes.HOME) { 
                    HomeScreen(viewModel, navController, onMicClick = { voiceLauncher.launch(voiceIntent) }) 
                }
                composable(Routes.LOADING) { LoadingScreen(viewModel) }
                composable(Routes.RESPONDING) { RespondingScreen(viewModel) }
                composable(Routes.SETTINGS_MENU) { SettingsMenuScreen(navController) }
                composable(Routes.AI_SETTINGS) { AiSettingsScreen(viewModel, navController) }
                composable(Routes.MODEL_SELECTION) { ModelSelectionScreen(viewModel, navController) }
                composable(Routes.PROMPT_EDITOR) { PromptEditorScreen(viewModel, navController) }
                composable(Routes.ABOUT) { AboutScreen(navController) }
                composable(Routes.QR_CODE) { QRCodeScreen() }
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: MainViewModel, 
    navController: NavHostController,
    onMicClick: () -> Unit
) {
    val responseText = viewModel.responseText.value

    LaunchedEffect(Unit) {
        viewModel.triggerAutoListenIfNeeded()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MicButton(
                onClick = onMicClick,
                size = 80.dp,
                icon = Icons.Default.Mic
            )
            if (responseText.startsWith("Error:") || responseText.startsWith("Ошибка")) {
                Text(
                    text = responseText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
            }
        }
        
        IconButton(
            onClick = { navController.navigate(Routes.SETTINGS_MENU) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Настройки", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun LoadingScreen(viewModel: MainViewModel) {
    val statusText = viewModel.statusText.value
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        JumpingDots()
    }
}

@Composable
fun RespondingScreen(viewModel: MainViewModel) {
    val visiblePhrases = viewModel.visiblePhrases.value
    val ttsIndex = viewModel.currentIndex.value
    val playbackState = viewModel.playbackState.value
    val listState = rememberTransformingLazyColumnState()
    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp

    LaunchedEffect(ttsIndex) {
        if (ttsIndex >= 0 && ttsIndex < visiblePhrases.size) {
            listState.animateScrollToItem(ttsIndex)
        }
    }

    var stabilizedIndex by remember { mutableIntStateOf(-1) }
    val currentCenterIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItems
            if (visibleItems.isEmpty()) -1
            else {
                val viewportCenter = layoutInfo.viewportSize.height / 2
                val bestItem = visibleItems.minByOrNull { item ->
                    val itemMiddle = item.offset + item.transformedHeight / 2
                    abs(itemMiddle - viewportCenter)
                }
                if (bestItem != null) {
                    val currentItem = visibleItems.find { it.index == stabilizedIndex }
                    if (currentItem == null || abs((bestItem.offset + bestItem.transformedHeight / 2) - viewportCenter) < abs((currentItem.offset + currentItem.transformedHeight / 2) - viewportCenter) - 20 || ttsIndex == bestItem.index) {
                        stabilizedIndex = bestItem.index
                    }
                }
                stabilizedIndex
            }
        }
    }

    ScreenScaffold(scrollState = listState) {
        Box(modifier = Modifier.fillMaxSize()) {
            TransformingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = screenHeight / 2, bottom = screenHeight / 2 + 80.dp),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(visiblePhrases.size) { index ->
                    val isActive = index == currentCenterIndex
                    val alpha by animateFloatAsState(targetValue = if (isActive) 1f else 0.2f, animationSpec = tween(100))
                    Text(
                        text = visiblePhrases[index],
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = if (isActive) 18.sp else 14.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).alpha(alpha).fillMaxWidth()
                    )
                }
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MicButton(onClick = { viewModel.triggerVoiceDirectly() }, size = 48.dp, icon = Icons.Default.Mic)
                Box(modifier = Modifier.size(16.dp))
                
                val controlIcon = when (playbackState) {
                    PlaybackState.PLAYING -> Icons.Default.Pause
                    PlaybackState.PAUSED -> Icons.Default.PlayArrow
                    PlaybackState.FINISHED -> Icons.Default.Replay
                }
                ControlButton(icon = controlIcon, onClick = { viewModel.togglePauseResume() })
            }
        }
    }
}

@Composable
fun SettingsMenuScreen(navController: NavHostController) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Text("Настройки", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp)) }
        item {
            TitleCard(onClick = { navController.navigate(Routes.AI_SETTINGS) }, title = { Text("Настройки ИИ") }) {
                Text("Ключ, Модель, Промпт", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            TitleCard(onClick = { navController.navigate(Routes.ABOUT) }, title = { Text("О проекте") }) {
                Text("Инфо и GitHub", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AiSettingsScreen(viewModel: MainViewModel, navController: NavHostController) {
    var key by remember { mutableStateOf(viewModel.getApiKey()) }
    val currentModel = viewModel.getModel()
    val verificationStatus = viewModel.verificationStatus.value

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 32.dp, bottom = 48.dp, start = 8.dp, end = 8.dp)
    ) {
        item { Text("AI Config", style = MaterialTheme.typography.titleSmall) }
        
        item {
            SimpleInputField(value = key, onValueChange = { key = it }, placeholder = "Groq API Key")
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { 
                        viewModel.setApiKey(key)
                        viewModel.verifyKey() 
                    },
                    modifier = Modifier.height(32.dp).padding(horizontal = 4.dp)
                ) {
                    Text("Проверить", fontSize = 10.sp)
                }
                
                when (verificationStatus) {
                    VerificationStatus.Idle -> {}
                    VerificationStatus.Verifying -> CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    VerificationStatus.Success -> Text("🟢", fontSize = 14.sp)
                    is VerificationStatus.Error -> Text("🔴", fontSize = 14.sp)
                }
            }
        }
        
        item {
            Button(
                onClick = { navController.navigate(Routes.MODEL_SELECTION) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text("Модель: ${currentModel.split("/").last()}", fontSize = 12.sp)
            }
        }
        
        item {
            Button(
                onClick = { navController.navigate(Routes.PROMPT_EDITOR) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Text("Системный промпт", fontSize = 12.sp)
            }
        }
        
        item {
            var autoListen by remember { mutableStateOf(viewModel.getAutoListen()) }
            SwitchButton(
                checked = autoListen,
                onCheckedChange = { 
                    autoListen = it
                    viewModel.setAutoListen(it) 
                },
                label = { Text("Слушать при запуске", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        item {
            var longPress by remember { mutableStateOf(viewModel.getLongPressEnabled()) }
            SwitchButton(
                checked = longPress,
                onCheckedChange = { 
                    longPress = it
                    viewModel.setLongPressEnabled(it) 
                },
                label = { Text("Запуск по зажатию", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { 
                    viewModel.setApiKey(key)
                    navController.popBackStack()
                }) { Text("OK", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun PromptEditorScreen(viewModel: MainViewModel, navController: NavHostController) {
    var prompt by remember { mutableStateOf(viewModel.getSystemPrompt()) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("System Prompt", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        
        SimpleInputField(value = prompt, onValueChange = { prompt = it }, placeholder = "Prompt Text", singleLine = false)
        
        Spacer(Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { 
                viewModel.resetSystemPrompt() 
                prompt = viewModel.getSystemPrompt()
            }) { Text("Сброс", fontSize = 10.sp) }
            
            Button(onClick = { 
                viewModel.setSystemPrompt(prompt)
                navController.popBackStack()
            }) { Text("Save", fontSize = 10.sp) }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun ModelSelectionScreen(viewModel: MainViewModel, navController: NavHostController) {
    val models = viewModel.availableModels
    val current = viewModel.getModel()
    
    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Text("Выберите модель", style = MaterialTheme.typography.titleSmall) }
        items(models.size) { index ->
            val model = models[index]
            val isSelected = model == current
            Button(
                onClick = { 
                    viewModel.setModel(model)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
            ) {
                Text(model.split("/").last(), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AboutScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("AIMalb", style = MaterialTheme.typography.titleMedium)
        Text("v1.2.6-beta (Build 66)", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Text("AI Assistant for Wear OS", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "github.com/Malboron/AIMalbWearOS", 
            color = Color.Cyan, 
            fontSize = 10.sp, 
            textAlign = TextAlign.Center,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            modifier = Modifier.clickable { navController.navigate(Routes.QR_CODE) }
        )
    }
}

/**
 * v1.2.5: Полноэкранный QR-код без кнопок.
 */
@Composable
fun QRCodeScreen() {
    val qrBitmap = remember { 
        QRCodeHelper.generateQRCode("https://github.com/Malboron/AIMalbWearOS", 300) 
    }
    
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GitHub Repository", fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            Image(
                bitmap = qrBitmap.asImageBitmap(), 
                contentDescription = "QR Code",
                modifier = Modifier.size(160.dp).background(Color.White).padding(8.dp)
            )
        }
    }
}

@Composable
fun SimpleInputField(value: String, onValueChange: (String) -> Unit, placeholder: String, singleLine: Boolean = true) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(placeholder, style = TextStyle(fontSize = 10.sp, color = Color.Gray))
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

@Composable
fun MicButton(onClick: () -> Unit, size: androidx.compose.ui.unit.Dp, icon: ImageVector) {
    Button(onClick = onClick, modifier = Modifier.size(size), contentPadding = PaddingValues(0.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = "Микрофон", modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
fun ControlButton(icon: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = "Управление", modifier = Modifier.size(26.dp))
        }
    }
}
