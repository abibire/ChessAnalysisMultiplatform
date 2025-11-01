#import "StockfishBridge.h"
#include <string>

static std::string g_lastResult;

void stockfish_init(void) {
}

const char* stockfish_evaluate(const char* fen, int depth) {
    g_lastResult = "bestmove 0000 eval 0";
    return g_lastResult.c_str();
}

void stockfish_cleanup(void) {
}
