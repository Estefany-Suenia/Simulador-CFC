package com.example.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.QuestionSeeder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Question
import com.example.data.SimulationResult
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CfcApp(viewModel: CfcViewModel) {
    val coroutineScope = rememberCoroutineScope()
    
    MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
        val currentScreen = viewModel.currentScreen
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Show bottom navigation bar ONLY on main navigation screens (Home, Performance, Review)
                if (currentScreen in listOf(Screen.Home, Screen.Performance, Screen.Review)) {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Home,
                            onClick = { viewModel.navigateTo(Screen.Home) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Simulados") },
                            label = { Text("Simular") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        
                        NavigationBarItem(
                            selected = currentScreen == Screen.Performance,
                            onClick = { viewModel.navigateTo(Screen.Performance) },
                            icon = { Icon(Icons.Default.Star, contentDescription = "Desempenho") },
                            label = { Text("Desempenho") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        
                        NavigationBarItem(
                            selected = currentScreen == Screen.Review,
                            onClick = { viewModel.navigateTo(Screen.Review) },
                            icon = { Icon(Icons.Default.List, contentDescription = "Revisão") },
                            label = { Text("Revisão") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        Screen.Home -> HomeScreen(viewModel)
                        Screen.Simulator -> SimulatorScreen(viewModel)
                        Screen.Result -> ResultScreen(viewModel)
                        Screen.Performance -> PerformanceScreen(viewModel)
                        Screen.Review -> ReviewScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: CfcViewModel) {
    val history by viewModel.simulationHistory.collectAsStateWithLifecycle()
    val allQs by viewModel.allQuestions.collectAsStateWithLifecycle()
    var showImportDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Dark mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CFC Quest",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Pronto para ser aprovado?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            
            // Theme Mode Toggle
            IconButton(
                onClick = { viewModel.toggleDarkMode() },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(
                    imageVector = if (viewModel.isDarkMode) Icons.Default.Settings else Icons.Default.PlayArrow, // Fallbacks to represent sun/moon neatly in default icons
                    contentDescription = "Toggle Theme",
                    tint = if (viewModel.isDarkMode) CfcGold else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Daily Streak and goal (Duolingo Style)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Streak Flame icon box
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(CfcGold.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star, // Representing the streak fire
                        contentDescription = "Fogo de Ofensiva",
                        tint = CfcGold,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = "Ofensiva de Estudos",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${viewModel.userStreak} de fato dias consecutivos!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { (viewModel.dailyGoalCount.toFloat() / viewModel.dailyGoalTarget.toFloat()).coerceAtMost(1.0f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = CfcGold,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Meta diária: ${viewModel.dailyGoalCount}/${viewModel.dailyGoalTarget} respondidas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Config Simulator setup
        Text(
            text = "Novo Simulado",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Escolha a Edição da Prova",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val editions = listOf("Mista", "2026.1", "2025.2")
                    editions.forEach { ed ->
                        val isSelected = viewModel.selectedSimulationEdition == ed
                        Button(
                            onClick = { viewModel.selectedSimulationEdition = ed },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (ed == "Mista") "Mista 🌀" else "CFC $ed 📘",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Duração Recomendada: 4h",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "50 Questões aleatórias",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cronômetro",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = viewModel.isTimerVisible,
                            onCheckedChange = { viewModel.toggleTimerVisibility() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Button(
                    onClick = { viewModel.startSimulation(viewModel.selectedSimulationEdition) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("iniciar_simulado_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMEÇAR SIMULADO REAL",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Section Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Import custom button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showImportDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Importar",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Importar JSON",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bookmarked shortcut
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo(Screen.Review) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Revisar",
                        tint = CfcPurple,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Estudar Revisões",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Simulation History
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Histórico de Simulados",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            if (history.isNotEmpty()) {
                Text(
                    text = "Limpar Tudo",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { viewModel.removeAllHistory() }
                )
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Nenhum simulado realizado ainda",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Os resultados de suas tentativas oficiais de 50 questões serão gravados aqui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                history.forEach { res ->
                    HistoryItem(res)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }

    // Dynamic JSON Import Dialog
    if (showImportDialog) {
        ImportQuestionsDialog(viewModel) {
            showImportDialog = false
        }
    }
}

@Composable
fun HistoryItem(result: SimulationResult) {
    val dateStr = remember(result.timestamp) {
        try {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(result.timestamp))
        } catch (e: Exception) {
            "Data recente"
        }
    }

    val elapsedMinutes = result.durationSeconds / 60
    val elapsedRemainingSeconds = result.durationSeconds % 60
    val timeFormatted = String.format("%02d:%02d", elapsedMinutes, elapsedRemainingSeconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (result.approved) CfcGreen else CfcRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (result.approved) "Aprovado 🎉" else "Não Aprovado",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (result.approved) CfcGreen else CfcRed
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Edição: ${if (result.edition == "Mista") "Simulado Completo" else "CFC " + result.edition}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$dateStr • Tempo: $timeFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.0f%%", result.percentRating),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (result.approved) CfcGreen else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${result.correctAnswers} ACERTOS",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = CfcGreen
                )
            }
        }
    }
}

// SIMULATOR ACTIVE TEST SCREEN
@Composable
fun SimulatorScreen(viewModel: CfcViewModel) {
    if (viewModel.currentExamQuestions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentQuestion = viewModel.currentExamQuestions[viewModel.currentQuestionIndex]
    val isFirst = viewModel.currentQuestionIndex == 0
    val isLast = viewModel.currentQuestionIndex == viewModel.currentExamQuestions.size - 1
    val selectedOption = viewModel.selectedAnswers[viewModel.currentQuestionIndex]
    val totalQs = viewModel.currentExamQuestions.size
    
    var showQuitConfirm by remember { mutableStateOf(false) }

    val hasSavedStateFlow = viewModel.isQuestionSavedFlow(currentQuestion.id).collectAsStateWithLifecycle(initialValue = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header Card matching 'Professional Polish' Style
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SIMULADO CFC ${currentQuestion.edition}".uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentQuestion.subject,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Pulse timer box and Quit Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (viewModel.isTimerVisible) {
                            val mins = viewModel.elapsedSeconds / 60
                            val secs = viewModel.elapsedSeconds % 60
                            val formattedTime = String.format("%02d:%02d", mins, secs)

                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse_alpha"
                            )

                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .graphicsLayer { this.alpha = alpha }
                                        .background(Color(0xFFEF4444), CircleShape)
                                )
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showQuitConfirm = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Sair do Simulado",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Linear indicator & Question index text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { ((viewModel.currentQuestionIndex + 1).toFloat() / totalQs.toFloat()).coerceAtMost(1.0f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )

                    Text(
                        text = "${viewModel.currentQuestionIndex + 1} de $totalQs",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Question Statement & Options
        Column(
            modifier = Modifier
                .weight(1.0f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentQuestion.subject,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(CfcPurple.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Exame ${currentQuestion.edition}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = CfcPurple
                    )
                }

                Box(
                    modifier = Modifier
                        .background(CfcOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentQuestion.difficulty,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = CfcOrange
                    )
                }
            }

            // Statement text
            Text(
                text = currentQuestion.statement,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Dynamic Options list
            val options = listOf(
                "A" to currentQuestion.optionA,
                "B" to currentQuestion.optionB,
                "C" to currentQuestion.optionC,
                "D" to currentQuestion.optionD
            )

            options.forEach { (tag, text) ->
                val isSelected = selectedOption == tag
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectAnswer(viewModel.currentQuestionIndex, tag) }
                        .testTag("option_${tag}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 4.dp else 0.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Letter badge (Beautiful contrasting circle badge)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                    CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Bottom Controls Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            OutlinedButton(
                onClick = { viewModel.previousQuestion() },
                enabled = !isFirst,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Voltar", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Save for Review Bookmark icon button
            IconButton(
                onClick = { viewModel.toggleBookmark(currentQuestion.id) },
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        if (hasSavedStateFlow.value) CfcPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (hasSavedStateFlow.value) CfcPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Star, // Flame/Bookmark key
                    contentDescription = "Save for Revision",
                    tint = if (hasSavedStateFlow.value) CfcPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Next / Finish Button
            Button(
                onClick = {
                    if (isLast) {
                        viewModel.finishSimulation()
                    } else {
                        viewModel.nextQuestion()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLast) CfcGreen else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1.5f)
                    .height(50.dp)
                    .testTag("prosseguir_btn")
            ) {
                Text(
                    text = if (isLast) "FINALIZAR" else "Avançar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }

    // Quit Exam Confirmation Modal
    if (showQuitConfirm) {
        AlertDialog(
            onDismissRequest = { showQuitConfirm = false },
            title = { Text("Cancelar Simulado?") },
            text = { Text("Se você sair agora os seus dados atuais e respostas deste simulado serão perdidos. Tem certeza que deseja abortar a prova?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showQuitConfirm = false
                        viewModel.navigateTo(Screen.Home)
                    }
                ) {
                    Text("Sair e Perder Progresso", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitConfirm = false }) {
                    Text("Continuar Respondendo")
                }
            }
        )
    }
}

// SIMULATOR EXAM COMPLETED SUMMARY SCREEN
@Composable
fun ResultScreen(viewModel: CfcViewModel) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Result Top Illustration Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = (if (viewModel.lastApproved) CfcGreen else MaterialTheme.colorScheme.surface).copy(alpha = 0.1f)
            ),
            border = BorderStroke(1.5.dp, if (viewModel.lastApproved) CfcGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(if (viewModel.lastApproved) CfcGreen else CfcOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (viewModel.lastApproved) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = if (viewModel.lastApproved) "APROVADO NO SIMULADO!" else "EXAME NÃO APROVADO",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (viewModel.lastApproved) CfcGreen else CfcOrange
                )

                Text(
                    text = if (viewModel.lastApproved)
                        "Excelente trabalho! Você obteve acertos suficientes para passar no critério oficial do CFC (mínimo de 25 acertos de 50)."
                    else
                        "Sua meta deve ser no mínimo 25 acertos de 50. Use a correção inteligente detalhada abaixo para dominar as teorias!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Stats Numbers row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Correct box
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Acertos ✅", style = MaterialTheme.typography.bodySmall, color = CfcGreen)
                    Text(
                        text = "${viewModel.lastCorrectCount}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = CfcGreen
                    )
                }
            }

            // Incorrect box
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Erros ❌", style = MaterialTheme.typography.bodySmall, color = CfcRed)
                    Text(
                        text = "${viewModel.lastIncorrectCount}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = CfcRed
                    )
                }
            }

            // Percent rating box
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Aproveitamento", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Text(
                        text = String.format("%.0f%%", viewModel.lastPercentRating),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        // Correção Inteligente heading
        Text(
            text = "Correção Inteligente e Gabarito",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Toque em qualquer questão abaixo para ver o mini-resumo teórico e a explicação detalhada de cada alternativa:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        // Question Navigator buttons (1 to 50)
        // Let's create an elegant grid or list. Since Grid can scroll, a Lazy Row helps pick question fast!
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(viewModel.lastExamQuestions.size) { index ->
                val q = viewModel.lastExamQuestions[index]
                val selected = viewModel.lastAnswers[index]
                val isCorrect = selected == q.correctAnswer
                val isReviewing = viewModel.reviewSelectedQuestionIndex == index

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isReviewing) MaterialTheme.colorScheme.primary
                            else if (isCorrect) CfcGreen.copy(alpha = 0.15f)
                            else CfcRed.copy(alpha = 0.15f),
                            CircleShape
                        )
                        .border(
                            2.dp,
                            if (isReviewing) MaterialTheme.colorScheme.primary
                            else if (isCorrect) CfcGreen
                            else CfcRed,
                            CircleShape
                        )
                        .clickable { viewModel.reviewSelectedQuestionIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isReviewing) Color.White else if (isCorrect) CfcGreen else CfcRed
                    )
                }
            }
        }

        // Selected Question Review detail card
        if (viewModel.reviewSelectedQuestionIndex in viewModel.lastExamQuestions.indices) {
            val q = viewModel.lastExamQuestions[viewModel.reviewSelectedQuestionIndex]
            val sel = viewModel.lastAnswers[viewModel.reviewSelectedQuestionIndex]
            val isCorrect = sel == q.correctAnswer
            val hasSavedStateFlow = viewModel.isQuestionSavedFlow(q.id).collectAsStateWithLifecycle(initialValue = false)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gabarito Questão ${viewModel.reviewSelectedQuestionIndex + 1}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCorrect) CfcGreen else CfcRed
                        )

                        IconButton(
                            onClick = { viewModel.toggleBookmark(q.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Salvar para Revisar",
                                tint = if (hasSavedStateFlow.value) CfcPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = q.subject,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = q.statement,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // User responses mapping
                    Text(
                        text = "Sua resposta: ${sel ?: "Nenhuma"} — Alternativa Oficial Correta: ${q.correctAnswer}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCorrect) CfcGreen else CfcRed
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Didactical Explanation - Professor particular
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CfcPurple.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, CfcPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎓", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Explicação do Professor Particular (Análise Geral)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CfcPurple
                                )
                            }
                            Text(
                                text = q.correctExplanation,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Alternatives breakdowns
                    Text(
                        text = "Análise Prática das Alternativas",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    val answersList = listOf(
                        "Alternativa A" to q.explanationA,
                        "Alternativa B" to q.explanationB,
                        "Alternativa C" to q.explanationC,
                        "Alternativa D" to q.explanationD
                    )

                    answersList.forEach { (title, expl) ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = expl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Mini resumo teórico card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💡", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mini Resumo Teórico do Tema",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = q.summary,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Return Home action
        Button(
            onClick = { viewModel.navigateTo(Screen.Home) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("VOLTAR À TELA INICIAL", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// PERFORMANCE METRICS AND ANALYTICS
@Composable
fun PerformanceScreen(viewModel: CfcViewModel) {
    val history by viewModel.simulationHistory.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Aggregate statistics
    val totalSimulations = history.size
    val totalApproved = history.count { it.approved }

    // Calculating accuracy per subject
    // We will cross references history responses with corresponding question details in seeder!
    val allSeededQuestionsMap = remember {
        QuestionSeeder.getSeededQuestions().associateBy { it.id }
    }

    // Let's analyze discipline success metrics
    val subjectTotal = mutableMapOf<String, Int>()
    val subjectCorrect = mutableMapOf<String, Int>()

    var totalAnswersChecked = 0
    var totalAnswersCorrect = 0

    // Parse answers data JSON
    val moshi: Moshi = remember { Moshi.Builder().build() }
    val type: java.lang.reflect.ParameterizedType = remember { Types.newParameterizedType(Map::class.java, java.lang.Integer::class.java, String::class.java) }
    val adapter: com.squareup.moshi.JsonAdapter<Map<Int, String>> = remember { moshi.adapter(type) }

    history.forEach { res ->
        try {
            val answersMap = adapter.fromJson(res.answersDataJson) ?: emptyMap()
            answersMap.forEach { (qId, selectedOpt) ->
                val q = allSeededQuestionsMap[qId]
                if (q != null) {
                    val isCorrect = selectedOpt == q.correctAnswer
                    val currentT = subjectTotal[q.subject] ?: 0
                    subjectTotal[q.subject] = currentT + 1

                    val currentC = subjectCorrect[q.subject] ?: 0
                    if (isCorrect) {
                        subjectCorrect[q.subject] = currentC + 1
                        totalAnswersCorrect++
                    }
                    totalAnswersChecked++
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors on mock histories
        }
    }

    // Identify subjects with most errors
    val sortedSubjectsWithErrors = subjectTotal.keys.map { key ->
        val tot = subjectTotal[key] ?: 0
        val cor = subjectCorrect[key] ?: 0
        val err = tot - cor
        val accuracy = if (tot > 0) (cor.toDouble() / tot.toDouble()) * 100 else 0.0
        Triple(key, err, accuracy)
    }.sortedByDescending { it.second } // Most errors first

    val criticalDiscipline = sortedSubjectsWithErrors.firstOrNull()?.first ?: "Nenhuma (Sem histórico)"
    val bestDisciplineObj = sortedSubjectsWithErrors.sortedByDescending { it.third }.firstOrNull()
    val bestDiscipline = if (bestDisciplineObj != null && bestDisciplineObj.third > 0) bestDisciplineObj.first else "Nenhuma"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Análise de Desempenho",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Streak Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🔥", fontSize = 36.sp)
                Column {
                    Text(
                        text = "Status da Ofensiva",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Você completou ${viewModel.userStreak} dias de estudos oficiais seguidos!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Summary Metric boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Box 1
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Provas", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Text(
                        text = "$totalSimulations",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Box 2
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Aprovações", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Text(
                        text = "$totalApproved",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = CfcGreen
                    )
                }
            }

            // Box 3
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Acertos Geral", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    val generalAcc = if (totalAnswersChecked > 0) (totalAnswersCorrect.toDouble() / totalAnswersChecked.toDouble()) * 100 else 0.0
                    Text(
                        text = String.format("%.0f%%", generalAcc),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = if (generalAcc >= 50) CfcGreen else CfcOrange
                    )
                }
            }
        }

        // Focus recommendations card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Diretrizes de Foco",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Estudar mais (Crítico) 🚨", style = MaterialTheme.typography.bodySmall, color = CfcRed)
                        Text(
                            text = criticalDiscipline,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Seu ponto forte ⭐", style = MaterialTheme.typography.bodySmall, color = CfcGreen)
                        Text(
                            text = bestDiscipline,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Checklist of all 11 disciplines list
        Text(
            text = "Estatísticas por Matéria",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        val allCfcMatérias = listOf(
            "Contabilidade Geral", "Contabilidade de Custos", "Contabilidade Pública",
            "Auditoria", "Perícia", "Legislação e Ética", "Matemática Financeira",
            "Estatística", "Português", "Teoria da Contabilidade", "Controladoria"
        )

        allCfcMatérias.forEach { mat ->
            val tot = subjectTotal[mat] ?: 0
            val cor = subjectCorrect[mat] ?: 0
            val acc = if (tot > 0) (cor.toFloat() / tot.toFloat()) * 100.0f else 0.0f

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mat,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (tot > 0) String.format("%.0f%%", acc) else "Sem Dados",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (tot == 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else if (acc >= 50) CfcGreen
                            else CfcOrange
                        )
                    }

                    LinearProgressIndicator(
                        progress = { if (tot > 0) acc / 100.0f else 0.0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (acc >= 50f) CfcGreen else CfcOrange,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$tot respondidas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        Text(
                            text = "$cor acertos • ${tot - cor} erros",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(50.dp))
    }
}

// BOOKMARKED QUESTIONS AND REVISION MODE SCREEN
@Composable
fun ReviewScreen(viewModel: CfcViewModel) {
    val size = viewModel.bookmarkedQuestionsList.size
    val scrollState = rememberScrollState()

    // When the screen loads or remains empty, display beautifully
    if (viewModel.bookmarkedQuestionsList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(CfcPurple.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = CfcPurple,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = "Seu Caderno de Erros está Limpo!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Toque no ícone de marcador ⭐ (salvar para revisão) durante os simulados ou nas correções das questões erradas para estudar elas aqui mais tarde.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Button(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Fazer Simulado Agora")
                }
            }
        }
        return
    }

    val currentQIndex = viewModel.savedQuestionReviewIndex
    val q = viewModel.bookmarkedQuestionsList[currentQIndex]

    var userGuess by remember(q.id) { mutableStateOf<String?>(null) }
    var verCorrecao by remember(q.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Core Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Modo Revisão (Caderno de Erros)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CfcPurple
            )

            Text(
                text = "${currentQIndex + 1} de $size",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Question Statement and interactive choices
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(CfcPurple.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = q.subject,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = CfcPurple
                    )
                }

                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Exame ${q.edition}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Text(
                text = q.statement,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onBackground
            )

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Options selection for mock attempt in study
            val listOptions = listOf(
                "A" to q.optionA,
                "B" to q.optionB,
                "C" to q.optionC,
                "D" to q.optionD
            )

            listOptions.forEach { (tag, text) ->
                val isSelected = userGuess == tag
                val isCorrectTag = tag == q.correctAnswer
                val showingAnswersFeedback = verCorrecao

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!verCorrecao) {
                                userGuess = tag
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            showingAnswersFeedback && isCorrectTag -> CfcGreen.copy(alpha = 0.1f)
                            showingAnswersFeedback && isSelected -> CfcRed.copy(alpha = 0.1f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        width = if (isSelected || (showingAnswersFeedback && isCorrectTag)) 2.dp else 1.dp,
                        color = when {
                            showingAnswersFeedback && isCorrectTag -> CfcGreen
                            showingAnswersFeedback && isSelected -> CfcRed
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    when {
                                        showingAnswersFeedback && isCorrectTag -> CfcGreen
                                        showingAnswersFeedback && isSelected -> CfcRed
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected || (showingAnswersFeedback && isCorrectTag)) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Explanations expanded if verified clicked
            if (verCorrecao) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CfcPurple.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .border(1.dp, CfcPurple.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎓", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Correção do Professor Particular",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = CfcPurple
                            )
                        }
                        
                        Text(
                            text = q.correctExplanation,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                        )

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        Text(
                            text = "💡 Mini Resumo Teórico:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = q.summary,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
                        )
                    }
                }
            }
        }

        // Action controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Delete Bookmark
            Button(
                onClick = { viewModel.toggleBookmark(q.id) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar de Revisões", tint = Color.White)
            }

            // Toggle Correction panel
            Button(
                onClick = { verCorrecao = !verCorrecao },
                enabled = userGuess != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(
                    text = if (verCorrecao) "Ocultar Correção" else "Ver Correção 🎓",
                    fontWeight = FontWeight.Bold
                )
            }

            // Slide and Next item
            Button(
                onClick = {
                    if (currentQIndex < size - 1) {
                        viewModel.savedQuestionReviewIndex++
                    } else {
                        viewModel.savedQuestionReviewIndex = 0
                    }
                    verCorrecao = false
                    userGuess = null
                },
                enabled = size > 1,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Próxima ➡️", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// COMPASSIONATE DYNAMIC JSON IMPORT INTERRUPT DIALOG MAPPING
@Composable
fun ImportQuestionsDialog(viewModel: CfcViewModel, onDismiss: () -> Unit) {
    var rawText by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Importar Questões (JSON) 📂",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Text(
                    text = "Abaixo, cole a string JSON de questões do CFC (no formato exigido pelo banco) para adicionar dinamicamente novas provas:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Paste field text
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("Ex: [ {\"statement\": \"...\", \"optionA\": \"...\"} ]") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 15,
                    textStyle = MaterialTheme.typography.bodySmall
                )

                if (importStatus != null) {
                    Text(
                        text = importStatus!!,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (importStatus!!.startsWith("Sucesso")) CfcGreen else MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Populate example structure to make pasting simple
                            rawText = """[
  {
    "statement": "Enunciado da nova questão da prova para fins de auditoria direta fiscal...",
    "optionA": "Alternativa A comercial",
    "optionB": "Alternativa B",
    "optionC": "Alternativa C",
    "optionD": "Alternativa D",
    "correctAnswer": "A",
    "subject": "Auditoria",
    "difficulty": "Médio",
    "edition": "2026.1",
    "correctExplanation": "Explicação didática detalhada de aprovação.",
    "explanationA": "Detalhe de A.",
    "explanationB": "Detalhe de B.",
    "explanationC": "Detalhe de C.",
    "explanationD": "Detalhe de D.",
    "summary": "Resumo do tema da contabilidade."
  }
]"""
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ver Exemplo")
                    }

                    Button(
                        onClick = {
                            if (rawText.trim().isEmpty()) {
                                importStatus = "Por favor, cole um conteúdo JSON primeiro."
                                return@Button
                            }
                            isImporting = true
                            importStatus = "Processando arquivo..."
                            scope.launch {
                                val result = viewModel.importQuestionsJson(rawText)
                                isImporting = false
                                result.fold(
                                    onSuccess = { count ->
                                        importStatus = "Sucesso! $count novas questões importadas."
                                        rawText = ""
                                    },
                                    onFailure = { err ->
                                        importStatus = "Erro de importação: ${err.localizedMessage}"
                                    }
                                )
                            }
                        },
                        enabled = !isImporting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Importar")
                    }
                }
            }
        }
    }
}
