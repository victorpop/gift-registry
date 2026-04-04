# Phase 1: Firebase Foundation - Research

**Researched:** 2026-04-04
**Domain:** Firebase project setup, Firestore schema design, Firestore Security Rules, Firebase Emulator Suite, Android string resources, i18n file structure
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-06:** Single Firebase project (no dev/staging/prod split)
- **D-07:** Firebase Emulator Suite enabled for local development (Firestore, Auth, Functions)
- **D-08:** Firebase project created from scratch — Phase 1 includes full setup instructions
- **D-09:** Use default Firebase Hosting URL ([project].web.app) — custom domain deferred

### Claude's Discretion
- **D-01:** Collection structure (flat vs subcollections for registries/items) — choose based on access patterns: registry owner CRUD, giver browsing items, real-time reservation status updates, cross-registry queries not needed for v1
- **D-02:** Reservation storage (fields on item doc vs separate collection) — optimize for the 30-min timer, concurrent access via Firestore transactions, and Cloud Tasks expiry pattern
- **D-03:** Guest user documents vs inline-only — optimize for guest-to-account conversion flow (AUTH-06) while avoiding orphan documents
- **D-04:** Private registry access enforcement (invited list on doc vs invites subcollection) — choose the approach that works within Firestore rules limitations and supports the invite flow (REG-05 through REG-08)
- **D-05:** Public registry read access — balance openness for gift givers with protection against scraping, using App Check where appropriate
- **D-10:** Convention and naming pattern only — define the i18n key structure with a few example keys, each subsequent phase adds its own strings
- **D-11:** Key organization approach (by screen/feature vs by type) — pick what scales well for a 7-phase project with Android + web fallback

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| I18N-02 | All UI labels stored in separate resource files (strings.xml for Android, i18n files for web) | Android `res/values/strings.xml` + `res/values-ro/strings.xml` pattern confirmed; i18next `en.json`/`ro.json` structure defined; key naming convention established in this phase |
</phase_requirements>

---

## Summary

Phase 1 is a pure infrastructure and schema phase — no feature code. It delivers: a configured Firebase project (Auth, Firestore, Cloud Functions, Hosting, App Check), a final and stable Firestore schema (collections, subcollections, all field names and types), Firestore Security Rules tested against the four required access patterns, a placeholder `assetlinks.json` served from Firebase Hosting, and the i18n resource file structure for all future phases.

The existing project research files (`.planning/research/`) are comprehensive and accurate. This phase-level research adds specificity on: the exact Firestore rules `rules_version = '2'` declaration, the `@firebase/rules-unit-testing` v5 testing approach, the role-based map pattern for private registry invite enforcement in rules (which avoids the subcollection limitation), the firebase.json hosting configuration for `/.well-known/assetlinks.json`, the current firebase-functions v7.2.3 and firebase-admin v13.7.0 package versions, and a concrete i18n key naming convention recommendation.

The most important decision to make well in this phase is the Firestore schema — specifically the reservation data model (D-02) and the private registry access pattern in security rules (D-04). Both are resolved below with recommended approaches based on research findings.

**Primary recommendation:** Use the schema from `.planning/research/ARCHITECTURE.md` with one refinement: store invited user UIDs (not emails) in a map on the registry document for security rules compatibility. Use `rules_version = '2'`, `@firebase/rules-unit-testing` v5 with the Firebase Emulator Suite for automated rules tests, and a feature-namespaced i18n key convention (`screen_component_description`).

---

## Standard Stack

### Core (Phase 1 specific)

| Tool / Library | Version | Purpose | Why Standard |
|----------------|---------|---------|--------------|
| Firebase CLI | latest (install via npm) | Project init, deploy, emulator control | Required for `firebase init`, Functions 2nd gen, Hosting deploy |
| firebase-functions | 7.2.3 | Cloud Functions SDK (Node.js) | Current stable; verified via npm registry 2026-04-04 |
| firebase-admin | 13.7.0 | Admin SDK for Functions | Current stable; verified via npm registry 2026-04-04 |
| @firebase/rules-unit-testing | 5.0.0 | Automated Firestore Security Rules tests | Current stable; integrates with Emulator Suite; verified via npm 2026-04-04 |
| firebase JS SDK | 12.11.0 | Used in rules test environment | Current stable; verified via npm 2026-04-04 |
| Node.js | 22 (LTS) | Functions runtime | Firebase-recommended LTS; fully supported by firebase-functions 7.x |
| TypeScript | 5.x | Functions language | Type safety on Firestore document shapes; `firebase init functions` scaffolds this |

### Firebase Services to Enable

| Service | Purpose | Notes |
|---------|---------|-------|
| Firebase Authentication | User identity (email/password, anonymous/guest) | Enable in console before Phase 2; referenced in security rules from Phase 1 |
| Cloud Firestore | Primary datastore | Native mode (not Datastore mode); region: `europe-west3` (Frankfurt) for Romanian market latency |
| Cloud Functions (2nd gen) | Server-side logic, reservation expiry | 2nd gen required; 1st gen is deprecated |
| Firebase Hosting | Web fallback SPA + `assetlinks.json` | Needed in Phase 1 for the `assetlinks.json` placeholder |
| Firebase App Check | API abuse prevention (Play Integrity) | Enable in console; security rules reference `request.app` from Phase 1 |

### Development Tools

| Tool | Version | Purpose |
|------|---------|---------|
| Firebase Emulator Suite | bundled with CLI | Local Firestore, Auth, Functions, Hosting emulation |
| Java JDK | 11+ | Required by Firebase Emulator Suite |

**Installation:**

```bash
# Install Firebase CLI globally
npm install -g firebase-tools

# Verify
firebase --version   # should be 13+

# Login
firebase login

# Initialize project (run in repo root)
firebase init
# Select: Firestore, Functions, Hosting, Emulators
# Functions: TypeScript, Node 22, install deps now
```

**Version verification (run to confirm current):**

```bash
npm view firebase-functions version   # 7.2.3 as of 2026-04-04
npm view firebase-admin version       # 13.7.0 as of 2026-04-04
npm view @firebase/rules-unit-testing version  # 5.0.0 as of 2026-04-04
```

---

## Architecture Patterns

### Recommended Firestore Schema

The schema below incorporates all decisions from `.planning/research/ARCHITECTURE.md` plus the resolution of D-01 through D-04.

**Resolution of D-01 (flat vs subcollections):** Use subcollections for items and invites under each registry. This matches the access pattern: registry owner CRUD is always scoped to one registry, giver browsing is always scoped to one registry, and cross-registry queries are explicitly out of scope for v1. Subcollections enable security rules that cascade from the parent registry document.

**Resolution of D-02 (reservation storage):** Use a top-level `reservations` collection (not a subcollection of items). This allows Cloud Functions to query all active reservations for a given item or user without knowing which registry/item they belong to. The `cloudTaskName` field enables task cancellation when a purchase is confirmed.

**Resolution of D-03 (guest user documents):** Do not create Firestore `users/` documents for guests. Guests are identified by email, stored only in the `reservations` document (written by Cloud Functions). Guest-to-account conversion in AUTH-06 links the anonymous Firebase Auth UID to email credentials — no Firestore document migration required.

**Resolution of D-04 (private registry access):** Store invited user UIDs in a map on the registry document (not in a subcollection). This is required because Firestore security rules cannot perform reads inside `allow` conditions without a security rules `get()` call, which counts against read limits and complicates rules. A map field on the document itself is directly readable in rules via `resource.data.invitedUsers[request.auth.uid]`.

```
registries/
  {registryId}/
    ownerId: string                 # Firebase Auth UID
    title: string
    occasion: string                # "wedding" | "birthday" | "baby" | "anniversary" | "christmas" | "custom"
    customOccasionLabel: string?    # present only when occasion == "custom"
    eventDate: timestamp
    eventLocation: string
    description: string
    visibility: "public" | "private"
    invitedUsers: map<uid, true>    # only present when visibility == "private"; map for O(1) rules lookup
    locale: "ro" | "en"             # registry default locale for sharing
    notificationsEnabled: boolean   # owner opt-in for purchase notifications
    createdAt: timestamp
    updatedAt: timestamp

    items/                          # subcollection
      {itemId}/
        title: string
        imageUrl: string
        originalUrl: string
        affiliateUrl: string
        merchant: string            # "emag" | "unknown"
        price: number?              # optional; stored if available from Open Graph
        currency: string?           # "RON" | "EUR" etc
        notes: string?
        status: "available" | "reserved" | "purchased"
        reservedAt: timestamp?
        reservedBy: string?         # giver email (display only; authoritative state in reservations/)
        addedAt: timestamp

    invites/                        # subcollection — tracks invite status per email for REG-06/07/08
      {inviteId}/
        email: string
        invitedUid: string?         # populated if email matches a Firebase Auth user at invite time
        status: "pending" | "accepted"
        sentAt: timestamp

reservations/
  {reservationId}/
    registryId: string
    itemId: string
    giverEmail: string
    giverFirstName: string
    giverLastName: string
    giverUserId: string?            # null for guests; populated for registered users
    status: "active" | "expired" | "cancelled" | "purchased"
    createdAt: timestamp
    expiresAt: timestamp            # createdAt + 30 minutes
    cloudTaskName: string           # Cloud Tasks task name for cancellation

users/
  {userId}/
    email: string
    displayName: string
    locale: "ro" | "en"
    notificationsEnabled: boolean
    fcmToken: string?
    createdAt: timestamp
```

### Recommended Project Structure (Phase 1 deliverables only)

```
gift-registry/
├── firebase.json                   # Hosting, Firestore, Functions, Emulators config
├── .firebaserc                     # Project alias
├── firestore.rules                 # Security rules (rules_version = '2')
├── firestore.indexes.json          # Composite index definitions
├── functions/
│   ├── src/
│   │   └── index.ts                # Empty entry point (populated in Phase 3+)
│   ├── package.json                # firebase-functions 7.2.3, firebase-admin 13.7.0
│   └── tsconfig.json
├── hosting/
│   └── public/
│       └── .well-known/
│           └── assetlinks.json     # Placeholder for Android App Links (Phase 5)
├── tests/
│   └── rules/
│       ├── firestore.rules.test.ts  # @firebase/rules-unit-testing v5 tests
│       └── package.json
└── app/
    └── src/main/res/
        ├── values/
        │   └── strings.xml         # English strings (I18N-02)
        └── values-ro/
            └── strings.xml         # Romanian strings (I18N-02)
```

### Pattern 1: Firestore Security Rules (rules_version = '2')

**What:** Rules must be declared with `rules_version = '2'` to use the wildcard `**` recursive match syntax and benefit from the latest evaluation engine. All access control for public/private registry visibility, owner-only writes, and guest reservation writes must live entirely in rules — never in client code.

**When to use:** This is the only rules pattern for this project. No exceptions.

```javascript
// Source: https://firebase.google.com/docs/rules/rules-language
// Source: https://firebase.google.com/docs/firestore/solutions/role-based-access
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isSignedIn() {
      return request.auth != null;
    }

    function isOwner(registryData) {
      return isSignedIn() && request.auth.uid == registryData.ownerId;
    }

    function isPublicRegistry(registryData) {
      return registryData.visibility == 'public';
    }

    function isInvited(registryData) {
      return isSignedIn() &&
             registryData.visibility == 'private' &&
             registryData.invitedUsers[request.auth.uid] == true;
    }

    function canReadRegistry(registryData) {
      return isPublicRegistry(registryData) ||
             isOwner(registryData) ||
             isInvited(registryData);
    }

    // Registries collection
    match /registries/{registryId} {
      allow read: if canReadRegistry(resource.data);
      allow create: if isSignedIn() && request.resource.data.ownerId == request.auth.uid;
      allow update, delete: if isOwner(resource.data);

      // Items subcollection — inherits registry visibility for reads
      match /items/{itemId} {
        allow read: if canReadRegistry(get(/databases/$(database)/documents/registries/$(registryId)).data);
        // Items written only by owner (client) or Cloud Functions (bypass rules via Admin SDK)
        allow create, update, delete: if isOwner(
          get(/databases/$(database)/documents/registries/$(registryId)).data
        );
      }

      // Invites subcollection — owner-only read/write
      match /invites/{inviteId} {
        allow read, write: if isOwner(
          get(/databases/$(database)/documents/registries/$(registryId)).data
        );
      }
    }

    // Reservations — Cloud Functions write only (Admin SDK bypasses rules)
    // Clients must never write reservations directly
    match /reservations/{reservationId} {
      // No client read/write allowed — all access via Cloud Functions
      allow read, write: if false;
    }

    // Users — owner reads/writes their own document only
    match /users/{userId} {
      allow read, update: if isSignedIn() && request.auth.uid == userId;
      allow create: if isSignedIn() && request.auth.uid == userId;
      allow delete: if false; // accounts not deletable in v1
    }
  }
}
```

**Important:** The `items` and `invites` subcollections use `get()` calls to fetch the parent registry document. This counts as an additional Firestore read per rules evaluation. For Phase 1, this is acceptable. At scale (Phase 4+), consider denormalizing `ownerId` and `visibility` into item documents to avoid the `get()` call.

### Pattern 2: Firestore Security Rules Tests with @firebase/rules-unit-testing v5

**What:** All four required security rules scenarios must have automated tests that run against the Firebase Emulator Suite. Tests use `assertSucceeds` and `assertFails` from `@firebase/rules-unit-testing`.

**When to use:** Write tests before writing rules. Rules are done when all four test scenarios pass.

```typescript
// Source: https://firebase.google.com/docs/rules/unit-tests
// tests/rules/firestore.rules.test.ts
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc } from "firebase/firestore";
import * as fs from "fs";

let testEnv: RulesTestEnvironment;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "gift-registry-test",
    firestore: {
      rules: fs.readFileSync("../../firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
  });
});

afterAll(async () => {
  await testEnv.cleanup();
});

describe("Public registry read", () => {
  it("allows unauthenticated read of a public registry", async () => {
    // Seed test data bypassing rules
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "registries/pub1"), {
        ownerId: "owner1",
        visibility: "public",
        title: "Test Registry",
        invitedUsers: {},
      });
    });

    const unauthedDb = testEnv.unauthenticatedContext().firestore();
    await assertSucceeds(getDoc(doc(unauthedDb, "registries/pub1")));
  });
});

describe("Private registry owner-only read", () => {
  it("denies non-owner read of a private registry", async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "registries/priv1"), {
        ownerId: "owner2",
        visibility: "private",
        title: "Private Registry",
        invitedUsers: {},
      });
    });

    const otherUserDb = testEnv.authenticatedContext("other-user").firestore();
    await assertFails(getDoc(doc(otherUserDb, "registries/priv1")));
  });

  it("allows owner to read their own private registry", async () => {
    const ownerDb = testEnv.authenticatedContext("owner2").firestore();
    await assertSucceeds(getDoc(doc(ownerDb, "registries/priv1")));
  });
});

describe("Guest reservation write", () => {
  it("denies direct client write to reservations collection", async () => {
    const guestDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(
      setDoc(doc(guestDb, "reservations/res1"), {
        giverEmail: "guest@example.com",
        status: "active",
      })
    );
  });
});

describe("Owner-only item write", () => {
  it("denies non-owner write to items subcollection", async () => {
    const otherUserDb = testEnv.authenticatedContext("other-user").firestore();
    await assertFails(
      setDoc(doc(otherUserDb, "registries/pub1/items/item1"), {
        title: "Unauthorized item",
      })
    );
  });

  it("allows owner to write items to their registry", async () => {
    const ownerDb = testEnv.authenticatedContext("owner1").firestore();
    await assertSucceeds(
      setDoc(doc(ownerDb, "registries/pub1/items/item1"), {
        title: "Authorized item",
        status: "available",
        originalUrl: "https://emag.ro/product/123",
        affiliateUrl: "https://emag.ro/product/123",
        merchant: "emag",
        addedAt: new Date(),
      })
    );
  });
});
```

### Pattern 3: firebase.json — Hosting Configuration for assetlinks.json

**What:** Firebase Hosting must serve the Android App Links verification file at `/.well-known/assetlinks.json`. The file lives in `hosting/public/.well-known/assetlinks.json` and is served directly (no rewrite needed if the directory structure matches).

**When to use:** Phase 1 delivers a placeholder file; Phase 5 populates it with the real SHA-256 fingerprint.

```json
// firebase.json
{
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  },
  "functions": [
    {
      "source": "functions",
      "codebase": "default",
      "ignore": ["node_modules", ".git", "firebase-debug.log"]
    }
  ],
  "hosting": {
    "public": "hosting/public",
    "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
    "headers": [
      {
        "source": "/.well-known/assetlinks.json",
        "headers": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ]
      }
    ],
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ]
  },
  "emulators": {
    "auth": {
      "port": 9099
    },
    "functions": {
      "port": 5001
    },
    "firestore": {
      "port": 8080
    },
    "hosting": {
      "port": 5000
    },
    "ui": {
      "enabled": true,
      "port": 4000
    },
    "singleProjectMode": true
  }
}
```

**assetlinks.json placeholder:**

```json
// hosting/public/.well-known/assetlinks.json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "ro.PLACEHOLDER.giftregistry",
      "sha256_cert_fingerprints": [
        "PLACEHOLDER:SHA256:FINGERPRINT:GOES:HERE"
      ]
    }
  }
]
```

### Pattern 4: i18n Key Convention (I18N-02)

**What:** All user-visible strings are stored in resource files, never hardcoded. Android uses `res/values/strings.xml` (English) and `res/values-ro/strings.xml` (Romanian). Web uses `i18n/en.json` and `i18n/ro.json` (following i18next conventions).

**Key naming convention (D-11 recommendation: feature-namespaced):** Use `feature_screen_element` snake_case for Android keys. Use nested JSON objects by feature for web (i18next flat or nested both work; nested is recommended for readability at scale).

**Rationale for feature-namespaced over type-based:** A 7-phase project with 45 requirements will grow to 200+ string keys. Feature namespacing lets each phase add strings in isolation without cross-phase conflicts. Type-based (e.g., all `error_*` strings together) makes it hard to audit what a screen needs.

```xml
<!-- app/src/main/res/values/strings.xml — English (default) -->
<!-- Phase 1 establishes structure; Phase 2+ adds feature strings -->
<resources>
    <!-- App-level -->
    <string name="app_name">Gift Registry</string>

    <!-- Common / shared -->
    <string name="common_ok">OK</string>
    <string name="common_cancel">Cancel</string>
    <string name="common_save">Save</string>
    <string name="common_loading">Loading…</string>
    <string name="common_error_generic">Something went wrong. Please try again.</string>
    <string name="common_retry">Retry</string>

    <!-- Auth screen (Phase 2 will expand) -->
    <string name="auth_sign_in_title">Sign In</string>
    <string name="auth_sign_up_title">Create Account</string>
    <string name="auth_continue_as_guest">Continue as Guest</string>
    <string name="auth_email_label">Email</string>
    <string name="auth_password_label">Password</string>

    <!-- Registry (Phase 3 will expand) -->
    <string name="registry_create_title">New Registry</string>
    <string name="registry_occasion_label">Occasion</string>

    <!-- Reservation (Phase 4 will expand) -->
    <string name="reservation_timer_label">Reserved for %1$d minutes</string>
    <string name="reservation_expired">Reservation expired</string>
</resources>
```

```xml
<!-- app/src/main/res/values-ro/strings.xml — Romanian -->
<resources>
    <string name="app_name">Registru de Cadouri</string>

    <string name="common_ok">OK</string>
    <string name="common_cancel">Anulează</string>
    <string name="common_save">Salvează</string>
    <string name="common_loading">Se încarcă…</string>
    <string name="common_error_generic">Ceva a mers prost. Încearcă din nou.</string>
    <string name="common_retry">Reîncearcă</string>

    <string name="auth_sign_in_title">Autentificare</string>
    <string name="auth_sign_up_title">Creează Cont</string>
    <string name="auth_continue_as_guest">Continuă ca Oaspete</string>
    <string name="auth_email_label">Email</string>
    <string name="auth_password_label">Parolă</string>

    <string name="registry_create_title">Registru Nou</string>
    <string name="registry_occasion_label">Ocazie</string>

    <string name="reservation_timer_label">Rezervat pentru %1$d minute</string>
    <string name="reservation_expired">Rezervare expirată</string>
</resources>
```

```json
// web/i18n/en.json — English (i18next nested format)
{
  "app": {
    "name": "Gift Registry"
  },
  "common": {
    "ok": "OK",
    "cancel": "Cancel",
    "save": "Save",
    "loading": "Loading…",
    "error_generic": "Something went wrong. Please try again.",
    "retry": "Retry"
  },
  "auth": {
    "sign_in_title": "Sign In",
    "sign_up_title": "Create Account",
    "continue_as_guest": "Continue as Guest",
    "email_label": "Email",
    "password_label": "Password"
  },
  "registry": {
    "create_title": "New Registry",
    "occasion_label": "Occasion"
  },
  "reservation": {
    "timer_label": "Reserved for {{minutes}} minutes",
    "expired": "Reservation expired"
  }
}
```

```json
// web/i18n/ro.json — Romanian
{
  "app": {
    "name": "Registru de Cadouri"
  },
  "common": {
    "ok": "OK",
    "cancel": "Anulează",
    "save": "Salvează",
    "loading": "Se încarcă…",
    "error_generic": "Ceva a mers prost. Încearcă din nou.",
    "retry": "Reîncearcă"
  },
  "auth": {
    "sign_in_title": "Autentificare",
    "sign_up_title": "Creează Cont",
    "continue_as_guest": "Continuă ca Oaspete",
    "email_label": "Email",
    "password_label": "Parolă"
  },
  "registry": {
    "create_title": "Registru Nou",
    "occasion_label": "Ocazie"
  },
  "reservation": {
    "timer_label": "Rezervat pentru {{minutes}} minute",
    "expired": "Rezervare expirată"
  }
}
```

### Anti-Patterns to Avoid

- **Using `rules_version = '1'`:** v1 is the implicit default when no version is declared. v2 is required for recursive wildcard matching. Always declare `rules_version = '2'` explicitly.
- **Checking invite status in a subcollection `get()` inside rules:** Requires a read call that counts against Firestore usage and can fail if the document doesn't exist. Use the map-on-document pattern instead.
- **Writing reservation documents from the Android client:** `expiresAt` can be set to any value by a malicious client. Reservations must only be written by Cloud Functions (Admin SDK, which bypasses security rules).
- **Hardcoding Romanian strings directly in Kotlin or XML layouts:** Discovered strings are expensive to extract after the fact. Establish the resource file discipline in Phase 1, before any UI code exists.
- **Using Firebase Dynamic Links:** Deprecated August 2025. Android App Links + `assetlinks.json` is the current pattern.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Security rules testing | Custom test harness that calls the REST API | `@firebase/rules-unit-testing` v5 | Integrates with emulator, handles auth context, provides `assertSucceeds`/`assertFails`; REST API testing is slower and more complex |
| i18n pluralization on Android | Custom `getQuantityString` wrapper | Android built-in `<plurals>` resource type | Android resource system handles plural forms for Romanian (which has a third plural form) natively |
| i18n pluralization on web | Custom string formatting | i18next `_plural` key suffix convention | i18next handles Romanian plural rules automatically when configured with `i18next-icu` or locale data |
| Emulator startup in CI | Shell script that polls ports | `firebase emulators:exec` command | Runs emulators, executes a command, then shuts down — atomic and CI-friendly |
| `assetlinks.json` verification | Custom HTTP endpoint | Firebase Hosting static file serve | Static file serve is sufficient; no dynamic content needed for App Links |

**Key insight:** The Firebase toolchain (CLI, emulator, rules-unit-testing) is designed to work together end-to-end. Any custom plumbing around these tools adds maintenance burden with no benefit.

---

## Common Pitfalls

### Pitfall 1: rules_version = '2' Not Declared
**What goes wrong:** Rules are evaluated by the v1 engine, which does not support `match /{document=**}` wildcard syntax. If you later add recursive matches (common when adding subcollection rules), the rules silently fall back to v1 behavior.
**Why it happens:** The version declaration is optional; v1 is the default. Many tutorials omit it.
**How to avoid:** First line of `firestore.rules` after the service declaration is always `rules_version = '2';`.
**Warning signs:** Rules simulator warns about unsupported syntax; wildcard matches behave unexpectedly.

### Pitfall 2: Invite Check in Rules Using get() on Subcollection
**What goes wrong:** Rules use `get(/databases/$(database)/documents/registries/$(registryId)/invites/$(request.auth.token.email))` to check invite status. This works in the simulator but in production: (1) email is not a safe document ID (contains dots, special chars), (2) every rules evaluation costs one Firestore read, (3) if the invites document doesn't exist the `get()` returns null and the rules fail unexpectedly.
**Why it happens:** Subcollection-based invite lists seem intuitive from a data modeling perspective.
**How to avoid:** Store invited user UIDs in a map on the registry document: `invitedUsers: { "uid1": true, "uid2": true }`. Rules check `resource.data.invitedUsers[request.auth.uid] == true` — no additional read required.
**Warning signs:** Rules simulator passes but production denies access intermittently.

### Pitfall 3: Items Subcollection Rules Allow Direct Client Reservation Writes
**What goes wrong:** A developer adds `allow update: if request.auth != null` to items — intending to allow givers to mark items as reserved. This bypasses the Cloud Tasks expiry pattern: any authenticated user can set `status = "reserved"` with any `expiresAt` they choose.
**Why it happens:** The reservation write path is not yet implemented (it's Phase 4), so placeholder-permissive rules are added.
**How to avoid:** In Phase 1, items are owner-write-only. The `status` field update path via giver action goes through a Cloud Function in Phase 4. Never add permissive item rules as a placeholder.
**Warning signs:** Items subcollection has `allow write: if request.auth != null`.

### Pitfall 4: Firebase Hosting Does Not Serve assetlinks.json at /.well-known/
**What goes wrong:** The `assetlinks.json` file is placed in `hosting/public/assetlinks.json` (root) but Android App Links requires it at `https://domain/.well-known/assetlinks.json`. The verification fails silently and App Links do not work.
**Why it happens:** The `.well-known/` directory name (leading dot) is hidden on macOS/Linux by default and easy to miscreate.
**How to avoid:** Create the directory explicitly: `mkdir -p hosting/public/.well-known/` and place `assetlinks.json` inside. Verify the hosting config serves it by running `firebase emulators:start` and requesting `http://localhost:5000/.well-known/assetlinks.json`.
**Warning signs:** App Links never intercept URLs even after installing the app; Android logs show `assetlinks.json` verification failed.

### Pitfall 5: Firestore Region Selection Ignored
**What goes wrong:** Firestore is created in the default `us-central1` region. All read/write latency from Romanian users is 80-150ms higher than using a European region.
**Why it happens:** The Firebase console defaults to us-central1; developers accept the default.
**How to avoid:** Select `europe-west3` (Frankfurt) when creating the Firestore database. **This cannot be changed after creation.** Region selection is a Phase 1 decision that is permanent.
**Warning signs:** Firestore latency consistently above 200ms from Romanian IP addresses.

### Pitfall 6: String Key Naming Inconsistency Across Phases
**What goes wrong:** Phase 2 adds strings named `login_button`, Phase 3 adds strings named `btn_add_item`, Phase 5 adds strings named `webfallback_reserve`. Three different naming conventions in the same file; impossible to audit coverage.
**Why it happens:** Each phase is implemented independently without a naming convention document to reference.
**How to avoid:** Phase 1 establishes the convention (`feature_screen_element` snake_case) with 15-20 example keys. Every subsequent phase's plan references this convention. The convention is documented in Phase 1 deliverables.
**Warning signs:** `strings.xml` has mixed prefix styles after Phase 3.

---

## Code Examples

### Firestore indexes for Phase 1 (starter)

```json
// firestore.indexes.json
// Source: https://firebase.google.com/docs/firestore/query-data/indexing
{
  "indexes": [
    {
      "collectionGroup": "items",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "addedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "reservations",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "expiresAt", "order": "ASCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

### App Check initialization (Android — Phase 1 console setup, Phase 2 code)

```kotlin
// Source: https://firebase.google.com/docs/app-check/android/play-integrity-provider
// In Application.onCreate() — Phase 2 implementation reference
// Firebase App Check with Play Integrity (production)
FirebaseApp.initializeApp(this)
val firebaseAppCheck = FirebaseAppCheck.getInstance()
firebaseAppCheck.installAppCheckProviderFactory(
    PlayIntegrityAppCheckProviderFactory.getInstance()
)
// During development use DebugAppCheckProviderFactory instead
```

### Running security rules tests

```bash
# Run emulators + tests in one command (CI-friendly)
firebase emulators:exec --only firestore,auth \
  "cd tests/rules && npm test"

# Or start emulators separately for local development
firebase emulators:start --only firestore,auth,functions,hosting
# In another terminal:
cd tests/rules && npm test
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Firebase Dynamic Links for sharing | Android App Links + Firebase Hosting web fallback | August 2025 (DFL shutdown) | All shareable links must use HTTPS + `assetlinks.json`; any DFL link now returns 4xx |
| Firebase KTX modules (`firebase-auth-ktx`, etc.) | Main modules only (`firebase-auth`, `firebase-firestore`) | Firebase BoM v34.0.0 (July 2025) | Projects on BoM <34 must migrate before upgrading; new projects use main modules only |
| Cloud Functions 1st gen | Cloud Functions 2nd gen | Ongoing (1st gen deprecated) | 2nd gen has better concurrency, longer timeout, min instances; use 2nd gen for all new functions |
| `rules_version = '1'` (implicit) | `rules_version = '2'` (explicit) | Firebase rules v2 available since ~2020; v1 still default if omitted | v2 required for recursive wildcards; always declare explicitly |
| Firebase Realtime Database | Cloud Firestore | 2017+; Firestore is the current product | Firestore has atomic transactions (required for reservation race condition), better querying, subcollections |

**Deprecated/outdated:**
- **Firebase Dynamic Links:** Shut down August 2025. Do not use. All links return 4xx.
- **Firebase KTX modules (`firebase-auth-ktx`, `firebase-firestore-ktx`, etc.):** Removed from BoM v34+. Use main modules.
- **Cloud Functions 1st gen:** Deprecated; use 2nd gen.

---

## Open Questions

1. **Firebase project ID and app package name**
   - What we know: Project will be created from scratch in Phase 1
   - What's unclear: The exact project ID (e.g., `gift-registry-ro`) and Android package name (e.g., `ro.example.giftregistry`) need to be chosen before `google-services.json` can be generated and before `assetlinks.json` can be fully populated
   - Recommendation: Decide project ID and package name as the first task in Phase 1 execution; these are permanent choices

2. **App Check enforcement timing**
   - What we know: App Check should be enabled from Phase 1; Play Integrity requires the app to be installed from Play Store
   - What's unclear: App Check enforcement (blocking requests without a valid token) cannot happen until the app is published to Play Store. Should rules reference `request.app` in Phase 1, or add that enforcement layer in a later phase?
   - Recommendation: Enable App Check in the Firebase console in Phase 1, but do not enforce in security rules until Phase 5 (when the web fallback ships and App Check web SDK is configured). Use Debug provider during development.

3. **Firestore composite indexes needed by Phase 1**
   - What we know: Two starter indexes are defined above (items by status+addedAt, reservations by status+expiresAt)
   - What's unclear: The full set of query patterns across all phases is not yet known; Firestore auto-creates indexes for single-field queries but requires manual composite index definitions
   - Recommendation: Define the two starter indexes in Phase 1; Firestore will error with a console link to create missing indexes when queries are first executed in later phases — add those indexes incrementally.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| Node.js | Firebase CLI, Functions, rules tests | Yes | v22.14.0 | — |
| npm | Package installation | Yes | 10.9.2 | — |
| Firebase CLI | `firebase init`, emulators, deploy | No (not installed) | — | `npm install -g firebase-tools` |
| Java JDK 11+ | Firebase Emulator Suite | Unknown — not checked | — | `brew install openjdk@17` |
| Android Studio Meerkat (2024.3+) | Android app scaffold (Phase 2) | Unknown | — | Required for Phase 2; not blocking Phase 1 |

**Missing dependencies with no fallback:**
- Firebase CLI: blocking for Phase 1 execution. Install as the first task: `npm install -g firebase-tools`

**Missing dependencies with fallback:**
- Java JDK: required by Firebase Emulator Suite. If not installed, emulators will fail to start. Install via `brew install openjdk@17` or equivalent before running emulators.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | `@firebase/rules-unit-testing` v5 + Jest (or Mocha — both supported) |
| Config file | `tests/rules/package.json` (created in Wave 0) |
| Quick run command | `cd tests/rules && npm test` (with emulators running) |
| Full suite command | `firebase emulators:exec --only firestore,auth "cd tests/rules && npm test"` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| I18N-02 | `strings.xml` exists with English keys | Lint/structural | Android Lint `HardcodedText` check: `./gradlew lintDebug` | Wave 0 gap (no Android project yet) |
| I18N-02 | `values-ro/strings.xml` has same key count as `values/strings.xml` | Structural | Custom script or Android Lint | Wave 0 gap |
| I18N-02 | `en.json` and `ro.json` have matching key structure | Structural | `npx i18next-scanner` or Jest snapshot | Wave 0 gap |
| Phase 1 SC-3 | Public registry read allowed unauthenticated | Security rules unit | `firebase emulators:exec ... npm test` | Wave 0 gap |
| Phase 1 SC-3 | Private registry denied non-owner non-invited | Security rules unit | `firebase emulators:exec ... npm test` | Wave 0 gap |
| Phase 1 SC-3 | Guest cannot write reservation document directly | Security rules unit | `firebase emulators:exec ... npm test` | Wave 0 gap |
| Phase 1 SC-3 | Owner-only item write enforced | Security rules unit | `firebase emulators:exec ... npm test` | Wave 0 gap |

### Sampling Rate
- **Per task commit:** `cd tests/rules && npm test` (requires emulators running)
- **Per wave merge:** `firebase emulators:exec --only firestore,auth "cd tests/rules && npm test"`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `tests/rules/package.json` — jest + @firebase/rules-unit-testing v5 deps
- [ ] `tests/rules/firestore.rules.test.ts` — four required test scenarios
- [ ] `tests/rules/tsconfig.json` — TypeScript config for test runner
- [ ] Framework install: `cd tests/rules && npm install` — after package.json created

---

## Sources

### Primary (HIGH confidence)
- [Firebase Security Rules Language v2](https://firebase.google.com/docs/rules/rules-language) — `rules_version = '2'` declaration, v2 is current recommended
- [Firestore Security Rules Conditions](https://firebase.google.com/docs/firestore/security/rules-conditions) — `request.auth.uid`, `resource.data` field checks
- [Firestore Role-Based Access](https://firebase.google.com/docs/firestore/solutions/role-based-access) — map-on-document pattern for role/invite checks in rules
- [Firebase Rules Unit Testing](https://firebase.google.com/docs/rules/unit-tests) — `@firebase/rules-unit-testing` v5, `assertSucceeds`, `assertFails`, `initializeTestEnvironment`
- [Firebase Emulator Suite Install](https://firebase.google.com/docs/emulator-suite/install_and_configure) — emulator ports, `firebase.json` emulators block, Java 11+ requirement
- [Firebase Hosting Full Config](https://firebase.google.com/docs/hosting/full-config) — `firebase.json` hosting configuration, headers, rewrites
- [Firebase Dynamic Links Deprecation FAQ](https://firebase.google.com/support/dynamic-links-faq) — confirmed shutdown August 2025
- [Android App Links](https://developer.android.com/training/app-links/deep-linking) — `assetlinks.json` verification pattern
- npm registry: `@firebase/rules-unit-testing@5.0.0` — verified 2026-04-04
- npm registry: `firebase@12.11.0` (JS SDK) — verified 2026-04-04
- npm registry: `firebase-functions@7.2.3` — verified 2026-04-04
- npm registry: `firebase-admin@13.7.0` — verified 2026-04-04

### Secondary (MEDIUM confidence)
- [Firebase App Check with Play Integrity (Android)](https://firebase.google.com/docs/app-check/android/play-integrity-provider) — console setup and Gradle dependency confirmed; initialization code not fully loaded from docs page
- `.planning/research/ARCHITECTURE.md` — Firestore schema design, build order rationale, subcollection vs top-level decisions (project-specific research, HIGH for this project's context)
- `.planning/research/STACK.md` — Firebase BoM 34.11.0, firebase-functions 4.x (note: npm shows 7.2.3 current — version from this file may reflect a different package version history; npm registry is authoritative)
- `.planning/research/PITFALLS.md` — security rules pitfalls, reservation race condition patterns

### Tertiary (LOW confidence)
- Firestore `europe-west3` latency benefit for Romanian users — inferred from general GCP region selection guidance; not benchmarked for this specific use case

---

## Metadata

**Confidence breakdown:**
- Firestore schema: HIGH — based on architecture research + official Firestore data modeling docs
- Security rules: HIGH — patterns verified against official Firebase rules documentation and role-based access guide
- Rules testing: HIGH — `@firebase/rules-unit-testing` v5 confirmed via npm registry; API verified against official docs
- firebase.json configuration: HIGH — verified against Firebase Hosting docs
- assetlinks.json: HIGH — Firebase Hosting static file serving is well-documented; App Links pattern confirmed from official Android docs
- i18n structure: HIGH — Android resource system and i18next are both well-documented; key naming convention is a discretionary choice (D-11)
- Package versions: HIGH — verified against npm registry on 2026-04-04

**Research date:** 2026-04-04
**Valid until:** 2026-07-04 (stable — Firebase infrastructure APIs change slowly; re-verify package versions before execution)
