package dev.mj31.logger.client.domain.model.legal

/**
 * One licence text as it travels with the binaries.
 *
 * [fileName] is deliberately the name of the file that ships rather than a prettier label: the same
 * name appears in the disk image, inside the installed application and in this list, so a user
 * comparing the three sees one document, not three that merely look alike.
 */
data class LegalNotice(
    val fileName: String,
    val text: String,
)
