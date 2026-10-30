package com.nawawia.ui.feature

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nawawia.AppContainer
import com.nawawia.core.clock.AppClock
import com.nawawia.core.error.AppError
import com.nawawia.core.result.AppResult
import com.nawawia.data.audio.AudioController
import com.nawawia.data.audio.AudioPlayerState
import com.nawawia.data.content.ContentBundle
import com.nawawia.data.content.ContentLoader
import com.nawawia.data.db.ActiveLessonSessionEntity
import com.nawawia.data.db.HadithProgressEntity
import com.nawawia.data.db.NawawiaDao
import com.nawawia.data.db.UserStateEntity
import com.nawawia.domain.game.AchievementsEngine
import com.nawawia.domain.game.HeartsEngine
import com.nawawia.domain.game.StreakEngine
import com.nawawia.domain.game.XpEngine
import com.nawawia.domain.model.Achievement
import com.nawawia.domain.model.Hadith
import com.nawawia.domain.model.HadithUnit
import com.nawawia.domain.model.HeartsState
import com.nawawia.domain.model.MatchSession
import com.nawawia.domain.model.NodeStatus
import com.nawawia.domain.model.Question
import com.nawawia.domain.model.QuestionType
import com.nawawia.domain.model.ReviewMode
import com.nawawia.domain.model.Sm2State
import com.nawawia.domain.model.StreakState
import com.nawawia.domain.security.LinkGuard
import com.nawawia.domain.srs.Sm2Scheduler
import com.nawawia.domain.text.ArabicNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class BottomNavTab(val titleAr: String, val testTag: String) {
    PATH("الطريق", "nav_tab_path"),
    REVIEW("المراجعة", "nav_tab_review"),
    INDEX("الأحاديث", "nav_tab_index"),
    PROFILE("حسابي", "nav_tab_profile")
}

enum class IndexFilter(val labelAr: String, val testTag: String) {
    ALL("الكل", "filter_all"),
    BOOKMARKED("المحفوظة", "filter_bookmarked"),
    MASTERED("المتقنة", "filter_mastered"),
    NOT_STARTED("لم تبدأ", "filter_not_started")
}

enum class ThemeModeOption(val labelAr: String) {
    LIGHT("نهاري"),
    DARK("ليلي"),
    SYSTEM("تلقائي")
}

sealed interface ModalScreen {
    data class Onboarding(val step: Int = 1) : ModalScreen
    data class Reader(val hadithId: String) : ModalScreen
    data object Lesson : ModalScreen
    data object Result : ModalScreen
    data object Settings : ModalScreen
    data object PassAndPlay : ModalScreen
}

@Immutable
data class ActiveLessonUiState(
    val hadithId: String,
    val hadithTitleAr: String,
    val seed: Long,
    val questions: List<Question>,
    val currentIndex: Int = 0,
    val selectedOptionId: String? = null,
    val currentPhraseOrder: List<String> = emptyList(),
    val selectedRightPairId: String? = null,
    val selectedLeftPairId: String? = null,
    val matchedPairIds: Set<String> = emptySet(),
    val isAnswerRevealed: Boolean = false,
    val isCurrentAnswerCorrect: Boolean = false,
    val feedbackTextAr: String = "",
    val correctFirstTryCount: Int = 0,
    val correctAfterRetryCount: Int = 0,
    val retriesCount: Int = 0,
    val retriedQuestionIds: Set<String> = emptySet(),
    val isReviewMode: Boolean = false,
    val reviewMode: ReviewMode? = null
) {
    val currentQuestion: Question?
        get() = questions.getOrNull(currentIndex)

    val progressFraction: Float
        get() = if (questions.isEmpty()) 0f else ((currentIndex + 1).toFloat() / questions.size.toFloat()).coerceIn(0f, 1f)

    val canCheckAnswer: Boolean
        get() {
            if (isAnswerRevealed) return false
            val q = currentQuestion ?: return false
            return when (q.type) {
                QuestionType.ORDER_PHRASES -> currentPhraseOrder.isNotEmpty()
                QuestionType.MATCH_PAIRS -> matchedPairIds.size == q.matchPairs.size && q.matchPairs.isNotEmpty()
                else -> selectedOptionId != null
            }
        }
}

@Immutable
data class LessonResultUiState(
    val hadithId: String,
    val hadithNumber: Int,
    val hadithTitleAr: String,
    val xpEarned: Int,
    val scorePct: Int,
    val masteryPct: Int,
    val heartsRemaining: Int,
    val isUnitCompleted: Boolean,
    val encouragementTextAr: String,
    val nextHadithId: String?
)

@Immutable
data class AppUiState(
    val isContentValid: Boolean = true,
    val contentErrorReason: String? = null,
    val contentVersion: String = "nawawi-1.0.0",
    val units: List<HadithUnit> = emptyList(),
    val hadiths: List<Hadith> = emptyList(),
    val questionsByHadithId: Map<String, List<Question>> = emptyMap(),
    val progressByHadithId: Map<String, Sm2State> = emptyMap(),
    val bookmarkedHadithIds: Set<String> = emptySet(),
    val activeTab: BottomNavTab = BottomNavTab.PATH,
    val activeModal: ModalScreen? = null,
    val totalXp: Int = 0,
    val heartsState: HeartsState = HeartsState(),
    val streakState: StreakState = StreakState(),
    val dailyGoalMinutes: Int = 10,
    val totalMinutesSpent: Int = 0,
    val completedQuickReview10Min: Boolean = false,
    val themeMode: ThemeModeOption = ThemeModeOption.LIGHT,
    val showTashkeel: Boolean = true,
    val useLargeFont: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val searchQuery: String = "",
    val selectedFilter: IndexFilter = IndexFilter.ALL,
    val activeLesson: ActiveLessonUiState? = null,
    val lastLessonResult: LessonResultUiState? = null,
    val matchSession: MatchSession? = null,
    val showAdabDialog: Boolean = false,
    val showResetConfirmDialog: Boolean = false,
    val showExitLessonConfirmDialog: Boolean = false,
    val showHeartsEmptySheet: Boolean = false,
    val showStatusDialog: Boolean = false,
    val snackbarMessage: String? = null
) {
    /**
     * Determines the node status for each hadith on the Path:
     * Hadith 1 is always at least AVAILABLE on first launch.
     * Hadith N (N > 1) is unlocked if Hadith N-1 has repetitions >= 1 or masteryPct > 0.
     */
    fun getNodeStatus(hadith: Hadith): NodeStatus {
        val progress = progressByHadithId[hadith.id]
        val mastery = progress?.masteryPct ?: 0
        if (mastery >= 80) return NodeStatus.MASTERED
        if (mastery > 0) return NodeStatus.IN_PROGRESS

        if (hadith.number == 1) return NodeStatus.AVAILABLE
        val prevHadithId = "h${hadith.number - 1}"
        val prevMastery = progressByHadithId[prevHadithId]?.masteryPct ?: 0
        return if (prevMastery > 0) NodeStatus.AVAILABLE else NodeStatus.LOCKED
    }

    /**
     * Returns the current recommended hadith ID for the "تابع من هنا" hero CTA.
     */
    val currentContinueHadith: Hadith?
        get() {
            return hadiths.firstOrNull { getNodeStatus(it) == NodeStatus.AVAILABLE || getNodeStatus(it) == NodeStatus.IN_PROGRESS }
                ?: hadiths.firstOrNull()
        }

    val masteredHadithCount: Int
        get() = hadiths.count { (progressByHadithId[it.id]?.masteryPct ?: 0) >= 60 }

    val completedUnitsCount: Int
        get() = units.count { unit ->
            unit.hadithIds.all { hId -> (progressByHadithId[hId]?.masteryPct ?: 0) >= 60 }
        }

    val achievements: List<Achievement>
        get() = AchievementsEngine.evaluateAchievements(
            masteredHadithCount = masteredHadithCount,
            completedUnitsCount = completedUnitsCount,
            streakDays = streakState.streakDays,
            completedQuickReview10Min = completedQuickReview10Min,
            totalXp = totalXp
        )

    val filteredIndexHadiths: List<Hadith>
        get() {
            val normalizedQuery = ArabicNormalizer.normalizeForSearch(searchQuery)
            return hadiths.filter { h ->
                val matchesFilter = when (selectedFilter) {
                    IndexFilter.ALL -> true
                    IndexFilter.BOOKMARKED -> bookmarkedHadithIds.contains(h.id)
                    IndexFilter.MASTERED -> (progressByHadithId[h.id]?.masteryPct ?: 0) >= 60
                    IndexFilter.NOT_STARTED -> (progressByHadithId[h.id]?.masteryPct ?: 0) == 0
                }
                val matchesQuery = if (normalizedQuery.isEmpty()) {
                    true
                } else {
                    val normTitle = ArabicNormalizer.normalizeForSearch(h.titleAr)
                    val normMatn = ArabicNormalizer.normalizeForSearch(h.matnAr)
                    val normNarrator = ArabicNormalizer.normalizeForSearch(h.narratorAr)
                    normTitle.contains(normalizedQuery) ||
                        normMatn.contains(normalizedQuery) ||
                        normNarrator.contains(normalizedQuery) ||
                        h.number.toString().contains(normalizedQuery)
                }
                matchesFilter && matchesQuery
            }
        }
}

class NawawiaViewModel(
    private val clock: AppClock,
    private val dao: NawawiaDao?,
    val audioController: AudioController,
    contentResult: AppResult<ContentBundle>
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState(contentResult))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val audioState: StateFlow<AudioPlayerState> = audioController.state

    init {
        if (dao != null && _uiState.value.isContentValid) {
            loadPersistedStateFromDb()
        }
    }

    private fun createInitialState(contentResult: AppResult<ContentBundle>): AppUiState {
        return when (contentResult) {
            is AppResult.Failure -> {
                AppUiState(
                    isContentValid = false,
                    contentErrorReason = contentResult.error.userMessageAr()
                )
            }
            is AppResult.Success -> {
                val bundle = contentResult.data
                AppUiState(
                    isContentValid = true,
                    contentVersion = bundle.contentVersion,
                    units = bundle.units.sortedBy { it.order },
                    hadiths = bundle.hadiths.sortedBy { it.number },
                    questionsByHadithId = bundle.questions.groupBy { it.hadithId }
                )
            }
        }
    }

    private fun loadPersistedStateFromDb() {
        viewModelScope.launch {
            val dbDao = dao ?: return@launch
            val storedUser = dbDao.getUserState()
            val storedProgress = dbDao.getAllHadithProgress()
            val storedSession = dbDao.getActiveLessonSession()

            _uiState.update { state ->
                val rawHearts = if (storedUser != null) {
                    HeartsState(
                        currentHearts = storedUser.currentHearts,
                        lastHeartSpentEpochMillis = storedUser.lastHeartSpentEpochMillis
                    )
                } else {
                    state.heartsState
                }
                val recomputedHearts = HeartsEngine.recomputeHearts(rawHearts, clock)

                val progressMap = storedProgress.associate { entity ->
                    entity.hadithId to Sm2State(
                        hadithId = entity.hadithId,
                        easeFactor = entity.easeFactor,
                        intervalDays = entity.intervalDays,
                        repetitions = entity.repetitions,
                        dueEpochDay = entity.dueEpochDay,
                        masteryPct = entity.masteryPct,
                        lastGrade = entity.lastGrade
                    )
                }
                val bookmarks = storedProgress.filter { it.isBookmarked }.map { it.hadithId }.toSet()

                val restoredLesson = if (storedSession != null) {
                    restoreActiveLessonFromEntity(storedSession, state)
                } else null

                val historyDays = storedUser?.activeEpochDaysCsv
                    ?.split(",")
                    ?.mapNotNull { it.trim().toLongOrNull() }
                    ?.toSet()
                    ?: emptySet()

                state.copy(
                    totalXp = storedUser?.totalXp ?: state.totalXp,
                    heartsState = recomputedHearts,
                    streakState = if (storedUser != null) {
                        StreakState(
                            streakDays = storedUser.streakDays,
                            lastActiveEpochDay = storedUser.lastActiveEpochDay,
                            freezesAvailable = storedUser.freezesAvailable,
                            activeEpochDaysHistory = historyDays,
                            clockRollbackDiagnosticsCount = storedUser.clockRollbackCount
                        )
                    } else state.streakState,
                    dailyGoalMinutes = storedUser?.dailyGoalMinutes ?: state.dailyGoalMinutes,
                    totalMinutesSpent = storedUser?.totalMinutesSpent ?: state.totalMinutesSpent,
                    completedQuickReview10Min = storedUser?.completedQuickReview10Min ?: state.completedQuickReview10Min,
                    themeMode = storedUser?.themeMode?.let {
                        runCatching { ThemeModeOption.valueOf(it) }.getOrNull()
                    } ?: state.themeMode,
                    showTashkeel = storedUser?.tashkeelEnabled ?: state.showTashkeel,
                    useLargeFont = storedUser?.largeFontEnabled ?: state.useLargeFont,
                    soundEnabled = storedUser?.soundEnabled ?: state.soundEnabled,
                    hapticsEnabled = storedUser?.hapticsEnabled ?: state.hapticsEnabled,
                    progressByHadithId = if (progressMap.isNotEmpty()) progressMap else state.progressByHadithId,
                    bookmarkedHadithIds = if (bookmarks.isNotEmpty()) bookmarks else state.bookmarkedHadithIds,
                    activeLesson = restoredLesson ?: state.activeLesson
                )
            }
        }
    }

    private fun restoreActiveLessonFromEntity(
        entity: ActiveLessonSessionEntity,
        state: AppUiState
    ): ActiveLessonUiState? {
        val allQuestions = state.questionsByHadithId.values.flatten().associateBy { it.id }
        val orderedQuestions = entity.questionIdsCsv
            .split(",")
            .mapNotNull { allQuestions[it.trim()] }
        if (orderedQuestions.isEmpty()) return null

        val hadith = state.hadiths.find { it.id == entity.hadithId }
        val safeIdx = entity.currentQuestionIndex.coerceIn(0, orderedQuestions.lastIndex)
        val currentQ = orderedQuestions[safeIdx]
        val initialPhrases = if (currentQ.type == QuestionType.ORDER_PHRASES) {
            shufflePhrasesDeterministically(currentQ.orderedPhrases, entity.seed + safeIdx)
        } else emptyList()

        return ActiveLessonUiState(
            hadithId = entity.hadithId,
            hadithTitleAr = hadith?.titleAr ?: "مراجعة الأحاديث",
            seed = entity.seed,
            questions = orderedQuestions,
            currentIndex = safeIdx,
            currentPhraseOrder = initialPhrases,
            correctFirstTryCount = entity.correctFirstTryCount,
            correctAfterRetryCount = entity.correctAfterRetryCount,
            retriesCount = entity.retriesCount,
            isReviewMode = entity.isReviewMode
        )
    }

    // ---------------------------------------------------------------------------
    // Navigation & Screen Intents
    // ---------------------------------------------------------------------------

    fun selectTab(tab: BottomNavTab) {
        _uiState.update {
            it.copy(
                activeTab = tab,
                activeModal = null
            )
        }
    }

    fun onContinueLearningClick() {
        val targetHadith = _uiState.value.currentContinueHadith ?: return
        openReader(targetHadith.id)
    }

    fun onQuickReviewFromHomeClick() {
        startReviewSession(ReviewMode.QUICK_10_MIN)
    }

    fun onBrowseHadithsFromHomeClick() {
        selectTab(BottomNavTab.INDEX)
    }

    fun onNodeClick(hadith: Hadith) {
        openReader(hadith.id)
    }

    fun onUnitStartClick(unit: HadithUnit) {
        val state = _uiState.value
        val unitHadiths = state.hadiths.filter { it.unitId == unit.id }
        val target = unitHadiths.firstOrNull { (state.progressByHadithId[it.id]?.masteryPct ?: 0) < 80 }
            ?: unitHadiths.firstOrNull()
        if (target != null) {
            openReader(target.id)
        }
    }

    fun openReader(hadithId: String) {
        val hadith = _uiState.value.hadiths.find { it.id == hadithId } ?: return
        val arNum = ArabicNormalizer.formatNumber(hadith.number)
        audioController.loadHadithAudio(
            audioId = hadith.audio.matnId,
            spokenTextAr = "الحديث $arNum: ${hadith.titleAr}. ${hadith.narratorAr}. ${hadith.matnAr}",
            titleAr = hadith.titleAr
        )
        _uiState.update {
            it.copy(activeModal = ModalScreen.Reader(hadithId))
        }
    }

    fun playCurrentQuestionAudio() {
        val currentQ = _uiState.value.activeLesson?.currentQuestion
        val hadith = _uiState.value.hadiths.find { it.id == currentQ?.hadithId }
            ?: _uiState.value.hadiths.firstOrNull()
        if (hadith != null) {
            audioController.playPromptAudio(
                spokenTextAr = "${hadith.narratorAr}. ${hadith.matnAr}",
                titleAr = hadith.titleAr
            )
        } else {
            audioController.togglePlayPause()
        }
    }

    fun closeReader() {
        audioController.onAudioFocusLost()
        _uiState.update {
            it.copy(activeModal = null)
        }
    }

    // ---------------------------------------------------------------------------
    // Reader Controls
    // ---------------------------------------------------------------------------

    fun toggleTashkeel() {
        _uiState.update { it.copy(showTashkeel = !it.showTashkeel) }
        persistCurrentUserStateAsync()
    }

    fun toggleLargeFont() {
        _uiState.update { it.copy(useLargeFont = !it.useLargeFont) }
        persistCurrentUserStateAsync()
    }

    fun toggleBookmark(hadithId: String) {
        var nowBookmarked = false
        _uiState.update { state ->
            val nextSet = if (state.bookmarkedHadithIds.contains(hadithId)) {
                nowBookmarked = false
                state.bookmarkedHadithIds - hadithId
            } else {
                nowBookmarked = true
                state.bookmarkedHadithIds + hadithId
            }
            state.copy(
                bookmarkedHadithIds = nextSet,
                snackbarMessage = if (nowBookmarked) "تمت إضافة الحديث إلى المحفوظات" else "تمت إزالة الحديث من المحفوظات"
            )
        }
        persistHadithProgressAsync(hadithId)
    }

    fun onCopyHadith(hadith: Hadith): String {
        val attributed = ArabicNormalizer.formatAttributedHadith(hadith)
        _uiState.update {
            it.copy(snackbarMessage = "تم نسخ الحديث مع التخريج الموثّق")
        }
        return attributed
    }

    fun onOpenSharhUrl(url: String): Boolean {
        return when (val validation = LinkGuard.validate(url)) {
            is AppResult.Success -> {
                _uiState.update {
                    it.copy(snackbarMessage = "رابط موثوق: جاري فتح الشرح في الدرر السنية")
                }
                true
            }
            is AppResult.Failure -> {
                _uiState.update {
                    it.copy(snackbarMessage = "رابط غير موثوق — تم حجبه")
                }
                false
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Lesson & Quiz Engine (All 6 Question Types + Seeded Shuffle)
    // ---------------------------------------------------------------------------

    fun startLessonForHadith(hadithId: String, explicitSeed: Long? = null) {
        val state = _uiState.value
        val recomputedHearts = HeartsEngine.recomputeHearts(state.heartsState, clock)
        if (recomputedHearts.currentHearts <= 0) {
            _uiState.update {
                it.copy(
                    heartsState = recomputedHearts,
                    showHeartsEmptySheet = true
                )
            }
            return
        }

        val hadith = state.hadiths.find { it.id == hadithId } ?: return
        val pool = state.questionsByHadithId[hadithId].orEmpty()
        if (pool.isEmpty()) return

        val seed = explicitSeed ?: clock.now().toEpochMilli()
        val shuffled = pool.shuffled(Random(seed))
        val firstQ = shuffled.first()
        val initialPhrases = if (firstQ.type == QuestionType.ORDER_PHRASES) {
            shufflePhrasesDeterministically(firstQ.orderedPhrases, seed)
        } else emptyList()

        val lessonState = ActiveLessonUiState(
            hadithId = hadithId,
            hadithTitleAr = hadith.titleAr,
            seed = seed,
            questions = shuffled,
            currentIndex = 0,
            currentPhraseOrder = initialPhrases,
            isReviewMode = false
        )

        audioController.onAudioFocusLost()
        _uiState.update {
            it.copy(
                heartsState = recomputedHearts,
                activeLesson = lessonState,
                activeModal = ModalScreen.Lesson,
                showExitLessonConfirmDialog = false
            )
        }
        persistActiveSessionAsync()
    }

    fun startReviewSession(mode: ReviewMode, explicitSeed: Long? = null) {
        val state = _uiState.value
        val todayEpochDay = StreakEngine.currentEpochDay(clock)
        val seed = explicitSeed ?: clock.now().toEpochMilli()

        val selectedQuestions: List<Question> = when (mode) {
            ReviewMode.QUICK_10_MIN -> {
                // 1 question from each of the 42 hadiths
                state.hadiths.mapNotNull { h ->
                    state.questionsByHadithId[h.id]?.firstOrNull()
                }
            }
            ReviewMode.SMART_SRS -> {
                val dueHadiths = state.hadiths.filter { h ->
                    val srs = state.progressByHadithId[h.id]
                    srs == null || srs.dueEpochDay <= todayEpochDay
                }.take(10)
                dueHadiths.mapNotNull { h ->
                    state.questionsByHadithId[h.id]?.firstOrNull()
                }
            }
            ReviewMode.HARDEST_FIRST -> {
                val hardest = state.hadiths.sortedBy { h ->
                    state.progressByHadithId[h.id]?.masteryPct ?: 0
                }.take(7)
                hardest.mapNotNull { h ->
                    state.questionsByHadithId[h.id]?.firstOrNull()
                }
            }
        }

        if (selectedQuestions.isEmpty()) return
        val firstQ = selectedQuestions.first()
        val initialPhrases = if (firstQ.type == QuestionType.ORDER_PHRASES) {
            shufflePhrasesDeterministically(firstQ.orderedPhrases, seed)
        } else emptyList()

        val lessonState = ActiveLessonUiState(
            hadithId = firstQ.hadithId,
            hadithTitleAr = mode.titleAr,
            seed = seed,
            questions = selectedQuestions,
            currentIndex = 0,
            currentPhraseOrder = initialPhrases,
            isReviewMode = true,
            reviewMode = mode
        )

        _uiState.update {
            it.copy(
                activeLesson = lessonState,
                activeModal = ModalScreen.Lesson,
                showExitLessonConfirmDialog = false
            )
        }
        persistActiveSessionAsync()
    }

    private fun shufflePhrasesDeterministically(phrases: List<String>, seed: Long): List<String> {
        if (phrases.size <= 1) return phrases
        val shuffled = phrases.shuffled(Random(seed))
        return if (shuffled == phrases) phrases.reversed() else shuffled
    }

    fun selectQuizOption(optionId: String) {
        _uiState.update { state ->
            val lesson = state.activeLesson ?: return@update state
            if (lesson.isAnswerRevealed) return@update state
            state.copy(
                activeLesson = lesson.copy(selectedOptionId = optionId)
            )
        }
    }

    /**
     * Accessible non-drag phrase ordering (up/down buttons per phrase — Section 6.4 & 9.28).
     */
    fun movePhraseUp(index: Int) {
        _uiState.update { state ->
            val lesson = state.activeLesson ?: return@update state
            if (lesson.isAnswerRevealed || index <= 0 || index >= lesson.currentPhraseOrder.size) {
                return@update state
            }
            val mutable = lesson.currentPhraseOrder.toMutableList()
            val tmp = mutable[index - 1]
            mutable[index - 1] = mutable[index]
            mutable[index] = tmp
            state.copy(activeLesson = lesson.copy(currentPhraseOrder = mutable))
        }
    }

    fun movePhraseDown(index: Int) {
        _uiState.update { state ->
            val lesson = state.activeLesson ?: return@update state
            if (lesson.isAnswerRevealed || index < 0 || index >= lesson.currentPhraseOrder.lastIndex) {
                return@update state
            }
            val mutable = lesson.currentPhraseOrder.toMutableList()
            val tmp = mutable[index + 1]
            mutable[index + 1] = mutable[index]
            mutable[index] = tmp
            state.copy(activeLesson = lesson.copy(currentPhraseOrder = mutable))
        }
    }

    fun setExactPhraseOrderForTest(ordered: List<String>) {
        _uiState.update { state ->
            val lesson = state.activeLesson ?: return@update state
            state.copy(activeLesson = lesson.copy(currentPhraseOrder = ordered))
        }
    }

    fun selectMatchPairRight(pairId: String) {
        _uiState.update { state ->
            val lesson = state.activeLesson ?: return@update state
            if (lesson.isAnswerRevealed || lesson.matchedPairIds.contains(pairId)) return@update state
            val leftId = lesson.selectedLeftPairId
            return@update if (leftId != null) {
                if (leftId == pairId) {
                    state.copy(
                        activeLesson = lesson.copy(
                            selectedRightPairId = null,
                            selectedLeftPairId = null,
                            matchedPairIds = lesson.matchedPairIds + pairId
                        )
                    )
                } else {
                    state.copy(
                        activeLesson = lesson.copy(selectedRightPairId = pairId, selectedLeftPairId = null),
                        snackbarMessage = "اختر المعنى المطابق لهذه المفردة من العمود المقابل"
                    )
                }
            } else {
                state.copy(activeLesson = lesson.copy(selectedRightPairId = pairId))
            }
        }
    }

    fun selectMatchPairLeft(pairId: String) {
        _uiState.update { state ->
            val lesson = state.activeLesson ?: return@update state
            if (lesson.isAnswerRevealed || lesson.matchedPairIds.contains(pairId)) return@update state
            val rightId = lesson.selectedRightPairId
            return@update if (rightId != null) {
                if (rightId == pairId) {
                    state.copy(
                        activeLesson = lesson.copy(
                            selectedRightPairId = null,
                            selectedLeftPairId = null,
                            matchedPairIds = lesson.matchedPairIds + pairId
                        )
                    )
                } else {
                    state.copy(
                        activeLesson = lesson.copy(selectedRightPairId = null, selectedLeftPairId = pairId),
                        snackbarMessage = "اختر المفردة المطابقة لهذا المعنى من العمود المقابل"
                    )
                }
            } else {
                state.copy(activeLesson = lesson.copy(selectedLeftPairId = pairId))
            }
        }
    }

    fun checkCurrentAnswer() {
        _uiState.update { state ->
            val lesson = state.activeLesson ?: return@update state
            if (lesson.isAnswerRevealed) return@update state
            val currentQ = lesson.currentQuestion ?: return@update state

            val isCorrect = when (currentQ.type) {
                QuestionType.ORDER_PHRASES -> {
                    lesson.currentPhraseOrder == currentQ.orderedPhrases
                }
                QuestionType.MATCH_PAIRS -> {
                    lesson.matchedPairIds.size == currentQ.matchPairs.size
                }
                else -> {
                    val chosen = currentQ.options.find { it.id == lesson.selectedOptionId }
                    chosen?.correct == true
                }
            }

            val wasRetriedBefore = lesson.retriedQuestionIds.contains(currentQ.id)

            if (isCorrect) {
                val nextFirstTry = if (!wasRetriedBefore) lesson.correctFirstTryCount + 1 else lesson.correctFirstTryCount
                val nextRetryCorrect = if (wasRetriedBefore) lesson.correctAfterRetryCount + 1 else lesson.correctAfterRetryCount
                state.copy(
                    activeLesson = lesson.copy(
                        isAnswerRevealed = true,
                        isCurrentAnswerCorrect = true,
                        feedbackTextAr = "صحيح! أحسنت الإجابة",
                        correctFirstTryCount = nextFirstTry,
                        correctAfterRetryCount = nextRetryCorrect
                    )
                )
            } else {
                // Wrong answer: spend 1 heart unless in Quick 10-min review mode
                val penalizeHearts = lesson.reviewMode != ReviewMode.QUICK_10_MIN
                val nextHearts = if (penalizeHearts) {
                    HeartsEngine.spendHeart(state.heartsState, clock)
                } else {
                    state.heartsState
                }
                val heartsExhausted = penalizeHearts && nextHearts.currentHearts <= 0

                // Re-queue wrong question at the end of the lesson so the learner masters it
                val updatedQuestions = lesson.questions + currentQ

                state.copy(
                    heartsState = nextHearts,
                    showHeartsEmptySheet = heartsExhausted,
                    activeLesson = lesson.copy(
                        questions = updatedQuestions,
                        isAnswerRevealed = true,
                        isCurrentAnswerCorrect = false,
                        feedbackTextAr = "غير صحيح — راجع الإجابة الصحيحة بالأسفل",
                        retriesCount = lesson.retriesCount + 1,
                        retriedQuestionIds = lesson.retriedQuestionIds + currentQ.id
                    )
                )
            }
        }
        persistActiveSessionAsync()
    }

    fun advanceLessonQuestion() {
        val currentState = _uiState.value
        val lesson = currentState.activeLesson ?: return

        if (lesson.currentIndex < lesson.questions.lastIndex) {
            val nextIdx = lesson.currentIndex + 1
            val nextQ = lesson.questions[nextIdx]
            val initialPhrases = if (nextQ.type == QuestionType.ORDER_PHRASES) {
                shufflePhrasesDeterministically(nextQ.orderedPhrases, lesson.seed + nextIdx)
            } else emptyList()

            _uiState.update {
                it.copy(
                    activeLesson = lesson.copy(
                        currentIndex = nextIdx,
                        selectedOptionId = null,
                        currentPhraseOrder = initialPhrases,
                        selectedRightPairId = null,
                        matchedPairIds = emptySet(),
                        isAnswerRevealed = false,
                        isCurrentAnswerCorrect = false,
                        feedbackTextAr = ""
                    )
                )
            }
            persistActiveSessionAsync()
        } else {
            finishLessonSession()
        }
    }

    private fun finishLessonSession() {
        _uiState.update { state ->
            val lesson = state.activeLesson ?: return@update state
            val todayEpochDay = StreakEngine.currentEpochDay(clock)
            val updatedStreak = StreakEngine.recordActivity(state.streakState, clock)

            val prevSm2 = state.progressByHadithId[lesson.hadithId] ?: Sm2State(hadithId = lesson.hadithId)
            val grade = Sm2Scheduler.gradeFromLesson(
                retriesCount = lesson.retriesCount,
                heartsExhausted = state.heartsState.currentHearts <= 0
            )
            val nextSm2 = Sm2Scheduler.schedule(prevSm2, grade, todayEpochDay)
            val nextProgressMap = state.progressByHadithId + (lesson.hadithId to nextSm2)

            val hadith = state.hadiths.find { it.id == lesson.hadithId }
            val unit = state.units.find { it.id == hadith?.unitId }
            val isUnitDone = unit?.hadithIds?.all { hId ->
                (nextProgressMap[hId]?.masteryPct ?: 0) >= 60
            } ?: false

            val nextMinutes = state.totalMinutesSpent + 3
            val goalJustReached = state.totalMinutesSpent < state.dailyGoalMinutes && nextMinutes >= state.dailyGoalMinutes

            val xpEarned = XpEngine.calculateLessonXp(
                correctFirstTry = lesson.correctFirstTryCount,
                correctAfterRetry = lesson.correctAfterRetryCount,
                isUnitComplete = isUnitDone,
                isDailyGoalReached = goalJustReached,
                streakDays = updatedStreak.streakDays
            )

            val updatedHearts = if (lesson.isReviewMode) {
                HeartsEngine.grantReviewHeart(state.heartsState, clock)
            } else {
                state.heartsState
            }

            val totalQuestionsCount = (lesson.correctFirstTryCount + lesson.correctAfterRetryCount).coerceAtLeast(1)
            val scorePct = ((lesson.correctFirstTryCount * 100) / totalQuestionsCount).coerceIn(0, 100)

            val nextHadithNumber = (hadith?.number ?: 1) + 1
            val nextHadithId = if (nextHadithNumber <= 42) "h$nextHadithNumber" else null

            val resultState = LessonResultUiState(
                hadithId = lesson.hadithId,
                hadithNumber = hadith?.number ?: 1,
                hadithTitleAr = lesson.hadithTitleAr,
                xpEarned = xpEarned,
                scorePct = scorePct,
                masteryPct = nextSm2.masteryPct,
                heartsRemaining = updatedHearts.currentHearts,
                isUnitCompleted = isUnitDone,
                encouragementTextAr = if (isUnitDone) "رائع يا بطل! أتممت الوحدة بنجاح" else "أحسنت! خطوة ممتازة في حفظ الأربعين",
                nextHadithId = nextHadithId
            )

            state.copy(
                totalXp = state.totalXp + xpEarned,
                heartsState = updatedHearts,
                streakState = updatedStreak,
                totalMinutesSpent = nextMinutes,
                completedQuickReview10Min = state.completedQuickReview10Min || (lesson.reviewMode == ReviewMode.QUICK_10_MIN),
                progressByHadithId = nextProgressMap,
                activeLesson = null,
                lastLessonResult = resultState,
                activeModal = ModalScreen.Result,
                showHeartsEmptySheet = false
            )
        }
        persistCompletedLessonAsync()
    }

    fun requestExitLesson() {
        _uiState.update { it.copy(showExitLessonConfirmDialog = true) }
    }

    fun dismissExitLessonDialog() {
        _uiState.update { it.copy(showExitLessonConfirmDialog = false) }
    }

    fun confirmExitLesson() {
        _uiState.update {
            it.copy(
                activeLesson = null,
                activeModal = null,
                showExitLessonConfirmDialog = false,
                showHeartsEmptySheet = false
            )
        }
        viewModelScope.launch {
            dao?.clearActiveLessonSession()
        }
    }

    fun earnHeartByReviewFromSheet() {
        _uiState.update { state ->
            val refilled = HeartsEngine.grantReviewHeart(state.heartsState, clock)
            state.copy(
                heartsState = refilled,
                showHeartsEmptySheet = false,
                snackbarMessage = "حصلت على قلب إضافي للمتابعة!"
            )
        }
        persistCurrentUserStateAsync()
    }

    fun dismissHeartsEmptySheet() {
        _uiState.update { it.copy(showHeartsEmptySheet = false) }
    }

    // ---------------------------------------------------------------------------
    // Result Screen Actions
    // ---------------------------------------------------------------------------

    fun onNextHadithFromResult() {
        val nextId = _uiState.value.lastLessonResult?.nextHadithId
        if (nextId != null) {
            openReader(nextId)
        } else {
            onReturnToPathFromResult()
        }
    }

    fun onReturnToPathFromResult() {
        _uiState.update {
            it.copy(
                activeModal = null,
                activeTab = BottomNavTab.PATH
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Index Search & Filters
    // ---------------------------------------------------------------------------

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearSearchQuery() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    fun selectIndexFilter(filter: IndexFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    // ---------------------------------------------------------------------------
    // Onboarding, Settings, Profile, Pass-and-Play
    // ---------------------------------------------------------------------------

    fun openOnboarding() {
        _uiState.update { it.copy(activeModal = ModalScreen.Onboarding(1)) }
    }

    fun advanceOnboardingStep() {
        _uiState.update { state ->
            val currentModal = state.activeModal as? ModalScreen.Onboarding
            val step = currentModal?.step ?: 1
            if (step < 3) {
                state.copy(activeModal = ModalScreen.Onboarding(step + 1))
            } else {
                state.copy(activeModal = null)
            }
        }
        persistCurrentUserStateAsync()
    }

    fun skipOnboarding() {
        _uiState.update { it.copy(activeModal = null) }
        persistCurrentUserStateAsync()
    }

    fun selectDailyGoalMinutes(minutes: Int) {
        _uiState.update {
            it.copy(
                dailyGoalMinutes = minutes,
                snackbarMessage = "تم ضبط الهدف اليومي: ${ArabicNormalizer.formatNumber(minutes)} دقائق"
            )
        }
        persistCurrentUserStateAsync()
    }

    fun openSettings() {
        _uiState.update { it.copy(activeModal = ModalScreen.Settings) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(activeModal = null) }
    }

    fun setThemeMode(mode: ThemeModeOption) {
        _uiState.update { it.copy(themeMode = mode) }
        persistCurrentUserStateAsync()
    }

    fun toggleSound() {
        _uiState.update { it.copy(soundEnabled = !it.soundEnabled) }
        persistCurrentUserStateAsync()
    }

    fun toggleHaptics() {
        _uiState.update { it.copy(hapticsEnabled = !it.hapticsEnabled) }
        persistCurrentUserStateAsync()
    }

    fun openAdabDialog() {
        _uiState.update { it.copy(showAdabDialog = true) }
    }

    fun dismissAdabDialog() {
        _uiState.update { it.copy(showAdabDialog = false) }
    }

    fun openStatusDialog() {
        _uiState.update { it.copy(showStatusDialog = true) }
    }

    fun dismissStatusDialog() {
        _uiState.update { it.copy(showStatusDialog = false) }
    }

    fun requestResetData() {
        _uiState.update { it.copy(showResetConfirmDialog = true) }
    }

    fun cancelResetData() {
        _uiState.update { it.copy(showResetConfirmDialog = false) }
    }

    fun confirmResetData() {
        _uiState.update {
            it.copy(
                totalXp = 0,
                heartsState = HeartsState(),
                streakState = StreakState(),
                progressByHadithId = emptyMap(),
                bookmarkedHadithIds = emptySet(),
                completedQuickReview10Min = false,
                totalMinutesSpent = 0,
                activeLesson = null,
                lastLessonResult = null,
                showResetConfirmDialog = false,
                activeModal = null,
                activeTab = BottomNavTab.PATH,
                snackbarMessage = "تم حذف جميع البيانات وإعادة الضبط بنجاح"
            )
        }
        viewModelScope.launch {
            dao?.resetAllDataTransaction()
        }
    }

    fun exportProgressBackup(): String {
        val state = _uiState.value
        val summary = """{"version":1,"xp":${state.totalXp},"streak":${state.streakState.streakDays},"mastered":${state.masteredHadithCount}}"""
        _uiState.update {
            it.copy(snackbarMessage = "تم تجهيز نسخة التقدّم الاحتياطية بنجاح")
        }
        return summary
    }

    // ---------------------------------------------------------------------------
    // Local Pass-and-Play (لعب مع صاحبك — Section 6.10)
    // ---------------------------------------------------------------------------

    fun startPassAndPlay() {
        val state = _uiState.value
        val sampleQuestions = state.hadiths.take(4).mapNotNull { h ->
            state.questionsByHadithId[h.id]?.firstOrNull()
        }
        _uiState.update {
            it.copy(
                matchSession = MatchSession(questions = sampleQuestions),
                activeModal = ModalScreen.PassAndPlay
            )
        }
    }

    fun submitPassAndPlayAnswer(optionId: String) {
        _uiState.update { state ->
            val session = state.matchSession ?: return@update state
            if (session.isFinished) return@update state
            val currentQ = session.questions.getOrNull(session.currentTurnIndex) ?: return@update state
            val isCorrect = currentQ.options.find { it.id == optionId }?.correct == true
            val isPlayerOne = session.currentTurnIndex % 2 == 0
            val nextP1 = if (isPlayerOne && isCorrect) session.playerOneScore + 10 else session.playerOneScore
            val nextP2 = if (!isPlayerOne && isCorrect) session.playerTwoScore + 10 else session.playerTwoScore
            val nextTurn = session.currentTurnIndex + 1
            val done = nextTurn >= session.questions.size

            state.copy(
                matchSession = session.copy(
                    playerOneScore = nextP1,
                    playerTwoScore = nextP2,
                    currentTurnIndex = nextTurn,
                    isFinished = done
                )
            )
        }
    }

    fun closePassAndPlay() {
        _uiState.update {
            it.copy(
                matchSession = null,
                activeModal = null
            )
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // ---------------------------------------------------------------------------
    // Persistence Helpers (Room Transactions)
    // ---------------------------------------------------------------------------

    private fun toUserStateEntity(state: AppUiState): UserStateEntity {
        return UserStateEntity(
            id = 1,
            totalXp = state.totalXp,
            currentHearts = state.heartsState.currentHearts,
            lastHeartSpentEpochMillis = state.heartsState.lastHeartSpentEpochMillis,
            streakDays = state.streakState.streakDays,
            lastActiveEpochDay = state.streakState.lastActiveEpochDay,
            freezesAvailable = state.streakState.freezesAvailable,
            activeEpochDaysCsv = state.streakState.activeEpochDaysHistory.joinToString(","),
            clockRollbackCount = state.streakState.clockRollbackDiagnosticsCount,
            completedQuickReview10Min = state.completedQuickReview10Min,
            totalMinutesSpent = state.totalMinutesSpent,
            dailyGoalMinutes = state.dailyGoalMinutes,
            onboardingCompleted = true,
            themeMode = state.themeMode.name,
            largeFontEnabled = state.useLargeFont,
            tashkeelEnabled = state.showTashkeel,
            soundEnabled = state.soundEnabled,
            hapticsEnabled = state.hapticsEnabled
        )
    }

    private fun persistCurrentUserStateAsync() {
        val current = _uiState.value
        val dbDao = dao ?: return
        viewModelScope.launch {
            dbDao.upsertUserState(toUserStateEntity(current))
        }
    }

    private fun persistHadithProgressAsync(hadithId: String) {
        val current = _uiState.value
        val dbDao = dao ?: return
        val sm2 = current.progressByHadithId[hadithId] ?: Sm2State(hadithId = hadithId)
        val isBookmarked = current.bookmarkedHadithIds.contains(hadithId)
        viewModelScope.launch {
            dbDao.upsertHadithProgress(
                HadithProgressEntity(
                    hadithId = hadithId,
                    easeFactor = sm2.easeFactor,
                    intervalDays = sm2.intervalDays,
                    repetitions = sm2.repetitions,
                    dueEpochDay = sm2.dueEpochDay,
                    masteryPct = sm2.masteryPct,
                    lastGrade = sm2.lastGrade,
                    isBookmarked = isBookmarked,
                    completedCount = sm2.repetitions
                )
            )
        }
    }

    private fun persistActiveSessionAsync() {
        val current = _uiState.value
        val lesson = current.activeLesson ?: return
        val dbDao = dao ?: return
        val sessionEntity = ActiveLessonSessionEntity(
            id = 1,
            hadithId = lesson.hadithId,
            seed = lesson.seed,
            questionIdsCsv = lesson.questions.joinToString(",") { it.id },
            currentQuestionIndex = lesson.currentIndex,
            correctFirstTryCount = lesson.correctFirstTryCount,
            correctAfterRetryCount = lesson.correctAfterRetryCount,
            retriesCount = lesson.retriesCount,
            isReviewMode = lesson.isReviewMode
        )
        val userEntity = toUserStateEntity(current)
        viewModelScope.launch {
            dbDao.recordAnswerTransaction(sessionEntity, userEntity)
        }
    }

    private fun persistCompletedLessonAsync() {
        val current = _uiState.value
        val result = current.lastLessonResult ?: return
        val dbDao = dao ?: return
        val sm2 = current.progressByHadithId[result.hadithId] ?: Sm2State(hadithId = result.hadithId)
        val isBookmarked = current.bookmarkedHadithIds.contains(result.hadithId)
        val progressEntity = HadithProgressEntity(
            hadithId = result.hadithId,
            easeFactor = sm2.easeFactor,
            intervalDays = sm2.intervalDays,
            repetitions = sm2.repetitions,
            dueEpochDay = sm2.dueEpochDay,
            masteryPct = sm2.masteryPct,
            lastGrade = sm2.lastGrade,
            isBookmarked = isBookmarked,
            completedCount = sm2.repetitions
        )
        val userEntity = toUserStateEntity(current)
        viewModelScope.launch {
            dbDao.completeLessonTransaction(progressEntity, userEntity)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioController.release()
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NawawiaViewModel(
                        clock = container.clock,
                        dao = container.dao,
                        audioController = container.audioController,
                        contentResult = container.contentResult
                    ) as T
                }
            }
        }
    }
}
