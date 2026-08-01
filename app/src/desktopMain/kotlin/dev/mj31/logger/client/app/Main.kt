package dev.mj31.logger.client.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(width = 1024.dp, height = 768.dp),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Logger Client - Desktop App",
        state = windowState,
    ) {
        App()
    }
}
