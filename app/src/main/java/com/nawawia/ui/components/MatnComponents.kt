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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nawawia.domain.model.Hadith
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.theme.NawawiaTheme

/**
 * Sacred-text surface (Section 5.6 #7):
 * - Uses Noto Naskh Arabic exclusively for narrator and matn
 * - Never truncates or ellipsizes sacred text
 * - Always renders the takhrij attribution line
 * - Contains zero game chrome (no XP, no hearts, no playful font)
 */
@Composable
fun MatnCard(
    hadith: Hadith,
    showTashkeel: Boolean,
    useLargeFont: Boolean,
    onOpenSharhClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val customType = NawawiaTheme.customTypography
    // كتلة المتن ليست بطاقة: لا إطار كامل ولا استدارة ٢٠dp كبقية العناصر.
    // هي كتلة مسطورة على هامش مُذهَّب — كما يُفرد المتن في المخطوط عن حواشيه.
    val shape = RoundedCornerShape(3.dp)

    val displayMatn = if (showTashkeel) {
        hadith.matnAr
    } else {
        ArabicNormalizer.stripTashkeelOnly(hadith.matnAr)
    }

    val matnTextStyle = if (useLargeFont) customType.matnLarge else customType.matn
    val goldEdge = colors.gold

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceAlt, shape)
            .drawBehind {
                // هامش التذهيب على جانب البدء (اليمين في RTL).
                val w = 3.dp.toPx()
                drawRect(
                    color = goldEdge,
                    topLeft = Offset(size.width - w, 0f),
                    size = Size(w, size.height)
                )
            }
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp)
            .testTag("matn_card_${hadith.id}")
            .semantics(mergeDescendants = false) {
                contentDescription = "متن الحديث: ${hadith.titleAr}"
            }
    ) {
        // Narrator line (in Noto Naskh Arabic)
        if (hadith.narratorAr.isNotBlank()) {
            Text(
                text = hadith.narratorAr,
                style = customType.narrator,
                color = colors.inkMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Sacred Matn text — never truncated, never ellipsized, selectable
        SelectionContainer {
            Text(
                text = displayMatn,
                style = matnTextStyle,
                color = colors.ink,
                softWrap = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("matn_text_display")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = colors.outline)
        Spacer(modifier = Modifier.height(10.dp))

        // التخريج لا يُحذف أبدًا، ويُرسم بحِبر الترقيم كما في المخطوط.
        Text(
            text = hadith.takhrij.textAr,
            style = customType.takhrij,
            color = colors.rubric,
            modifier = Modifier.testTag("takhrij_line")
        )

        Spacer(modifier = Modifier.height(12.dp))

        SharhLinkRow(
            sharhUrl = hadith.sharhUrl,
            onClick = { onOpenSharhClick(hadith.sharhUrl) }
        )
    }
}

/**
 * External scholarly explanation link row (Section 5.6 #10 & Section 6.12).
 */
@Composable
fun SharhLinkRow(
    sharhUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(colors.surfaceAlt, shape)
            .border(1.dp, colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("sharh_link_button")
            .semantics {
                contentDescription = "شرح الحديث في الدرر السنية — يفتح في متصفح خارجي"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "شرح الحديث في الدرر السنية",
                style = MaterialTheme.typography.titleMedium,
                color = colors.info
            )
            Text(
                text = "يفتح مصدر الشرح الموثوق في نافذة خارجية",
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkMuted
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = colors.info,
            modifier = Modifier.size(20.dp)
        )
    }
}
