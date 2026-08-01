package dev.mj31.logger.client.app

import androidx.compose.runtime.Composable
import dev.mj31.logger.client.app.theme.LoggerTheme
import dev.mj31.logger.client.app.ui.LogViewerScreen

@Composable
fun App() {
    LoggerTheme {
        LogViewerScreen()
    }
}
