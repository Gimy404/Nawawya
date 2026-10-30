# DESIGN_SYSTEM.md — Nawawia Design System

## 1. Design Direction
Material 3 structure combined with tactile Duolingo-grade pressable surfaces and paper-and-ink reverence.
- **Lip**: Solid 4dp/6dp offset lip with zero blur on `ChunkyButton`. On press, content translates by lip depth over 120ms `EaseOut` and the lip collapses to 0dp without layout shifts.
- **Tonal Elevation**: Material Design 3 surface elevation everywhere else. Zero arbitrary box shadows, zero glassmorphism, zero gradients, zero neumorphism.
- **Typography Separation**:
  - `Baloo Bhaijaan 2`: Display headers, unit banners, and button labels. Never below 15sp, never for body paragraphs or matn.
  - `Readex Pro`: Body copy, explanations, secondary meta, and numerals.
  - `Noto Naskh Arabic`: Reserved strictly and exclusively for sacred text (matn), narrator, and takhrīj lines.

---

## 2. Color Tokens & WCAG 2.1 Contrast Table

Tokens are defined in `app/src/main/java/com/nawawia/ui/theme/Color.kt` and tested against WCAG 2.1 relative luminance in `ColorContrastTest.kt`. Dynamic Color is disabled to ensure brand consistency across all Android devices.

### Contrast Verification Table (from `ColorContrastTest`)

| Scheme | Pair | Category | Token Hexes | Ratio | WCAG Min | Result |
|---|---|---|---|---|---|---|
| Light | ink on bg | Body text | `#14201B` on `#FBF9F4` | 15.94:1 | ≥ 4.5:1 | **PASS** |
| Light | ink on surface | Body text | `#14201B` on `#FFFFFF` | 16.77:1 | ≥ 4.5:1 | **PASS** |
| Light | inkMuted on bg | Secondary text | `#55655C` on `#FBF9F4` | 5.87:1 | ≥ 4.5:1 | **PASS** |
| Light | inkMuted on surface | Secondary text | `#55655C` on `#FFFFFF` | 6.17:1 | ≥ 4.5:1 | **PASS** |
| Light | onBrand on brand | Button label | `#FFFFFF` on `#12805C` | 4.92:1 | ≥ 4.5:1 | **PASS** |
| Light | onGold on gold | Button label | `#1A1407` on `#F2B93B` | 10.27:1 | ≥ 4.5:1 | **PASS** |
| Light | onWrong on wrong | Button label | `#FFFFFF` on `#C3261C` | 5.81:1 | ≥ 4.5:1 | **PASS** |
| Light | brand on bg | UI component / icon | `#12805C` on `#FBF9F4` | 4.68:1 | ≥ 3.0:1 | **PASS** |
| Light | wrong on bg | UI component / icon | `#C3261C` on `#FBF9F4` | 5.52:1 | ≥ 3.0:1 | **PASS** |
| Light | info on bg | UI component / icon | `#0B6E7A` on `#FBF9F4` | 5.66:1 | ≥ 3.0:1 | **PASS** |
| Dark | ink on bg | Body text | `#ECF3EE` on `#0D1411` | 16.55:1 | ≥ 4.5:1 | **PASS** |
| Dark | ink on surface | Body text | `#ECF3EE` on `#151F1A` | 14.98:1 | ≥ 4.5:1 | **PASS** |
| Dark | inkMuted on bg | Secondary text | `#9CAFA5` on `#0D1411` | 8.08:1 | ≥ 4.5:1 | **PASS** |
| Dark | inkMuted on surface | Secondary text | `#9CAFA5` on `#151F1A` | 7.31:1 | ≥ 4.5:1 | **PASS** |
| Dark | onBrand on brand | Button label | `#06251A` on `#3BD68E` (Role flip) | 8.70:1 | ≥ 4.5:1 | **PASS** |
| Dark | onGold on gold | Button label | `#221A06` on `#F7C85A` | 10.97:1 | ≥ 4.5:1 | **PASS** |
| Dark | onWrong on wrong | Button label | `#2A0C09` on `#FF7066` (Role flip) | 6.72:1 | ≥ 4.5:1 | **PASS** |
| Dark | brand on bg | UI component / icon | `#3BD68E` on `#0D1411` | 9.95:1 | ≥ 3.0:1 | **PASS** |
| Dark | wrong on bg | UI component / icon | `#FF7066` on `#0D1411` | 6.90:1 | ≥ 3.0:1 | **PASS** |
| Dark | info on bg | UI component / icon | `#4FC3D1` on `#0D1411` | 8.94:1 | ≥ 3.0:1 | **PASS** |

---

## 3. Typography Scale

All sizes are specified in `sp` and tested to adapt cleanly at system font scale 2.0 without clipping or truncation.

| Role | Family | Size / Line Height | Primary Use |
|---|---|---|---|
| `displayLarge` | Baloo Bhaijaan 2 ExtraBold | 36sp / 44sp | Result scores, hero numbers |
| `headlineLarge` | Baloo Bhaijaan 2 Bold | 28sp / 36sp | Screen top titles |
| `headlineMedium` | Baloo Bhaijaan 2 Bold | 24sp / 32sp | Unit banners |
| `titleLarge` | Baloo Bhaijaan 2 Bold | 20sp / 28sp | Card headers, modal titles |
| `titleMedium` | Readex Pro SemiBold | 17sp / 24sp | Subheaders, list item headers |
| `bodyLarge` | Readex Pro Regular | 16sp / 26sp | Primary body text |
| `bodyMedium` | Readex Pro Regular | 14sp / 22sp | Secondary descriptions, captions |
| `labelLarge` | Baloo Bhaijaan 2 Bold | 15sp / 20sp | ChunkyButton labels |
| `labelMedium` | Readex Pro Medium | 12sp / 16sp | Badges, chips, metadata tags |
| `matn` | Noto Naskh Arabic Regular | 20sp / 40sp | Hadith sacred text (2.0x leading for tashkeel) |
| `matnLarge` | Noto Naskh Arabic Regular | 24sp / 48sp | Large-text reader setting |
| `takhrij` | Readex Pro Medium | 13sp / 20sp | Hadith source line (`inkMuted`) |

---

## 4. Motion Curves & Durations

Defined in `app/src/main/java/com/nawawia/ui/theme/Motion.kt`:
- `EaseOut` = CubicBezierEasing(0.23f, 1f, 0.32f, 1f) — Standard UI feedback
- `EaseInOut` = CubicBezierEasing(0.77f, 0f, 0.175f, 1f) — Screen transitions
- `EaseSheet` = CubicBezierEasing(0.32f, 0.72f, 0f, 1f) — Bottom sheets
- `SpringPlay` = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow) — Delight celebrations
- `LocalReduceMotion`: Reads system `ANIMATOR_DURATION_SCALE` or settings toggle. If `0f`, transforms become 0ms instant updates or 100ms subtle crossfades.
