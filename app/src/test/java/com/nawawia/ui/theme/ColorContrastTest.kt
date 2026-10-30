package com.nawawia.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow

class ColorContrastTest {

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            return if (c <= 0.04045f) {
                c.toDouble() / 12.92
            } else {
                ((c.toDouble() + 0.055) / 1.055).pow(2.4)
            }
        }
        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    data class ContrastCheck(
        val scheme: String,
        val pairName: String,
        val foreground: Color,
        val background: Color,
        val minRatio: Double,
        val category: String
    )

    @Test
    fun testAllColorContrastPairsMeetWcagRequirements() {
        val light = LightNawawiaColors
        val dark = DarkNawawiaColors

        val checks = listOf(
            // Light Theme Checks
            ContrastCheck("Light", "ink on bg", light.ink, light.bg, 4.5, "Body text"),
            ContrastCheck("Light", "ink on surface", light.ink, light.surface, 4.5, "Body text"),
            ContrastCheck("Light", "inkMuted on bg", light.inkMuted, light.bg, 4.5, "Secondary text"),
            ContrastCheck("Light", "inkMuted on surface", light.inkMuted, light.surface, 4.5, "Secondary text"),
            ContrastCheck("Light", "onBrand on brand", light.onBrand, light.brand, 4.5, "Button label"),
            ContrastCheck("Light", "onGold on gold", light.onGold, light.gold, 4.5, "Button label"),
            ContrastCheck("Light", "onWrong on wrong", light.onWrong, light.wrong, 4.5, "Button label"),
            ContrastCheck("Light", "brand on bg", light.brand, light.bg, 3.0, "UI component / icon"),
            ContrastCheck("Light", "wrong on bg", light.wrong, light.bg, 3.0, "UI component / icon"),
            ContrastCheck("Light", "info on bg", light.info, light.bg, 3.0, "UI component / icon"),

            // Dark Theme Checks
            ContrastCheck("Dark", "ink on bg", dark.ink, dark.bg, 4.5, "Body text"),
            ContrastCheck("Dark", "ink on surface", dark.ink, dark.surface, 4.5, "Body text"),
            ContrastCheck("Dark", "inkMuted on bg", dark.inkMuted, dark.bg, 4.5, "Secondary text"),
            ContrastCheck("Dark", "inkMuted on surface", dark.inkMuted, dark.surface, 4.5, "Secondary text"),
            ContrastCheck("Dark", "onBrand on brand", dark.onBrand, dark.brand, 4.5, "Button label"),
            ContrastCheck("Dark", "onGold on gold", dark.onGold, dark.gold, 4.5, "Button label"),
            ContrastCheck("Dark", "onWrong on wrong", dark.onWrong, dark.wrong, 4.5, "Button label"),
            ContrastCheck("Dark", "brand on bg", dark.brand, dark.bg, 3.0, "UI component / icon"),
            ContrastCheck("Dark", "wrong on bg", dark.wrong, dark.bg, 3.0, "UI component / icon"),
            ContrastCheck("Dark", "info on bg", dark.info, dark.bg, 3.0, "UI component / icon")
        )

        println("| Scheme | Pair | Category | Ratio | Required | Result |")
        println("|---|---|---|---|---|---|")

        for (check in checks) {
            val ratio = contrastRatio(check.foreground, check.background)
            val pass = ratio >= check.minRatio
            val formattedRatio = String.format("%.2f:1", ratio)
            val formattedRequired = String.format("%.1f:1", check.minRatio)
            val status = if (pass) "PASS" else "FAIL"
            println("| ${check.scheme} | ${check.pairName} | ${check.category} | $formattedRatio | $formattedRequired | $status |")
            assertThat(ratio).isAtLeast(check.minRatio)
        }
    }
}
