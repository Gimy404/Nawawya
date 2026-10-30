package com.nawawia.domain.notify

import kotlin.math.abs

/**
 * نصوص التذكير.
 *
 * ثلاث قواعد كُتب عليها كل نصّ هنا:
 *
 * ١. **لا لوم ولا تأنيب.** ولا سخرية. التطبيق يذكّر، ولا يعاتب. والعتاب في محتوى
 *    نبوي أشنع منه في تطبيق لغات.
 * ٢. **التعيين لا العموم.** «الحديث ١٢» و«سلسلتك ٧ أيام» لا «وقت المراجعة!».
 * ٣. **دوران بلا تكرار.** الدليل التجريبي المنشور على أن تدوير النصوص يرفع
 *    الاحتفاظ يعود إلى *عقوبة التكرار* نفسها — لأن المستخدم يملّ النصّ المعاد.
 *    فنحن نطبّق القاعدة التي أنتجت الأثر: لا يُعاد نصّ قبل [NO_REPEAT_WINDOW] يومًا.
 *
 * ولا إيموجي على متن حديث أبدًا. يُسمح بواحد في نصّ إنجاز فقط.
 */
data class ReminderTemplate(
    val id: String,
    val mood: ReminderMood,
    /** العناصر البديلة: {n} رقم الحديث · {t} عنوانه · {s} طول السلسلة · {d} عدد المستحقّ للمراجعة */
    val patternAr: String
)

object ReminderCopy {

    /** لا يُعاد النصّ قبل مرور هذه المدة. */
    const val NO_REPEAT_WINDOW = 14

    val templates: List<ReminderTemplate> = listOf(
        // ── أوّل الطريق ────────────────────────────────────────────────
        t("fs1", ReminderMood.FIRST_STEPS, "الحديث الأول في انتظارك. دقيقتان، لا أكثر."),
        t("fs2", ReminderMood.FIRST_STEPS, "ابدأ بحديث واحد: «{t}» — أقصر ممّا تظنّ."),
        t("fs3", ReminderMood.FIRST_STEPS, "أربعون حديثًا تبدأ بواحد. نبدأ الليلة؟"),
        t("fs4", ReminderMood.FIRST_STEPS, "حديث اليوم: «{t}». اسمعه مرّة، وستجد نصفه محفوظًا."),

        // ── سلسلة تتكوّن ───────────────────────────────────────────────
        t("sb1", ReminderMood.STREAK_BUILDING, "سلسلتك {s} أيام. الحديث {n} في انتظارك."),
        t("sb2", ReminderMood.STREAK_BUILDING, "يومك {s}. «{t}» — دقيقتان وتُكمله."),
        t("sb3", ReminderMood.STREAK_BUILDING, "ثلاث دقائق تحفظ لك اليوم في السلسلة."),
        t("sb4", ReminderMood.STREAK_BUILDING, "الحديث {n}: «{t}». ابدأ وأنت قادر."),

        // ── سلسلة طويلة ───────────────────────────────────────────────
        t("sl1", ReminderMood.STREAK_LONG, "{s} يومًا بلا انقطاع. هذا ليس قليلًا."),
        t("sl2", ReminderMood.STREAK_LONG, "سلسلتك {s} يومًا. تُكملها الليلة؟"),
        t("sl3", ReminderMood.STREAK_LONG, "{s} يومًا، والحديث {n} هو التالي."),
        t("sl4", ReminderMood.STREAK_LONG, "لا تدع {s} يومًا تنقطع على حديث واحد."),

        // ── مراجعة مستحقّة ─────────────────────────────────────────────
        t("rd1", ReminderMood.REVIEWS_DUE, "{d} أحاديث حان وقت مراجعتها. ابدأ بالأقرب للنسيان."),
        t("rd2", ReminderMood.REVIEWS_DUE, "مراجعة اليوم: {d} أحاديث. خمس دقائق."),
        t("rd3", ReminderMood.REVIEWS_DUE, "الحفظ يثبت بالعَود عليه — {d} أحاديث تنتظر."),
        t("rd4", ReminderMood.REVIEWS_DUE, "اليوم مراجعة لا حفظ جديد. {d} أحاديث فقط."),

        // ── فاته يوم ──────────────────────────────────────────────────
        t("l11", ReminderMood.LAPSED_ONE_DAY, "فاتك يوم — والسلسلة محفوظة بتجميدة. نُكمل من الحديث {n}؟"),
        t("l12", ReminderMood.LAPSED_ONE_DAY, "لا بأس. حديث واحد يُعيدك إلى الطريق."),
        t("l13", ReminderMood.LAPSED_ONE_DAY, "الحديث {n} ما زال في مكانه. متى شئت."),
        t("l14", ReminderMood.LAPSED_ONE_DAY, "يوم واحد لا يُفسد شهرًا. نُكمل؟"),

        // ── انقطاع أطول ───────────────────────────────────────────────
        t("ll1", ReminderMood.LAPSED_LONG, "مضت أيام. الأربعون لا تذهب، وأنت تعرف أين وقفت: الحديث {n}."),
        t("ll2", ReminderMood.LAPSED_LONG, "نبدأ من جديد بلا لوم؟ الحديث {n} ينتظر."),
        t("ll3", ReminderMood.LAPSED_LONG, "محفوظك محفوظ. راجِع حديثًا واحدًا فقط اليوم."),
        t("ll4", ReminderMood.LAPSED_LONG, "ما حفظتَه لم يضع. خمس دقائق تُعيده كلّه."),

        // ── الجمعة (قناة منفصلة، مُطفأة افتراضيًا) ────────────────────
        t("fr1", ReminderMood.FRIDAY, "جمعة مباركة. حديث واحد قبل الزحام؟"),
        t("fr2", ReminderMood.FRIDAY, "جمعة مباركة. أنسب يوم لمراجعة ما حفظت."),
        t("fr3", ReminderMood.FRIDAY, "جمعة مباركة. الحديث {n} في انتظارك متى فرغت."),

        // ── أتمّ الأربعين ─────────────────────────────────────────────
        t("ca1", ReminderMood.COMPLETED_ALL, "أتممتَ الأربعين. والمراجعة هي التي تُبقيها."),
        t("ca2", ReminderMood.COMPLETED_ALL, "الأربعون كلها في حفظك. {d} منها تستحقّ العَود عليها اليوم."),
        t("ca3", ReminderMood.COMPLETED_ALL, "حفظتَها كلّها. مراجعة قصيرة اليوم تُثبّتها.")
    )

    /** نصوص الإنجاز — قناة صامتة منفصلة، تُرسَل عند الحدث لا على موعد. */
    val milestones: List<ReminderTemplate> = listOf(
        t("m1", ReminderMood.STREAK_LONG, "أتممتَ وحدة كاملة. مصباحها مُضاء في حسابك ✨"),
        t("m2", ReminderMood.STREAK_LONG, "{s} يومًا متتابعة. سلسلة تستحقّ."),
        t("m3", ReminderMood.STREAK_LONG, "عشرون حديثًا — نصف الطريق."),
        t("m4", ReminderMood.STREAK_LONG, "{n} حديثًا في حفظك الآن."),
        t("m5", ReminderMood.COMPLETED_ALL, "الأربعون النووية كاملةً في حفظك. تمّت.")
    )

    private fun t(id: String, mood: ReminderMood, pattern: String) =
        ReminderTemplate(id, mood, pattern)

    /**
     * النصّ الأخير قبل الصمت.
     *
     * أنجح إشعار في تطبيقات العادة هو الذي يُعلن أنه سيتوقّف — لأنه صادق، ولأنه
     * يترك للمستخدم كرامته. ونحن نلتزم به فعلًا: بعد هذا النصّ يتوقّف التذكير.
     */
    fun finalNotice(): String =
        "لن أذكّرك بعد اليوم. التطبيق في مكانه متى أردت، ومحفوظك محفوظ."

    /**
     * يختار نصًّا لم يُستعمل حديثًا.
     * [recentlyUsedIds] أحدث المعرّفات أولًا. فإن كانت كل نصوص الحالة مستعملة
     * حديثًا، يُختار أقدمها استعمالًا — فلا يتعطّل التذكير بسبب القاعدة.
     */
    fun pick(
        mood: ReminderMood,
        recentlyUsedIds: List<String>,
        seed: Long
    ): ReminderTemplate {
        val pool = templates.filter { it.mood == mood }
            .ifEmpty { templates.filter { it.mood == ReminderMood.FIRST_STEPS } }

        val recent = recentlyUsedIds.take(NO_REPEAT_WINDOW)
        val fresh = pool.filter { it.id !in recent }
        if (fresh.isNotEmpty()) {
            val idx = (abs(seed) % fresh.size).toInt()
            return fresh[idx]
        }
        // كلها مستعملة حديثًا ⇒ أقدمها استعمالًا (أبعدها في القائمة).
        return pool.maxByOrNull { tpl ->
            val at = recent.indexOf(tpl.id)
            if (at < 0) Int.MAX_VALUE else at
        } ?: pool.first()
    }

    /** يستبدل العناصر البديلة بالأرقام العربية الهندية وعنوان الحديث. */
    fun render(template: ReminderTemplate, ctx: ReminderContext): String =
        template.patternAr
            .replace("{n}", arabicNumber(ctx.nextHadithNumber))
            .replace("{t}", ctx.nextHadithTitleAr)
            .replace("{s}", arabicNumber(ctx.streakDays))
            .replace("{d}", arabicNumber(ctx.dueReviewCount))

    private val ARABIC_INDIC = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    internal fun arabicNumber(value: Int): String {
        if (value == 0) return "٠"
        val sb = StringBuilder()
        var v = value
        val negative = v < 0
        if (negative) v = -v
        while (v > 0) {
            sb.append(ARABIC_INDIC[v % 10])
            v /= 10
        }
        if (negative) sb.append('-')
        return sb.reverse().toString()
    }
}
