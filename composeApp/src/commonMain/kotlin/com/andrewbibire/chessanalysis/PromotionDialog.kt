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
                .width(200.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2b2b2b)  // DarkSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // First row: Queen and Rook
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    PromotionPieceSquare(
                        piece = if (isWhite) "Q" else "q",
                        onClick = { onPieceSelected(if (isWhite) "Q" else "q") }
                    )
                    PromotionPieceSquare(
                        piece = if (isWhite) "R" else "r",
                        onClick = { onPieceSelected(if (isWhite) "R" else "r") }
                    )
                }

                // Second row: Bishop and Knight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    PromotionPieceSquare(
                        piece = if (isWhite) "B" else "b",
                        onClick = { onPieceSelected(if (isWhite) "B" else "b") }
                    )
                    PromotionPieceSquare(
                        piece = if (isWhite) "N" else "n",
                        onClick = { onPieceSelected(if (isWhite) "N" else "n") }
                    )
                }
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
            .size(80.dp)
            .background(Color(0xFF3d3d3d), shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
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
