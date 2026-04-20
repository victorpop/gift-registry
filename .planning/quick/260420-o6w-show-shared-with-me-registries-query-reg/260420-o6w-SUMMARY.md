---
phase: quick-260420-o6w
plan: "01"
subsystem: data/registry
tags: [firestore, flow, combine, dedupe, sort, invited-registries]
dependency_graph:
  requires: [inviteToRegistry Cloud Function (writes invitedUsers.{uid}=true), firestore.rules isInvited() helper]
  provides: [RegistryRepositoryImpl.observeRegistries returns owned + invited merged]
  affects: [RegistryListViewModel.uiState (via ObserveRegistriesUseCase — no changes needed)]
tech_stack:
  added: []
  patterns: [callbackFlow + awaitClose for Firestore snapshot listener, combine() for multi-flow merge, linkedMapOf dedupe-by-id, sortedByDescending]
key_files:
  created:
    - app/src/test/java/com/giftregistry/data/registry/RegistryRepositoryImplObserveTest.kt
  modified:
    - app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt
    - app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt
decisions:
  - open class FirestoreDataSource to allow MockK stubbing without MockKAgent inline instrumentation (relaxed=true used)
  - Parameter name ownerId kept in interface/impl signature for stability; KDoc updated to clarify it is now "current user's UID"
  - Dedupe strategy: owner entry wins when uid is both owner and invitee — implemented via linkedMapOf with putIfAbsent for invited stream
metrics:
  duration: ~15min
  completed_date: "2026-04-20"
  tasks_completed: 1
  tasks_pending: 1 (checkpoint:human-verify)
  files_changed: 3
---

# Phase quick-260420-o6w Plan 01: Show Shared-With-Me Registries Summary

**One-liner:** Added `observeInvitedRegistries(uid)` Firestore query using `invitedUsers.$uid==true` map filter; merged with owned registries via `combine()`, deduped by id (owner wins), sorted by updatedAt DESC.

## What Was Built

### FirestoreDataSource Changes

`FirestoreDataSource` (`app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt`) was updated:

1. **Renamed** `observeRegistries(ownerId)` to `observeOwnedRegistries(uid)` — body identical (`.whereEqualTo("ownerId", uid)`). KDoc added.
2. **Added** `observeInvitedRegistries(uid)` — same `callbackFlow`/`awaitClose` shape as `observeOwnedRegistries` but using `.whereEqualTo("invitedUsers.$uid", true)`. This is a Firestore dotted-path map-key equality query.
3. **Opened** the class and both new methods (`open class`, `open fun`) so MockK can stub them in unit tests.

### RegistryRepositoryImpl Changes

`observeRegistries(ownerId)` body replaced in `RegistryRepositoryImpl.kt`:

```kotlin
override fun observeRegistries(ownerId: String): Flow<List<Registry>> {
    val owned = dataSource.observeOwnedRegistries(ownerId)
    val invited = dataSource.observeInvitedRegistries(ownerId)
    return combine(owned, invited) { ownedDtos, invitedDtos ->
        val byId = linkedMapOf<String, RegistryDto>()
        for (dto in ownedDtos) byId[dto.id] = dto
        for (dto in invitedDtos) byId.putIfAbsent(dto.id, dto)
        byId.values
            .map { it.toDomain() }
            .sortedByDescending { it.updatedAt }
    }
}
```

Import added: `kotlinx.coroutines.flow.combine`.

### Unit Tests

`RegistryRepositoryImplObserveTest.kt` created with 6 test cases (all pass):

| # | Description |
|---|-------------|
| 1 | owned + invited merged into one list |
| 2 | Dedupe — same registry in both streams appears once, owned title wins |
| 3 | Sort by updatedAt DESC across stream boundaries |
| 4a | Empty invited — returns owned only |
| 4b | Empty owned — returns invited only |
| 4c | Both empty — returns empty list |
| 5 | Reactive — new value on invited flow triggers re-emission of merged list |

## Firestore Rules — No Change Required

`firestore.rules` is **byte-identical** before and after this plan (`git diff --name-only firestore.rules` returns empty).

Reason: the existing `isInvited()` helper at L30-L36 already grants invited users read access on private registries:

```
function isInvited(registryData) {
    return isSignedIn() &&
           registryData.get('visibility', 'public') == 'private' &&
           registryData.get('invitedUsers', {})[request.auth.uid] == true;
}
// canReadRegistry = isOwner || isPublicRegistry || isInvited
```

The new `.whereEqualTo("invitedUsers.$uid", true)` query matches docs where the invited user's UID key is `true`. Each matched doc passes `isInvited()` (private) or `isPublicRegistry()` (public) in the security rule, so the list query succeeds without rules changes.

## Invite Pipeline — Confirmed Data Shape

`inviteToRegistry.ts` L85-L91 writes:

```typescript
await registryRef.update(new FieldPath("invitedUsers", inviteKey), true);
```

Where `inviteKey = invitedUid` for registered users. The Android query uses the UID as the key, which matches exactly. Non-user invites use `email:` prefix keys — these never match `auth.uid`, so they are correctly excluded from the invited query (those users access the registry via the invite email URL, not through the app).

## Firestore Index — No Index Required

Single-field map-key equality (`invitedUsers.$uid == true`) does not require a composite index. If Firestore requests one at runtime (via `FAILED_PRECONDITION`), the Firebase console will print a click-to-create link. Expected: no index creation needed.

## Deviations from Plan

None — plan executed exactly as written. The `open class` modifier was explicit in the plan's behavior section ("make the whole class `open`").

## Verification Results

- `./gradlew :app:testDebugUnitTest --tests "*RegistryRepositoryImplObserveTest*"` — BUILD SUCCESSFUL, all 6 tests pass
- `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL, zero errors
- `git diff --name-only firestore.rules` — empty output (rules unchanged)
- `FirestoreDataSource.observeRegistries` no longer exists; `observeOwnedRegistries` and `observeInvitedRegistries` both present
- `RegistryRepositoryImpl.observeRegistries` uses `combine` + dedupe-by-id + sort-by-updatedAt-desc

## Known Stubs

None — all wiring is live. The merge pipeline feeds directly into the existing `RegistryListViewModel.uiState` flow via `ObserveRegistriesUseCase` which is unchanged.

## Pending Task

**Task 2 (checkpoint:human-verify):** Two-account live test — requires Device A (Maria) and Device B (Victor) with debug APK installed. See plan for exact steps.

## Deferred Items for Follow-Up Quick Tasks

1. **"Shared with me" section header** — current UX mixes owned + invited sorted by updatedAt (no visual grouping). A follow-up quick task can add a section divider if desired.
2. **Email-prefixed invite handling** — users who receive an invite before creating an account have `email:<addr>` keys in `invitedUsers`, not UID keys. After they sign up, a separate flow would need to migrate their key. Out of scope here.
3. **Web fallback parity** — `useRegistryQuery` on the web only fetches a single registry by ID; if the web Home ever shows a list, it will need a similar `invitedUsers.$uid==true` query with the Firebase JS SDK.

## Self-Check

- [x] `app/src/test/java/com/giftregistry/data/registry/RegistryRepositoryImplObserveTest.kt` created
- [x] `app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt` modified
- [x] `app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt` modified
- [x] Commit `498bf43` exists in git log

## Self-Check: PASSED
