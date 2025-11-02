#import "StockfishBridge.h"
#import <Foundation/Foundation.h>
#include <string>
#include <vector>
#include <sstream>
#include <iomanip>
#include "engine.h"
#include "bitboard.h"
#include "position.h"
#include "search.h"
#include "types.h"

using namespace Stockfish;

static Engine* g_engine = nullptr;
static std::string g_lastResult;
static std::string g_bestmove;
static std::string g_lastScoreString;  // Store score as string
static bool g_initialized = false;

void stockfish_init(void) {
    @autoreleasepool {
        NSLog(@"[Bridge] Init called");
        
        if (!g_initialized) {
            Bitboards::init();
            Position::init();
            g_initialized = true;
            NSLog(@"[Bridge] Tables initialized");
        }
        
        if (!g_engine) {
            g_engine = new Engine();
            
            try {
                g_engine->set_on_bestmove([](std::string_view best, std::string_view ponder) {
                    g_bestmove = std::string(best);
                    NSLog(@"[Bridge] Best move received: %s", g_bestmove.c_str());
                });
            } catch (...) {
                NSLog(@"[Bridge] Error setting bestmove callback");
            }
            
            try {
                // Capture the evaluation score from search updates
                g_engine->set_on_update_full([](const Search::InfoFull& info) {
                    if (info.depth > 0) {
                        // Score in InfoFull might be a VALUE type, try treating as integer
                        // In Stockfish, internal values are typically already in centipawn-like scale
                        int scoreValue = *reinterpret_cast<const int*>(&info.score);
                        std::ostringstream oss;
                        oss << scoreValue;
                        g_lastScoreString = oss.str();
                        NSLog(@"[Bridge] Score update: depth=%d score=%s", info.depth, g_lastScoreString.c_str());
                    }
                });
            } catch (...) {
                NSLog(@"[Bridge] Error setting update_full callback");
            }
            
            try {
                g_engine->set_on_update_no_moves([](const Search::InfoShort& info) {});
            } catch (...) {}
            
            try {
                g_engine->set_on_iter([](const Search::InfoIteration& info) {});
            } catch (...) {}
            
            NSLog(@"[Bridge] Engine created");
        }
    }
}

const char* stockfish_evaluate(const char* fen, int depth) {
    @autoreleasepool {
        NSLog(@"[Bridge] Evaluate: %s depth=%d", fen, depth);
        
        if (!g_engine) {
            g_lastResult = "error: no engine";
            return g_lastResult.c_str();
        }
        
        g_bestmove.clear();
        g_lastScoreString.clear();  // Reset score before search
        
        std::vector<std::string> moves;
        g_engine->set_position(fen, moves);
        
        Search::LimitsType limits;
        limits.depth = depth;
        
        g_engine->go(limits);
        g_engine->wait_for_search_finished();
        
        // Parse the score string and convert to decimal
        // Score string might be like "36" (centipawns) or "mate 5"
        std::string result;
        if (g_lastScoreString.empty()) {
            result = "0.00";
        } else if (g_lastScoreString.find("mate") != std::string::npos) {
            result = g_lastScoreString;  // Return mate as is
        } else {
            try {
                int cp = std::stoi(g_lastScoreString);
                std::ostringstream oss;
                oss.precision(2);
                oss << std::fixed << (cp / 100.0);
                result = oss.str();
            } catch (...) {
                result = "0.00";
            }
        }
        
        g_lastResult = result;
        
        NSLog(@"[Bridge] Result: %s", g_lastResult.c_str());
        return g_lastResult.c_str();
    }
}

void stockfish_cleanup(void) {
    if (g_engine) {
        delete g_engine;
        g_engine = nullptr;
    }
}
