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

private enum class OnboardingStep {
    Welcome,
    TypeOrRecord,
    ModelSelection,
    HomeActions,
    MenuAndSearch,
    Categories,
    NoteView,
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
    onSearchHighlightChange: (Boolean) -> Unit,
    onMenuHighlightChange: (Boolean) -> Unit,
    onCategoriesHighlightChange: (Boolean) -> Unit,
    onSettingsSectionHighlightChange: (SettingsTourSection?) -> Unit,
    onComplete: () -> Unit
) {
    val steps = remember(shouldSelectModel) {
        buildList {
            add(OnboardingStep.Welcome)
            add(OnboardingStep.TypeOrRecord)
            if (shouldSelectModel) add(OnboardingStep.ModelSelection)
            add(OnboardingStep.HomeActions)
            add(OnboardingStep.MenuAndSearch)
            add(OnboardingStep.Categories)
            if (canOpenWelcomeNote) add(OnboardingStep.NoteView)
            add(OnboardingStep.SettingsGuide)
        }
    }
    var currentStepIndex by remember { mutableStateOf(0) }
    var selectedSize by remember { mutableStateOf(ModelSize.NONE) }
    val step = steps[currentStepIndex]

    LaunchedEffect(step) {
        when (step) {
            OnboardingStep.Welcome -> {
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
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
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.HomeActions -> {
                onNavigateHome()
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(true)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.MenuAndSearch -> {
                onNavigateHome()
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(true)
                onMenuHighlightChange(true)
                onCategoriesHighlightChange(false)
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.Categories -> {
                onNavigateToCategories()
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(true)
                onSettingsSectionHighlightChange(null)
            }
            OnboardingStep.NoteView -> {
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                onSettingsSectionHighlightChange(null)
                onNavigateToWelcomeNote()
            }
            OnboardingStep.SettingsGuide -> {
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                onNavigateToSettings()
            }
            OnboardingStep.ModelSelection -> {
                onBottomBarHighlightChange(false)
                onNoteActionsHighlightChange(false)
                onSearchHighlightChange(false)
                onMenuHighlightChange(false)
                onCategoriesHighlightChange(false)
                onSettingsSectionHighlightChange(null)
            }
        }
    }

    val darkSplashGradient = Brush.verticalGradient(
        listOf(Color(0xFF000000), Color(0xFF0057D8),)
    )
    val lightSplashGradient = Brush.verticalGradient(
        listOf(Color(0xFFFF7A00), Color(0xFFFFC26A))
    )
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    when (step) {
        OnboardingStep.Welcome -> {
            BrandedSplashPage(
                gradient = if (isDarkTheme) {
                    darkSplashGradient
                } else {
                    lightSplashGradient
                }
            )
        }
        OnboardingStep.TypeOrRecord -> {
            CoachmarkCard(
                title = "Type or record a new note",
                body = "Use the highlighted input box and action button to quickly capture ideas.",
                centerContent = true,
                emphasizedTextBlock = false,
                cardOffsetY = (-120).dp
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
                emphasizedTextBlock = false
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
        OnboardingStep.HomeActions -> {
            CoachmarkCard(
                title = "Note Actions",
                body = "Append - New Follow-Up Note\nEdit - Edit Note\nDelete - Delete Note",
                centerContent = false,
                emphasizedTextBlock = false,
                cardOffsetY = (-84).dp
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
                body = "Use Menu for Categories/Settings and Search to find notes quickly.",
                centerContent = true,
                emphasizedTextBlock = false,
                cardOffsetY = 108.dp
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
        OnboardingStep.NoteView -> {
            CoachmarkCard(
                title = "Welcome guide note",
                body = "Opening your welcome note now. It contains detailed usage instructions and a running What's new log."
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
        OnboardingStep.Categories -> {
            CoachmarkCard(
                title = "Categories",
                body = "Create, Edit or Delete categories here to organize your notes.",
                centerContent = true,
                emphasizedTextBlock = false
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
                onFinish = onComplete
            )
        }
    }
}

@Composable
private fun GradientMessagePage(
    gradient: Brush,
    title: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun BrandedSplashPage(
    gradient: Brush
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
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Note Taking and\nPersonal Knowledge Management",
                style = MaterialTheme.typography.headlineSmall.copy(
                    lineHeight = 34.sp
                ),
                color = Color.White,
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
            border = BorderStroke(3.dp, outlineColor),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = cardOffsetY)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
    onFinish: () -> Unit
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
        body = "Theme - Light/Dark/System\nNote Details - Display note preview\nModel - Transcription Model Selection\nData Management - Import & Export Notes\nConnection - Connect to Flit - Core",
        centerContent = false,
        emphasizedTextBlock = false,
        cardOffsetY = (-64).dp
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onFinish
        ) {
            Text("Finish")
        }
    }
}
