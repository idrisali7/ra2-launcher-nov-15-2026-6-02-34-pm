package com.example.model

enum class AppLanguage(val code: String) {
    ARABIC("ar"),
    ENGLISH("en")
}

enum class GameEdition(val exeName: String, val iniName: String) {
    RA2("Ra2.exe", "RA2.ini"),
    YURI("Yuri.exe", "RA2MD.ini")
}

enum class GameSpeed(val index: Int, val iniValue: String) {
    VERY_SLOW(0, "4"),
    NORMAL(1, "2"),
    FAST(2, "1"),
    VERY_FAST(3, "0");

    companion object {
        fun fromIniValue(value: String?): GameSpeed? {
            return entries.firstOrNull { it.iniValue == value?.trim() }
        }
    }
}

data class ResolutionOption(
    val width: Int,
    val height: Int
) {
    val label: String get() = "${width}x${height}"

    companion object {
        val presets = listOf(
            ResolutionOption(1920, 1080),
            ResolutionOption(1600, 900),
            ResolutionOption(1366, 768),
            ResolutionOption(1280, 720),
            ResolutionOption(1024, 768),
            ResolutionOption(800, 600)
        )

        fun parse(raw: String): ResolutionOption? {
            val parts = raw.lowercase().split("x")
            if (parts.size != 2) return null
            val w = parts[0].trim().toIntOrNull() ?: return null
            val h = parts[1].trim().toIntOrNull() ?: return null
            if (w < 640 || h < 480) return null
            return ResolutionOption(w, h)
        }
    }
}

data class VideoSettings(
    val resolution: ResolutionOption = ResolutionOption(1920, 1080),
    val gameSpeed: GameSpeed = GameSpeed.NORMAL,
    val windowedMode: Boolean = false,
    val stretchMovies: Boolean = true,
    val allowHiResModes: Boolean = true,
    val videoBackBuffer: Boolean = false
)

data class GameFileStatus(
    val fileName: String,
    val isRequiredCore: Boolean,
    val isExecutable: Boolean,
    val isConfig: Boolean,
    val exists: Boolean,
    val sizeBytes: Long = 0L
)

data class ModEntry(
    val name: String,
    val documentIdOrPath: String,
    val fileCount: Int,
    val fileNames: List<String>,
    val isActive: Boolean = false
)

enum class StorageSourceMode {
    NONE,
    DEVICE_SAF,
    APP_WORKSPACE
}

data class LaunchReport(
    val edition: GameEdition,
    val commandLine: String,
    val iniFileName: String,
    val syncedModName: String?,
    val syncedFilesCount: Int,
    val backupCreatedCount: Int,
    val emulatorLaunchAttempted: Boolean,
    val emulatorLaunched: Boolean,
    val logLines: List<String>,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class DirectoryScanResult(
    val folderDisplayName: String,
    val files: List<GameFileStatus>,
    val mods: List<ModEntry>,
    val hasBackupFiles: Boolean,
    val ra2IniContent: String?,
    val yuriIniContent: String?
) {
    val hasAllRequiredMixFiles: Boolean
        get() = files.filter { it.isRequiredCore }.all { it.exists }

    val hasRa2Exe: Boolean
        get() = files.firstOrNull { it.fileName.equals("Ra2.exe", ignoreCase = true) }?.exists == true

    val hasYuriExe: Boolean
        get() = files.firstOrNull { it.fileName.equals("Yuri.exe", ignoreCase = true) }?.exists == true
}

data class LauncherUiState(
    val language: AppLanguage = AppLanguage.ARABIC,
    val storageMode: StorageSourceMode = StorageSourceMode.NONE,
    val selectedTreeUri: String? = null,
    val folderDisplayName: String = "",
    val isScanning: Boolean = false,
    val isBusy: Boolean = false,
    val fileStatuses: List<GameFileStatus> = defaultFileStatuses(),
    val mods: List<ModEntry> = emptyList(),
    val selectedModName: String? = null,
    val activeModName: String? = null,
    val backupBeforeModSync: Boolean = true,
    val hasBackupFiles: Boolean = false,
    val videoSettings: VideoSettings = VideoSettings(),
    val emulatorPackage: String = "",
    val ra2IniPreview: String = "",
    val yuriIniPreview: String = "",
    val latestLaunchReport: LaunchReport? = null,
    val launchLogs: List<String> = emptyList(),
    val snackbarMessage: String? = null
) {
    val hasFolderConfigured: Boolean
        get() = storageMode != StorageSourceMode.NONE

    val hasAllCoreMixFiles: Boolean
        get() = hasFolderConfigured && fileStatuses.filter { it.isRequiredCore }.all { it.exists }

    val hasRa2Exe: Boolean
        get() = hasFolderConfigured && fileStatuses.firstOrNull { it.fileName.equals("Ra2.exe", ignoreCase = true) }?.exists == true

    val hasYuriExe: Boolean
        get() = hasFolderConfigured && fileStatuses.firstOrNull { it.fileName.equals("Yuri.exe", ignoreCase = true) }?.exists == true

    companion object {
        fun defaultFileStatuses(): List<GameFileStatus> = listOf(
            GameFileStatus("Ra2.mix", isRequiredCore = true, isExecutable = false, isConfig = false, exists = false),
            GameFileStatus("Lang.mix", isRequiredCore = true, isExecutable = false, isConfig = false, exists = false),
            GameFileStatus("Cache.mix", isRequiredCore = true, isExecutable = false, isConfig = false, exists = false),
            GameFileStatus("Multi.mix", isRequiredCore = true, isExecutable = false, isConfig = false, exists = false),
            GameFileStatus("Ra2.exe", isRequiredCore = false, isExecutable = true, isConfig = false, exists = false),
            GameFileStatus("Yuri.exe", isRequiredCore = false, isExecutable = true, isConfig = false, exists = false),
            GameFileStatus("RA2.ini", isRequiredCore = false, isExecutable = false, isConfig = true, exists = false),
            GameFileStatus("RA2MD.ini", isRequiredCore = false, isExecutable = false, isConfig = true, exists = false)
        )
    }
}
