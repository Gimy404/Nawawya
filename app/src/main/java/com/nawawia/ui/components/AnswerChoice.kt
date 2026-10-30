package com.nawawia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.nawawia.ui.theme.NawawiaTheme

enum class AnswerChoiceState {
    IDLE,
    SELECTED,
    CORRECT,
    INCORRECT,
    REVEALED_CORRECT
}

/**
 * Tactile quiz option row (Section 5.6 #8).
 * Never uses color alone: pairs fill + icon + explicit Arabic status label.
 */
@Composable
fun AnswerChoice(
    optionId: String,
    text: String,
    state: AnswerChoiceState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(16.dp)

    val (bgColor, borderColor, textColor, icon, statusWordAr) = when (state) {
        AnswerChoiceState.IDLE -> Quintuple(
            colors.surface,
            colors.outline,
            colors.ink,
            Icons.Default.RadioButtonUnchecked,
            null
        )
        AnswerChoiceState.SELECTED -> Quintuple(
            colors.brandSoft,
            colors.brand,
            colors.ink,
            Icons.Default.RadioButtonChecked,
            "محدّد"
        )
        AnswerChoiceState.CORRECT -> Quintuple(
            colors.correctSoft,
            colors.correct,
            colors.ink,
            Icons.Default.CheckCircle,
            "صحيح"
        )
        AnswerChoiceState.INCORRECT -> Quintuple(
            colors.wrongSoft,
            colors.wrong,
            colors.ink,
            Icons.Default.Close,
            "غير صحيح"
        )
        AnswerChoiceState.REVEALED_CORRECT -> Quintuple(
            colors.correctSoft,
            colors.correct,
            colors.ink,
            Icons.Default.CheckCircle,
            "الإجابة الصحيحة"
        )
    }

    val iconTint = when (state) {
        AnswerChoiceState.IDLE -> colors.inkMuted
        AnswerChoiceState.SELECTED -> colors.brand
        AnswerChoiceState.CORRECT,
        AnswerChoiceState.REVEALED_CORRECT -> colors.correct
        AnswerChoiceState.INCORRECT -> colors.wrong
    }

    val stateDesc = statusWordAr ?: "غير محدّد"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(bgColor, shape)
            .border(2.dp, borderColor, shape)
            .then(
                if (enabled) {
                    Modifier.clickable(role = Role.RadioButton, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("answer_choice_$optionId")
            .semantics {
                role = Role.RadioButton
                selected = (state == AnswerChoiceState.SELECTED || state == AnswerChoiceState.CORRECT)
                stateDescription = stateDesc
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
                if (statusWordAr != null) {
                    Text(
                        text = statusWordAr,
                        style = MaterialTheme.typography.labelMedium,
                        color = iconTint
                    )
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
