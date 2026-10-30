package com.nawawia.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.nawawia.core.clock.FakeClock
import com.nawawia.core.error.AppError
import com.nawawia.core.result.AppResult
import com.nawawia.data.audio.AudioController
import com.nawawia.data.content.ContentLoader
import com.nawawia.domain.model.NodeStatus
import com.nawawia.domain.model.QuestionType
import com.nawawia.ui.feature.BottomNavTab
import com.nawawia.ui.feature.IndexFilter
import com.nawawia.ui.feature.ModalScreen
import com.nawawia.ui.feature.NawawiaAppRoot
import com.nawawia.ui.feature.NawawiaViewModel
import com.nawawia.ui.feature.ThemeModeOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class FullAppNavigationAndButtonsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestViewModel(): NawawiaViewModel {
        val clock = FakeClock()
        val audioController = AudioController(null)
        val bundle = ContentLoader.buildVerifiedSyntheticBundle()
        return NawawiaViewModel(
            clock = clock,
            dao = null,
            audioController = audioController,
            contentResult = AppResult.Success(bundle)
        )
    }

    @Test
    fun test01_pathSingleCallToActionAndAllFourBottomNavigationTabsWork() {
        val vm = createTestViewModel()
        composeTestRule.setContent {
            NawawiaAppRoot(viewModel = vm)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("path_screen").assertIsDisplayed()

        // شاشة الطريق صار فيها نداء واحد: الخاتم النشط.
        // وقد حُذف «ابدأ التعلم» و«تابع من هنا» الثابت (كانا يستدعيان نفس الدالة
        // حرفيًا)، و«المراجعة السريعة» و«تصفح الأحاديث» (كانا يكرّران تبويبين في
        // الشريط السفلي). والتبويبان هما الطريق الصحيح إليهما.
        composeTestRule.onNodeWithTag("start_learning_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("hadith_reader_screen").assertIsDisplayed()
        assertThat(vm.uiState.value.activeModal).isEqualTo(ModalScreen.Reader("h1"))

        composeTestRule.onNodeWithTag("reader_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("path_screen").assertIsDisplayed()

        // المراجعة عبر تبويبها، لا عبر زِرّ مكرّر في الطريق.
        composeTestRule.onNodeWithTag("nav_tab_review").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("review_screen").assertIsDisplayed()

        // الفهرس عبر تبويبه.
        composeTestRule.onNodeWithTag("nav_tab_index").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("index_screen").assertIsDisplayed()
        assertThat(vm.uiState.value.activeTab).isEqualTo(BottomNavTab.INDEX)

        composeTestRule.onNodeWithTag("nav_tab_profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_path").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("path_screen").assertIsDisplayed()
    }

    @Test
    fun test02_hadithReaderAndAudioPlayerAllButtonsWork() {
        val vm = createTestViewModel()
        vm.openReader("h1")

        composeTestRule.setContent {
            NawawiaAppRoot(viewModel = vm)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("hadith_reader_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matn_card_h1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("takhrij_line").assertIsDisplayed()

        // Toggle Tashkeel button
        assertThat(vm.uiState.value.showTashkeel).isTrue()
        composeTestRule.onNodeWithTag("reader_tashkeel_toggle").performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value.showTashkeel).isFalse()

        // Toggle Font Size button
        assertThat(vm.uiState.value.useLargeFont).isFalse()
        composeTestRule.onNodeWithTag("reader_font_size_toggle").performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value.useLargeFont).isTrue()

        // Toggle Bookmark button
        assertThat(vm.uiState.value.bookmarkedHadithIds).doesNotContain("h1")
        composeTestRule.onNodeWithTag("reader_bookmark_toggle").performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value.bookmarkedHadithIds).contains("h1")

        // Click Copy button -> verifies attributed copy includes takhrij
        composeTestRule.onNodeWithTag("reader_copy_button").performClick()
        composeTestRule.waitForIdle()

        // ── الصوت ───────────────────────────────────────────────────────
        // لا تسجيلات بشرية مضمّنة في المستودع بعد (تنتظر إذن النشر من المصدر).
        // وقد حُذف محرّك النطق الآلي ومولّد النبرات، فالسلوك الصحيح الآن هو
        // **إعلان صريح بغياب التسجيل**، لا تشغيل بديل مزيّف.
        //
        // الاختبار السابق كان يمرّ لأن `isAudioFileAvailable` كان افتراضه `true`
        // بينما `assets/audio/` غير موجود أصلًا — أي أنه كان يُثبّت سلوكًا كاذبًا.
        composeTestRule.onNodeWithTag("reader_content_list").performScrollToKey("audio_bar")
        composeTestRule.waitForIdle()

        assertThat(vm.audioState.value.isAudioFileAvailable).isFalse()
        composeTestRule.onNodeWithTag("audio_missing_label").assertIsDisplayed()
        composeTestRule.onNodeWithTag("audio_retry_button").assertIsDisplayed()

        // ولا يُشغَّل شيء مهما ضُغط.
        composeTestRule.onNodeWithTag("audio_retry_button").performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.audioState.value.isPlaying).isFalse()

        // Click "ابدأ الاختبار" -> launches LessonScreen
        composeTestRule.onNodeWithTag("start_quiz_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lesson_screen").assertIsDisplayed()
    }

    @Test
    fun test03_completeFullLessonAcrossAllSixQuestionTypesAndUnlockNextHadith() {
        val vm = createTestViewModel()
        // Hadith 2 is initially LOCKED
        val h2 = vm.uiState.value.hadiths.first { it.id == "h2" }
        assertThat(vm.uiState.value.getNodeStatus(h2)).isEqualTo(NodeStatus.LOCKED)

        vm.startLessonForHadith("h1", explicitSeed = 42L)

        composeTestRule.setContent {
            NawawiaAppRoot(viewModel = vm)
        }
        composeTestRule.waitForIdle()

        // Step through all 6 questions of Hadith 1
        repeat(6) {
            val lesson = vm.uiState.value.activeLesson
            assertThat(lesson).isNotNull()
            val q = lesson!!.currentQuestion!!

            when (q.type) {
                QuestionType.ORDER_PHRASES -> {
                    // Test accessible up/down buttons first, then set exact order
                    composeTestRule.onNodeWithTag("phrase_down_0").performClick()
                    composeTestRule.waitForIdle()
                    vm.setExactPhraseOrderForTest(q.orderedPhrases)
                    composeTestRule.waitForIdle()
                }
                QuestionType.MATCH_PAIRS -> {
                    for (pair in q.matchPairs) {
                        composeTestRule.onNodeWithTag("match_right_${pair.id}").performClick()
                        composeTestRule.waitForIdle()
                        composeTestRule.onNodeWithTag("match_left_${pair.id}").performClick()
                        composeTestRule.waitForIdle()
                    }
                }
                else -> {
                    val correctOption = q.options.first { it.correct }
                    composeTestRule.onNodeWithTag("answer_choice_${correctOption.id}").performClick()
                    composeTestRule.waitForIdle()
                }
            }

            // Click "تحقّق" (Check Answer)
            composeTestRule.onNodeWithTag("lesson_check_button").performClick()
            composeTestRule.waitForIdle()
            assertThat(vm.uiState.value.activeLesson?.isAnswerRevealed).isTrue()
            assertThat(vm.uiState.value.activeLesson?.isCurrentAnswerCorrect).isTrue()

            // Click "متابعة" (Continue)
            composeTestRule.onNodeWithTag("lesson_continue_button").performClick()
            composeTestRule.waitForIdle()
        }

        // Verify ResultScreen is now displayed with earned XP and Hadith 2 is unlocked!
        composeTestRule.onNodeWithTag("result_screen").assertIsDisplayed()
        assertThat(vm.uiState.value.totalXp).isGreaterThan(0)
        assertThat(vm.uiState.value.getNodeStatus(h2)).isEqualTo(NodeStatus.AVAILABLE)

        // Click "الحديث اللي بعده" on ResultScreen -> opens Hadith 2 Reader!
        composeTestRule.onNodeWithTag("result_next_hadith_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("hadith_reader_screen").assertIsDisplayed()
        assertThat(vm.uiState.value.activeModal).isEqualTo(ModalScreen.Reader("h2"))
    }

    @Test
    fun test04_reviewModesPassAndPlayIndexFiltersAndSettingsAllButtonsWork() {
        val vm = createTestViewModel()
        composeTestRule.setContent {
            NawawiaAppRoot(viewModel = vm)
        }
        composeTestRule.waitForIdle()

        // 1. Go to Review Tab and test Pass-and-Play
        composeTestRule.onNodeWithTag("nav_tab_review").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("review_screen").performScrollToKey("pass_and_play")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("start_pass_and_play_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pass_and_play_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pass_and_play_close_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("review_screen").assertIsDisplayed()

        // 2. Go to Index Tab and test Search + Filter Chips + Bookmark + Row Click
        composeTestRule.onNodeWithTag("nav_tab_index").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("index_bookmark_h1").performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value.bookmarkedHadithIds).contains("h1")

        composeTestRule.onNodeWithTag("filter_bookmarked").performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value.selectedFilter).isEqualTo(IndexFilter.BOOKMARKED)
        assertThat(vm.uiState.value.filteredIndexHadiths.map { it.id }).containsExactly("h1")

        composeTestRule.onNodeWithTag("filter_all").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("index_search_input").performTextInput("42")
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value.filteredIndexHadiths.map { it.id }).contains("h42")

        composeTestRule.onNodeWithTag("index_search_clear").performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value.searchQuery).isEmpty()

        // 3. Go to Profile Tab -> open Adab Dialog -> open Settings Screen
        composeTestRule.onNodeWithTag("nav_tab_profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_adab_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("adab_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("adab_dialog_close").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("open_settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()

        // Toggle Dark Theme & Daily Goal in Settings
        composeTestRule.onNodeWithTag("theme_option_DARK").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value.themeMode).isEqualTo(ThemeModeOption.DARK)

        composeTestRule.onNodeWithTag("goal_option_20").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value.dailyGoalMinutes).isEqualTo(20)

        // Test Reset Data confirmation dialog
        composeTestRule.onNodeWithTag("settings_reset_data_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reset_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reset_confirm_yes").performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value.bookmarkedHadithIds).isEmpty()
    }

    @Test
    fun test05_contentUnavailableScreen_blocksAppWhenIntegrityFails() {
        val clock = FakeClock()
        val audioController = AudioController(null)
        val brokenVm = NawawiaViewModel(
            clock = clock,
            dao = null,
            audioController = audioController,
            contentResult = AppResult.Failure(AppError.ContentCorrupt("SHA-256 mismatch"))
        )

        composeTestRule.setContent {
            NawawiaAppRoot(viewModel = brokenVm)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("content_unavailable_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("copy_diagnostics_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun test06_authentic42Hadiths_node3Click_audioPlayer_andSharhDialog() {
        val vm = createTestViewModel()
        val hadiths = vm.uiState.value.hadiths
        assertThat(hadiths).hasSize(42)

        // Verify zero test fixtures or placeholder titles exist anywhere in the 42 Hadiths
        hadiths.forEach { h ->
            assertThat(h.isExample).isFalse()
            assertThat(h.titleAr).doesNotContain("عنوان اختباري")
            assertThat(h.titleAr).doesNotContain("TEST FIXTURE")
            assertThat(h.matnAr).doesNotContain("TEST FIXTURE")
            assertThat(h.sharhSummaryAr).isNotEmpty()
            assertThat(h.benefitsAr).isNotEmpty()
            assertThat(h.narratorBioAr).isNotEmpty()
        }

        val h3 = hadiths.first { it.number == 3 }
        assertThat(h3.titleAr).isEqualTo("بني الإسلام على خمس")
        assertThat(h3.matnAr).contains("بُنِيَ الإِسْلامُ عَلَى خَمْسٍ")

        composeTestRule.setContent {
            NawawiaAppRoot(viewModel = vm)
        }
        composeTestRule.waitForIdle()

        // Clicking Hadith 3 node on PathScreen immediately opens Hadith 3 Reader
        composeTestRule.onNodeWithTag("path_lazy_column").performScrollToKey("h3")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("path_node_3").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("hadith_reader_screen").assertIsDisplayed()
        assertThat(vm.uiState.value.activeModal).isEqualTo(ModalScreen.Reader("h3"))

        // Audio bar is at the top of HadithReaderScreen and plays Hadith 3 immediately when clicked
        composeTestRule.onNodeWithTag("audio_play_pause_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.audioState.value.isPlaying).isTrue()
        composeTestRule.onNodeWithTag("audio_time_counter").assertIsDisplayed()
        composeTestRule.onNodeWithTag("audio_active_segment_box").assertIsDisplayed()

        // Clicking Sharh row opens the in-app Sharh detail dialog
        composeTestRule.onNodeWithTag("reader_content_list").performScrollToKey("matn_card")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sharh_link_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sharh_detail_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sharh_dialog_close_button").performClick()
        composeTestRule.waitForIdle()

        // Start quiz for Hadith 3 and verify Lesson audio button works on every question
        composeTestRule.onNodeWithTag("start_quiz_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lesson_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lesson_play_audio_prompt").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertThat(vm.audioState.value.isPlaying).isTrue()
    }
}

