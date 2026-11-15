package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.GameSpeed
import com.example.model.ResolutionOption
import com.example.model.StorageSourceMode
import com.example.model.VideoSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "ra2_launcher_prefs")

data class StoredPreferences(
    val language: AppLanguage = AppLanguage.ARABIC,
    val storageMode: StorageSourceMode = StorageSourceMode.NONE,
    val treeUri: String? = null,
    val videoSettings: VideoSettings = VideoSettings(),
    val activeModName: String? = null,
    val emulatorPackage: String = "",
    val backupBeforeModSync: Boolean = true
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val STORAGE_MODE = stringPreferencesKey("storage_mode")
        val TREE_URI = stringPreferencesKey("tree_uri")
        val RESOLUTION = stringPreferencesKey("resolution")
        val GAME_SPEED = stringPreferencesKey("game_speed")
        val WINDOWED_MODE = booleanPreferencesKey("windowed_mode")
        val STRETCH_MOVIES = booleanPreferencesKey("stretch_movies")
        val ALLOW_HIRES = booleanPreferencesKey("allow_hires_modes")
        val VIDEO_BACK_BUFFER = booleanPreferencesKey("video_back_buffer")
        val ACTIVE_MOD_NAME = stringPreferencesKey("active_mod_name")
        val EMULATOR_PACKAGE = stringPreferencesKey("emulator_package")
        val BACKUP_BEFORE_MOD = booleanPreferencesKey("backup_before_mod_sync")
    }

    val preferencesFlow: Flow<StoredPreferences> = context.launcherDataStore.data.map { prefs ->
        val lang = prefs[Keys.LANGUAGE]?.let { code ->
            AppLanguage.entries.firstOrNull { it.code == code }
        } ?: AppLanguage.ARABIC

        val storageMode = prefs[Keys.STORAGE_MODE]?.let { name ->
            runCatching { StorageSourceMode.valueOf(name) }.getOrNull()
        } ?: StorageSourceMode.NONE

        val res = prefs[Keys.RESOLUTION]?.let { ResolutionOption.parse(it) }
            ?: ResolutionOption(1920, 1080)

        val speed = GameSpeed.fromIniValue(prefs[Keys.GAME_SPEED]) ?: GameSpeed.NORMAL

        StoredPreferences(
            language = lang,
            storageMode = storageMode,
            treeUri = prefs[Keys.TREE_URI],
            videoSettings = VideoSettings(
                resolution = res,
                gameSpeed = speed,
                windowedMode = prefs[Keys.WINDOWED_MODE] ?: false,
                stretchMovies = prefs[Keys.STRETCH_MOVIES] ?: true,
                allowHiResModes = prefs[Keys.ALLOW_HIRES] ?: true,
                videoBackBuffer = prefs[Keys.VIDEO_BACK_BUFFER] ?: false
            ),
            activeModName = prefs[Keys.ACTIVE_MOD_NAME],
            emulatorPackage = prefs[Keys.EMULATOR_PACKAGE] ?: "",
            backupBeforeModSync = prefs[Keys.BACKUP_BEFORE_MOD] ?: true
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.launcherDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setStorageLocation(mode: StorageSourceMode, treeUri: String?) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.STORAGE_MODE] = mode.name
            if (treeUri != null) {
                prefs[Keys.TREE_URI] = treeUri
            } else {
                prefs.remove(Keys.TREE_URI)
            }
        }
    }

    suspend fun saveVideoSettings(settings: VideoSettings) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.RESOLUTION] = settings.resolution.label
            prefs[Keys.GAME_SPEED] = settings.gameSpeed.iniValue
            prefs[Keys.WINDOWED_MODE] = settings.windowedMode
            prefs[Keys.STRETCH_MOVIES] = settings.stretchMovies
            prefs[Keys.ALLOW_HIRES] = settings.allowHiResModes
            prefs[Keys.VIDEO_BACK_BUFFER] = settings.videoBackBuffer
        }
    }

    suspend fun setActiveModName(modName: String?) {
        context.launcherDataStore.edit { prefs ->
            if (modName.isNullOrBlank()) {
                prefs.remove(Keys.ACTIVE_MOD_NAME)
            } else {
                prefs[Keys.ACTIVE_MOD_NAME] = modName
            }
        }
    }

    suspend fun setEmulatorPackage(pkg: String) {
        context.launcherDataStore.edit { it[Keys.EMULATOR_PACKAGE] = pkg.trim() }
    }

    suspend fun setBackupBeforeModSync(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.BACKUP_BEFORE_MOD] = enabled }
    }
}
