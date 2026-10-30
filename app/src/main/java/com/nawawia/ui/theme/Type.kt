package com.nawawia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * خطوط نواوية — عائلتان، والقاعدة بينهما **معنى لا ذوق**:
 *
 * | الخط | يُستعمل لـ |
 * |---|---|
 * | **Amiri** — إحياء نسخ مطبعة بولاق (١٩٠٥) | كل ما هو **من التراث**: المتن، الراوي، التخريج، عناوين الأحاديث والوحدات، وأرقام الأحاديث |
 * | **Readex Pro** — سانس عربي أولًا | كل ما هو **كلام التطبيق**: الأزرار، التسميات، الإحصاءات |
 *
 * فالمستخدم يعرف من شكل الحرف وحده متى يقرأ كلام النبوة ومتى يقرأ كلام البرنامج.
 *
 * ⚠️ **ما كان قبل هذا الملف:** كانت `res/font/` تحوي ثلاثة خطوط
 * (`baloo_bhaijaan_2`, `readex_pro`, `noto_naskh_arabic`) **كلها مقتطعة على
 * اللاتينية، بصفر حرف عربي**. فكان كل حرف عربي في التطبيق — بما فيه متن الحديث —
 * يُرسم بخط النظام الافتراضي، لا بالخط المضمّن. ونظام الخطوط الموصوف في
 * `DESIGN_SYSTEM.md` لم يكن يظهر على الشاشة أصلًا.
 *
 * الخطوط الحالية مُتحقَّق من تغطيتها: Amiri فيه ٢٥٥ حرفًا عربيًا وحركات التشكيل
 * الثماني كاملة ورمز ﷺ؛ وReadex Pro فيه ٧٣ حرفًا عربيًا والأرقام العربية الهندية.
 * كلاهما بترخيص SIL OFL (نسخة الترخيص في `docs/AMIRI-OFL.txt`).
 *
 * وReadex Pro أُخرج إلى أوزان ثابتة من الخط المتغيّر، لأن `minSdk = 24`
 * والخطوط المتغيّرة لا تُدعم قبل API 26.
 */
val AmiriFontFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)

val ReadexProFontFamily = FontFamily(
    Font(R.font.readex_pro_regular, FontWeight.Normal),
    Font(R.font.readex_pro_medium, FontWeight.Medium),
    Font(R.font.readex_pro_semibold, FontWeight.SemiBold),
    Font(R.font.readex_pro_bold, FontWeight.Bold)
)

/**
 * مُبقاة للتوافق مع أي استدعاء قديم: كل ما كان يطلب خطًا تراثيًا يحصل على Amiri.
 * (`Baloo Bhaijaan 2` خطّ مدوّر طفولي، لا يناسب محتوى نبويًا، وحُذف.)
 */
@Deprecated("استُبدل بـ AmiriFontFamily", ReplaceWith("AmiriFontFamily"))
val NotoNaskhArabicFontFamily = AmiriFontFamily

val NawawiaMaterialTypography = Typography(
    // ── من التراث: Amiri ──────────────────────────────────────────────
    displayLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 46.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),

    // ── كلام التطبيق: Readex Pro ──────────────────────────────────────
    titleMedium = TextStyle(
        fontFamily = ReadexProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 25.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ReadexProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 27.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ReadexProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 23.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ReadexProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ReadexProFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp
    )
)

/**
 * أدوار مخصّصة للنصّ الشرعي وما يتبعه.
 *
 * حجم المتن **٢٢sp فأكثر** عمدًا: أكثر شكوى متكرّرة في مراجعات تطبيقات الأحاديث
 * هي «الخط صغير» و«لا يوجد تكبير» و«النص باهت». وارتفاع السطر ضِعف الحجم لتأخذ
 * الحركات مداها فوق الحرف وتحته دون تداخل.
 */
@Immutable
data class NawawiaCustomTypography(
    /** المتن — لا يُرسم بغير هذا الدور أبدًا. */
    val matn: TextStyle = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 44.sp
    ),
    /** إعداد «تكبير خط المتن». */
    val matnLarge: TextStyle = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 27.sp,
        lineHeight = 54.sp
    ),
    /** سطر الراوي قبل المتن. */
    val narrator: TextStyle = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 32.sp
    ),
    /** التخريج — يُرسم بحِبر الترقيم (`colors.rubric`). */
    val takhrij: TextStyle = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    /** رقم الحديث داخل الخاتم وفي الفهرس. */
    val rubricNumeral: TextStyle = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 28.sp
    )
)

val LocalNawawiaCustomTypography = staticCompositionLocalOf { NawawiaCustomTypography() }
