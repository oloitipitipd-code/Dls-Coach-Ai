package com.dlscoachai.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormationAssessmentTest {
    @Test
    fun managerChoosesAFormationFromTheRegisteredSquad() {
        val squad = listOf(
            Player("Keeper", "GK", 90),
            Player("Left back", "LB", 88), Player("Right back", "RB", 88),
            Player("Centre back 1", "CB", 91), Player("Centre back 2", "CB", 90),
            Player("Defensive midfielder", "CDM", 92),
            Player("Midfielder 1", "CM", 90), Player("Midfielder 2", "CM", 89),
            Player("Left winger", "LW", 91), Player("Right winger", "RW", 90),
            Player("Striker", "ST", 94), Player("Extra striker", "ST", 89)
        )

        val selected = chooseBestFormation(squad)

        assertNotNull(selected)
        assertEquals(11, selected!!.filledSlots)
        assertTrue(selected.formation in listOf("4-2-3-1", "4-3-3", "4-1-2-1-2", "3-5-2", "5-3-2", "4-4-2"))
        assertEquals(11, selected.lineup.mapNotNull { it.player }.distinct().size)
    }

    @Test
    fun unknownOutfieldPositionsAreNotDiscardedButConfidenceIsLow() {
        val squad = listOf(
            Player("Keeper", "GK", 90),
            Player("Unknown 1", "UNK", 96), Player("Unknown 2", "UNK", 95),
            Player("Unknown 3", "UNK", 94), Player("Unknown 4", "UNK", 93),
            Player("Unknown 5", "UNK", 92), Player("Unknown 6", "UNK", 91),
            Player("Unknown 7", "UNK", 90), Player("Unknown 8", "UNK", 89),
            Player("Unknown 9", "UNK", 88), Player("Unknown 10", "UNK", 87)
        )

        val selected = chooseBestFormation(squad)

        assertNotNull(selected)
        assertEquals(11, selected!!.filledSlots)
        assertEquals("LOW", selected.confidence)
    }

    @Test
    fun noSquadProducesNoFormationGuess() {
        assertEquals(null, chooseBestFormation(emptyList()))
    }
}
