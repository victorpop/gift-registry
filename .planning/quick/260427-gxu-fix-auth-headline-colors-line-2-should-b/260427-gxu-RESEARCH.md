# Quick Task: Bundle Instrument Serif + Fix Auth Headline Colors — Research

**Researched:** 2026-04-27
**Domain:** Compose font bundling, AnnotatedString color spans
**Confidence:** HIGH

---

## 1. Font Bundling Strategy

### Compose FontFamily selection when multiple Font entries match

Compose's `FontFamily` resolution picks the **first** entry that matches the requested `FontWeight` and `FontStyle`. There is no weighted-random: the list is scanned top-to-bottom and the first exact (or closest-weight) match wins. Consequence: adding `Font(R.font.xxx)` entries **before** the `Font(googleFont=...)` entries makes local resources take priority. Adding them **after** makes local resources the fallback only if the Google Fonts provider returns null.

**Decision — use local resources as fallback (append after GMS entries):**

```kotlin
val InstrumentSerifFamily: FontFamily = FontFamily(
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider,
         weight = FontWeight.Normal),
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider,
         weight = FontWeight.Normal, style = FontStyle.Italic),
    // Fallback: used when Google Play Services unavailable
    Font(resId = R.font.instrument_serif_regular, weight = FontWeight.Normal),
    Font(resId = R.font.instrument_serif_italic,  weight = FontWeight.Normal,
         style = FontStyle.Italic),
)
```

This matches the documented Compose fallback pattern (Android Developers — "Using fonts in Compose"). GMS entries load asynchronously; local entries are synchronous. On a non-GMS device or first cold launch before the download completes, the local file renders immediately. Once the GMS download succeeds it takes over for subsequent compositions.

### Should we also bundle Inter and JetBrains Mono?

APK size analysis (uncompressed TTF):

| Family | Weights/styles to bundle | Approx size |
|--------|--------------------------|-------------|
| Instrument Serif | Regular + Italic (2 files) | ~80 KB |
| Inter | Regular + Medium + SemiBold (3 files) | ~240 KB |
| JetBrains Mono | Medium + SemiBold (2 files) | ~150 KB |
| **Total if all three** | | **~470 KB** |

**Recommendation: bundle Instrument Serif only.**

Rationale:
- Instrument Serif is brand-critical — it renders the wordmark, the auth headline, and every display heading. A system-serif fallback is visually wrong and brand-breaking.
- Inter and JetBrains Mono are body/label fonts. System sans-serif is an acceptable fallback for a first-launch flash; the GMS download completes within seconds on any production device. A wrong sans font is far less conspicuous than a wrong display serif.
- +80 KB vs +470 KB is the right tradeoff for v1.1.
- Handoff anti-pattern to avoid (from 08-RESEARCH.md): "Bundling .ttf for Inter + JetBrains Mono. Adds ~400 KB APK for fonts that download free via Google Fonts. Handoff is explicit: pull from Google Fonts." Instrument Serif is the exception because brand-critical display use.

---

## 2. Obtaining the Instrument Serif TTF Files

Google Fonts CSS API returns these URLs (verified via live curl against Google Fonts API, 2026-04-27):

```
# Regular (upright):
https://fonts.gstatic.com/s/instrumentserif/v5/jizBRFtNs2ka5fXjeivQ4LroWlx-2zI.ttf

# Italic:
https://fonts.gstatic.com/s/instrumentserif/v5/jizHRFtNs2ka5fXjeivQ4LroWlx-6zATiw.ttf
```

Executor download commands:

```bash
# Create res/font directory
mkdir -p app/src/main/res/font

# Download Regular
curl -o app/src/main/res/font/instrument_serif_regular.ttf \
  "https://fonts.gstatic.com/s/instrumentserif/v5/jizBRFtNs2ka5fXjeivQ4LroWlx-2zI.ttf"

# Download Italic
curl -o app/src/main/res/font/instrument_serif_italic.ttf \
  "https://fonts.gstatic.com/s/instrumentserif/v5/jizHRFtNs2ka5fXjeivQ4LroWlx-6zATiw.ttf"
```

**Filename convention:** Android `res/font/` files must be lowercase, snake_case, no spaces, no Unicode. `instrument_serif_regular.ttf` and `instrument_serif_italic.ttf` are correct.

**Note on URL stability:** Google Fonts CDN URLs include a version segment (`v5`). The version increments when Google updates the font outlines. These URLs are correct as of 2026-04-27. If the font is updated before bundling, re-run the curl to `fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&display=swap` and extract the new URLs from the `src:` lines.

---

## 3. Auth Headline Color Fix

### Current behavior (bug)

`authHeadlineAnnotatedString` in `AuthHeadline.kt` produces 3 spans:

| Span | Text | Color |
|------|------|-------|
| 1 | "Start your" | `inkSoft` |
| (literal) | "\n" | (no span) |
| 2 | "first registry" | `ink` |
| 3 | "." | `accent` |

### Design handoff requirement (authoritative)

`design_handoff_android_owner_flow/README.md` line 61:
> `Headline: 'Start your' / 'first registry.' (second line italic, accent colour)`

"accent colour" applies to the **entire second line** — both the words and the period. The wordmark period pattern (single accent glyph) was incorrectly reused for the headline.

### Correct behavior (2 spans only)

| Span | Text | Color |
|------|------|-------|
| 1 | "Start your" | `inkSoft` |
| (literal) | "\n" | (no span) |
| 2 | "first registry." | `accent` |

The period moves from its own span into span 2. The `ink` span disappears entirely.

### Existing test contradiction — must update AuthHeadlineTest.kt

`AuthHeadlineTest.kt` currently asserts:

```kotlin
@Test fun firstRegistryWords_areInkColored() {
    // asserts ink colour on "first registry"
}
```

This test is wrong per the handoff. It must be updated to assert `accent` colour on "first registry.". The `period_isAccentColored()` test remains valid.

**Updated contract (for test + implementation):**

```kotlin
// Test updated assertions:
@Test fun firstRegistryLine_isAccentColored() {
    val result = build()
    val startIdx = result.text.indexOf("first registry.")
    val span = result.spanStyles.firstOrNull { span ->
        startIdx in span.start until span.end
    }
    assertEquals(
        "'first registry.' must be entirely accent-coloured",
        accent, span?.item?.color
    )
}
```

### Factory function change

```kotlin
// BEFORE (current, 3 spans):
fun authHeadlineAnnotatedString(...): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = inkSoft)) { append(prefix) }
    append("\n")
    withStyle(SpanStyle(color = ink)) { append(accent) }
    withStyle(SpanStyle(color = accentColor)) { append(".") }
}

// AFTER (correct, 2 spans):
fun authHeadlineAnnotatedString(...): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = inkSoft)) { append(prefix) }
    append("\n")
    withStyle(SpanStyle(color = accentColor)) {
        append(accent)   // "first registry"
        append(".")      // period stays inside the accent span
    }
}
```

Note: The `ink` parameter is no longer used in the 2-span version. The executor should either remove it from the signature or keep it as a named parameter with a deprecation comment to avoid breaking the existing call-site in `AuthHeadline.kt` (which passes `colors.ink` for `ink`). Simpler path: remove the `ink` parameter from the factory and from the `@Composable` call-site — `colors.ink` is not referenced at all.

---

## 4. License Compliance

Both Instrument Serif (Google, 2022) and Inter (Rasmus Anderby, 2016) are licensed under the **SIL Open Font License 1.1 (OFL-1.1)**. JetBrains Mono is licensed under the **Apache License 2.0**.

**OFL-1.1 bundling requirements:**
- Permitted: bundle in any application, redistribute, modify
- Required: include the font license text when distributing the font files
- NOT required: attribution in the app UI, About screen mention, or runtime display

**Practical steps for compliance:**
1. Add `app/src/main/res/font/OFL.txt` (or `LICENSE_instrument_serif.txt`) alongside the TTF files — one copy of the OFL-1.1 text covers both files of the same family.
2. Add a row to the app's OSS Notices / Open Source Licenses screen (if one exists). Standard Android OSS practice — not legally required by OFL but good hygiene.
3. No visible attribution needed in the running app.

Apache 2.0 (JetBrains Mono) requires similar: include the license text in distribution. If JetBrains Mono is ever bundled, add `LICENSE_jetbrains_mono.txt` alongside.

For Instrument Serif specifically, the OFL-1.1 full text is at:
`https://scripts.sil.org/OFL`

---

## 5. Implementation Checklist for Executor

**Task 1 — Bundle Instrument Serif:**
1. `mkdir -p app/src/main/res/font`
2. Download `instrument_serif_regular.ttf` and `instrument_serif_italic.ttf` via the curl commands above
3. Add `OFL.txt` to the font directory (copy from `https://scripts.sil.org/OFL` or the font's GitHub)
4. Edit `GiftMaisonFonts.kt`: append two `Font(resId = R.font.xxx)` entries to `InstrumentSerifFamily` after the existing GMS entries (fallback position)
5. Verify: build compiles; Android Studio Preview for `StyleGuidePreview.kt` shows Instrument Serif even when emulator has no GMS

**Task 2 — Fix AuthHeadline colors:**
1. Update `authHeadlineAnnotatedString` in `AuthHeadline.kt`: collapse spans 2+3 into a single accent span; remove unused `ink` parameter
2. Update call-site in `AuthHeadline @Composable`: remove `ink = colors.ink` argument
3. Update `AuthHeadlineTest.kt`: change `firstRegistryWords_areInkColored` to `firstRegistryLine_isAccentColored` asserting `accent` on `"first registry."`; remove any assertion that `ink` is used
4. Run tests: `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.auth.AuthHeadlineTest"`

---

## Sources

- `design_handoff_android_owner_flow/README.md` line 61 — authoritative headline spec (HIGH)
- `app/src/main/java/com/giftregistry/ui/auth/AuthHeadline.kt` — current implementation (code read)
- `app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt` — existing test contract (code read)
- `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt` — current font setup (code read)
- `.planning/phases/08-giftmaison-design-foundation/08-RESEARCH.md` — Phase 8 font strategy, alternatives table
- Live curl: `https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&display=swap` — confirmed TTF URLs (HIGH)
- Android Developers "Using fonts in Compose" — FontFamily resolution order (HIGH)
- SIL OFL-1.1 — bundling permissions (HIGH)
