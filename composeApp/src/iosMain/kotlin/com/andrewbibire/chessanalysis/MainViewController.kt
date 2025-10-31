package com.andrewbibire.chessanalysis

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    App(context = Unit) // Pass Unit (non-null) so StockfishEngine gets created
}