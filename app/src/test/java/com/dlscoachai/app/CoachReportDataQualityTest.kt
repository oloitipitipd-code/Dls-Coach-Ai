package com.dlscoachai.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachReportDataQualityTest {
    @Test
    fun doesNotPresentPlaceholderAttributesAsVerifiedValues() {
        val player = Player(name = "Example Striker", position = "ST", rating = 90)
        val report = buildCoachReport(
            players = listOf(player),
            matches = emptyList(),
            formation = "4-3-3",
            style = "Aggressive attack + compact defence"
        )

        assertTrue(report.contains("card attributes not entered"))
        assertFalse(report.contains("SPE 50"))
        assertFalse(report.contains("GKH 50"))
    }

    @Test
    fun includesAttributesWhenTheyHaveBeenConfirmed() {
        val player = Player(
            name = "Verified Striker",
            position = "ST",
            rating = 91,
            spe = 92,
            acc = 93,
            sta = 88,
            str = 84,
            con = 95,
            pas = 86,
            sho = 97,
            tac = 40,
            gkr = 10,
            gkh = 12,
            statsKnown = true
        )
        val report = buildCoachReport(
            players = listOf(player),
            matches = emptyList(),
            formation = "4-3-3",
            style = "Aggressive attack + compact defence"
        )

        assertTrue(report.contains("SPE 92"))
        assertTrue(report.contains("SHO 97"))
        assertTrue(report.contains("GKH 12"))
        assertFalse(report.contains("card attributes not entered"))
    }

    @Test
    fun labelsUnknownNaturalPositionClearly() {
        val player = Player(name = "Unassigned Player", position = "UNK", rating = 85)
        val report = buildCoachReport(
            players = listOf(player),
            matches = emptyList(),
            formation = "4-3-3",
            style = "Aggressive attack + compact defence"
        )

        assertTrue(report.contains("position not entered"))
    }
}
