package com.andrewbibire.chessanalysis

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import chessanalysis.composeapp.generated.resources.Res
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest

@Composable
actual fun ChessPieceImage(
    piece: String,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val fileName = when (piece) {
        "K" -> "wK.svg"
        "Q" -> "wQ.svg"
        "R" -> "wR.svg"
        "B" -> "wB.svg"
        "N" -> "wN.svg"
        "P" -> "wP.svg"
        "k" -> "bK.svg"
        "q" -> "bQ.svg"
        "r" -> "bR.svg"
        "b" -> "bB.svg"
        "n" -> "bN.svg"
        "p" -> "bP.svg"
        else -> null
    }

    if (fileName != null) {
        val context = LocalPlatformContext.current
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(Res.getUri("drawable/$fileName"))
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
