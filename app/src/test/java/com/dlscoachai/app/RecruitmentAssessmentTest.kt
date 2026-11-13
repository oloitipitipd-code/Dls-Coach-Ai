package com.dlscoachai.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecruitmentAssessmentTest {
    @Test
    fun rejectsCandidatesBelowMinimumOverall() {
        val candidate = Player("Candidate", "ST", 79)
        val result = assessRecruitment(candidate, emptyList())
        assertEquals("DO NOT SIGN", result.decision)
    }

    @Test
    fun requiresConfirmedNaturalPosition() {
        val candidate = Player("Candidate", "UNK", 90)
        val result = assessRecruitment(candidate, emptyList())
        assertEquals("NEEDS DATA", result.decision)
    }

    @Test
    fun flagsPlayersAlreadyInSquad() {
        val owned = Player("Existing", "ST", 90)
        val result = assessRecruitment(owned, listOf(owned))
        assertEquals("ALREADY OWNED", result.decision)
    }

    @Test
    fun identifiesAnUnfilledPositionAsUsefulDepth() {
        val candidate = Player("Goalkeeper", "GK", 84)
        val squad = listOf(Player("Centre back", "CB", 90))
        val result = assessRecruitment(candidate, squad)
        assertEquals("SIGN — USEFUL DEPTH", result.decision)
        assertTrue(result.reason.contains("No player is currently registered"))
    }

    @Test
    fun recognisesClearOverallUpgradeInSamePosition() {
        val current = Player("Current striker", "ST", 90)
        val candidate = Player("Scouted striker", "ST", 94)
        val result = assessRecruitment(candidate, listOf(current))
        assertEquals("SIGN — STRONG UPGRADE", result.decision)
    }

    @Test
    fun avoidsSigningAWeakerDuplicateWhenDepthIsAlreadyAdequate() {
        val squad = listOf(
            Player("Striker one", "ST", 94),
            Player("Striker two", "ST", 92),
            Player("Striker three", "ST", 90)
        )
        val candidate = Player("Weaker striker", "ST", 85)
        val result = assessRecruitment(candidate, squad)
        assertEquals("DO NOT SIGN — SAVE RESOURCES", result.decision)
    }
}
