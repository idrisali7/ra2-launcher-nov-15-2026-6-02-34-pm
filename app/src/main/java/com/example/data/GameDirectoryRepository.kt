package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.model.DirectoryScanResult
import com.example.model.GameFileStatus
import com.example.model.ModEntry
import com.example.model.StorageSourceMode
import com.example.model.VideoSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class GameDirectoryRepository(private val context: Context) {

    private data class SafChild(
        val documentId: String,
        val displayName: String,
        val isDirectory: Boolean,
        val sizeBytes: Long
    )

    private val expectedFiles = listOf(
        ExpectedFileSpec("Ra2.mix", isRequiredCore = true, isExecutable = false, isConfig = false),
        ExpectedFileSpec("Lang.mix", isRequiredCore = true, isExecutable = false, isConfig = false),
        ExpectedFileSpec("Cache.mix", isRequiredCore = true, isExecutable = false, isConfig = false),
        ExpectedFileSpec("Multi.mix", isRequiredCore = true, isExecutable = false, isConfig = false),
        ExpectedFileSpec("Ra2.exe", isRequiredCore = false, isExecutable = true, isConfig = false),
        ExpectedFileSpec("Yuri.exe", isRequiredCore = false, isExecutable = true, isConfig = false),
        ExpectedFileSpec("RA2.ini", isRequiredCore = false, isExecutable = false, isConfig = true),
        ExpectedFileSpec("RA2MD.ini", isRequiredCore = false, isExecutable = false, isConfig = true)
    )

    private data class ExpectedFileSpec(
        val fileName: String,
        val isRequiredCore: Boolean,
        val isExecutable: Boolean,
        val isConfig: Boolean
    )

    private val workspaceRoot: File
        get() = File(context.filesDir, "ra2_workspace").apply { if (!exists()) mkdirs() }

    suspend fun scanDirectory(
        mode: StorageSourceMode,
        treeUriString: String?,
        activeModName: String?
    ): DirectoryScanResult = withContext(Dispatchers.IO) {
        when (mode) {
            StorageSourceMode.NONE -> DirectoryScanResult(
                folderDisplayName = "",
                files = expectedFiles.map {
                    GameFileStatus(it.fileName, it.isRequiredCore, it.isExecutable, it.isConfig, exists = false)
                },
                mods = emptyList(),
                hasBackupFiles = false,
                ra2IniContent = null,
                yuriIniContent = null
            )

            StorageSourceMode.APP_WORKSPACE -> scanLocalWorkspace(activeModName)
            StorageSourceMode.DEVICE_SAF -> {
                val uri = treeUriString?.let { Uri.parse(it) }
                    ?: error("Missing SAF directory URI")
                scanSafDirectory(uri, activeModName)
            }
        }
    }

    private fun scanLocalWorkspace(activeModName: String?): DirectoryScanResult {
        val root = workspaceRoot
        val existingFiles = root.listFiles()?.toList().orEmpty()
        val fileMap = existingFiles.filter { it.isFile }.associateBy { it.name.lowercase() }

        val statuses = expectedFiles.map { spec ->
            val found = fileMap[spec.fileName.lowercase()]
            GameFileStatus(
                fileName = spec.fileName,
                isRequiredCore = spec.isRequiredCore,
                isExecutable = spec.isExecutable,
                isConfig = spec.isConfig,
                exists = found != null,
                sizeBytes = found?.length() ?: 0L
            )
        }

        // Automatically create Mods folder if missing, matching LoadInstalledMods() in Form1.cs
        val modsDir = existingFiles.firstOrNull { it.isDirectory && it.name.equals("Mods", ignoreCase = true) }
            ?: File(root, "Mods").apply { mkdirs() }

        val modEntries = modsDir.listFiles()
            ?.filter { it.isDirectory && !it.name.equals("_Backup", ignoreCase = true) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { dir ->
                val files = dir.listFiles()?.filter { it.isFile }?.map { it.name }?.sorted().orEmpty()
                ModEntry(
                    name = dir.name,
                    documentIdOrPath = dir.absolutePath,
                    fileCount = files.size,
                    fileNames = files,
                    isActive = dir.name.equals(activeModName, ignoreCase = true)
                )
            }
            .orEmpty()

        val backupDir = modsDir.listFiles()?.firstOrNull { it.isDirectory && it.name.equals("_Backup", ignoreCase = true) }
        val hasBackups = backupDir?.listFiles()?.any { it.isFile } == true

        val ra2Ini = fileMap["ra2.ini"]?.readText()
        val yuriIni = fileMap["ra2md.ini"]?.readText()

        return DirectoryScanResult(
            folderDisplayName = "Workspace/ra2_workspace",
            files = statuses,
            mods = modEntries,
            hasBackupFiles = hasBackups,
            ra2IniContent = ra2Ini,
            yuriIniContent = yuriIni
        )
    }

    private fun scanSafDirectory(treeUri: Uri, activeModName: String?): DirectoryScanResult {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootChildren = listSafChildren(treeUri, rootDocId)
        val fileMap = rootChildren.filter { !it.isDirectory }.associateBy { it.displayName.lowercase() }

        val statuses = expectedFiles.map { spec ->
            val found = fileMap[spec.fileName.lowercase()]
            GameFileStatus(
                fileName = spec.fileName,
                isRequiredCore = spec.isRequiredCore,
                isExecutable = spec.isExecutable,
                isConfig = spec.isConfig,
                exists = found != null,
                sizeBytes = found?.sizeBytes ?: 0L
            )
        }

        var modsChild = rootChildren.firstOrNull { it.isDirectory && it.displayName.equals("Mods", ignoreCase = true) }
        if (modsChild == null) {
            val createdId = createSafDirectory(treeUri, rootDocId, "Mods")
            if (createdId != null) {
                modsChild = SafChild(createdId, "Mods", isDirectory = true, sizeBytes = 0L)
            }
        }

        var hasBackups = false
        val modEntries = if (modsChild != null) {
            val modsSubChildren = listSafChildren(treeUri, modsChild.documentId)
            val backupDir = modsSubChildren.firstOrNull { it.isDirectory && it.displayName.equals("_Backup", ignoreCase = true) }
            if (backupDir != null) {
                hasBackups = listSafChildren(treeUri, backupDir.documentId).any { !it.isDirectory }
            }
            modsSubChildren
                .filter { it.isDirectory && !it.displayName.equals("_Backup", ignoreCase = true) }
                .sortedBy { it.displayName.lowercase() }
                .map { modDir ->
                    val modFiles = listSafChildren(treeUri, modDir.documentId)
                        .filter { !it.isDirectory }
                        .map { it.displayName }
                        .sorted()
                    ModEntry(
                        name = modDir.displayName,
                        documentIdOrPath = modDir.documentId,
                        fileCount = modFiles.size,
                        fileNames = modFiles,
                        isActive = modDir.displayName.equals(activeModName, ignoreCase = true)
                    )
                }
        } else {
            emptyList()
        }

        val ra2Ini = fileMap["ra2.ini"]?.let { readSafText(treeUri, it.documentId) }
        val yuriIni = fileMap["ra2md.ini"]?.let { readSafText(treeUri, it.documentId) }

        val folderName = rootDocId.substringAfterLast(':').ifBlank { rootDocId }
        return DirectoryScanResult(
            folderDisplayName = folderName,
            files = statuses,
            mods = modEntries,
            hasBackupFiles = hasBackups,
            ra2IniContent = ra2Ini,
            yuriIniContent = yuriIni
        )
    }

    suspend fun writeIniSettings(
        mode: StorageSourceMode,
        treeUriString: String?,
        iniFileName: String,
        settings: VideoSettings
    ): String = withContext(Dispatchers.IO) {
        when (mode) {
            StorageSourceMode.NONE -> error("No game directory selected")
            StorageSourceMode.APP_WORKSPACE -> {
                val root = workspaceRoot
                val existingFile = root.listFiles()?.firstOrNull {
                    it.isFile && it.name.equals(iniFileName, ignoreCase = true)
                } ?: File(root, iniFileName)
                val existingContent = if (existingFile.exists()) existingFile.readText() else ""
                val updated = IniFileEngine.applyVideoSettings(existingContent, settings)
                existingFile.writeText(updated)
                updated
            }

            StorageSourceMode.DEVICE_SAF -> {
                val treeUri = Uri.parse(treeUriString ?: error("Missing SAF URI"))
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val children = listSafChildren(treeUri, rootDocId)
                val existing = children.firstOrNull { !it.isDirectory && it.displayName.equals(iniFileName, ignoreCase = true) }
                val existingContent = existing?.let { readSafText(treeUri, it.documentId) } ?: ""
                val updated = IniFileEngine.applyVideoSettings(existingContent, settings)
                writeSafText(treeUri, rootDocId, existing?.documentId, iniFileName, "text/plain", updated)
                updated
            }
        }
    }

    suspend fun initializeConfigTemplates(
        mode: StorageSourceMode,
        treeUriString: String?,
        settings: VideoSettings
    ) = withContext(Dispatchers.IO) {
        writeIniSettings(mode, treeUriString, "RA2.ini", settings)
        writeIniSettings(mode, treeUriString, "RA2MD.ini", settings)
        when (mode) {
            StorageSourceMode.APP_WORKSPACE -> {
                val modsDir = File(workspaceRoot, "Mods")
                if (!modsDir.exists()) modsDir.mkdirs()
            }
            StorageSourceMode.DEVICE_SAF -> {
                val treeUri = Uri.parse(treeUriString ?: return@withContext)
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val children = listSafChildren(treeUri, rootDocId)
                if (children.none { it.isDirectory && it.displayName.equals("Mods", ignoreCase = true) }) {
                    createSafDirectory(treeUri, rootDocId, "Mods")
                }
            }
            StorageSourceMode.NONE -> Unit
        }
    }

    suspend fun createModFolder(
        mode: StorageSourceMode,
        treeUriString: String?,
        modName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanName = modName.trim().replace(Regex("""[\\/:*?"<>|]"""), "_")
        if (cleanName.isEmpty() || cleanName.equals("_Backup", ignoreCase = true)) return@withContext false

        when (mode) {
            StorageSourceMode.NONE -> false
            StorageSourceMode.APP_WORKSPACE -> {
                val modsDir = File(workspaceRoot, "Mods").apply { if (!exists()) mkdirs() }
                val target = File(modsDir, cleanName)
                if (!target.exists()) target.mkdirs() else true
            }
            StorageSourceMode.DEVICE_SAF -> {
                val treeUri = Uri.parse(treeUriString ?: return@withContext false)
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootChildren = listSafChildren(treeUri, rootDocId)
                val modsId = rootChildren.firstOrNull { it.isDirectory && it.displayName.equals("Mods", ignoreCase = true) }?.documentId
                    ?: createSafDirectory(treeUri, rootDocId, "Mods")
                    ?: return@withContext false
                val existing = listSafChildren(treeUri, modsId).any { it.isDirectory && it.displayName.equals(cleanName, ignoreCase = true) }
                if (existing) true else createSafDirectory(treeUri, modsId, cleanName) != null
            }
        }
    }

    suspend fun importFilesToMod(
        mode: StorageSourceMode,
        treeUriString: String?,
        modName: String,
        sourceUris: List<Uri>
    ): Int = withContext(Dispatchers.IO) {
        if (sourceUris.isEmpty()) return@withContext 0
        var imported = 0
        when (mode) {
            StorageSourceMode.NONE -> 0
            StorageSourceMode.APP_WORKSPACE -> {
                val modDir = File(File(workspaceRoot, "Mods"), modName).apply { mkdirs() }
                sourceUris.forEach { uri ->
                    val name = resolveDisplayName(uri) ?: "mod_file_${System.currentTimeMillis()}"
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        File(modDir, name).outputStream().use { output ->
                            input.copyTo(output)
                        }
                        imported++
                    }
                }
                imported
            }
            StorageSourceMode.DEVICE_SAF -> {
                val treeUri = Uri.parse(treeUriString ?: return@withContext 0)
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val modsId = listSafChildren(treeUri, rootDocId)
                    .firstOrNull { it.isDirectory && it.displayName.equals("Mods", ignoreCase = true) }?.documentId
                    ?: createSafDirectory(treeUri, rootDocId, "Mods")
                    ?: return@withContext 0
                val modDirId = listSafChildren(treeUri, modsId)
                    .firstOrNull { it.isDirectory && it.displayName.equals(modName, ignoreCase = true) }?.documentId
                    ?: createSafDirectory(treeUri, modsId, modName)
                    ?: return@withContext 0
                val existingFiles = listSafChildren(treeUri, modDirId).filter { !it.isDirectory }
                sourceUris.forEach { srcUri ->
                    val name = resolveDisplayName(srcUri) ?: "mod_file_${System.currentTimeMillis()}"
                    val mime = context.contentResolver.getType(srcUri) ?: "application/octet-stream"
                    val existingDoc = existingFiles.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
                    context.contentResolver.openInputStream(srcUri)?.use { input ->
                        copyStreamToSaf(treeUri, modDirId, existingDoc?.documentId, name, mime, input)
                        imported++
                    }
                }
                imported
            }
        }
    }

    suspend fun syncModFiles(
        mode: StorageSourceMode,
        treeUriString: String?,
        modName: String,
        createBackup: Boolean
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        // Returns Pair(syncedFilesCount, backupFilesCount)
        when (mode) {
            StorageSourceMode.NONE -> 0 to 0
            StorageSourceMode.APP_WORKSPACE -> {
                val root = workspaceRoot
                val modsDir = File(root, "Mods")
                val modDir = modsDir.listFiles()?.firstOrNull {
                    it.isDirectory && it.name.equals(modName, ignoreCase = true)
                } ?: return@withContext 0 to 0

                val modFiles = modDir.listFiles()?.filter { it.isFile }.orEmpty()
                if (modFiles.isEmpty()) return@withContext 0 to 0

                var backupCount = 0
                val backupDir = if (createBackup) File(modsDir, "_Backup").apply { mkdirs() } else null
                var synced = 0
                modFiles.forEach { modFile ->
                    val destFile = root.listFiles()?.firstOrNull {
                        it.isFile && it.name.equals(modFile.name, ignoreCase = true)
                    } ?: File(root, modFile.name)

                    if (backupDir != null && destFile.exists()) {
                        val backupTarget = File(backupDir, destFile.name)
                        if (!backupTarget.exists()) {
                            destFile.copyTo(backupTarget, overwrite = false)
                            backupCount++
                        }
                    }
                    modFile.copyTo(destFile, overwrite = true)
                    synced++
                }
                synced to backupCount
            }

            StorageSourceMode.DEVICE_SAF -> {
                val treeUri = Uri.parse(treeUriString ?: return@withContext 0 to 0)
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootChildren = listSafChildren(treeUri, rootDocId)
                val modsDir = rootChildren.firstOrNull { it.isDirectory && it.displayName.equals("Mods", ignoreCase = true) }
                    ?: return@withContext 0 to 0
                val modsSub = listSafChildren(treeUri, modsDir.documentId)
                val modDir = modsSub.firstOrNull { it.isDirectory && it.displayName.equals(modName, ignoreCase = true) }
                    ?: return@withContext 0 to 0

                val modFiles = listSafChildren(treeUri, modDir.documentId).filter { !it.isDirectory }
                if (modFiles.isEmpty()) return@withContext 0 to 0

                var backupDirId: String? = null
                var existingBackups: List<SafChild> = emptyList()
                if (createBackup) {
                    backupDirId = modsSub.firstOrNull { it.isDirectory && it.displayName.equals("_Backup", ignoreCase = true) }?.documentId
                        ?: createSafDirectory(treeUri, modsDir.documentId, "_Backup")
                    if (backupDirId != null) {
                        existingBackups = listSafChildren(treeUri, backupDirId).filter { !it.isDirectory }
                    }
                }

                val rootFiles = rootChildren.filter { !it.isDirectory }
                var synced = 0
                var backupCount = 0
                modFiles.forEach { modFile ->
                    val existingRoot = rootFiles.firstOrNull { it.displayName.equals(modFile.displayName, ignoreCase = true) }
                    if (backupDirId != null && existingRoot != null) {
                        val alreadyBackedUp = existingBackups.any { it.displayName.equals(existingRoot.displayName, ignoreCase = true) }
                        if (!alreadyBackedUp) {
                            openSafInput(treeUri, existingRoot.documentId)?.use { srcIn ->
                                copyStreamToSaf(
                                    treeUri = treeUri,
                                    parentDocId = backupDirId,
                                    existingDocId = null,
                                    displayName = existingRoot.displayName,
                                    mimeType = "application/octet-stream",
                                    input = srcIn
                                )
                                backupCount++
                            }
                        }
                    }
                    openSafInput(treeUri, modFile.documentId)?.use { modIn ->
                        copyStreamToSaf(
                            treeUri = treeUri,
                            parentDocId = rootDocId,
                            existingDocId = existingRoot?.documentId,
                            displayName = modFile.displayName,
                            mimeType = "application/octet-stream",
                            input = modIn
                        )
                        synced++
                    }
                }
                synced to backupCount
            }
        }
    }

    suspend fun restoreVanillaBackup(
        mode: StorageSourceMode,
        treeUriString: String?
    ): Int = withContext(Dispatchers.IO) {
        when (mode) {
            StorageSourceMode.NONE -> 0
            StorageSourceMode.APP_WORKSPACE -> {
                val root = workspaceRoot
                val backupDir = File(File(root, "Mods"), "_Backup")
                if (!backupDir.exists()) return@withContext 0
                val files = backupDir.listFiles()?.filter { it.isFile }.orEmpty()
                var restored = 0
                files.forEach { bFile ->
                    val dest = File(root, bFile.name)
                    bFile.copyTo(dest, overwrite = true)
                    bFile.delete()
                    restored++
                }
                restored
            }
            StorageSourceMode.DEVICE_SAF -> {
                val treeUri = Uri.parse(treeUriString ?: return@withContext 0)
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootChildren = listSafChildren(treeUri, rootDocId)
                val modsDir = rootChildren.firstOrNull { it.isDirectory && it.displayName.equals("Mods", ignoreCase = true) }
                    ?: return@withContext 0
                val backupDir = listSafChildren(treeUri, modsDir.documentId)
                    .firstOrNull { it.isDirectory && it.displayName.equals("_Backup", ignoreCase = true) }
                    ?: return@withContext 0
                val backupFiles = listSafChildren(treeUri, backupDir.documentId).filter { !it.isDirectory }
                val rootFiles = rootChildren.filter { !it.isDirectory }
                var restored = 0
                backupFiles.forEach { bFile ->
                    val existingRoot = rootFiles.firstOrNull { it.displayName.equals(bFile.displayName, ignoreCase = true) }
                    openSafInput(treeUri, bFile.documentId)?.use { bIn ->
                        copyStreamToSaf(
                            treeUri = treeUri,
                            parentDocId = rootDocId,
                            existingDocId = existingRoot?.documentId,
                            displayName = bFile.displayName,
                            mimeType = "application/octet-stream",
                            input = bIn
                        )
                        restored++
                    }
                    runCatching {
                        val delUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, bFile.documentId)
                        DocumentsContract.deleteDocument(context.contentResolver, delUri)
                    }
                }
                restored
            }
        }
    }

    private fun listSafChildren(treeUri: Uri, parentDocId: String): List<SafChild> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        val result = mutableListOf<SafChild>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val mime = cursor.getString(mimeCol) ?: ""
                val size = if (cursor.isNull(sizeCol)) 0L else cursor.getLong(sizeCol)
                result.add(
                    SafChild(
                        documentId = id,
                        displayName = name,
                        isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                        sizeBytes = size
                    )
                )
            }
        }
        return result
    }

    private fun createSafDirectory(treeUri: Uri, parentDocId: String, name: String): String? {
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
        val createdUri = runCatching {
            DocumentsContract.createDocument(
                context.contentResolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                name
            )
        }.getOrNull() ?: return null
        return DocumentsContract.getDocumentId(createdUri)
    }

    private fun openSafInput(treeUri: Uri, docId: String): InputStream? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        return context.contentResolver.openInputStream(docUri)
    }

    private fun readSafText(treeUri: Uri, docId: String): String {
        return openSafInput(treeUri, docId)?.bufferedReader()?.use { it.readText() } ?: ""
    }

    private fun writeSafText(
        treeUri: Uri,
        parentDocId: String,
        existingDocId: String?,
        displayName: String,
        mimeType: String,
        text: String
    ) {
        text.byteInputStream().use { input ->
            copyStreamToSaf(treeUri, parentDocId, existingDocId, displayName, mimeType, input)
        }
    }

    private fun copyStreamToSaf(
        treeUri: Uri,
        parentDocId: String,
        existingDocId: String?,
        displayName: String,
        mimeType: String,
        input: InputStream
    ) {
        val targetUri: Uri = if (existingDocId != null) {
            val existingUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, existingDocId)
            val stream = runCatching { context.contentResolver.openOutputStream(existingUri, "wt") }.getOrNull()
            if (stream != null) {
                stream.use { out -> input.copyTo(out) }
                return
            }
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, existingUri) }
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
            DocumentsContract.createDocument(context.contentResolver, parentUri, mimeType, displayName)
                ?: error("Failed to recreate document $displayName")
        } else {
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
            DocumentsContract.createDocument(context.contentResolver, parentUri, mimeType, displayName)
                ?: error("Failed to create document $displayName")
        }

        val out: OutputStream = context.contentResolver.openOutputStream(targetUri, "w")
            ?: context.contentResolver.openOutputStream(targetUri)
            ?: error("Failed to open output stream for $displayName")
        out.use { input.copyTo(it) }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
