package com.dlscoachai.app

/**
 * Offline coaching engine. It uses only squad and match records stored by this app.
 * Unknown attributes are never scored as if they were measured facts.
 */
data class LocalCoachInsight(
    val title: String,
    val evidence: String,
    val recommendation: String,
    val confidence: Confidence
) {
    enum class Confidence { HIGH, MEDIUM, LOW }
}

object LocalTacticalCoach {
    fun analyse(players: List<Player>, matches: List<Match>, preferredStyle: String = "Aggressive attack + compact defence"): List<LocalCoachInsight> {
        val insights = mutableListOf<LocalCoachInsight>()
        val known = players.filter { it.statsKnown }
        val unknown = players.size - known.size

        if (players.isEmpty()) return listOf(LocalCoachInsight(
            "Squad data needed", "No players have been saved in the local squad.",
            "Add your squad and confirm positions and visible attributes before requesting a best XI.",
            LocalCoachInsight.Confidence.HIGH
        ))

        if (unknown > 0) insights += LocalCoachInsight(
            "Squad data quality", "$unknown of ${players.size} players have unconfirmed or missing card attributes.",
            "Confirm positions and attributes. Until then, use ratings only as a broad signal; default attribute values are not real measurements.",
            LocalCoachInsight.Confidence.HIGH
        )

        val goalkeepers = players.filter { it.position.equals("GK", true) }
        if (goalkeepers.isEmpty()) insights += LocalCoachInsight(
            "Goalkeeper role not confirmed", "No player is recorded with position GK.",
            "Confirm the goalkeeper and visible GKR/GKH values before judging shot-stopping.",
            LocalCoachInsight.Confidence.HIGH
        ) else goalkeepers.filter { it.statsKnown }.maxByOrNull { it.gkr + it.gkh }?.let { best ->
            insights += LocalCoachInsight(
                "Goalkeeper snapshot", "${best.name}: GKR ${best.gkr}, GKH ${best.gkh}.",
                "Use this as a baseline and compare it with logged match evidence; a score alone cannot prove keeper error.",
                LocalCoachInsight.Confidence.MEDIUM
            )
        }

        val recent = matches.take(5)
        if (recent.isEmpty()) {
            insights += LocalCoachInsight(
                "Match evidence needed", "There are no saved match records yet.",
                "Log at least three matches with result, goals for, goals against and a short note before drawing form-based conclusions.",
                LocalCoachInsight.Confidence.HIGH
            )
        } else {
            val scored = recent.sumOf { it.scored }
            val conceded = recent.sumOf { it.conceded }
            val losses = recent.count { it.result.equals("Loss", true) }
            insights += LocalCoachInsight(
                if (conceded > scored) "Defensive balance" else "Recent goal balance",
                "Latest ${recent.size} matches: $scored goals scored, $conceded conceded.",
                if (conceded > scored)
                    "Review the first cause of each conceded goal. For an aggressive style, retain a holding midfielder and avoid advancing both fullbacks together."
                else
                    "Keep the structure under review; change one major tactical variable at a time and compare the next results.",
                LocalCoachInsight.Confidence.MEDIUM
            )
            if (losses >= 3) insights += LocalCoachInsight(
                "Repeated losses", "$losses losses in the latest ${recent.size} logged matches.",
                "Test one controlled adjustment for the next two matches and record the outcome before changing anything else.",
                LocalCoachInsight.Confidence.MEDIUM
            )
            if (scored == 0) insights += LocalCoachInsight(
                "Chance creation", "No goals were recorded across the latest ${recent.size} matches.",
                "Review chance quality, striker role fit and attacking support. The score alone cannot identify the cause.",
                LocalCoachInsight.Confidence.MEDIUM
            )
        }

        if (known.isNotEmpty()) {
            val average = known.map { it.rating }.average()
            insights += LocalCoachInsight(
                "Confirmed-player overview",
                "${known.size} players have confirmed attributes; mean card rating ${"%.1f".format(java.util.Locale.ROOT, average)}.",
                "Use role-relevant attributes for selection. Overall rating alone does not establish the best player for a position.",
                LocalCoachInsight.Confidence.MEDIUM
            )
        }
        if (preferredStyle.contains("aggressive", true)) insights += LocalCoachInsight(
            "Attacking with cover", "Selected approach: aggressive attack with defensive compactness.",
            "Use width and forward support, but keep a central screen and stagger fullback advances to reduce counterattack exposure.",
            LocalCoachInsight.Confidence.LOW
        )
        return insights.distinctBy { it.title }.take(6)
    }
}
