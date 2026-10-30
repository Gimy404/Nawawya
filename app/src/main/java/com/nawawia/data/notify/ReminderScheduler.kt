package com.nawawia.data.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nawawia.domain.notify.ReminderPlanner
import com.nawawia.domain.time.AsrMadhab
import com.nawawia.domain.time.BundledCities
import com.nawawia.domain.time.CalculationMethod
import com.nawawia.domain.time.PrayerClock
import java.util.Calendar

/**
 * جدولة التذكير اليومي وعرضه.
 *
 * **لماذا `AlarmManager` لا `WorkManager`:** توثيق أندرويد ينصّ على أن العمل الدوري
 * في `WorkManager` **غير مضبوط** (`PeriodicWorkRequest` حدّه الأدنى ١٥ دقيقة، والفاصل
 * «أقلّ زمن بين التكرارات» لا موعدًا)، وأنه يعمل فوق `JobScheduler` الذي **لا يعمل
 * في وضع Doze**. فهاتف موضوع على المنضدة ليلًا لن يُسلِّم تذكير الثامنة والنصف أصلًا.
 *
 * **ولماذا `setAndAllowWhileIdle` لا `setExactAndAllowWhileIdle`:** الأخير يحتاج
 * `SCHEDULE_EXACT_ALARM` (يمنحه المستخدم يدويًا) أو `USE_EXACT_ALARM` (وهو خاضع
 * لمراجعة سياسة Google Play ومخصّص للتطبيقات التي الضبط الزمني وظيفتها الأساسية —
 * ونحن تطبيق مذاكرة، لا منبّه). و`setAndAllowWhileIdle` يعمل في Doze **بلا أي إذن**،
 * وانزياح يصل إلى ساعة لا يضرّ تذكيرًا بمراجعة حديث. فنبقى بلا أذونات.
 *
 * **ولا نطلب `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` أبدًا:** سياسة Play تمنعه
 * لتطبيق كهذا، وهو يبدو عدوانيًّا للمستخدم.
 */
object ReminderScheduler {

    const val CHANNEL_DAILY = "daily_reminder"
    const val CHANNEL_MILESTONES = "milestones"
    const val CHANNEL_FRIDAY = "friday"

    private const val REQUEST_DAILY = 4201
    private const val NOTIFICATION_DAILY = 4202

    const val ACTION_FIRE = "com.nawawia.action.DAILY_REMINDER"
    const val EXTRA_BODY = "body"

    /**
     * تُنشأ القنوات عند الإقلاع.
     * ⚠️ **أهمية القناة غير قابلة للتغيير بعد إنشائها.** يمكن تعديل الاسم والوصف
     * لاحقًا، لا الأهمية ولا الصوت ولا الاهتزاز. وحذف القناة وإعادة إنشائها لتغيير
     * الأهمية **ظاهر للمستخدم**: شاشة الإعدادات تعرض عدد القنوات المحذوفة كمؤشّر
     * إزعاج. فالأهمية تُضبط صحيحةً من الإصدار الأول.
     */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY,
                "تذكير المراجعة اليومي",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكير واحد في اليوم، في الوقت الذي تختاره."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MILESTONES,
                "الإنجازات",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إتمام وحدة، أو بلوغ سلسلة طويلة. صامتة."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FRIDAY,
                "تذكير الجمعة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تذكير إضافي يوم الجمعة. مُطفأ ما لم تُفعّله."
            }
        )
    }

    /** هل يُسمح لنا بالعرض أصلًا؟ يُسأل قبل كل عرض. */
    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * يجدول تذكير الغد (أو اليوم إن لم يفت موعده)، مزاحًا خارج نوافذ الصلاة.
     * يُعاد استدعاؤه بعد كل إطلاق، وعند الإقلاع، وعند تغيّر المنطقة الزمنية —
     * فالمنبّهات تُمحى عند إطفاء الجهاز، والتكرار في `AlarmManager` غير مضبوط أصلًا.
     */
    fun scheduleNext(
        context: Context,
        preferredHour: Int,
        preferredMinute: Int,
        cityId: String,
        methodName: String,
        madhabName: String,
        respectPrayerTimes: Boolean
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val city = BundledCities.byId(cityId) ?: BundledCities.default
        val method = runCatching { CalculationMethod.valueOf(methodName) }
            .getOrElse { BundledCities.suggestedMethod(city) }
        val madhab = runCatching { AsrMadhab.valueOf(madhabName) }.getOrDefault(AsrMadhab.SHAFI)

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // الموعد المطلوب بالدقائق، ثم الإزاحة خارج نافذة صلاة ذلك اليوم.
        fun deliveryMinuteFor(cal: Calendar): Int {
            val prayers = PrayerClock.compute(
                city = city,
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH),
                method = method,
                madhab = madhab
            )
            return ReminderPlanner.avoidPrayerWindows(
                preferredMinute = preferredHour * 60 + preferredMinute,
                prayers = prayers,
                respectPrayerTimes = respectPrayerTimes
            )
        }

        var minuteOfDay = deliveryMinuteFor(target)
        target.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        target.set(Calendar.MINUTE, minuteOfDay % 60)

        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
            minuteOfDay = deliveryMinuteFor(target)
            target.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            target.set(Calendar.MINUTE, minuteOfDay % 60)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_DAILY,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pending)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, target.timeInMillis, pending)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_DAILY,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    /** يعرض التذكير. لا يُستدعى إلا بعد `canPost`. */
    fun show(context: Context, bodyAr: String, channelId: String = CHANNEL_DAILY) {
        if (!canPost(context)) return
        ensureChannels(context)

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("نواوية")
            .setContentText(bodyAr)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyAr))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { contentIntent?.let { setContentIntent(it) } }
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_DAILY, notification)
        }
    }
}

/**
 * يستقبل إطلاق المنبّه، وإعادة الإقلاع، وتغيّر المنطقة الزمنية.
 * لا يقرّر شيئًا بنفسه: يسأل `ReminderPlanner` في طبقة `domain`.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_FIRE -> {
                val body = intent.getStringExtra(ReminderScheduler.EXTRA_BODY)
                if (!body.isNullOrBlank()) {
                    ReminderScheduler.show(context, body)
                }
                // المنبّه يُعاد تسليحه بعد كل إطلاق — لا تكرار تلقائيًّا.
                ReminderBootstrap.rearm(context)
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> ReminderBootstrap.rearm(context)
        }
    }
}

/**
 * الوصلة بين المُستقبِل وتفضيلات المستخدم.
 * تُملأ من `AppContainer` عند الإقلاع؛ وتُترك فارغة في الاختبارات فلا تفعل شيئًا.
 */
object ReminderBootstrap {
    /** تُسنَد من طبقة التطبيق: تقرأ التفضيلات وتعيد الجدولة. */
    @Volatile
    var rearmAction: ((Context) -> Unit)? = null

    fun rearm(context: Context) {
        rearmAction?.invoke(context.applicationContext)
    }
}
