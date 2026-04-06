# Phase 3: Registry + Item Management - Discussion Log (Assumptions Mode)

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions captured in CONTEXT.md — this log preserves the analysis.

**Date:** 2026-04-06
**Phase:** 03-registry-item-management
**Mode:** assumptions (--auto)
**Areas analyzed:** Firestore Data Access, Affiliate URL Transformation, OG Metadata Extraction, Navigation/Screen Structure, Registry Invite Flow

## Assumptions Presented

### Firestore Data Access Layer
| Assumption | Confidence | Evidence |
|------------|-----------|----------|
| Items as subcollection `registries/{id}/items/{itemId}` with real-time listeners | Confident | `firestore.rules` lines 37-44, `.planning/research/ARCHITECTURE.md` lines 346-355 |
| New `FirestoreDataSource` mirroring `FirebaseAuthDataSource` pattern | Confident | `app/src/main/java/com/giftregistry/data/auth/FirebaseAuthDataSource.kt` |
| `firebase-firestore` dependency needed | Confident | Not in `app/build.gradle.kts` or `gradle/libs.versions.toml` |

### Affiliate URL Transformation
| Assumption | Confidence | Evidence |
|------------|-----------|----------|
| Client-side utility, not Cloud Function | Likely | `.planning/research/ARCHITECTURE.md` lines 258-267, 466-470 |
| Both `originalUrl` and `affiliateUrl` stored | Confident | Schema defines both fields |
| Unknown merchants pass through unmodified | Confident | AFF-04 requirement |

### OG Metadata Extraction
| Assumption | Confidence | Evidence |
|------------|-----------|----------|
| Cloud Function callable for server-side fetch | Likely | `functions/src/index.ts` scaffold ready; client-side unreliable |
| Fallback to manual entry on extraction failure | Confident | UX necessity |

### Navigation and Screen Structure
| Assumption | Confidence | Evidence |
|------------|-----------|----------|
| Replace `HomeKey` placeholder with registry list | Confident | `AppNavigation.kt` line 102: "Home -- Phase 3" |
| `@Serializable` key pattern, `data class` for parameterized keys | Confident | `AppNavKeys.kt` existing pattern |

### Registry Invite Flow
| Assumption | Confidence | Evidence |
|------------|-----------|----------|
| `invitedUsers` map on registry doc for membership check | Confident | Phase 1 decision in STATE.md |
| Email stubs in Phase 3, full delivery in Phase 6 | Likely | Phase 6 scope covers notifications/email |

## Corrections Made

No corrections — all assumptions auto-confirmed (--auto mode).

## Auto-Resolved

- Affiliate URL transformation location: auto-selected client-side utility (recommended default)
- OG metadata extraction: auto-selected Cloud Function callable (recommended default)
- Invite email scope: auto-selected Phase 3 stubs + Phase 6 full delivery (recommended default)

## External Research Flagged

- EMAG affiliate tag format: Exact URL parameter/format for Profitshare/2Performant affiliate tags on EMAG URLs
- OG metadata parsing library: Reliable Node.js library for fetching and parsing Open Graph metadata from arbitrary URLs
