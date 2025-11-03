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
#include "uci.h"

using namespace Stockfish;

static Engine* g_engine = nullptr;
static std::string g_lastResult;
static std::string g_bestmove;
static std::string g_lastScoreString;
static bool g_initialized = false;

void stockfish_init(void) {
    @autoreleasepool {
        if (!g_initialized) {
            Bitboards::init();
            Position::init();
            g_initialized = true;
        }
        
        if (!g_engine) {
            g_engine = new Engine();
            
            try {
                g_engine->set_on_bestmove([](std::string_view best, std::string_view ponder) {
                    g_bestmove = std::string(best);
                });
            } catch (...) {
            }
            
            try {
                g_engine->set_on_update_full([](const Search::InfoFull& info) {
                    if (info.depth > 0) {
                        std::string raw = UCIEngine::format_score(info.score);
                        g_lastScoreString = raw;
                    }
                });
            } catch (...) {
            }
            
            try {
                g_engine->set_on_update_no_moves([](const Search::InfoShort& info) {});
            } catch (...) {}
            
            try {
                g_engine->set_on_iter([](const Search::InfoIteration& info) {});
            } catch (...) {}
        }
    }
}

const char* stockfish_evaluate(const char* fen, int depth) {
    @autoreleasepool {
        if (!g_engine) {
            g_lastResult = "error: no engine";
            return g_lastResult.c_str();
        }
        
        g_bestmove.clear();
        g_lastScoreString.clear();
        
        std::vector<std::string> moves;
        g_engine->set_position(fen, moves);
        
        Search::LimitsType limits;
        limits.depth = depth;
        
        g_engine->go(limits);
        g_engine->wait_for_search_finished();
        
        std::string result;
        
        if (g_lastScoreString.empty()) {
            if (g_bestmove.empty()) {
                result = "mate 0";
            } else {
                result = "0.00";
            }
        } else if (g_lastScoreString.rfind("mate", 0) == 0) {
            result = g_lastScoreString;
        } else if (g_lastScoreString.rfind("cp", 0) == 0) {
            try {
                int cp = std::stoi(g_lastScoreString.substr(3));
                std::ostringstream oss;
                oss << std::fixed << std::setprecision(2)
                    << (cp / 100.0);
                result = oss.str();
            } catch (...) {
                result = "0.00";
            }
        } else {
            result = g_lastScoreString;
        }
        
        g_lastResult = result;
        return g_lastResult.c_str();
    }
}

void stockfish_cleanup(void) {
    if (g_engine) {
        delete g_engine;
        g_engine = nullptr;
    }
}
