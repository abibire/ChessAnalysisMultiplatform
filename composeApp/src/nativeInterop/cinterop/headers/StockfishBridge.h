#ifndef StockfishBridge_h
#define StockfishBridge_h

#include <stdint.h>

void stockfish_init(void);
const char* stockfish_evaluate(const char* fen, int depth);
void stockfish_cleanup(void);

#endif