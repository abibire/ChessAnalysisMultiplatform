package com.andrewbibire.chessanalysis

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

actual fun createStockfishEngine(context: Any?): StockfishEngine {
    return StockfishEngine(null)
}

actual suspend fun readClipboard(): String? {
    return try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val contents = clipboard.getContents(null)

        if (contents?.isDataFlavorSupported(DataFlavor.stringFlavor) == true) {
            contents.getTransferData(DataFlavor.stringFlavor) as? String
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
