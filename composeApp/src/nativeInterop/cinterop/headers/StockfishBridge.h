#ifndef StockfishBridge_h
#define StockfishBridge_h

#include <stdint.h>

void stockfish_init(void);
const char* stockfish_evaluate(const char* fen, int depth);
const char* stockfish_evaluate_multipv(const char* fen, int depth, int numLines);
void stockfish_cleanup(void);

#endif