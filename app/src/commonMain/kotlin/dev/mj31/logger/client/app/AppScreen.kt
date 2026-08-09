package dev.mj31.logger.client.app

/**
 * The two things the window can be showing.
 *
 * Navigation is this small on purpose: choosing a session and working on one are the only two modes
 * the application has, and a back stack would be machinery for a journey with one step.
 */
enum class AppScreen {

    /** Where a launch lands: what was open last, what has been saved, and a clean sheet. */
    SESSIONS,

    /** The workspace itself — the log beside the screencast. */
    PLAYER,
}
