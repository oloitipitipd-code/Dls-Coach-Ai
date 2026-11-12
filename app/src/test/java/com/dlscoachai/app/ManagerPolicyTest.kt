package com.dlscoachai.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagerPolicyTest {
    @Test
    fun localReversibleCoachingTasksCanRunAutomatically() {
        val allowed = listOf(
            ManagerOperation.REFRESH_SQUAD_ASSESSMENT,
            ManagerOperation.GENERATE_STARTING_XI,
            ManagerOperation.REFRESH_TACTICAL_PLAN,
            ManagerOperation.PREPARE_PLAYER_DEVELOPMENT_PRIORITIES,
            ManagerOperation.SUMMARISE_RECORDED_MATCHES
        )

        allowed.forEach { operation ->
            assertTrue("$operation should be autonomous local work", ManagerPolicy.canRunAutomatically(operation))
            assertFalse("$operation should not need a purchase approval gate", ManagerPolicy.requiresExplicitApproval(operation))
            assertTrue("$operation should be supported locally", ManagerPolicy.isExecutionSupported(operation))
        }
    }

    @Test
    fun consequentialAndGameAffectingTasksAreGatedAndNotExecutable() {
        val restricted = listOf(
            ManagerOperation.SPEND_COINS,
            ManagerOperation.SPEND_GEMS,
            ManagerOperation.SIGN_OR_RELEASE_PLAYER,
            ManagerOperation.UPGRADE_PLAYER,
            ManagerOperation.CLAIM_REWARD,
            ManagerOperation.CHANGE_DLS_SETTINGS,
            ManagerOperation.CHANGE_ACCOUNT_OR_PROFILE,
            ManagerOperation.SEND_GAME_INPUT
        )

        restricted.forEach { operation ->
            assertFalse("$operation must not run automatically", ManagerPolicy.canRunAutomatically(operation))
            assertTrue("$operation must require explicit approval", ManagerPolicy.requiresExplicitApproval(operation))
            assertFalse("$operation has no supported execution path in this app", ManagerPolicy.isExecutionSupported(operation))
        }
    }
    @Test
    fun autonomyLabelDoesNotClaimGameControl() {
        assertTrue(ManagerPolicy.MODE_LABEL.contains("LOCAL"))
        assertFalse(ManagerPolicy.MODE_LABEL.contains("FULL AUTONOMOUS"))
    }

}
