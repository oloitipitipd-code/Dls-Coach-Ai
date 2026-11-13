package com.dlscoachai.app

/** Explainable recommendations based only on entered squad data and manually tagged match notes. */
data class TacticalRecommendation(
    val formation: String, val score: Int, val filledSlots: Int, val totalSlots: Int,
    val squadFit: Int, val opponentFit: Int, val reasons: List<String>
)
private data class FormationProfile(val name: String, val slots: List<String>, val description: String)
private val formationProfiles = listOf(
    FormationProfile("4-2-3-1", listOf("GK","CB","CB","LB","RB","CDM","CDM","CAM","LW","RW","ST"), "Two holding midfielders protect the back four while the attacking midfield line supports one striker."),
    FormationProfile("4-3-3", listOf("GK","CB","CB","LB","RB","CDM","CM","CM","LW","RW","ST"), "A holding midfielder anchors two central midfielders; wingers provide width."),
    FormationProfile("4-1-2-1-2", listOf("GK","CB","CB","LB","RB","CDM","CM","CM","CAM","ST","ST"), "A narrow diamond overloads central areas but relies on full-backs for width."),
    FormationProfile("3-5-2", listOf("GK","CB","CB","CB","LM","RM","CDM","CM","CAM","ST","ST"), "Three centre-backs protect the centre while wing-backs provide width and recovery."),
    FormationProfile("5-3-2", listOf("GK","CB","CB","CB","LB","RB","CDM","CM","CM","ST","ST"), "A deeper five-player defence offers central protection and two forward outlets."),
    FormationProfile("4-4-2", listOf("GK","CB","CB","LB","RB","LM","RM","CM","CM","ST","ST"), "Two banks of four offer a simple defensive shape and two forward outlets.")
)
private fun compatiblePosition(natural: String, slot: String): Int {
    if (natural == slot) return 34
    val groups = mapOf(
        "LB" to setOf("LB","LM"), "RB" to setOf("RB","RM"),
        "CDM" to setOf("CDM","CM"), "CM" to setOf("CM","CDM","CAM"),
        "CAM" to setOf("CAM","CM","CF"), "LM" to setOf("LM","LW","LB"),
        "RM" to setOf("RM","RW","RB"), "LW" to setOf("LW","LM","ST"),
        "RW" to setOf("RW","RM","ST"), "ST" to setOf("ST","CF"),
        "CB" to setOf("CB"), "GK" to setOf("GK")
    )
    return if (natural in groups[slot].orEmpty()) 19 else 0
}
private fun roleAttributeBonus(player: Player, slot: String): Int {
    if (!player.statsKnown) return 0
    val values = when (slot) {
        "GK" -> listOf(player.gkr, player.gkh, player.str)
        "CB","LB","RB" -> listOf(player.tac, player.str, player.spe, player.sta)
        "CDM" -> listOf(player.tac, player.pas, player.sta, player.str)
        "CM" -> listOf(player.pas, player.con, player.sta, player.tac)
        "CAM" -> listOf(player.pas, player.con, player.sho, player.acc)
        "LM","RM","LW","RW" -> listOf(player.spe, player.acc, player.con, player.pas)
        else -> listOf(player.sho, player.spe, player.acc, player.con)
    }
    return (values.average().toInt() - 50).coerceIn(-10, 10)
}
private fun bestSlotAssignment(players: List<Player>, slots: List<String>): Pair<Int, Int> {
    val remaining = players.toMutableList()
    var total = 0
    var filled = 0
    slots.forEach { slot ->
        val best = remaining.map { p ->
            val compatibility = compatiblePosition(p.position, slot)
            p to if (compatibility == 0) Int.MIN_VALUE else compatibility + p.rating / 10 + roleAttributeBonus(p, slot)
        }.maxByOrNull { it.second }
        if (best != null && best.second != Int.MIN_VALUE) {
            remaining.remove(best.first); total += best.second; filled++
        }
    }
    return total to filled
}
private fun opponentTacticalSignals(matches: List<Match>): Map<String, Int> {
    val recent = matches.take(5)
    val tags = listOf("[COUNTER]", "[HIGH PRESS]", "[MIDFIELD LOST]", "[MISSED CHANCE]", "[GK ERROR]", "[DEFENSIVE ERROR]", "[SET PIECE]")
    return tags.associateWith { tag -> recent.sumOf { m -> Regex(Regex.escape(tag), RegexOption.IGNORE_CASE).findAll(m.note).count() } }
}
fun recommendTactics(players: List<Player>, matches: List<Match>): List<TacticalRecommendation> {
    if (players.isEmpty()) return emptyList()
    val signals = opponentTacticalSignals(matches)
    val counter = signals["[COUNTER]"] ?: 0
    val press = signals["[HIGH PRESS]"] ?: 0
    val midfieldLoss = signals["[MIDFIELD LOST]"] ?: 0
    val defensiveErrors = signals["[DEFENSIVE ERROR]"] ?: 0
    return formationProfiles.map { profile ->
        val (rawFit, filled) = bestSlotAssignment(players, profile.slots)
        val coverage = filled * 100 / profile.slots.size
        val averageFit = if (filled == 0) 0 else rawFit / filled
        var opponentFit = 0
        val reasons = mutableListOf<String>()
        if (counter > 0) {
            val bonus = if (profile.name in setOf("5-3-2", "4-2-3-1")) 8 else -2
            opponentFit += bonus
            reasons += "Recent notes contain $counter opponent counter-attack tag(s); ${if (bonus > 0) "this shape offers extra cover or holding midfield protection" else "check the space left during attacking transitions"}."
        }
        if (press > 0) {
            val bonus = if (profile.name in setOf("4-3-3", "4-2-3-1")) 5 else 0
            opponentFit += bonus
            reasons += "Recent notes contain $press high-press tag(s); use midfield outlets and avoid forcing central passes."
        }
        if (midfieldLoss > 0) {
            val bonus = if (profile.name in setOf("4-2-3-1", "5-3-2", "4-3-3")) 5 else -1
            opponentFit += bonus
            reasons += "Recent notes contain $midfieldLoss midfield-loss tag(s); prioritise a connected central screen."
        }
        if (defensiveErrors > 0) reasons += "$defensiveErrors defensive-error tag(s) were logged; review footage before attributing the cause to formation."
        if (reasons.isEmpty()) reasons += "No opponent pattern is sufficiently documented in the latest five match notes; ranking is driven mainly by squad-role coverage."
        reasons += profile.description
        val score = (coverage * 65 / 100 + averageFit.coerceIn(0, 50) + opponentFit).coerceIn(0, 100)
        TacticalRecommendation(profile.name, score, filled, profile.slots.size, averageFit, opponentFit, reasons)
    }.sortedWith(compareByDescending<TacticalRecommendation> { it.score }.thenByDescending { it.filledSlots }.thenBy { it.formation })
}
