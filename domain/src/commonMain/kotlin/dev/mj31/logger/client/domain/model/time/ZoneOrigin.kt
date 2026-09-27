package dev.mj31.logger.client.domain.model.time

/** Where the zone a file is read in was taken from, strongest first. */
enum class ZoneOrigin {

    /** The timestamps carry their own offset, so there was nothing to decide. */
    LINE,

    /** The user chose it for this file. */
    CHOSEN,

    /** The file names its zone in the text before its first record. */
    HEADER,

    /** Nothing said anything, and UTC was assumed. */
    DEFAULT,
}
