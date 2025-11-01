#pragma once

#ifdef __cplusplus
extern "C" {
#endif

void stockfish_init(void);
const char* stockfish_evaluate(const char* fen, int depth);
void stockfish_cleanup(void);

#ifdef __cplusplus
}
#endif
