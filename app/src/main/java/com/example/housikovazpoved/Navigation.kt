package com.example.housikovazpoved.ui.theme

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.housikovazpoved.*
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

// --- Barevné schéma inspirované Apple designem ---
object AppleTheme {
    val Blue = Color(0xFF007AFF)
    val Background = Color(0xFFF2F2F7)
    val SecondaryBackground = Color.White
    val Label = Color.Black
    val SecondaryLabel = Color(0x993C3C43) // Černá s 60% průhledností
    val Separator = Color(0x33C6C6C8) // Šedá s 20% průhledností
    val Red = Color(0xFFFF3B30)
    val Green = Color(0xFF34C759)
    val ButtonGray = Color(0xFFE5E5EA)
}

// --- Cesty pro navigaci ---
object Routes {
    const val MENU = "menu"
    const val PLAYER_SETUP = "player_setup"
    const val GAME = "game"
    const val GAME_OVER = "gameOver"
    const val SETTINGS = "settings"
}

// --- Hlavní navigační graf ---
@Composable
fun AppNavigation(
    gameViewModel: GameViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val gameUiState by gameViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = Routes.MENU) {
        composable(Routes.MENU) {
            MenuScreen(
                onStartGameClick = { navController.navigate(Routes.PLAYER_SETUP) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                hapticsEnabled = settingsUiState.hapticsEnabled
            )
        }
        composable(Routes.PLAYER_SETUP) {
            PlayerSetupScreen(
                onStartGame = { players ->
                    gameViewModel.setPlayers(players)
                    navController.navigate(Routes.GAME) { popUpTo(Routes.MENU) }
                },
                onBack = { navController.popBackStack() },
                hapticsEnabled = settingsUiState.hapticsEnabled
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                settingsState = settingsUiState,
                onToggleHaptics = { settingsViewModel.toggleHaptics(it) }
            )
        }
        composable(Routes.GAME) {
            LaunchedEffect(gameUiState.isGameOver) {
                if (gameUiState.isGameOver && !gameUiState.isLoading) {
                    navController.navigate(Routes.GAME_OVER) { popUpTo(Routes.GAME) { inclusive = true } }
                }
            }
            if (!gameUiState.isGameOver || gameUiState.isLoading) {
                GameScreen(
                    question = gameUiState.currentQuestion,
                    isLoading = gameUiState.isLoading,
                    canGoBack = gameUiState.canGoBack,
                    currentPlayerName = gameUiState.currentPlayerName,
                    hapticsEnabled = settingsUiState.hapticsEnabled,
                    onNextQuestion = { gameViewModel.nextQuestion() },
                    onPreviousQuestion = { gameViewModel.previousQuestion() }
                )
            }
        }
        composable(Routes.GAME_OVER) {
            GameOverScreen(
                onPlayAgainClick = {
                    gameViewModel.restartGame()
                    navController.navigate(Routes.GAME) { popUpTo(Routes.GAME_OVER) { inclusive = true } }
                },
                onBackToMenuClick = {
                    navController.navigate(Routes.MENU) { popUpTo(Routes.GAME_OVER) { inclusive = true } }
                },
                hapticsEnabled = settingsUiState.hapticsEnabled
            )
        }
    }
}


// ===== OBRAZOVKY V NOVÉM DESIGNU =====

@Composable
fun MenuScreen(onStartGameClick: () -> Unit, onSettingsClick: () -> Unit, hapticsEnabled: Boolean) {
    SetSystemBarColor(AppleTheme.Background)
    Column(
        modifier = Modifier.fillMaxSize().background(AppleTheme.Background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Housíkova\nzpověď",
            color = AppleTheme.Label,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 48.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        StyledButton(
            onClick = onStartGameClick,
            text = "Hrát",
            hapticsEnabled = hapticsEnabled,
            isPrimary = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        StyledButton(
            onClick = onSettingsClick,
            text = "Nastavení",
            hapticsEnabled = hapticsEnabled,
            isPrimary = false
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSetupScreen(onStartGame: (List<String>) -> Unit, onBack: () -> Unit, hapticsEnabled: Boolean) {
    var players by remember { mutableStateOf(listOf("")) }
    SetSystemBarColor(AppleTheme.Background)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kdo bude hrát?", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zpět") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppleTheme.Background)
            )
        },
        containerColor = AppleTheme.Background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(players) { index, player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = player,
                            onValueChange = { newValue -> players = players.toMutableList().also { it[index] = newValue } },
                            label = { Text("Jméno hráče ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = AppleTheme.Blue,
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            )
                        )
                        if (players.size > 1) {
                            IconButton(onClick = { players = players.toMutableList().also { it.removeAt(index) } }) {
                                Icon(Icons.Default.RemoveCircle, "Odebrat hráče", tint = AppleTheme.Red)
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { players = players + "" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Přidat dalšího hráče", color = AppleTheme.Blue)
                    }
                }
            }
            TextButton(
                onClick = { onStartGame(emptyList()) },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Pokračovat bez jmen", color = AppleTheme.Blue)
            }
            StyledButton(
                onClick = { onStartGame(players.filter { it.isNotBlank() }) },
                text = "Začít hru",
                hapticsEnabled = hapticsEnabled,
                isPrimary = true,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun GameScreen(
    question: Question?,
    isLoading: Boolean,
    canGoBack: Boolean,
    currentPlayerName: String?,
    hapticsEnabled: Boolean,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    SetSystemBarColor(AppleTheme.Background)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleTheme.Background)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (!isLoading) {
                    performHaptic(haptic, HapticFeedbackType.TextHandleMove, hapticsEnabled)
                    onNextQuestion()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            PlayerNameDisplay(
                name = currentPlayerName,
                isBonus = question?.isBonus == true
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(color = AppleTheme.Blue)
            } else {
                AnimatedQuestionCard(question = question)
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Klepni kamkoliv pro další otázku",
                color = AppleTheme.SecondaryLabel,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (canGoBack && !isLoading) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .clickable {
                        performHaptic(haptic, HapticFeedbackType.LongPress, hapticsEnabled)
                        onPreviousQuestion()
                    },
                shape = CircleShape,
                color = AppleTheme.Background.copy(alpha = 0.8f),
                shadowElevation = 4.dp
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Předchozí otázka",
                    tint = AppleTheme.Blue,
                    modifier = Modifier.padding(12.dp).size(24.dp)
                )
            }
        }
    }
}

@Composable
fun GameOverScreen(onPlayAgainClick: () -> Unit, onBackToMenuClick: () -> Unit, hapticsEnabled: Boolean) {
    SetSystemBarColor(AppleTheme.Background)
    var showKonfetti by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(AppleTheme.Background).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Konec hry",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = AppleTheme.Label,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                "Všechny otázky byly zodpovězeny!",
                fontSize = 18.sp,
                color = AppleTheme.SecondaryLabel,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 40.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            StyledButton(
                onClick = onPlayAgainClick,
                text = "Hrát znovu",
                isPrimary = true,
                hapticsEnabled = hapticsEnabled
            )
            Spacer(modifier = Modifier.height(16.dp))
            StyledButton(
                onClick = onBackToMenuClick,
                text = "Zpět do menu",
                isPrimary = false,
                hapticsEnabled = hapticsEnabled
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (showKonfetti) {
            val party = remember {
                Party(
                    speed = 0f, maxSpeed = 30f, damping = 0.9f, spread = 360,
                    colors = listOf(AppleTheme.Blue.toArgb(), AppleTheme.Red.toArgb(), AppleTheme.Green.toArgb(), 0xFFFFA500),
                    emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                    position = Position.Relative(0.5, 0.3)
                )
            }
            KonfettiView(
                parties = listOf(party),
                modifier = Modifier.fillMaxSize(),
                updateListener = object : OnParticleSystemUpdateListener {
                    override fun onParticleSystemEnded(system: PartySystem, activeSystems: Int) {
                        if (activeSystems == 0) showKonfetti = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsState: SettingsState,
    onToggleHaptics: (Boolean) -> Unit
) {
    SetSystemBarColor(AppleTheme.Background)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nastavení", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zpět") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppleTheme.Background)
            )
        },
        containerColor = AppleTheme.Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(top = 16.dp).background(AppleTheme.SecondaryBackground)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleHaptics(!settingsState.hapticsEnabled) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Haptická odezva", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settingsState.hapticsEnabled,
                        onCheckedChange = onToggleHaptics,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppleTheme.Green
                        )
                    )
                }
                Divider(color = AppleTheme.Separator, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}


// ===== PŘESTYLOVANÉ A POMOCNÉ KOMPONENTY =====

@Composable
fun StyledButton(
    onClick: () -> Unit,
    text: String,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            performHaptic(haptic, HapticFeedbackType.LongPress, hapticsEnabled)
            onClick()
        },
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = if (isPrimary) {
            ButtonDefaults.buttonColors(
                containerColor = AppleTheme.Blue,
                contentColor = Color.White
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = AppleTheme.ButtonGray,
                contentColor = AppleTheme.Blue
            )
        },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PlayerNameDisplay(name: String?, isBonus: Boolean) {
    val textToShow = if (isBonus) "🔥 ODPOVÍDAJÍ VŠICHNI 🔥" else name

    AnimatedVisibility(
        visible = !textToShow.isNullOrBlank(),
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Text(
            text = textToShow ?: "",
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBonus) AppleTheme.Red else AppleTheme.Label,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.2f),
                    offset = Offset(1f, 1f),
                    blurRadius = 2f
                )
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedQuestionCard(question: Question?) {
    AnimatedContent(
        targetState = question,
        transitionSpec = {
            val enter = slideInHorizontally(animationSpec = tween(durationMillis = 400, easing = EaseOut), initialOffsetX = { fullWidth -> -fullWidth }) + fadeIn()
            val exit = slideOutHorizontally(animationSpec = tween(durationMillis = 400, easing = EaseIn), targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
            enter togetherWith exit using SizeTransform(clip = false)
        },
        label = "QuestionTransition"
    ) { targetQuestion ->
        QuestionCard(question = targetQuestion)
    }
}

@Composable
fun QuestionCard(question: Question?) {
    val cardBgColor = AppleTheme.SecondaryBackground
    val textColor = if (question?.isBonus == true) AppleTheme.Red else AppleTheme.Label
    val cardShape = remember { RoundedCornerShape(20.dp) }

    Card(
        modifier = Modifier.widthIn(max = 340.dp).heightIn(min = 400.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = question?.text ?: "Načítání...",
                textAlign = TextAlign.Center,
                color = textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp
            )
        }
    }
}


// --- POMOCNÉ FUNKCE ---
fun performHaptic(haptic: HapticFeedback, type: HapticFeedbackType, isEnabled: Boolean) {
    if (isEnabled) {
        haptic.performHapticFeedback(type)
    }
}

@Composable
private fun SetSystemBarColor(color: Color) {
    val view = LocalView.current
    val isLightColor = color.red * 0.299 + color.green * 0.587 + color.blue * 0.114 > 0.5
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = color.toArgb()
            window.navigationBarColor = color.toArgb() // Sjednocení barvy i pro navigační lištu
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLightColor
        }
    }
}