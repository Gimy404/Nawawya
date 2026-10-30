package com.nawawia.domain.game

import com.nawawia.domain.model.Achievement

object AchievementsEngine {

    fun evaluateAchievements(
        masteredHadithCount: Int,
        completedUnitsCount: Int,
        streakDays: Int,
        completedQuickReview10Min: Boolean,
        totalXp: Int
    ): List<Achievement> {
        return listOf(
            Achievement(
                id = "ach_first_hadith",
                titleAr = "أول حديث",
                descriptionAr = "أكملت تعلّم الحديث الأول في الأربعين النووية",
                unlocked = masteredHadithCount >= 1
            ),
            Achievement(
                id = "ach_first_unit",
                titleAr = "أول وحدة",
                descriptionAr = "أتممت جميع أحاديث الوحدة الأولى بنجاح",
                unlocked = completedUnitsCount >= 1
            ),
            Achievement(
                id = "ach_streak_7",
                titleAr = "٧ أيام",
                descriptionAr = "واظبت على التعلم والمراجعة لمدة ٧ أيام متتالية",
                unlocked = streakDays >= 7
            ),
            Achievement(
                id = "ach_streak_30",
                titleAr = "٣٠ يومًا",
                descriptionAr = "واظبت على التعلم والمراجعة لمدة ٣٠ يومًا متتالية",
                unlocked = streakDays >= 30
            ),
            Achievement(
                id = "ach_streak_100",
                titleAr = "١٠٠ يوم",
                descriptionAr = "همّة عالية: ١٠٠ يوم متواصلة مع الأربعين النووية",
                unlocked = streakDays >= 100
            ),
            Achievement(
                id = "ach_quick_review_10",
                titleAr = "مراجعة كاملة في ١٠ دقائق",
                descriptionAr = "راجعت الأربعين النووية كاملة في جلسة المراجعة السريعة",
                unlocked = completedQuickReview10Min
            ),
            Achievement(
                id = "ach_all_42",
                titleAr = "٤٢/٤٢",
                descriptionAr = "أتقنت جميع أحاديث الأربعين النووية الاثنين والأربعين",
                unlocked = masteredHadithCount >= 42
            ),
            Achievement(
                id = "ach_xp_1000",
                titleAr = "١٠٠٠ XP",
                descriptionAr = "جمعت ١٠٠٠ نقطة خبرة من الدروس والمراجعات",
                unlocked = totalXp >= 1000
            )
        )
    }
}
