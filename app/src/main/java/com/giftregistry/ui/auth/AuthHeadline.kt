package com.giftregistry.ui.auth

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString

/**
 * SCR-06: Auth screen headline AnnotatedString factory.
 *
 * Wave 0 stub — unblocks AuthHeadlineTest compilation so the 4 Plan 02 Wave 0
 * tests (TabFilter / Draft / IsPrimary / AvatarInitials) can run without the
 * entire test suite compile-failing.
 *
 * STUB: This implementation is intentionally minimal and will cause
 * AuthHeadlineTest assertions to FAIL (RED state preserved). Plan 03 replaces
 * this with the correct 3-colour AnnotatedString implementation:
 *   Line 1 "Start your"     → SpanStyle(color = inkSoft)
 *   Line 2 "first registry" → SpanStyle(color = ink)
 *   Line 2 "."              → SpanStyle(color = accentColor)
 */
fun authHeadlineAnnotatedString(
    prefix: String,
    accent: String,
    ink: Color,
    accentColor: Color,
    inkSoft: Color,
): AnnotatedString = AnnotatedString("$prefix\n$accent.")
