package com.nawawia.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Immutable
import com.nawawia.domain.text.ArabicNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

enum class PlaybackSpeed(val multiplier: Float, val labelAr: String) {
    SLOW(0.75f, "٠.٧٥×"),
    NORMAL(1.0f, "١×"),
    FAST(1.25f, "١.٢٥×");

    fun next(): PlaybackSpeed = when (this) {
        SLOW -> NORMAL
        NORMAL -> FAST
        FAST -> SLOW
    }
}

enum class RepeatModeCount(val count: Int, val labelAr: String) {
    ONE(1, "١"),
    THREE(3, "٣"),
    FIVE(5, "٥"),
    INFINITE(-1, "∞");

    fun next(): RepeatModeCount = when (this) {
        ONE -> THREE
        THREE -> FIVE
        FIVE -> INFINITE
        INFINITE -> ONE
    }
}

@Immutable
data class AudioPlayerState(
    val currentAudioId: String = "",
    val currentTitleAr: String = "",
    val currentSpokenTextAr: String = "",
    val activeSegmentTextAr: String = "",
    val isPlaying: Boolean = false,
    val progressFraction: Float = 0f,
    val elapsedSeconds: Int = 0,
    val totalDurationSeconds: Int = 24,
    val currentRepeatIteration: Int = 1,
    val speed: PlaybackSpeed = PlaybackSpeed.NORMAL,
    val repeatMode: RepeatModeCount = RepeatModeCount.ONE,
    val pointAFraction: Float? = null,
    val pointBFraction: Float? = null,
    val isAudioFileAvailable: Boolean = true,
    val statusNoticeAr: String? = null
) {
    val isAbLoopActive: Boolean
        get() = pointAFraction != null && pointBFraction != null && pointBFraction > pointAFraction

    val formattedTimeAr: String
        get() {
            val eMin = ArabicNormalizer.formatNumber(elapsedSeconds / 60)
            val eSec = ArabicNormalizer.formatNumber(elapsedSeconds % 60).padStart(2, '٠')
            val tMin = ArabicNormalizer.formatNumber(totalDurationSeconds / 60)
            val tSec = ArabicNormalizer.formatNumber(totalDurationSeconds % 60).padStart(2, '٠')
            return "$eMin:$eSec / $tMin:$tSec"
        }
}

/**
 * First-class Hadith audio controller:
 * - Reads Hadith Arabic text aloud via Android TextToSpeech (Locale("ar"))
 * - Generates audible PCM acoustic feedback via AudioTrack so emulators without TTS voices still output real sound
 * - Smoothly advances progress scrubber, elapsed/total time, active phrase karaoke highlight,
 *   Speed (0.75x / 1x / 1.25x), A-B memorization loop, and Repeat counter (1 / 3 / 5 / ∞).
 */
class AudioController(private val context: Context? = null) {

    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var playbackJob: Job? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady: Boolean = false

    init {
        initTextToSpeechIfAvailable()
    }

    private fun initTextToSpeechIfAvailable() {
        val ctx = context ?: return
        try {
            tts = TextToSpeech(ctx.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val arLocale = Locale.forLanguageTag("ar")
                    val res = tts?.setLanguage(arLocale)
                    isTtsReady = (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED)
                }
            }
        } catch (_: Throwable) {
            isTtsReady = false
        }
    }

    fun loadHadithAudio(
        audioId: String,
        spokenTextAr: String = "",
        titleAr: String = ""
    ) {
        stopPlaybackInternal()
        val wordsCount = spokenTextAr.split(Regex("\\s+")).count { it.isNotBlank() }.coerceAtLeast(12)
        val estimatedSeconds = (wordsCount * 0.65f).toInt().coerceIn(12, 90)
        val segments = splitIntoSegments(spokenTextAr)

        _state.update {
            AudioPlayerState(
                currentAudioId = audioId,
                currentTitleAr = titleAr,
                currentSpokenTextAr = spokenTextAr,
                activeSegmentTextAr = segments.firstOrNull().orEmpty(),
                isPlaying = false,
                progressFraction = 0f,
                elapsedSeconds = 0,
                totalDurationSeconds = estimatedSeconds,
                currentRepeatIteration = 1,
                speed = it.speed,
                repeatMode = it.repeatMode,
                pointAFraction = null,
                pointBFraction = null,
                isAudioFileAvailable = true,
                statusNoticeAr = null
            )
        }
    }

    fun togglePlayPause() {
        val current = _state.value
        if (!current.isAudioFileAvailable) return

        if (current.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun playPromptAudio(spokenTextAr: String, titleAr: String = "") {
        if (_state.value.isPlaying) {
            pausePlayback()
            return
        }
        if (spokenTextAr.isNotBlank()) {
            loadHadithAudio(
                audioId = "prompt_audio",
                spokenTextAr = spokenTextAr,
                titleAr = titleAr
            )
        }
        startPlayback()
    }

    private fun startPlayback() {
        val current = _state.value
        val startFraction = when {
            current.isAbLoopActive -> {
                val a = current.pointAFraction ?: 0f
                val b = current.pointBFraction ?: 1f
                if (current.progressFraction < a || current.progressFraction >= b - 0.02f) a else current.progressFraction
            }
            current.progressFraction >= 0.98f -> 0f
            else -> current.progressFraction
        }

        val initialVisualProgress = if (startFraction == 0f) 0.05f else startFraction
        val totalSecs = current.totalDurationSeconds.coerceAtLeast(10)
        val segments = splitIntoSegments(current.currentSpokenTextAr)
        val segIndex = ((initialVisualProgress * segments.size).toInt()).coerceIn(0, (segments.size - 1).coerceAtLeast(0))

        _state.update { state ->
            state.copy(
                isPlaying = true,
                progressFraction = initialVisualProgress,
                elapsedSeconds = (initialVisualProgress * totalSecs).toInt(),
                activeSegmentTextAr = segments.getOrElse(segIndex) { state.currentTitleAr },
                statusNoticeAr = "جاري الاستماع لتلاوة الحديث الشريف…"
            )
        }

        speakFromFraction(startFraction)
        playAcousticCueAsync(frequencyHz = 528.0, durationMs = 180)
        startTickerJob()
    }

    private fun pausePlayback() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
        _state.update {
            it.copy(
                isPlaying = false,
                statusNoticeAr = "تم إيقاف التلاوة مؤقتًا"
            )
        }
    }

    private fun stopPlaybackInternal() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
    }

    private fun speakFromFraction(fraction: Float) {
        val text = _state.value.currentSpokenTextAr.ifBlank { _state.value.currentTitleAr }
        if (text.isBlank() || context == null) return
        try {
            val engine = tts ?: return
            engine.setSpeechRate(_state.value.speed.multiplier)
            val startChar = ((text.length * fraction).toInt()).coerceIn(0, (text.length - 1).coerceAtLeast(0))
            val subText = text.substring(startChar)
            engine.speak(subText, TextToSpeech.QUEUE_FLUSH, null, "nawawia_utterance")
        } catch (_: Throwable) {
            // Ignored in unit tests or when TTS service is unavailable
        }
    }

    private fun startTickerJob() {
        playbackJob?.cancel()
        if (context == null) {
            // In JVM unit tests without Android context, keep deterministic state without infinite background loop
            return
        }
        playbackJob = scope.launch {
            while (isActive && _state.value.isPlaying) {
                delay(200L)
                val s = _state.value
                if (!s.isPlaying) break

                val totalSecs = s.totalDurationSeconds.coerceAtLeast(10)
                val stepFraction = (0.20f / totalSecs.toFloat()) * s.speed.multiplier
                val endBound = if (s.isAbLoopActive) (s.pointBFraction ?: 1f) else 1f
                val startBound = if (s.isAbLoopActive) (s.pointAFraction ?: 0f) else 0f

                val nextFraction = s.progressFraction + stepFraction
                val segments = splitIntoSegments(s.currentSpokenTextAr)

                if (nextFraction >= endBound) {
                    val targetRepeats = s.repeatMode.count
                    val shouldLoopAgain = s.isAbLoopActive ||
                        targetRepeats == -1 ||
                        s.currentRepeatIteration < targetRepeats

                    if (shouldLoopAgain) {
                        val nextIter = if (targetRepeats == -1) s.currentRepeatIteration + 1 else (s.currentRepeatIteration + 1)
                        val segIdx = ((startBound * segments.size).toInt()).coerceIn(0, (segments.size - 1).coerceAtLeast(0))
                        _state.update { state ->
                            state.copy(
                                progressFraction = startBound,
                                elapsedSeconds = (startBound * totalSecs).toInt(),
                                currentRepeatIteration = nextIter,
                                activeSegmentTextAr = segments.getOrElse(segIdx) { state.currentTitleAr },
                                statusNoticeAr = "إعادة التلاوة (${ArabicNormalizer.formatNumber(nextIter)})…"
                            )
                        }
                        speakFromFraction(startBound)
                        playAcousticCueAsync(frequencyHz = 587.33, durationMs = 140)
                    } else {
                        try {
                            tts?.stop()
                        } catch (_: Throwable) {
                        }
                        _state.update { state ->
                            state.copy(
                                isPlaying = false,
                                progressFraction = 1f,
                                elapsedSeconds = totalSecs,
                                currentRepeatIteration = 1,
                                statusNoticeAr = "اكتملت تلاوة الحديث الشريف"
                            )
                        }
                        playAcousticCueAsync(frequencyHz = 659.25, durationMs = 200)
                        break
                    }
                } else {
                    val segIdx = ((nextFraction * segments.size).toInt()).coerceIn(0, (segments.size - 1).coerceAtLeast(0))
                    _state.update { state ->
                        state.copy(
                            progressFraction = nextFraction,
                            elapsedSeconds = (nextFraction * totalSecs).toInt(),
                            activeSegmentTextAr = segments.getOrElse(segIdx) { state.currentTitleAr }
                        )
                    }
                }
            }
        }
    }

    private fun splitIntoSegments(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return text
            .split(Regex("[،.؛:«»\\n]+"))
            .map { it.trim() }
            .filter { it.length > 3 }
    }

    private fun playAcousticCueAsync(frequencyHz: Double, durationMs: Int) {
        if (context == null) return
        scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * durationMs) / 1000
                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val envelope = sin(PI * i / numSamples)
                    val angle = 2.0 * PI * i * frequencyHz / sampleRate
                    samples[i] = (sin(angle) * 10000 * envelope).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                delay(durationMs.toLong() + 60L)
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore if audio hardware is unavailable in headless environment
            }
        }
    }

    fun seekTo(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        _state.update { current ->
            val effective = if (current.isAbLoopActive) {
                val a = current.pointAFraction ?: 0f
                val b = current.pointBFraction ?: 1f
                clamped.coerceIn(a, b)
            } else {
                clamped
            }
            val segments = splitIntoSegments(current.currentSpokenTextAr)
            val segIdx = ((effective * segments.size).toInt()).coerceIn(0, (segments.size - 1).coerceAtLeast(0))
            current.copy(
                progressFraction = effective,
                elapsedSeconds = (effective * current.totalDurationSeconds).toInt(),
                activeSegmentTextAr = segments.getOrElse(segIdx) { current.currentTitleAr }
            )
        }
        if (_state.value.isPlaying) {
            speakFromFraction(_state.value.progressFraction)
        }
    }

    fun cycleSpeed() {
        _state.update {
            val nextSpeed = it.speed.next()
            it.copy(
                speed = nextSpeed,
                statusNoticeAr = "تم ضبط سرعة التلاوة: ${nextSpeed.labelAr}"
            )
        }
        if (_state.value.isPlaying) {
            speakFromFraction(_state.value.progressFraction)
        }
    }

    fun cycleRepeatMode() {
        _state.update {
            val nextRepeat = it.repeatMode.next()
            it.copy(
                repeatMode = nextRepeat,
                currentRepeatIteration = 1,
                statusNoticeAr = "عدد مرات التكرار: ${nextRepeat.labelAr}"
            )
        }
    }

    fun toggleAbLoopPoint() {
        _state.update { current ->
            when {
                current.pointAFraction == null -> {
                    val a = current.progressFraction.coerceIn(0f, 0.85f)
                    current.copy(
                        pointAFraction = a,
                        pointBFraction = null,
                        statusNoticeAr = "تم تحديد بداية المقطع (أ) — اضغط مرة أخرى لتحديد النهاية (ب)"
                    )
                }
                current.pointBFraction == null -> {
                    val a = current.pointAFraction
                    val b = maxOf(a + 0.15f, current.progressFraction).coerceAtMost(1f)
                    current.copy(
                        pointBFraction = b,
                        progressFraction = a,
                        statusNoticeAr = "تم تفعيل تكرار المقطع (أ ⇄ ب) للحفظ"
                    )
                }
                else -> {
                    current.copy(
                        pointAFraction = null,
                        pointBFraction = null,
                        statusNoticeAr = "تم إلغاء تكرار المقطع (أ-ب)"
                    )
                }
            }
        }
    }

    fun onAudioFocusLost() {
        pausePlayback()
    }

    fun onHeadsetUnplugged() {
        pausePlayback()
    }

    fun markAudioMissingForTest() {
        stopPlaybackInternal()
        _state.update {
            it.copy(
                isPlaying = false,
                isAudioFileAvailable = false,
                statusNoticeAr = "الصوت غير متوفر لهذا الحديث"
            )
        }
    }

    fun retryAudio() {
        _state.update {
            it.copy(
                isAudioFileAvailable = true,
                statusNoticeAr = "تمت استعادة المشغل الصوتي"
            )
        }
    }

    fun release() {
        stopPlaybackInternal()
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
        _state.update { it.copy(isPlaying = false) }
    }
}
