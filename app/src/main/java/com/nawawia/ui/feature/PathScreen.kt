package com.nawawia.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nawawia.domain.model.Hadith
import com.nawawia.domain.model.HadithUnit
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.components.ChunkyButton
import com.nawawia.ui.components.ChunkyButtonSize
import com.nawawia.ui.components.ChunkyButtonVariant
import com.nawawia.ui.components.PathNode
import com.nawawia.ui.components.TopGameBar
import com.nawawia.ui.components.UnitBanner
import com.nawawia.ui.components.computeWindingOffsetDp
import com.nawawia.ui.theme.NawawiaTheme

@Composable
fun PathScreen(
    state: AppUiState,
    onStartLearningClick: () -> Unit,
    onQuickReviewClick: () -> Unit,
    onBrowseHadithsClick: () -> Unit,
    onNodeClick: (Hadith) -> Unit,
    onUnitStartClick: (HadithUnit) -> Unit,
    onStreakClick: () -> Unit,
    onHeartsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val currentHadith = state.currentContinueHadith
    val arCurrentNum = ArabicNormalizer.formatNumber(currentHadith?.number ?: 1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .testTag("path_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with StreakFlame, XpPill, HeartBar
            TopGameBar(
                streakDays = state.streakState.streakDays,
                totalXp = state.totalXp,
                heartsCount = state.heartsState.currentHearts,
                onStreakClick = onStreakClick,
                onHeartsClick = onHeartsClick
            )

            HorizontalDivider(thickness = 1.dp, color = colors.outline)

            // Winding Path LazyColumn with stable keys
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("path_lazy_column"),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Quick-Action Card at top of Path
                item(key = "hero_welcome_card") {
                    val cardShape = RoundedCornerShape(20.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.surface, cardShape)
                            .border(2.dp, colors.outline, cardShape)
                            .padding(20.dp)
                            .testTag("path_hero_card"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "نواوية — الأربعون النووية",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.brand,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تسمع، تراجع، وتُسأل في ١٠ دقائق يوميًا — ابدأ من الحديث رقم $arCurrentNum",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        ChunkyButton(
                            label = "ابدأ التعلم",
                            leadingIcon = Icons.Default.PlayArrow,
                            onClick = onStartLearningClick,
                            variant = ChunkyButtonVariant.Primary,
                            size = ChunkyButtonSize.Large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_learning_button")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ChunkyButton(
                                label = "المراجعة السريعة (١٠ دقائق)",
                                leadingIcon = Icons.Default.Timer,
                                onClick = onQuickReviewClick,
                                variant = ChunkyButtonVariant.Gold,
                                size = ChunkyButtonSize.Medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_review_home_button")
                            )

                            ChunkyButton(
                                label = "تصفح الأحاديث",
                                leadingIcon = Icons.Default.MenuBook,
                                onClick = onBrowseHadithsClick,
                                variant = ChunkyButtonVariant.Tonal,
                                size = ChunkyButtonSize.Medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("browse_hadiths_home_button")
                            )
                        }
                    }
                }

                // Render all 6 units and their 7 hadith nodes
                for (unit in state.units) {
                    val unitHadiths = state.hadiths.filter { it.unitId == unit.id }
                    val completedInUnit = unitHadiths.count { h ->
                        (state.progressByHadithId[h.id]?.masteryPct ?: 0) >= 60
                    }

                    item(key = "unit_header_${unit.id}") {
                        UnitBanner(
                            unit = unit,
                            completedCount = completedInUnit,
                            onContinueUnitClick = { onUnitStartClick(unit) }
                        )
                    }

                    itemsIndexed(
                        items = unitHadiths,
                        key = { _, hadith -> hadith.id }
                    ) { idxInUnit, hadith ->
                        val status = state.getNodeStatus(hadith)
                        val mastery = state.progressByHadithId[hadith.id]?.masteryPct ?: 0
                        val offsetDp = computeWindingOffsetDp(idxInUnit)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PathNode(
                                hadithNumber = hadith.number,
                                hadithTitle = hadith.titleAr,
                                status = status,
                                masteryPct = mastery,
                                onClick = { onNodeClick(hadith) },
                                modifier = Modifier.offset(x = offsetDp)
                            )
                        }
                    }
                }
            }

            // Sticky "تابع من هنا" CTA bar above bottom navigation bar (non-overlapping)
            HorizontalDivider(thickness = 1.dp, color = colors.outline)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                ChunkyButton(
                    label = "تابع من هنا (الحديث $arCurrentNum)",
                    leadingIcon = Icons.Default.PlayArrow,
                    onClick = onStartLearningClick,
                    variant = ChunkyButtonVariant.Primary,
                    size = ChunkyButtonSize.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continue_here_fab_button")
                )
            }
        }
    }
}
