# Pitfalls Research

**Domain:** Gift registry Android app with Firebase backend, EMAG product catalog, affiliate monetization
**Researched:** 2026-04-04
**Confidence:** MEDIUM-HIGH (core patterns HIGH from official docs; EMAG-specific LOW from limited public documentation)

---

## Critical Pitfalls

### Pitfall 1: Reservation Race Condition — Two Givers Reserve the Same Item Simultaneously

**What goes wrong:**
Two gift givers open the same registry at the same moment, both see an item as "available," and both tap "Reserve." Without an atomic transaction, both writes succeed and the registry owner receives two identical gifts.

**Why it happens:**
Developers check availability in a separate read before writing the reservation — the classic check-then-act pattern. The window between the read and the write is where both concurrent requests slip through. Batched writes don't help here; only Firestore transactions with conditional logic ("write only if current status == available") prevent this.

**How to avoid:**
Use a Firestore transaction that reads the item's `status` field and writes the reservation atomically. The transaction body must be: read current status → abort if not `available` → write `reserved` with `reservedBy`, `reservedAt`, `expiresAt`. Firestore's serializable isolation guarantees this is atomic. Never check availability outside a transaction and then write separately.

**Warning signs:**
- Reservation logic written as two sequential Firestore calls (get, then set) rather than a single `runTransaction`
- Status field updated via `set()` or `update()` without a transaction guard
- Tests that only check single-user reservation, not concurrent reservation attempts

**Phase to address:**
Core reservation system phase — this is the foundational correctness guarantee. Must be correct before any UI is built on top of it.

---

### Pitfall 2: Timer Expiry Not Enforced on the Server — Clients Disagree on State

**What goes wrong:**
The 30-minute countdown is implemented only in the Android app. When the timer runs out client-side, the UI updates to "available," but the Firestore document still holds `status: reserved`. A second giver attempting to reserve finds the item locked indefinitely. The item is effectively lost from the registry.

**Why it happens:**
Client-side timers are unreliable: the app can be backgrounded, killed, or the phone rebooted. When enforcement lives only in the client, server state diverges from what users see.

**How to avoid:**
Expiry is authoritative only on the server. Use Cloud Tasks (not Cloud Scheduler cron) to schedule a cleanup function at the exact `expiresAt` timestamp for each reservation. The Cloud Function checks: if status is still `reserved` and `now > expiresAt`, atomically set status back to `available` and clear reservation fields. The Android client displays a countdown for UX only — it never unilaterally mutates status.

**Warning signs:**
- Timer logic in Android `ViewModel` or `Service` that writes `status: available` back to Firestore
- Using `ScheduledExecutorService` or `Handler.postDelayed` to drive state changes
- No Cloud Function exists to handle expiry
- Cloud Scheduler cron (e.g., every 5 minutes) used instead of per-reservation Cloud Tasks — introduces up to 5 minutes of zombie-reserved items

**Phase to address:**
Core reservation system phase. Server-side expiry must be implemented alongside the reservation write — they are one feature, not two.

---

### Pitfall 3: Item Status Stored Only in One Place, Causing Stale State After Expiry Email

**What goes wrong:**
The expiry email tells the giver their reservation lapsed and offers a "re-reserve" link. The giver clicks the link 3 hours later. If the item has been re-reserved by someone else in the meantime, the link still shows the item as available because the app re-reads the item optimistically. The giver re-reserves, then discovers someone else bought it. This destroys trust in the "no duplicates" core value.

**Why it happens:**
The re-reserve flow skips the same transactional check used in the initial reservation, treating the expiry email's deep link as pre-authorization.

**How to avoid:**
The re-reserve flow must go through exactly the same Firestore transaction as any other reservation. No shortcuts: even deep links from email must check current status atomically at reservation time. Display a "this item is no longer available" error if the transaction fails.

**Warning signs:**
- A separate code path for "re-reserve from email" that skips the normal reservation transaction
- Deep link handler that reads item status from a cache or intent extra rather than fetching fresh from Firestore

**Phase to address:**
Notification and email flow phase — test the full reservation → expiry → re-reserve cycle as an explicit scenario.

---

### Pitfall 4: Firestore Hot Document — Single Item Document Becomes a Write Bottleneck

**What goes wrong:**
A popular registry item (e.g., a high-demand wedding gift) receives many concurrent reservation attempts. All writes target the same Firestore document. At sustained rates above ~1 write/second per document, Firestore begins throttling and transaction contention errors spike. Reservation attempts silently fail or time out.

**Why it happens:**
Firestore distributes data across nodes by document path. A single document is always on a single node. The soft limit is ~1 sustained write/second per document; short bursts can be higher but sustained traffic hits the ceiling.

**How to avoid:**
For items with `quantity > 1`, do not store a single `status` field. Instead, store a subcollection of reservation slots (one document per slot). Each concurrent reserver targets a different slot document, distributing write load across multiple Firestore nodes. For `quantity = 1` items (the majority), the single-document transaction pattern is fine.

**Warning signs:**
- Data model stores all reservation state in one document regardless of item quantity
- Firestore error logs showing `ABORTED` or `RESOURCE_EXHAUSTED` during load tests

**Phase to address:**
Core data model design phase — the document structure for multi-quantity items must be decided before the reservation logic is implemented, not refactored after.

---

### Pitfall 5: Affiliate Link Injection Fails Silently — Revenue Drops to Zero for Unrecognized Merchants

**What goes wrong:**
The URL transformer only handles merchants it recognizes (EMAG, and perhaps a handful of others). When an owner pastes a URL from an unrecognized Romanian retailer, the app stores and uses the raw URL without an affiliate tag. The purchase happens, no commission is earned, but the transaction looks successful. No alert is raised.

**Why it happens:**
The URL transformer is written as a list of known merchant patterns. The fallback for unknown merchants is to pass the URL through unchanged, which is treated as "working correctly" from the app's perspective.

**How to avoid:**
Make affiliate tag injection observable. Log every URL transformation attempt with outcome (matched / not matched). Track the percentage of purchases that result in untagged URLs. Alert when the no-match rate exceeds a threshold. For unrecognized merchants, consider redirecting through a central affiliate platform (e.g., Awin, Impact) that may cover a broader Romanian merchant set.

**Warning signs:**
- URL transformer has no logging or metrics
- No test coverage for "unknown merchant" URLs
- Revenue dashboard shows purchase clicks but zero commissions for a merchant

**Phase to address:**
Affiliate monetization phase — build logging alongside the transformer itself, not as a later instrumentation task.

---

### Pitfall 6: EMAG API Dependency Without a Fallback — Catalog Browsing Breaks Silently

**What goes wrong:**
EMAG changes its API (endpoint URLs, authentication scheme, response schema, rate limits, or terms of service). The in-app catalog browse screen returns empty results or throws exceptions. Since this is a third-party public API not under the project's control, breaking changes can arrive without notice.

**Why it happens:**
The EMAG API is treated as a guaranteed dependency rather than a fragile external service. No defensive coding, no version pinning, no fallback UI.

**How to avoid:**
Build the catalog feature so that EMAG API failure degrades gracefully — the owner can still add items by pasting any URL manually. The catalog is a convenience, not the only addition path. Wrap all EMAG API calls in error boundaries with explicit fallback UI. Cache the last successful product search results for a short period (5–15 minutes) to survive transient failures. Monitor API response codes in production.

**Warning signs:**
- EMAG catalog and manual URL entry are coupled (if catalog fails, manual add is blocked)
- No error state UI for the catalog screen
- No defensive parsing for unexpected EMAG response schema

**Phase to address:**
EMAG catalog integration phase — design the degraded state (manual URL only) before implementing the catalog feature.

---

### Pitfall 7: Guest State Lost After App Kill — Guest Giver Cannot Resume Reservation

**What goes wrong:**
A guest (no account) provides first name, last name, and email, then reserves an item. Before completing the purchase at the retailer, they switch apps or Android kills the gift registry process. When they return, the app treats them as a new anonymous session. The 30-minute reservation is ticking but the guest cannot see it. They may try to re-reserve the same item, which fails because it's already reserved (by their own previous session).

**Why it happens:**
Guest identity is stored in-memory or in a `ViewModel` that doesn't survive process death. The guest session is not persisted locally and not tied to a Firestore document they can re-authenticate to.

**How to avoid:**
Persist guest identity (name + email) to `SharedPreferences` or `EncryptedSharedPreferences` on first collection. On app resume, reconstruct the guest session from local storage and query Firestore for active reservations tied to that email. Show an in-progress reservation countdown if one exists. Clear the stored guest identity only after the reservation expires or the guest converts to an account.

**Warning signs:**
- Guest data stored only in `ViewModel` with no persistence layer
- No "you have an active reservation" re-entry flow
- Reservation recovery tested only for logged-in users

**Phase to address:**
Guest access and reservation flow phase — test process-death and resume as a first-class test scenario during development.

---

### Pitfall 8: Private Registry Invite System Exposes Registry to Unauthenticated Web Fallback

**What goes wrong:**
Private registries require an invite. The Android app enforces this correctly. However, the web fallback for gift givers (a separate web deployment) has a looser implementation — it checks the invite status client-side (in JavaScript) rather than in Firestore security rules. A determined gift giver can access any private registry by navigating directly to the URL.

**Why it happens:**
The web fallback is built as a secondary feature, often by a different developer or at a different time. Security rules are tested against the Android client, not the web client. Client-side visibility checks are mistaken for access control.

**How to avoid:**
Firestore security rules are the only authoritative access control layer — they apply equally to all clients (Android SDK, web SDK, REST API). Implement the private/public/invite check entirely in security rules: `allow read: if resource.data.visibility == 'public' || request.auth.uid in resource.data.invitedUsers`. Do not rely on any client to enforce visibility.

**Warning signs:**
- Security rules for registries do not reference the `visibility` field or an invite list
- Web fallback has JavaScript code that checks registry visibility before rendering
- Web fallback security not tested independently from Android

**Phase to address:**
Registry creation and visibility phase — write and test security rules before building any client-side visibility logic.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Client-side reservation expiry only | Simpler, no Cloud Functions needed | Zombie reservations, state divergence; requires full backend rework | Never |
| Hardcode affiliate tag logic in Android | Faster to build | Cannot update merchant list without a Play Store release | Never — always deploy via Cloud Function or remote config |
| Store guest identity in `ViewModel` only | Less code | Guest loses reservation on process death | Never for reservation-bearing sessions |
| Skip Firestore transactions for reservation writes | Simpler write code | Race condition → duplicate gifts → core value failure | Never |
| Build web fallback as a copy-paste of Android logic | Faster initial build | Two diverging codebases; security rule gaps | Only if web fallback is truly read-only with no writes |
| Use Cloud Scheduler cron for expiry cleanup | One function, no per-reservation scheduling | Up to cron-interval lag (5+ minutes) where items appear reserved | Acceptable only if lag is surfaced to users in UI |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| EMAG API | Assuming the public API is stable and versioned | Treat every response field as potentially absent; use defensive parsing; version-check responses where schema is documented |
| EMAG API | Calling EMAG catalog API directly from Android with the user's auth context | All EMAG API calls should go through Cloud Functions to hide API keys and to centralize rate limit handling |
| Firebase Cloud Tasks | Using `setTimeout` inside a Cloud Function to delay expiry cleanup | Cloud Functions have a max runtime; schedule expiry cleanup as a separate Cloud Task created at reservation time |
| Firebase Auth (guest) | Equating Firebase Anonymous Auth with "guest" in product requirements | Anonymous Auth creates a Firebase UID; a true guest (no UID) needs a different identity strategy (email-keyed document in Firestore) |
| Affiliate URLs + Android deep links | Passing affiliate URLs through `Intent` deep links without encoding | URL parameters in deep links can be mis-parsed by Android's Intent system if not properly encoded |
| Firebase Security Rules | Writing rules without testing them with the Firebase Rules Simulator | Always test rules with the simulator using both authenticated and unauthenticated request contexts |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Fetching full registry item list on every screen entry | Slow load times, excessive Firestore reads | Use Firestore real-time listeners with local cache; only fetch on first load, then listen for diffs | At ~50+ items per registry |
| Single Firestore listener for entire registry feed | No incremental updates; rerenders entire list on any item change | Listen at the item level; use `RecyclerView` with diff callbacks | At ~20+ concurrent viewers of a popular registry |
| Resolving affiliate URL at reservation time (blocking) | Reservation tap feels slow; race condition window widens | Pre-transform URLs at item-add time, store the transformed URL; at reservation, use pre-computed URL | Immediately, if URL transformation involves network I/O |
| EMAG product image loading without caching | Slow catalog scroll, excessive network usage, high Firebase egress costs | Use Glide or Coil with disk caching; set explicit image size constraints | At ~30+ product images in a single catalog page |
| Cloud Functions cold start on reservation | First reservation after idle period takes 3–5 seconds | Use minimum instance count = 1 for the reservation Cloud Function; cold starts are unacceptable for a 30-second time-sensitive flow | Every cold start |

---

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Storing affiliate tag logic or API keys in Android APK | Key extraction from APK gives competitor or bad actor access to your affiliate account and EMAG API credentials | All secrets live in Cloud Functions environment config; Android client never holds keys |
| Firestore rules that allow any authenticated user to read any registry | Private registries are exposed to any app user | Rules must check `visibility` field and `invitedUsers` list before granting read access |
| Trusting client-supplied `userId` in reservation documents | Malicious client can reserve items as another user | Derive identity from `request.auth.uid` in security rules, never from client-supplied fields |
| Not validating `expiresAt` field in security rules | Client could submit a reservation with `expiresAt` set to year 2099, permanently locking an item | Cloud Function creates the reservation (including `expiresAt`); clients cannot write reservations directly — only Cloud Functions can |
| Exposing guest email addresses in Firestore documents readable by other users | GDPR / privacy risk; one user can harvest other users' emails | Guest email stored in a private subcollection or Cloud Function-only document; never in the public-readable registry item document |

---

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Showing the reservation timer only after purchase redirect | Giver doesn't know they have 30 minutes; returns hours later to find reservation expired | Show countdown prominently before and during the retailer redirect; send a reminder notification at 25 minutes |
| Forcing account creation before gift givers can see a registry | High abandonment; guests are sent by the registry owner via link and expect immediate access | Allow unauthenticated browsing of public registries; require only name + email at reservation time |
| Not distinguishing "reserved by me" from "reserved by someone else" | A giver who reserved an item sees it as "reserved" and thinks someone else took it; tries to find a different gift | Reserved items show "reserved by you" state to the original reserver (match by email/UID) |
| Ambiguous item state during reservation window | A second giver sees the item as "reserved" with no ETA on when it becomes available again | Show "reserved — available again in X minutes" to other givers during the 30-minute window |
| Language switcher buried in settings | Romanian users who land on English (or vice versa) abandon before finding the toggle | Auto-detect device locale on first launch; prompt "Continue in Romanian?" if device locale doesn't match app default |

---

## "Looks Done But Isn't" Checklist

- [ ] **Reservation system:** Works for single user — verify with two concurrent sessions attempting to reserve the same item within the same second
- [ ] **Timer expiry:** Countdown reaches zero in UI — verify the Firestore document is actually updated to `available` by the server-side Cloud Function, not just the client
- [ ] **Affiliate URLs:** Links open correctly in browser — verify affiliate tag is present in the URL and the affiliate account registers the click
- [ ] **Private registry invite:** Only invited users see the registry in the app — verify that a non-invited authenticated user hitting the Firestore document directly via REST API is also blocked by security rules
- [ ] **Guest flow:** Guest provides details and reserves — verify that process death mid-reservation (force-stop the app) followed by re-open shows the active reservation rather than a clean state
- [ ] **Web fallback:** Renders registry correctly — verify that Firestore security rules block private registry access from the web client identically to the Android client
- [ ] **Multilingual:** All strings display in Romanian — verify that no strings are hardcoded in layouts or Kotlin/Java code (use Android lint's `HardcodedText` check)
- [ ] **EMAG catalog:** Products load — verify the screen degrades gracefully with a useful error message when the EMAG API returns a 5xx or empty response

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Race condition discovered post-launch (duplicate gifts received) | HIGH | Refactor reservation writes to use Firestore transactions; audit all existing reservations; communicate transparently to affected users; compensate duplicates |
| Client-side timer expiry in production | HIGH | Deploy server-side Cloud Task expiry; run a one-time cleanup function to release all currently expired reservations; no client update required if cleanup is server-only |
| Affiliate tag not injecting for a merchant | LOW | Update the URL transformer Cloud Function (no app release needed); re-transform and re-store existing items for that merchant |
| EMAG API breaking change | MEDIUM | Disable catalog browse feature via remote config; owners fall back to manual URL entry; fix EMAG integration in Cloud Function without requiring app update |
| Security rule gap exposing private registries | HIGH | Fix rules immediately (no app release required); audit Firestore logs for unauthorized reads; notify affected registry owners |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Reservation race condition | Core reservation system | Concurrent reservation integration test: two clients, one item, one succeeds |
| Server-side timer expiry | Core reservation system | Force-expire a reservation via Firestore write; confirm Cloud Task fires and sets status to `available` |
| Re-reserve path skips transaction | Notification + email flow phase | Send expiry email, click re-reserve link, simultaneously have another client reserve — only one should succeed |
| Firestore hot document (multi-quantity) | Data model design phase (before reservation) | Load test with 10 concurrent reservation attempts on same item; verify no ABORTED errors |
| Affiliate link silent failure | Affiliate monetization phase | Submit an unrecognized URL; verify a log entry is created and no untagged URL reaches the registry |
| EMAG API dependency without fallback | EMAG catalog phase | Simulate EMAG API 500 error; verify manual URL add still works and catalog shows a clear error state |
| Guest state lost on process death | Guest access + reservation phase | Android developer options → "Don't keep activities"; reserve as guest; return to app; verify reservation shown |
| Private registry web fallback bypass | Registry visibility + security rules phase | Attempt to read a private Firestore registry document via REST API without auth token; verify 403 |

---

## Sources

- [Transactions and batched writes — Firebase official docs](https://firebase.google.com/docs/firestore/manage-data/transactions)
- [Transaction serializability and isolation — Firebase official docs](https://firebase.google.com/docs/firestore/transaction-data-contention)
- [Understand reads and writes at scale — Firebase official docs](https://firebase.google.com/docs/firestore/understand-reads-writes-scale)
- [Best practices for Cloud Firestore — Firebase official docs](https://firebase.google.com/docs/firestore/best-practices)
- [Schedule functions — Cloud Functions for Firebase](https://firebase.google.com/docs/functions/schedule-functions)
- [Avoid Race Conditions with Reservation Pattern — Medium (Mohammad Ghanbari)](https://medium.com/@mmdGhanbari/avoid-race-conditions-with-reservation-pattern-bc4846602417)
- [Race Conditions in Firestore: How to Solve it — QuintoAndar Tech Blog](https://medium.com/quintoandar-tech-blog/race-conditions-in-firestore-how-to-solve-it-5d6ff9e69ba7)
- [How to Handle Firestore 10-Write-Per-Second Document Limit — OneUptime Blog](https://oneuptime.com/blog/post/2026-02-17-how-to-handle-firestore-10-write-per-second-document-limit/view)
- [Firebase Security Rules — 7 tips](https://firebase.blog/posts/2019/03/firebase-security-rules-admin-sdk-tips/)
- [Basic Security Rules — Firebase official docs](https://firebase.google.com/docs/rules/basics)
- [Checkout UX Best Practices — Baymard Institute](https://baymard.com/blog/current-state-of-checkout-ux)
- [Support different languages — Android Developers](https://developer.android.com/training/basics/supporting-devices/languages)
- [Enabling Offline Capabilities — Firebase Realtime Database](https://firebase.google.com/docs/database/android/offline-capabilities)

---
*Pitfalls research for: Gift registry Android app (Java/Kotlin) with Firebase, EMAG catalog, affiliate monetization*
*Researched: 2026-04-04*
