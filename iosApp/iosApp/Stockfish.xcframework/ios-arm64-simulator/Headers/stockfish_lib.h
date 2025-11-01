// stockfish_lib.h
#ifndef STOCKFISH_LIB_H
#define STOCKFISH_LIB_H

#ifdef __cplusplus
extern "C" {
#endif

void stockfish_init(const char* nnue_path_main, const char* nnue_path_small);
const char* stockfish_evaluate_fen(const char* fen, int depth);
void stockfish_cleanup(void);

#ifdef __cplusplus
}
#endif

#endif // STOCKFISH_LIB_H
