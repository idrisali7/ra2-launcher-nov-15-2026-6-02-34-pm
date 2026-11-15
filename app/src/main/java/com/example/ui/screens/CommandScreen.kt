package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GameEdition
import com.example.model.LauncherUiState
import com.example.model.StorageSourceMode
import com.example.ui.AppStrings
import com.example.ui.components.StatusIndicatorPill
import com.example.ui.components.TacticalCard
import com.example.ui.theme.AlliedCyan
import com.example.ui.theme.AlliedCyanContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.RadarEmeraldContainer
import com.example.ui.theme.SovietCrimson
import com.example.ui.theme.SovietCrimsonBright
import com.example.ui.theme.SovietCrimsonContainer
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalAmberContainer
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalSurfaceElevated
import com.example.ui.theme.TacticalVoid
import com.example.ui.theme.YuriViolet
import com.example.ui.theme.YuriVioletContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommandScreen(
    state: LauncherUiState,
    strings: AppStrings,
    onPickSafFolder: () -> Unit,
    onUseLocalWorkspace: () -> Unit,
    onCheckFiles: () -> Unit,
    onInitializeTemplates: () -> Unit,
    onSaveIniOnly: () -> Unit,
    onLaunchGame: (GameEdition) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Tactical Command Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(TacticalSurfaceElevated)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_command_banner_1794754536103),
                    contentDescription = strings.appTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    TacticalVoid.copy(alpha = 0.65f),
                                    TacticalVoid.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val badgeText = when (state.storageMode) {
                            StorageSourceMode.NONE -> strings.statusNoFolder
                            StorageSourceMode.DEVICE_SAF -> "${strings.safBadge}: ${state.folderDisplayName}"
                            StorageSourceMode.APP_WORKSPACE -> "${strings.workspaceBadge}: ${state.folderDisplayName}"
                        }
                        val badgeColor = when {
                            !state.hasFolderConfigured -> TacticalAmber
                            state.hasAllCoreMixFiles -> RadarEmerald
                            else -> SovietCrimsonBright
                        }
                        val badgeBg = when {
                            !state.hasFolderConfigured -> TacticalAmberContainer
                            state.hasAllCoreMixFiles -> RadarEmeraldContainer
                            else -> SovietCrimsonContainer
                        }
                        StatusIndicatorPill(
                            label = badgeText,
                            dotColor = badgeColor,
                            containerColor = badgeBg.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (state.hasFolderConfigured) {
                            IconButton(
                                onClick = onCheckFiles,
                                enabled = !state.isScanning && !state.isBusy,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("refresh_scan_button")
                            ) {
                                if (state.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = strings.checkFilesBtn,
                                        tint = AlliedCyan
                                    )
                                }
                            }
                        }
                    }

                    if (!state.hasFolderConfigured) {
                        Text(
                            text = strings.noFolderTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.noFolderSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = strings.activeFolderLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = AlliedCyan
                        )
                        Text(
                            text = state.folderDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPickSafFolder,
                            colors = ButtonDefaults.buttonColors(containerColor = SovietCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("pick_saf_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.hasFolderConfigured) strings.changeGameFolder else strings.selectGameFolder,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onUseLocalWorkspace,
                            border = BorderStroke(1.dp, AlliedCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("use_workspace_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = AlliedCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.useLocalWorkspace,
                                color = AlliedCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Integrity Status Card (mirrors CheckGameFiles() in Form1.cs)
        item {
            val statusBorder = when {
                !state.hasFolderConfigured -> TacticalBorder
                state.hasAllCoreMixFiles -> RadarEmerald.copy(alpha = 0.65f)
                else -> SovietCrimsonBright.copy(alpha = 0.7f)
            }
            val statusContainer = when {
                !state.hasFolderConfigured -> TacticalSurfaceElevated
                state.hasAllCoreMixFiles -> RadarEmeraldContainer.copy(alpha = 0.4f)
                else -> SovietCrimsonContainer.copy(alpha = 0.4f)
            }
            TacticalCard(
                borderColor = statusBorder,
                containerColor = statusContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (state.hasAllCoreMixFiles) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = when {
                            !state.hasFolderConfigured -> TacticalAmber
                            state.hasAllCoreMixFiles -> RadarEmerald
                            else -> SovietCrimsonBright
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = when {
                            !state.hasFolderConfigured -> strings.statusNoFolder
                            state.hasAllCoreMixFiles -> strings.statusReady
                            else -> strings.statusMissingFiles
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.fileStatuses.filter { it.isRequiredCore }.forEach { file ->
                        StatusIndicatorPill(
                            label = file.fileName,
                            dotColor = if (file.exists) RadarEmerald else SovietCrimsonBright,
                            containerColor = TacticalVoid.copy(alpha = 0.85f),
                            contentColor = if (file.exists) RadarEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.hasFolderConfigured) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onCheckFiles,
                            enabled = !state.isScanning && !state.isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("check_files_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.checkFilesBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = onInitializeTemplates,
                            enabled = !state.isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("init_templates_button")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.initIniTemplatesBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Quick Tactical Profile Summary
        item {
            TacticalCard {
                Text(
                    text = strings.quickOverviewTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = AlliedCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileBadge(
                        title = strings.resolutionLabel,
                        value = state.videoSettings.resolution.label
                    )
                    ProfileBadge(
                        title = strings.gameSpeedLabel,
                        value = strings.speedLabel(state.videoSettings.gameSpeed)
                    )
                    ProfileBadge(
                        title = "-win",
                        value = if (state.videoSettings.windowedMode) "ON" else "OFF"
                    )
                    ProfileBadge(
                        title = strings.activeModLabel,
                        value = state.activeModName ?: strings.noModActive,
                        highlight = !state.activeModName.isNullOrBlank()
                    )
                }

                if (state.hasFolderConfigured) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onSaveIniOnly,
                        enabled = !state.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("save_ini_quick_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.saveIniOnlyBtn)
                    }
                }
            }
        }

        // Unified Launch Deck (mirrors btnLaunchRA2 and btnLaunchYuri)
        item {
            TacticalCard {
                Text(
                    text = strings.launchDeckTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LaunchEditionButton(
                        title = strings.launchRa2Btn,
                        subtitle = if (state.videoSettings.windowedMode) "Ra2.exe -win  •  RA2.ini" else "Ra2.exe  •  RA2.ini",
                        badge = if (state.hasRa2Exe) strings.exeFound else strings.exeMissing,
                        enabled = state.hasRa2Exe && !state.isBusy,
                        accentColor = SovietCrimson,
                        containerColor = SovietCrimsonContainer,
                        testTag = "launch_ra2_button",
                        onClick = { onLaunchGame(GameEdition.RA2) }
                    )

                    LaunchEditionButton(
                        title = strings.launchYuriBtn,
                        subtitle = if (state.videoSettings.windowedMode) "Yuri.exe -win  •  RA2MD.ini" else "Yuri.exe  •  RA2MD.ini",
                        badge = if (state.hasYuriExe) strings.exeFound else strings.exeMissing,
                        enabled = state.hasYuriExe && !state.isBusy,
                        accentColor = YuriViolet,
                        containerColor = YuriVioletContainer,
                        testTag = "launch_yuri_button",
                        onClick = { onLaunchGame(GameEdition.YURI) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileBadge(
    title: String,
    value: String,
    highlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (highlight) TacticalAmberContainer.copy(alpha = 0.65f) else TacticalVoid,
        border = BorderStroke(
            width = 1.dp,
            color = if (highlight) TacticalAmber.copy(alpha = 0.6f) else TacticalBorder
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (highlight) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = TacticalAmber,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (highlight) TacticalAmber else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LaunchEditionButton(
    title: String,
    subtitle: String,
    badge: String,
    enabled: Boolean,
    accentColor: Color,
    containerColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) containerColor else TacticalSurfaceElevated.copy(alpha = 0.55f),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (enabled) accentColor else TacticalBorder.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (enabled) accentColor else TacticalBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusIndicatorPill(
                label = badge,
                dotColor = if (enabled) RadarEmerald else TacticalBorder,
                containerColor = TacticalVoid.copy(alpha = 0.8f),
                contentColor = if (enabled) RadarEmerald else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
