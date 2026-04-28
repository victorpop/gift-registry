# GSD Debug Knowledge Base

Resolved debug sessions. Used by `gsd-debugger` to surface known-pattern hypotheses at the start of new investigations.

---

## item-image-fetch-broken — og:image served as http:// blocked by Android cleartext policy
- **Date:** 2026-04-28
- **Error patterns:** image broken, placeholder, http, cleartext, Coil, og:image, imageUrl, AsyncImage, error painter, fetch, mobexpert
- **Root cause:** Cloud Function returns og:image verbatim (no scheme normalization). Retailer og:image uses http://. Android targetSdk=36 blocks cleartext HTTP by default — no usesCleartextTraffic, no network_security_config for external domains. Coil silently falls back to the error painter.
- **Fix:** Added normalizeImageUrl helper in fetchOgMetadata.ts rewriting http:// → https:// before returning imageUrl. Also applied the same normalization in ItemRepositoryImpl.fetchOgMetadata (client-side defense-in-depth).
- **Files changed:** functions/src/registry/fetchOgMetadata.ts, app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt
---
