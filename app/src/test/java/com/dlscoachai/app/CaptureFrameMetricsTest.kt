package com.dlscoachai.app

import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureFrameMetricsTest {
    @Test
    fun samplesOpaqueRedRgbaPixels() {
        val bytes = ByteArray(4) { index -> if (index == 0) 0xff.toByte() else 0 }
        val result = CaptureFrameMetrics.averageBrightness(ByteBuffer.wrap(bytes), 1, 1, 4, 4)
        assertEquals(54, result)
    }

    @Test
    fun respectsRowStridePadding() {
        // Two RGBA pixels per row, followed by four padding bytes.
        val bytes = ByteArray(24)
        for (row in 0..1) {
            val offset = row * 12
            bytes[offset] = 0xff.toByte()
            bytes[offset + 1] = 0xff.toByte()
            bytes[offset + 2] = 0xff.toByte()
            bytes[offset + 3] = 0xff.toByte()
            bytes[offset + 4] = 0xff.toByte()
            bytes[offset + 5] = 0xff.toByte()
            bytes[offset + 6] = 0xff.toByte()
            bytes[offset + 7] = 0xff.toByte()
        }
        val result = CaptureFrameMetrics.averageBrightness(ByteBuffer.wrap(bytes), 2, 2, 12, 4)
        assertEquals(255, result)
    }

    @Test
    fun rejectsInvalidDimensionsAndPixelStride() {
        val buffer = ByteBuffer.wrap(ByteArray(16))
        assertEquals(-1, CaptureFrameMetrics.averageBrightness(buffer, 0, 1, 4, 4))
        assertEquals(-1, CaptureFrameMetrics.averageBrightness(buffer, 1, 1, 4, 2))
    }

    @Test
    fun rejectsBufferTooShortToReadAnyRgbPixel() {
        val result = CaptureFrameMetrics.averageBrightness(ByteBuffer.wrap(byteArrayOf(1, 2)), 1, 1, 4, 4)
        assertEquals(-1, result)
    }
}

class MatchPeriodComparisonTest {
    @Test
    fun requiresTenMatchesForTwoCompleteSamples() {
        val matches = List(9) { Match("Win", 2, 0, "") }
        assertEquals(true, compareRecentPeriods(matches).first().contains("at least ten matches"))
    }

    @Test
    fun detectsImprovedResultsAcrossTwoFiveMatchPeriods() {
        val latest = List(5) { Match("Win", 2, 0, "") }
        val previous = List(5) { Match("Loss", 0, 2, "") }
        val insights = compareRecentPeriods(latest + previous)
        assertEquals(true, insights.first().contains("goals scored per match +2.0"))
        assertEquals(true, insights.first().contains("goals conceded per match -2.0"))
        assertEquals(true, insights.first().contains("win rate +100.0 percentage points"))
        assertEquals(true, insights.any { it.contains("defensive results improved") })
    }
}

class TacticalEventInsightsTest {
    @Test
    fun countsOnlyExplicitlyTaggedEvents() {
        val matches = listOf(
            Match("Loss", 1, 3, "Lost shape [COUNTER] then another [COUNTER]"),
            Match("Win", 2, 0, "Poor pass [MIDFIELD LOST]"),
            Match("Loss", 0, 1, "Keeper mistake [GK ERROR]")
        )
        val insights = tacticalEventInsights(matches)
        assertEquals(true, insights.any { it.contains("Opponent counter-attacks: 2") })
        assertEquals(true, insights.any { it.contains("Midfield possession losses: 1") })
        assertEquals(true, insights.any { it.contains("Goalkeeper errors: 1") })
    }


    @Test
    fun comparesLatestAndPreviousFiveMatchEventCounts() {
        val latest = List(5) { Match("Loss", 0, 2, "[COUNTER]") }
        val previous = List(5) { Match("Win", 2, 0, "") }
        val insights = tacticalEventInsights(latest + previous)
        assertEquals(true, insights.any { it.contains("Opponent counter-attacks: 5 in the latest 5") && it.contains("up from 0 in the previous five") })
    }

    @Test
    fun doesNotClaimACompleteComparisonWithFewerThanTenMatches() {
        val matches = List(6) { Match("Loss", 0, 1, "[COUNTER]") }
        val insights = tacticalEventInsights(matches)
        assertEquals(true, insights.any { it.contains("Log at least ten matches") })
        assertEquals(true, insights.any { it.contains("no earlier five-match comparison") })
    }

    @Test
    fun explainsWhenNoTacticalEventsHaveBeenTagged() {
        val insights = tacticalEventInsights(listOf(Match("Win", 2, 1, "Good game")))
        assertEquals(true, insights.single().contains("No tagged tactical events in the latest five"))
    }
}

class PlayerContributionStatsTest {
    @Test
    fun aggregatesRepeatedGoalScorersAndAssists() {
        val matches = listOf(
            Match("Win", 3, 1, "", scorers = listOf("Mbappe", "Mbappe", "Salah"), assists = listOf("Salah", "Bellingham")),
            Match("Draw", 1, 1, "", scorers = listOf("Salah"), assists = listOf("Mbappe"))
        )
        val stats = playerContributionStats(matches).associateBy { it.name }
        assertEquals(2, stats["Mbappe"]?.goals)
        assertEquals(1, stats["Mbappe"]?.assists)
        assertEquals(2, stats["Salah"]?.goals)
        assertEquals(1, stats["Salah"]?.assists)
        assertEquals(1, stats["Bellingham"]?.assists)
    }

    @Test
    fun ranksPlayersByRecordedGoalAndAssistContributions() {
        val matches = listOf(
            Match("Win", 2, 0, "", scorers = listOf("Player A", "Player B"), assists = listOf("Player A"))
        )
        val stats = playerContributionStats(matches)
        assertEquals("Player A", stats.first().name)
    }

    @Test
    fun returnsNoContributionRowsWhenNoPlayersWereEntered() {
        assertEquals(emptyList<PlayerContribution>(), playerContributionStats(listOf(Match("Win", 1, 0, ""))))
    }
}

class PlayerContributionTrendTest {
    @Test
    fun comparesPlayerContributionsAcrossTwoCompleteFiveMatchPeriods() {
        val recent = List(5) { Match("Win", 2, 0, "", scorers = listOf("Mbappe"), assists = listOf("Salah")) }
        val previous = List(5) { Match("Win", 1, 0, "", scorers = listOf("Salah")) }
        val trends = playerContributionTrends(recent + previous).associateBy { it.name }
        assertEquals(5, trends["Mbappe"]?.recentGoals)
        assertEquals(0, trends["Mbappe"]?.previousGoals)
        assertEquals(5, trends["Salah"]?.recentAssists)
        assertEquals(5, trends["Salah"]?.previousGoals)
    }

    @Test
    fun doesNotComparePlayerFormBeforeTenMatches() {
        val matches = List(9) { Match("Win", 1, 0, "", scorers = listOf("Player A")) }
        assertEquals(emptyList<PlayerContributionTrend>(), playerContributionTrends(matches))
    }
    @Test
    fun emptySavedSquadRemainsEmptyAfterReload() {
        assertTrue(loadStoredPlayers("[]", hasStoredValue = true).isEmpty())
    }

    @Test
    fun firstLaunchUsesSeedSquadOnlyWhenNoSavedValueExists() {
        assertTrue(loadStoredPlayers(null, hasStoredValue = false).isNotEmpty())
    }

    @Test
    fun malformedSavedSquadDoesNotSilentlyRestoreSeedPlayers() {
        assertTrue(loadStoredPlayers("{broken", hasStoredValue = true).isEmpty())
    }

    @Test
    fun diamondFormationHasCorrectRoleSlots() {
        assertEquals(
            listOf("GK", "LB", "CB", "CB", "RB", "CDM", "CM", "CM", "CAM", "ST", "ST"),
            formationSlots("4-1-2-1-2")
        )
    }

    @Test
    fun supportedFormationsAlwaysHaveElevenSlots() {
        listOf("4-2-3-1", "4-3-3", "4-1-2-1-2", "3-5-2", "5-3-2", "4-4-2")
            .forEach { assertEquals(it, 11, formationSlots(it).size) }
    }

}
