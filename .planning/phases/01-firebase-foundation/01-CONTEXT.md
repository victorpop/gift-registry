# Phase 1: Firebase Foundation - Context

**Gathered:** 2026-04-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Configure Firebase project infrastructure with a stable Firestore schema, tested security rules, and string resource structure. No feature code — this is the foundation all subsequent phases build on. Only requirement mapped: I18N-02 (string resource file structure).

</domain>

<decisions>
## Implementation Decisions

### Data Model
- **D-01:** Claude's discretion on collection structure (flat vs subcollections for registries/items) — choose based on access patterns: registry owner CRUD, giver browsing items, real-time reservation status updates, cross-registry queries not needed for v1
- **D-02:** Claude's discretion on reservation storage (fields on item doc vs separate collection) — optimize for the 30-min timer, concurrent access via Firestore transactions, and Cloud Tasks expiry pattern
- **D-03:** Claude's discretion on guest user documents vs inline-only — optimize for guest-to-account conversion flow (AUTH-06) while avoiding orphan documents

### Security Rules
- **D-04:** Claude's discretion on private registry access enforcement (invited list on doc vs invites subcollection) — choose the approach that works within Firestore rules limitations and supports the invite flow (REG-05 through REG-08)
- **D-05:** Claude's discretion on public registry read access — balance openness for gift givers with protection against scraping, using App Check where appropriate

### Environment Setup
- **D-06:** Single Firebase project (no dev/staging/prod split)
- **D-07:** Firebase Emulator Suite enabled for local development (Firestore, Auth, Functions)
- **D-08:** Firebase project created from scratch — Phase 1 includes full setup instructions
- **D-09:** Use default Firebase Hosting URL ([project].web.app) — custom domain deferred

### String Resources
- **D-10:** Convention and naming pattern only — define the i18n key structure with a few example keys, each subsequent phase adds its own strings
- **D-11:** Claude's discretion on key organization approach (by screen/feature vs by type) — pick what scales well for a 7-phase project with Android + web fallback

### Claude's Discretion
Data model structure (D-01, D-02, D-03), security rules approach (D-04, D-05), and string key organization (D-11) are all at Claude's discretion. Make choices based on Firestore best practices, the specific access patterns this app requires, and the research findings in .planning/research/.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project context
- `.planning/PROJECT.md` — Core value, constraints, key decisions (Firebase over SQLite rationale)
- `.planning/REQUIREMENTS.md` — Full v1 requirements with I18N-02 mapped to this phase
- `.planning/ROADMAP.md` — Phase 1 success criteria and dependency chain

### Research findings
- `.planning/research/STACK.md` — Firebase BoM version, recommended libraries, version constraints
- `.planning/research/ARCHITECTURE.md` — Component boundaries, Firestore schema patterns, security rules approach
- `.planning/research/PITFALLS.md` — Reservation race condition prevention, security rules as sole access boundary, hot document limits
- `.planning/research/SUMMARY.md` — Synthesized recommendations and roadmap implications

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
None — greenfield project. No existing code to build on.

### Established Patterns
None — this phase establishes the foundational patterns all future phases will follow.

### Integration Points
- Firebase project configuration will be consumed by Phase 2 (Android app scaffold)
- Firestore schema defines the data contract for all subsequent phases
- Security rules must support access patterns from Phases 2-7
- String resource convention will be followed by all UI phases (2, 3, 5)

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches. User deferred most technical decisions to Claude's judgment, indicating trust in Firebase best practices.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 01-firebase-foundation*
*Context gathered: 2026-04-04*
