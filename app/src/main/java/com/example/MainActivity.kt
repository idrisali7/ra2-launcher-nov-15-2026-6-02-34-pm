package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.AppLanguage
import com.example.ui.AppStrings
import com.example.ui.components.LaunchReportDialog
import com.example.ui.screens.CommandScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.ModsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AlliedCyan
import com.example.ui.theme.SovietCrimson
import com.example.ui.theme.SovietCrimsonContainer
import com.example.ui.theme.RA2LauncherTheme
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalVoid
import com.example.viewmodel.LauncherViewModel

const val ROUTE_COMMAND = "command"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_MODS = "mods"
const val ROUTE_DIAGNOSTICS = "diagnostics"

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RA2LauncherTheme {
                RA2LauncherApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RA2LauncherApp(
    viewModel: LauncherViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = remember(state.language) { AppStrings.forLanguage(state.language) }
    val layoutDirection = if (state.language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingImportModName by remember { mutableStateOf<String?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onSafTreeSelected(uri)
        }
    }

    val modFilesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val targetMod = pendingImportModName
        pendingImportModName = null
        if (!targetMod.isNullOrBlank() && uris.isNotEmpty()) {
            viewModel.importFilesIntoMod(targetMod, uris)
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val tabs = remember(strings) {
        listOf(
            NavTab(
                route = ROUTE_COMMAND,
                label = strings.navCommand,
                selectedIcon = Icons.Filled.RocketLaunch,
                unselectedIcon = Icons.Outlined.RocketLaunch,
                testTag = "nav_tab_command"
            ),
            NavTab(
                route = ROUTE_SETTINGS,
                label = strings.navSettings,
                selectedIcon = Icons.Filled.Tune,
                unselectedIcon = Icons.Outlined.Tune,
                testTag = "nav_tab_settings"
            ),
            NavTab(
                route = ROUTE_MODS,
                label = strings.navMods,
                selectedIcon = Icons.Filled.Extension,
                unselectedIcon = Icons.Outlined.Extension,
                testTag = "nav_tab_mods"
            ),
            NavTab(
                route = ROUTE_DIAGNOSTICS,
                label = strings.navDiagnostics,
                selectedIcon = Icons.Filled.FactCheck,
                unselectedIcon = Icons.Outlined.FactCheck,
                testTag = "nav_tab_diagnostics"
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        if (state.latestLaunchReport != null) {
            LaunchReportDialog(
                report = state.latestLaunchReport!!,
                strings = strings,
                onCopyCommand = viewModel::copyCommand,
                onDismiss = viewModel::dismissLaunchReport
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = TacticalVoid,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = TacticalSurface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = strings.appSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AlliedCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            OutlinedButton(
                                onClick = viewModel::toggleLanguage,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("toggle_language_button")
                            ) {
                                Text(
                                    text = strings.languageToggleLabel,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = TacticalSurface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SovietCrimson,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        indicatorColor = SovietCrimsonContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = TacticalSurface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_COMMAND,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 760.dp)
                        ) {
                            composable(ROUTE_COMMAND) {
                                CommandScreen(
                                    state = state,
                                    strings = strings,
                                    onPickSafFolder = { folderPickerLauncher.launch(null) },
                                    onUseLocalWorkspace = viewModel::selectLocalWorkspace,
                                    onCheckFiles = { viewModel.scanDirectory(showCompletionToast = true) },
                                    onInitializeTemplates = viewModel::initializeConfigTemplates,
                                    onSaveIniOnly = viewModel::applySettingsToIniFiles,
                                    onLaunchGame = viewModel::launchGame
                                )
                            }
                            composable(ROUTE_SETTINGS) {
                                SettingsScreen(
                                    state = state,
                                    strings = strings,
                                    onSelectResolution = viewModel::updateResolution,
                                    onSelectSpeed = viewModel::updateGameSpeed,
                                    onToggleWindowed = viewModel::updateWindowedMode,
                                    onToggleStretchMovies = viewModel::updateStretchMovies,
                                    onToggleAllowHiRes = viewModel::updateAllowHiResModes,
                                    onToggleVideoBackBuffer = viewModel::updateVideoBackBuffer,
                                    onEmulatorPackageChange = viewModel::updateEmulatorPackage,
                                    onApplyToIniFiles = viewModel::applySettingsToIniFiles
                                )
                            }
                            composable(ROUTE_MODS) {
                                ModsScreen(
                                    state = state,
                                    strings = strings,
                                    onSelectMod = viewModel::selectMod,
                                    onActivateMod = viewModel::activateSelectedMod,
                                    onClearActiveMod = viewModel::clearActiveMod,
                                    onCreateMod = viewModel::createModFolder,
                                    onImportModFiles = { modName ->
                                        pendingImportModName = modName
                                        modFilesPickerLauncher.launch(arrayOf("*/*"))
                                    },
                                    onToggleBackupBeforeSync = viewModel::setBackupBeforeModSync,
                                    onRestoreBackup = viewModel::restoreVanillaBackup
                                )
                            }
                            composable(ROUTE_DIAGNOSTICS) {
                                DiagnosticsScreen(
                                    state = state,
                                    strings = strings,
                                    onCheckFiles = { viewModel.scanDirectory(showCompletionToast = true) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
