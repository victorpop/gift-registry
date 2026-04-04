---
phase: 1
slug: firebase-foundation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-04
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | @firebase/rules-unit-testing v5 + jest/vitest |
| **Config file** | firestore.rules + firebase.json |
| **Quick run command** | `npm test -- --testPathPattern=rules` |
| **Full suite command** | `npm test` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `npm test -- --testPathPattern=rules`
- **After every plan wave:** Run `npm test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | I18N-02 | file check | `test -f app/src/main/res/values/strings.xml` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | SC-03 | unit | `npm test -- --testPathPattern=rules` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `tests/rules/` — security rules test directory
- [ ] `package.json` — with @firebase/rules-unit-testing dependency
- [ ] Firebase Emulator Suite — configured for local testing

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Firebase project exists with Auth, Firestore, Functions, Hosting, App Check | SC-01 | Requires Firebase Console | Verify at console.firebase.google.com |
| assetlinks.json served from Hosting | SC-04 | Requires deployed hosting | `curl https://[project].web.app/.well-known/assetlinks.json` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
