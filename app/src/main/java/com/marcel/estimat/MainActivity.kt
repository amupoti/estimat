package com.marcel.estimat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marcel.estimat.data.QuestionRepository
import com.marcel.estimat.data.SettingsRepository
import com.marcel.estimat.model.QuestionBank
import com.marcel.estimat.model.QuestionFilter
import com.marcel.estimat.model.ThemePreference
import com.marcel.estimat.ui.AppUiState
import com.marcel.estimat.ui.AppViewModel
import com.marcel.estimat.ui.AppViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(
            repository = SettingsRepository(applicationContext),
            questionRepository = QuestionRepository(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AppTheme(themePreference = state.themePreference) {
                EstimatApp(
                    state = state,
                    viewModel = viewModel,
                    onThemePreferenceChange = viewModel::setThemePreference,
                )
            }
        }
    }
}

@Composable
private fun AppTheme(
    themePreference: ThemePreference,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF81D5C7),
            onPrimary = Color(0xFF003730),
            primaryContainer = Color(0xFF005047),
            onPrimaryContainer = Color(0xFF9EF2E2),
            secondary = Color(0xFFB1CCC5),
            secondaryContainer = Color(0xFF334B46),
            onSecondaryContainer = Color(0xFFCDE8E1),
            background = Color(0xFF0E1513),
            surface = Color(0xFF0E1513),
            surfaceVariant = Color(0xFF3F4946),
            outline = Color(0xFF89938F),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF006B5F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF9EF2E2),
            onPrimaryContainer = Color(0xFF00201C),
            secondary = Color(0xFF4A635E),
            secondaryContainer = Color(0xFFCDE8E1),
            onSecondaryContainer = Color(0xFF06201B),
            background = Color(0xFFF5FBF8),
            surface = Color(0xFFF5FBF8),
            surfaceVariant = Color(0xFFDAE5E1),
            outline = Color(0xFF6F7976),
        )
    }
    val systemBarColor = Color.Transparent.toArgb()
    val activity = LocalContext.current as? ComponentActivity
    SideEffect {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = systemBarColor,
                darkScrim = systemBarColor,
                detectDarkMode = { darkTheme },
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = systemBarColor,
                darkScrim = systemBarColor,
                detectDarkMode = { darkTheme },
            ),
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            headlineLarge = Typography().headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            headlineMedium = Typography().headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}


private enum class Destination(val titleRes: Int, val labelRes: Int) {
    GAME(R.string.app_name, R.string.navigation_game),
    SESSIONS(R.string.navigation_sessions, R.string.navigation_sessions),
    FILTERS(R.string.navigation_filters, R.string.navigation_filters),
    SETTINGS(R.string.navigation_settings, R.string.navigation_settings),
}

@Composable
private fun Destination.icon(): Painter = when (this) {
    Destination.GAME -> painterResource(R.drawable.ic_nav_question)
    Destination.SESSIONS -> painterResource(R.drawable.ic_nav_sessions)
    Destination.FILTERS -> painterResource(R.drawable.ic_nav_filters)
    Destination.SETTINGS -> rememberVectorPainter(Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EstimatApp(
    state: AppUiState,
    viewModel: AppViewModel,
    onThemePreferenceChange: (ThemePreference) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(Destination.GAME) }
    // Filters reset to defaults but keep the "only verified" choice.
    val clearedFilter = QuestionFilter(verifiedOnly = state.filter.verifiedOnly)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(selected.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                actions = {
                    if (selected == Destination.FILTERS) {
                        TextButton(
                            onClick = { viewModel.setQuestionFilter(clearedFilter) },
                            enabled = state.filter != clearedFilter,
                        ) {
                            Text(stringResource(R.string.filters_clear))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Destination.entries.forEach { destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = selected == destination,
                        onClick = { selected = destination },
                        icon = { Icon(painter = destination.icon(), contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selected) {
                Destination.GAME -> QuestionScreen(
                    state = state,
                    onDrawQuestion = viewModel::nextQuestion,
                    onRevealAnswer = viewModel::revealAnswer,
                    onRestartDeck = viewModel::restartDeck,
                    onOpenSessions = { selected = Destination.SESSIONS },
                    onOpenFilters = { selected = Destination.FILTERS },
                    onDeleteQuestion = viewModel::deleteQuestion,
                    onFilterChange = viewModel::setQuestionFilter,
                    onFunnyOnlyChange = { funnyOnly ->
                        val tones = state.filter.tones
                        viewModel.setQuestionFilter(
                            state.filter.copy(
                                // Off only drops "funny", so other tone choices survive.
                                tones = if (funnyOnly) setOf(QuestionBank.TAG_FUNNY) else tones - QuestionBank.TAG_FUNNY,
                            ),
                        )
                    },
                )
                Destination.SESSIONS -> SessionsScreen(
                    state = state,
                    actions = SessionActions(
                        onCreate = viewModel::createSession,
                        onSelect = viewModel::selectSession,
                        onRename = viewModel::renameSession,
                        onReset = viewModel::resetSession,
                        onDelete = viewModel::deleteSession,
                    ),
                    onPlay = { selected = Destination.GAME },
                )
                Destination.FILTERS -> FiltersScreen(
                    state = state,
                    onFilterChange = viewModel::setQuestionFilter,
                )
                Destination.SETTINGS -> SettingsScreen(
                    themePreference = state.themePreference,
                    language = state.language,
                    onThemePreferenceChange = onThemePreferenceChange,
                    onLanguageChange = viewModel::setQuestionLanguage,
                )
            }
        }
    }
}
