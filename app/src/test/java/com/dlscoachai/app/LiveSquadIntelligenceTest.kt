package com.dlscoachai.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSquadIntelligenceTest {
    @Test
    fun parsesPlayerNamesAndRatingsOnlyOnSquadScreen() {
        val result = LiveSquadIntelligence.analyse(
            """
            MY SQUAD
            Kylian Mbappe 96
            Erling Haaland 95
            Speed 90
            """.trimIndent()
        )

        assertEquals("SQUAD", result.screenType)
        assertEquals(2, result.candidates.size)
        assertEquals("Kylian Mbappe", result.candidates[0].name)
        assertEquals(96, result.candidates[0].rating)
        assertEquals(95, result.candidates[1].rating)
        assertEquals("MEDIUM", result.confidence)
    }

    @Test
    fun doesNotTreatArbitraryNumbersAsPlayerRatingsOnUnknownScreen() {
        val result = LiveSquadIntelligence.analyse(
            """
            WELCOME BACK
            Kylian Mbappe 96
            """.trimIndent()
        )

        assertEquals("UNKNOWN", result.screenType)
        assertTrue(result.candidates.isEmpty())
    }

    @Test
    fun ignoresAttributeLabelsAndDuplicateNames() {
        val result = LiveSquadIntelligence.analyse(
            """
            SQUAD
            Speed 90
            Kylian Mbappe 96
            Kylian Mbappe 96
            """.trimIndent()
        )

        assertEquals(1, result.candidates.size)
        assertEquals("Kylian Mbappe", result.candidates.single().name)
    }

    @Test
    fun identifiesSettingsWithoutCreatingSquadCandidates() {
        val result = LiveSquadIntelligence.analyse(
            """
            SETTINGS
            Camera
            Graphics
            Controls
            """.trimIndent()
        )

        assertEquals("SETTINGS", result.screenType)
        assertTrue(result.candidates.isEmpty())
    }
}
