package com.nawawia.core.error

/**
 * Domain error representations.
 * Layer boundaries must never throw raw exceptions.
 */
sealed interface AppError {
    data class ContentMissing(val message: String) : AppError
    data class ContentCorrupt(val reason: String) : AppError
    data class AudioUnavailable(val audioId: String) : AppError
    data class StorageFailure(val causeMessage: String) : AppError
    data class LinkBlocked(val url: String, val reason: String) : AppError
    data class Generic(val message: String) : AppError

    /**
     * User-facing Arabic explanation with recovery instruction.
     */
    fun userMessageAr(): String = when (this) {
        is ContentMissing -> "تعذّر العثور على ملف المحتوى. يُرجى إعادة تشغيل التطبيق."
        is ContentCorrupt -> "ملف المحتوى غير صالح أو تالف ($reason). تعذّر بدء التطبيق بمحتوى غير متحقَّق منه."
        is AudioUnavailable -> "الملف الصوتي غير متوفر لهذا الحديث حاليًا."
        is StorageFailure -> "حدث خطأ أثناء حفظ التقدّم: $causeMessage. يُرجى المحاولة لاحقًا."
        is LinkBlocked -> "رابط غير موثوق تم حجبه لأمانك: $url"
        is Generic -> message
    }
}
