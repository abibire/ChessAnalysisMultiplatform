package com.andrewbibire.chessanalysis.icons

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val BulletIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Bullet",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 512f,
        viewportHeight = 512f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(495.212f, 16.785f)
            curveToRelative(-44.125f, -44.141f, -188.297f, 5.875f, -250.078f, 67.656f)
            curveTo(184.354f, 145.221f, 61.79f, 267.8f, 61.79f, 267.8f)
            lineToRelative(182.406f, 182.407f)
            curveToRelative(0f, 0f, 121.563f, -121.579f, 183.359f, -183.36f)
            curveTo(489.321f, 205.082f, 539.337f, 60.91f, 495.212f, 16.785f)
            close()
        }
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(0.009f, 329.597f)
            lineTo(182.399f, 512.004f)
            lineTo(217.712f, 476.691f)
            lineTo(35.306f, 294.285f)
            close()
        }
    }.build()
