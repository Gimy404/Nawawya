package com.nawawia.ui.feature

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nawawia.domain.model.Question
import com.nawawia.domain.model.QuestionType
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.components.AnswerChoice
import com.nawawia.ui.components.AnswerChoiceState
import com.nawawia.ui.components.ChunkyButton
import com.nawawia.ui.components.ChunkyButtonSize
import com.nawawia.ui.components.ChunkyButtonVariant
import com.nawawia.ui.components.HeartBar
import com.nawawia.ui.components.SharhLinkRow
import com.nawawia.ui.theme.NawawiaTheme

@Composable
fun LessonScreen(
    lesson: ActiveLessonUiState,
    heartsCount: Int,
    audioState: com.nawawia.data.audio.AudioPlayerState = com.nawawia.data.audio.AudioPlayerState(),
    onRequestExitClick: () -> Unit,
    onHeartsClick: () -> Unit,
    onSelectOption: (String) -> Unit,
    onMovePhraseUp: (Int) -> Unit,
    onMovePhraseDown: (Int) -> Unit,
    onSelectMatchRight: (String) -> Unit,
    onSelectMatchLeft: (String) -> Unit,
    onPlayAudioPrompt: () -> Unit,
    onCheckAnswerClick: () -> Unit,
    onContinueClick: () -> Unit,
    onOpenSharhClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val currentQuestion = lesson.currentQuestion ?: return
    val arCurrentIndex = ArabicNormalizer.formatNumber(lesson.currentIndex + 1)
    val arTotalCount = ArabicNormalizer.formatNumber(lesson.questions.size)

    BackHandler(onBack = onRequestExitClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .testTag("lesson_screen")
    ) {
        // Top Lesson Bar: Close, Progress Bar (RTL right-to-left fill), HeartBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRequestExitClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("lesson_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إنهاء الدرس",
                    tint = colors.inkMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { lesson.progressFraction },
                    color = colors.brand,
                    trackColor = colors.surfaceAlt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .testTag("lesson_progress_bar")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "السؤال $arCurrentIndex من $arTotalCount • ${lesson.hadithTitleAr}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkMuted
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            HeartBar(
                heartsCount = heartsCount,
                onClick = onHeartsClick
            )
        }

        HorizontalDivider(thickness = 1.dp, color = colors.outline)

        // Question Body
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("lesson_question_body"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "question_type_badge") {
                val badgeText = when (currentQuestion.type) {
                    QuestionType.MCQ -> "اختيار من متعدد"
                    QuestionType.TRUE_FALSE -> "صواب أم خطأ"
                    QuestionType.FILL_BLANK -> "أكمل الفراغ"
                    QuestionType.ORDER_PHRASES -> "ترتيب المقاطع"
                    QuestionType.MATCH_PAIRS -> "صل العبارات"
                    QuestionType.AUDIO_RECOGNITION -> "تمييز سماعي"
                }
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.brand
                )
            }

            item(key = "question_prompt") {
                Text(
                    text = currentQuestion.promptAr,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.ink,
                    modifier = Modifier.testTag("question_prompt_text")
                )
            }

            item(key = "audio_prompt_button") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChunkyButton(
                        label = if (audioState.isPlaying) "إيقاف التلاوة الصوتية (${audioState.formattedTimeAr})" else "استمع للمقطع الصوتي للحديث",
                        leadingIcon = Icons.Default.VolumeUp,
                        onClick = onPlayAudioPrompt,
                        variant = if (audioState.isPlaying) ChunkyButtonVariant.Gold else ChunkyButtonVariant.Tonal,
                        size = ChunkyButtonSize.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lesson_play_audio_prompt")
                    )
                    if (audioState.isPlaying && audioState.activeSegmentTextAr.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.brandSoft, RoundedCornerShape(12.dp))
                                .border(1.dp, colors.brand, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .testTag("lesson_active_audio_excerpt")
                        ) {
                            Text(
                                text = "🔊 جاري تلاوة الحديث الشريف…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.brand
                            )
                        }
                    }
                }
            }

            when (currentQuestion.type) {
                QuestionType.ORDER_PHRASES -> {
                    itemsIndexed(
                        items = lesson.currentPhraseOrder,
                        key = { _, phrase -> phrase }
                    ) { index, phrase ->
                        PhraseOrderRow(
                            index = index,
                            totalCount = lesson.currentPhraseOrder.size,
                            phrase = phrase,
                            enabled = !lesson.isAnswerRevealed,
                            onMoveUp = { onMovePhraseUp(index) },
                            onMoveDown = { onMovePhraseDown(index) }
                        )
                    }
                }

                QuestionType.MATCH_PAIRS -> {
                    item(key = "match_pairs_grid") {
                        MatchPairsSection(
                            question = currentQuestion,
                            selectedRightId = lesson.selectedRightPairId,
                            selectedLeftId = lesson.selectedLeftPairId,
                            matchedIds = lesson.matchedPairIds,
                            enabled = !lesson.isAnswerRevealed,
                            onSelectRight = onSelectMatchRight,
                            onSelectLeft = onSelectMatchLeft
                        )
                    }
                }

                else -> {
                    items(
                        items = currentQuestion.options,
                        key = { it.id }
                    ) { option ->
                        val choiceState = when {
                            !lesson.isAnswerRevealed -> {
                                if (lesson.selectedOptionId == option.id) {
                                    AnswerChoiceState.SELECTED
                                } else {
                                    AnswerChoiceState.IDLE
                                }
                            }
                            option.id == lesson.selectedOptionId && option.correct -> AnswerChoiceState.CORRECT
                            option.id == lesson.selectedOptionId && !option.correct -> AnswerChoiceState.INCORRECT
                            option.correct -> AnswerChoiceState.REVEALED_CORRECT
                            else -> AnswerChoiceState.IDLE
                        }

                        AnswerChoice(
                            optionId = option.id,
                            text = option.textAr,
                            state = choiceState,
                            enabled = !lesson.isAnswerRevealed,
                            onClick = { onSelectOption(option.id) }
                        )
                    }
                }
            }
        }

        // Bottom Reveal / Check Action Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        !lesson.isAnswerRevealed -> colors.surface
                        lesson.isCurrentAnswerCorrect -> colors.brandSoft
                        else -> colors.wrongSoft
                    }
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (lesson.isAnswerRevealed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (lesson.isCurrentAnswerCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (lesson.isCurrentAnswerCorrect) colors.brand else colors.wrong,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lesson.isCurrentAnswerCorrect) "صحيح" else "غير صحيح",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (lesson.isCurrentAnswerCorrect) colors.brand else colors.wrong,
                            modifier = Modifier.testTag("lesson_feedback_title")
                        )
                        if (!currentQuestion.explanationAr.isNullOrBlank()) {
                            Text(
                                text = currentQuestion.explanationAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink
                            )
                        }
                    }
                }

                if (!lesson.isCurrentAnswerCorrect && currentQuestion.sharhUrl != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SharhLinkRow(
                        sharhUrl = currentQuestion.sharhUrl,
                        onClick = { onOpenSharhClick(currentQuestion.sharhUrl) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                ChunkyButton(
                    label = "متابعة",
                    onClick = onContinueClick,
                    variant = if (lesson.isCurrentAnswerCorrect) ChunkyButtonVariant.Primary else ChunkyButtonVariant.Danger,
                    size = ChunkyButtonSize.Large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lesson_continue_button")
                )
            } else {
                ChunkyButton(
                    label = "تحقّق",
                    onClick = onCheckAnswerClick,
                    enabled = lesson.canCheckAnswer,
                    variant = ChunkyButtonVariant.Primary,
                    size = ChunkyButtonSize.Large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lesson_check_button")
                )
            }
        }
    }
}

/**
 * Accessible non-drag phrase ordering item with explicit Up/Down buttons (Section 6.4 & 9.28).
 */
@Composable
private fun PhraseOrderRow(
    index: Int,
    totalCount: Int,
    phrase: String,
    enabled: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val arPosition = ArabicNormalizer.formatNumber(index + 1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(colors.surface, shape)
            .border(2.dp, colors.outline, shape)
            .clickable(enabled = enabled) {
                if (index > 0) onMoveUp() else onMoveDown()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("phrase_order_row_$index"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$arPosition. $phrase",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.ink,
            modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMoveUp,
                enabled = enabled && index > 0,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("phrase_up_$index")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "تحريك للأعلى",
                    tint = if (enabled && index > 0) colors.brand else colors.inkMuted
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = enabled && index < totalCount - 1,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("phrase_down_$index")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "تحريك للأسفل",
                    tint = if (enabled && index < totalCount - 1) colors.brand else colors.inkMuted
                )
            }
        }
    }
}

@Composable
private fun MatchPairsSection(
    question: Question,
    selectedRightId: String?,
    selectedLeftId: String?,
    matchedIds: Set<String>,
    enabled: Boolean,
    onSelectRight: (String) -> Unit,
    onSelectLeft: (String) -> Unit
) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(14.dp)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (pair in question.matchPairs) {
            val isMatched = matchedIds.contains(pair.id)
            val isRightSelected = selectedRightId == pair.id
            val isLeftSelected = selectedLeftId == pair.id

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Right item
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(shape)
                        .background(
                            when {
                                isMatched -> colors.brandSoft
                                isRightSelected -> colors.gold
                                else -> colors.surface
                            },
                            shape
                        )
                        .border(
                            2.dp,
                            if (isMatched || isRightSelected) colors.brand else colors.outline,
                            shape
                        )
                        .clickable(enabled = enabled && !isMatched, role = Role.Button) {
                            onSelectRight(pair.id)
                        }
                        .padding(12.dp)
                        .testTag("match_right_${pair.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pair.rightTextAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink,
                        textAlign = TextAlign.Center
                    )
                }

                // Left item
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(shape)
                        .background(
                            when {
                                isMatched -> colors.brandSoft
                                isLeftSelected -> colors.gold
                                else -> colors.surface
                            },
                            shape
                        )
                        .border(
                            2.dp,
                            if (isMatched || isLeftSelected) colors.brand else colors.outline,
                            shape
                        )
                        .clickable(enabled = enabled && !isMatched, role = Role.Button) {
                            onSelectLeft(pair.id)
                        }
                        .padding(12.dp)
                        .testTag("match_left_${pair.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pair.leftTextAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Lesson Result Screen (النتيجة — Section 6.5).
 */
@Composable
fun ResultScreen(
    result: LessonResultUiState,
    onNextHadithClick: () -> Unit,
    onBackToPathClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val cardShape = RoundedCornerShape(24.dp)
    val arXp = ArabicNormalizer.formatNumber(result.xpEarned)
    val arScore = ArabicNormalizer.formatNumber(result.scorePct)
    val arMastery = ArabicNormalizer.formatNumber(result.masteryPct)
    val arHearts = ArabicNormalizer.formatNumber(result.heartsRemaining)

    BackHandler(onBack = onBackToPathClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(24.dp)
            .testTag("result_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surface, cardShape)
                .border(2.dp, colors.brand, cardShape)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = colors.gold,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = result.encouragementTextAr,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = result.hadithTitleAr,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Big XP Number in displayLarge
            Text(
                text = "+$arXp XP",
                style = MaterialTheme.typography.displayLarge,
                color = colors.brand,
                modifier = Modifier.testTag("result_xp_earned")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultStatItem(label = "الدقة", value = "$arScore%")
                ResultStatItem(label = "الإتقان", value = "$arMastery%")
                ResultStatItem(label = "القلوب", value = arHearts)
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (result.nextHadithId != null) {
                ChunkyButton(
                    label = "الحديث اللي بعده",
                    onClick = onNextHadithClick,
                    variant = ChunkyButtonVariant.Primary,
                    size = ChunkyButtonSize.Large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_next_hadith_button")
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            ChunkyButton(
                label = "العودة للطريق",
                onClick = onBackToPathClick,
                variant = ChunkyButtonVariant.Tonal,
                size = ChunkyButtonSize.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result_back_to_path_button")
            )
        }
    }
}

@Composable
private fun ResultStatItem(label: String, value: String) {
    val colors = NawawiaTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted
        )
    }
}
