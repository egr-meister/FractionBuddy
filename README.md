# FractionBuddy

FractionBuddy is an offline Android app for children. It combines a basic calculator with visual fraction tools: interactive models, comparison, addition, and locally generated practice with explanations. It is written in Kotlin with Jetpack Compose. The interface is in English, and nothing in the app uses the network.

## Features

- **Fraction Workbench (main screen).** A large interactive fraction model with numerator and denominator controls below it. A tool strip switches between **Explore**, **Compare**, **Add** and **Practice**, and a small **Calculator** tab opens the calculator. A Circle / Pizza / Bar selector sits next to the model, and History and Settings are in the top utility bar. On first launch the app opens Explore showing 1/2.
- **Explore.** Pick a denominator from 1 to 12, adjust the numerator, and tap pieces to shade or unshade them. You can switch models and reset. The screen shows the entered fraction, "selected parts / equal parts", the simplified form, "One whole" and "No parts selected".
- **Compare.** Two independent fractions, A and B, drawn as models of the same size. The app asks "Which fraction is larger?" with unscored answers, and **Show comparison** gives an explanation through a common denominator.
- **Add.** Two fractions and **Show sum**, which walks through the four classroom steps. Improper and mixed forms are shown, sums above one are drawn as several equal-sized wholes, and a whole-number result is shown as a whole number.
- **Practice.** Four topics (Identify, Compare, Add, Equivalent) plus Mixed. Every session has 10 questions and no timer. There are hints, explanations and a results review, and you can retry the questions you got wrong.
- **History.** The latest 100 answered sessions, with lifetime totals for each topic.
- **Calculator.** One binary operation at a time using BigDecimal, with a history of the latest 50 calculations.
- **Settings.** Defaults, the preferred model, reduced animation, data clearing behind a grown-up check, and a privacy screen bundled in the app.

## Architecture

The app is a single module, `app`, with manual dependency injection (`AppContainer`) and an MVVM structure using `ViewModel` and `StateFlow`.

```
com.fractionbuddy.app
├── domain/
│   ├── fractions/    Fraction, FractionMath (GCD/LCM), ComparisonBreakdown, AdditionBreakdown,
│   │                 Explanations, FractionWords (spoken text), ModelGeometry, PieceSelection
│   ├── calculator/   CalculatorEngine (BigDecimal state machine)
│   ├── generation/   Topic/Difficulty, DifficultyRules (exact candidate filtering), QuestionGenerator
│   ├── progress/     Score, RetentionPolicy, resumePosition
│   └── Providers.kt  Clock and RandomProvider (injected; seeded in tests)
├── data/
│   ├── local/        Room entities, DAOs, AppDatabase, Migrations
│   └── repository/   PracticeRepository, CalculatorRepository, PreferencesRepository (DataStore)
└── ui/
    ├── workbench/  models/  practice/  calculator/  history/  settings/  theme/
    └── FractionBuddyApp.kt   Navigation Compose host
```

Fraction arithmetic, geometry, question generation, explanations and scoring are pure Kotlin with no Android or Compose dependencies, so they are covered by plain JVM unit tests. Database work runs in suspend functions or Flows, off the main thread.

## Toolchain versions

| Component | Version |
|---|---|
| JDK | 17 (Temurin in CI) |
| Gradle (Wrapper, committed) | 8.14.3 |
| Android Gradle Plugin | 8.13.0 |
| Kotlin / Compose compiler plugin | 2.2.21 |
| KSP | 2.2.21-2.0.5 |
| Compose BOM | 2025.09.00 |
| Room | 2.7.2 |
| DataStore Preferences | 1.1.7 |
| Navigation Compose | 2.9.3 |
| Lifecycle | 2.9.2 |
| Activity Compose | 1.10.1 |
| Core KTX / SplashScreen | 1.17.0 / 1.0.1 |
| Coroutines | 1.10.2 |
| SDK | **compileSdk 36, targetSdk 36, minSdk 26** |
| Build tools (CI) | 36.0.0 |

All versions are pinned in `gradle/libs.versions.toml`. The project uses no React Native, Expo, JavaScript, TypeScript or WebView.

## Setup and build commands

You need JDK 17 and the Android SDK with Platform 36. Set `sdk.dir` in `local.properties` or set `ANDROID_HOME`.

```bash
./gradlew testDebugUnitTest          # unit tests
./gradlew lintRelease                # release lint
./gradlew assembleDebug              # debug APK (no release credentials needed)
./gradlew assembleRelease bundleRelease   # signed release APK + AAB (credentials required)
```

## Fraction rules and exact arithmetic

- A fraction is a non-negative numerator over a **positive** denominator. Denominator 0 is rejected.
- Learning inputs allow denominators from 1 to 12 and numerators from 0 up to the denominator. Negative fractions are not supported.
- All operations use integers: **GCD** to simplify, **LCM** for common denominators, and **cross multiplication** to compare (with `Long` products). Floating point is never used for equality or comparison.
- The entered form is kept, so 2/4 stays 2/4, and the simplified form (1/2) is shown next to it.
- Addition results may be greater than one whole. They are shown as improper (3/2) and mixed (1 1/2) forms, and a whole-number result is shown as "1", never "1 0/4".

## Model behavior and the rendering fallback

- **Circle:** mathematically equal sectors, starting at 12 o'clock and going clockwise (`ModelGeometry.sectors`).
- **Pizza-style circle:** the same geometry as Circle, with a plain crust ring drawn *outside* the sectors so it never hides a boundary. There are no toppings and no food claims.
- **Bar:** a fixed-length rectangle divided into equal cells.
- Every model of a given type has the same overall size, so a whole always means the same amount. Compare draws A and B with identical dimensions.
- Selected pieces differ by colour **and** by a diagonal hatch pattern with a yellow centre marker. At zero the partitions stay visible with nothing shaded, and at one whole every part is shaded.
- The numerator always equals the number of selected pieces (`PieceSelection`). Switching models keeps both values. If the denominator drops below the numerator, the numerator is clamped and the change is announced in a polite live region. The app never substitutes a different fraction.
- **Common denominator up to 24:** the full repartitioned model is drawn (bars at the common denominator, and the sum as wholes).
- **Common denominator above 24** (for example 1/11 + 1/12, which needs 132): the original readable models stay on screen, the symbolic conversion and simplification are shown exactly, and the text reads "The common denominator is 132." No dense or approximate diagram is drawn.
- Every model has a text summary for screen readers, plus custom accessibility actions ("Shade one more part" and "Unshade one part") that do the same thing as tapping pieces.

## Practice generation and scoring

| Level | Denominators | Addition |
|---|---|---|
| Easy | 2, 3, 4 | Same denominators; sum ≤ 1 |
| Medium | 2, 3, 4, 5, 6, 8 | Same or different; common denominator ≤ 24; sum ≤ 2 |
| Hard | 2–12 | Different denominators; common denominator ≤ 24; sum ≤ 2 |

- Questions are chosen from exactly filtered candidate lists (`DifficultyRules`) using an injected `Random`. Tests use seeds.
- Identification questions can include zero and one whole. Compare sessions always include at least one equal-valued pair, such as 1/2 vs 2/4. Equivalence questions always ask about equal values, and Hard also asks you to simplify.
- Identify, Add and Equivalent questions have **4 options with exactly one correct value**. Options are unique by *value*, normalised with GCD, so 1/2 and 2/4 never appear together. Distractors use common mistakes: counting the unshaded parts, swapping numerator and denominator, adding the denominators, forgetting to convert, a nearby fraction, or "adding the same number" to top and bottom.
- Compare questions have three options: A is larger, B is larger, They are equal.
- Answers are stored as canonical codes ("3/4", or A/B/EQUAL). Scoring rebuilds structured `Fraction` values and compares them by exact value. It never compares display text.
- Only the first submitted answer counts. It is recorded once (`UPDATE … WHERE selectedAnswer IS NULL`) in the same Room transaction as the session counts and lifetime totals. After that, the options are disabled, the correct answer is marked, an explanation built from the actual values is shown, and **Next** appears. The app never advances on its own.
- Sessions are saved after every answer. Back keeps the session, and the question index survives process death through `SavedStateHandle`. Only one unfinished session exists at a time, with **Resume** or **End session**. A session ended early keeps its answered questions, and unanswered ones are not counted as errors. A session with no answers is discarded.
- **Try incorrect questions** runs in memory. It is untimed, shows explanations, and never changes the original result.
- History keeps the latest 100 answered sessions. Lifetime totals per topic are stored separately (`progress_totals`), so pruning old sessions never lowers lifetime progress. There are no streaks, rankings, coins or rewards.

## Calculator limits and rounding

- One binary operation at a time (+ − × ÷), with C, backspace and ± (negative operands and results are supported).
- The largest allowed absolute value for an operand or result is **1,000,000**, with **at most 6 fractional digits per operand**.
- Results are rounded to 6 fractional digits with **HALF_UP**. A rounded result is marked approximate (≈) on screen and in history.
- Trailing zeros are removed, leading zeros are normalised, and a second decimal point is ignored.
- Pressing Equals again does not repeat the operation.
- Division by zero and out-of-range results show a friendly message and are **not** added to history.
- History keeps the latest 50 successful calculations. The calculator is separate from the fraction tools and never affects scores. The current entry is saved as a draft in DataStore.

## Offline operation, privacy, storage and backup

- No `INTERNET` or `ACCESS_NETWORK_STATE` permission. The manifest also removes both with `tools:node="remove"` in case a library adds them.
- No runtime permissions, networking libraries, Firebase, ads, analytics, payments, accounts or external links.
- Data is stored only in app-private storage: Room (`fractionbuddy.db`) and DataStore (`fractionbuddy_prefs`).
- Backup is disabled: `android:allowBackup="false"`, `fullBackupContent` (Android 11 and lower) and `dataExtractionRules` (Android 12+) exclude every domain from both **cloud backup and device-to-device transfer**.
- The privacy screen in Settings explains all of this. "Clear all data" requires confirmation and a grown-up check, which is a simple multiplication meant to prevent accidental taps, not to authenticate anyone.

## Permission verification

The CI runs `aapt2 dump permissions` on the release APK and fails if any permission other than AndroidX core's app-scoped `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (signature level, not a runtime permission) appears. To check the merged manifest locally:

```bash
./gradlew :app:processReleaseManifest
cat app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml | grep uses-permission
$ANDROID_HOME/build-tools/36.0.0/aapt2 dump permissions app/build/outputs/apk/release/app-release.apk
```

## Release signing (PKCS12)

`app/build.gradle.kts` defines `signingConfigs.release` with `storeType = "PKCS12"` and assigns it to the release build type. Credentials are read from:

| Environment variable (CI) | `keystore.properties` key (local, git-ignored) |
|---|---|
| `ANDROID_KEYSTORE_PATH` (decoded file) | `storeFile` |
| `ANDROID_KEYSTORE_PASSWORD` | `storePassword` |
| `ANDROID_KEY_ALIAS` | `keyAlias` |
| `ANDROID_KEY_PASSWORD` | `keyPassword` |

If the credentials are missing, `assembleRelease`, `bundleRelease`, `packageRelease` and `signReleaseBundle` **fail**. The build never falls back to debug signing. Debug builds work without credentials.

**GitHub Secrets** (Settings → Secrets and variables → Actions):

- `ANDROID_KEYSTORE_BASE64`: the `.p12` file, base64-encoded (`base64 -w0 release.p12`, or `base64 -i release.p12` on macOS)
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD` (for PKCS12 this is the same as the store password)

Never commit signing material. `.gitignore` excludes `*.p12`, `*.jks`, `*.keystore`, `*.b64` and `keystore.properties`.

**Upload key vs app signing key.** With Play App Signing, this keystore is your **upload key**: you sign the AAB with it, and Google re-signs the APKs it delivers with the **app signing key** that Google holds. Keep the upload keystore and its passwords backed up somewhere private. If it is lost, an upload-key reset can be requested in Play Console. The app signing key itself is managed by Google.

## CI (GitHub Actions)

`.github/workflows/android.yml`:

1. JDK 17 and Android SDK Platform 36 / Build Tools 36.0.0, using the committed Wrapper (validated).
2. `testDebugUnitTest` and `lintRelease`, with reports uploaded.
3. Decodes the PKCS12 keystore into `$RUNNER_TEMP`.
4. `assembleRelease bundleRelease`.
5. `apksigner verify --print-certs` on the APK. The build fails if verification fails or a certificate contains `CN=Android Debug`.
6. `jarsigner -verify` on the AAB, and a comparison of the signer's SHA-256 with the keystore certificate. A self-signed upload certificate is accepted.
7. The permission check, plus a native-library / 16 KB alignment check (`zipalign -c -P 16`, plus `readelf` LOAD alignment ≥ 16 KB if any `.so` is packaged).
8. Uploads the verified artifacts, then deletes the keystore (`if: always()`).

No emulator test runs in CI.

## APK and AAB locations

- APK: `app/build/outputs/apk/release/app-release.apk` (for local verification and sideloading)
- AAB: `app/build/outputs/bundle/release/app-release.aab`. **Submit only the AAB to Google Play.**
- CI artifact: `fractionbuddy-release` (APK, AAB, certificate, permission and native-library reports)

## 16 KB page-size compatibility

The app has no NDK code, and none of its declared dependencies (Compose, Room using framework SQLite, DataStore Preferences, Navigation, Lifecycle, Coroutines) is expected to package `.so` files. The CI step "Check native libraries and 16 KB alignment" lists every `.so` in the final APK and AAB and records the result in `native-libs.txt`. If any native library ever appears, the same step checks the zip alignment and the ELF LOAD segment alignment and fails below 16 KB.

**Status:** pending the first CI run. Read `native-libs.txt` in the release artifact. Targeting API 36 does not by itself prove 16 KB compatibility. If native libraries appear, test on a 16 KB emulator image before claiming runtime compatibility.

## Android 16 (API 36) behavior changes handled

- Edge-to-edge is enforced: the app calls `enableEdgeToEdge()`, and every screen pads with `WindowInsets.safeDrawing` (system bars and cutouts).
- Predictive Back: `android:enableOnBackInvokedCallback="true"`, with `BackHandler` and Navigation Compose, which support predictive Back.
- Orientation and resizability restrictions are ignored on large screens: the app never locks orientation and uses an adaptive layout (side by side at 600 dp and wider).
- No sticky immersive mode, the system bars stay visible, and the screen is not kept awake.

## R8 and resource shrinking

Release builds currently run with `isMinifyEnabled = false` and `isShrinkResources = false`. The plan is:

1. Verify the signed, non-minified release (CI plus the local checks below).
2. Then turn both on, re-run the arithmetic, model interaction, generation, persistence and navigation checks, and keep `app/build/outputs/mapping/release/mapping.txt`.

Step 2 is **pending**.

## Local verification with adb

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat --pid=$(adb shell pidof -s com.fractionbuddy.app) '*:W'
adb shell dumpsys package com.fractionbuddy.app | grep -A5 "requested permissions"
```

Checklist (record the device or emulator, Android version, artifact and result):

| Check | Status |
|---|---|
| First launch in airplane mode opens Explore with 1/2 | pending |
| Circle / Pizza / Bar stay in sync; numerator/denominator controls; tapping pieces | pending |
| Zero and one-whole models | pending |
| Compare: equal-sized models, equal values shown as equal | pending |
| Addition above one whole (3/4 + 3/4), whole-number result (1/2 + 1/2) | pending |
| Large common denominator fallback (1/11 + 1/12 → 132) | pending |
| All practice topics and difficulties; hints; explanations; results; retry | pending |
| Resume after process death (`adb shell am kill com.fractionbuddy.app` while in background) | pending |
| Calculator: rounding, ÷0, out of range, history limit | pending |
| Rotation, large fonts (200%), TalkBack, predictive Back | pending |
| Clear data (calculator / progress / all) behind the grown-up check | pending |
| No permission prompts, crashes or network use | pending |

## Completed checks and pending items

**Completed (in the authoring environment):**
- The pure-Kotlin domain layer (fractions, geometry, calculator, generation, progress) was compiled with Kotlin 2.0.21. All **36 unit tests pass**: GCD/LCM, normalisation and zero, denominator validation, exact comparison, value-based option deduplication, addition and mixed numbers, whole-number results, difficulty constraints across 600 seeded sessions, explanations, model geometry and selection counts, the rendering fallback, calculator rounding and errors, repeated Equals, retention, and resume.
- A manual review of the Compose, Room and Gradle code.

**Pending:**
- A full Gradle and Android build, release lint, and signed APK/AAB. The authoring environment had no Android SDK or Maven access, so the first GitHub Actions run is the first real build.
- Committing the Room schema (`app/schemas/…/1.json`), which the first build generates. It is also uploaded as the `room-schemas` CI artifact.
- 16 KB artifact inspection (first CI run), on-device adb verification (table above), and the R8 step.
