package com.giftregistry.ui.registry.detail

/**
 * SCR-08: Share banner URL builder.
 *
 * Format is locked by CONTEXT.md § Share banner: the Firebase Hosting
 * production target is `gift-registry-ro.web.app` (confirmed by the quick-task
 * 260420-nh8 fix for the email invite URL). The web fallback router's /r/:id
 * param matches this path exactly.
 *
 * NOT URL-encoded — Firestore auto-generated IDs are base-62 and never contain
 * characters that need encoding. URLEncoder.encode() would break the web
 * fallback route match. Unit-tested by ShareUrlTest (Wave 0).
 */
fun shareUrlOf(registryId: String): String =
    "https://gift-registry-ro.web.app/r/$registryId"
