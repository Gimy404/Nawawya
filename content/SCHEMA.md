# SCHEMA.md — توصيف مخطط محتوى نواوية (Nawawia Content Schema)

## أولًا: بالعربية (Arabic Specification)

يُحفظ المحتوى في ملف JSON موحّد ومُتحقَّق منه رقميًا عبر بصمة `SHA-256`. يرفض التطبيق تشغيل أي محتوى يخل بأيٍّ من الفحوص السبعة في `ContentValidator`.

### الحقول الأساسية (Top-Level Fields)
- `schemaVersion`: رقم إصدار المخطط (`1`).
- `contentVersion`: رقم إصدار المحتوى (مثل `"nawawi-1.0.0"`).
- `collection`: بيانات المجموعة (`id`, `titleAr`, `titleEn`, `compilerAr`, `expectedHadithCount: 42`).
- `integrity.payloadSha256`: البصمة الرقمية بصيغة Hex للحقول (`units` + `hadiths` + `questions`).
- `units`: مصفوفة من ٦ وحدات تقسم الأحاديث الـ ٤٢ بالترتيب (٧ أحاديث لكل وحدة).
- `hadiths`: مصفوفة من ٤٢ حديثًا مرقمة من `1` إلى `42` دون فجوات أو تكرار، ويشترط لكل حديث:
  - `matnAr`: نص الحديث بالتشكيل الكامل كما في المتن المطبوع المعتمدة (لا يُعدَّل برمجيًا أبدًا).
  - `takhrij.textAr`: نص التخريج الإلزامي (يرفض التطبيق عرض أي حديث بلا تخريج).
  - `sharhUrl`: رابط HTTPS لموقع مشمول في القائمة البيضاء (`dorar.net`, `islamqa.info`, `sunnah.com`, `shamela.ws`).
- `questions`: مصفوفة الأسئلة المرتبطة بمعرفات الأحاديث (`MCQ`, `TRUE_FALSE`, `FILL_BLANK`, `ORDER_PHRASES`, `MATCH_PAIRS`, `AUDIO_RECOGNITION`).

---

## Second: English Specification

All content resides in a single JSON bundle verified at launch by `ContentValidator`.

### Integrity Gate Rules
1. Strict JSON parsing (unknown or missing required keys fail immediately).
2. `hadiths.size == 42`.
3. `hadith.number` covers `1..42` with zero gaps or duplicates.
4. `units` partition all 42 hadith IDs cleanly.
5. Every question maps to a valid `hadithId` and has valid answer payloads.
6. Every hadith has non-blank `matnAr`, non-blank `takhrij.textAr`, and an HTTPS `sharhUrl` on an allowlisted scholarly host.
7. Canonical SHA-256 checksum matches `integrity.payloadSha256`.
