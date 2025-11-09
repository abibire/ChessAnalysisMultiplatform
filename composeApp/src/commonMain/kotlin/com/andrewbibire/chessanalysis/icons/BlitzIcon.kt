package com.andrewbibire.chessanalysis.icons

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val BlitzIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Blitz",
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
            // Simple lightning bolt shape
            moveTo(12f, 2f)
            lineTo(4f, 12f)
            lineTo(10f, 12f)
            lineTo(8f, 22f)
            lineTo(20f, 10f)
            lineTo(14f, 10f)
            lineTo(12f, 2f)
            close()
        }
    }.build()
