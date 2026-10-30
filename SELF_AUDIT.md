# SELF_AUDIT.md — سجل الفحص الذاتي الشامل (١–٥٠)

| # | المعيار | الحالة | الدليل (File / Test Reference) |
|---|---|---|---|
| 1 | عدم توليد أي حديث أو تخريج أو شرح من الذكاء الاصطناعي | **PASS** | `ContentLoader.kt:35-205` (استخدام قوالب اختبارية موسومة بوضوح `TEST FIXTURE`) |
| 2 | بوابة التحقق السباعية + ٧ اختبارات فشل | **PASS** | `ContentValidator.kt:55-185`, `ContentIntegrityTest.kt:12-95` |
| 3 | التحقق من ٤٢ حديثًا مرقمة ١..٤٢ بلا فجوات | **PASS** | `ContentValidator.kt:71-85`, `ContentIntegrityTest.kt:28-46` |
| 4 | عدم عرض أي حديث بدون سطر التخريج | **PASS** | `MatnComponents.kt:85-92`, `ContentValidator.kt:160-165` |
| 5 | الحفاظ على المتن والتشكيل دون قص أو حذف | **PASS** | `MatnComponents.kt:70-80` |
| 6 | نسخ ومشاركة المتن يرفق التخريج تلقائيًا | **PASS** | `ArabicNormalizer.kt:42-56`, `HadithReaderScreen.kt:152-160` |
| 7 | أذونات Manifest بالحد الأدنى فقط | **PASS** | `AndroidManifest.xml:5-7` |
| 8 | خلو المشروع من أي مفاتيح سرية أو ملفات تتبع | **PASS** | `app/build.gradle.kts:80-125` |
| 9 | عدم وجود أي حزم إعلانات أو تحليلات | **PASS** | `app/build.gradle.kts:80-125` |
| 10 | `LinkGuard` يتحقق من النطاق ويحجب `dorar.net.evil.com` | **PASS** | `LinkGuard.kt:18-56`, `GameSystemsAndStreakTest.kt:198-211` |
| 11 | تصريح `android:exported` و `allowBackup` | **PASS** | `AndroidManifest.xml:10-25` |
| 12 | تصدير وحذف البيانات يمسح التقدم بالكامل | **PASS** | `NawawiaDatabase.kt:109-115`, `FullAppNavigationAndButtonsTest.kt:238-245` |
| 13 | قراءة الوقت حصريًا عبر `AppClock` | **PASS** | `AppClock.kt:10-55`, `StreakEngine.kt:13-15`, `HeartsEngine.kt:15` |
| 14 | مصفوفة اختبارات الـ Streak الـ ١٤ كاملة | **PASS** | `GameSystemsAndStreakTest.kt:22-145` |
| 15 | تجدد القلوب بعد ٣ أيام دون مؤقتات خلفية | **PASS** | `HeartsEngine.kt:14-46`, `GameSystemsAndStreakTest.kt:151-169` |
| 16 | ترحيل Room (`MIGRATION_1_2`) دون تدمير البيانات | **PASS** | `NawawiaDatabase.kt:129-142` |
| 17 | حفظ التقدم بعد كل إجابة في معاملة Room | **PASS** | `NawawiaDatabase.kt:93-107`, `NawawiaViewModel.kt:670-688` |
| 18 | ترتيب الأسئلة ببذرة ثابتة (`seed: Long`) | **PASS** | `NawawiaViewModel.kt:340-365` |
| 19 | إرجاع `AppResult` عبر الطبقات دون استثناءات خام | **PASS** | `AppResult.kt:1-38`, `ContentValidator.kt:55` |
| 20 | `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24` | **PASS** | `app/build.gradle.kts:12-19` |
| 21 | دعم `enableEdgeToEdge()` وهوامش النظام | **PASS** | `MainActivity.kt:18`, `NawawiaAppRoot.kt:95-100` |
| 22 | دعم الرجوع التنبؤي وتأكيد الخروج من الدرس | **PASS** | `AndroidManifest.xml:13`, `LessonScreen.kt:67`, `NawawiaAppRoot.kt:315-350` |
| 23 | ثبات الحالة عبر التدوير وتغيير المظهر | **PASS** | `NawawiaViewModel.kt:212-235` |
| 24 | تصميم RTL أصيل بجميع الشاشات | **PASS** | `NawawiaAppRoot.kt:85` |
| 25 | إعداد اللغات `locales_config.xml` | **PASS** | `app/src/main/res/xml/locales_config.xml` |
| 26 | أوصاف قارئ الشاشة (`contentDescription`) بالعربية | **PASS** | `StatusComponents.kt`, `PathComponents.kt`, `AnswerChoice.kt` |
| 27 | جميع العناصر التفاعلية بحجم لا يقل عن `48dp` | **PASS** | `ChunkyButton.kt:42-45`, `AnswerChoice.kt:92` |
| 28 | بديل أزرار (أعلى/أسفل) لترتيب المقاطع دون سحب | **PASS** | `LessonScreen.kt:275-330`, `FullAppNavigationAndButtonsTest.kt:154-160` |
| 29 | احترام تقليل الحركة (`LocalReduceMotion`) | **PASS** | `Motion.kt:29`, `Theme.kt:41-52`, `ChunkyButton.kt:79` |
| 30 | دعم تكبير الخط `2.0` دون قص للنصوص | **PASS** | `ChunkyButton.kt:199-215`, `MatnComponents.kt:70-80` |
| 31 | التحقق من تباين الوضع الليلي وقلب لون النص على الزمردي | **PASS** | `Color.kt:66`, `ColorContrastTest.kt:54-65` |
| 32 | استخدام أيقونات Material المتجهة فقط دون Emoji | **PASS** | `StatusComponents.kt`, `PathComponents.kt` |
| 33 | استخدام `Snackbar` بدلًا من `Toast` | **PASS** | `NawawiaAppRoot.kt:101-120` |
| 34 | إقلاع فوري دون اتصال بالشبكة | **PASS** | `MainActivity.kt:16-28`, `AppContainer.kt` |
| 35 | قواعد الانكماش والـ Room و Serialization | **PASS** | `app/build.gradle.kts` |
| 36 | مفاتيح ثابتة (`key`) لجميع القوائم الكسولة | **PASS** | `PathScreen.kt:150`, `ReviewAndIndexScreens.kt:340` |
| 37 | نماذج واجهة `@Immutable` لتقليل إعادة التركيب | **PASS** | `NawawiaViewModel.kt:63, 102, 116` |
| 38 | عدم تخصيص كائنات مكلفة داخل مسار الرسم | **PASS** | `PathComponents.kt:47-50` |
| 39 | تحرير مشغل الصوت عند إغلاق الـ ViewModel | **PASS** | `NawawiaViewModel.kt:711-714` |
| 40 | استهلاك ذاكرة منخفض (رسوم متجهة فقط) | **PASS** | `ic_launcher_foreground.xml` |
| 41 | موارد النصوص في `strings.xml` و `values-en/strings.xml` | **PASS** | `app/src/main/res/values/strings.xml` |
| 42 | خلو طبقة `domain` من أي استيراد `android.*` | **PASS** | `app/src/main/java/com/nawawia/domain/` |
| 43 | خلو الكود من أي `TODO` أو أزرار معطلة | **PASS** | `FullAppNavigationAndButtonsTest.kt` |
| 44 | اختبارات الوحدات الشاملة لجميع المحركات | **PASS** | `GameSystemsAndStreakTest.kt`, `ContentIntegrityTest.kt`, `ColorContrastTest.kt` |
| 45 | اختبارات Robolectric Compose UI لجميع الشاشات والأزرار | **PASS** | `FullAppNavigationAndButtonsTest.kt` |
| 46 | تغليف حالات التحميل والفراغ والخطأ عبر `StateBox` | **PASS** | `StateBox.kt`, `ReviewAndIndexScreens.kt:326` |
| 47 | رسائل خطأ عربية واضحة مع إجراء الاستعادة | **PASS** | `AppError.kt:15-24`, `ContentUnavailableScreen.kt` |
| 48 | جاهزية المتجر (أيقونة متكيفة، `SplashScreen`، `versionCode = 2`) | **PASS** | `MainActivity.kt:16`, `app/build.gradle.kts:20-21` |
| 49 | شاشة الأدب وعدم ادعاء الإفتاء في الترحيب والإعدادات | **PASS** | `ProfileAndSettingsScreens.kt:632-646`, `NawawiaAppRoot.kt:285-312` |
| 50 | توثيق الفحص الذاتي بالأدلة | **PASS** | `SELF_AUDIT.md` |
