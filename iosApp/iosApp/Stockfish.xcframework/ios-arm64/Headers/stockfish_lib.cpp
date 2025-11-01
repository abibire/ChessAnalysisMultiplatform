// stockfish_lib.cpp
// C interface for Stockfish as a library

#include <string>
#include <sstream>
#include <iostream>
#include "uci.h"
#include "position.h"
#include "search.h"
#include "thread.h"
#include "tt.h"
#include "nnue/network.h"
#include "misc.h"

using namespace Stockfish;

extern "C" {

// Initialize Stockfish engine
void stockfish_init(const char* nnue_path_main, const char* nnue_path_small) {
    // Initialize UCI
    UCI::init(Options);
    Bitboards::init();
    Position::init();
    
    // Load NNUE networks
    if (nnue_path_main && strlen(nnue_path_main) > 0) {
        Eval::NNUE::Networks::Big.load(std::string(nnue_path_main));
    }
    
    if (nnue_path_small && strlen(nnue_path_small) > 0) {
        Eval::NNUE::Networks::Small.load(std::string(nnue_path_small));
    }
    
    // Initialize threads
    Threads.set(1);
    Search::clear();
}

// Evaluate a position from FEN
const char* stockfish_evaluate_fen(const char* fen, int depth) {
    static std::string result;
    
    try {
        // Parse FEN and set position
        Position pos;
        StateListPtr states(new std::deque<StateInfo>(1));
        pos.set(fen, false, &states->back());
        
        // Set up search limits
        Search::LimitsType limits;
        limits.depth = depth;
        
        // Start search
        Threads.start_thinking(pos, states, limits);
        Threads.main()->wait_for_search_finished();
        
        // Get the best score
        Value score = Threads.main()->rootMoves[0].score;
        
        // Format result
        std::ostringstream oss;
        if (abs(score) < VALUE_TB_WIN_IN_MAX_PLY) {
            oss << (double(score) / 100.0);
        } else {
            int mateIn = (VALUE_MATE - abs(score)) / 2;
            if (score > 0) {
                oss << "Mate in " << mateIn;
            } else {
                oss << "Mate in -" << mateIn;
            }
        }
        
        result = oss.str();
        return result.c_str();
        
    } catch (const std::exception& e) {
        result = "Error: ";
        result += e.what();
        return result.c_str();
    } catch (...) {
        result = "Unknown error";
        return result.c_str();
    }
}

// Cleanup
void stockfish_cleanup() {
    Threads.set(0);
    Search::clear();
}

} // extern "C"
