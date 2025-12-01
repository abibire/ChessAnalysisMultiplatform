package com.andrewbibire.chessanalysis.online

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AllInclusive
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
import androidx.compose.ui.unit.dp
import com.andrewbibire.chessanalysis.BoardDark
import com.andrewbibire.chessanalysis.icons.BulletIcon
import com.andrewbibire.chessanalysis.icons.BlitzIcon
import com.andrewbibire.chessanalysis.icons.RapidIcon
import com.andrewbibire.chessanalysis.icons.ClassicIcon

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
        } else if (userProfile.platform == Platform.LICHESS && !isInitialized) {
            // For Lichess, fetch current month's games initially (no 'until' parameter)
            val result = LichessService.getCurrentMonthGames(userProfile.username)
            when (result) {
                is com.andrewbibire.chessanalysis.network.NetworkResult.Success -> {
                    // Cache the current month's games
                    val cacheKey = "$currentYear-$currentMonth"
                    gamesCache[cacheKey] = result.data
                    monthGames = result.data
                    errorMessage = null
                    isInitialized = true
                    isLoading = false
                }
                is com.andrewbibire.chessanalysis.network.NetworkResult.Error -> {
                    errorMessage = result.message ?: "Failed to load games"
                    snackbarHostState.showSnackbar(
                        message = errorMessage!!,
                        duration = SnackbarDuration.Short
                    )
                    isInitialized = true
                    isLoading = false
                }
            }
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
                Platform.LICHESS -> LichessService.getGamesForYearMonth(
                    userProfile.username,
                    currentYear,
                    currentMonth
                )
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
                            text = userProfile.username,
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
                    if (games.isNotEmpty()) {
                    }
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
                                                    isSelected -> BoardDark
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
                                                isSelected -> Color.White
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
                onClick = { onDateSelected(selectedYear, selectedMonth) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = BoardDark
                )
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = BoardDark
                )
            ) {
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
    val opponentName = if (isUserWhite) game.black else game.white
    val userRating = if (isUserWhite) game.whiteRating else game.blackRating
    val opponentRating = if (isUserWhite) game.blackRating else game.whiteRating

    val resultColor = when {
        game.result == "1/2-1/2" -> Color(0xFF939391)
        (game.result == "1-0" && isUserWhite) || (game.result == "0-1" && !isUserWhite) -> Color(0xFF66BB6A)
        else -> Color(0xFFEF5350)
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
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (!isUserWhite) Color.White else Color(0xFF2C2C2C))
                    )
                    Text(
                        text = opponentName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    opponentRating?.let {
                        Text(
                            text = "($it)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val timeCategory = getTimeControlCategory(game.timeControl)
                    when (timeCategory) {
                        TimeControlCategory.BULLET -> {
                            Icon(
                                imageVector = BulletIcon,
                                contentDescription = timeCategory.name,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Unspecified
                            )
                        }
                        TimeControlCategory.BLITZ -> {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = timeCategory.name,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFFAD642)
                            )
                        }
                        TimeControlCategory.RAPID -> {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = timeCategory.name,
                                modifier = Modifier.size(20.dp),
                                tint = BoardDark
                            )
                        }
                        TimeControlCategory.CLASSIC -> {
                            Icon(
                                imageVector = Icons.Filled.AllInclusive,
                                contentDescription = timeCategory.name,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = formatTimeControl(game.timeControl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(21.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(resultColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = " ",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
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

enum class TimeControlCategory {
    BULLET,
    BLITZ,
    RAPID,
    CLASSIC
}

private fun getTimeControlCategory(timeControl: String): TimeControlCategory {
    val totalSeconds = when {
        timeControl.contains("+") -> {
            val parts = timeControl.split("+")
            parts[0].toIntOrNull() ?: 0
        }
        else -> {
            timeControl.toIntOrNull() ?: Int.MAX_VALUE // Default to CLASSIC if no time info
        }
    }

    val minutes = totalSeconds / 60

    return when {
        minutes <= 2 -> TimeControlCategory.BULLET
        minutes < 10 -> TimeControlCategory.BLITZ
        minutes in 10..60 -> TimeControlCategory.RAPID
        else -> TimeControlCategory.CLASSIC
    }
}
