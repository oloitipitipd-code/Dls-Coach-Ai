package com.dlscoachai.app

/**
 * Conservative interpretation of visible OCR text only.
 *
 * This is not game/account access. A screen label and player candidates are
 * hypotheses derived from text the user explicitly allowed Android to capture.
 * Candidates must be reviewed before they are added to the local squad.
 */
data class LiveSquadCandidate(
    val name: String,
    val rating: Int
)

data class LiveScreenAnalysis(
    val screenType: String,
    val confidence: String,
    val candidates: List<LiveSquadCandidate>
)

object LiveSquadIntelligence {
    /**
     * Accumulates candidates across a user-guided scan session.
     * A later observation updates a candidate's rating without duplicating its name.
     * The cap prevents unbounded preference growth from noisy OCR.
     */
    fun mergeCandidates(
        existing: List<LiveSquadCandidate>,
        observed: List<LiveSquadCandidate>,
        limit: Int = 100
    ): List<LiveSquadCandidate> {
        val merged = LinkedHashMap<String, LiveSquadCandidate>()
        existing.forEach { candidate ->
            if (candidate.name.isNotBlank() && candidate.rating in 1..100) {
                merged[candidate.name.trim().lowercase()] = candidate.copy(name = candidate.name.trim())
            }
        }
        observed.forEach { candidate ->
            if (candidate.name.isNotBlank() && candidate.rating in 1..100) {
                val key = candidate.name.trim().lowercase()
                if (key in merged || merged.size < limit) {
                    merged[key] = candidate.copy(name = candidate.name.trim())
                }
            }
        }
        return merged.values.take(limit)
    }

    private val ratingAtEnd = Regex("^(.{2,32}?)\\s+(100|[1-9][0-9]?)$")
    private val ignored = setOf(
        "overall rating", "rating", "speed", "acceleration", "stamina",
        "strength", "control", "passing", "shooting", "tackling",
        "goalkeeping", "height", "weight", "player", "players", "squad"
    )

    fun analyse(rawText: String): LiveScreenAnalysis {
        val normalised = rawText.lineSequence()
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }
            .toList()
        val upper = normalised.joinToString(" ").uppercase()

        val screenType = when {
            listOf("PLAYER PROFILE", "PLAYER DEVELOPMENT", "UPGRADE PLAYER").any { it in upper } -> "PLAYER_PROFILE"
            listOf("SQUAD", "MY PLAYERS", "TEAM MANAGEMENT", "STARTING XI").any { it in upper } -> "SQUAD"
            listOf("TRANSFERS", "SCOUT", "TRANSFER MARKET").any { it in upper } -> "TRANSFERS"
            listOf("SETTINGS", "CAMERA", "GRAPHICS", "CONTROLS").any { it in upper } -> "SETTINGS"
            listOf("MATCH", "PAUSED", "HALF TIME", "FULL TIME").any { it in upper } -> "MATCH"
            else -> "UNKNOWN"
        }

        // Only parse player-like lines on a screen that has squad/player context.
        val candidates = if (screenType == "SQUAD" || screenType == "PLAYER_PROFILE") {
            normalised.mapNotNull { line ->
                val match = ratingAtEnd.matchEntire(line) ?: return@mapNotNull null
                val name = match.groupValues[1].trim().trim('-', ':', '|', '•')
                val rating = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                if (name.lowercase() in ignored || name.count { it.isLetter() } < 2) return@mapNotNull null
                if (name.any { it.isDigit() }) return@mapNotNull null
                LiveSquadCandidate(name, rating)
            }.distinctBy { it.name.lowercase() }.take(30)
        } else emptyList()

        val confidence = when {
            screenType == "UNKNOWN" -> "LOW"
            candidates.size >= 2 -> "MEDIUM"
            candidates.size == 1 -> "LOW"
            else -> "LOW"
        }
        return LiveScreenAnalysis(screenType, confidence, candidates)
    }
}
