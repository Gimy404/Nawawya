package com.nawawia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nawawia.domain.model.HadithUnit
import com.nawawia.domain.model.NodeStatus
import com.nawawia.domain.text.ArabicNormalizer
import com.nawawia.ui.theme.NawawiaTheme

/**
 * عناصر «الطريق» في نظام المَشكاة.
 *
 * الطريق ليس تَلّة كرتونية: هو **سلسلة إسناد** تنزل في الصفحة، معقود فيها أربعون
 * خاتمًا وخاتمان. ولكل حديث **حاشية** في الهامش كما تُكتب الحاشية في المخطوط،
 * موصولة بالخاتم بخطّ منقّط. وفاتحة كل وحدة **عُنوان مُذهَّب**.
 *
 * وقد بُني هذا كلّه ورُئي في نموذج تفاعلي قبل كتابته هنا — `docs/prototype/`.
 */

/** مقاسات ثابتة، تُقرأ مرّة ولا تُحسب في مسار الرسم. */
object PathMetrics {
    val SealSize = 70.dp
    val RingSize = 84.dp
    val LipDepth = 4.dp
    /** عرض عمود الخاتم من حافة البدء — عليه يستقرّ شريط الإسناد. */
    val SpineInset = 38.dp
    val GlossInset = 124.dp
    val RowHeight = 84.dp
}

/**
 * الخاتم — عقدة حديث واحد.
 *
 * لا يعتمد على اللون وحده أبدًا: لكل حالة **شكل** (قُفل / علامة / رقم) و**وصف
 * منطوق** لقارئ الشاشة، فضلًا عن اللون.
 */
@Composable
fun IsnadSeal(
    hadithNumber: Int,
    hadithTitle: String,
    status: NodeStatus,
    masteryPct: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val customType = NawawiaTheme.customTypography
    val arNumber = ArabicNormalizer.formatNumber(hadithNumber)
    val arMastery = ArabicNormalizer.formatNumber(masteryPct)

    val spokenStatus = when (status) {
        NodeStatus.LOCKED -> "درسه مغلق — تقرأه وتسمعه الآن، ويُفتح درسه بعد الحديث السابق"
        NodeStatus.AVAILABLE -> "جاهز للبدء"
        NodeStatus.IN_PROGRESS -> "قيد الحفظ — الإتقان $arMastery بالمئة"
        NodeStatus.MASTERED -> "مُتقَن"
    }

    val faceColor: Color
    val lipColor: Color
    val contentColor: Color
    when (status) {
        NodeStatus.LOCKED -> {
            faceColor = colors.surfaceAlt; lipColor = colors.outline; contentColor = colors.inkMuted
        }
        NodeStatus.AVAILABLE -> {
            faceColor = colors.brand; lipColor = colors.brandPress; contentColor = colors.onBrand
        }
        NodeStatus.IN_PROGRESS -> {
            faceColor = colors.brandSoft; lipColor = colors.brand; contentColor = colors.brand
        }
        NodeStatus.MASTERED -> {
            faceColor = colors.goldSoft; lipColor = colors.gold; contentColor = colors.goldInk
        }
    }

    val ringFraction = when (status) {
        NodeStatus.MASTERED -> 1f
        NodeStatus.IN_PROGRESS -> (masteryPct / 100f).coerceIn(0.08f, 0.97f)
        else -> 0f
    }
    val ringColor = colors.gold
    val trackColor = colors.outline
    // الهالة الذهبية تُعلِن النداء الوحيد في الشاشة.
    val auraColor = if (status == NodeStatus.AVAILABLE) colors.gold else Color.Transparent

    Box(
        modifier = modifier
            .size(PathMetrics.RingSize)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("path_node_$hadithNumber")
            .semantics(mergeDescendants = true) {
                contentDescription = "الحديث $arNumber: $hadithTitle — $spokenStatus"
            },
        contentAlignment = Alignment.Center
    ) {
        if (ringFraction > 0f || auraColor != Color.Transparent) {
            Canvas(modifier = Modifier.size(PathMetrics.RingSize)) {
                val stroke = 3.5.dp.toPx()
                val inset = stroke / 2f
                if (ringFraction > 0f) {
                    drawCircle(color = trackColor, style = Stroke(width = stroke))
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * ringFraction,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                if (auraColor != Color.Transparent) {
                    drawCircle(color = auraColor, style = Stroke(width = stroke))
                }
            }
        }

        // الحافة السفلية الصلبة (lip) — إحساس ختم يُضغط، بلا أي ظلّ ضبابي.
        Box(
            modifier = Modifier
                .size(PathMetrics.SealSize)
                .clip(CircleShape)
                .background(lipColor, CircleShape)
                .padding(bottom = PathMetrics.LipDepth)
                .clip(CircleShape)
                .background(faceColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                NodeStatus.LOCKED -> Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
                NodeStatus.MASTERED -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
                else -> Text(
                    text = arNumber,
                    style = customType.rubricNumeral,
                    color = contentColor
                )
            }
        }
    }
}

/**
 * الحاشية — عنوان الحديث في الهامش، موصولًا بالخاتم بخطّ منقّط.
 * (ديولينجو لا يعرض عناوين عقده أصلًا؛ ومحتوانا هو القيمة، فنُظهره.)
 */
@Composable
fun SealGloss(
    title: String,
    isLocked: Boolean,
    masteryPct: Int,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val leaderColor = colors.outline
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = if (isLocked) colors.inkMuted else colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .drawBehind { drawDottedLeader(leaderColor) }
        )
        if (masteryPct in 1..99) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${ArabicNormalizer.formatNumber(masteryPct)}٪",
                style = MaterialTheme.typography.labelMedium,
                color = colors.goldInk
            )
        }
    }
}

/** خطّ منقّط بلا تخصيص كائنات في مسار الرسم. */
private fun DrawScope.drawDottedLeader(color: Color) {
    val dot = 2.dp.toPx()
    val gap = 4.dp.toPx()
    var x = 0f
    val y = size.height / 2f
    while (x < size.width) {
        drawLine(color = color, start = Offset(x, y), end = Offset(x + dot, y), strokeWidth = 1.5.dp.toPx())
        x += dot + gap
    }
}

/**
 * شريط الإسناد — الخيط الذي يصل الخواتم.
 * يُرسم خلف الصفّ، لا كعنصر مستقلّ، فلا يضيف عقدة تخطيط.
 */
fun Modifier.isnadRibbon(color: Color, inset: Dp, topOpen: Boolean, bottomOpen: Boolean): Modifier =
    this.drawBehind {
        // يُرسم على جانب البدء: اليمين في العربية، واليسار لو شُغّل التطبيق بلغة LTR.
        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - inset.toPx() else inset.toPx()
        val w = 3.dp.toPx()
        val dash = 9.dp.toPx()
        val gap = 6.dp.toPx()
        val from = if (topOpen) 0f else size.height / 2f
        val to = if (bottomOpen) size.height else size.height / 2f
        var y = from
        while (y < to) {
            val end = minOf(y + dash, to)
            drawRect(color = color, topLeft = Offset(x - w / 2f, y), size = Size(w, end - y))
            y += dash + gap
        }
    }

/**
 * العُنوان المُذهَّب — فاتحة الوحدة، كما يُفتتح باب في المخطوط.
 * شريط بلون الوحدة، عليه شبكة الگِرِه (نجمة ثمانية متشابكة)، وإطار ذهبي رفيع،
 * وخرطوشة فيها رقم الوحدة.
 */
@Composable
fun UnwanBanner(
    unit: HadithUnit,
    masteredCount: Int,
    firstHadithNumber: Int,
    lastHadithNumber: Int,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val customType = NawawiaTheme.customTypography
    val band = colors.bandFor(unit.colorRole)
    val shape = RoundedCornerShape(3.dp)
    val total = unit.hadithIds.size.coerceAtLeast(1)

    val latticeColor = Color.White.copy(alpha = 0.18f)
    val frameColor = colors.gold.copy(alpha = 0.55f)
    val goldBright = colors.gold

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(band, shape)
            .drawBehind {
                drawGirihLattice(latticeColor)
                drawInsetFrame(frameColor)
            }
            .padding(horizontal = 16.dp, vertical = 15.dp)
            .testTag("unit_banner_${unit.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // الخرطوشة — رقم الوحدة بحِبر على ذهب
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(goldBright, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ArabicNormalizer.formatNumber(unit.order),
                    style = customType.rubricNumeral,
                    color = colors.onGold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = unit.titleAr,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "الأحاديث ${ArabicNormalizer.formatNumber(firstHadithNumber)}" +
                        "–${ArabicNormalizer.formatNumber(lastHadithNumber)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }

        Spacer(modifier = Modifier.height(11.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.Black.copy(alpha = 0.26f), RoundedCornerShape(99.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (masteredCount.toFloat() / total).coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(goldBright, RoundedCornerShape(99.dp))
            )
        }
    }
}

/** شبكة الگِرِه: مربّع ومربّع مُدار ٤٥° = خاتم سليمان، مُكرّرة. بخطوط فقط، بلا تخصيص. */
private fun DrawScope.drawGirihLattice(color: Color) {
    val tile = 34.dp.toPx()
    val pad = 7.dp.toPx()
    val side = tile - pad * 2
    val stroke = 1.dp.toPx()
    val half = side / 2f
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            val l = x + pad
            val t = y + pad
            drawRect(color = color, topLeft = Offset(l, t), size = Size(side, side), style = Stroke(stroke))
            val cx = l + half
            val cy = t + half
            val r = half * 1.414f
            drawLine(color, Offset(cx, cy - r), Offset(cx + r, cy), stroke)
            drawLine(color, Offset(cx + r, cy), Offset(cx, cy + r), stroke)
            drawLine(color, Offset(cx, cy + r), Offset(cx - r, cy), stroke)
            drawLine(color, Offset(cx - r, cy), Offset(cx, cy - r), stroke)
            x += tile
        }
        y += tile
    }
}

/** إطار المخطوط الرفيع، داخل الحافة بـ ٥dp. */
private fun DrawScope.drawInsetFrame(color: Color) {
    val i = 5.dp.toPx()
    drawRect(
        color = color,
        topLeft = Offset(i, i),
        size = Size(size.width - i * 2, size.height - i * 2),
        style = Stroke(1.dp.toPx())
    )
}

/**
 * المَشكاة — مصباح الوحدة.
 *
 * رفيق التطبيق، وهو **غير مُصوَّر** عمدًا: لا يجوز رسم نبيّ ولا صحابيّ ولا إجراء
 * كلام على ألسنتهم، فلا مكان لشخصية ناطقة كما في تطبيقات اللغات. والمصباح يعطي
 * الأثر العاطفيّ نفسه بلا محذور: يُضاء عند إتمام الوحدة.
 */
@Composable
fun MishkahLamp(
    isLit: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 78.dp
) {
    val colors = NawawiaTheme.colors
    val stroke = if (isLit) colors.gold else colors.outline
    val flame = colors.gold
    val flameCore = colors.goldSoft
    val bowl = if (isLit) colors.goldSoft else Color.Transparent

    Box(modifier = modifier.size(width = size, height = size * 92f / 78f)) {
        Canvas(modifier = Modifier.size(width = size, height = size * 92f / 78f)) {
            val s = this.size.width / 78f
            fun p(x: Float, y: Float) = Offset(x * s, y * s)
            val lineW = 2.2f * s

            // قوس المشكاة
            val niche = Path().apply {
                moveTo(5f * s, 90f * s)
                lineTo(5f * s, 36f * s)
                cubicTo(5f * s, 18f * s, 20f * s, 5f * s, 39f * s, 5f * s)
                cubicTo(58f * s, 5f * s, 73f * s, 18f * s, 73f * s, 36f * s)
                lineTo(73f * s, 90f * s)
            }
            drawPath(niche, color = stroke, style = Stroke(width = lineW))

            // قوس داخلي رفيع
            val inner = Path().apply {
                moveTo(12f * s, 90f * s)
                lineTo(12f * s, 37f * s)
                cubicTo(12f * s, 23f * s, 24f * s, 12f * s, 39f * s, 12f * s)
                cubicTo(54f * s, 12f * s, 66f * s, 23f * s, 66f * s, 37f * s)
                lineTo(66f * s, 90f * s)
            }
            drawPath(inner, color = stroke.copy(alpha = 0.45f), style = Stroke(width = 1f * s))

            // سلك التعليق
            drawLine(stroke, p(39f, 43f), p(39f, 48f), 1.4f * s)

            // كأس المصباح
            val cup = Path().apply {
                moveTo(26f * s, 48f * s)
                lineTo(52f * s, 48f * s)
                lineTo(47.5f * s, 64.5f * s)
                cubicTo(47.5f * s, 69f * s, 44f * s, 71.5f * s, 39f * s, 71.5f * s)
                cubicTo(34f * s, 71.5f * s, 30.5f * s, 69f * s, 30.5f * s, 64.5f * s)
                close()
            }
            if (bowl != Color.Transparent) drawPath(cup, color = bowl)
            drawPath(cup, color = stroke, style = Stroke(width = 1.9f * s))
            drawLine(stroke.copy(alpha = 0.55f), p(30.5f, 54f), p(47.5f, 54f), 1f * s)

            // اللهب — لا يُرسم إلا مُضاءً
            if (isLit) {
                val fire = Path().apply {
                    moveTo(39f * s, 25f * s)
                    cubicTo(42.6f * s, 29.2f * s, 44.6f * s, 32f * s, 44.6f * s, 35.2f * s)
                    cubicTo(44.6f * s, 38.6f * s, 42.1f * s, 41f * s, 39f * s, 41f * s)
                    cubicTo(35.9f * s, 41f * s, 33.4f * s, 38.6f * s, 33.4f * s, 35.2f * s)
                    cubicTo(33.4f * s, 32f * s, 35.4f * s, 29.2f * s, 39f * s, 25f * s)
                    close()
                }
                drawPath(fire, color = flame)
                val core = Path().apply {
                    moveTo(39f * s, 31.5f * s)
                    cubicTo(40.7f * s, 33.6f * s, 41.6f * s, 35f * s, 41.6f * s, 36.6f * s)
                    cubicTo(41.6f * s, 38.3f * s, 40.4f * s, 39.5f * s, 39f * s, 39.5f * s)
                    cubicTo(37.6f * s, 39.5f * s, 36.4f * s, 38.3f * s, 36.4f * s, 36.6f * s)
                    cubicTo(36.4f * s, 35f * s, 37.3f * s, 33.6f * s, 39f * s, 31.5f * s)
                    close()
                }
                drawPath(core, color = flameCore)
            }
        }
    }
}

/** مصباح الوحدة في الطريق، مع سطر حاله. */
@Composable
fun UnitLampNode(
    unitOrder: Int,
    isLit: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val arOrder = ArabicNormalizer.formatNumber(unitOrder)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("unit_lamp_$unitOrder")
            .semantics(mergeDescendants = true) {
                contentDescription = if (isLit) {
                    "مصباح الوحدة $arOrder مُضاء — أتممتَها"
                } else {
                    "مصباح الوحدة $arOrder يُضيء بإتمام الوحدة"
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MishkahLamp(isLit = isLit, size = 76.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isLit) "مصباح الوحدة $arOrder مُضاء" else "يُضيء بإتمام الوحدة",
            style = MaterialTheme.typography.labelMedium,
            color = if (isLit) colors.goldInk else colors.inkMuted,
            textAlign = TextAlign.Center
        )
    }
}
