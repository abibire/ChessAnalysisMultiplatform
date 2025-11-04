package com.andrewbibire.chessanalysis

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.abs

enum class EvalType { Centipawn, Mate }
data class Eval(val type: EvalType, val value: Double)
enum class MoveColour { WHITE, BLACK }

fun isWhiteToMove(fen: String): Boolean {
    val parts = fen.split(" ")
    return parts.getOrNull(1) == "w"
}

fun parseEvaluationWhiteCentric(raw: String?, fen: String): Eval? {
    if (raw == null) return null
    val whiteToMove = isWhiteToMove(fen)
    val lower = raw.trim().lowercase()
    return if (lower.startsWith("mate")) {
        val n = Regex("""[+-]?\d+""").find(lower)?.value?.toIntOrNull() ?: 0
        val v = if (n == 0) {
            if (whiteToMove) -999.0 else 999.0
        } else {
            if (whiteToMove) n.toDouble() else -n.toDouble()
        }
        Eval(EvalType.Mate, v)
    } else {
        val pawns = lower.toDoubleOrNull() ?: return null
        val cp = pawns * 100.0
        val v = if (whiteToMove) cp else -cp
        Eval(EvalType.Centipawn, v)
    }
}

private fun flipColour(c: MoveColour) = if (c == MoveColour.WHITE) MoveColour.BLACK else MoveColour.WHITE

fun getExpectedPoints(evaluation: Eval, moveColour: MoveColour, centipawnGradient: Double = 0.0035): Double {
    return if (evaluation.type == EvalType.Mate) {
        if (evaluation.value == 0.0) {
            if (moveColour == MoveColour.WHITE) 1.0 else 0.0
        } else {
            if (evaluation.value > 0) 1.0 else 0.0
        }
    } else {
        1.0 / (1.0 + exp(-centipawnGradient * evaluation.value))
    }
}

fun getExpectedPointsLoss(previous: Eval, current: Eval, moveColour: MoveColour): Double {
    val prevEp = getExpectedPoints(previous, flipColour(moveColour))
    val curEp = getExpectedPoints(current, moveColour)
    return max(0.0, (prevEp - curEp) * if (moveColour == MoveColour.WHITE) 1.0 else -1.0)
}

fun classifyPointLoss(previous: Eval, current: Eval, moveColour: MoveColour): String {
    val previousSubjectiveValue = previous.value * if (moveColour == MoveColour.WHITE) 1.0 else -1.0
    val subjectiveValue = current.value * if (moveColour == MoveColour.WHITE) 1.0 else -1.0

    if (
        previous.type == EvalType.Mate &&
        previousSubjectiveValue > 0 &&
        (
                (current.type == EvalType.Mate && abs(current.value) > 900) ||
                        (current.type == EvalType.Centipawn && current.value == 0.0)
                )
    ) {
        return "Best"
    }

    if (previous.type == EvalType.Mate && current.type == EvalType.Mate) {
        if (previousSubjectiveValue > 0 && subjectiveValue < 0) {
            return if (subjectiveValue < -3) "Mistake" else "Blunder"
        }
        val mateLoss = (current.value - previous.value) * if (moveColour == MoveColour.WHITE) 1.0 else -1.0
        return if (mateLoss < 0 || (mateLoss == 0.0 && subjectiveValue < 0)) {
            "Best"
        } else if (mateLoss < 2) {
            "Excellent"
        } else if (mateLoss < 7) {
            "Okay"
        } else {
            "Inaccuracy"
        }
    }

    if (previous.type == EvalType.Mate && current.type == EvalType.Centipawn) {
        return if (subjectiveValue >= 800) {
            "Excellent"
        } else if (subjectiveValue >= 400) {
            "Okay"
        } else if (subjectiveValue >= 200) {
            "Inaccuracy"
        } else if (subjectiveValue >= 0) {
            "Mistake"
        } else {
            "Blunder"
        }
    }

    if (previous.type == EvalType.Centipawn && current.type == EvalType.Mate) {
        return if (subjectiveValue > 0) {
            "Best"
        } else if (subjectiveValue >= -2) {
            "Blunder"
        } else if (subjectiveValue >= -5) {
            "Mistake"
        } else {
            "Inaccuracy"
        }
    }

    val pointLoss = getExpectedPointsLoss(previous, current, moveColour)
    return if (pointLoss < 0.01) {
        "Best"
    } else if (pointLoss < 0.045) {
        "Excellent"
    } else if (pointLoss < 0.08) {
        "Okay"
    } else if (pointLoss < 0.12) {
        "Inaccuracy"
    } else if (pointLoss < 0.22) {
        "Mistake"
    } else {
        "Blunder"
    }
}
