package com.nawawia.ui.feature

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nawawia.domain.model.Hadith
import com.nawawia.domain.model.MatchSession
import com.nawawia.domain.model.ReviewMode
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.components.AnswerChoice
import com.nawawia.ui.components.AnswerChoiceState
import com.nawawia.ui.components.ChunkyButton
import com.nawawia.ui.components.ChunkyButtonSize
import com.nawawia.ui.components.ChunkyButtonVariant
import com.nawawia.ui.components.StateBox
import com.nawawia.ui.components.UiState
import com.nawawia.ui.theme.NawawiaTheme

/**
 * Review Hub Screen (المراجعة — Section 6.6).
 * Features the 10-minute 42-hadith review promise, SM-2 Smart SRS review, Hardest First review,
 * and local 2-player Pass-and-Play (لعب مع صاحبك).
 */
@Composable
fun ReviewScreen(
    state: AppUiState,
    onStartReviewMode: (ReviewMode) -> Unit,
    onStartPassAndPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .testTag("review_screen"),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "review_header") {
            Column {
                Text(
                    text = "المراجعة والتثبيت",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اختر نمط المراجعة المناسب لوقتك اليوم",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
            }
        }

        // Mode 1: Quick 10-Min Review
        item(key = "mode_quick") {
            ReviewModeCard(
                mode = ReviewMode.QUICK_10_MIN,
                questionCount = 42,
                icon = Icons.Default.Speed,
                variant = ChunkyButtonVariant.Primary,
                buttonTag = "review_mode_quick_button",
                onStartClick = { onStartReviewMode(ReviewMode.QUICK_10_MIN) }
            )
        }

        // Mode 2: Smart SRS
        item(key = "mode_smart") {
            ReviewModeCard(
                mode = ReviewMode.SMART_SRS,
                questionCount = 10,
                icon = Icons.Default.Psychology,
                variant = ChunkyButtonVariant.Gold,
                buttonTag = "review_mode_smart_button",
                onStartClick = { onStartReviewMode(ReviewMode.SMART_SRS) }
            )
        }

        // Mode 3: Hardest First
        item(key = "mode_hardest") {
            ReviewModeCard(
                mode = ReviewMode.HARDEST_FIRST,
                questionCount = 7,
                icon = Icons.Default.TrendingUp,
                variant = ChunkyButtonVariant.Tonal,
                buttonTag = "review_mode_hardest_button",
                onStartClick = { onStartReviewMode(ReviewMode.HARDEST_FIRST) }
            )
        }

        // Local Pass-and-Play (لعب مع صاحبك — Section 6.10)
        item(key = "pass_and_play") {
            val shape = RoundedCornerShape(20.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.surface, shape)
                    .border(2.dp, colors.outline, shape)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = colors.info,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "لعب مع صاحبك (جهاز واحد)",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.ink
                        )
                        Text(
                            text = "تحدٍّ محلي بين لاعبين بالتناوب دون إنترنت",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                ChunkyButton(
                    label = "ابدأ التحدي المحلي",
                    onClick = onStartPassAndPlayClick,
                    variant = ChunkyButtonVariant.Tonal,
                    size = ChunkyButtonSize.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_pass_and_play_button")
                )
            }
        }
    }
}

@Composable
private fun ReviewModeCard(
    mode: ReviewMode,
    questionCount: Int,
    icon: ImageVector,
    variant: ChunkyButtonVariant,
    buttonTag: String,
    onStartClick: () -> Unit
) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val arMins = ArabicNormalizer.formatNumber(mode.estimatedMinutes)
    val arQuestions = ArabicNormalizer.formatNumber(questionCount)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface, shape)
            .border(2.dp, colors.outline, shape)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = mode.titleAr,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink
                )
            }

            Text(
                text = "~$arMins دقائق • $arQuestions سؤال",
                style = MaterialTheme.typography.labelMedium,
                color = colors.brand
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = mode.subtitleAr,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        ChunkyButton(
            label = "ابدأ ${mode.titleAr}",
            onClick = onStartClick,
            variant = variant,
            size = ChunkyButtonSize.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(buttonTag)
        )
    }
}

/**
 * Searchable Hadith Index Screen (الأحاديث — Section 6.7).
 */
@Composable
fun IndexScreen(
    state: AppUiState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearchClick: () -> Unit,
    onSelectFilter: (IndexFilter) -> Unit,
    onHadithClick: (Hadith) -> Unit,
    onToggleBookmark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val filteredList = state.filteredIndexHadiths

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .testTag("index_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "فهرس الأربعين النووية",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.ink
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Input Field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "ابحث برقم الحديث أو العنوان أو المتن…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.inkMuted
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearchClick,
                            modifier = Modifier.testTag("index_search_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح البحث",
                                tint = colors.inkMuted
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("index_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = IndexFilter.entries,
                    key = { it.name }
                ) { filter ->
                    val selected = state.selectedFilter == filter
                    val chipShape = RoundedCornerShape(999.dp)
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(chipShape)
                            .background(if (selected) colors.brand else colors.surfaceAlt, chipShape)
                            .border(1.5.dp, if (selected) colors.brand else colors.outline, chipShape)
                            .clickable(role = Role.Tab) { onSelectFilter(filter) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag(filter.testTag),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filter.labelAr,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colors.onBrand else colors.ink
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = colors.outline)

        val listState = if (filteredList.isEmpty()) {
            UiState.Empty(
                title = "لا توجد أحاديث مطابقة",
                message = "جرّب البحث بكلمة أخرى أو تغيير التصفية لعرض جميع الأحاديث",
                actionLabel = "عرض الكل",
                onAction = {
                    onClearSearchClick()
                    onSelectFilter(IndexFilter.ALL)
                }
            )
        } else {
            UiState.Content(filteredList)
        }

        StateBox(
            state = listState,
            modifier = Modifier.weight(1f)
        ) { items ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("index_hadith_list"),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) { hadith ->
                    val masteryPct = state.progressByHadithId[hadith.id]?.masteryPct ?: 0
                    val isBookmarked = state.bookmarkedHadithIds.contains(hadith.id)
                    IndexHadithCard(
                        hadith = hadith,
                        masteryPct = masteryPct,
                        isBookmarked = isBookmarked,
                        onClick = { onHadithClick(hadith) },
                        onBookmarkToggle = { onToggleBookmark(hadith.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IndexHadithCard(
    hadith: Hadith,
    masteryPct: Int,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val arNumber = ArabicNormalizer.formatNumber(hadith.number)
    val arMastery = ArabicNormalizer.formatNumber(masteryPct)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface, shape)
            .border(1.5.dp, colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
            .testTag("index_hadith_row_${hadith.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "الحديث $arNumber",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.brand
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• إتقان $arMastery%",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hadith.titleAr,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hadith.takhrij.textAr,
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkMuted
            )
        }

        IconButton(
            onClick = onBookmarkToggle,
            modifier = Modifier
                .size(48.dp)
                .testTag("index_bookmark_${hadith.id}")
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isBookmarked) "إزالة من المحفوظات" else "إضافة للمحفوظات",
                tint = if (isBookmarked) colors.goldPress else colors.inkMuted
            )
        }
    }
}

/**
 * Pass-and-Play Local 2-Player Screen (لعب مع صاحبك — Section 6.10).
 */
@Composable
fun PassAndPlayScreen(
    session: MatchSession,
    onSelectOption: (String) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val p1ScoreAr = ArabicNormalizer.formatNumber(session.playerOneScore)
    val p2ScoreAr = ArabicNormalizer.formatNumber(session.playerTwoScore)

    BackHandler(onBack = onCloseClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(20.dp)
            .testTag("pass_and_play_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${session.playerOneName}: $p1ScoreAr",
                style = MaterialTheme.typography.titleMedium,
                color = colors.brand
            )
            Text(
                text = "${session.playerTwoName}: $p2ScoreAr",
                style = MaterialTheme.typography.titleMedium,
                color = colors.goldPress
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (session.isFinished) {
            Text(
                text = "انتهى التحدي!",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            ChunkyButton(
                label = "العودة للمراجعة",
                onClick = onCloseClick,
                variant = ChunkyButtonVariant.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pass_and_play_close_button")
            )
        } else {
            val currentQ = session.questions.getOrNull(session.currentTurnIndex)
            Text(
                text = "دور: ${session.activePlayerName}",
                style = MaterialTheme.typography.titleLarge,
                color = colors.brand
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (currentQ != null) {
                Text(
                    text = currentQ.promptAr,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(16.dp))
                for (opt in currentQ.options) {
                    AnswerChoice(
                        optionId = opt.id,
                        text = opt.textAr,
                        state = AnswerChoiceState.IDLE,
                        enabled = true,
                        onClick = { onSelectOption(opt.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            ChunkyButton(
                label = "إنهاء التحدي",
                onClick = onCloseClick,
                variant = ChunkyButtonVariant.Tonal,
                size = ChunkyButtonSize.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pass_and_play_close_button")
            )
        }
    }
}
