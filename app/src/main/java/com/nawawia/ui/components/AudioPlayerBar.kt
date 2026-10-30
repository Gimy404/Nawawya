package com.nawawia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nawawia.data.audio.AudioPlayerState
import com.nawawia.domain.voice.NawawiaVoice
import com.nawawia.ui.theme.NawawiaTheme
import com.nawawia.ui.theme.AmiriFontFamily

/**
 * First-class audio player bar for Listen -> Shadow -> Recall (Section 5.6 #9).
 * Supports Play/Pause with real Arabic TTS & acoustic playback, live time counter,
 * active phrase highlight, Scrubber, Speed (0.75x / 1x / 1.25x), A-B Loop, Repeat (1 / 3 / 5 / ∞),
 * and graceful missing-audio fallback.
 */
@Composable
fun AudioPlayerBar(
    audioState: AudioPlayerState,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onCycleSpeedClick: () -> Unit,
    onToggleAbLoopClick: () -> Unit,
    onCycleRepeatClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val borderWidth = if (colors.isDark) 1.dp else 2.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (audioState.isPlaying) colors.brandSoft else colors.surface, shape)
            .border(borderWidth, if (audioState.isPlaying) colors.brand else colors.outline, shape)
            .padding(16.dp)
            .testTag("audio_player_bar")
    ) {
        if (!audioState.isAudioFileAvailable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = null,
                        tint = colors.inkMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الصوت غير متوفر لهذا الحديث",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                        modifier = Modifier.testTag("audio_missing_label")
                    )
                }
                ChunkyButton(
                    label = "إعادة المحاولة",
                    onClick = onRetryClick,
                    variant = ChunkyButtonVariant.Tonal,
                    size = ChunkyButtonSize.Medium,
                    modifier = Modifier.testTag("audio_retry_button")
                )
            }
        } else {
            // Top Row: Play/Pause + Slider + Elapsed/Total Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChunkyButton(
                    label = if (audioState.isPlaying) "إيقاف" else "استمع",
                    leadingIcon = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onClick = onPlayPauseClick,
                    variant = if (audioState.isPlaying) ChunkyButtonVariant.Gold else ChunkyButtonVariant.Primary,
                    size = ChunkyButtonSize.Medium,
                    modifier = Modifier.testTag("audio_play_pause_button")
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = audioState.progressFraction,
                        onValueChange = onSeek,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.brand,
                            activeTrackColor = colors.brand,
                            inactiveTrackColor = colors.surfaceAlt
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audio_scrubber")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val loopStatusText = when {
                            !audioState.isAudioFileAvailable ->
                                NawawiaVoice.Errors.AUDIO_MISSING_TITLE
                            audioState.statusNoticeAr != null -> audioState.statusNoticeAr
                            audioState.isAbLoopActive -> "يُكرّر ما بين (أ) و(ب)"
                            audioState.pointAFraction != null -> "حدّدتَ (أ) — اضغط عند (ب)"
                            audioState.isPlaying -> "قراءة: ${'$'}{audioState.currentTitleAr}"
                            else -> "اسمع الحديث بصوت القارئ"
                        }
                        Text(
                            text = loopStatusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (audioState.isPlaying || audioState.isAbLoopActive) colors.brand else colors.inkMuted,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = audioState.formattedTimeAr,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.ink,
                            modifier = Modifier.testTag("audio_time_counter")
                        )
                    }
                }
            }

            // Live Active Phrase Box when playing
            if (audioState.isPlaying && audioState.activeSegmentTextAr.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.brand, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("audio_active_segment_box")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = colors.brand,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = audioState.activeSegmentTextAr,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AmiriFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.ink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Row: Speed, A-B Loop, Repeat Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Pill
                AudioControlChip(
                    label = "السرعة: ${audioState.speed.labelAr}",
                    isActive = audioState.speed.multiplier != 1.0f,
                    onClick = onCycleSpeedClick,
                    testTag = "audio_speed_button",
                    contentDesc = "تغيير سرعة الصوت، الحالية ${audioState.speed.labelAr}",
                    modifier = Modifier.weight(1f)
                )

                // A-B Loop Pill
                val abLabel = when {
                    audioState.isAbLoopActive -> "إلغاء (أ-ب)"
                    audioState.pointAFraction != null -> "نقطة (ب)"
                    else -> "تحديد (أ-ب)"
                }
                AudioControlChip(
                    label = abLabel,
                    isActive = audioState.pointAFraction != null,
                    onClick = onToggleAbLoopClick,
                    testTag = "audio_ab_loop_button",
                    contentDesc = "تحديد مقطع للحفظ والتكرار أ إلى ب",
                    modifier = Modifier.weight(1f)
                )

                // Repeat Count Pill
                AudioControlChip(
                    label = "تكرار: ${audioState.repeatMode.labelAr}",
                    isActive = audioState.repeatMode.count != 1,
                    onClick = onCycleRepeatClick,
                    testTag = "audio_repeat_button",
                    contentDesc = "عدد مرات التكرار: ${audioState.repeatMode.labelAr}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AudioControlChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    contentDesc: String,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val bgColor = if (isActive) colors.brandSoft else colors.surfaceAlt
    val borderColor = if (isActive) colors.brand else colors.outline
    val textColor = if (isActive) colors.brand else colors.ink

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(bgColor, shape)
            .border(1.5.dp, borderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag(testTag)
            .semantics { contentDescription = contentDesc },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
