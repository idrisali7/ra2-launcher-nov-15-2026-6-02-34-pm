package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.GameFileStatus
import com.example.model.LauncherUiState
import com.example.ui.AppStrings
import com.example.ui.components.StatusIndicatorPill
import com.example.ui.components.TacticalCard
import com.example.ui.theme.AlliedCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.RadarEmeraldContainer
import com.example.ui.theme.SovietCrimson
import com.example.ui.theme.SovietCrimsonBright
import com.example.ui.theme.SovietCrimsonContainer
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalVoid

@Composable
fun DiagnosticsScreen(
    state: LauncherUiState,
    strings: AppStrings,
    onCheckFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TacticalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.diagnosticsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onCheckFiles,
                        enabled = state.hasFolderConfigured && !state.isScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = SovietCrimson),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("diagnostics_verify_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.checkFilesBtn)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                FileGroupSection(
                    title = strings.coreMixSection,
                    files = state.fileStatuses.filter { it.isRequiredCore },
                    strings = strings
                )
                Spacer(modifier = Modifier.height(12.dp))
                FileGroupSection(
                    title = strings.executablesSection,
                    files = state.fileStatuses.filter { it.isExecutable },
                    strings = strings
                )
                Spacer(modifier = Modifier.height(12.dp))
                FileGroupSection(
                    title = strings.configIniSection,
                    files = state.fileStatuses.filter { it.isConfig },
                    strings = strings
                )
            }
        }

        item {
            TacticalCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = AlliedCyan)
                    Text(
                        text = strings.launchLogsTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TacticalVoid,
                    border = BorderStroke(1.dp, TacticalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.launchLogs.isEmpty()) {
                        Text(
                            text = strings.noLogsYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.launchLogs.forEach { log ->
                                Text(
                                    text = log,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RadarEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileGroupSection(
    title: String,
    files: List<GameFileStatus>,
    strings: AppStrings
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = AlliedCyan
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = TacticalVoid,
            border = BorderStroke(1.dp, TacticalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                files.forEachIndexed { idx, file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (file.exists) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (file.exists) RadarEmerald else SovietCrimsonBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = file.fileName,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (file.exists && file.sizeBytes > 0L) {
                                    Text(
                                        text = formatByteSize(file.sizeBytes),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        StatusIndicatorPill(
                            label = if (file.exists) strings.filePresent else strings.fileMissing,
                            dotColor = if (file.exists) RadarEmerald else SovietCrimsonBright,
                            containerColor = if (file.exists) RadarEmeraldContainer.copy(alpha = 0.6f) else SovietCrimsonContainer.copy(alpha = 0.6f),
                            contentColor = if (file.exists) RadarEmerald else SovietCrimsonBright
                        )
                    }
                    if (idx < files.lastIndex) {
                        HorizontalDivider(color = TacticalBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

private fun formatByteSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024f * 1024f))
    }
}
