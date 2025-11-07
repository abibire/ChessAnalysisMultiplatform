@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.andrewbibire.chessanalysis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import chessanalysis.composeapp.generated.resources.Res
import chessanalysis.composeapp.generated.resources.material_symbols_sharp

@Composable
private fun rememberSymbolsSharp(
    fill: Float = 0f,
    wght: Float = 400f,
    grad: Float = 0f,
    opsz: Float = 24f
): FontFamily {
    val font = Font(
        resource = Res.font.material_symbols_sharp,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("FILL", fill),
            FontVariation.Setting("wght", wght),
            FontVariation.Setting("GRAD", grad),
            FontVariation.Setting("opsz", opsz)
        )
    )
    return remember(fill, wght, grad, opsz) { FontFamily(font) }
}

@Composable
fun MaterialSymbol(
    name: String,
    tint: Color,
    sizeSp: Float = 32f,
    fill: Float = 0f,
    flipHorizontally: Boolean = false
) {
    val symbolsSharp = rememberSymbolsSharp(fill = fill)
    Box(
        modifier = Modifier.graphicsLayer(
            scaleX = if (flipHorizontally) -1f else 1f
        )
    ) {
        BasicText(
            text = name,
            style = TextStyle(
                fontFamily = symbolsSharp,
                fontSize = sizeSp.sp,
                color = tint
            )
        )
    }
}
