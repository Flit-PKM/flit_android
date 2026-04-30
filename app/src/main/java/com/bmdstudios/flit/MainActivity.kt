package com.bmdstudios.flit

import android.os.Bundle
import androidx.annotation.RawRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.data.database.NotesearchRebuilder
import com.bmdstudios.flit.data.database.PurgeDeletedRunner
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.dao.NotesearchDao
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.data.database.entity.NoteCategoryCrossRef
import com.bmdstudios.flit.data.database.entity.NoteSearchEntity
import com.bmdstudios.flit.data.database.model.NoteStatus
import com.bmdstudios.flit.data.repository.SettingsRepository
import com.bmdstudios.flit.data.search.SearchNormalizer
import com.bmdstudios.flit.ui.component.BottomBar
import com.bmdstudios.flit.ui.component.navigation.HomeButton
import com.bmdstudios.flit.ui.component.navigation.NavigationIcon
import com.bmdstudios.flit.ui.component.navigation.SearchButton
import com.bmdstudios.flit.ui.component.TopBarTitle
import com.bmdstudios.flit.ui.dialog.SearchDialog
import com.bmdstudios.flit.ui.navigation.Screen
import com.bmdstudios.flit.ui.onboarding.NoteDetailCoachSection
import com.bmdstudios.flit.ui.onboarding.OnboardingOverlay
import com.bmdstudios.flit.ui.onboarding.OnboardingPulseStyle
import com.bmdstudios.flit.ui.onboarding.SettingsTourSection
import com.bmdstudios.flit.ui.onboarding.onboardingPulseHighlight
import com.bmdstudios.flit.ui.screen.CategoriesScreen
import com.bmdstudios.flit.ui.settings.ModelSize
import com.bmdstudios.flit.ui.screen.HomeScreen
import com.bmdstudios.flit.ui.screen.NoteDetailScreen
import com.bmdstudios.flit.ui.screen.NoteDetailNavigationAction
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
import kotlinx.coroutines.delay

private const val TAG = "MainActivity"
private const val WELCOME_NOTE_ID = 0L
private const val EXAMPLE_CATEGORY_NAME = "Example"
private const val TITLE_AUTO_SAVE_DELAY_MS = 500L

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

    @Inject
    lateinit var noteDao: NoteDao

    @Inject
    lateinit var notesearchDao: NotesearchDao

    @Inject
    lateinit var categoryDao: CategoryDao

    @Inject
    lateinit var noteCategoryDao: NoteCategoryDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).d("onCreate called")
        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.IO) {
            purgeDeletedRunner.purge()
            notesearchRebuilder.rebuildIfNeeded()
            ensureWelcomeNoteSeeded()
        }

        setContent {
            val themeMode by settingsRepository.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            var showOnboarding by remember { mutableStateOf(false) }
            var showModelSelectionDialog by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val modelDownloadViewModel: ModelDownloadViewModel = viewModel()

            LaunchedEffect(Unit) {
                showOnboarding = settingsRepository.shouldShowOnboarding()
                if (!showOnboarding && !settingsRepository.hasModelSizePreference()) {
                    showModelSelectionDialog = true
                }
            }

            FlitTheme(themeMode = themeMode) {
                MainContent(
                    activity = this,
                    modelDownloadViewModel = modelDownloadViewModel,
                    showOnboarding = showOnboarding,
                    onOnboardingComplete = {
                        coroutineScope.launch {
                            settingsRepository.setOnboardingRevisionCompleted(AppConfig.ONBOARDING_REVISION)
                            showOnboarding = false
                        }
                    },
                    onOnboardingModelSelected = { selectedSize ->
                        coroutineScope.launch {
                            settingsRepository.setModelSize(selectedSize)
                            modelDownloadViewModel.downloadModels(this@MainActivity)
                        }
                    }
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

    private suspend fun ensureWelcomeNoteSeeded() {
        if (settingsRepository.hasAttemptedInstallWelcomeSeed()) {
            return
        }

        val totalNotes = noteDao.getTotalNoteCount()
        if (totalNotes == 0) {
            val markdown = loadRawResourceText(R.raw.welcome_note)
            val now = System.currentTimeMillis()
            noteDao.insertWelcomeNoteWithId(
                id = WELCOME_NOTE_ID,
                ver = 1,
                title = "Welcome to Flit",
                text = markdown,
                createdAt = now,
                updatedAt = now,
                workflowStatus = NoteStatus.DRAFT.name
            )
            notesearchDao.upsert(
                NoteSearchEntity(
                    noteId = WELCOME_NOTE_ID,
                    content = SearchNormalizer.normalize("Welcome to Flit $markdown")
                )
            )

            val exampleCategoryId = categoryDao.getCategoryByName(EXAMPLE_CATEGORY_NAME)?.id
                ?: categoryDao.insertCategory(
                    CategoryEntity(
                        name = EXAMPLE_CATEGORY_NAME,
                        created_at = now,
                        updated_at = now
                    )
                )
            noteCategoryDao.insertNoteCategory(
                NoteCategoryCrossRef(
                    note_id = WELCOME_NOTE_ID,
                    category_id = exampleCategoryId,
                    updated_at = now
                )
            )
        }
        settingsRepository.setInstallWelcomeSeedAttempted(true)
    }

    private fun loadRawResourceText(@RawRes resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }
}

/**
 * Main content composable with top bar and bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    activity: ComponentActivity,
    modelDownloadViewModel: ModelDownloadViewModel,
    showOnboarding: Boolean,
    onOnboardingComplete: () -> Unit,
    onOnboardingModelSelected: (ModelSize) -> Unit
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
    val notes by notesViewModel.notes.collectAsStateWithLifecycle(initialValue = emptyList())

    var showSearchDialog by remember { mutableStateOf(false) }
    var highlightBottomBar by remember { mutableStateOf(false) }
    var highlightNoteActions by remember { mutableStateOf(false) }
    var highlightSearchButton by remember { mutableStateOf(false) }
    var highlightMenuButton by remember { mutableStateOf(false) }
    var highlightCategoryActions by remember { mutableStateOf(false) }
    var welcomeNoteLongPressOnboarding by remember { mutableStateOf(false) }
    var highlightNoteDetailTitle by remember { mutableStateOf(false) }
    var noteDetailOnboardingSection by remember { mutableStateOf(NoteDetailCoachSection.None) }
    var highlightedSettingsSection by remember { mutableStateOf<SettingsTourSection?>(null) }
    var pendingNoteDetailNavigationAction by remember { mutableStateOf<NoteDetailNavigationAction?>(null) }
    var showNoteOptionsMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topBarTitle = TopBarTitle(navBackStackEntry, notesViewModel)
    val isNoteDetailRoute = currentRoute?.startsWith("note/") == true
    val activeNoteId = if (isNoteDetailRoute) navBackStackEntry?.arguments?.getLong("noteId") else null
    val activeNote = if (activeNoteId != null) {
        val noteState = notesViewModel.noteDao.getNoteByIdFlow(activeNoteId)
            .collectAsStateWithLifecycle(initialValue = null)
        noteState.value
    } else {
        null
    }
    var topBarTitleDraft by remember(activeNoteId) { mutableStateOf("") }
    var topBarLastSavedTitle by remember(activeNoteId) { mutableStateOf("") }

    LaunchedEffect(activeNote?.id) {
        if (activeNote != null) {
            topBarTitleDraft = activeNote.title
            topBarLastSavedTitle = activeNote.title
        }
    }

    LaunchedEffect(activeNote?.id, topBarTitleDraft) {
        val currentNote = activeNote ?: return@LaunchedEffect
        if (topBarTitleDraft.isBlank()) return@LaunchedEffect
        if (topBarTitleDraft == topBarLastSavedTitle) return@LaunchedEffect

        delay(TITLE_AUTO_SAVE_DELAY_MS)
        if (topBarTitleDraft == topBarLastSavedTitle) return@LaunchedEffect

        val updatedNote = currentNote.copy(
            title = topBarTitleDraft.trim(),
            updated_at = System.currentTimeMillis(),
            ver = currentNote.ver + 1
        )
        notesViewModel.updateNote(updatedNote)
        notesViewModel.scheduleSyncAfterMutation()
        topBarLastSavedTitle = updatedNote.title
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (activeNote != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .onboardingPulseHighlight(
                                        enabled = highlightNoteDetailTitle,
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        style = OnboardingPulseStyle.BorderOnly
                                    )
                            ) {
                                BasicTextField(
                                    value = topBarTitleDraft,
                                    onValueChange = { topBarTitleDraft = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        } else {
                            Text(topBarTitle)
                        }
                    },
                    navigationIcon = {
                        NavigationIcon(
                            navController = navController,
                            currentRoute = currentRoute,
                            highlightMenu = highlightMenuButton,
                            onBackClick = if (isNoteDetailRoute) {
                                { pendingNoteDetailNavigationAction = NoteDetailNavigationAction.BACK }
                            } else {
                                null
                            }
                        )
                    },
                    actions = {
                        if (currentRoute == Screen.Home.route) {
                            SearchButton(
                                onClick = { showSearchDialog = true },
                                highlighted = highlightSearchButton
                            )
                        }
                        if (isNoteDetailRoute && activeNote != null) {
                            Box {
                                IconButton(onClick = { showNoteOptionsMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Note options"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showNoteOptionsMenu,
                                    onDismissRequest = { showNoteOptionsMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (activeNote.pinned) "Unpin" else "Pin") },
                                        onClick = {
                                            showNoteOptionsMenu = false
                                            coroutineScope.launch(Dispatchers.IO) {
                                                notesViewModel.setNotePinned(activeNote.id, !activeNote.pinned)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Append") },
                                        onClick = {
                                            showNoteOptionsMenu = false
                                            pendingNoteDetailNavigationAction = NoteDetailNavigationAction.APPEND
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            showNoteOptionsMenu = false
                                            pendingNoteDetailNavigationAction = NoteDetailNavigationAction.DELETE
                                        }
                                    )
                                }
                            }
                        }
                        if (currentRoute == Screen.Settings.route || currentRoute == Screen.Categories.route) {
                            HomeButton(navController = navController)
                        }
                        if ((currentRoute?.startsWith("note/") == true && !currentRoute.endsWith("/edit")) ||
                            currentRoute?.startsWith("notes/category/") == true ||
                            currentRoute?.startsWith("search/") == true) {
                            HomeButton(
                                navController = navController,
                                onClick = if (isNoteDetailRoute) {
                                    { pendingNoteDetailNavigationAction = NoteDetailNavigationAction.HOME }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (currentRoute == Screen.Home.route) {
                    BottomBar(
                        modelDownloadState = uiState,
                        modelSize = modelSize,
                        voiceRecorderViewModel = voiceRecorderViewModel,
                        transcriptionViewModel = transcriptionViewModel,
                        notesViewModel = notesViewModel,
                        highlightInputAndAction = highlightBottomBar
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
                        noteDetailsEnabled = noteDetails,
                        highlightCoachMarks = highlightNoteActions,
                        welcomeNoteId = WELCOME_NOTE_ID,
                        welcomeNoteLongPressOnboarding = welcomeNoteLongPressOnboarding
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        modelDownloadViewModel = modelDownloadViewModel,
                        highlightConnectionSection = showOnboarding,
                        highlightedTourSection = highlightedSettingsSection
                    )
                }
                composable(Screen.Categories.route) {
                    CategoriesScreen(highlightCategoryActions = highlightCategoryActions)
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
                        navController = navController,
                        titleText = topBarTitleDraft,
                        pendingNavigationAction = pendingNoteDetailNavigationAction,
                        onNavigationActionConsumed = { pendingNoteDetailNavigationAction = null },
                        onboardingSectionHighlight = noteDetailOnboardingSection
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
                    SearchResultsScreen(
                        query = encodedQuery,
                        categoryId = categoryId,
                        notesViewModel = notesViewModel,
                        navController = navController
                    )
                }
            }
        }

        if (showOnboarding) {
            OnboardingOverlay(
                shouldSelectModel = true,
                canOpenWelcomeNote = notes.any { it.id == WELCOME_NOTE_ID },
                onModelConfirmed = onOnboardingModelSelected,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToWelcomeNote = {
                    navController.navigate(Screen.NoteDetail.createRoute(WELCOME_NOTE_ID)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onBottomBarHighlightChange = { highlightBottomBar = it },
                onNoteActionsHighlightChange = { highlightNoteActions = it },
                onNoteDetailTitleHighlightChange = { highlightNoteDetailTitle = it },
                onSearchHighlightChange = { highlightSearchButton = it },
                onMenuHighlightChange = { highlightMenuButton = it },
                onCategoriesHighlightChange = { highlightCategoryActions = it },
                onWelcomeNoteHomeLongPressOptionsChange = { welcomeNoteLongPressOnboarding = it },
                onNoteDetailSectionHighlightChange = { noteDetailOnboardingSection = it },
                onSettingsSectionHighlightChange = { highlightedSettingsSection = it },
                onComplete = {
                    highlightBottomBar = false
                    highlightNoteActions = false
                    highlightSearchButton = false
                    highlightMenuButton = false
                    highlightCategoryActions = false
                    welcomeNoteLongPressOnboarding = false
                    highlightNoteDetailTitle = false
                    noteDetailOnboardingSection = NoteDetailCoachSection.None
                    highlightedSettingsSection = null
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                    onOnboardingComplete()
                }
            )
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
