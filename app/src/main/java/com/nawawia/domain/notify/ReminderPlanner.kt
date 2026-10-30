package com.nawawia.domain.notify

import com.nawawia.domain.time.AsrMadhab
import com.nawawia.domain.time.CalculationMethod
import com.nawawia.domain.time.GeoCity
import com.nawawia.domain.time.PrayerClock
import com.nawawia.domain.time.PrayerDayMinutes

/**
 * منطق التذكير اليومي — نقي تمامًا: لا `android.*` ولا مؤقتات ولا قاعدة بيانات.
 * الطبقة الأندرويدية تسأل هذا الملف «متى؟» و«بماذا؟» ثم تجدول وتعرض.
 *
 * ثلاث قواعد تحكم كل ما في هذا الملف:
 *  ١. تذكير واحد في اليوم بأقصى حال. لا اثنان ولو فاتت أيام.
 *  ٢. لا يقع التذكير داخل نافذة صلاة.
 *  ٣. بعد سبعة تذكيرات متتالية بلا فتح: نصّ أخير صريح، ثم صمت حتى يعود المستخدم.
 */

/** النافذة المحجوزة حول كل صلاة. */
object PrayerBuffer {
    const val MINUTES_BEFORE_ADHAN = 10
    const val MINUTES_AFTER_ADHAN = 30
}

/** حالة المتعلّم وقت التذكير — هي التي تختار نبرة النصّ. */
enum class ReminderMood {
    /** لم يُتمّ حديثًا بعد. */
    FIRST_STEPS,

    /** سلسلة قائمة بين يومين وستة. */
    STREAK_BUILDING,

    /** سلسلة سبعة أيام فأكثر. */
    STREAK_LONG,

    /** فاته يوم واحد. */
    LAPSED_ONE_DAY,

    /** فاته ثلاثة أيام فأكثر. */
    LAPSED_LONG,

    /** عنده أحاديث استحقّت المراجعة اليوم. */
    REVIEWS_DUE,

    /** يوم الجمعة — قناة منفصلة، مُطفأة افتراضيًا. */
    FRIDAY,

    /** أتمّ الأربعين كلها. */
    COMPLETED_ALL
}

/**
 * ما يكفي لاتخاذ قرار التذكير، دون معرفة أي شيء عن أندرويد.
 * [lastOpenedDaysAgo] = ٠ يعني فتح التطبيق اليوم.
 */
data class ReminderContext(
    val streakDays: Int,
    val lastOpenedDaysAgo: Int,
    val masteredCount: Int,
    val totalCount: Int,
    val dueReviewCount: Int,
    val nextHadithNumber: Int,
    val nextHadithTitleAr: String,
    val isFriday: Boolean,
    val fridayChannelEnabled: Boolean,
    /** عدد التذكيرات المتتالية التي أُرسلت ولم يُفتح التطبيق بعدها. */
    val consecutiveIgnored: Int
)

/** قرار التذكير: إمّا نرسل نصًّا، وإمّا نصمت. */
sealed interface ReminderDecision {
    data class Send(
        val mood: ReminderMood,
        val bodyAr: String,
        /** النصّ الأخير قبل التوقّف — تُطفئ الطبقة الأعلى التذكير بعده. */
        val isFinalNotice: Boolean = false
    ) : ReminderDecision

    data class Skip(val reasonAr: String) : ReminderDecision
}

object ReminderPlanner {

    private const val MINUTES_PER_DAY = 24 * 60

    /** بعد هذا العدد من التذكيرات المُهمَلة نُرسل نصًّا أخيرًا ثم نصمت. */
    const val IGNORED_BEFORE_FINAL_NOTICE = 7

    // ── متى؟ ────────────────────────────────────────────────────────────

    /**
     * يزيح [preferredMinute] خارج أي نافذة صلاة.
     * يُرجِع نفس الدقيقة إذا لم تكن أوقات اليوم صالحة (خطوط عرض عالية) أو إن كانت
     * المراعاة مُطفأة — فلا نُغيّر اختيار المستخدم بلا سبب.
     */
    fun avoidPrayerWindows(
        preferredMinute: Int,
        prayers: PrayerDayMinutes,
        respectPrayerTimes: Boolean = true
    ): Int {
        if (!respectPrayerTimes || !prayers.isValid) return preferredMinute

        var minute = preferredMinute.coerceIn(0, MINUTES_PER_DAY - 1)
        val windows = prayers.fivePrayers
            .map { it - PrayerBuffer.MINUTES_BEFORE_ADHAN to it + PrayerBuffer.MINUTES_AFTER_ADHAN }
            .sortedBy { it.first }

        // الإزاحة قد تُوقعنا في نافذة تالية، فنكرّر. خمس صلوات ⇒ خمس محاولات تكفي.
        repeat(windows.size + 1) {
            val hit = windows.firstOrNull { (start, end) -> minute in start..end }
                ?: return minute
            val after = hit.second + 1
            minute = if (after < MINUTES_PER_DAY) {
                after
            } else {
                // الدفع للأمام يتجاوز منتصف الليل ⇒ ندفع للخلف بدل تخطّي اليوم.
                (hit.first - 1).coerceAtLeast(0)
            }
        }
        return minute
    }

    /** الصورة الكاملة: من المدينة والتاريخ إلى دقيقة التسليم. */
    fun deliveryMinute(
        preferredMinute: Int,
        city: GeoCity,
        year: Int,
        month: Int,
        day: Int,
        method: CalculationMethod,
        madhab: AsrMadhab,
        respectPrayerTimes: Boolean = true
    ): Int {
        if (!respectPrayerTimes) return preferredMinute
        val prayers = PrayerClock.compute(city, year, month, day, method, madhab)
        return avoidPrayerWindows(preferredMinute, prayers, respectPrayerTimes = true)
    }

    /**
     * وضع رمضان: التذكير بعد العشاء بساعة (بعد التراويح عادةً)، وليس في ساعات الصيام.
     * يُرجِع الدقيقة المقترحة، أو الأصلية إن تعذّر الحساب.
     */
    fun ramadanMinute(prayers: PrayerDayMinutes, fallbackMinute: Int): Int {
        if (!prayers.isValid) return fallbackMinute
        return (prayers.isha + 60).coerceAtMost(MINUTES_PER_DAY - 1)
    }

    // ── بماذا؟ ──────────────────────────────────────────────────────────

    fun moodFor(ctx: ReminderContext): ReminderMood = when {
        ctx.masteredCount >= ctx.totalCount && ctx.totalCount > 0 -> ReminderMood.COMPLETED_ALL
        ctx.lastOpenedDaysAgo >= 3 -> ReminderMood.LAPSED_LONG
        ctx.lastOpenedDaysAgo == 2 || ctx.lastOpenedDaysAgo == 1 -> ReminderMood.LAPSED_ONE_DAY
        ctx.isFriday && ctx.fridayChannelEnabled -> ReminderMood.FRIDAY
        ctx.dueReviewCount > 0 -> ReminderMood.REVIEWS_DUE
        ctx.streakDays >= 7 -> ReminderMood.STREAK_LONG
        ctx.streakDays >= 2 -> ReminderMood.STREAK_BUILDING
        else -> ReminderMood.FIRST_STEPS
    }

    /**
     * القرار الكامل.
     * [recentlyUsedTemplateIds] أحدث المعرّفات المُرسَلة — تُمرَّر من التخزين لتطبيق
     * قاعدة عدم التكرار؛ أحدثها أولًا.
     */
    fun decide(
        ctx: ReminderContext,
        recentlyUsedTemplateIds: List<String>,
        rotationSeed: Long
    ): ReminderDecision {
        if (ctx.lastOpenedDaysAgo == 0) {
            return ReminderDecision.Skip("فتح التطبيق اليوم — لا داعي للتذكير")
        }
        if (ctx.consecutiveIgnored > IGNORED_BEFORE_FINAL_NOTICE) {
            return ReminderDecision.Skip("توقّف التذكير بعد النصّ الأخير حتى يعود المستخدم")
        }

        val isFinal = ctx.consecutiveIgnored == IGNORED_BEFORE_FINAL_NOTICE
        if (isFinal) {
            return ReminderDecision.Send(
                mood = ReminderMood.LAPSED_LONG,
                bodyAr = ReminderCopy.finalNotice(),
                isFinalNotice = true
            )
        }

        val mood = moodFor(ctx)
        val template = ReminderCopy.pick(mood, recentlyUsedTemplateIds, rotationSeed)
        return ReminderDecision.Send(mood = mood, bodyAr = ReminderCopy.render(template, ctx))
    }
}
