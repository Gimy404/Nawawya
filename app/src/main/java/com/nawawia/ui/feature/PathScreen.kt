package com.nawawia.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nawawia.domain.model.Hadith
import com.nawawia.domain.model.HadithUnit
import com.nawawia.domain.model.NodeStatus
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.components.ChunkyButton
import com.nawawia.ui.components.ChunkyButtonSize
import com.nawawia.ui.components.ChunkyButtonVariant
import com.nawawia.ui.components.IsnadSeal
import com.nawawia.ui.components.PathMetrics
import com.nawawia.ui.components.SealGloss
import com.nawawia.ui.components.TopGameBar
import com.nawawia.ui.components.UnitLampNode
import com.nawawia.ui.components.UnwanBanner
import com.nawawia.ui.components.isnadRibbon
import com.nawawia.ui.theme.NawawiaTheme
import kotlinx.coroutines.launch

/**
 * الطريق — سلسلة الإسناد.
 *
 * **ما تغيّر عن النسخة السابقة، ولماذا:** كانت هذه الشاشة تحمل ستّة نداءات متنافسة
 * («ابدأ التعلم»، و«تابع من هنا» وكلاهما يستدعي نفس الدالة حرفيًا، و«المراجعة
 * السريعة» و«تصفح الأحاديث» وكلاهما يكرّر تبويبًا في الشريط السفلي، و«ابدأ الوحدة»
 * لكل وحدة، والعقدة نفسها). فلم يكن المستخدم يعرف أين يضغط.
 *
 * الآن **نداء واحد**: الخاتم النشط، وعليه هالة ذهبية. وزِرّ «تابع من هنا» لا يظهر
 * إلا إذا خرج الخاتم النشط عن الشاشة بالتمرير — فهو عودة، لا تكرار.
 */
private sealed interface PathEntry {
    val key: String

    data class Unwan(
        val unit: HadithUnit,
        val masteredCount: Int,
        val firstNumber: Int,
        val lastNumber: Int
    ) : PathEntry {
        override val key: String get() = "unwan_${unit.id}"
    }

    data class Seal(
        val hadith: Hadith,
        val status: NodeStatus,
        val masteryPct: Int,
        val isFirstInUnit: Boolean,
        val isLastInUnit: Boolean
    ) : PathEntry {
        override val key: String get() = "seal_${hadith.id}"
    }

    data class Lamp(val unit: HadithUnit, val isLit: Boolean) : PathEntry {
        override val key: String get() = "lamp_${unit.id}"
    }
}

@Composable
fun PathScreen(
    state: AppUiState,
    onNodeClick: (Hadith) -> Unit,
    onStreakClick: () -> Unit,
    onHeartsClick: () -> Unit,
    onXpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val entries = remember(state.units, state.hadiths, state.progressByHadithId) {
        buildList {
            for (unit in state.units) {
                val unitHadiths = state.hadiths.filter { it.unitId == unit.id }
                if (unitHadiths.isEmpty()) continue
                val mastered = unitHadiths.count {
                    (state.progressByHadithId[it.id]?.masteryPct ?: 0) >= Mastery.MASTERED
                }
                add(
                    PathEntry.Unwan(
                        unit = unit,
                        masteredCount = mastered,
                        firstNumber = unitHadiths.first().number,
                        lastNumber = unitHadiths.last().number
                    )
                )
                unitHadiths.forEachIndexed { index, hadith ->
                    add(
                        PathEntry.Seal(
                            hadith = hadith,
                            status = state.getNodeStatus(hadith),
                            masteryPct = state.progressByHadithId[hadith.id]?.masteryPct ?: 0,
                            isFirstInUnit = index == 0,
                            isLastInUnit = index == unitHadiths.lastIndex
                        )
                    )
                }
                add(PathEntry.Lamp(unit = unit, isLit = mastered == unitHadiths.size))
            }
        }
    }

    val activeIndex = remember(entries) {
        entries.indexOfFirst {
            it is PathEntry.Seal &&
                (it.status == NodeStatus.AVAILABLE || it.status == NodeStatus.IN_PROGRESS)
        }
    }
    val isActiveOffScreen by remember {
        derivedStateOf {
            activeIndex >= 0 &&
                listState.layoutInfo.visibleItemsInfo.none { it.index == activeIndex }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .testTag("path_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopGameBar(
                streakDays = state.streakState.streakDays,
                totalXp = state.totalXp,
                heartsCount = state.heartsState.currentHearts,
                onStreakClick = onStreakClick,
                onHeartsClick = onHeartsClick,
                onXpClick = onXpClick
            )
            HorizontalDivider(thickness = 1.dp, color = colors.outline)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("path_lazy_column"),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(items = entries, key = { it.key }) { entry ->
                    when (entry) {
                        is PathEntry.Unwan -> Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                        ) {
                            UnwanBanner(
                                unit = entry.unit,
                                masteredCount = entry.masteredCount,
                                firstHadithNumber = entry.firstNumber,
                                lastHadithNumber = entry.lastNumber
                            )
                        }

                        is PathEntry.Seal -> SealRow(
                            entry = entry,
                            onClick = { onNodeClick(entry.hadith) }
                        )

                        is PathEntry.Lamp -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .isnadRibbon(
                                    color = colors.outline,
                                    inset = PathMetrics.RingSize / 2,
                                    topOpen = true,
                                    bottomOpen = false
                                )
                        ) {
                            UnitLampNode(unitOrder = entry.unit.order, isLit = entry.isLit)
                        }
                    }
                }
            }
        }

        // عودة إلى الخاتم النشط — لا تظهر إلا إذا غاب عن الشاشة.
        if (isActiveOffScreen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                ChunkyButton(
                    label = "تابع من هنا",
                    leadingIcon = Icons.Default.ArrowDownward,
                    onClick = { scope.launch { listState.animateScrollToItem(activeIndex) } },
                    variant = ChunkyButtonVariant.Primary,
                    size = ChunkyButtonSize.Medium,
                    modifier = Modifier.testTag("continue_here_fab_button")
                )
            }
        }
    }
}

@Composable
private fun SealRow(
    entry: PathEntry.Seal,
    onClick: () -> Unit
) {
    val colors = NawawiaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PathMetrics.RowHeight)
            .isnadRibbon(
                color = colors.outline,
                inset = PathMetrics.RingSize / 2,
                topOpen = !entry.isFirstInUnit,
                bottomOpen = true
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IsnadSeal(
            hadithNumber = entry.hadith.number,
            hadithTitle = entry.hadith.titleAr,
            status = entry.status,
            masteryPct = entry.masteryPct,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (entry.status == NodeStatus.AVAILABLE) {
                StartCue(hadithNumber = entry.hadith.number, onClick = onClick)
                Spacer(modifier = Modifier.height(7.dp))
            }
            SealGloss(
                title = entry.hadith.titleAr,
                isLocked = entry.status == NodeStatus.LOCKED,
                masteryPct = entry.masteryPct
            )
        }
    }
}

/** فقاعة «ابدأ» — النداء الوحيد في الشاشة. */
@Composable
private fun StartCue(hadithNumber: Int, onClick: () -> Unit) {
    ChunkyButton(
        label = "ابدأ الحديث ${ArabicNormalizer.formatNumber(hadithNumber)}",
        onClick = onClick,
        variant = ChunkyButtonVariant.Primary,
        size = ChunkyButtonSize.Small,
        modifier = Modifier.testTag("start_learning_button")
    )
}
