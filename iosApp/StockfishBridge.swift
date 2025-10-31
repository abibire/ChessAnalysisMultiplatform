import Foundation
import ChessKitEngine

// Global engine instance
private var engine: Engine?
private var isEngineReady = false

@_cdecl("stockfish_init")
public func stockfish_init() {
    print("LOG: stockfish_init called")
    
    // Initialize engine in background
    Task {
        print("LOG: Creating Engine instance...")
        engine = Engine(type: .stockfish)
        
        // Get main NNUE file path from bundle
        guard let mainNNUE = Bundle.main.url(forResource: "nn-1111cefa1111", withExtension: "nnue") else {
            print("LOG: ❌ ERROR - Main NNUE file not found in bundle!")
            return
        }
        
        print("LOG: Found NNUE files:")
        print("LOG: Main: \(mainNNUE.path)")
        
        // Check for optional small file
        let smallNNUE = Bundle.main.url(forResource: "nn-37f18f62d772", withExtension: "nnue")
        if let small = smallNNUE {
            print("LOG: Small: \(small.path)")
        }
        
        print("LOG: Starting engine...")
        await engine?.start()
        print("LOG: Engine started, setting NNUE files...")
        
        await engine?.send(command: .setoption(id: "EvalFile", value: mainNNUE.path))
        if let small = smallNNUE {
            await engine?.send(command: .setoption(id: "EvalFileSmall", value: small.path))
        }
        
        isEngineReady = true
        print("LOG: ✅ Stockfish engine ready!")
    }
}

@_cdecl("stockfish_evaluate")
public func stockfish_evaluate(_ fen: UnsafePointer<CChar>, _ depth: Int32) -> UnsafePointer<CChar> {
    let fenString = String(cString: fen).trimmingCharacters(in: .whitespacesAndNewlines)
    
    // Check if engine is ready
    if !isEngineReady {
        let result = strdup("N/A (Not Ready)")!
        return UnsafePointer(result)
    }
    
    guard let engine = engine else {
        let result = strdup("N/A (No Engine)")!
        return UnsafePointer(result)
    }

    let stateWrapper = StateWrapper()
    let dispatchGroup = DispatchGroup()
    dispatchGroup.enter()
    
    let depthInt = Int(depth)

    Task {
        // Try to create position from FEN
        if let positionString = EngineCommand.PositionString(rawValue: fenString) {
            print("LOG: Using FEN: \(fenString)")
            await engine.send(command: .position(positionString))
        } else {
            print("LOG: ⚠️ Invalid FEN, using startpos: \(fenString)")
            await engine.send(command: .position(.startpos))
        }
        
        await engine.send(command: .go(depth: depthInt))
        
        if let responseStream = await engine.responseStream {
            for await response in responseStream {
                switch response {
                case .info(let info):
                    if let score = info.score {
                        if let cpValue = score.cp {
                            let scoreValue = Double(cpValue) / 100.0
                            // Adjust score based on side to move
                            let adjustedScore = fenString.contains(" b ") ? -scoreValue : scoreValue
                            stateWrapper.bestScore = String(format: "%.2f", adjustedScore)
                        } else if let moves = score.mate {
                            stateWrapper.bestScore = "Mate in \(moves)"
                        }
                    }
                case .bestmove(_, _):
                    print("LOG: Score: \(stateWrapper.bestScore)")
                    dispatchGroup.leave()
                    return
                default:
                    break
                }
            }
        }
        
        dispatchGroup.leave()
    }

    // Wait for result (reduced timeout)
    let waitResult = dispatchGroup.wait(timeout: .now() + 15.0)
    
    if waitResult == .timedOut {
        stateWrapper.bestScore = "N/A (Timeout)"
        print("LOG: ❌ Timeout")
    }

    let result = strdup(stateWrapper.bestScore)!
    return UnsafePointer(result)
}

@_cdecl("stockfish_cleanup")
public func stockfish_cleanup() {
    print("LOG: stockfish_cleanup called")
    isEngineReady = false
    Task {
        await engine?.send(command: .quit)
        await engine?.stop()
        engine = nil
    }
}

// Helper class
private final class StateWrapper: @unchecked Sendable {
    var bestScore: String = "N/A"
}
