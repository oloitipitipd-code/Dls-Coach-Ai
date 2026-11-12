package com.dlscoachai.app

/**
 * Opponent-aware formation selection.
 *
 * The engine ranks formations using the existing squad/role-fit assessment plus
 * transparent tactical heuristics. Opponent details are observations supplied
 * by the user; this version does not claim to detect formations from live video.
 * Scores are coaching estimates, not official DLS formulas or win probabilities.
 */
data class AdaptiveFormationRecommendation(
    val assessment: FormationAssessment,
    val tacticalFit: Int,
    val reason: String,
    val confidence: String
)

object AdaptiveTacticalManager {
    val supportedFormations = listOf("4-2-3-1", "4-3-3", "4-1-2-1-2", "3-5-2", "5-3-2", "4-4-2")
    val supportedOpponentFormations = supportedFormations + "4-2-4"
    val opponentStyles = listOf(
        "Unknown",
        "High press",
        "Fast counter-attack",
        "Wide play",
        "Central build-up",
        "Deep defensive block"
    )

    fun recommend(
        players: List<Player>,
        opponentFormation: String,
        opponentStyle: String = "Unknown"
    ): AdaptiveFormationRecommendation? {
        if (players.isEmpty()) return null
        val candidates = supportedFormations.map { formation ->
            val assessment = assessFormation(players, formation)
            val fit = tacticalFit(formation, opponentFormation, opponentStyle)
            AdaptiveFormationRecommendation(
                assessment = assessment,
                tacticalFit = fit,
                reason = explanation(formation, opponentFormation, opponentStyle),
                confidence = when {
                    assessment.confidence == "LOW" -> "LOW"
                    opponentFormation !in supportedOpponentFormations -> "LOW"
                    opponentStyle == "Unknown" -> "MEDIUM"
                    else -> assessment.confidence
                }
            )
        }
        return candidates.maxWithOrNull(
            compareBy<AdaptiveFormationRecommendation> {
                it.assessment.totalScore + it.tacticalFit * 4.0
            }.thenBy { it.assessment.formation }
        )
    }

    fun rankedOptions(
        players: List<Player>,
        opponentFormation: String,
        opponentStyle: String = "Unknown"
    ): List<AdaptiveFormationRecommendation> {
        if (players.isEmpty()) return emptyList()
        return supportedFormations.mapNotNull { formation ->
        val assessment = assessFormation(players, formation)
        val fit = tacticalFit(formation, opponentFormation, opponentStyle)
        AdaptiveFormationRecommendation(
            assessment,
            fit,
            explanation(formation, opponentFormation, opponentStyle),
            when {
                assessment.confidence == "LOW" || opponentFormation !in supportedOpponentFormations -> "LOW"
                opponentStyle == "Unknown" -> "MEDIUM"
                else -> assessment.confidence
            }
        )
    }.sortedWith(
        compareByDescending<AdaptiveFormationRecommendation> { it.assessment.totalScore + it.tacticalFit * 4.0 }
            .thenBy { it.assessment.formation }
    )
    }

    /** Public for transparent unit tests and an explainable candidate comparison. */
    fun tacticalFit(ourFormation: String, opponentFormation: String, opponentStyle: String): Int {
        val matchup = when (opponentFormation) {
            "4-3-3" -> mapOf("5-3-2" to 8, "4-2-3-1" to 5, "4-4-2" to 3, "3-5-2" to 2)
            "4-2-4" -> mapOf("5-3-2" to 9, "4-3-3" to 8, "4-2-3-1" to 6, "4-4-2" to 3)
            "5-3-2" -> mapOf("4-3-3" to 9, "4-2-3-1" to 8, "4-4-2" to 6, "3-5-2" to 4)
            "4-4-2" -> mapOf("4-3-3" to 8, "4-2-3-1" to 7, "4-1-2-1-2" to 5, "3-5-2" to 3)
            "3-5-2" -> mapOf("4-3-3" to 9, "4-4-2" to 7, "4-2-3-1" to 6, "5-3-2" to 3)
            "4-2-3-1" -> mapOf("4-3-3" to 7, "4-1-2-1-2" to 6, "5-3-2" to 5, "4-4-2" to 4)
            else -> emptyMap()
        }
        val styleBonus = when (opponentStyle) {
            "High press" -> mapOf("4-2-3-1" to 4, "5-3-2" to 3, "4-3-3" to 2, "4-4-2" to 1)
            "Fast counter-attack" -> mapOf("5-3-2" to 5, "4-2-3-1" to 4, "4-1-2-1-2" to 3, "4-3-3" to 1)
            "Wide play" -> mapOf("5-3-2" to 5, "3-5-2" to 4, "4-4-2" to 3, "4-2-3-1" to 1)
            "Central build-up" -> mapOf("4-3-3" to 5, "4-2-3-1" to 4, "4-1-2-1-2" to 3, "5-3-2" to 1)
            "Deep defensive block" -> mapOf("4-3-3" to 5, "4-2-3-1" to 4, "4-4-2" to 3, "3-5-2" to 2)
            else -> emptyMap()
        }
        return (matchup[ourFormation] ?: 0) + (styleBonus[ourFormation] ?: 0)
    }

    private fun explanation(our: String, opponent: String, style: String): String {
        val shapeReason = when (opponent) {
            "4-3-3" -> when (our) {
                "5-3-2" -> "A back five offers extra cover against the opponent's three forward lanes; use wing-backs carefully."
                "4-2-3-1" -> "A double pivot can protect central space while the wide attackers support transitions."
                else -> "This option is selected mainly for your squad's role fit; monitor the opponent's wide forwards."
            }
            "4-2-4" -> when (our) {
                "5-3-2" -> "Extra central defenders help manage four forwards; the midfield three can compete for second balls."
                "4-3-3" -> "A three-player midfield can contest the centre and connect quickly to the front three."
                else -> "Keep midfield compact and avoid leaving the defence isolated against four forwards."
            }
            "5-3-2" -> when (our) {
                "4-3-3" -> "Use the front three to stretch the back five and attack the wide channels."
                "4-2-3-1" -> "The wide attackers and central attacking midfielder can create width and pockets around the back five."
                else -> "Look for width and patient circulation rather than forcing passes through the central block."
            }
            "4-4-2" -> "A midfield three can create a central overload; protect the space left in wide areas."
            "3-5-2" -> "Use wide forwards or wide midfielders to challenge the opponent's wing-back channels."
            "4-2-3-1" -> "Consider width and midfield pressure to disrupt the opponent's double pivot and central creator."
            else -> "Opponent shape is unknown; this is primarily the best squad-fit option, not a verified tactical counter."
        }
        val styleReason = when (style) {
            "High press" -> " Against high pressing, keep a safe passing outlet and avoid forcing the first forward pass."
            "Fast counter-attack" -> " Against fast counters, retain defensive cover behind the ball and avoid advancing both full-backs together."
            "Wide play" -> " Against wide play, protect both channels and track wide runners."
            "Central build-up" -> " Against central build-up, keep midfield distances compact and screen passes into the central attacker."
            "Deep defensive block" -> " Against a deep block, use width, patient switches and cutbacks rather than crowded central shots."
            else -> ""
        }
        return shapeReason + styleReason
    }
}
