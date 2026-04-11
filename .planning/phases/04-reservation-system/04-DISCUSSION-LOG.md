# Phase 4: Reservation System - Discussion Log (Assumptions Mode)

> **Audit trail only.** Decisions captured in CONTEXT.md.

**Date:** 2026-04-11
**Phase:** 04-reservation-system
**Mode:** assumptions (--auto via /gsd:autonomous)

## Assumptions Presented

### Reservation Write Mechanism
| Assumption | Confidence |
|------------|-----------|
| createReservation callable Cloud Function with runTransaction | Confident |
| Atomic item status update + reservation doc creation | Confident |
| Client never writes reservations directly (hard-deny preserved) | Confident |

### Timer / Expiry (Cloud Tasks)
| Assumption | Confidence |
|------------|-----------|
| Per-reservation Cloud Task scheduled at now+30min | Confident |
| cloudTaskName stored on reservation doc for cancellation | Confident |
| Server-authoritative expiry, no client-side timer authority | Confident |
| firebase-functions v2 onTaskDispatched helper | Confident |

### Expiration Email + Re-Reserve
| Assumption | Confidence |
|------------|-----------|
| Stub email in Phase 4, full SendGrid in Phase 6 | Likely (auto-resolved) |
| Re-reserve flow goes through same createReservation transaction | Confident |

### UI / Guest Identity
| Assumption | Confidence |
|------------|-----------|
| Reuse FirestoreDataSource.observeItems for real-time status | Confident |
| RegistryDetailScreen hosts Reserve button + countdown | Likely |
| GuestPreferencesDataStore for guest identity persistence | Likely |
| affiliateUrl Intent launched after callable success only | Confident |

## Auto-Resolved

- **Expiration email scope:** Selected "stub in Phase 4, defer SendGrid to Phase 6" to match Phase 3 invite precedent and maintain phase boundaries

## External Research Flagged

- Cloud Tasks cancellation API signature (@google-cloud/tasks current version)
- firebase-functions v2 onTaskDispatched queue configuration
- SendGrid vs Firebase Extensions "Trigger Email" (informs Phase 6 but worth noting now)
- Firestore composite index requirements for reservations queries

## Corrections Made

None — all auto-confirmed in autonomous mode.
