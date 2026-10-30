package com.nawawia.ui.feature

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.components.ChunkyButton
import com.nawawia.ui.components.ChunkyButtonSize
import com.nawawia.ui.components.ChunkyButtonVariant
import com.nawawia.ui.theme.NawawiaTheme

@Composable
fun NawawiaAppRoot(
    viewModel: NawawiaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isDark = when (state.themeMode) {
        ThemeModeOption.LIGHT -> false
        ThemeModeOption.DARK -> true
        ThemeModeOption.SYSTEM -> isSystemInDarkTheme()
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    val openVerifiedUrl: (String) -> Unit = { rawUrl ->
        val allowed = viewModel.onOpenSharhUrl(rawUrl)
        if (allowed) {
            try {
                val customTabsIntent = CustomTabsIntent.Builder().build()
                customTabsIntent.launchUrl(context, Uri.parse(rawUrl))
            } catch (_: Exception) {
                try {
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                } catch (_: Exception) {
                    // Handled via snackbar message
                }
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NawawiaTheme(darkTheme = isDark) {
            val colors = NawawiaTheme.colors

            if (!state.isContentValid) {
                ContentUnavailableScreen(
                    validationErrorReason = state.contentErrorReason ?: "خطأ في التحقق من سلامة المحتوى",
                    modifier = modifier.safeDrawingPadding()
                )
                return@NawawiaTheme
            }

            Scaffold(
                modifier = modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .safeDrawingPadding(),
                containerColor = colors.bg,
                contentWindowInsets = WindowInsets.navigationBars,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.testTag("app_snackbar_host")
                    ) { data ->
                        // Also render an inline banner with explicit testTag for immediate UI test visibility
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.ink, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("snackbar_banner")
                        ) {
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.surface
                            )
                        }
                    }
                },
                bottomBar = {
                    if (state.activeModal == null) {
                        NavigationBar(
                            containerColor = colors.surface,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .border(width = 1.dp, color = colors.outline)
                                .testTag("bottom_navigation_bar")
                        ) {
                            for (tab in BottomNavTab.entries) {
                                val selected = state.activeTab == tab
                                val icon = when (tab) {
                                    BottomNavTab.PATH -> Icons.Default.Route
                                    BottomNavTab.REVIEW -> Icons.Default.Autorenew
                                    BottomNavTab.INDEX -> Icons.Default.MenuBook
                                    BottomNavTab.PROFILE -> Icons.Default.AccountCircle
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = tab.titleAr
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.titleAr,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.brand,
                                        selectedTextColor = colors.brand,
                                        indicatorColor = colors.brandSoft,
                                        unselectedIconColor = colors.inkMuted,
                                        unselectedTextColor = colors.inkMuted
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (val modal = state.activeModal) {
                        is ModalScreen.Onboarding -> {
                            OnboardingScreen(
                                step = modal.step,
                                dailyGoalMinutes = state.dailyGoalMinutes,
                                onSelectDailyGoal = viewModel::selectDailyGoalMinutes,
                                onNextStepClick = viewModel::advanceOnboardingStep,
                                onSkipClick = viewModel::skipOnboarding
                            )
                        }

                        is ModalScreen.Reader -> {
                            val hadith = state.hadiths.find { it.id == modal.hadithId } ?: state.hadiths.first()
                            HadithReaderScreen(
                                hadith = hadith,
                                showTashkeel = state.showTashkeel,
                                useLargeFont = state.useLargeFont,
                                isBookmarked = state.bookmarkedHadithIds.contains(hadith.id),
                                audioState = audioState,
                                onBackClick = viewModel::closeReader,
                                onToggleTashkeel = viewModel::toggleTashkeel,
                                onToggleLargeFont = viewModel::toggleLargeFont,
                                onToggleBookmark = { viewModel.toggleBookmark(hadith.id) },
                                onCopyHadith = { viewModel.onCopyHadith(hadith) },
                                onOpenSharhUrl = openVerifiedUrl,
                                onPlayPauseAudio = viewModel.audioController::togglePlayPause,
                                onSeekAudio = viewModel.audioController::seekTo,
                                onCycleAudioSpeed = viewModel.audioController::cycleSpeed,
                                onToggleAbLoop = viewModel.audioController::toggleAbLoopPoint,
                                onCycleRepeat = viewModel.audioController::cycleRepeatMode,
                                onRetryAudio = viewModel.audioController::retryAudio,
                                onStartQuizClick = { viewModel.startLessonForHadith(hadith.id) }
                            )
                        }

                        is ModalScreen.Lesson -> {
                            val activeLesson = state.activeLesson
                            if (activeLesson != null) {
                                LessonScreen(
                                    lesson = activeLesson,
                                    heartsCount = state.heartsState.currentHearts,
                                    audioState = audioState,
                                    onRequestExitClick = viewModel::requestExitLesson,
                                    onHeartsClick = viewModel::openStatusDialog,
                                    onSelectOption = viewModel::selectQuizOption,
                                    onMovePhraseUp = viewModel::movePhraseUp,
                                    onMovePhraseDown = viewModel::movePhraseDown,
                                    onSelectMatchRight = viewModel::selectMatchPairRight,
                                    onSelectMatchLeft = viewModel::selectMatchPairLeft,
                                    onPlayAudioPrompt = viewModel::playCurrentQuestionAudio,
                                    onCheckAnswerClick = viewModel::checkCurrentAnswer,
                                    onContinueClick = viewModel::advanceLessonQuestion,
                                    onOpenSharhClick = openVerifiedUrl
                                )
                            }
                        }

                        is ModalScreen.Result -> {
                            val result = state.lastLessonResult
                            if (result != null) {
                                ResultScreen(
                                    result = result,
                                    onNextHadithClick = viewModel::onNextHadithFromResult,
                                    onBackToPathClick = viewModel::onReturnToPathFromResult
                                )
                            }
                        }

                        is ModalScreen.Settings -> {
                            SettingsScreen(
                                state = state,
                                onBackClick = viewModel::closeSettings,
                                onSelectThemeMode = viewModel::setThemeMode,
                                onToggleLargeFont = viewModel::toggleLargeFont,
                                onToggleTashkeel = viewModel::toggleTashkeel,
                                onToggleSound = viewModel::toggleSound,
                                onToggleHaptics = viewModel::toggleHaptics,
                                onSelectDailyGoal = viewModel::selectDailyGoalMinutes,
                                onExportBackupClick = viewModel::exportProgressBackup,
                                onOpenAdabClick = viewModel::openAdabDialog,
                                onRequestResetDataClick = viewModel::requestResetData
                            )
                        }

                        is ModalScreen.PassAndPlay -> {
                            val session = state.matchSession
                            if (session != null) {
                                PassAndPlayScreen(
                                    session = session,
                                    onSelectOption = viewModel::submitPassAndPlayAnswer,
                                    onCloseClick = viewModel::closePassAndPlay
                                )
                            }
                        }

                        null -> {
                            when (state.activeTab) {
                                BottomNavTab.PATH -> {
                                    PathScreen(
                                        state = state,
                                        onNodeClick = viewModel::onNodeClick,
                                        onStreakClick = viewModel::openStatusDialog,
                                        onHeartsClick = viewModel::openStatusDialog,
                                        onXpClick = viewModel::openStatusDialog
                                    )
                                }

                                BottomNavTab.REVIEW -> {
                                    ReviewScreen(
                                        state = state,
                                        onStartReviewMode = { mode -> viewModel.startReviewSession(mode) },
                                        onStartPassAndPlayClick = viewModel::startPassAndPlay
                                    )
                                }

                                BottomNavTab.INDEX -> {
                                    IndexScreen(
                                        state = state,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onClearSearchClick = viewModel::clearSearchQuery,
                                        onSelectFilter = viewModel::selectIndexFilter,
                                        onHadithClick = { hadith -> viewModel.openReader(hadith.id) },
                                        onToggleBookmark = viewModel::toggleBookmark
                                    )
                                }

                                BottomNavTab.PROFILE -> {
                                    ProfileScreen(
                                        state = state,
                                        onOpenSettingsClick = viewModel::openSettings,
                                        onOpenOnboardingClick = viewModel::openOnboarding,
                                        onOpenAdabClick = viewModel::openAdabDialog
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------------
            // Modals & Confirmation Dialogs
            // -------------------------------------------------------------------

            // 1. Adab & Scholarly Attribution Dialog (Section 6.1 & 9.49)
            if (state.showAdabDialog) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissAdabDialog,
                    title = {
                        Text(
                            text = "بيان المنهج وأدب المتعلم",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.ink
                        )
                    },
                    text = {
                        Text(
                            text = "هذا التطبيق وسيلة للمراجعة والحفظ، وليس مرجعًا للفتوى. لفهم الأحكام، راجع أهل العلم.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.ink,
                            modifier = Modifier.testTag("adab_dialog_text")
                        )
                    },
                    confirmButton = {
                        ChunkyButton(
                            label = "حسنًا، فهمت",
                            onClick = viewModel::dismissAdabDialog,
                            variant = ChunkyButtonVariant.Primary,
                            size = ChunkyButtonSize.Medium,
                            modifier = Modifier.testTag("adab_dialog_close")
                        )
                    },
                    containerColor = colors.surface,
                    modifier = Modifier.testTag("adab_dialog")
                )
            }

            // 2. Exit Active Lesson Confirmation Dialog (Section 9.22)
            if (state.showExitLessonConfirmDialog) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissExitLessonDialog,
                    title = {
                        Text(
                            text = "مغادرة الدرس الحالي؟",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.ink
                        )
                    },
                    text = {
                        Text(
                            text = "هل تريد الخروج من الاختبار الحالي والعودة إلى الشاشة الرئيسية؟",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkMuted
                        )
                    },
                    confirmButton = {
                        ChunkyButton(
                            label = "مغادرة الدرس",
                            onClick = viewModel::confirmExitLesson,
                            variant = ChunkyButtonVariant.Danger,
                            size = ChunkyButtonSize.Medium,
                            modifier = Modifier.testTag("exit_lesson_confirm_yes")
                        )
                    },
                    dismissButton = {
                        ChunkyButton(
                            label = "مواصلة التعلم",
                            onClick = viewModel::dismissExitLessonDialog,
                            variant = ChunkyButtonVariant.Tonal,
                            size = ChunkyButtonSize.Medium,
                            modifier = Modifier.testTag("exit_lesson_confirm_cancel")
                        )
                    },
                    containerColor = colors.surface,
                    modifier = Modifier.testTag("exit_lesson_dialog")
                )
            }

            // 3. Reset All Data Confirmation Dialog (Section 6.9 & 9.12)
            if (state.showResetConfirmDialog) {
                AlertDialog(
                    onDismissRequest = viewModel::cancelResetData,
                    title = {
                        Text(
                            text = "تأكيد حذف جميع البيانات",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.wrong
                        )
                    },
                    text = {
                        Text(
                            text = "سيتم مسح جميع نقاط الخبرة، سلسلة الأيام، المحفوظات، وسجل الإتقان من الجهاز نهائيًا. هل أنت متأكد؟",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink
                        )
                    },
                    confirmButton = {
                        ChunkyButton(
                            label = "نعم، احذف بياناتي",
                            onClick = viewModel::confirmResetData,
                            variant = ChunkyButtonVariant.Danger,
                            size = ChunkyButtonSize.Medium,
                            modifier = Modifier.testTag("reset_confirm_yes")
                        )
                    },
                    dismissButton = {
                        ChunkyButton(
                            label = "إلغاء",
                            onClick = viewModel::cancelResetData,
                            variant = ChunkyButtonVariant.Tonal,
                            size = ChunkyButtonSize.Medium,
                            modifier = Modifier.testTag("reset_confirm_cancel")
                        )
                    },
                    containerColor = colors.surface,
                    modifier = Modifier.testTag("reset_confirm_dialog")
                )
            }

            // 4. Hearts Exhausted Sheet / Dialog (Section 6.4)
            if (state.showHeartsEmptySheet) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissHeartsEmptySheet,
                    title = {
                        Text(
                            text = "نفدت القلوب المتاحة",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.wrong
                        )
                    },
                    text = {
                        Text(
                            text = "يتجدد قلب واحد تلقائيًا كل ٣٠ دقيقة، أو يمكنك استعادة قلب فورًا عبر المراجعة المجانية (دون إعلانات أو اشتراكات).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink
                        )
                    },
                    confirmButton = {
                        ChunkyButton(
                            label = "استعد قلبًا بالمراجعة الآن",
                            onClick = viewModel::earnHeartByReviewFromSheet,
                            variant = ChunkyButtonVariant.Primary,
                            size = ChunkyButtonSize.Medium,
                            modifier = Modifier.testTag("earn_heart_review_button")
                        )
                    },
                    dismissButton = {
                        ChunkyButton(
                            label = "إغلاق",
                            onClick = viewModel::dismissHeartsEmptySheet,
                            variant = ChunkyButtonVariant.Tonal,
                            size = ChunkyButtonSize.Medium,
                            modifier = Modifier.testTag("hearts_empty_close_button")
                        )
                    },
                    containerColor = colors.surface,
                    modifier = Modifier.testTag("hearts_empty_dialog")
                )
            }

            // 5. Status Info Dialog (when clicking StreakFlame or HeartBar)
            if (state.showStatusDialog) {
                val arStreak = ArabicNormalizer.formatNumber(state.streakState.streakDays)
                val arFreezes = ArabicNormalizer.formatNumber(state.streakState.freezesAvailable)
                val arHearts = ArabicNormalizer.formatNumber(state.heartsState.currentHearts)
                AlertDialog(
                    onDismissRequest = viewModel::dismissStatusDialog,
                    title = {
                        Text(
                            text = "حالة المواظبة والقلوب",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.ink
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "• سلسلة الأيام الحالية: $arStreak يوم (تجميد متاح: $arFreezes)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink
                            )
                            Text(
                                text = "• القلوب الحالية: $arHearts من ٥ (يتجدد قلب كل ٣٠ دقيقة)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink
                            )
                        }
                    },
                    confirmButton = {
                        ChunkyButton(
                            label = "إغلاق",
                            onClick = viewModel::dismissStatusDialog,
                            variant = ChunkyButtonVariant.Primary,
                            size = ChunkyButtonSize.Medium,
                            modifier = Modifier.testTag("status_info_close_button")
                        )
                    },
                    containerColor = colors.surface,
                    modifier = Modifier.testTag("status_info_dialog")
                )
            }
        }
    }
}
