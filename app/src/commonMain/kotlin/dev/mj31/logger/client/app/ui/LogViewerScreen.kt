package dev.mj31.logger.client.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mj31.logger.client.app.theme.LogLevelDebug
import dev.mj31.logger.client.app.theme.LogLevelError
import dev.mj31.logger.client.app.theme.LogLevelFatal
import dev.mj31.logger.client.app.theme.LogLevelInfo
import dev.mj31.logger.client.app.theme.LogLevelVerbose
import dev.mj31.logger.client.app.theme.LogLevelWarn
import dev.mj31.logger.client.data.repository.DefaultLogRepository
import dev.mj31.logger.client.domain.model.LogEntry
import dev.mj31.logger.client.domain.model.LogLevel
import dev.mj31.logger.client.domain.repository.LogRepository

@Composable
fun LogViewerScreen(
    repository: LogRepository = remember { DefaultLogRepository() },
) {
    var searchQuery by remember { mutableStateOf(value = "") }
    val logs by repository.observeLogs().collectAsState(initial = emptyList())

    val filteredLogs = remember(key1 = searchQuery, key2 = logs) {
        if (searchQuery.isBlank()) {
            logs
        } else {
            logs.filter {
                it.tag.contains(other = searchQuery, ignoreCase = true) ||
                    it.message.contains(other = searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(all = 16.dp),
    ) {
        HeaderSection(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            logCount = filteredLogs.size,
        )

        Spacer(modifier = Modifier.height(height = 16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            items(items = filteredLogs, key = { it.id }) { log ->
                LogItemRow(log = log)
            }
        }
    }
}

@Composable
private fun HeaderSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    logCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Logger Client",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Showing $logCount log events",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(text = "Filter logs by tag or message...") },
            modifier = Modifier.width(width = 360.dp),
            singleLine = true,
        )
    }
}

@Composable
private fun LogItemRow(log: LogEntry) {
    val levelColor = when (log.level) {
        LogLevel.VERBOSE -> LogLevelVerbose
        LogLevel.DEBUG -> LogLevelDebug
        LogLevel.INFO -> LogLevelInfo
        LogLevel.WARN -> LogLevelWarn
        LogLevel.ERROR -> LogLevelError
        LogLevel.FATAL -> LogLevelFatal
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(size = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(width = 6.dp)
                    .height(height = 40.dp)
                    .background(color = levelColor, shape = RoundedCornerShape(size = 3.dp)),
            )

            Spacer(modifier = Modifier.width(width = 12.dp))

            Column(modifier = Modifier.weight(weight = 1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.level.name,
                        color = levelColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.width(width = 8.dp))
                    Text(
                        text = "[${log.tag}]",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Spacer(modifier = Modifier.height(height = 4.dp))

                Text(
                    text = log.message,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
