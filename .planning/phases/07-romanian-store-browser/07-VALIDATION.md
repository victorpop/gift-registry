---
phase: 7
slug: romanian-store-browser
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-19
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + MockK (Android unit tests) — existing infrastructure from Phases 2–6 |
| **Config file** | `app/build.gradle.kts` test deps already configured |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*{ModifiedTestClass}*"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` + `cd tests/rules && npm test` |
| **Estimated runtime** | ~60s (Android) + ~2s (rules) |

---

## Sampling Rate

- **After every task commit:** Run the targeted test class
- **After every plan wave:** Run full `./gradlew :app:testDebugUnitTest` for the plan's scope
- **Before `/gsd:verify-work`:** Full suite green across Android + rules
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD — populated by planner | | | STORE-01..04 | | | | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Extend `tests/rules/firestore.rules.test.ts` with `config/stores` read/write rules (allow read:true, allow write:false for all clients)
- [ ] Seed data file `functions/data/stores.seed.json` with the 8 curated stores
- [ ] Seed script `functions/scripts/seedStores.ts` — idempotent (`.set` not `.add`) — writes to `config/stores`
- [ ] ProGuard keep rule for drawable resource stripping in release builds (`-keep class **.R$drawable { *; }`) — **verify** whether `app/proguard-rules.pro` exists and whether R8 is enabled; add if missing

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| WebView renders store homepages correctly on real device/emulator | STORE-02 | WebView rendering is framework-provided; unit tests verify ViewModel state flow, not rendering | Install app, tap a store, verify the store homepage renders with expected content, JS-driven features work (search bar, product cards), cookies persist across visits |
| Persistent bottom "Add to list" CTA remains accessible while scrolling a long product page | STORE-03 | Requires scrolling a real retailer page to verify the CTA is always tappable | Open any store, scroll deep into a product listing, tap "Add to list" — verify the add-item sheet opens with the current URL pre-filled |
| Add-to-list sheet funnels through the existing Phase 3 affiliate + OG fetch pipeline | STORE-03 | Integration spans UI → sheet → Cloud Function → Firestore write; end-to-end across multiple modules | Open eMAG WebView, navigate to a product page, tap "Add to list", confirm — verify item appears in the registry with affiliate tag (Firestore console) and OG metadata auto-filled |
| FAB menu opens smoothly on Home screen; both "Create registry" and "Browse stores" actions reachable | STORE-01 | Extended FAB menu animation cannot be unit tested | Tap Home FAB, verify menu expands with both actions; tap each; verify no regression in "Create registry" flow |
| Store list error state (empty Firestore doc) shows retry UI | STORE-04 | Requires manually corrupting Firestore state | In Firestore emulator, delete the `config/stores` doc, open the Store List screen — verify error state with Retry button; seed the doc and tap Retry; verify list loads |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter (planner flips after populating per-task map)

**Approval:** pending
