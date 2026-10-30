package com.nawawia.domain.time

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * حساب أوقات الصلاة على الجهاز، بلا شبكة وبلا إذن موقع وبلا أي تبعية خارجية.
 *
 * التطبيق لا يعرض أوقات الصلاة للمستخدم، ولا يدّعي أنه تطبيق مواقيت. الغرض الوحيد:
 * ألّا يقع التذكير اليومي داخل نافذة صلاة. ولذلك تكفي دقّة الدقيقة من مدينة يختارها
 * المستخدم، ولا حاجة لإحداثيات الجهاز.
 *
 * الخوارزمية هي حساب الموضع الشمسي المعتاد (معادلة الزمن + الميل) المشتقّ من
 * Astronomical Algorithms لِـ Jean Meeus، وهو ما تقوم عليه مكتبات المواقيت المعروفة.
 *
 * ملف نقي: لا `android.*` ولا `java.time` — يقبل تاريخًا ميلاديًا كأعداد صحيحة،
 * ويُرجِع الدقائق منذ منتصف الليل بالتوقيت المحلي للمدينة.
 */

/** زاويتا الفجر والعشاء تختلفان باختلاف الهيئة الحاسبة. */
enum class CalculationMethod(
    val labelAr: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    /** بعض الهيئات تحسب العشاء بدقائق ثابتة بعد المغرب بدل الزاوية (أم القرى). */
    val ishaMinutesAfterMaghrib: Int = 0
) {
    UMM_AL_QURA("أم القرى — مكة المكرمة", 18.5, 0.0, ishaMinutesAfterMaghrib = 90),
    EGYPTIAN("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    MUSLIM_WORLD_LEAGUE("رابطة العالم الإسلامي", 18.0, 17.0),
    KARACHI("جامعة العلوم الإسلامية — كراتشي", 18.0, 18.0),
    DUBAI("الإمارات", 18.2, 18.2),
    QATAR("قطر", 18.0, 0.0, ishaMinutesAfterMaghrib = 90),
    KUWAIT("الكويت", 18.0, 17.5),
    TURKEY("ديانت — تركيا", 18.0, 17.0),
    NORTH_AMERICA("أمريكا الشمالية — ISNA", 15.0, 15.0)
}

/** العصر: ظلّ المثل (الشافعي) أو ظلّ المثلين (الحنفي). */
enum class AsrMadhab(val labelAr: String, val shadowFactor: Int) {
    SHAFI("الشافعي ومالك وأحمد — ظلّ المثل", 1),
    HANAFI("الحنفي — ظلّ المثلين", 2)
}

/** مدينة من قائمة مضمّنة في التطبيق — بديل إذن الموقع. */
data class GeoCity(
    val id: String,
    val nameAr: String,
    val latitude: Double,
    val longitude: Double,
    /** إزاحة التوقيت عن UTC بالدقائق، موجبة شرقًا. */
    val utcOffsetMinutes: Int
)

/**
 * أوقات اليوم بالدقائق منذ منتصف الليل المحلي.
 * [isValid] تصير false في خطوط العرض العالية حين لا تبلغ الشمس زاوية الفجر أو العشاء
 * أصلًا؛ وحينها يتوقّف التطبيق عن تعديل موعد التذكير ويُبقيه كما اختاره المستخدم.
 */
data class PrayerDayMinutes(
    val fajr: Int,
    val sunrise: Int,
    val dhuhr: Int,
    val asr: Int,
    val maghrib: Int,
    val isha: Int,
    val isValid: Boolean
) {
    /** الصلوات الخمس وحدها — الشروق ليس صلاة ولا يُراعى في التذكير. */
    val fivePrayers: List<Int> get() = listOf(fajr, dhuhr, asr, maghrib, isha)
}

object PrayerClock {

    private const val MINUTES_PER_DAY = 24 * 60

    // ── مثلثات بالدرجات ──────────────────────────────────────────────────
    private fun dSin(d: Double) = sin(d * PI / 180.0)
    private fun dCos(d: Double) = cos(d * PI / 180.0)
    private fun dTan(d: Double) = tan(d * PI / 180.0)
    private fun dArcSin(x: Double) = asin(x) * 180.0 / PI
    private fun dArcCos(x: Double) = acos(x) * 180.0 / PI
    private fun dArcTan2(y: Double, x: Double) = atan2(y, x) * 180.0 / PI

    private fun fixAngle(a: Double): Double {
        val r = a - 360.0 * floor(a / 360.0)
        return if (r < 0) r + 360.0 else r
    }

    private fun fixHour(h: Double): Double {
        val r = h - 24.0 * floor(h / 24.0)
        return if (r < 0) r + 24.0 else r
    }

    /** اليوم اليولياني لمنتصف ليل التوقيت العالمي. */
    internal fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** يُرجِع (ميل الشمس بالدرجات، معادلة الزمن بالساعات). */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)          // الشذوذ الوسطي
        val q = fixAngle(280.459 + 0.98564736 * d)          // الطول الوسطي
        val l = fixAngle(q + 1.915 * dSin(g) + 0.020 * dSin(2 * g)) // الطول الكسوفي
        val e = 23.439 - 0.00000036 * d                     // ميل فلك البروج
        val declination = dArcSin(dSin(e) * dSin(l))
        val rightAscension = fixHour(dArcTan2(dCos(e) * dSin(l), dCos(l)) / 15.0)
        val equationOfTime = q / 15.0 - rightAscension
        return declination to equationOfTime
    }

    private fun midDay(jd: Double, t: Double): Double =
        fixHour(12.0 - sunPosition(jd + t).second)

    /**
     * زمن بلوغ الشمس انخفاضًا قدره [angle] تحت الأفق.
     * يُرجِع NaN إذا لم تبلغ الشمس تلك الزاوية في ذلك اليوم (خطوط العرض العالية).
     */
    private fun sunAngleTime(
        jd: Double,
        latitude: Double,
        angle: Double,
        t: Double,
        beforeNoon: Boolean
    ): Double {
        val declination = sunPosition(jd + t).first
        val noon = midDay(jd, t)
        val numerator = -dSin(angle) - dSin(declination) * dSin(latitude)
        val denominator = dCos(declination) * dCos(latitude)
        if (denominator == 0.0) return Double.NaN
        val ratio = numerator / denominator
        if (ratio > 1.0 || ratio < -1.0) return Double.NaN
        val v = dArcCos(ratio) / 15.0
        return if (beforeNoon) noon - v else noon + v
    }

    private fun asrTime(jd: Double, latitude: Double, shadowFactor: Int, t: Double): Double {
        val declination = sunPosition(jd + t).first
        val angle = -dArcTan2(1.0, shadowFactor + dTan(abs(latitude - declination)))
        return sunAngleTime(jd, latitude, angle, t, beforeNoon = false)
    }

    /** زاوية انخفاص قرص الشمس عند الشروق والغروب. */
    private const val HORIZON_ANGLE = 0.833

    /**
     * يحسب أوقات اليوم [year]/[month]/[day] الميلادي لمدينة [city].
     * يمرّ مرّتين على التقدير الابتدائي، وهو كافٍ لدقّة الدقيقة.
     */
    fun compute(
        city: GeoCity,
        year: Int,
        month: Int,
        day: Int,
        method: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        madhab: AsrMadhab = AsrMadhab.SHAFI
    ): PrayerDayMinutes {
        val jd = julianDate(year, month, day) - city.longitude / (15.0 * 24.0)
        val lat = city.latitude

        // تقديرات ابتدائية بكسور اليوم، ثم تُنقَّح.
        var fajr = 5.0 / 24.0
        var sunrise = 6.0 / 24.0
        var dhuhr = 12.0 / 24.0
        var asr = 13.0 / 24.0
        var maghrib = 18.0 / 24.0
        var isha = 18.0 / 24.0

        repeat(2) {
            fajr = sunAngleTime(jd, lat, method.fajrAngle, fajr, beforeNoon = true) / 24.0
            sunrise = sunAngleTime(jd, lat, HORIZON_ANGLE, sunrise, beforeNoon = true) / 24.0
            dhuhr = midDay(jd, dhuhr) / 24.0
            asr = asrTime(jd, lat, madhab.shadowFactor, asr) / 24.0
            maghrib = sunAngleTime(jd, lat, HORIZON_ANGLE, maghrib, beforeNoon = false) / 24.0
            isha = if (method.ishaMinutesAfterMaghrib > 0) {
                maghrib + method.ishaMinutesAfterMaghrib / (60.0 * 24.0)
            } else {
                sunAngleTime(jd, lat, method.ishaAngle, isha, beforeNoon = false) / 24.0
            }
        }

        // من التوقيت الشمسي الحقيقي إلى الساعة المحلية للمدينة.
        val shiftHours = city.utcOffsetMinutes / 60.0 - city.longitude / 15.0
        val hours = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha).map { it * 24.0 + shiftHours }

        val valid = hours.none { it.isNaN() }
        if (!valid) {
            return PrayerDayMinutes(0, 0, 0, 0, 0, 0, isValid = false)
        }

        val mins = hours.map { (fixHour(it) * 60.0).roundToInt().coerceIn(0, MINUTES_PER_DAY - 1) }
        return PrayerDayMinutes(
            fajr = mins[0],
            sunrise = mins[1],
            dhuhr = mins[2],
            asr = mins[3],
            maghrib = mins[4],
            isha = mins[5],
            isValid = true
        )
    }
}

/**
 * قائمة المدن المضمّنة. مختصرة عمدًا: دقّة الدقيقة على مستوى المدينة تكفي تمامًا
 * لنافذة احترازية طولها أربعون دقيقة، فلا داعي لإذن موقع ولا لقاعدة بيانات مدن ضخمة.
 */
object BundledCities {
    val all: List<GeoCity> = listOf(
        GeoCity("cairo",    "القاهرة",        30.0444, 31.2357, 120),
        GeoCity("alex",     "الإسكندرية",     31.2001, 29.9187, 120),
        GeoCity("makkah",   "مكة المكرمة",    21.4225, 39.8262, 180),
        GeoCity("madinah",  "المدينة المنورة", 24.4686, 39.6142, 180),
        GeoCity("riyadh",   "الرياض",         24.7136, 46.6753, 180),
        GeoCity("jeddah",   "جدة",            21.4858, 39.1925, 180),
        GeoCity("dubai",    "دبي",            25.2048, 55.2708, 240),
        GeoCity("doha",     "الدوحة",         25.2854, 51.5310, 180),
        GeoCity("kuwait",   "الكويت",         29.3759, 47.9774, 180),
        GeoCity("manama",   "المنامة",        26.2285, 50.5860, 180),
        GeoCity("muscat",   "مسقط",           23.5880, 58.3829, 240),
        GeoCity("sanaa",    "صنعاء",          15.3694, 44.1910, 180),
        GeoCity("baghdad",  "بغداد",          33.3152, 44.3661, 180),
        GeoCity("amman",    "عمّان",          31.9454, 35.9284, 180),
        GeoCity("quds",     "القدس",          31.7683, 35.2137, 120),
        GeoCity("damascus", "دمشق",           33.5138, 36.2765, 180),
        GeoCity("beirut",   "بيروت",          33.8938, 35.5018, 120),
        GeoCity("khartoum", "الخرطوم",        15.5007, 32.5599, 120),
        GeoCity("tripoli",  "طرابلس",         32.8872, 13.1913, 120),
        GeoCity("tunis",    "تونس",           36.8065, 10.1815, 60),
        GeoCity("algiers",  "الجزائر",        36.7538,  3.0588, 60),
        GeoCity("rabat",    "الرباط",         34.0209, -6.8416, 60),
        GeoCity("casa",     "الدار البيضاء",   33.5731, -7.5898, 60),
        GeoCity("nouakchott","نواكشوط",        18.0735,-15.9582, 0),
        GeoCity("istanbul", "إسطنبول",        41.0082, 28.9784, 180),
        GeoCity("karachi",  "كراتشي",         24.8607, 67.0011, 300),
        GeoCity("jakarta",  "جاكرتا",         -6.2088,106.8456, 420),
        GeoCity("kualalumpur","كوالالمبور",     3.1390,101.6869, 480),
        GeoCity("london",   "لندن",           51.5074, -0.1278, 0),
        GeoCity("paris",    "باريس",          48.8566,  2.3522, 60),
        GeoCity("berlin",   "برلين",          52.5200, 13.4050, 60),
        GeoCity("toronto",  "تورنتو",         43.6532,-79.3832,-300),
        GeoCity("newyork",  "نيويورك",        40.7128,-74.0060,-300)
    )

    val default: GeoCity get() = all.first()

    fun byId(id: String): GeoCity? = all.firstOrNull { it.id == id }

    /** الهيئة التي يغلب استعمالها في بلد المدينة — يُقلّل سؤال المستخدم. */
    fun suggestedMethod(city: GeoCity): CalculationMethod = when (city.id) {
        "cairo", "alex" -> CalculationMethod.EGYPTIAN
        "makkah", "madinah", "riyadh", "jeddah" -> CalculationMethod.UMM_AL_QURA
        "doha" -> CalculationMethod.QATAR
        "kuwait" -> CalculationMethod.KUWAIT
        "dubai", "muscat" -> CalculationMethod.DUBAI
        "karachi" -> CalculationMethod.KARACHI
        "istanbul" -> CalculationMethod.TURKEY
        "toronto", "newyork" -> CalculationMethod.NORTH_AMERICA
        else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
    }
}
