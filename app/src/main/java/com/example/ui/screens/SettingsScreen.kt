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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.GameSpeed
import com.example.model.LauncherUiState
import com.example.model.ResolutionOption
import com.example.ui.AppStrings
import com.example.ui.components.TacticalCard
import com.example.ui.theme.AlliedCyan
import com.example.ui.theme.AlliedCyanContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.SovietCrimson
import com.example.ui.theme.SovietCrimsonContainer
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalVoid

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: LauncherUiState,
    strings: AppStrings,
    onSelectResolution: (ResolutionOption) -> Unit,
    onSelectSpeed: (GameSpeed) -> Unit,
    onToggleWindowed: (Boolean) -> Unit,
    onToggleStretchMovies: (Boolean) -> Unit,
    onToggleAllowHiRes: (Boolean) -> Unit,
    onToggleVideoBackBuffer: (Boolean) -> Unit,
    onEmulatorPackageChange: (String) -> Unit,
    onApplyToIniFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customWidth by remember(state.videoSettings.resolution.width) {
        mutableStateOf(state.videoSettings.resolution.width.toString())
    }
    var customHeight by remember(state.videoSettings.resolution.height) {
        mutableStateOf(state.videoSettings.resolution.height.toString())
    }
    var showYuriIni by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Resolution Card
        item {
            TacticalCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = AlliedCyan)
                    Text(
                        text = strings.resolutionLabel,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResolutionOption.presets.forEach { preset ->
                        val selected = state.videoSettings.resolution == preset
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectResolution(preset) },
                            label = {
                                Text(
                                    text = preset.label,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SovietCrimsonContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("resolution_chip_${preset.label}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.customResolutionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customWidth,
                        onValueChange = { customWidth = it.filter { ch -> ch.isDigit() } },
                        label = { Text(strings.widthLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_width_input")
                    )
                    OutlinedTextField(
                        value = customHeight,
                        onValueChange = { customHeight = it.filter { ch -> ch.isDigit() } },
                        label = { Text(strings.heightLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_height_input")
                    )
                    OutlinedButton(
                        onClick = {
                            val w = customWidth.toIntOrNull()
                            val h = customHeight.toIntOrNull()
                            if (w != null && h != null && w >= 640 && h >= 480) {
                                onSelectResolution(ResolutionOption(w, h))
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("apply_custom_res_button")
                    ) {
                        Text(strings.applyCustomResBtn)
                    }
                }
            }
        }

        // GameSpeed Card
        item {
            TacticalCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = AlliedCyan)
                    Text(
                        text = strings.gameSpeedLabel,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameSpeed.entries.forEach { speed ->
                        val selected = state.videoSettings.gameSpeed == speed
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectSpeed(speed) },
                            label = {
                                Text("${strings.speedLabel(speed)} (${speed.iniValue})")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AlliedCyanContainer,
                                selectedLabelColor = AlliedCyan
                            ),
                            modifier = Modifier.testTag("speed_chip_${speed.iniValue}")
                        )
                    }
                }
            }
        }

        // Windowed & Video Flags Card
        item {
            TacticalCard {
                Text(
                    text = strings.videoIniFlagsTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))

                ToggleSettingRow(
                    title = strings.windowedModeLabel,
                    subtitle = strings.windowedModeHint,
                    checked = state.videoSettings.windowedMode,
                    testTag = "switch_windowed_mode",
                    onCheckedChange = onToggleWindowed
                )
                ToggleSettingRow(
                    title = strings.stretchMoviesLabel,
                    subtitle = "StretchMovies=${if (state.videoSettings.stretchMovies) "yes" else "no"}",
                    checked = state.videoSettings.stretchMovies,
                    testTag = "switch_stretch_movies",
                    onCheckedChange = onToggleStretchMovies
                )
                ToggleSettingRow(
                    title = strings.allowHiResLabel,
                    subtitle = "AllowHiResModes=${if (state.videoSettings.allowHiResModes) "yes" else "no"}",
                    checked = state.videoSettings.allowHiResModes,
                    testTag = "switch_allow_hires",
                    onCheckedChange = onToggleAllowHiRes
                )
                ToggleSettingRow(
                    title = strings.videoBackBufferLabel,
                    subtitle = "VideoBackBuffer=${if (state.videoSettings.videoBackBuffer) "yes" else "no"}",
                    checked = state.videoSettings.videoBackBuffer,
                    testTag = "switch_video_backbuffer",
                    onCheckedChange = onToggleVideoBackBuffer
                )
            }
        }

        // Companion Emulator Bridge Card
        item {
            TacticalCard {
                Text(
                    text = strings.emulatorBridgeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = AlliedCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.emulatorPackage,
                    onValueChange = onEmulatorPackageChange,
                    singleLine = true,
                    label = { Text(strings.emulatorPackageLabel) },
                    supportingText = { Text(strings.emulatorPackageHint) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emulator_package_input")
                )
            }
        }

        // Live INI Output Preview & Write Button
        item {
            TacticalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = RadarEmerald)
                        Text(
                            text = strings.iniPreviewTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !showYuriIni,
                            onClick = { showYuriIni = false },
                            label = { Text("RA2.ini", fontFamily = JetBrainsMonoFontFamily) },
                            modifier = Modifier.testTag("preview_ra2_ini_chip")
                        )
                        FilterChip(
                            selected = showYuriIni,
                            onClick = { showYuriIni = true },
                            label = { Text("RA2MD.ini", fontFamily = JetBrainsMonoFontFamily) },
                            modifier = Modifier.testTag("preview_yuri_ini_chip")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TacticalVoid,
                    border = BorderStroke(1.dp, TacticalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val content = if (showYuriIni) state.yuriIniPreview else state.ra2IniPreview
                    Text(
                        text = content.ifBlank { "[Video]\nScreenWidth=${state.videoSettings.resolution.width}\nScreenHeight=${state.videoSettings.resolution.height}" },
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = RadarEmerald,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onApplyToIniFiles,
                    enabled = state.hasFolderConfigured && !state.isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = SovietCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("write_ini_files_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.applyToIniFilesBtn)
                }
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
