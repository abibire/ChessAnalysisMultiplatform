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

static std::string g_multiPVResult;

struct PVLineData {
    size_t multiPV;
    int depth;
    Score score;
    std::string pv;  // Store as string, not string_view
};

static std::vector<PVLineData> g_pvLines;

const char* stockfish_evaluate(const char* fen, int depth) {
    @autoreleasepool {
        if (!g_engine) {
            g_lastResult = "error: no engine|";
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
        std::string scoreResult;
        if (g_lastScoreString.empty()) {
            if (g_bestmove.empty()) {
                scoreResult = "mate 0";
            } else {
                scoreResult = "0.00";
            }
        } else if (g_lastScoreString.rfind("mate", 0) == 0) {
            scoreResult = g_lastScoreString;
        } else if (g_lastScoreString.rfind("cp", 0) == 0) {
            try {
                int cp = std::stoi(g_lastScoreString.substr(3));
                std::ostringstream oss;
                oss << std::fixed << std::setprecision(2)
                    << (cp / 100.0);
                scoreResult = oss.str();
            } catch (...) {
                scoreResult = "0.00";
            }
        } else {
            scoreResult = g_lastScoreString;
        }
        if (scoreResult.rfind("mate", 0) == 0) {
            size_t space = scoreResult.find(' ');
            if (space != std::string::npos && space + 1 < scoreResult.size() && scoreResult[space + 1] == '+') {
                scoreResult.erase(space + 1, 1);
            }
        }
        g_lastResult = scoreResult + "|" + g_bestmove;
        return g_lastResult.c_str();
    }
}

const char* stockfish_evaluate_multipv(const char* fen, int depth, int numLines) {
    @autoreleasepool {
        if (!g_engine) {
            g_multiPVResult = "error: no engine||";
            return g_multiPVResult.c_str();
        }

        g_bestmove.clear();
        g_lastScoreString.clear();
        g_pvLines.clear();

        // Set up callback to capture all PV lines
        g_engine->set_on_update_full([](const Search::InfoFull& info) {
            if (info.depth > 0 && !info.pv.empty()) {
                // Update or add the PV line - always keep the latest update for each multiPV
                // Copy string_view to string to avoid dangling references
                PVLineData lineData;
                lineData.multiPV = info.multiPV;
                lineData.depth = info.depth;
                lineData.score = info.score;
                lineData.pv = std::string(info.pv);  // Copy to string

                bool found = false;
                for (auto& line : g_pvLines) {
                    if (line.multiPV == info.multiPV) {
                        // Always update with latest info
                        line = lineData;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    g_pvLines.push_back(lineData);
                }

                std::string raw = UCIEngine::format_score(info.score);
                if (info.multiPV == 1) {
                    g_lastScoreString = raw;
                }
            }
        });

        // Set MultiPV option
        std::istringstream optionStream("name MultiPV value " + std::to_string(numLines));
        g_engine->get_options().setoption(optionStream);

        std::vector<std::string> moves;
        g_engine->set_position(fen, moves);
        Search::LimitsType limits;
        limits.depth = depth;
        g_engine->go(limits);
        g_engine->wait_for_search_finished();

        // Process main score
        std::string scoreResult;
        if (g_lastScoreString.empty()) {
            if (g_bestmove.empty()) {
                scoreResult = "mate 0";
            } else {
                scoreResult = "0.00";
            }
        } else if (g_lastScoreString.rfind("mate", 0) == 0) {
            scoreResult = g_lastScoreString;
        } else if (g_lastScoreString.rfind("cp", 0) == 0) {
            try {
                int cp = std::stoi(g_lastScoreString.substr(3));
                std::ostringstream oss;
                oss << std::fixed << std::setprecision(2) << (cp / 100.0);
                scoreResult = oss.str();
            } catch (...) {
                scoreResult = "0.00";
            }
        } else {
            scoreResult = g_lastScoreString;
        }
        if (scoreResult.rfind("mate", 0) == 0) {
            size_t space = scoreResult.find(' ');
            if (space != std::string::npos && space + 1 < scoreResult.size() && scoreResult[space + 1] == '+') {
                scoreResult.erase(space + 1, 1);
            }
        }

        // Build result with PV lines
        // Format: score|bestMove||line1Score:line1Move:pv1,pv2,pv3|line2Score:line2Move:pv1,pv2,pv3|...
        std::ostringstream result;
        result << scoreResult << "|" << g_bestmove << "||";

        // Sort by multiPV index
        std::sort(g_pvLines.begin(), g_pvLines.end(),
                  [](const PVLineData& a, const PVLineData& b) {
                      return a.multiPV < b.multiPV;
                  });

        for (size_t i = 0; i < g_pvLines.size(); i++) {
            if (i > 0) result << "|";

            const auto& lineData = g_pvLines[i];
            std::string lineScore = UCIEngine::format_score(lineData.score);

            // Format score
            if (lineScore.rfind("mate", 0) == 0) {
                size_t space = lineScore.find(' ');
                if (space != std::string::npos && space + 1 < lineScore.size() && lineScore[space + 1] == '+') {
                    lineScore.erase(space + 1, 1);
                }
            } else if (lineScore.rfind("cp", 0) == 0) {
                try {
                    int cp = std::stoi(lineScore.substr(3));
                    std::ostringstream oss;
                    oss << std::fixed << std::setprecision(2) << (cp / 100.0);
                    lineScore = oss.str();
                } catch (...) {
                    lineScore = "0.00";
                }
            }

            result << lineScore << ":";

            // PV string contains space-separated moves - already a string
            const std::string& pvStr = lineData.pv;

            if (pvStr.empty()) {
                // If no PV, just add empty move and empty pv
                result << ":";
            } else {
                // Extract first move
                size_t firstSpace = pvStr.find(' ');
                std::string firstMove = (firstSpace != std::string::npos) ? pvStr.substr(0, firstSpace) : pvStr;
                result << firstMove << ":";

                // All PV moves - replace spaces with commas
                for (size_t j = 0; j < pvStr.length(); j++) {
                    if (pvStr[j] == ' ') {
                        result << ',';
                    } else {
                        result << pvStr[j];
                    }
                }
            }
        }

        // Reset MultiPV to 1
        std::istringstream resetStream("name MultiPV value 1");
        g_engine->get_options().setoption(resetStream);

        g_multiPVResult = result.str();
        return g_multiPVResult.c_str();
    }
}

void stockfish_cleanup(void) {
    if (g_engine) {
        delete g_engine;
        g_engine = nullptr;
    }
}
