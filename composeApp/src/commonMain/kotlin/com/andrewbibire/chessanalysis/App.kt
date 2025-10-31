package com.andrewbibire.chessanalysis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext

@Composable
fun App(context: Any? = null) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ChessAnalysisApp(context)
        }
    }
}

@Composable
fun ChessAnalysisApp(context: Any?) {
    val samplePgn = """
        [Event "Example Game"]
        [Site "Berlin"]
        [Date "1852.??.??"]
        [Round "?"]
        [White "Adolf Anderssen"]
        [Black "Jean Dufresne"]
        [Result "1-0"]
        
        1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. b4 Bxb4 5. c3 Ba5 6. d4 exd4 7. O-O d3 8. Qb3 Qf6
        9. e5 Qg6 10. Re1 Nge7 11. Ba3 b5 12. Qxb5 Rb8 13. Qa4 Bb6 14. Nbd2 Bb7 15. Ne4 Qf5
        16. Bxd3 Qh5 17. Nf6+ gxf6 18. exf6 Rg8 19. Rad1 Qxf3 20. Rxe7+ Nxe7 21. Qxd7+ Kf8
        22. Qxe7# 1-0
    """.trimIndent()

    val positions = remember { generateFensFromPgn(samplePgn) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isEvaluating by remember { mutableStateOf(false) }
    val stockfishEngine = remember(context) {
        if (context != null) createStockfishEngine(context) else null
    }

    LaunchedEffect(context) {
        if (stockfishEngine != null && positions.isNotEmpty()) {
            isEvaluating = true

            // Wait 2 seconds for the Swift engine to fully initialize
            kotlinx.coroutines.delay(2000)
            println("KOTLIN: Starting evaluations after delay")

            // Run evaluations on background thread to avoid blocking UI
            withContext(kotlinx.coroutines.Dispatchers.Default) {
                for (position in positions) {
                    println("KOTLIN: Evaluating position: ${position.fenString}")
                    position.score = stockfishEngine.evaluatePosition(position.fenString, depth = 5)
                    println("KOTLIN: Got score: ${position.score}")
                }
            }

            isEvaluating = false
            println("KOTLIN: All evaluations complete")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Move: $currentIndex", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Chessboard(fen = positions[currentIndex].fenString)
        Spacer(modifier = Modifier.height(16.dp))
        if (isEvaluating) {
            CircularProgressIndicator()
        } else {
            Text(
                text = "Score: ${positions[currentIndex].score ?: "N/A"}",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { if (currentIndex > 0) currentIndex-- }, enabled = currentIndex > 0) {
                Text("Back")
            }
            Button(onClick = { if (currentIndex < positions.size - 1) currentIndex++ }, enabled = currentIndex < positions.size - 1) {
                Text("Forward")
            }
        }
    }
}

expect fun createStockfishEngine(context: Any?): StockfishEngine