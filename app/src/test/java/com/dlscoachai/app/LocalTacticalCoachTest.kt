package com.dlscoachai.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalTacticalCoachTest {
    @Test fun emptySquadRequestsEvidenceInsteadOfInventingPlayers() {
        val result = LocalTacticalCoach.analyse(emptyList(), emptyList())
        assertEquals("Squad data needed", result.single().title)
    }

    @Test fun unknownAttributesAreExplicitlyFlagged() {
        val players = listOf(Player("Mbappe", "FW", 96, statsKnown = false))
        val result = LocalTacticalCoach.analyse(players, emptyList())
        assertTrue(result.any { it.title == "Squad data quality" })
        assertTrue(result.first { it.title == "Squad data quality" }.evidence.contains("1 of 1"))
    }

    @Test fun matchAnalysisUsesOnlyRecordedResults() {
        val matches = listOf(
            Match("Loss", 0, 2, "Conceded on counter"),
            Match("Loss", 1, 3, "Lost midfield"),
            Match("Loss", 0, 1, "Late goal")
        )
        val result = LocalTacticalCoach.analyse(
            listOf(Player("Rodri", "DM", 95, statsKnown = false)), matches
        )
        assertTrue(result.any { it.title == "Defensive balance" })
        assertTrue(result.any { it.title == "Repeated losses" })
        assertFalse(result.any { it.title == "Chance creation" })
    }

    @Test fun goalkeeperSnapshotRequiresConfirmedGoalkeeperAttributes() {
        val unknownKeeper = Player("Keeper", "GK", 90, gkr = 99, gkh = 99, statsKnown = false)
        val unknownResult = LocalTacticalCoach.analyse(listOf(unknownKeeper), emptyList())
        assertFalse(unknownResult.any { it.title == "Goalkeeper snapshot" })

        val confirmedKeeper = unknownKeeper.copy(statsKnown = true)
        val confirmedResult = LocalTacticalCoach.analyse(listOf(confirmedKeeper), emptyList())
        assertTrue(confirmedResult.any { it.title == "Goalkeeper snapshot" })
    }
}
