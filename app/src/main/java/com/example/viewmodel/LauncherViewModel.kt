package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDirectoryRepository
import com.example.data.IniFileEngine
import com.example.data.PreferencesRepository
import com.example.model.AppLanguage
import com.example.model.GameEdition
import com.example.model.GameSpeed
import com.example.model.LaunchReport
import com.example.model.LauncherUiState
import com.example.model.ResolutionOption
import com.example.model.StorageSourceMode
import com.example.model.VideoSettings
import com.example.ui.AppStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val gameRepo = GameDirectoryRepository(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        viewModelScope.launch {
            val saved = prefsRepo.preferencesFlow.first()
            _uiState.update {
                it.copy(
                    language = saved.language,
                    storageMode = saved.storageMode,
                    selectedTreeUri = saved.treeUri,
                    videoSettings = saved.videoSettings,
                    activeModName = saved.activeModName,
                    selectedModName = saved.activeModName,
                    emulatorPackage = saved.emulatorPackage,
                    backupBeforeModSync = saved.backupBeforeModSync
                )
            }
            if (saved.storageMode != StorageSourceMode.NONE) {
                scanDirectory(showCompletionToast = false, loadSettingsFromIni = true)
            } else {
                refreshPreviewsOnly()
            }
        }
    }

    private fun currentStrings(): AppStrings = AppStrings.forLanguage(_uiState.value.language)

    fun toggleLanguage() {
        val next = if (_uiState.value.language == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
        _uiState.update { it.copy(language = next) }
        viewModelScope.launch { prefsRepo.setLanguage(next) }
    }

    fun onSafTreeSelected(uri: Uri) {
        val context = getApplication<Application>()
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }
        val uriString = uri.toString()
        _uiState.update {
            it.copy(
                storageMode = StorageSourceMode.DEVICE_SAF,
                selectedTreeUri = uriString
            )
        }
        viewModelScope.launch {
            prefsRepo.setStorageLocation(StorageSourceMode.DEVICE_SAF, uriString)
            appendLog("Linked SAF folder: $uriString")
            scanDirectory(showCompletionToast = false, loadSettingsFromIni = true)
        }
    }

    fun selectLocalWorkspace() {
        _uiState.update {
            it.copy(
                storageMode = StorageSourceMode.APP_WORKSPACE,
                selectedTreeUri = null
            )
        }
        viewModelScope.launch {
            prefsRepo.setStorageLocation(StorageSourceMode.APP_WORKSPACE, null)
            appendLog("Switched to app-private workspace: filesDir/ra2_workspace")
            scanDirectory(showCompletionToast = false, loadSettingsFromIni = true)
        }
    }

    fun scanDirectory(showCompletionToast: Boolean = true, loadSettingsFromIni: Boolean = false) {
        val state = _uiState.value
        if (state.storageMode == StorageSourceMode.NONE) return

        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            runCatching {
                gameRepo.scanDirectory(
                    mode = state.storageMode,
                    treeUriString = state.selectedTreeUri,
                    activeModName = state.activeModName
                )
            }.onSuccess { scan ->
                val strings = currentStrings()
                val updatedSettings = if (loadSettingsFromIni && !scan.ra2IniContent.isNullOrBlank()) {
                    IniFileEngine.extractVideoSettings(scan.ra2IniContent, _uiState.value.videoSettings)
                } else {
                    _uiState.value.videoSettings
                }
                if (loadSettingsFromIni && !scan.ra2IniContent.isNullOrBlank()) {
                    prefsRepo.saveVideoSettings(updatedSettings)
                }

                val activeStillExists = scan.mods.any { it.name.equals(_uiState.value.activeModName, ignoreCase = true) }
                val resolvedActiveMod = if (activeStillExists) _uiState.value.activeModName else null
                if (!activeStillExists && _uiState.value.activeModName != null) {
                    prefsRepo.setActiveModName(null)
                }

                val ra2Preview = IniFileEngine.applyVideoSettings(scan.ra2IniContent ?: "", updatedSettings)
                val yuriPreview = IniFileEngine.applyVideoSettings(scan.yuriIniContent ?: "", updatedSettings)

                _uiState.update {
                    it.copy(
                        isScanning = false,
                        folderDisplayName = scan.folderDisplayName,
                        fileStatuses = scan.files,
                        mods = scan.mods,
                        activeModName = resolvedActiveMod,
                        hasBackupFiles = scan.hasBackupFiles,
                        videoSettings = updatedSettings,
                        ra2IniPreview = ra2Preview,
                        yuriIniPreview = yuriPreview,
                        snackbarMessage = if (showCompletionToast) strings.checkFilesSuccessMsg else it.snackbarMessage
                    )
                }
                appendLog("Verified folder '${scan.folderDisplayName}' — core=${scan.hasAllRequiredMixFiles}, ra2=${scan.hasRa2Exe}, yuri=${scan.hasYuriExe}, mods=${scan.mods.size}")
            }.onFailure { err ->
                val strings = currentStrings()
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        snackbarMessage = "${strings.errorPrefix}${err.localizedMessage ?: err.javaClass.simpleName}"
                    )
                }
                appendLog("Scan failed: ${err.localizedMessage}")
            }
        }
    }

    fun initializeConfigTemplates() {
        val state = _uiState.value
        if (!state.hasFolderConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            runCatching {
                gameRepo.initializeConfigTemplates(state.storageMode, state.selectedTreeUri, state.videoSettings)
            }.onSuccess {
                _uiState.update {
                    it.copy(isBusy = false, snackbarMessage = currentStrings().initIniSuccessMsg)
                }
                appendLog("Initialized RA2.ini, RA2MD.ini, and Mods/ directory")
                scanDirectory(showCompletionToast = false)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, snackbarMessage = "${currentStrings().errorPrefix}${err.localizedMessage}")
                }
            }
        }
    }

    fun updateResolution(resolution: ResolutionOption) {
        updateVideoSettings(_uiState.value.videoSettings.copy(resolution = resolution))
    }

    fun updateGameSpeed(speed: GameSpeed) {
        updateVideoSettings(_uiState.value.videoSettings.copy(gameSpeed = speed))
    }

    fun updateWindowedMode(enabled: Boolean) {
        updateVideoSettings(_uiState.value.videoSettings.copy(windowedMode = enabled))
    }

    fun updateStretchMovies(enabled: Boolean) {
        updateVideoSettings(_uiState.value.videoSettings.copy(stretchMovies = enabled))
    }

    fun updateAllowHiResModes(enabled: Boolean) {
        updateVideoSettings(_uiState.value.videoSettings.copy(allowHiResModes = enabled))
    }

    fun updateVideoBackBuffer(enabled: Boolean) {
        updateVideoSettings(_uiState.value.videoSettings.copy(videoBackBuffer = enabled))
    }

    private fun updateVideoSettings(newSettings: VideoSettings) {
        _uiState.update {
            it.copy(
                videoSettings = newSettings,
                ra2IniPreview = IniFileEngine.applyVideoSettings(it.ra2IniPreview, newSettings),
                yuriIniPreview = IniFileEngine.applyVideoSettings(it.yuriIniPreview, newSettings)
            )
        }
        viewModelScope.launch {
            prefsRepo.saveVideoSettings(newSettings)
        }
    }

    private fun refreshPreviewsOnly() {
        val settings = _uiState.value.videoSettings
        val preview = IniFileEngine.applyVideoSettings("", settings)
        _uiState.update { it.copy(ra2IniPreview = preview, yuriIniPreview = preview) }
    }

    fun updateEmulatorPackage(pkg: String) {
        _uiState.update { it.copy(emulatorPackage = pkg) }
        viewModelScope.launch { prefsRepo.setEmulatorPackage(pkg) }
    }

    fun setBackupBeforeModSync(enabled: Boolean) {
        _uiState.update { it.copy(backupBeforeModSync = enabled) }
        viewModelScope.launch { prefsRepo.setBackupBeforeModSync(enabled) }
    }

    fun applySettingsToIniFiles() {
        val state = _uiState.value
        if (!state.hasFolderConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            runCatching {
                val ra2 = gameRepo.writeIniSettings(state.storageMode, state.selectedTreeUri, "RA2.ini", state.videoSettings)
                val yuri = gameRepo.writeIniSettings(state.storageMode, state.selectedTreeUri, "RA2MD.ini", state.videoSettings)
                ra2 to yuri
            }.onSuccess { (ra2, yuri) ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        ra2IniPreview = ra2,
                        yuriIniPreview = yuri,
                        snackbarMessage = currentStrings().settingsSavedMsg
                    )
                }
                appendLog("Wrote settings (${state.videoSettings.resolution.label}, speed=${state.videoSettings.gameSpeed.iniValue}) to RA2.ini & RA2MD.ini")
                scanDirectory(showCompletionToast = false)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, snackbarMessage = "${currentStrings().errorPrefix}${err.localizedMessage}")
                }
            }
        }
    }

    fun selectMod(modName: String) {
        _uiState.update { it.copy(selectedModName = modName) }
    }

    fun activateSelectedMod() {
        val strings = currentStrings()
        val selected = _uiState.value.selectedModName
        if (selected.isNullOrBlank()) {
            _uiState.update { it.copy(snackbarMessage = strings.selectModWarning) }
            return
        }
        viewModelScope.launch {
            prefsRepo.setActiveModName(selected)
            _uiState.update { state ->
                state.copy(
                    activeModName = selected,
                    mods = state.mods.map { it.copy(isActive = it.name.equals(selected, ignoreCase = true)) },
                    snackbarMessage = strings.modActivatedMsg(selected)
                )
            }
            appendLog("Activated mod: $selected")
        }
    }

    fun clearActiveMod() {
        val strings = currentStrings()
        viewModelScope.launch {
            prefsRepo.setActiveModName(null)
            _uiState.update { state ->
                state.copy(
                    activeModName = null,
                    mods = state.mods.map { it.copy(isActive = false) },
                    snackbarMessage = strings.modDeactivatedMsg
                )
            }
            appendLog("Cleared active mod (Vanilla mode)")
        }
    }

    fun createModFolder(modName: String) {
        val state = _uiState.value
        if (!state.hasFolderConfigured || modName.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            runCatching {
                gameRepo.createModFolder(state.storageMode, state.selectedTreeUri, modName)
            }.onSuccess { created ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        selectedModName = if (created) modName.trim() else it.selectedModName
                    )
                }
                if (created) {
                    appendLog("Created mod folder: Mods/${modName.trim()}")
                    scanDirectory(showCompletionToast = false)
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, snackbarMessage = "${currentStrings().errorPrefix}${err.localizedMessage}")
                }
            }
        }
    }

    fun importFilesIntoMod(modName: String, uris: List<Uri>) {
        val state = _uiState.value
        if (!state.hasFolderConfigured || uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            runCatching {
                gameRepo.importFilesToMod(state.storageMode, state.selectedTreeUri, modName, uris)
            }.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        snackbarMessage = currentStrings().filesImportedMsg(count, modName)
                    )
                }
                appendLog("Imported $count file(s) into Mods/$modName")
                scanDirectory(showCompletionToast = false)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, snackbarMessage = "${currentStrings().errorPrefix}${err.localizedMessage}")
                }
            }
        }
    }

    fun restoreVanillaBackup() {
        val state = _uiState.value
        if (!state.hasFolderConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            runCatching {
                gameRepo.restoreVanillaBackup(state.storageMode, state.selectedTreeUri)
            }.onSuccess { restored ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        snackbarMessage = currentStrings().backupRestoredMsg(restored)
                    )
                }
                appendLog("Restored $restored file(s) from Mods/_Backup")
                scanDirectory(showCompletionToast = false)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, snackbarMessage = "${currentStrings().errorPrefix}${err.localizedMessage}")
                }
            }
        }
    }

    fun launchGame(edition: GameEdition) {
        val state = _uiState.value
        val strings = currentStrings()
        if (!state.hasFolderConfigured) return

        val exeExists = when (edition) {
            GameEdition.RA2 -> state.hasRa2Exe
            GameEdition.YURI -> state.hasYuriExe
        }
        if (!exeExists) {
            _uiState.update { it.copy(snackbarMessage = strings.missingExeError(edition.exeName)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            runCatching {
                val logs = mutableListOf<String>()
                // 1. Apply INI settings (ApplyGameSettings)
                val updatedIni = gameRepo.writeIniSettings(
                    mode = state.storageMode,
                    treeUriString = state.selectedTreeUri,
                    iniFileName = edition.iniName,
                    settings = state.videoSettings
                )
                logs.add("Wrote [Video] (${state.videoSettings.resolution.label}) & [Settings] (GameSpeed=${state.videoSettings.gameSpeed.iniValue}) to ${edition.iniName}")

                // 2. Sync active mod files (SyncModFiles)
                var syncedCount = 0
                var backupCount = 0
                val activeMod = state.activeModName
                if (!activeMod.isNullOrBlank()) {
                    val (synced, backedUp) = gameRepo.syncModFiles(
                        mode = state.storageMode,
                        treeUriString = state.selectedTreeUri,
                        modName = activeMod,
                        createBackup = state.backupBeforeModSync
                    )
                    syncedCount = synced
                    backupCount = backedUp
                    logs.add("Synced $synced file(s) from Mods/$activeMod (backed up $backedUp root files)")
                } else {
                    logs.add("No mod active — launching vanilla configuration")
                }

                // 3. Build launch command (e.g. Ra2.exe -win)
                val cmd = if (state.videoSettings.windowedMode) {
                    "${edition.exeName} -win"
                } else {
                    edition.exeName
                }
                copyToClipboard(cmd)
                logs.add("Prepared command: $cmd (copied to clipboard)")

                // 4. Launch companion Windows emulator if configured
                var emuAttempted = false
                var emuLaunched = false
                val pkg = state.emulatorPackage.trim()
                if (pkg.isNotEmpty()) {
                    emuAttempted = true
                    val context = getApplication<Application>()
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        emuLaunched = true
                        logs.add("Launched companion app: $pkg")
                    } else {
                        logs.add("Companion app '$pkg' not found on device")
                    }
                }

                LaunchReport(
                    edition = edition,
                    commandLine = cmd,
                    iniFileName = edition.iniName,
                    syncedModName = activeMod,
                    syncedFilesCount = syncedCount,
                    backupCreatedCount = backupCount,
                    emulatorLaunchAttempted = emuAttempted,
                    emulatorLaunched = emuLaunched,
                    logLines = logs
                ) to updatedIni
            }.onSuccess { (report, updatedIni) ->
                _uiState.update { stateNow ->
                    stateNow.copy(
                        isBusy = false,
                        latestLaunchReport = report,
                        ra2IniPreview = if (edition == GameEdition.RA2) updatedIni else stateNow.ra2IniPreview,
                        yuriIniPreview = if (edition == GameEdition.YURI) updatedIni else stateNow.yuriIniPreview
                    )
                }
                report.logLines.forEach { appendLog(it) }
                scanDirectory(showCompletionToast = false)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        snackbarMessage = "${strings.errorPrefix}${err.localizedMessage ?: err.javaClass.simpleName}"
                    )
                }
                appendLog("Launch failed: ${err.localizedMessage}")
            }
        }
    }

    fun copyCommand(command: String) {
        copyToClipboard(command)
        _uiState.update { it.copy(snackbarMessage = currentStrings().commandCopiedMsg) }
    }

    fun dismissLaunchReport() {
        _uiState.update { it.copy(latestLaunchReport = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun copyToClipboard(text: String) {
        val context = getApplication<Application>()
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("RA2 Launch Command", text))
    }

    private fun appendLog(entry: String) {
        val stamped = "[${timeFmt.format(Date())}] $entry"
        _uiState.update { state ->
            state.copy(launchLogs = (listOf(stamped) + state.launchLogs).take(40))
        }
    }
}
