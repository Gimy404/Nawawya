package com.nawawia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nawawia.domain.model.HadithUnit
import com.nawawia.domain.model.NodeStatus
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.theme.NawawiaTheme
import kotlin.math.sin

/**
 * Pure deterministic function computing horizontal offset for the winding path based on node index.
 * Stable across recompositions and screen rotations (Section 6.2).
 */
fun computeWindingOffsetDp(indexInUnit: Int): Dp {
    val wave = sin(indexInUnit * 1.05).toFloat()
    return (wave * 44f).dp
}

/**
 * 72dp tactile circle node representing a single hadith on the learning path.
 * Never relies on color alone: combines fill + icon (Lock / Play / Star / Check) + Arabic label.
 */
@Composable
fun PathNode(
    hadithNumber: Int,
    hadithTitle: String,
    status: NodeStatus,
    masteryPct: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val arNumber = ArabicNormalizer.formatNumber(hadithNumber)
    val arMastery = ArabicNormalizer.formatNumber(masteryPct)

    val statusDescriptionAr = when (status) {
        NodeStatus.LOCKED -> "مغلق — أكمل الحديث السابق أولًا"
        NodeStatus.AVAILABLE -> "متاح للبدء الآن"
        NodeStatus.IN_PROGRESS -> "قيد التعلم — نسبة الإتقان $arMastery بالمئة"
        NodeStatus.MASTERED -> "متقَن بالكامل — نسبة الإتقان $arMastery بالمئة"
    }

    val (faceColor, lipColor, contentColor, statusIcon) = when (status) {
        NodeStatus.LOCKED -> Quadruple(
            colors.surfaceAlt,
            colors.outline,
            colors.inkMuted,
            Icons.Default.Lock
        )
        NodeStatus.AVAILABLE -> Quadruple(
            colors.brand,
            colors.brandPress,
            colors.onBrand,
            Icons.Default.PlayArrow
        )
        NodeStatus.IN_PROGRESS -> Quadruple(
            colors.brand,
            colors.brandPress,
            colors.onBrand,
            Icons.Default.Star
        )
        NodeStatus.MASTERED -> Quadruple(
            colors.gold,
            colors.goldPress,
            colors.onGold,
            Icons.Default.Check
        )
    }

    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(6.dp)
            .testTag("path_node_$hadithNumber")
            .semantics(mergeDescendants = true) {
                contentDescription = "الحديث $arNumber: $hadithTitle — الحالة: $statusDescriptionAr"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(84.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer progress ring for IN_PROGRESS or MASTERED
            val ringProgress = when (status) {
                NodeStatus.MASTERED -> 1f
                NodeStatus.IN_PROGRESS -> (masteryPct / 100f).coerceIn(0.15f, 0.95f)
                NodeStatus.AVAILABLE -> 0.1f
                NodeStatus.LOCKED -> 0f
            }

            if (ringProgress > 0f) {
                val trackColor = colors.outline
                val arcColor = if (status == NodeStatus.MASTERED) colors.gold else colors.brand
                Canvas(modifier = Modifier.size(84.dp)) {
                    val strokeWidth = 4.dp.toPx()
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // 72dp Circle with 3D bottom lip
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(lipColor, CircleShape)
                    .padding(bottom = 5.dp)
                    .clip(CircleShape)
                    .background(faceColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = arNumber,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "الحديث $arNumber",
            style = MaterialTheme.typography.labelMedium,
            color = if (status == NodeStatus.LOCKED) colors.inkMuted else colors.ink,
            textAlign = TextAlign.Center
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * Sticky Unit Banner displaying unit number, title, "٧ أحاديث", mastery count, and a Tonal action button.
 */
@Composable
fun UnitBanner(
    unit: HadithUnit,
    completedCount: Int,
    onContinueUnitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val arOrder = ArabicNormalizer.formatNumber(unit.order)
    val arCompleted = ArabicNormalizer.formatNumber(completedCount)
    val arTotal = ArabicNormalizer.formatNumber(unit.hadithIds.size)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.brandSoft, shape)
            .border(2.dp, colors.brand, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("unit_banner_${unit.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "الوحدة $arOrder • $arTotal أحاديث",
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = unit.titleAr,
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "الإنجاز: $arCompleted / $arTotal",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.brand
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        ChunkyButton(
            label = "ابدأ الوحدة",
            onClick = onContinueUnitClick,
            variant = ChunkyButtonVariant.Tonal,
            size = ChunkyButtonSize.Medium,
            modifier = Modifier.testTag("unit_start_button_${unit.id}")
        )
    }
}
