package com.nawawia.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.compose.runtime.Immutable
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.domain.voice.NawawiaVoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob

/**
 * ⚠️ قاعدة هذا الملف، وهي قاعدة المشروع كلّه:
 *
 * **لا يُقرأ حديث رسول الله ﷺ إلا بصوت قارئ بشري مُسجَّل.**
 *
 * النسخة السابقة من هذا الملف كانت تنطق المتن بمحرّك `android.speech.tts.TextToSpeech`
 * بسرعة قابلة للتعديل، وتولّد موجة جيبية عبر `AudioTrack` وتُسمعها بدلًا من الحديث
 * حين يغيب محرّك النطق. وقد حُذف الأمران بالكامل، لمخالفتهما `AUDIO_GUIDE.md` بند ٣،
 * ولأنّ إجراء كلام النبوة على لسان آلة لا يصحّ.
 *
 * فإن لم يوجد تسجيل، يُعلِن هذا الملف ذلك صراحةً عبر `isAudioFileAvailable = false`
 * ولا يُصدر صوتًا البتّة. والقراءة والدرس يعملان كاملين بلا صوت.
 */

enum class PlaybackSpeed(val multiplier: Float, val labelAr: String) {
    SLOW(0.75f, "٠.٧٥"),
    NORMAL(1.0f, "١"),
    FAST(1.25f, "١.٢٥");

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

/**
 * توقيت كلمة واحدة في التسجيل — أساس التظليل كلمةً بكلمة أثناء التلاوة.
 * التوقيتات تُحمَّل مع المحتوى، ولا تُولَّد في التطبيق.
 */
@Immutable
data class WordTiming(
    val wordIndex: Int,
    val startMillis: Int,
    val endMillis: Int
)

@Immutable
data class AudioPlayerState(
    val currentAudioId: String = "",
    val currentTitleAr: String = "",
    val currentSpokenTextAr: String = "",
    val activeSegmentTextAr: String = "",
    val isPlaying: Boolean = false,
    val progressFraction: Float = 0f,
    val elapsedSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val currentRepeatIteration: Int = 1,
    val speed: PlaybackSpeed = PlaybackSpeed.NORMAL,
    val repeatMode: RepeatModeCount = RepeatModeCount.ONE,
    val pointAFraction: Float? = null,
    val pointBFraction: Float? = null,
    /** false ⇒ لا تسجيل لهذا الحديث. تُعرض حالة صريحة، ولا يُشغَّل أي بديل. */
    val isAudioFileAvailable: Boolean = false,
    val statusNoticeAr: String? = null,
    /** موضع الكلمة الجارية في المتن، أو -1 إذا لا تظليل. */
    val activeWordIndex: Int = -1
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

class AudioController(private val context: Context? = null) {

    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private var player: MediaPlayer? = null
    private var timings: List<WordTiming> = emptyList()
    private var playsRemaining: Int = 1

    /** الامتدادات المقبولة، بالأفضلية. Opus أصغر، وAAC أوسع توافقًا. */
    private val extensions = listOf("ogg", "opus", "m4a", "mp3")

    // ── التحميل ─────────────────────────────────────────────────────────

    /**
     * يحمّل تسجيل حديث.
     * [spokenTextAr] نصّ المتن — يُستعمل **للتظليل فقط**، ولا يُنطق أبدًا.
     */
    fun loadHadithAudio(
        audioId: String,
        spokenTextAr: String,
        titleAr: String = "",
        wordTimings: List<WordTiming> = emptyList()
    ) {
        stopInternal()
        timings = wordTimings
        val opened = openPlayer(audioId)
        _state.value = AudioPlayerState(
            currentAudioId = audioId,
            currentTitleAr = titleAr,
            currentSpokenTextAr = spokenTextAr,
            isAudioFileAvailable = opened != null,
            totalDurationSeconds = ((opened?.duration ?: 0) / 1000).coerceAtLeast(0),
            statusNoticeAr = if (opened == null) NawawiaVoice.Errors.AUDIO_MISSING_BODY else null,
            speed = _state.value.speed,
            repeatMode = _state.value.repeatMode
        )
    }

    /** مطالبة الدرس بتشغيل تلاوة الحديث الجاري. لا تنطق شيئًا إن لم يوجد تسجيل. */
    fun playPromptAudio(
        audioId: String,
        spokenTextAr: String,
        titleAr: String = "",
        wordTimings: List<WordTiming> = emptyList()
    ) {
        if (_state.value.currentAudioId != audioId) {
            loadHadithAudio(audioId, spokenTextAr, titleAr, wordTimings)
        }
        if (_state.value.isAudioFileAvailable) startPlayback()
    }

    private fun openPlayer(audioId: String): MediaPlayer? {
        val ctx = context ?: return null
        for (ext in extensions) {
            val mp = runCatching {
                val afd = ctx.assets.openFd("audio/$audioId.$ext")
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                afd.use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                mp.prepare()
                mp
            }.getOrNull()
            if (mp != null) {
                mp.setOnCompletionListener { onTrackCompleted() }
                player = mp
                return mp
            }
        }
        player = null
        return null
    }

    // ── التشغيل ─────────────────────────────────────────────────────────

    fun togglePlayPause() {
        if (!_state.value.isAudioFileAvailable) return
        if (_state.value.isPlaying) pausePlayback() else startPlayback()
    }

    private fun startPlayback() {
        val mp = player ?: return
        playsRemaining = _state.value.repeatMode.count
        runCatching {
            mp.start()
            applySpeed(mp, _state.value.speed)
        }.onFailure { return }
        _state.update { it.copy(isPlaying = true, statusNoticeAr = null) }
        startTicker()
    }

    private fun pausePlayback() {
        runCatching { player?.pause() }
        tickerJob?.cancel()
        _state.update { it.copy(isPlaying = false) }
    }

    /**
     * السرعة عبر `PlaybackParams` (متاحة منذ API 23، و`minSdk = 24`).
     * ملاحظة: ضبط المعاملات على مشغّل متوقّف يبدأ التشغيل على بعض الإصدارات،
     * فلا نضبطها إلا أثناء التشغيل.
     */
    private fun applySpeed(mp: MediaPlayer, speed: PlaybackSpeed) {
        runCatching {
            if (mp.isPlaying) {
                mp.playbackParams = PlaybackParams().setSpeed(speed.multiplier)
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val mp = player ?: break
                val pos = runCatching { mp.currentPosition }.getOrDefault(0)
                val dur = runCatching { mp.duration }.getOrDefault(0).coerceAtLeast(1)

                val a = _state.value.pointAFraction
                val b = _state.value.pointBFraction
                if (a != null && b != null && b > a && pos >= (b * dur).toInt()) {
                    runCatching { mp.seekTo((a * dur).toInt()) }
                }

                _state.update {
                    it.copy(
                        progressFraction = (pos.toFloat() / dur).coerceIn(0f, 1f),
                        elapsedSeconds = pos / 1000,
                        totalDurationSeconds = dur / 1000,
                        activeWordIndex = wordIndexAt(pos)
                    )
                }
                delay(80)
            }
        }
    }

    private fun wordIndexAt(positionMillis: Int): Int {
        if (timings.isEmpty()) return -1
        val hit = timings.lastOrNull { positionMillis >= it.startMillis }
        return if (hit != null && positionMillis <= hit.endMillis + 250) hit.wordIndex else -1
    }

    private fun onTrackCompleted() {
        val mode = _state.value.repeatMode
        val again = mode == RepeatModeCount.INFINITE || --playsRemaining > 0
        if (again) {
            runCatching {
                player?.seekTo(0)
                player?.start()
            }
            _state.update { it.copy(currentRepeatIteration = it.currentRepeatIteration + 1) }
        } else {
            tickerJob?.cancel()
            _state.update {
                it.copy(
                    isPlaying = false,
                    progressFraction = 1f,
                    activeWordIndex = -1,
                    currentRepeatIteration = 1
                )
            }
        }
    }

    // ── التحكّم ─────────────────────────────────────────────────────────

    fun seekTo(fraction: Float) {
        val mp = player ?: return
        val dur = runCatching { mp.duration }.getOrDefault(0)
        if (dur <= 0) return
        val target = (fraction.coerceIn(0f, 1f) * dur).toInt()
        runCatching { mp.seekTo(target) }
        _state.update {
            it.copy(
                progressFraction = fraction.coerceIn(0f, 1f),
                elapsedSeconds = target / 1000,
                activeWordIndex = wordIndexAt(target)
            )
        }
    }

    fun cycleSpeed() {
        val next = _state.value.speed.next()
        _state.update { it.copy(speed = next) }
        player?.let { applySpeed(it, next) }
    }

    fun cycleRepeatMode() {
        val next = _state.value.repeatMode.next()
        playsRemaining = next.count
        _state.update { it.copy(repeatMode = next, currentRepeatIteration = 1) }
    }

    /** أ ← ب: الضغطة الأولى تثبّت أ، والثانية ب، والثالثة تُلغي التكرار. */
    fun toggleAbLoopPoint() {
        _state.update { s ->
            when {
                s.pointAFraction == null ->
                    s.copy(pointAFraction = s.progressFraction, pointBFraction = null)
                s.pointBFraction == null && s.progressFraction > s.pointAFraction ->
                    s.copy(pointBFraction = s.progressFraction)
                else ->
                    s.copy(pointAFraction = null, pointBFraction = null)
            }
        }
    }

    fun onAudioFocusLost() = pausePlayback()

    fun onHeadsetUnplugged() = pausePlayback()

    /** يُعيد محاولة فتح الملف بعد تركيب حزمة التلاوة. */
    fun retryAudio() {
        val id = _state.value.currentAudioId
        if (id.isNotEmpty()) {
            loadHadithAudio(id, _state.value.currentSpokenTextAr, _state.value.currentTitleAr, timings)
        }
    }

    /** للاختبارات: يُجبر حالة «لا تسجيل» دون ملفات. */
    fun markAudioMissingForTest() {
        stopInternal()
        _state.update {
            it.copy(
                isAudioFileAvailable = false,
                isPlaying = false,
                statusNoticeAr = NawawiaVoice.Errors.AUDIO_MISSING_BODY
            )
        }
    }

    private fun stopInternal() {
        tickerJob?.cancel()
        tickerJob = null
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        playsRemaining = 1
    }

    fun release() {
        stopInternal()
        scope.coroutineContext[Job]?.cancel()
        _state.value = AudioPlayerState()
    }
}
