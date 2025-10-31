import Foundation
import ChessKitEngine

// Global engine instance
private var engine: Engine?

@_cdecl("stockfish_init")
public func stockfish_init() {
    engine = Engine(type: .stockfish)
    // Start the engine
    Task {
        await engine?.start()
    }
}

@_cdecl("stockfish_evaluate")
public func stockfish_evaluate(_ fen: UnsafePointer<CChar>, _ depth: Int32) -> UnsafePointer<CChar> {
    guard let engine = engine else {
        let result = strdup("Engine not initialized")!
        return UnsafePointer(result)
    }
    
    let fenString = String(cString: fen)
    var bestScore = "N/A"
    let semaphore = DispatchSemaphore(value: 0)
    
    Task {
        do {
            // Set up position
            let startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
            let positionString = EngineCommand.PositionString(rawValue: fenString) ?? EngineCommand.PositionString(rawValue: startFen)!
            await engine.send(command: .position(positionString))
            
            // Start a task to observe engine output using responseStream
            Task {
                if let responseStream = await engine.responseStream {
                    for await response in responseStream {
                        switch response {
                        case .info(let info):
                            // Extract score from info
                            if let score = info.score {
                                switch score {
                                case .centipawns(let cp):
                                    let scoreValue = Double(cp) / 100.0
                                    // Adjust for side to move
                                    let adjustedScore = fenString.contains(" b ") ? -scoreValue : scoreValue
                                    bestScore = String(format: "%.2f", adjustedScore)
                                case .mate(let moves):
                                    bestScore = "Mate in \(moves)"
                                }
                            }
                        case .bestMove(_, _):
                            // Analysis complete
                            semaphore.signal()
                            return
                        default:
                            break
                        }
                    }
                }
            }
            
            // Start analysis
            await engine.send(command: .go(depth: Int(depth)))
            
        } catch {
            bestScore = "Error: \(error.localizedDescription)"
            semaphore.signal()
        }
    }
    
    // Wait for completion (with timeout)
    _ = semaphore.wait(timeout: .now() + 30.0)
    
    let result = strdup(bestScore)!
    return UnsafePointer(result)
}

@_cdecl("stockfish_cleanup")
public func stockfish_cleanup() {
    Task {
        await engine?.send(command: .quit)
        await engine?.stop()
    }
    engine = nil
}
