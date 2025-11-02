#import "StockfishBridge.h"
#import <Foundation/Foundation.h>
#include <string>
#include <vector>
#include <sstream>
#include "engine.h"
#include "bitboard.h"
#include "position.h"
#include "search.h"

using namespace Stockfish;

static Engine* g_engine = nullptr;
static std::string g_lastResult;
static std::string g_bestmove;
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
                });
            } catch (...) {}
            
            try {
                g_engine->set_on_update_full([](const Search::InfoFull& info) {});
            } catch (...) {}
            
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
        
        std::vector<std::string> moves;
        g_engine->set_position(fen, moves);
        
        Search::LimitsType limits;
        limits.depth = depth;
        
        g_engine->go(limits);
        g_engine->wait_for_search_finished();
        
        std::ostringstream oss;
        oss << "bestmove " << (g_bestmove.empty() ? "0000" : g_bestmove) << " eval cp 0";
        g_lastResult = oss.str();
        
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
