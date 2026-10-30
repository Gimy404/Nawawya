# نواوية (Nawawia) — Full Interactive Application & Automated Verification Plan

Deliver a complete, fully interactive, offline-first Arabic learning application for the 42 hadith of *Al-Arba'in An-Nawawiyyah* where every button, navigation tab, filter, audio control, and quiz interaction works end-to-end—backed by an automated UI test suite that clicks and verifies every control before delivery.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The previous build stopped at Stage 0 (theme & skeleton), leaving the main buttons without navigation handlers. This plan builds out the **entire interactive application across all screens** and adds an **automated Robolectric UI click-test suite** that simulates pressing every button in the app to guarantee zero dead buttons.

- **Confirmed Decision 1 — Navigation Architecture**: Full 4-tab bottom navigation (`الطريق` Path, `المراجعة` Review, `الأحاديث` Hadith Index, `حسابي` Profile) plus full-screen flows for Hadith Reader (`الحديث`), Interactive Quiz (`الدرس`), Lesson Result (`النتيجة`), Settings (`الإعدادات`), and Onboarding.
- **Confirmed Decision 2 — Content & Quiz Experience**: Complete 42-entry catalog structured into 6 thematic units (7 entries per unit) with a working multi-question interactive quiz engine, while strictly honoring the Content Contract (using clearly marked, non-fabricated template/fixture structures where human-authored text is pending).
- **Confirmed Decision 3 — Zero Dead-Button Guarantee**: Every interactive element is assigned a `testTag` and verified via automated Robolectric + Compose UI tests that click each button and assert the resulting screen or state transition.

---

## 1. Overview & Core Concept

- **What It Does**: Allows learners to master the 42 hadith through a winding visual learning path, an interactive reader with A–B audio looping and tashkeel controls, a 6-format gamified quiz engine, and a 10-minute smart spaced-repetition (SM-2) review system.
- **Target Audience**: Arabic-speaking learners (ages 12–45), students, and teachers seeking a tactile, distraction-free, offline memorization companion.
- **Key Value**: Combines Duolingo-grade tactile responsiveness and gamified progression (Hearts, XP, Streaks, Mastery rings) with reverent typography and 100% offline reliability.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Home / Learning Path (`الطريق`)**: Top status bar displays Streak flame, total XP, and Heart bar (tappable to inspect refill status). A sticky `UnitBanner` shows current unit progress with a working "تابع من هنا" (Continue) button. Tapping an unlocked `PathNode` opens a bottom sheet or navigates directly to the Hadith Reader or Lesson; tapping a locked node shows an Arabic snackbar («أكمل الحديث السابق أولًا»).
  2. **Hadith Reader (`الحديث`)**: Displays the sacred text in `MatnCard` using `Noto Naskh Arabic`. Top bar buttons toggle **Tashkeel** on/off, toggle **Text Size** (`matn` ↔ `matnLarge`), toggle **Bookmark**, and **Copy / Share** with automatic attribution. Includes interactive **Key Terms** chips, a `SharhLinkRow` verified by `LinkGuard`, an interactive `AudioPlayerBar` (Play/Pause, speed `0.75× / 1× / 1.25×`, A–B loop markers, repeat `1 / 3 / 5 / ∞`), and a primary `ChunkyButton` («ابدأ الاختبار») that launches the quiz.
  3. **Interactive Quiz Lesson (`الدرس`)**: Presents 5–7 seeded questions across 6 formats (`MCQ`, `FILL_BLANK`, `ORDER_PHRASES` with accessible up/down buttons, `AUDIO_RECOGNITION`, `MATCH_PAIRS`, `TRUE_FALSE`). Tapping an option highlights it; pressing «تحقّق» reveals the Correct/Incorrect banner, updates Hearts/XP in Room, and advances to the next question or Lesson Result screen.
  4. **Lesson Result (`النتيجة`)**: Celebrates completion with XP earned, accuracy score, mastery ring update, and working buttons for «الحديث التالي» (Next Hadith) or «العودة للطريق» (Back to Path).
  5. **Review Hub (`المراجعة`)**: Three interactive cards («مراجعة سريعة» 10-minute flow, «المراجعة الذكية» SM-2 due queue, «الأحاديث الصعبة» lowest mastery) — each with a working «ابدأ الآن» button that launches a customized review session.
  6. **Hadith Index (`الأحاديث`)**: Real-time search bar (tashkeel-insensitive normalized matching) and interactive filter chips (`الكل`, `المحفوظة`, `المتقنة`, `لم تبدأ`). Tapping any hadith card opens its Reader screen.
  7. **Profile (`حسابي`) & Settings (`الإعدادات`)**: Displays RTL monthly streak calendar, mastery bar chart per unit, and 8 achievement badges (tappable for details). Top-bar gear icon opens Settings with working toggles for Theme (Light/Dark/System), Font Size, Tashkeel default, Sound, Haptics, Daily Goal (5/10/20 min), Adab modal, Export/Import progress, and Reset Data with confirmation dialog.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Tactile 3D sticker-like buttons (`ChunkyButton` with 4dp/6dp solid offset lip) paired with warm paper-and-ink reading surfaces.
  - *Color Palette*: Light warm paper (`#FBF9F4`), pure surface cards (`#FFFFFF`), emerald brand (`#12805C` / `#0A5C43` lip), gold accents (`#F2B93B`), and dark theme role-flipped emerald (`#3BD68E` with `#06251A` dark ink).
  - *Typography*: `Baloo Bhaijaan 2` for headings and chunky buttons; `Readex Pro` for UI body and numbers; `Noto Naskh Arabic` exclusively for hadith text and citations.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Single-Activity State-Driven Navigation with Predictive Back**:
  - *Chosen Approach*: Reactive navigation controller supporting bottom tabs and full-screen modal flows (Reader, Lesson, Result, Settings, Onboarding) with full `BackHandler` support on every secondary screen.
  - *Why*: Guarantees instantaneous transitions, preserves state across rotation, and ensures the back button never traps the user or exits unexpectedly mid-lesson (prompts confirmation dialog when exiting an active quiz).
- **Decision 2 — Deterministic Content & Persistence Layer**:
  - *Chosen Approach*: 42 structured catalog entries across 6 units loaded via the verified JSON schema and persisted alongside user progress, bookmarks, hearts, streaks, and SM-2 schedules in Room + DataStore.
  - *Why*: Works 100% offline in airplane mode, keeps cold start under 2 seconds, and strictly respects the non-hallucination rule by using explicit verified schema structures.
- **Decision 3 — Automated Click-Every-Button Verification**:
  - *Chosen Approach*: Build a comprehensive Robolectric + Compose UI test suite that boots the app and clicks every tab, button, toggle, filter, and quiz action before completion.
  - *Why*: Directly addresses the issue of unhooked UI buttons and proves mathematically that every user journey is wired and functional.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Jetpack Compose)                        │
│                                                                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐  │
│  │  PathScreen │  │ReviewScreen │  │ IndexScreen │  │  ProfileScreen   │  │
│  │  (الطريق)   │  │ (المراجعة)  │  │ (الأحاديث)  │  │     (حسابي)      │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └────────┬─────────┘  │
│         │                │                │                  │            │
│         ▼                ▼                ▼                  ▼            │
│  ┌──────────────────────────────┐  ┌───────────────────────────────────┐  │
│  │   HadithReaderScreen (الحديث)│  │      SettingsScreen (الإعدادات)   │  │
│  │  MatnCard + AudioPlayerBar   │  │ Theme / Font / Goal / Reset / Adab│  │
│  └──────────────┬───────────────┘  └───────────────────────────────────┘  │
│                 │                                                         │
│                 ▼                                                         │
│  ┌──────────────────────────────┐         ┌────────────────────────────┐  │
│  │     LessonScreen (الدرس)     │────────►│   ResultScreen (النتيجة)   │  │
│  │  6 Question Types + Reveal   │         │  XP + Mastery + Next Flow  │  │
│  └──────────────────────────────┘         └────────────────────────────┘  │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │ Intents / StateFlow
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                    Domain Layer (Pure Kotlin — Zero Android)              │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐  │
│  │ HeartsEngine │ │ StreakEngine │ │  Sm2Scheduler│ │   Xp & Scoring   │  │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────────┘  │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │ AppResult<T>
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                         Data Layer & AppContainer                         │
│  ┌────────────────────┐  ┌─────────────────────┐  ┌────────────────────┐  │
│  │   ContentLoader    │  │    Room Database    │  │  AudioController   │  │
│  │ + Integrity Gate   │  │ Progress/SRS/Hearts │  │  Media3 + A-B Loop │  │
│  └────────────────────┘  └─────────────────────┘  └────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
```

### Interactive Component & State Mapping

| Screen / Component | Interactive Control | Target Action & State Mutation |
|---|---|---|
| **Bottom Navigation Bar** | 4 Tabs (`الطريق`, `المراجعة`, `الأحاديث`, `حسابي`) | Switches active root tab state; preserves scroll position. |
| **Path Screen (`الطريق`)** | `تابع من هنا` Hero CTA, 42 `PathNode` buttons, Top bar `HeartBar` & `StreakFlame` | Hero CTA opens current active hadith reader; unlocked node opens Reader/Quiz sheet; locked node triggers snackbar; Heart/Streak pills open status dialog. |
| **Hadith Reader (`الحديث`)** | `ابدأ الاختبار` CTA, Tashkeel toggle, Font Size toggle, Bookmark button, Copy/Share buttons, `AudioPlayerBar` (Play, Speed, A-B Loop, Repeat), `SharhLinkRow` | Toggles display tashkeel/size immediately; persists bookmark in Room; copies text + takhrīj to clipboard with snackbar; controls audio playback state; validates & opens sharh link; launches LessonScreen. |
| **Lesson Screen (`الدرس`)** | Answer choices (`MCQ`, `TRUE_FALSE`, `FILL_BLANK`, `ORDER_PHRASES` up/down, `MATCH_PAIRS`, `AUDIO_RECOGNITION`), `تحقّق` / `متابعة` CTA, Close/Back button | Selects choice -> reveals Correct/Incorrect feedback banner -> updates Hearts/XP/SM-2 in Room -> advances to next question or ResultScreen. Back button opens confirmation dialog. |
| **Result Screen (`النتيجة`)** | `الحديث التالي` CTA, `العودة للطريق` button | Advances directly to the next unlocked hadith reader or returns to PathScreen with updated mastery ring. |
| **Review Screen (`المراجعة`)** | `مراجعة سريعة`, `المراجعة الذكية`, `الأحاديث الصعبة` start buttons | Generates targeted review session (42-question sprint, SM-2 due items, or lowest-mastery items) and launches LessonScreen in Review mode. |
| **Index Screen (`الأحاديث`)** | Search input field, Clear search button, Filter chips (`الكل`, `المحفوظة`, `المتقنة`, `لم تبدأ`), Hadith list rows, Bookmark icon per row | Filters 42 hadiths in real time using normalized Arabic matching; toggles bookmark; navigates to HadithReaderScreen on row tap. |
| **Profile & Settings** | Settings gear button, Theme selector, Font size toggle, Tashkeel default toggle, Daily goal selector, Adab screen button, Reset Data button | Mutates preferences in DataStore/state immediately (including live Light/Dark switch); shows Adab modal; resets progress after double confirmation. |

---

## 5. Professional Automated Testing Plan (Verification Gate)

Before delivering the build, the following automated test suites will be executed via `gradle :app:testDebugUnitTest`:

1. **`FullAppNavigationAndButtonsTest` (Robolectric + Compose UI Test)**:
   - Launches `MainActivity` and verifies the Path screen renders all units and nodes.
   - Clicks every Bottom Navigation tab (`الطريق`, `المراجعة`, `الأحاديث`, `حسابي`) and asserts each screen's headline and controls appear.
   - Clicks `تابع من هنا` and unlocked `PathNode` -> verifies transition to `HadithReaderScreen`.
   - Clicks a locked `PathNode` -> verifies the Arabic snackbar «أكمل الحديث السابق أولًا» appears.
   - On `HadithReaderScreen`: clicks Tashkeel toggle, Font Size toggle, Bookmark toggle, Copy button, Audio Play/Speed/Loop buttons, and `ابدأ الاختبار` -> verifies transition to `LessonScreen`.
   - On `LessonScreen`: selects answers, clicks `تحقّق` (Check) and `متابعة` (Continue) through an entire lesson -> verifies transition to `ResultScreen` and back to `PathScreen` with updated XP and mastery.
   - On `ReviewScreen`: clicks all three review mode buttons (`مراجعة سريعة`, `المراجعة الذكية`, `الأحاديث الصعبة`) and verifies each starts a valid review session.
   - On `IndexScreen`: types an Arabic query into the search bar, clicks each filter chip (`الكل`, `المحفوظة`, `المتقنة`, `لم تبدأ`), and clicks a hadith item to open its reader.
   - On `ProfileScreen` & `SettingsScreen`: clicks the Settings button, toggles Dark Mode, changes Daily Goal, opens the Adab dialog, and tests the Reset Data confirmation flow.
2. **Domain & Integrity Unit Tests**:
   - `ColorContrastTest`: 20/20 WCAG 2.1 contrast checks across Light and Dark palettes.
   - `ContentIntegrityTest`: All 7 integrity gate checks (count = 42, sequence 1..42, unit mapping, non-blank takhrīj, allowlisted sharh host, SHA-256 checksum).
   - `StreakEngineTest`: Full 14-case streak matrix (same day, consecutive day, freeze consumption, clock rollback anti-cheat, timezone travel).
   - `HeartsAndSm2Test`: 30-minute heart refill math after process death, XP calculation, SM-2 ease-factor floor and interval scheduling, and `LinkGuard` host security tests.
