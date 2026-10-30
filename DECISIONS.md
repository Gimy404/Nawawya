# DECISIONS.md

This document records architectural, dependency, and design choices left open by the Master Build Prompt, with one line of reasoning for each.

| Decision ID | Topic | Choice | One-Line Rationale |
|---|---|---|---|
| DEC-001 | Application ID | `com.aistudio.nawawia.qxmztk` | Unique identifier following platform requirements while retaining `com.example` Android namespace for R references. |
| DEC-002 | Java Target | Java 17 | Aligns with Kotlin 2.x and compileSdk 36 runtime expectations specified in §2. |
| DEC-003 | Audio Format | Opus mono 24 kHz (.ogg) in assets | Superior compression at ~24 kbps (<20 MB total) requiring zero runtime network dependencies or asset packs in V1. |
| DEC-004 | Media3 Version | `androidx.media3:1.5.1` | Latest stable Media3 release compatible with targetSdk 36 without native page size conflicts. |
| DEC-005 | Room Database | SQLite Room 2.7.0 with manual migration strategy | Fully offline, zero-network structured storage with Migration_1_2 prepared from day one. |
| DEC-006 | Network & AI SDKs | Excluded completely in V1 | Strict adherence to §10 zero-network, zero-LLM, offline-first airplane-mode mandate. |
| DEC-007 | Font Bundling | Downloaded locally via `font-util` into `res/font/` | Complies with offline-first requirement avoiding runtime GoogleFont provider network failures. |
| DEC-008 | Dark Mode Brand Tone | High contrast brand `#3BD68E` with `#06251A` dark ink | Prevents 1.9:1 accessibility failure of white-on-green in dark mode while retaining emerald identity. |
