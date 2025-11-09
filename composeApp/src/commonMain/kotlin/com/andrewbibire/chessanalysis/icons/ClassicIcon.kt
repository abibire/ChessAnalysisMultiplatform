package com.andrewbibire.chessanalysis.icons

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ClassicIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Classic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            // Chess king crown shape
            moveTo(5f, 20f)
            lineTo(19f, 20f)
            lineTo(19f, 22f)
            lineTo(5f, 22f)
            close()
            moveTo(8f, 8f)
            lineTo(8f, 18f)
            lineTo(16f, 18f)
            lineTo(16f, 8f)
            lineTo(14f, 10f)
            lineTo(12f, 6f)
            lineTo(10f, 10f)
            close()
        }
    }.build()
