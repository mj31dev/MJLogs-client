package dev.mj31.logger.client.app.features.legal

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.mj31.logger.client.app.BuildInfo
import dev.mj31.logger.client.domain.model.legal.LegalNotice
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AboutContentTest {

    private val notices = listOf(
        LegalNotice(fileName = "THIRD-PARTY.txt", text = "This application bundles FFmpeg"),
        LegalNotice(fileName = "LGPL-3.0.txt", text = "GNU LESSER GENERAL PUBLIC LICENSE"),
    )

    @Test
    fun `the summary names the product, its version and the licence it is under`() = runComposeUiTest {
        setContent { AboutContent(notices = notices) }

        onNodeWithText(text = "MJLogs").assertIsDisplayed()
        onNodeWithText(text = "Version ${BuildInfo.PRODUCT_VERSION}").assertIsDisplayed()
        onNodeWithText(text = "Licensed under the Apache License, Version 2.0.").assertIsDisplayed()
    }

    @Test
    fun `the full texts are one step away from the summary`() = runComposeUiTest {
        setContent { AboutContent(notices = notices) }

        onNodeWithText(text = "Licenses and notices").performClick()

        onNodeWithText(text = "THIRD-PARTY.txt").assertIsDisplayed()
        onNodeWithText(text = "This application bundles FFmpeg").assertIsDisplayed()
    }

    @Test
    fun `picking a tab shows that licence text`() = runComposeUiTest {
        setContent { AboutContent(notices = notices) }
        onNodeWithText(text = "Licenses and notices").performClick()

        onNodeWithText(text = "LGPL-3.0.txt").performClick()

        onNodeWithText(text = "GNU LESSER GENERAL PUBLIC LICENSE").assertIsDisplayed()
    }

    @Test
    fun `the reader goes back to the summary`() = runComposeUiTest {
        setContent { AboutContent(notices = notices) }
        onNodeWithText(text = "Licenses and notices").performClick()

        onNodeWithText(text = "Back to About").performClick()

        onNodeWithText(text = "Copyright 2026 mj31dev").assertIsDisplayed()
    }

    /** A build that carries no notice must say where the texts are, not offer an empty reader. */
    @Test
    fun `an unpackaged run explains where the texts live instead of offering the reader`() = runComposeUiTest {
        setContent { AboutContent(notices = emptyList()) }

        onNodeWithText(text = "No licence text next to this build").assertIsDisplayed()
    }
}
