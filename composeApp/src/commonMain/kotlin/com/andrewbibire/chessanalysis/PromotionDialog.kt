package com.andrewbibire.chessanalysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import chessanalysis.composeapp.generated.resources.Res
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest

@Composable
fun PromotionDialog(
    isWhite: Boolean,
    onPieceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFEEEED2)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Queen
                PromotionPieceSquare(
                    piece = if (isWhite) "Q" else "q",
                    onClick = { onPieceSelected(if (isWhite) "Q" else "q") }
                )

                // Rook
                PromotionPieceSquare(
                    piece = if (isWhite) "R" else "r",
                    onClick = { onPieceSelected(if (isWhite) "R" else "r") }
                )

                // Bishop
                PromotionPieceSquare(
                    piece = if (isWhite) "B" else "b",
                    onClick = { onPieceSelected(if (isWhite) "B" else "b") }
                )

                // Knight
                PromotionPieceSquare(
                    piece = if (isWhite) "N" else "n",
                    onClick = { onPieceSelected(if (isWhite) "N" else "n") }
                )
            }
        }
    }
}

@Composable
fun PromotionPieceSquare(
    piece: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFF769656), shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val pieceFileName = getPieceSvgFileName(piece)
        if (pieceFileName != null) {
            val context = LocalPlatformContext.current
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Res.getUri("drawable/$pieceFileName"))
                    .build(),
                contentDescription = piece,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
