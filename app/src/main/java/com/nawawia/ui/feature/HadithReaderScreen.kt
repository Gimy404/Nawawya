package com.nawawia.ui.feature

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.nawawia.data.audio.AudioPlayerState
import com.nawawia.domain.model.Hadith
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.components.AudioPlayerBar
import com.nawawia.ui.components.ChunkyButton
import com.nawawia.ui.components.ChunkyButtonSize
import com.nawawia.ui.components.ChunkyButtonVariant
import com.nawawia.ui.components.MatnCard
import com.nawawia.ui.theme.NawawiaTheme

/**
 * Sacred Hadith Reader & Audio Screen (الحديث — Section 6.3).
 *
 * Features:
 * - Immediate top AudioPlayerBar with real Arabic TTS recitation, speed, A-B loop, and repeat
 * - Sacred MatnCard in Noto Naskh Arabic with tashkeel toggle & font size toggle
 * - Attributed Copy with Takhrij
 * - In-app Scholarly Sharh, Extracted Benefits (الفوائد), Key Vocabulary (معاني المفردات), and Narrator Bio (ترجمة الراوي)
 * - Direct "ابدأ الاختبار" CTA to launch the 6-format interactive quiz
 */
@Composable
fun HadithReaderScreen(
    hadith: Hadith,
    showTashkeel: Boolean,
    useLargeFont: Boolean,
    isBookmarked: Boolean,
    audioState: AudioPlayerState,
    onBackClick: () -> Unit,
    onToggleTashkeel: () -> Unit,
    onToggleLargeFont: () -> Unit,
    onToggleBookmark: () -> Unit,
    onCopyHadith: () -> String,
    onOpenSharhUrl: (String) -> Unit,
    onPlayPauseAudio: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onCycleAudioSpeed: () -> Unit,
    onToggleAbLoop: () -> Unit,
    onCycleRepeat: () -> Unit,
    onRetryAudio: () -> Unit,
    onStartQuizClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val clipboard = LocalClipboardManager.current
    val arNumber = ArabicNormalizer.formatNumber(hadith.number)
    var showInAppSharhDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .testTag("hadith_reader_screen")
    ) {
        // Reader Top App Bar with Back, Tashkeel toggle, Font Size toggle, Bookmark, and Copy
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = colors.ink
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "الحديث $arNumber من ٤٢",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.brand
                    )
                    Text(
                        text = hadith.titleAr,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tashkeel toggle
                IconButton(
                    onClick = onToggleTashkeel,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_tashkeel_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Spellcheck,
                        contentDescription = if (showTashkeel) "إخفاء التشكيل" else "إظهار التشكيل",
                        tint = if (showTashkeel) colors.brand else colors.inkMuted
                    )
                }

                // Font Size toggle
                IconButton(
                    onClick = onToggleLargeFont,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_font_size_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = if (useLargeFont) "حجم خط عادي" else "تكبير الخط",
                        tint = if (useLargeFont) colors.brand else colors.inkMuted
                    )
                }

                // Bookmark toggle
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_bookmark_toggle")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "إزالة من المحفوظات" else "حفظ الحديث",
                        tint = if (isBookmarked) colors.goldPress else colors.inkMuted
                    )
                }

                // Copy with automatic Takhrij attribution
                IconButton(
                    onClick = {
                        val attributed = onCopyHadith()
                        clipboard.setText(AnnotatedString(attributed))
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ الحديث مع التخريج",
                        tint = colors.inkMuted
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = colors.outline)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("reader_content_list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Audio Player Bar at the top so "استمع" is immediately visible without scrolling
            item(key = "audio_bar") {
                AudioPlayerBar(
                    audioState = audioState,
                    onPlayPauseClick = onPlayPauseAudio,
                    onSeek = onSeekAudio,
                    onCycleSpeedClick = onCycleAudioSpeed,
                    onToggleAbLoopClick = onToggleAbLoop,
                    onCycleRepeatClick = onCycleRepeat,
                    onRetryClick = onRetryAudio
                )
            }

            // 2. Sacred Matn Card
            item(key = "matn_card") {
                MatnCard(
                    hadith = hadith,
                    showTashkeel = showTashkeel,
                    useLargeFont = useLargeFont,
                    onOpenSharhClick = {
                        showInAppSharhDialog = true
                        onOpenSharhUrl(it)
                    }
                )
            }

            // 3. Key Vocabulary Terms (معاني المفردات)
            if (hadith.keyTermsAr.isNotEmpty()) {
                item(key = "key_terms_header") {
                    Text(
                        text = "معاني المفردات",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink
                    )
                }

                items(
                    items = hadith.keyTermsAr,
                    key = { it.term }
                ) { termItem ->
                    val termShape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(termShape)
                            .background(colors.surface, termShape)
                            .border(1.5.dp, colors.outline, termShape)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = termItem.term,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.brand
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = termItem.glossAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink
                        )
                    }
                }
            }

            // 4. Comprehensive Explanation (الشرح الإجمالي والفوائد)
            if (hadith.sharhSummaryAr.isNotBlank() || hadith.benefitsAr.isNotEmpty()) {
                item(key = "sharh_and_benefits") {
                    val cardShape = RoundedCornerShape(18.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.surface, cardShape)
                            .border(1.5.dp, colors.outline, cardShape)
                            .padding(18.dp)
                            .testTag("reader_sharh_section")
                    ) {
                        Text(
                            text = "الشرح الإجمالي للحديث",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.brand
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = hadith.sharhSummaryAr,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.ink
                        )

                        if (hadith.benefitsAr.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(thickness = 1.dp, color = colors.outline)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "من فوائد الحديث وأحكامه:",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            for (benefit in hadith.benefitsAr) {
                                Text(
                                    text = "• $benefit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.ink,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }

                        if (hadith.narratorBioAr.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(thickness = 1.dp, color = colors.outline)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ترجمة راوي الحديث:",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.inkMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = hadith.narratorBioAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inkMuted
                            )
                        }
                    }
                }
            }
        }

        // Bottom CTA Bar: Start Quiz
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            ChunkyButton(
                label = "ابدأ الاختبار في الحديث $arNumber",
                leadingIcon = Icons.Default.Quiz,
                onClick = onStartQuizClick,
                variant = ChunkyButtonVariant.Primary,
                size = ChunkyButtonSize.Large,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_quiz_button")
            )
        }
    }

    if (showInAppSharhDialog) {
        AlertDialog(
            onDismissRequest = { showInAppSharhDialog = false },
            title = {
                Text(
                    text = "شرح الحديث $arNumber: ${hadith.titleAr}",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = hadith.sharhSummaryAr.ifBlank { "راجع شرح الحديث في كتاب جامع العلوم والحكم لابن رجب وشرح الأربعين النووية." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink
                    )
                    for (benefit in hadith.benefitsAr) {
                        Text(
                            text = "• $benefit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.brand
                        )
                    }
                }
            },
            confirmButton = {
                ChunkyButton(
                    label = "إغلاق",
                    onClick = { showInAppSharhDialog = false },
                    variant = ChunkyButtonVariant.Primary,
                    size = ChunkyButtonSize.Medium,
                    modifier = Modifier.testTag("sharh_dialog_close_button")
                )
            },
            containerColor = colors.surface,
            modifier = Modifier.testTag("sharh_detail_dialog")
        )
    }
}
