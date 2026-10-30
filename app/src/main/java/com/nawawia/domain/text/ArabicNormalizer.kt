package com.nawawia.domain.text

import com.nawawia.domain.model.Hadith
import java.text.NumberFormat
import java.util.Locale

object ArabicNormalizer {

    // Arabic tashkeel / harakat unicode range: \u064B..\u065F, \u0670, \u06D6..\u06ED
    private val TASHKEEL_REGEX = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

    /**
     * Produces a normalized search copy (never displayed directly as matn):
     * - Strips tashkeel
     * - Normalizes Alef variants (أ إ آ ٱ -> ا)
     * - Normalizes Ta Marbuta (ة -> ه) and Alef Maqsura (ى -> ي)
     */
    fun normalizeForSearch(input: String): String {
        val withoutTashkeel = input.replace(TASHKEEL_REGEX, "")
        val sb = StringBuilder(withoutTashkeel.length)
        for (ch in withoutTashkeel) {
            val mapped = when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                'ة' -> 'ه'
                'ى' -> 'ي'
                'ؤ' -> 'و'
                'ئ' -> 'ي'
                else -> ch
            }
            sb.append(mapped)
        }
        return sb.toString().lowercase(Locale.ROOT).trim()
    }

    /**
     * Strips diacritical marks when the user toggles the Reader's optional tashkeel-free view.
     * Note: The underlying Hadith.matnAr remains strictly immutable.
     */
    fun stripTashkeelOnly(input: String): String {
        return input.replace(TASHKEEL_REGEX, "")
    }

    /**
     * Formats hadith text for clipboard copy or share, automatically appending narrator and takhrij
     * so no hadith text ever leaves the app unattributed (Section 6.3 & 9.6).
     */
    fun formatAttributedHadith(hadith: Hadith): String {
        return buildString {
            append(hadith.titleAr)
            append("\n\n")
            append(hadith.matnAr)
            append("\n\n")
            if (hadith.narratorAr.isNotBlank()) {
                append(hadith.narratorAr)
                append(" — ")
            }
            append("التخريج: ")
            append(hadith.takhrij.textAr)
        }
    }

    /**
     * Formats integers using locale-appropriate numerals (Arabic-Indic ٠١٢٣٤٥٦٧٨٩ in Arabic locale).
     */
    fun formatNumber(value: Int, locale: Locale = Locale.forLanguageTag("ar")): String {
        return NumberFormat.getIntegerInstance(locale).format(value)
    }
}
