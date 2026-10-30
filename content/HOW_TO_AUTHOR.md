# HOW_TO_AUTHOR.md — دليل إدخال المتن وتوليد بصمة التحقق

## أولًا: الدليل بالعربية (لغير المبرمجين)

لإدخال النصوص الحقيقية للأربعين النووية من طبعة محقَّقة ومعتمدة:

1. افتح ملف المحتوى `app/src/main/assets/content/nawawi_v1.json` في أي محرر نصوص يدعم الترميز `UTF-8`.
2. لكل حديث من `h1` إلى `h42`:
   - انسخ المتن المشكول بدقة من النسخة المطبوعة المعتمدة وضعه في حقل `"matnAr"`.
   - أدخل الراوي في `"narratorAr"` والتخريج في `"takhrij.textAr"`.
   - ضع رابط الشرح من موقع الدرر السنية (`https://dorar.net/...`) في `"sharhUrl"`.
3. بعد الانتهاء من تحرير الأحاديث والأسئلة، أعد توليد قيمة `payloadSha256` عبر تشغيل اختبار التحقق أو دالة `ContentValidator.computePayloadSha256(units, hadiths, questions)` وضع القيمة الناتجة في حقل `"integrity": { "payloadSha256": "..." }`.

---

## Second: English Guide (For Non-Programmers)

1. Open `app/src/main/assets/content/nawawi_v1.json` in any UTF-8 text editor.
2. Replace the synthetic fixture strings (`«‹... — TEST FIXTURE›»`) in `hadiths` (`h1`..`h42`) and `questions` with the verified printed text, narrator, takhrīj, and questions.
3. Ensure every `sharhUrl` starts with `https://` and points to an allowlisted domain (`dorar.net`, `islamqa.info`, `sunnah.com`, `shamela.ws`).
4. Recompute the SHA-256 checksum using `ContentValidator.computePayloadSha256` and update `integrity.payloadSha256`.
