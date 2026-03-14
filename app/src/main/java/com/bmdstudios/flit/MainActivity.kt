package com.bmdstudios.flit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.bmdstudios.flit.ui.dialog.ModelSelectionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bmdstudios.flit.data.database.NotesearchRebuilder
import com.bmdstudios.flit.data.database.PurgeDeletedRunner
import com.bmdstudios.flit.data.repository.SettingsRepository
import com.bmdstudios.flit.ui.component.BottomBar
import com.bmdstudios.flit.ui.component.navigation.HomeButton
import com.bmdstudios.flit.ui.component.navigation.NavigationIcon
import com.bmdstudios.flit.ui.component.navigation.SearchButton
import com.bmdstudios.flit.ui.component.TopBarTitle
import com.bmdstudios.flit.ui.dialog.SearchDialog
import com.bmdstudios.flit.ui.navigation.Screen
import com.bmdstudios.flit.ui.screen.CategoriesScreen
import com.bmdstudios.flit.ui.settings.ModelSize
import com.bmdstudios.flit.ui.screen.HomeScreen
import com.bmdstudios.flit.ui.screen.NoteDetailScreen
import com.bmdstudios.flit.ui.screen.NoteEditScreen
import com.bmdstudios.flit.ui.screen.NotesByCategoryScreen
import com.bmdstudios.flit.ui.screen.SearchResultsScreen
import com.bmdstudios.flit.ui.screen.SettingsScreen
import com.bmdstudios.flit.ui.theme.FlitTheme
import com.bmdstudios.flit.ui.theme.ThemeMode
import com.bmdstudios.flit.ui.viewmodel.ModelDownloadViewModel
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import com.bmdstudios.flit.ui.viewmodel.SettingsViewModel
import com.bmdstudios.flit.ui.viewmodel.TranscriptionViewModel
import com.bmdstudios.flit.ui.viewmodel.VoiceRecorderViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "MainActivity"

/**
 * Main activity of the application.
 * Coordinates UI composition and ViewModel interactions.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var purgeDeletedRunner: PurgeDeletedRunner

    @Inject
    lateinit var notesearchRebuilder: NotesearchRebuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).d("onCreate called")
        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.IO) {
            purgeDeletedRunner.purge()
            notesearchRebuilder.rebuildIfNeeded()
        }

        setContent {
            val themeMode by settingsRepository.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            var showModelSelectionDialog by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val modelDownloadViewModel: ModelDownloadViewModel = viewModel()

            // Check if this is first startup (no model size preference set)
            LaunchedEffect(Unit) {
                val hasPreference = settingsRepository.hasModelSizePreference()
                if (!hasPreference) {
                    showModelSelectionDialog = true
                }
            }

            FlitTheme(themeMode = themeMode) {
                MainContent(
                    activity = this,
                    modelDownloadViewModel = modelDownloadViewModel
                )
                
                if (showModelSelectionDialog) {
                    ModelSelectionDialog(
                        onConfirm = { selectedSize ->
                            coroutineScope.launch {
                                settingsRepository.setModelSize(selectedSize)
                                showModelSelectionDialog = false
                                // Trigger model download after setting model size
                                modelDownloadViewModel.downloadModels(this@MainActivity)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("onDestroy called")
        super.onDestroy()
    }
}

/**
 * Main content composable with top bar and bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    activity: ComponentActivity,
    modelDownloadViewModel: ModelDownloadViewModel
) {
    val navController = rememberNavController()
    val uiState by modelDownloadViewModel.uiState.collectAsStateWithLifecycle()

    // Trigger download on start
    LaunchedEffect(Unit) {
        Timber.tag(TAG).i("Triggering model download on app start")
        modelDownloadViewModel.downloadModels(activity)
    }

    val voiceRecorderViewModel: VoiceRecorderViewModel = viewModel()
    val transcriptionViewModel: TranscriptionViewModel = viewModel()
    val notesViewModel: NotesViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    
    val modelSize by settingsViewModel.modelSize.collectAsStateWithLifecycle(initialValue = ModelSize.NONE)
    val noteDetails by settingsViewModel.noteDetails.collectAsStateWithLifecycle(initialValue = false)

    var showSearchDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topBarTitle = TopBarTitle(navBackStackEntry, notesViewModel)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(topBarTitle) },
                navigationIcon = {
                    NavigationIcon(
                        navController = navController,
                        currentRoute = currentRoute
                    )
                },
                actions = {
                    if (currentRoute == Screen.Home.route) {
                        SearchButton(onClick = { showSearchDialog = true })
                    }
                    if (currentRoute == Screen.Settings.route || currentRoute == Screen.Categories.route) {
                        HomeButton(navController = navController)
                    }
                    if ((currentRoute?.startsWith("note/") == true && currentRoute?.endsWith("/edit") != true) ||
                        currentRoute?.startsWith("notes/category/") == true ||
                        currentRoute?.startsWith("search/") == true) {
                        HomeButton(navController = navController)
                    }
                }
            )
        },
        bottomBar = {
            // Only show bottom bar on home screen
            if (currentRoute == Screen.Home.route) {
                BottomBar(
                    modelDownloadState = uiState,
                    modelSize = modelSize,
                    voiceRecorderViewModel = voiceRecorderViewModel,
                    transcriptionViewModel = transcriptionViewModel,
                    notesViewModel = notesViewModel
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    modelDownloadState = uiState,
                    notesViewModel = notesViewModel,
                    navController = navController,
                    noteDetailsEnabled = noteDetails
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    modelDownloadViewModel = modelDownloadViewModel
                )
            }
            composable(Screen.Categories.route) {
                CategoriesScreen()
            }
            composable(
                route = Screen.NoteDetail.ROUTE,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                NoteDetailScreen(
                    noteId = noteId,
                    notesViewModel = notesViewModel,
                    navController = navController
                )
            }
            composable(
                route = Screen.NoteEdit.ROUTE,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                NoteEditScreen(
                    noteId = noteId,
                    notesViewModel = notesViewModel,
                    navController = navController
                )
            }
            composable(
                route = Screen.NotesByCategory.ROUTE,
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
                NotesByCategoryScreen(
                    categoryId = categoryId,
                    notesViewModel = notesViewModel,
                    navController = navController
                )
            }
            composable(
                route = Screen.SearchResults.ROUTE,
                arguments = listOf(
                    navArgument("query") {
                        type = NavType.StringType
                    },
                    navArgument("categoryId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val encodedQuery = backStackEntry.arguments?.getString("query") ?: ""
                val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: -1L
                // Note: query will be decoded in SearchResultsScreen
                SearchResultsScreen(
                    query = encodedQuery,
                    categoryId = categoryId,
                    notesViewModel = notesViewModel,
                    navController = navController
                )
            }
        }
    }

    // Search dialog
    if (showSearchDialog) {
        SearchDialog(
            onDismiss = { showSearchDialog = false },
            notesViewModel = notesViewModel,
            navController = navController
        )
    }
}
