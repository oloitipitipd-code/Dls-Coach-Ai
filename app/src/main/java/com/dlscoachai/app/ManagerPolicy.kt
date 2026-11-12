package com.dlscoachai.app

/**
 * Permission boundary for the user's selected full-autonomy manager mode.
 *
 * Only local, reversible coaching work is executable automatically. DLS account
 * operations and game inputs are deliberately unsupported by this app build,
 * even when a user has approved them; approval is not a substitute for an
 * authorised First Touch Games integration.
 */
enum class ManagerOperation {
    REFRESH_SQUAD_ASSESSMENT,
    GENERATE_STARTING_XI,
    REFRESH_TACTICAL_PLAN,
    PREPARE_PLAYER_DEVELOPMENT_PRIORITIES,
    SUMMARISE_RECORDED_MATCHES,
    SPEND_COINS,
    SPEND_GEMS,
    SIGN_OR_RELEASE_PLAYER,
    UPGRADE_PLAYER,
    CLAIM_REWARD,
    CHANGE_DLS_SETTINGS,
    CHANGE_ACCOUNT_OR_PROFILE,
    SEND_GAME_INPUT
}

object ManagerPolicy {
    const val MODE_LABEL = "FULL AUTONOMOUS"
    const val APPROVAL_LABEL = "APPROVAL REQUIRED"

    private val autonomousLocalOperations = setOf(
        ManagerOperation.REFRESH_SQUAD_ASSESSMENT,
        ManagerOperation.GENERATE_STARTING_XI,
        ManagerOperation.REFRESH_TACTICAL_PLAN,
        ManagerOperation.PREPARE_PLAYER_DEVELOPMENT_PRIORITIES,
        ManagerOperation.SUMMARISE_RECORDED_MATCHES
    )

    private val approvalGatedOperations = setOf(
        ManagerOperation.SPEND_COINS,
        ManagerOperation.SPEND_GEMS,
        ManagerOperation.SIGN_OR_RELEASE_PLAYER,
        ManagerOperation.UPGRADE_PLAYER,
        ManagerOperation.CLAIM_REWARD,
        ManagerOperation.CHANGE_DLS_SETTINGS,
        ManagerOperation.CHANGE_ACCOUNT_OR_PROFILE,
        ManagerOperation.SEND_GAME_INPUT
    )

    /** True only for reversible work performed entirely inside DLS Coach AI. */
    fun canRunAutomatically(operation: ManagerOperation): Boolean =
        operation in autonomousLocalOperations

    /** Consequential or game-affecting operations always require explicit approval. */
    fun requiresExplicitApproval(operation: ManagerOperation): Boolean =
        operation in approvalGatedOperations

    /**
     * Whether this app version has a supported execution path.
     * In-game/account operations remain false: approval alone cannot enable them.
     */
    fun isExecutionSupported(operation: ManagerOperation): Boolean =
        canRunAutomatically(operation)
}
