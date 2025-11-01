import Foundation
import ChessKitEngine

private var engine: Engine?
private var isEngineReady = false

@_cdecl("stockfish_init")
public func stockfish_init() {
    Task {
        engine = Engine(type: .stockfish)
        await engine?.start()
        await engine?.setLoggingEnabled(true)
        isEngineReady = true
    }
}

@_cdecl("stockfish_evaluate")
public func stockfish_evaluate(_ fen: UnsafePointer<CChar>, _ depth: Int32) -> UnsafePointer<CChar> {
    let fenString = String(cString: fen).trimmingCharacters(in: .whitespacesAndNewlines)
    guard isEngineReady, let engine = engine else {
        let res = strdup("N/A (Not Ready)")
        return UnsafePointer(res!)
    }

    final class Holder {
        var score: String = "N/A"
    }
    let holder = Holder()
    let semaphore = DispatchSemaphore(value: 0)
    let targetDepth = Int(depth)

    Task.detached(priority: .userInitiated) {
        await engine.send(command: .position(.fen(fenString)))
        await engine.send(command: .go(depth: targetDepth))

        if let responseStream = await engine.responseStream {
            do {
                for try await response in responseStream {
                    switch response {
                    case .info(let info):
                        if let cp = info.score?.cp {
                            let v = Double(cp) / 100.0
                            holder.score = String(format: "%.2f", v)
                        } else if let mate = info.score?.mate {
                            holder.score = "Mate in \(mate)"
                        }

                        if let d = info.depth, d >= targetDepth {
                            semaphore.signal()
                            return
                        }

                    default:
                        break
                    }
                }
            } catch {
                holder.score = "N/A (Stream error)"
                semaphore.signal()
                return
            }
        } else {
            holder.score = "N/A (No response)"
            semaphore.signal()
            return
        }
    }

    let waitResult = semaphore.wait(timeout: .now() + 30.0)
    if waitResult == .timedOut {
        holder.score = "N/A (Timeout)"
    }

    let cstr = strdup(holder.score)
    return UnsafePointer(cstr!)
}

@_cdecl("stockfish_cleanup")
public func stockfish_cleanup() {
    isEngineReady = false
    Task {
        await engine?.send(command: .quit)
        await engine?.stop()
        engine = nil
    }
}
