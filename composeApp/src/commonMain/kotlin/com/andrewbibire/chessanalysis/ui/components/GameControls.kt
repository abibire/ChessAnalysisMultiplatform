package com.andrewbibire.chessanalysis.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andrewbibire.chessanalysis.Position
import com.andrewbibire.chessanalysis.ui.components.buttons.EvaluationButton
@Composable
fun GameControls(
    currentIndex: Int,
    onCurrentIndexChange: (Int) -> Unit,
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    isEvaluating: Boolean,
    positions: List<Position>,
    isExploringAlternativeLine: Boolean,
    alternativeLineReturnIndex: Int,
    alternativeLineReturnPositionCount: Int,
    onExitAlternativeLine: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        EvaluationButton(
            onClick = { onCurrentIndexChange(0) },
            enabled = currentIndex > 0 && !isEvaluating,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "First move",
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        EvaluationButton(
            onClick = {
                if (currentIndex > 0) {
                    if (isExploringAlternativeLine && currentIndex == alternativeLineReturnIndex) {
                        onExitAlternativeLine()
                    } else {
                        onCurrentIndexChange(currentIndex - 1)
                    }
                }
            },
            enabled = currentIndex > 0 && !isEvaluating,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.NavigateBefore,
                contentDescription = "Previous move",
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        EvaluationButton(
            onClick = { onPlayingChange(!isPlaying) },
            enabled = currentIndex < positions.lastIndex && !isEvaluating,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        EvaluationButton(
            onClick = { if (currentIndex < positions.lastIndex) onCurrentIndexChange(currentIndex + 1) },
            enabled = currentIndex < positions.lastIndex && !isEvaluating,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.NavigateNext,
                contentDescription = "Next move",
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        EvaluationButton(
            onClick = { onCurrentIndexChange(positions.lastIndex) },
            enabled = currentIndex < positions.lastIndex && !isEvaluating,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Last move",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
