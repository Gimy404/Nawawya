# ═══════════════════════════════════════════════════════════════════
# قواعد R8 لنواوية.
#
# ⚠️ الانكماش مُطفأ حاليًا (`isMinifyEnabled = false` في app/build.gradle.kts).
# هذه القواعد مكتوبة وجاهزة، فمتى بنيتَ نسخة release وجرّبتَها على جهاز
# يكفي قلب المفتاح. ولا يُقلب قبل تلك التجربة.
# ═══════════════════════════════════════════════════════════════════

# ── kotlinx.serialization ──────────────────────────────────────────
# المُسلسِلات تُولَّد كأعضاء ثابتة ويُوصل إليها بالانعكاس عبر الصنف الرفيق.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.nawawia.**$$serializer { *; }
-keepclassmembers class com.nawawia.** {
    *** Companion;
}
-keepclasseswithmembers class com.nawawia.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Room ───────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ── نماذج المجال ───────────────────────────────────────────────────
# تُقرأ من JSON بأسماء حقولها، فلا تُعاد تسميتها.
-keep class com.nawawia.domain.model.** { *; }

# ── مُستقبِل التذكير ────────────────────────────────────────────────
# يُنشَأ بالاسم من الـ Manifest.
-keep class com.nawawia.data.notify.ReminderReceiver { *; }

# ── Compose ────────────────────────────────────────────────────────
# لا يحتاج قواعد خاصة؛ R8 يتعامل معه. هذا لمنع تحذيرات أدوات التطوير فقط.
-dontwarn androidx.compose.ui.tooling.**

# ── أثر البناء ─────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
