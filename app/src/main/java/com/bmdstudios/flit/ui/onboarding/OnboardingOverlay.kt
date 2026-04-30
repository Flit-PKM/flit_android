package com.bmdstudios.flit.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.bmdstudios.flit.R
import com.bmdstudios.flit.ui.dialog.ModelSelectionContent
import com.bmdstudios.flit.ui.settings.ModelSize
import kotlinx.coroutines.delay

private const val WELCOME_DELAY_MS = 4000L
private const val SETTINGS_SECTION_AUTO_ADVANCE_MS = 3500L
/** Coachmark vertical offset for note-detail Categories/Relationships steps (same height). */
private val NOTE_SECTION_COACHMARK_OFFSET_Y = (-200).dp

/** Which note-detail card to pulse during onboarding (not including options menu). */
enum class NoteDetailCoachSection {
    None,
    /** Main body editor card on the note screen. */
    BodyEditor,
    Categories,
    Relationships
}

private enum class OnboardingStep {
    Welcome,
    TypeOrRecord,
    ModelSelection,
    /** On Home: long-press note options (Pin / Append / Delete) on the welcome note card. */
    NoteOptionsMenu,
    MenuAndSearch,
    /** Title in app bar, body in editor, autosave. */
    NoteViewIntro,
    /** Categories section on the note screen. */
    NoteCategories,
    /** Relationships section on the note screen. */
    NoteRelationships,
    /** Categories management screen. */
    CategoriesScreen,
    SettingsGuide
}

enum class SettingsTourSection {
    Theme,
    NoteDetails,
    Model,
    DataManagement,
    Connection
}

@Composable
fun OnboardingOverlay(
    shouldSelectModel: Boolean,
    canOpenWelcomeNote: Boolean,
    onModelConfirmed: (ModelSize) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToWelcomeNote: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBottomBarHighlightChange: (Boolean) -> Unit,
    onNoteActionsHighlightChange: (Boolean) -> Unit,
    /** Pulse the note title field in the top app bar (NoteViewIntro). */
    onNoteDetailTitleHighlightChange: (Boolean) -> Unit,
    onSearchHighlightChange: (Boolean) -> Unit,
    onMenuHighlightChange: (Boolean) -> Unit,
    onCategoriesHighlightChange: (Boolean) -> Unit,
    /** When true, Home shows the welcome note with long-press options dialog (coach demo). */
    onWelcomeNoteHomeLongPressOptionsChange: (Boolean) -> Unit,
    onNoteDetailSectionHighlightChange: (NoteDetailCoachSection) -> Unit,
    onSettingsSectionHighlightChange: (SettingsTourSection?) -> Unit,
    onComplete: () -> Unit
) {
    val steps = remember(shouldSelectModel, canOpenWelcomeNote) {
        buildList {
            add(OnboardingStep.Welcome)
            add(OnboardingStep.TypeOrRecord)
            if (shouldSelectModel) add(OnboardingStep.ModelSelection)
            if (canOpenWelcomeNote) add(OnboardingStep.NoteOptionsMenu)
            add(OnboardingStep.MenuAndSearch)
            if (canOpenWelcomeNote) {
                add(OnboardingStep.NoteViewIntro)
                add(OnboardingStep.NoteCategories)
                add(OnboardingStep.NoteRelationships)
            }
            add(OnboardingStep.CategoriesScreen)
            add(OnboardingStep.SettingsGuide)
        }
    }
    var currentStepIndex by remember { mutableStateOf(0) }
    var selectedSize by remember { mutableStateOf(ModelSize.NONE) }
    val step = steps[currentStepIndex]

    fun clearNoteDetailHighlights() {
        onWelcomeNoteHomeLongPressOptionsChange(false)
        onNoteDetailTitleHighlightChange(false)
        onNoteDetailSectionHighlightChange(NoteDetailCoachSection.None)
    }

    LaunchedEffect(step) {
        when (step) {
            OnboardingStep.Welcome -> {
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                clearNoteDetailHighlights()
                onSettingsSectionHighlightChange(null)
                delay(WELCOME_DELAY_MS)
                if (currentStepIndex < steps.lastIndex) currentStepIndex += 1
            }
            OnboardingStep.TypeOrRecord -> {
                onNavigateHome()
                onBottomBarHighlightChange(true)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                clearNoteDetailHighlights()
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.NoteOptionsMenu -> {
                onNavigateHome()
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                onNoteDetailSectionHighlightChange(NoteDetailCoachSection.None)
                onWelcomeNoteHomeLongPressOptionsChange(true)
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.MenuAndSearch -> {
                onNavigateHome()
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(true)
                onMenuHighlightChange(true)
                onCategoriesHighlightChange(false)
                clearNoteDetailHighlights()
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.CategoriesScreen -> {
                onNavigateToCategories()
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(true)
                clearNoteDetailHighlights()
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.NoteViewIntro -> {
                onWelcomeNoteHomeLongPressOptionsChange(false)
                onNavigateToWelcomeNote()
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                onNoteDetailTitleHighlightChange(true)
                onNoteDetailSectionHighlightChange(NoteDetailCoachSection.BodyEditor)
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.NoteCategories -> {
                onWelcomeNoteHomeLongPressOptionsChange(false)
                onNavigateToWelcomeNote()
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                onNoteDetailTitleHighlightChange(false)
                onNoteDetailSectionHighlightChange(NoteDetailCoachSection.Categories)
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.NoteRelationships -> {
                onWelcomeNoteHomeLongPressOptionsChange(false)
                onNavigateToWelcomeNote()
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                onNoteDetailTitleHighlightChange(false)
                onNoteDetailSectionHighlightChange(NoteDetailCoachSection.Relationships)
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.SettingsGuide -> {
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                clearNoteDetailHighlights()
                onNavigateToSettings()
            }
            OnboardingStep.ModelSelection -> {
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                clearNoteDetailHighlights()
                onSettingsSectionHighlightChange(null)
            }
        }
    }

    val darkSplashGradient = Brush.verticalGradient(
        listOf(Color(0xFF000000), Color(0xFF0d274d),)
    )
    val lightSplashGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFCEA7), Color(0xFFFFFFFF))
    )
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    when (step) {
        OnboardingStep.Welcome -> {
            BrandedSplashPage(
                gradient = if (isDarkTheme) {
                    darkSplashGradient
                } else {
                    lightSplashGradient
                },
                textColor = if (isDarkTheme) Color.White else Color.Black
            )
        }
        OnboardingStep.TypeOrRecord -> {
            CoachmarkCard(
                title = "Type or record a new note",
                body = "Use the highlighted input and action on Home to capture ideas quickly. Open any note to edit its title and full text on the note screen.",
                centerContent = true,
                emphasizedTextBlock = false,
                cardOffsetY = (-120).dp,
                onSkip = onComplete
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentStepIndex < steps.lastIndex) {
                            currentStepIndex += 1
                        } else {
                            onComplete()
                        }
                    }
                ) {
                    Text("Next")
                }
            }
        }
        OnboardingStep.ModelSelection -> {
            CoachmarkCard(
                title = "Select Transcription Model",
                body = "",
                emphasizedTextBlock = false,
                onSkip = onComplete
            ) {
                ModelSelectionContent(
                    selectedSize = selectedSize,
                    onSelectedSizeChange = { selectedSize = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onModelConfirmed(selectedSize)
                        if (currentStepIndex < steps.lastIndex) {
                            currentStepIndex += 1
                        } else {
                            onComplete()
                        }
                    }
                ) {
                    Text("Continue")
                }
            }
        }
        OnboardingStep.NoteOptionsMenu -> {
            CoachmarkCard(
                title = "Note options (long-press)",
                body = "On Home, press and hold the welcome note card to open the same options you get from a long-press on any note.\n\n" +
                    "Pin — keep the note at the top of Home (tap again to unpin).\n" +
                    "Append — start a follow-up from this note.\n" +
                    "Delete — remove the note (you will confirm first).\n\n" +
                    "You can also use the (⋮) menu while viewing a note for the same actions.",
                centerContent = false,
                emphasizedTextBlock = false,
                cardOffsetY = (-84).dp,
                onSkip = onComplete
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentStepIndex < steps.lastIndex) {
                            currentStepIndex += 1
                        } else {
                            onComplete()
                        }
                    }
                ) {
                    Text("Next")
                }
            }
        }
        OnboardingStep.MenuAndSearch -> {
            CoachmarkCard(
                title = "Menu and Search",
                body = "Use Menu for Categories and Settings. Use Search to find notes quickly.",
                centerContent = true,
                emphasizedTextBlock = false,
                cardOffsetY = 108.dp,
                onSkip = onComplete
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentStepIndex < steps.lastIndex) {
                            currentStepIndex += 1
                        } else {
                            onComplete()
                        }
                    }
                ) {
                    Text("Next")
                }
            }
        }
        OnboardingStep.NoteViewIntro -> {
            CoachmarkCard(
                title = "Edit this note",
                body = "The note title is edited in the top app bar (center). The note content is edited in the main area below — type normally; markdown-style lines (headings, lists, tasks) format as you go. Changes save automatically after you pause typing.",
                centerContent = false,
                emphasizedTextBlock = false,
                cardOffsetY = (-84).dp,
                onSkip = onComplete
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentStepIndex < steps.lastIndex) {
                            currentStepIndex += 1
                        } else {
                            onComplete()
                        }
                    }
                ) {
                    Text("Next")
                }
            }
        }
        OnboardingStep.NoteCategories -> {
            CoachmarkCard(
                title = "Categories on this note",
                body = "Assign categories to organize this note. Tap a category chip to open the list of notes in that category. Use Add Category to pick one, or the × on a chip to remove it from this note only.",
                centerContent = false,
                emphasizedTextBlock = false,
                cardOffsetY = NOTE_SECTION_COACHMARK_OFFSET_Y,
                onSkip = onComplete
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentStepIndex < steps.lastIndex) {
                            currentStepIndex += 1
                        } else {
                            onComplete()
                        }
                    }
                ) {
                    Text("Next")
                }
            }
        }
        OnboardingStep.NoteRelationships -> {
            CoachmarkCard(
                title = "Relationships",
                body = "Link this note to others. Tap a relationship chip to open the related note. Use Add Relationship to create a link, or × to remove one.",
                centerContent = false,
                emphasizedTextBlock = false,
                cardOffsetY = NOTE_SECTION_COACHMARK_OFFSET_Y,
                onSkip = onComplete
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentStepIndex < steps.lastIndex) {
                            currentStepIndex += 1
                        } else {
                            onComplete()
                        }
                    }
                ) {
                    Text("Next")
                }
            }
        }
        OnboardingStep.CategoriesScreen -> {
            CoachmarkCard(
                title = "Categories list",
                body = "Here you create, rename, or delete categories for your whole library. You can also assign categories from each note’s Categories section.",
                centerContent = true,
                emphasizedTextBlock = false,
                onSkip = onComplete
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentStepIndex < steps.lastIndex) {
                            currentStepIndex += 1
                        } else {
                            onComplete()
                        }
                    }
                ) {
                    Text("Next")
                }
            }
        }
        OnboardingStep.SettingsGuide -> {
            SettingsGuideCoachmark(
                onSectionHighlightChange = onSettingsSectionHighlightChange,
                onFinish = onComplete,
                onSkip = onComplete
            )
        }
    }
}

@Composable
private fun BrandedSplashPage(
    gradient: Brush,
    textColor: Color
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        val logoSize: Dp = maxWidth.coerceIn(220.dp, 400.dp)

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Flit",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Note Taking and\nPersonal Knowledge Management",
                style = MaterialTheme.typography.headlineSmall.copy(
                    lineHeight = 34.sp
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Icon(
                painter = painterResource(id = R.drawable.flit_splash_logo),
                contentDescription = "Flit logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(logoSize)
            )
        }
    }
}

@Composable
private fun CoachmarkCard(
    title: String,
    body: String,
    centerContent: Boolean = true,
    emphasizedTextBlock: Boolean = true,
    scrimAlpha: Float = 0f,
    cardOffsetY: Dp = 0.dp,
    /** When true, coachmark card outline uses the same border pulse as other onboarding highlights. */
    pulseOutline: Boolean = false,
    onSkip: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val outlineColor = Color(0xFF7EC3FF)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC020710).copy(alpha = scrimAlpha))
            .padding(24.dp),
        contentAlignment = if (centerContent) Alignment.Center else Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            border = if (pulseOutline) null else BorderStroke(3.dp, outlineColor),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = cardOffsetY)
                .onboardingPulseHighlight(
                    enabled = pulseOutline,
                    shape = RoundedCornerShape(16.dp),
                    color = outlineColor,
                    style = OnboardingPulseStyle.BorderOnly
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onSkip != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onSkip) {
                            Text("Skip")
                        }
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (emphasizedTextBlock) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(outlineColor.copy(alpha = 0.15f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Follow the bright highlight to continue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                content()
            }
        }
    }
}

@Composable
private fun SettingsGuideCoachmark(
    onSectionHighlightChange: (SettingsTourSection?) -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val sections = remember {
        listOf(
            SettingsTourSection.Theme,
            SettingsTourSection.NoteDetails,
            SettingsTourSection.Model,
            SettingsTourSection.DataManagement,
            SettingsTourSection.Connection
        )
    }
    var sectionIndex by remember { mutableStateOf(0) }
    var autoPlay by remember { mutableStateOf(true) }
    val currentSection = sections[sectionIndex]

    LaunchedEffect(sectionIndex, autoPlay) {
        onSectionHighlightChange(currentSection)
        if (autoPlay) {
            delay(SETTINGS_SECTION_AUTO_ADVANCE_MS)
            sectionIndex = (sectionIndex + 1) % sections.size
        }
    }

    CoachmarkCard(
        title = "Settings",
        body = "Theme — Light, Dark, or System\n" +
            "Note details — Show a short markdown preview on note cards on Home\n" +
            "Model — Transcription model selection\n" +
            "Data management — Import and export notes\n" +
            "Connection — Connect to Flit Core",
        centerContent = false,
        emphasizedTextBlock = false,
        cardOffsetY = (-64).dp,
        pulseOutline = true,
        onSkip = onSkip
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onFinish
        ) {
            Text("Finish")
        }
    }
}
