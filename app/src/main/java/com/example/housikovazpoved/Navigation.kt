package com.example.housikovazpoved

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

// --- Barevné schéma inspirované Apple designem ---
object PremiumTheme {
    val Blue = Color(0xFF0A84FF)
    val Background = Color(0xFFF2F2F7)
    val CardBackground = Color(0xAAE8E8ED) // Poloprůhledná bílá pro "frosted glass" efekt
    val LabelPrimary = Color(0xFF000000)
    val LabelSecondary = Color(0x993C3C43)
    val LabelTertiary = Color(0x4D3C3C43)
    val Separator = Color(0x33C6C6C8)
    val Red = Color(0xFFFF453A)
    val Green = Color(0xFF32D74B)
    val ButtonPrimaryText = Color.White
    val ButtonSecondaryText = Color(0xFF0A84FF)
    val ButtonSecondaryBackground = Color(0xFFE5E5EA)
}

object Routes {
    const val MENU = "menu"
    const val PLAYER_SETUP = "player_setup"
    const val GAME = "game"
    const val GAME_OVER = "gameOver"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(gameViewModel: GameViewModel, settingsViewModel: SettingsViewModel) {
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

// ===== OBRAZOVKY V PRÉMIOVÉM DESIGNU =====

@Composable
fun MenuScreen(onStartGameClick: () -> Unit, onSettingsClick: () -> Unit, hapticsEnabled: Boolean) {
    SetSystemBarColor(PremiumTheme.Background)
    Column(
        modifier = Modifier.fillMaxSize().background(PremiumTheme.Background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Housíkova\nzpověď",
            color = PremiumTheme.LabelPrimary,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 52.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.1f),
                    offset = Offset(0f, 2f),
                    blurRadius = 4f
                )
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        PremiumButton(onClick = onStartGameClick, text = "Hrát", hapticsEnabled = hapticsEnabled)
        Spacer(modifier = Modifier.height(16.dp))
        PremiumButton(onClick = onSettingsClick, text = "Nastavení", hapticsEnabled = hapticsEnabled, isPrimary = false)
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSetupScreen(onStartGame: (List<String>) -> Unit, onBack: () -> Unit, hapticsEnabled: Boolean) {
    var players by remember { mutableStateOf(listOf("")) }
    SetSystemBarColor(PremiumTheme.Background)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kdo bude hrát?", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zpět") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PremiumTheme.Background)
            )
        },
        containerColor = PremiumTheme.Background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(players) { index, player ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                        OutlinedTextField(
                            value = player,
                            onValueChange = { newValue -> players = players.toMutableList().also { it[index] = newValue } },
                            label = { Text("Jméno hráče ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = PremiumTheme.Blue,
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            )
                        )
                        if (players.size > 1) {
                            IconButton(onClick = { players = players.toMutableList().also { it.removeAt(index) } }) {
                                Icon(Icons.Default.RemoveCircle, "Odebrat hráče", tint = PremiumTheme.Red)
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { players = players + "" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Přidat dalšího hráče", color = PremiumTheme.Blue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            TextButton(onClick = { onStartGame(emptyList()) }, modifier = Modifier.padding(bottom = 8.dp)) {
                Text("Pokračovat bez jmen", color = PremiumTheme.Blue)
            }
            PremiumButton(
                onClick = { onStartGame(players.filter { it.isNotBlank() }) },
                text = "Začít hru",
                hapticsEnabled = hapticsEnabled,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun GameScreen(
    question: Question?, isLoading: Boolean, canGoBack: Boolean, currentPlayerName: String?, hapticsEnabled: Boolean,
    onNextQuestion: () -> Unit, onPreviousQuestion: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val beautifulGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF8BC6EC), Color(0xFF9599E2))
    )

    SetSystemBarColor(Color(0xFF8BC6EC))

    Box(
        modifier = Modifier.fillMaxSize().background(beautifulGradient).clickable(
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
        // Efekt rozmazání pozadí za kartou
        Box(modifier = Modifier.fillMaxSize().blur(radius = 16.dp))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            PlayerNameDisplay(name = currentPlayerName, isBonus = question?.isBonus == true)
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            } else {
                AnimatedQuestionCard(question = question)
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Klepni kamkoliv pro další otázku",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (canGoBack && !isLoading) {
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp).clip(CircleShape)
                    .background(PremiumTheme.CardBackground.copy(alpha = 0.5f))
                    .clickable {
                        performHaptic(haptic, HapticFeedbackType.LongPress, hapticsEnabled)
                        onPreviousQuestion()
                    }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Předchozí otázka",
                    tint = PremiumTheme.LabelPrimary,
                    modifier = Modifier.padding(12.dp).size(24.dp)
                )
            }
        }
    }
}

@Composable
fun GameOverScreen(onPlayAgainClick: () -> Unit, onBackToMenuClick: () -> Unit, hapticsEnabled: Boolean) {
    SetSystemBarColor(PremiumTheme.Background)
    var showKonfetti by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(PremiumTheme.Background).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Konec hry", fontSize = 34.sp, fontWeight = FontWeight.Bold,
                color = PremiumTheme.LabelPrimary, modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                "Všechny otázky byly zodpovězeny!", fontSize = 18.sp, color = PremiumTheme.LabelSecondary,
                textAlign = TextAlign.Center, lineHeight = 24.sp, modifier = Modifier.padding(bottom = 40.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            PremiumButton(onClick = onPlayAgainClick, text = "Hrát znovu", hapticsEnabled = hapticsEnabled)
            Spacer(modifier = Modifier.height(16.dp))
            PremiumButton(onClick = onBackToMenuClick, text = "Zpět do menu", hapticsEnabled = hapticsEnabled, isPrimary = false)
            Spacer(modifier = Modifier.height(30.dp))
        }

        if (showKonfetti) {
            val party = remember {
                Party(
                    speed = 0f, maxSpeed = 30f, damping = 0.9f, spread = 360,
                    colors = listOf(PremiumTheme.Blue.toArgb(), PremiumTheme.Red.toArgb(), PremiumTheme.Green.toArgb(), 0xFFFFA500.toInt()),
                    emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                    position = Position.Relative(0.5, 0.3)
                )
            }
            KonfettiView(parties = listOf(party), modifier = Modifier.fillMaxSize(), updateListener = object : OnParticleSystemUpdateListener {
                override fun onParticleSystemEnded(system: PartySystem, activeSystems: Int) {
                    if (activeSystems == 0) showKonfetti = false
                }
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, settingsState: SettingsState, onToggleHaptics: (Boolean) -> Unit) {
    SetSystemBarColor(PremiumTheme.Background)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nastavení", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zpět") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PremiumTheme.Background)
            )
        },
        containerColor = PremiumTheme.Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp).clip(RoundedCornerShape(12.dp)).background(PremiumTheme.SecondaryBackground)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleHaptics(!settingsState.hapticsEnabled) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Haptická odezva", style = MaterialTheme.typography.bodyLarge, fontSize = 17.sp)
                    Switch(
                        checked = settingsState.hapticsEnabled,
                        onCheckedChange = onToggleHaptics,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PremiumTheme.Green
                        )
                    )
                }
                Divider(color = PremiumTheme.Separator, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

// ===== PRÉMIOVÉ A POMOCNÉ KOMPONENTY =====

@Composable
fun PremiumButton(onClick: () -> Unit, text: String, hapticsEnabled: Boolean, modifier: Modifier = Modifier, isPrimary: Boolean = true) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            performHaptic(haptic, HapticFeedbackType.LongPress, hapticsEnabled)
            onClick()
        },
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = if (isPrimary) ButtonDefaults.buttonColors(containerColor = PremiumTheme.Blue, contentColor = PremiumTheme.ButtonPrimaryText)
        else ButtonDefaults.buttonColors(containerColor = PremiumTheme.ButtonSecondaryBackground, contentColor = PremiumTheme.ButtonSecondaryText),
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
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBonus) Color(0xFFFFD700) else Color.White,
                textAlign = TextAlign.Center,
                shadow = Shadow(color = Color.Black.copy(alpha = 0.35f), offset = Offset(2f, 4f), blurRadius = 8f)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedQuestionCard(question: Question?) {
    val easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f) // plynulá a responzivní animace
    AnimatedContent(
        targetState = question,
        transitionSpec = {
            val enter = slideInHorizontally(animationSpec = tween(600, easing = easing), initialOffsetX = { -it - 100 }) + fadeIn(tween(300))
            val exit = slideOutHorizontally(animationSpec = tween(600, easing = easing), targetOffsetX = { it + 100 }) + fadeOut(tween(300))
            enter togetherWith exit using SizeTransform(clip = false)
        },
        label = "PremiumQuestionTransition"
    ) { targetQuestion ->
        QuestionCard(question = targetQuestion)
    }
}

@Composable
fun QuestionCard(question: Question?) {
    val cardShape = remember { RoundedCornerShape(24.dp) }
    val textColor = if (question?.isBonus == true) PremiumTheme.Red else PremiumTheme.LabelPrimary

    Card(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .heightIn(min = 420.dp)
            .shadow(elevation = 16.dp, shape = cardShape, ambientColor = PremiumTheme.Blue.copy(alpha = 0.5f)),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = PremiumTheme.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Stín řešíme přes Modifier.shadow
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = question?.text ?: "Načítání...",
                textAlign = TextAlign.Center,
                color = textColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp
            )
        }
    }
}

fun performHaptic(haptic: HapticFeedback, type: HapticFeedbackType, isEnabled: Boolean) {
    if (isEnabled) haptic.performHapticFeedback(type)
}

@Composable
private fun SetSystemBarColor(color: Color, isLight: Boolean? = null) {
    val view = LocalView.current
    val useLightStatusBars = isLight ?: (color.red * 0.299 + color.green * 0.587 + color.blue * 0.114 > 0.5)
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = color.toArgb()
            window.navigationBarColor = color.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useLightStatusBars
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = useLightStatusBars
        }
    }
}