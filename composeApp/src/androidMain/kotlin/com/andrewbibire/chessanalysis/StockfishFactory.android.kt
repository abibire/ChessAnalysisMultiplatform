package com.andrewbibire.chessanalysis

import android.content.Context

actual fun createStockfishEngine(context: Any): StockfishEngine {
    return StockfishEngine(context as Context)
}