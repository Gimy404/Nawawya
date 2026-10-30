package com.nawawia.ui.feature

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nawawia.domain.model.Achievement
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.components.ChunkyButton
import com.nawawia.ui.components.ChunkyButtonSize
import com.nawawia.ui.components.ChunkyButtonVariant
import com.nawawia.ui.theme.NawawiaTheme

/**
 * Local Profile & Statistics Screen (حسابي — Section 6.8).
 * Displays Streak Calendar (RTL week order), XP total, Hadiths mastered x/٤٢,
 * labelled per-unit mastery chart, and 8 achievements.
 */
@Composable
fun ProfileScreen(
    state: AppUiState,
    onOpenSettingsClick: () -> Unit,
    onOpenOnboardingClick: () -> Unit,
    onOpenAdabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val arStreak = ArabicNormalizer.formatNumber(state.streakState.streakDays)
    val arXp = ArabicNormalizer.formatNumber(state.totalXp)
    val arMastered = ArabicNormalizer.formatNumber(state.masteredHadithCount)
    val arTotal42 = ArabicNormalizer.formatNumber(42)
    val arMinutes = ArabicNormalizer.formatNumber(state.totalMinutesSpent)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .testTag("profile_screen"),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header + Settings Gear Button
        item(key = "profile_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "حسابي وإنجازاتي",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.ink
                    )
                    Text(
                        text = "محفوظ محليًا على جهازك دون حساب سحابي",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted
                    )
                }

                Row {
                    IconButton(
                        onClick = onOpenAdabClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("profile_adab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "أدب طالب العلم وتنبيه المرجعية",
                            tint = colors.info
                        )
                    }

                    IconButton(
                        onClick = onOpenSettingsClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات",
                            tint = colors.ink
                        )
                    }
                }
            }
        }

        // 4 Summary Stat Cards
        item(key = "stats_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileStatCard(
                        title = "سلسلة الأيام",
                        value = "$arStreak يوم",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        title = "مجموع الخبرة",
                        value = "$arXp XP",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileStatCard(
                        title = "الأحاديث المتقنة",
                        value = "$arMastered / $arTotal42",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        title = "وقت المراجعة",
                        value = "$arMinutes دقيقة",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // RTL Month Streak Calendar
        item(key = "streak_calendar") {
            val shape = RoundedCornerShape(20.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.surface, shape)
                    .border(2.dp, colors.outline, shape)
                    .padding(18.dp)
            ) {
                Text(
                    text = "تقويم المواظبة الشهري",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Week days header in RTL order
                val weekDays = listOf("ح", "ن", "ث", "ر", "خ", "ج", "س")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (dayLabel in weekDays) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.inkMuted,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4 weeks grid (28 days)
                for (week in 0 until 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (dayInWeek in 1..7) {
                            val dayNum = week * 7 + dayInWeek
                            val isActiveDay = dayNum <= state.streakState.streakDays.coerceAtLeast(1)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActiveDay) colors.brand else colors.surfaceAlt,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ArabicNormalizer.formatNumber(dayNum),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isActiveDay) colors.onBrand else colors.inkMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Per-Unit Mastery Bar Chart (with explicit values labelled, never color-only)
        item(key = "unit_mastery_chart") {
            val shape = RoundedCornerShape(20.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.surface, shape)
                    .border(2.dp, colors.outline, shape)
                    .padding(18.dp)
            ) {
                Text(
                    text = "نسبة إتقان الوحدات",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(12.dp))

                for (unit in state.units) {
                    val unitMasteries = unit.hadithIds.map { hId ->
                        state.progressByHadithId[hId]?.masteryPct ?: 0
                    }
                    val avgPct = if (unitMasteries.isEmpty()) 0 else unitMasteries.average().toInt()
                    val arAvg = ArabicNormalizer.formatNumber(avgPct)

                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = unit.titleAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$arAvg%",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.brand
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (avgPct / 100f).coerceIn(0f, 1f) },
                            color = colors.brand,
                            trackColor = colors.surfaceAlt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                        )
                    }
                }
            }
        }

        // Achievements Header
        item(key = "achievements_header") {
            Text(
                text = "الأوسمة والإنجازات",
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink
            )
        }

        items(
            items = state.achievements,
            key = { it.id }
        ) { achievement ->
            AchievementRowCard(achievement = achievement)
        }

        item(key = "profile_footer_buttons") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(
                    label = "فتح الإعدادات والتخصيص",
                    leadingIcon = Icons.Default.Settings,
                    onClick = onOpenSettingsClick,
                    variant = ChunkyButtonVariant.Primary,
                    size = ChunkyButtonSize.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_open_settings_cta")
                )

                ChunkyButton(
                    label = "جولة تعريفية بالتطبيق",
                    onClick = onOpenOnboardingClick,
                    variant = ChunkyButtonVariant.Tonal,
                    size = ChunkyButtonSize.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_onboarding_button")
                )
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface, shape)
            .border(1.5.dp, colors.outline, shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = colors.brand
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted
        )
    }
}

@Composable
private fun AchievementRowCard(achievement: Achievement) {
    val colors = NawawiaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val stateWord = if (achievement.unlocked) "مفتوح" else "مغلق"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (achievement.unlocked) colors.brandSoft else colors.surface, shape)
            .border(1.5.dp, if (achievement.unlocked) colors.brand else colors.outline, shape)
            .padding(16.dp)
            .testTag("achievement_${achievement.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = "وسام ${achievement.titleAr}: ${achievement.descriptionAr} — الحالة: $stateWord"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (achievement.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
            contentDescription = null,
            tint = if (achievement.unlocked) colors.goldPress else colors.inkMuted,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.titleAr,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink
            )
            Text(
                text = achievement.descriptionAr,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted
            )
        }
        if (achievement.unlocked) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = colors.brand,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Settings Screen (الإعدادات — Section 6.9).
 */
@Composable
fun SettingsScreen(
    state: AppUiState,
    onBackClick: () -> Unit,
    onSelectThemeMode: (ThemeModeOption) -> Unit,
    onToggleLargeFont: () -> Unit,
    onToggleTashkeel: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onSelectDailyGoal: (Int) -> Unit,
    onExportBackupClick: () -> String,
    onOpenAdabClick: () -> Unit,
    onRequestResetDataClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val clipboard = LocalClipboardManager.current

    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .testTag("settings_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = colors.ink
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "الإعدادات",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink
            )
        }

        HorizontalDivider(thickness = 1.dp, color = colors.outline)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Selection
            item(key = "theme_section") {
                val shape = RoundedCornerShape(18.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.surface, shape)
                        .border(1.5.dp, colors.outline, shape)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "المظهر اللوني",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (mode in ThemeModeOption.entries) {
                            val selected = state.themeMode == mode
                            val btnShape = RoundedCornerShape(12.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(btnShape)
                                    .background(if (selected) colors.brand else colors.surfaceAlt, btnShape)
                                    .clickable(role = Role.RadioButton) { onSelectThemeMode(mode) }
                                    .padding(vertical = 10.dp)
                                    .testTag("theme_option_${mode.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.labelAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) colors.onBrand else colors.ink
                                )
                            }
                        }
                    }
                }
            }

            // Daily Goal Minutes (5 / 10 / 20)
            item(key = "goal_section") {
                val shape = RoundedCornerShape(18.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.surface, shape)
                        .border(1.5.dp, colors.outline, shape)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "الهدف اليومي للمراجعة",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (mins in listOf(5, 10, 20)) {
                            val selected = state.dailyGoalMinutes == mins
                            val arMins = ArabicNormalizer.formatNumber(mins)
                            val btnShape = RoundedCornerShape(12.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(btnShape)
                                    .background(if (selected) colors.brand else colors.surfaceAlt, btnShape)
                                    .clickable(role = Role.RadioButton) { onSelectDailyGoal(mins) }
                                    .padding(vertical = 10.dp)
                                    .testTag("goal_option_$mins"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$arMins دقائق",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) colors.onBrand else colors.ink
                                )
                            }
                        }
                    }
                }
            }

            // Toggles: Large Font, Tashkeel, Sound, Haptics
            item(key = "toggles_section") {
                val shape = RoundedCornerShape(18.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.surface, shape)
                        .border(1.5.dp, colors.outline, shape)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsSwitchRow(
                        title = "تكبير خط المتن",
                        checked = state.useLargeFont,
                        onCheckedChange = { onToggleLargeFont() },
                        testTag = "settings_font_size_toggle"
                    )
                    HorizontalDivider(thickness = 1.dp, color = colors.outline)
                    SettingsSwitchRow(
                        title = "إظهار التشكيل الكامل للمتن",
                        checked = state.showTashkeel,
                        onCheckedChange = { onToggleTashkeel() },
                        testTag = "settings_tashkeel_toggle"
                    )
                    HorizontalDivider(thickness = 1.dp, color = colors.outline)
                    SettingsSwitchRow(
                        title = "المؤثرات الصوتية التشجيعية",
                        checked = state.soundEnabled,
                        onCheckedChange = { onToggleSound() },
                        testTag = "settings_sound_toggle"
                    )
                    HorizontalDivider(thickness = 1.dp, color = colors.outline)
                    SettingsSwitchRow(
                        title = "الاهتزاز التفاعلي (Haptics)",
                        checked = state.hapticsEnabled,
                        onCheckedChange = { onToggleHaptics() },
                        testTag = "settings_haptics_toggle"
                    )
                }
            }

            // Data Export, Adab Statement, Reset Data
            item(key = "actions_section") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChunkyButton(
                        label = "تصدير تقدّمي (نسخة احتياطية JSON)",
                        leadingIcon = Icons.Default.FileUpload,
                        onClick = {
                            val json = onExportBackupClick()
                            clipboard.setText(AnnotatedString(json))
                        },
                        variant = ChunkyButtonVariant.Tonal,
                        size = ChunkyButtonSize.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_export_button")
                    )

                    ChunkyButton(
                        label = "بيان المنهج وأدب المتعلم",
                        leadingIcon = Icons.Default.Info,
                        onClick = onOpenAdabClick,
                        variant = ChunkyButtonVariant.Tonal,
                        size = ChunkyButtonSize.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_adab_button")
                    )

                    ChunkyButton(
                        label = "حذف بياناتي وإعادة الضبط",
                        leadingIcon = Icons.Default.DeleteForever,
                        onClick = onRequestResetDataClick,
                        variant = ChunkyButtonVariant.Danger,
                        size = ChunkyButtonSize.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_reset_data_button")
                    )
                }
            }

            item(key = "version_footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "نواوية — الإصدار ٢.٠ • المحتوى: ${state.contentVersion}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkMuted
                    )
                    Text(
                        text = "الخطوط المضمّنة (SIL OFL): Baloo Bhaijaan 2 • Readex Pro • Noto Naskh Arabic",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val colors = NawawiaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.ink,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

/**
 * 3-Step Skippable Onboarding & Adab Screen (Section 6.1).
 */
@Composable
fun OnboardingScreen(
    step: Int,
    dailyGoalMinutes: Int,
    onSelectDailyGoal: (Int) -> Unit,
    onNextStepClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val cardShape = RoundedCornerShape(24.dp)

    BackHandler(onBack = onSkipClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "خطوة ${ArabicNormalizer.formatNumber(step)} من ٣",
                style = MaterialTheme.typography.labelLarge,
                color = colors.brand
            )
            ChunkyButton(
                label = "تخطي",
                onClick = onSkipClick,
                variant = ChunkyButtonVariant.Ghost,
                size = ChunkyButtonSize.Medium,
                modifier = Modifier.testTag("onboarding_skip_button")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surface, cardShape)
                .border(2.dp, colors.outline, cardShape)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                1 -> {
                    Text(
                        text = "تحفظ الأربعين النووية وأنت بتلعب",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.brand,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "تسمع المتن مع تكرار المقاطع (أ-ب)، تراجع الفهم، وتختبر حفظك في عشر دقائق يوميًا دون الحاجة للاتصال بالإنترنت.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                        textAlign = TextAlign.Center
                    )
                }
                2 -> {
                    Text(
                        text = "حدّد هدفك اليومي",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.ink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    for (mins in listOf(5, 10, 20)) {
                        val arMins = ArabicNormalizer.formatNumber(mins)
                        val selected = dailyGoalMinutes == mins
                        ChunkyButton(
                            label = "$arMins دقائق يوميًا",
                            onClick = { onSelectDailyGoal(mins) },
                            variant = if (selected) ChunkyButtonVariant.Primary else ChunkyButtonVariant.Tonal,
                            size = ChunkyButtonSize.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("onboarding_goal_$mins")
                        )
                    }
                }
                else -> {
                    Text(
                        text = "تنبيه وأدب",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.brand,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هذا التطبيق وسيلة للمراجعة والحفظ، وليس مرجعًا للفتوى. لفهم الأحكام، راجع أهل العلم.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("onboarding_adab_statement")
                    )
                }
            }
        }

        ChunkyButton(
            label = if (step < 3) "التالي" else "ابدأ الرحلة الآن",
            onClick = onNextStepClick,
            variant = ChunkyButtonVariant.Primary,
            size = ChunkyButtonSize.Large,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_next_button")
        )
    }
}
