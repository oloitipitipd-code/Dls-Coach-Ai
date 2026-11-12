package com.dlscoachai.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTacticalManagerTest {
    private val squad = listOf(
        Player("Keeper", "GK", 90),
        Player("LB", "LB", 88), Player("RB", "RB", 88),
        Player("CB1", "CB", 91), Player("CB2", "CB", 90), Player("CB3", "CB", 89),
        Player("CDM", "CDM", 92), Player("CM1", "CM", 90), Player("CM2", "CM", 89),
        Player("LW", "LW", 91), Player("RW", "RW", 90), Player("ST1", "ST", 94), Player("ST2", "ST", 89)
    )

    @Test fun returnsACompleteSquadBasedRecommendation() {
        val result = AdaptiveTacticalManager.recommend(squad, "4-3-3", "Wide play")
        assertNotNull(result)
        assertEquals(11, result!!.assessment.filledSlots)
        assertTrue(result.assessment.formation in AdaptiveTacticalManager.supportedFormations)
        assertTrue(result.reason.isNotBlank())
    }

    @Test fun opponentShapeChangesTacticalFit() {
        assertTrue(
            AdaptiveTacticalManager.tacticalFit("5-3-2", "4-3-3", "Unknown") >
                AdaptiveTacticalManager.tacticalFit("4-4-2", "4-3-3", "Unknown")
        )
        assertTrue(
            AdaptiveTacticalManager.tacticalFit("4-3-3", "5-3-2", "Unknown") >
                AdaptiveTacticalManager.tacticalFit("5-3-2", "5-3-2", "Unknown")
        )
    }

    @Test fun styleChangesTacticalFit() {
        assertTrue(
            AdaptiveTacticalManager.tacticalFit("5-3-2", "4-4-2", "Fast counter-attack") >
                AdaptiveTacticalManager.tacticalFit("4-4-2", "4-4-2", "Fast counter-attack")
        )
    }

    @Test fun emptySquadReturnsNoRecommendation() {
        assertEquals(null, AdaptiveTacticalManager.recommend(emptyList(), "4-3-3"))
        assertTrue(AdaptiveTacticalManager.rankedOptions(emptyList(), "4-3-3").isEmpty())
    }

    @Test fun unknownOpponentShapeIsLowConfidence() {
        val result = AdaptiveTacticalManager.recommend(squad, "Unknown", "Unknown")
        assertNotNull(result)
        assertEquals("LOW", result!!.confidence)
    }
}
