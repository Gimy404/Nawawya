package com.nawawia.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class UnitColorRole {
    BRAND,
    GOLD,
    INFO,
    PLUM,
    CLAY,
    SAND
}

@Serializable
data class HadithUnit(
    val id: String,
    val order: Int,
    val titleAr: String,
    val hadithIds: List<String>,
    val colorRole: UnitColorRole = UnitColorRole.BRAND
)

@Serializable
data class TakhrijRef(
    val book: String,
    val number: String
)

@Serializable
data class Takhrij(
    val textAr: String,
    val refs: List<TakhrijRef> = emptyList()
)

@Serializable
data class KeyTerm(
    val term: String,
    val glossAr: String
)

@Serializable
data class HadithAudio(
    val matnId: String,
    val titleId: String
)

@Serializable
data class Hadith(
    val id: String,
    val number: Int,
    val unitId: String,
    val titleAr: String,
    val matnAr: String,
    val narratorAr: String,
    val narratorBioAr: String = "",
    val takhrij: Takhrij,
    val keyTermsAr: List<KeyTerm> = emptyList(),
    val sharhSummaryAr: String = "",
    val benefitsAr: List<String> = emptyList(),
    val sharhUrl: String,
    val audio: HadithAudio,
    val isExample: Boolean = false
)

@Serializable
enum class QuestionType {
    MCQ,
    FILL_BLANK,
    ORDER_PHRASES,
    AUDIO_RECOGNITION,
    MATCH_PAIRS,
    TRUE_FALSE
}

@Serializable
data class QuestionOption(
    val id: String,
    val textAr: String,
    val correct: Boolean = false
)

@Serializable
data class MatchPair(
    val id: String,
    val rightTextAr: String,
    val leftTextAr: String
)

@Serializable
data class Question(
    val id: String,
    val hadithId: String,
    val type: QuestionType,
    val difficulty: Int = 1,
    val promptAr: String,
    val options: List<QuestionOption> = emptyList(),
    val orderedPhrases: List<String> = emptyList(),
    val matchPairs: List<MatchPair> = emptyList(),
    val blankStartOffset: Int? = null,
    val blankEndOffset: Int? = null,
    val explanationAr: String? = null,
    val sharhUrl: String? = null
)

@Serializable
data class EncouragementItem(
    val audioId: String,
    val textAr: String
)

@Serializable
data class EncouragementCatalog(
    val correct: List<EncouragementItem> = emptyList(),
    val wrong: List<EncouragementItem> = emptyList(),
    val unitDone: List<EncouragementItem> = emptyList()
)

enum class NodeStatus {
    LOCKED,
    AVAILABLE,
    IN_PROGRESS,
    MASTERED
}

@Serializable
data class HeartsState(
    val currentHearts: Int = MAX_HEARTS,
    val lastHeartSpentEpochMillis: Long? = null
) {
    companion object {
        const val MAX_HEARTS = 5
        const val REFILL_INTERVAL_MINUTES = 30L
    }
}

@Serializable
data class StreakState(
    val streakDays: Int = 0,
    val lastActiveEpochDay: Long? = null,
    val freezesAvailable: Int = 0,
    val activeEpochDaysHistory: Set<Long> = emptySet(),
    val clockRollbackDiagnosticsCount: Int = 0
) {
    companion object {
        const val MAX_FREEZES = 2
    }
}

@Serializable
data class Sm2State(
    val hadithId: String,
    val easeFactor: Double = INITIAL_EASE_FACTOR,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueEpochDay: Long = 0L,
    val masteryPct: Int = 0,
    val lastGrade: Int = 0
) {
    companion object {
        const val INITIAL_EASE_FACTOR = 2.5
        const val MIN_EASE_FACTOR = 1.3
    }
}

enum class ReviewMode(
    val titleAr: String,
    val subtitleAr: String,
    val estimatedMinutes: Int
) {
    QUICK_10_MIN(
        titleAr = "مراجعة سريعة",
        subtitleAr = "الأربعون كاملة في تدفق زمني سريع بلا خصم قلوب",
        estimatedMinutes = 10
    ),
    SMART_SRS(
        titleAr = "المراجعة الذكية",
        subtitleAr = "الأحاديث المستحقة للمراجعة اليوم وفق خوارزمية التذكر المتباعد",
        estimatedMinutes = 5
    ),
    HARDEST_FIRST(
        titleAr = "الأحاديث الصعبة",
        subtitleAr = "ركّز على الأحاديث الأقل إتقانًا لترسيخ الحفظ",
        estimatedMinutes = 7
    )
}

@Serializable
data class Achievement(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val unlocked: Boolean
)

/**
 * Pure domain model for V1.1 local Pass-and-Play (لعب مع صاحبك).
 */
@Serializable
data class MatchSession(
    val playerOneName: String = "اللاعب الأول",
    val playerTwoName: String = "اللاعب الثاني",
    val playerOneScore: Int = 0,
    val playerTwoScore: Int = 0,
    val currentTurnIndex: Int = 0,
    val questions: List<Question> = emptyList(),
    val isFinished: Boolean = false
) {
    val activePlayerName: String
        get() = if (currentTurnIndex % 2 == 0) playerOneName else playerTwoName
}
