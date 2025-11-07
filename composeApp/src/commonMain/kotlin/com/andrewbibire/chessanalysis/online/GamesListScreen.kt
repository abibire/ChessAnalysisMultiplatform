package com.andrewbibire.chessanalysis.online

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrewbibire.chessanalysis.BoardDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesListScreen(
    userProfile: UserProfile,
    onGameSelected: (OnlineGame) -> Unit,
    onBackPressed: () -> Unit
) {
    var currentYear by remember { mutableIntStateOf(getCurrentYear()) }
    var currentMonth by remember { mutableIntStateOf(getCurrentMonth()) }
    var isLoading by remember { mutableStateOf(true) }
    var monthGames by remember { mutableStateOf<List<OnlineGame>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var availableArchives by remember { mutableStateOf<List<String>>(emptyList()) }
    var isInitialized by remember { mutableStateOf(false) }
    val gamesCache = remember { mutableMapOf<String, List<OnlineGame>>() }
    var showDatePicker by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (userProfile.platform == Platform.CHESS_COM && !isInitialized) {
            val archivesResult = ChessComService.getAvailableArchives(userProfile.username)
            when (archivesResult) {
                is com.andrewbibire.chessanalysis.network.NetworkResult.Success -> {
                    availableArchives = archivesResult.data
                    if (availableArchives.isNotEmpty()) {
                        val lastArchive = availableArchives.last()
                        val parts = lastArchive.split("/")
                        if (parts.size >= 2) {
                            currentYear = parts[parts.size - 2].toIntOrNull() ?: getCurrentYear()
                            currentMonth = parts[parts.size - 1].toIntOrNull() ?: getCurrentMonth()
                        }
                    }
                    isInitialized = true
                }
                is com.andrewbibire.chessanalysis.network.NetworkResult.Error -> {
                    errorMessage = archivesResult.message ?: "Failed to load archives"
                    snackbarHostState.showSnackbar(
                        message = errorMessage!!,
                        duration = SnackbarDuration.Short
                    )
                    isInitialized = true
                    isLoading = false
                }
            }
        } else if (userProfile.platform == Platform.LICHESS) {
            isInitialized = true
        }
    }

    LaunchedEffect(currentYear, currentMonth, isInitialized) {
        if (!isInitialized) return@LaunchedEffect

        val cacheKey = "$currentYear-$currentMonth"
        val cachedGames = gamesCache[cacheKey]

        if (cachedGames != null) {
            // Use cached data
            monthGames = cachedGames
            errorMessage = null
        } else {
            // Fetch new data
            isLoading = true
            errorMessage = null

            val result = when (userProfile.platform) {
                Platform.CHESS_COM -> ChessComService.getGamesForYearMonth(
                    userProfile.username,
                    currentYear,
                    currentMonth
                )
                Platform.LICHESS -> {
                    val dummyGames = DummyData.fetchGamesForMonth(
                        userProfile.username,
                        userProfile.platform,
                        currentYear,
                        currentMonth
                    )
                    com.andrewbibire.chessanalysis.network.NetworkResult.Success(dummyGames.games)
                }
            }

            isLoading = false
            when (result) {
                is com.andrewbibire.chessanalysis.network.NetworkResult.Success -> {
                    monthGames = result.data
                    gamesCache[cacheKey] = result.data
                    errorMessage = null
                }
                is com.andrewbibire.chessanalysis.network.NetworkResult.Error -> {
                    monthGames = emptyList()
                    errorMessage = result.message ?: "Failed to load games"
                    snackbarHostState.showSnackbar(
                        message = errorMessage!!,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    val monthName = remember(currentMonth) {
        getMonthName(currentMonth)
    }

    val canGoForward = remember(currentYear, currentMonth) {
        val currentYearNow = getCurrentYear()
        val currentMonthNow = getCurrentMonth()
        currentYear < currentYearNow || (currentYear == currentYearNow && currentMonth < currentMonthNow)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${userProfile.username}'s Games",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (userProfile.platform) {
                                Platform.CHESS_COM -> "Chess.com"
                                Platform.LICHESS -> "Lichess"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentMonth == 1) {
                            currentMonth = 12
                            currentYear -= 1
                        } else {
                            currentMonth -= 1
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.NavigateBefore,
                        contentDescription = "Previous Month",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "$monthName $currentYear",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showDatePicker = true }
                        .padding(8.dp)
                )

                IconButton(
                    onClick = {
                        if (currentMonth == 12) {
                            currentMonth = 1
                            currentYear += 1
                        } else {
                            currentMonth += 1
                        }
                    },
                    enabled = canGoForward
                ) {
                    Icon(
                        imageVector = Icons.Filled.NavigateNext,
                        contentDescription = "Next Month",
                        modifier = Modifier.size(32.dp),
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading || !isInitialized) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BoardDark)
                    }
                } else {
                    val games = monthGames ?: emptyList()
                    if (games.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (errorMessage != null) errorMessage!! else "No games found for this month",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(games) { game ->
                                GameCard(
                                    game = game,
                                    userProfile = userProfile,
                                    onClick = { onGameSelected(game) }
                                )
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        MonthYearPickerDialog(
            currentYear = currentYear,
            currentMonth = currentMonth,
            availableArchives = availableArchives,
            onDismiss = { showDatePicker = false },
            onDateSelected = { year, month ->
                currentYear = year
                currentMonth = month
                showDatePicker = false
            }
        )
    }
}

@Composable
fun MonthYearPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    availableArchives: List<String>,
    onDismiss: () -> Unit,
    onDateSelected: (year: Int, month: Int) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }

    val yearRange = if (availableArchives.isNotEmpty()) {
        val years = availableArchives.mapNotNull { archive ->
            val parts = archive.split("/")
            if (parts.size >= 2) parts[parts.size - 2].toIntOrNull() else null
        }
        (years.minOrNull() ?: 2010)..(years.maxOrNull() ?: getCurrentYear())
    } else {
        2010..getCurrentYear()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Month and Year",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year selection
                Column {
                    Text(
                        text = "Year",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (selectedYear > yearRange.first) selectedYear-- },
                            enabled = selectedYear > yearRange.first
                        ) {
                            Icon(Icons.Filled.NavigateBefore, "Previous year")
                        }
                        Text(
                            text = selectedYear.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { if (selectedYear < yearRange.last) selectedYear++ },
                            enabled = selectedYear < yearRange.last
                        ) {
                            Icon(Icons.Filled.NavigateNext, "Next year")
                        }
                    }
                }

                // Month selection grid
                Column {
                    Text(
                        text = "Month",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val months = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0..2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0..3) {
                                    val monthIndex = row * 4 + col + 1
                                    val isSelected = monthIndex == selectedMonth
                                    val isDisabled = selectedYear == getCurrentYear() && monthIndex > getCurrentMonth()

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isDisabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .clickable(enabled = !isDisabled) {
                                                selectedMonth = monthIndex
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = months[monthIndex - 1],
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDateSelected(selectedYear, selectedMonth) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GameCard(
    game: OnlineGame,
    userProfile: UserProfile,
    onClick: () -> Unit
) {
    val isUserWhite = game.white.equals(userProfile.username, ignoreCase = true)
    val userColor = if (isUserWhite) "White" else "Black"
    val opponentName = if (isUserWhite) game.black else game.white
    val userRating = if (isUserWhite) game.whiteRating else game.blackRating
    val opponentRating = if (isUserWhite) game.blackRating else game.whiteRating

    val resultColor = when {
        game.result == "1/2-1/2" -> Color(0xFFFFB74D)
        (game.result == "1-0" && isUserWhite) || (game.result == "0-1" && !isUserWhite) -> Color(0xFF66BB6A)
        else -> Color(0xFFEF5350)
    }

    val resultText = when {
        game.result == "1/2-1/2" -> "Draw"
        (game.result == "1-0" && isUserWhite) || (game.result == "0-1" && !isUserWhite) -> "Won"
        else -> "Lost"
    }

    val dateText = remember(game.endTime) {
        formatGameDate(game.endTime)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isUserWhite) Color.White else Color(0xFF2C2C2C))
                    )
                    Text(
                        text = userProfile.username,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    userRating?.let {
                        Text(
                            text = "($it)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "vs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = opponentName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    opponentRating?.let {
                        Text(
                            text = "($it)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimeControl(game.timeControl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(resultColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private fun formatTimeControl(timeControl: String): String {
    return when {
        timeControl.contains("+") -> {
            val parts = timeControl.split("+")
            val minutes = parts[0].toIntOrNull()?.div(60) ?: 0
            val increment = parts[1]
            "$minutes+$increment"
        }
        else -> {
            val seconds = timeControl.toIntOrNull() ?: 0
            "${seconds / 60} min"
        }
    }
}
