package com.nawawia.data.content

import com.nawawia.core.error.AppError
import com.nawawia.core.result.AppResult
import com.nawawia.domain.model.EncouragementCatalog
import com.nawawia.domain.model.Hadith
import com.nawawia.domain.model.HadithUnit
import com.nawawia.domain.model.Question
import com.nawawia.domain.model.QuestionType
import com.nawawia.domain.security.LinkGuard
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

@Serializable
data class CollectionMeta(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val compilerAr: String,
    val expectedHadithCount: Int = 42,
    val note: String = ""
)

@Serializable
data class IntegrityMeta(
    val payloadSha256: String
)

@Serializable
data class CanonicalPayload(
    val units: List<HadithUnit>,
    val hadiths: List<Hadith>,
    val questions: List<Question>
)

@Serializable
data class ContentBundle(
    val schemaVersion: Int,
    val contentVersion: String,
    val collection: CollectionMeta,
    val integrity: IntegrityMeta,
    val units: List<HadithUnit>,
    val hadiths: List<Hadith>,
    val questions: List<Question>,
    val encouragement: EncouragementCatalog = EncouragementCatalog()
)

object ContentValidator {

    private val strictJson = Json {
        ignoreUnknownKeys = false
        isLenient = false
        encodeDefaults = true
    }

    /**
     * Computes the lowercase hex SHA-256 of the canonical JSON of (units + hadiths + questions).
     */
    fun computePayloadSha256(
        units: List<HadithUnit>,
        hadiths: List<Hadith>,
        questions: List<Question>
    ): String {
        val canonical = CanonicalPayload(
            units = units,
            hadiths = hadiths,
            questions = questions
        )
        val jsonBytes = strictJson.encodeToString(canonical).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(jsonBytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Parses raw JSON and enforces all 7 mandatory Integrity Gate checks (Section 4.4).
     */
    fun parseAndValidate(rawJson: String): AppResult<ContentBundle> {
        // Check 1: Parse strictly (unknown fields or missing required fields -> fail)
        val bundle = try {
            strictJson.decodeFromString<ContentBundle>(rawJson)
        } catch (e: Exception) {
            return AppResult.Failure(
                AppError.ContentCorrupt("Check 1 failed — strict JSON schema error: ${e.message}")
            )
        }

        return validateBundle(bundle)
    }

    fun validateBundle(bundle: ContentBundle): AppResult<ContentBundle> {
        // Check 2: hadiths.size == collection.expectedHadithCount (42)
        if (bundle.collection.expectedHadithCount != 42 || bundle.hadiths.size != bundle.collection.expectedHadithCount) {
            return AppResult.Failure(
                AppError.ContentCorrupt(
                    "Check 2 failed — expected ${bundle.collection.expectedHadithCount} hadiths (42), found ${bundle.hadiths.size}"
                )
            )
        }

        // Check 3: Hadith number values are exactly 1..42, no gaps, no duplicates
        val numbers = bundle.hadiths.map { it.number }
        val expectedNumbers = (1..42).toList()
        if (numbers.sorted() != expectedNumbers || numbers.toSet().size != 42) {
            return AppResult.Failure(
                AppError.ContentCorrupt("Check 3 failed — hadith numbers must be 1..42 with no gaps or duplicates")
            )
        }

        // Check 4: Every unit.hadithIds entry resolves; every hadith belongs to exactly one unit; union covers all 42
        val hadithById = bundle.hadiths.associateBy { it.id }
        if (hadithById.size != 42) {
            return AppResult.Failure(
                AppError.ContentCorrupt("Check 4 failed — duplicate hadith IDs detected")
            )
        }
        val assignedHadithIds = mutableListOf<String>()
        for (unit in bundle.units) {
            if (unit.hadithIds.isEmpty()) {
                return AppResult.Failure(
                    AppError.ContentCorrupt("Check 4 failed — unit '${unit.id}' has no hadiths")
                )
            }
            for (hId in unit.hadithIds) {
                val hadith = hadithById[hId]
                    ?: return AppResult.Failure(
                        AppError.ContentCorrupt("Check 4 failed — unit '${unit.id}' references unknown hadith '$hId'")
                    )
                if (hadith.unitId != unit.id) {
                    return AppResult.Failure(
                        AppError.ContentCorrupt("Check 4 failed — hadith '$hId' unitId '${hadith.unitId}' mismatches unit '${unit.id}'")
                    )
                }
                assignedHadithIds.add(hId)
            }
        }
        if (assignedHadithIds.size != 42 || assignedHadithIds.toSet() != hadithById.keys) {
            return AppResult.Failure(
                AppError.ContentCorrupt("Check 4 failed — units must partition all 42 hadiths exactly once")
            )
        }

        // Check 5: Every question.hadithId resolves and options/payloads are valid
        if (bundle.questions.isEmpty()) {
            return AppResult.Failure(
                AppError.ContentCorrupt("Check 5 failed — question pool cannot be empty")
            )
        }
        for (q in bundle.questions) {
            if (!hadithById.containsKey(q.hadithId)) {
                return AppResult.Failure(
                    AppError.ContentCorrupt("Check 5 failed — question '${q.id}' references unknown hadith '${q.hadithId}'")
                )
            }
            if (q.promptAr.isBlank()) {
                return AppResult.Failure(
                    AppError.ContentCorrupt("Check 5 failed — question '${q.id}' has blank promptAr")
                )
            }
            when (q.type) {
                QuestionType.ORDER_PHRASES -> {
                    if (q.orderedPhrases.size < 2) {
                        return AppResult.Failure(
                            AppError.ContentCorrupt("Check 5 failed — ORDER_PHRASES question '${q.id}' needs >= 2 phrases")
                        )
                    }
                }
                QuestionType.MATCH_PAIRS -> {
                    if (q.matchPairs.size < 2) {
                        return AppResult.Failure(
                            AppError.ContentCorrupt("Check 5 failed — MATCH_PAIRS question '${q.id}' needs >= 2 pairs")
                        )
                    }
                }
                else -> {
                    val correctCount = q.options.count { it.correct }
                    if (q.options.size < 2 || correctCount != 1) {
                        return AppResult.Failure(
                            AppError.ContentCorrupt(
                                "Check 5 failed — question '${q.id}' must have >= 2 options and exactly 1 correct option (found $correctCount)"
                            )
                        )
                    }
                }
            }
            if (q.sharhUrl != null && !LinkGuard.isAllowed(q.sharhUrl)) {
                return AppResult.Failure(
                    AppError.ContentCorrupt("Check 5 failed — question '${q.id}' has unallowed sharhUrl '${q.sharhUrl}'")
                )
            }
        }

        // Check 6: Every hadith has non-blank matnAr, non-blank takhrij.textAr, and allowlisted sharhUrl
        for (h in bundle.hadiths) {
            if (h.matnAr.isBlank()) {
                return AppResult.Failure(
                    AppError.ContentCorrupt("Check 6 failed — hadith '${h.id}' has blank matnAr")
                )
            }
            if (h.takhrij.textAr.isBlank()) {
                return AppResult.Failure(
                    AppError.ContentCorrupt("Check 6 failed — hadith '${h.id}' is missing takhrij.textAr")
                )
            }
            if (!LinkGuard.isAllowed(h.sharhUrl)) {
                return AppResult.Failure(
                    AppError.ContentCorrupt("Check 6 failed — hadith '${h.id}' has disallowed sharhUrl '${h.sharhUrl}'")
                )
            }
        }

        // Check 7: Recompute SHA-256 of canonical payload and compare to integrity.payloadSha256
        val computedSha = computePayloadSha256(bundle.units, bundle.hadiths, bundle.questions)
        if (!computedSha.equals(bundle.integrity.payloadSha256, ignoreCase = true)) {
            return AppResult.Failure(
                AppError.ContentCorrupt(
                    "Check 7 failed — SHA-256 mismatch (expected ${bundle.integrity.payloadSha256}, computed $computedSha)"
                )
            )
        }

        return AppResult.Success(bundle)
    }

    fun encodeBundle(bundle: ContentBundle): String {
        return strictJson.encodeToString(bundle)
    }
}
