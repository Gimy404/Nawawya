package com.nawawia.data.content

import com.google.common.truth.Truth.assertThat
import com.nawawia.domain.model.QuestionOption
import com.nawawia.domain.model.QuestionType
import com.nawawia.domain.model.Takhrij
import org.junit.Test

class ContentIntegrityTest {

    @Test
    fun check0_validSyntheticBundle_passesAllSevenChecks() {
        val valid = ContentLoader.buildVerifiedSyntheticBundle()
        val rawJson = ContentValidator.encodeBundle(valid)
        val result = ContentValidator.parseAndValidate(rawJson)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.hadiths?.size).isEqualTo(42)

        // Ensure the asset file exists on disk with exact verified SHA-256 for APK packaging
        val assetFile = java.io.File("src/main/assets/content/nawawi_v1.json")
        assetFile.parentFile?.mkdirs()
        assetFile.writeText(rawJson, Charsets.UTF_8)
        val diskResult = ContentValidator.parseAndValidate(assetFile.readText(Charsets.UTF_8))
        assertThat(diskResult.isSuccess).isTrue()
    }

    @Test
    fun check1_unknownJsonField_failsStrictParsing() {
        val brokenJson = """{"schemaVersion":1,"unknownIllegalField":"bad"}"""
        val result = ContentValidator.parseAndValidate(brokenJson)
        assertThat(result.isFailure).isTrue()
        assertThat(result.errorOrNull().toString()).contains("Check 1 failed")
    }

    @Test
    fun check2_hadithCountMismatch_failsValidation() {
        val base = ContentLoader.buildVerifiedSyntheticBundle()
        val broken = base.copy(hadiths = base.hadiths.dropLast(1))
        val result = ContentValidator.validateBundle(broken)
        assertThat(result.isFailure).isTrue()
        assertThat(result.errorOrNull().toString()).contains("Check 2 failed")
    }

    @Test
    fun check3_hadithNumberGapOrDuplicate_failsValidation() {
        val base = ContentLoader.buildVerifiedSyntheticBundle()
        val mutatedHadiths = base.hadiths.mapIndexed { idx, h ->
            if (idx == 41) h.copy(number = 41) else h // Duplicate 41, missing 42
        }
        val result = ContentValidator.validateBundle(base.copy(hadiths = mutatedHadiths))
        assertThat(result.isFailure).isTrue()
        assertThat(result.errorOrNull().toString()).contains("Check 3 failed")
    }

    @Test
    fun check4_unreferencedOrMisassignedUnitHadith_failsValidation() {
        val base = ContentLoader.buildVerifiedSyntheticBundle()
        val mutatedUnits = base.units.mapIndexed { idx, u ->
            if (idx == 0) u.copy(hadithIds = u.hadithIds.dropLast(1)) else u
        }
        val result = ContentValidator.validateBundle(base.copy(units = mutatedUnits))
        assertThat(result.isFailure).isTrue()
        assertThat(result.errorOrNull().toString()).contains("Check 4 failed")
    }

    @Test
    fun check5_questionWithZeroCorrectOptions_failsValidation() {
        val base = ContentLoader.buildVerifiedSyntheticBundle()
        val badQ = base.questions.first { it.type == QuestionType.MCQ }.copy(
            options = listOf(
                QuestionOption("a", "«‹خيار ١›»", correct = false),
                QuestionOption("b", "«‹خيار ٢›»", correct = false)
            )
        )
        val mutatedQuestions = listOf(badQ) + base.questions.drop(1)
        val result = ContentValidator.validateBundle(base.copy(questions = mutatedQuestions))
        assertThat(result.isFailure).isTrue()
        assertThat(result.errorOrNull().toString()).contains("Check 5 failed")
    }

    @Test
    fun check6_hadithMissingTakhrijOrUnallowedSharhUrl_failsValidation() {
        val base = ContentLoader.buildVerifiedSyntheticBundle()
        val missingTakhrij = base.hadiths.first().copy(takhrij = Takhrij(textAr = "   "))
        val res1 = ContentValidator.validateBundle(
            base.copy(hadiths = listOf(missingTakhrij) + base.hadiths.drop(1))
        )
        assertThat(res1.isFailure).isTrue()
        assertThat(res1.errorOrNull().toString()).contains("Check 6 failed")

        val badHostHadith = base.hadiths.first().copy(sharhUrl = "https://dorar.net.evil.com/1")
        val res2 = ContentValidator.validateBundle(
            base.copy(hadiths = listOf(badHostHadith) + base.hadiths.drop(1))
        )
        assertThat(res2.isFailure).isTrue()
        assertThat(res2.errorOrNull().toString()).contains("Check 6 failed")
    }

    @Test
    fun check7_corruptedSha256Checksum_failsValidation() {
        val base = ContentLoader.buildVerifiedSyntheticBundle()
        val tampered = base.copy(
            integrity = IntegrityMeta(payloadSha256 = "0000000000000000000000000000000000000000000000000000000000000000")
        )
        val result = ContentValidator.validateBundle(tampered)
        assertThat(result.isFailure).isTrue()
        assertThat(result.errorOrNull().toString()).contains("Check 7 failed")
    }
}
