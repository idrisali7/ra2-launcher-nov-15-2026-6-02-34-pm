package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.LauncherUiState
import com.example.model.ModEntry
import com.example.ui.AppStrings
import com.example.ui.components.CreateModDialog
import com.example.ui.components.StatusIndicatorPill
import com.example.ui.components.TacticalCard
import com.example.ui.theme.AlliedCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.SovietCrimson
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalAmberContainer
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalSurfaceElevated
import com.example.ui.theme.TacticalVoid

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModsScreen(
    state: LauncherUiState,
    strings: AppStrings,
    onSelectMod: (String) -> Unit,
    onActivateMod: () -> Unit,
    onClearActiveMod: () -> Unit,
    onCreateMod: (String) -> Unit,
    onImportModFiles: (String) -> Unit,
    onToggleBackupBeforeSync: (Boolean) -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateModDialog(
            strings = strings,
            onDismiss = { showCreateDialog = false },
            onCreate = onCreateMod
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Active Mod Status
        item {
            TacticalCard(
                borderColor = if (!state.activeModName.isNullOrBlank()) TacticalAmber.copy(alpha = 0.65f) else TacticalBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.modsTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.modsSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        enabled = state.hasFolderConfigured && !state.isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("new_mod_button")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.createModBtn)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TacticalVoid,
                    border = BorderStroke(1.dp, TacticalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = if (!state.activeModName.isNullOrBlank()) TacticalAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = strings.activeModLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.activeModName ?: strings.noModActive,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (!state.activeModName.isNullOrBlank()) TacticalAmber else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (!state.activeModName.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = onClearActiveMod,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("clear_mod_button")
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.deactivateModBtn)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Backup & Restore controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(strings.backupBeforeSyncLabel, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = strings.backupBeforeSyncHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.backupBeforeModSync,
                        onCheckedChange = onToggleBackupBeforeSync,
                        modifier = Modifier.testTag("switch_backup_before_sync")
                    )
                }

                if (state.hasBackupFiles && state.hasFolderConfigured) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRestoreBackup,
                        enabled = !state.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("restore_backup_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.restoreBackupBtn)
                    }
                }
            }
        }

        // Empty state or Mod items
        if (state.mods.isEmpty()) {
            item {
                TacticalCard(containerColor = TacticalSurfaceElevated) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = AlliedCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = strings.emptyModsTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = strings.emptyModsHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.hasFolderConfigured) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.testTag("empty_create_mod_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.createModBtn)
                            }
                        }
                    }
                }
            }
        } else {
            items(state.mods, key = { it.name }) { mod ->
                val isSelected = mod.name.equals(state.selectedModName, ignoreCase = true)
                ModItemCard(
                    mod = mod,
                    isSelected = isSelected,
                    strings = strings,
                    onClick = { onSelectMod(mod.name) },
                    onImportFiles = { onImportModFiles(mod.name) }
                )
            }

            // Apply Selected Mod Button (mirrors btnApplyMod_Click)
            item {
                Button(
                    onClick = onActivateMod,
                    enabled = state.hasFolderConfigured && !state.isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = SovietCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("apply_mod_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.activateModBtn)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModItemCard(
    mod: ModEntry,
    isSelected: Boolean,
    strings: AppStrings,
    onClick: () -> Unit,
    onImportFiles: () -> Unit
) {
    val border = when {
        mod.isActive -> TacticalAmber
        isSelected -> AlliedCyan
        else -> TacticalBorder
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) TacticalSurfaceElevated else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isSelected || mod.isActive) 1.5.dp else 1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mod_card_${mod.name}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = if (mod.isActive) TacticalAmber else AlliedCyan
                    )
                    Text(
                        text = mod.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                StatusIndicatorPill(
                    label = strings.filesCountLabel(mod.fileCount),
                    dotColor = if (mod.fileCount > 0) RadarEmerald else TacticalAmber,
                    containerColor = if (mod.isActive) TacticalAmberContainer else TacticalVoid,
                    contentColor = if (mod.isActive) TacticalAmber else MaterialTheme.colorScheme.onSurface
                )
            }

            if (mod.fileNames.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mod.fileNames.take(6).forEach { fileName ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TacticalVoid,
                            border = BorderStroke(1.dp, TacticalBorder)
                        ) {
                            Text(
                                text = fileName,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (mod.fileNames.size > 6) {
                        Text(
                            text = "+${mod.fileNames.size - 6}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AlliedCyan,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onImportFiles,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("import_mod_files_${mod.name}")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.importModFilesBtn)
                }
            }
        }
    }
}
