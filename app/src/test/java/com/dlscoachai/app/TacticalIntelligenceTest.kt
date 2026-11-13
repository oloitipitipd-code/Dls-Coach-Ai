package com.dlscoachai.app
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class TacticalIntelligenceTest {
    private fun squad() = listOf(
        Player("Keeper","GK",86), Player("Left CB","CB",88), Player("Centre CB","CB",90),
        Player("Right CB","CB",87), Player("Left back","LB",85), Player("Right back","RB",84),
        Player("Holder","CDM",89), Player("Mid one","CM",87), Player("Mid two","CM",86),
        Player("Striker one","ST",91), Player("Striker two","ST",88), Player("Left wing","LW",85),
        Player("Right wing","RW",86), Player("Attacking mid","CAM",88)
    )
    @Test fun returnsRankedFormationOptionsForKnownSquad() {
        val results = recommendTactics(squad(), emptyList())
        assertEquals(6, results.size)
        assertTrue(results.zipWithNext().all { (a,b) -> a.score >= b.score })
        assertTrue(results.first().filledSlots >= 10)
    }
    @Test fun emptySquadDoesNotInventAFormation() { assertTrue(recommendTactics(emptyList(), emptyList()).isEmpty()) }
    @Test fun counterAttackEvidenceChangesTacticalScoring() {
        val baseline = recommendTactics(squad(), emptyList()).associateBy { it.formation }
        val observed = recommendTactics(squad(), listOf(Match("Loss",0,2,"[COUNTER] [COUNTER] Opponent countered twice"))).associateBy { it.formation }
        assertTrue(observed.getValue("5-3-2").opponentFit > baseline.getValue("5-3-2").opponentFit)
    }
    @Test fun unknownPositionsAreNotCountedAsRoleCoverage() {
        val result = recommendTactics(listOf(Player("Unknown","UNK",99)), emptyList())
        assertTrue(result.all { it.filledSlots == 0 })
    }
    @Test fun verifiedAttributesCanInfluenceRoleFit() {
        val unverified = Player("Striker","ST",90,spe=99,sho=99,statsKnown=false)
        val verified = unverified.copy(statsKnown=true)
        val a = recommendTactics(listOf(unverified), emptyList()).first().squadFit
        val b = recommendTactics(listOf(verified), emptyList()).first().squadFit
        assertTrue(b >= a)
    }
}
