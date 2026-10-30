package com.nawawia.domain.voice

import kotlin.math.abs

/**
 * نبرة التطبيق.
 *
 * المشكلة التي يحلّها هذا الملف: النسخة السابقة كانت تملك **جملتي تشجيع اثنتين**
 * تتكرّران إلى الأبد («صحيح! أحسنت الإجابة» و«غير صحيح — راجع الإجابة الصحيحة
 * بالأسفل»)، فبدا التطبيق آلة تُصدر تقارير حالة لا معلّمًا يخاطبك.
 *
 * القواعد التي كُتبت عليها كل جملة هنا:
 *
 * ١. **صوت معلّم، لا صوت نظام.** «ضبطتَها» لا «إجابة صحيحة». خطابٌ للمخاطَب
 *    المفرد، وجملة قصيرة، وفعلٌ ماضٍ أو أمرٌ — لا جملة اسمية تقريرية.
 * ٢. **التشجيع للمجهود، لا للنصّ.** نمدح ضبط المتعلّم؛ ولا نَصِف كلام النبي ﷺ
 *    بألفاظ اللعب («رائع!»، «يا بطل»).
 * ٣. **لا «يا بطل» ولا نبرة ألعاب أطفال** فوق محتوى نبوي.
 * ٤. **تنويع حقيقي**: لكل موقف مجموعة، ولا تُعاد الجملة مباشرة بعد نفسها.
 * ٥. **الخطأ يُوجَّه ولا يُؤنَّب.** عند الخطأ نُظهر اللفظ الصحيح أولًا، ثم نطمئن.
 */
object NawawiaVoice {

    // ── إجابة صحيحة ────────────────────────────────────────────────────
    val correct: List<String> = listOf(
        "ضبطتَها",
        "تمامًا",
        "أصبتَ",
        "هذه رسخت",
        "نعم، هكذا لفظه",
        "دقيق",
        "بالضبط",
        "ما شاء الله",
        "هذه في حفظك الآن",
        "أحسنتَ"
    )

    /** يُقال عند الصواب من أول مرة بعد سلسلة صواب — تنويع أعلى درجة. */
    val correctStreak: List<String> = listOf(
        "ثلاثة على التوالي",
        "ماضٍ على ضبط",
        "لم تُخطئ بعد في هذا الحديث",
        "إتقانٌ متتابع"
    )

    // ── إجابة خاطئة: توجيه لا تأنيب ────────────────────────────────────
    /** يسبق اللفظ الصحيح مباشرة — والصيغة دائمًا «…: اللفظ». */
    val wrongLead: List<String> = listOf(
        "الصواب",
        "اللفظ",
        "لفظ الحديث",
        "الذي في الحديث"
    )

    /** يُقال بعد عرض اللفظ الصحيح — طمأنة قصيرة، ثم ما سيحدث. */
    val wrongReassure: List<String> = listOf(
        "سيعود عليك هذا السؤال قبل نهاية الدرس.",
        "لا بأس — ستراه مرّة أخرى قبل أن تُنهي.",
        "اقرأه مرّة، وسيأتيك ثانيةً.",
        "هذا موضع يُخطئ فيه كثيرون. سيتكرّر عليك."
    )

    // ── نهاية الدرس ────────────────────────────────────────────────────
    /** كل الأسئلة من أول مرة. */
    val lessonPerfect: List<String> = listOf(
        "بلا خطأ واحد",
        "ضبطتَه كلّه من أول مرة",
        "حفظٌ متين"
    )

    /** أغلبها من أول مرة. */
    val lessonGood: List<String> = listOf(
        "الحديث في حفظك",
        "أتممتَه على ضبط",
        "خطوة ثابتة"
    )

    /** تعثّر كثيرًا — لا نكذب عليه، ولا نُحبطه. */
    val lessonRough: List<String> = listOf(
        "أتممتَه — وما تعثّرتَ فيه سيعود في المراجعة",
        "الحفظ يحتاج عَودًا، وهذا أوّله",
        "أنهيتَه. والمراجعة تُثبّت ما بقي"
    )

    // ── إتمام وحدة ─────────────────────────────────────────────────────
    val unitDone: List<String> = listOf(
        "أتممتَ الوحدة",
        "وحدة كاملة في حفظك",
        "سبعة أحاديث، وانتهت الوحدة"
    )

    // ── السلسلة ────────────────────────────────────────────────────────
    fun streakLine(days: Int): String = when {
        days <= 0 -> "ابدأ سلسلتك اليوم"
        days == 1 -> "أول يوم في سلسلتك"
        days == 2 -> "يومان على التوالي"
        days < 7 -> "${ar(days)} أيام على التوالي"
        days == 7 -> "أسبوع كامل بلا انقطاع"
        days < 30 -> "${ar(days)} يومًا بلا انقطاع"
        days == 30 -> "شهر كامل. هذا ليس قليلًا"
        else -> "${ar(days)} يومًا — وما زلت"
    }

    // ── الحالات الفارغة: دعوة إلى فعل، لا وصف حال ──────────────────────
    object Empty {
        const val NO_REVIEWS_DUE =
            "لا مراجعة مستحقّة اليوم. تقدّم في الطريق، أو راجِع ما تشاء اختيارًا."
        const val NO_BOOKMARKS =
            "لم تحفظ حديثًا بعد. المعلّمة في أعلى شاشة الحديث تُبقيه هنا."
        const val NO_SEARCH_RESULTS =
            "لا شيء بهذا اللفظ. جرّب كلمة من المتن، أو اسم الراوي، أو رقم الحديث."
        const val HEARTS_EMPTY_TITLE = "نفدت قلوبك"
        const val HEARTS_EMPTY_BODY =
            "يعود قلب كل نصف ساعة. والمراجعة مفتوحة لك الآن بلا قلوب — وقراءة المتن لا تُغلق أبدًا."
    }

    // ── الأخطاء: ماذا حدث، وماذا تفعل. بلا اعتذار وبلا غموض ────────────
    object Errors {
        const val CONTENT_INVALID_TITLE = "تعذّر فتح متن الأربعين"
        const val CONTENT_INVALID_BODY =
            "ملف المتن لا يطابق بصمته، فلم نعرضه. إعادة تثبيت التطبيق تُصلحه، ولا يضيع تقدّمك."
        const val AUDIO_MISSING_TITLE = "لا تسجيل لهذا الحديث بعد"
        const val AUDIO_MISSING_BODY =
            "نواوية لا تقرأ الحديث بصوت آليّ. والقراءة والدرس يعملان كاملين بلا صوت."
        const val LINK_BLOCKED =
            "رابط خارج المصادر الموثوقة — حُجب."
    }

    /** مصدر الشرح يُسمّى باسمه، فلا نَعِد بما لا نفتح. */
    fun openingSharh(sourceNameAr: String): String = "يُفتح الشرح في $sourceNameAr"

    // ── الاختيار المتنوّع ───────────────────────────────────────────────
    /**
     * يختار جملة من [pool] بلا إعادة لآخر جملة قيلت.
     * [lastUsed] آخر نصّ عُرض من هذه المجموعة، أو null.
     */
    fun pick(pool: List<String>, seed: Long, lastUsed: String? = null): String {
        if (pool.isEmpty()) return ""
        if (pool.size == 1) return pool[0]
        val candidates = pool.filter { it != lastUsed }.ifEmpty { pool }
        return candidates[(abs(seed) % candidates.size).toInt()]
    }

    /** يبني سطر الخطأ كاملًا: «الصواب: اللفظ». */
    fun wrongHeadline(correctTextAr: String, seed: Long): String =
        "${pick(wrongLead, seed)}: $correctTextAr"

    /** يختار خاتمة الدرس على قدر الأداء الحقيقي — بلا مبالغة. */
    fun lessonClosing(correctFirstTry: Int, totalQuestions: Int, seed: Long): String {
        if (totalQuestions <= 0) return pick(lessonGood, seed)
        val ratio = correctFirstTry.toDouble() / totalQuestions.toDouble()
        return when {
            ratio >= 1.0 -> pick(lessonPerfect, seed)
            ratio >= 0.7 -> pick(lessonGood, seed)
            else -> pick(lessonRough, seed)
        }
    }

    private val ARABIC_INDIC = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /** أرقام عربية هندية — التطبيق كلّه بها. */
    fun ar(value: Int): String {
        if (value == 0) return "٠"
        var v = if (value < 0) -value else value
        val sb = StringBuilder()
        while (v > 0) {
            sb.append(ARABIC_INDIC[v % 10])
            v /= 10
        }
        if (value < 0) sb.append('-')
        return sb.reverse().toString()
    }
}
