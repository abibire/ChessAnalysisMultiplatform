package com.andrewbibire.chessanalysis

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * Platform-specific chess piece image renderer.
 * Desktop uses painterResource (reliable on Windows), mobile uses Coil for SVG loading.
 */
@Composable
expect fun ChessPieceImage(
    piece: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
)
