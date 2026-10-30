package com.nawawia.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nawawia.domain.model.HeartsState
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.theme.EaseOut
import com.nawawia.ui.theme.NawawiaDurations
import com.nawawia.ui.theme.NawawiaTheme

/**
 * 5-heart bar with animated scale on lost hearts and no layout shift.
 */
@Composable
fun HeartBar(
    heartsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val reduceMotion = NawawiaTheme.isReduceMotion
    val arHearts = ArabicNormalizer.formatNumber(heartsCount)
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .background(colors.surfaceAlt, shape)
            .border(1.5.dp, colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("heart_bar_button")
            .semantics {
                contentDescription = "القلوب المتبقية: $arHearts من ٥"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..HeartsState.MAX_HEARTS) {
            val filled = i <= heartsCount
            val scale by animateFloatAsState(
                targetValue = if (filled) 1f else 0.85f,
                animationSpec = if (reduceMotion) tween(0) else tween(
                    durationMillis = NawawiaDurations.AnswerSelect,
                    easing = EaseOut
                ),
                label = "heart_scale_$i"
            )
            Icon(
                imageVector = if (filled) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (filled) colors.heart else colors.inkMuted,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = arHearts,
            style = MaterialTheme.typography.labelLarge,
            color = colors.ink
        )
    }
}

/**
 * Streak counter pill with flame icon and milestone emphasis at 7 / 30 / 100 days.
 */
@Composable
fun StreakFlame(
    streakDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val arDays = ArabicNormalizer.formatNumber(streakDays)
    val isMilestone = streakDays >= 7
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .background(if (isMilestone) colors.gold else colors.surfaceAlt, shape)
            .border(1.5.dp, if (isMilestone) colors.goldPress else colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("streak_flame_button")
            .semantics {
                contentDescription = "سلسلة الأيام المتتالية: $arDays يوم"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Whatshot,
            contentDescription = null,
            tint = if (isMilestone) colors.onGold else if (streakDays > 0) colors.goldPress else colors.inkMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = arDays,
            style = MaterialTheme.typography.labelLarge,
            color = if (isMilestone) colors.onGold else colors.ink
        )
    }
}

/**
 * XP total badge pill.
 */
@Composable
fun XpPill(
    totalXp: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val arXp = ArabicNormalizer.formatNumber(totalXp)
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(colors.brandSoft, shape)
            .border(1.5.dp, colors.brand, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("xp_pill_badge")
            .semantics {
                contentDescription = "نقاط الخبرة: $arXp"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = colors.brand,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$arXp XP",
            style = MaterialTheme.typography.labelLarge,
            color = colors.ink
        )
    }
}

/**
 * Top status bar carrying StreakFlame, XpPill, and HeartBar.
 */
@Composable
fun TopGameBar(
    streakDays: Int,
    totalXp: Int,
    heartsCount: Int,
    onStreakClick: () -> Unit,
    onHeartsClick: () -> Unit,
    onXpClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreakFlame(
            streakDays = streakDays,
            onClick = onStreakClick
        )
        XpPill(totalXp = totalXp, onClick = onXpClick)
        HeartBar(
            heartsCount = heartsCount,
            onClick = onHeartsClick
        )
    }
}
