package com.andrewbibire.chessanalysis

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import chessanalysis.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun ChessPieceImage(
    piece: String,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val resource = when (piece) {
        "K" -> Res.drawable.wK
        "Q" -> Res.drawable.wQ
        "R" -> Res.drawable.wR
        "B" -> Res.drawable.wB
        "N" -> Res.drawable.wN
        "P" -> Res.drawable.wP
        "k" -> Res.drawable.bK
        "q" -> Res.drawable.bQ
        "r" -> Res.drawable.bR
        "b" -> Res.drawable.bB
        "n" -> Res.drawable.bN
        "p" -> Res.drawable.bP
        else -> null
    }

    if (resource != null) {
        Image(
            painter = painterResource(resource),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
