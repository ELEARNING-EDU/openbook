# Quality bar — Sách (Android)

Thresholds are fixed once here and enforced by Gradle, not renegotiated per task. A gate is
reported PASS / FAIL / NOT RUN with the exact command and the measured value.

## Gates

| Gate | Command | Threshold | Current |
| --- | --- | --- | --- |
| Unit tests | `./gradlew :app:testDebugUnitTest` | all pass, 0 errors | PASS — 106/106 |
| Lint (variant) | `./gradlew :app:lintDebug` | `abortOnError = true`, no errors | PASS |
| Branch coverage — domain, data, ViewModels | `./gradlew :app:koverXmlReportDebug` | **80%** branch (enforcement waived) | **22.6%** — waiver W-001 |
| Whole app aggregate | — | no threshold | reported only |

Build gate for handoff: `./gradlew :app:testDebugUnitTest :app:assembleDebug` must be green.

## Coverage detail (Kover 0.9.11, `:app:koverXmlReportDebug`, 2026-10-26)

| Package | Branch coverage |
| --- | --- |
| `reader` (ReaderEngine, SpeechReader, CurlView, …) | 126/1223 (10%) |
| `epub` (EpubArchive, Xml, ChapterImages, HtmlText) | 223/496 (45%) |
| `library` (LibraryStore, HomeSections, codecs, files) | 103/267 (39%) |
| `ui` ViewModels (`AppViewModel`, stores VM) | 0/10 (0%) |
| **Filtered total** | **452/1996 (22.65%)** |

The pure-logic classes are well covered (`HomeSections` 37/46, `BookMetadataCodec` 37/42,
`VoiceCatalog` 46/68, `EpubArchive` 163/325). The gap is Android-bound code with no JVM tests
(engagement with `Context`, `StaticLayout`, `TextToSpeech`, custom `View`).

## Active waiver

| ID | Gate | Owner | Reason | Expires |
| --- | --- | --- | --- | --- |
| W-001 | Kover 80% branch | cuong | Pre-existing Android-layer code was never under a coverage gate; reaching 80% needs ViewModel/instrumentation tests for `ReaderEngine`, `SpeechReader`, `LibraryStore`, `CurlView`. The threshold is not lowered and no class is excluded to make the gate green; while this waiver is open the Kover report is measured but not wired into `check`, so `build`/CI stay green. | 2026-12-31 |

Renewing or dropping W-001 is a decision for the owner; a new waiver needs an ID, an owner and an
expiry. Never disable a check, delete a test or add a suppression to turn a gate green.

## Not part of the bar yet

- Instrumentation/Compose UI tests: none (`app/src/androidTest` absent).
- Screenshot/golden tests: none.
- CI: no workflow in the repo; gates are run locally.
