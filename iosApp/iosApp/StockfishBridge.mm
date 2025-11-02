#import "StockfishBridge.h"
#include <string>
#include <vector>
#include <sstream>
#include "engine.h"
#include "uci.h"
#include "search.h"
#include "bitboard.h"
#include "position.h"
#include "thread.h"

using namespace Stockfish;

static Engine* g_engine = nullptr;
static std::string g_lastResult;
static std::string g_bestmove;
static std::string g_evalString;
static bool g_initialized = false;

void stockfish_init(void) {
    if (!g_initialized) {
        // Initialize Stockfish's internal tables
        Bitboards::init();
        Position::init();
        
        g_initialized = true;
    }
    
    if (!g_engine) {
        g_engine = new Engine();
        
        // Set up bestmove callback
        g_engine->set_on_bestmove([](std::string_view best, std::string_view ponder) {
            g_bestmove = std::string(best);
        });
        
        // Set up evaluation callback
        g_engine->set_on_update_full([](const Search::InfoFull& info) {
            if (info.depth > 0) {
                g_evalString = UCIEngine::format_score(info.score);
            }
        });
    }
}

const char* stockfish_evaluate(const char* fen, int depth) {
    if (!g_engine) {
        g_lastResult = "error: engine not initialized";
        return g_lastResult.c_str();
    }
    
    // Clear previous results
    g_bestmove.clear();
    g_evalString.clear();
    
    // Set position from FEN
    std::vector<std::string> moves;
    g_engine->set_position(fen, moves);
    
    // Configure search limits
    Search::LimitsType limits;
    limits.depth = depth;
    
    // Start search
    g_engine->go(limits);
    
    // Wait for completion
    g_engine->wait_for_search_finished();
    
    // Build result string
    std::ostringstream oss;
    oss << "bestmove " << (g_bestmove.empty() ? "none" : g_bestmove);
    if (!g_evalString.empty()) {
        oss << " eval " << g_evalString;
    } else {
        oss << " eval cp 0";
    }
    
    g_lastResult = oss.str();
    return g_lastResult.c_str();
}

void stockfish_cleanup(void) {
    if (g_engine) {
        delete g_engine;
        g_engine = nullptr;
    }
}
