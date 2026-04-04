# Phase 1: Firebase Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-04
**Phase:** 01-Firebase Foundation
**Areas discussed:** Data model, Security rules, Environment setup, String resources

---

## Data Model

### Collection Structure
| Option | Description | Selected |
|--------|-------------|----------|
| Subcollections | Items as subcollection under each registry doc — natural grouping, simpler queries per registry | |
| Top-level flat | Separate top-level collections with registryId field — easier cross-registry queries | |
| You decide | Let Claude pick based on access patterns | ✓ |

**User's choice:** You decide
**Notes:** Claude has discretion to choose based on access patterns for the app.

### Reservation Storage
| Option | Description | Selected |
|--------|-------------|----------|
| On the item doc | Reservation fields directly on item document — simpler, one read for status | |
| Separate collection | Dedicated reservations/ collection — easier history tracking, audit trail | |
| You decide | Let Claude pick based on 30-min timer and concurrent access requirements | ✓ |

**User's choice:** You decide

### Guest User Documents
| Option | Description | Selected |
|--------|-------------|----------|
| Guests in users/ | Create guest user doc with name+email — uniform queries | |
| Guests inline only | Store guest name+email only on reservation doc — simpler | |
| You decide | Let Claude pick based on guest-to-account conversion flow | ✓ |

**User's choice:** You decide

---

## Security Rules

### Private Registry Access Enforcement
| Option | Description | Selected |
|--------|-------------|----------|
| Invited list on doc | Registry doc has invitedEmails array — rules check list membership | |
| Invites collection | Separate invites/ subcollection — rules check doc existence | |
| You decide | Let Claude pick what works with Firestore rules limitations | ✓ |

**User's choice:** You decide

### Public Registry Read Access
| Option | Description | Selected |
|--------|-------------|----------|
| Yes, fully open | Public registries readable by anyone — maximum accessibility | |
| Rate-limited open | Public but rate-limited via App Check — prevents scraping | |
| You decide | Let Claude pick the right balance | ✓ |

**User's choice:** You decide

---

## Environment Setup

### Firebase Project Count
| Option | Description | Selected |
|--------|-------------|----------|
| Single project | One Firebase project — simpler, lower cost | ✓ |
| Dev + Prod | Two separate projects — test safely | |
| Dev + Staging + Prod | Three projects — full pipeline | |

**User's choice:** Single project

### Firebase Emulator Suite
| Option | Description | Selected |
|--------|-------------|----------|
| Yes (Recommended) | Run Firestore, Auth, Functions locally | ✓ |
| No | Develop against cloud project | |

**User's choice:** Yes (Recommended)

### Project Creation
| Option | Description | Selected |
|--------|-------------|----------|
| Create from scratch | Phase 1 defines project setup steps | ✓ |
| Already have one | Focus on schema and rules only | |

**User's choice:** Create from scratch

### Domain
| Option | Description | Selected |
|--------|-------------|----------|
| Firebase default | Use [project].web.app for now | ✓ |
| Custom domain ready | Configure custom domain in Phase 1 | |

**User's choice:** Firebase default

---

## String Resources

### Key Organization
| Option | Description | Selected |
|--------|-------------|----------|
| By screen/feature | Keys grouped by screen — easy to find | |
| By type | Keys grouped by type — reusable | |
| You decide | Let Claude pick a convention that scales well | ✓ |

**User's choice:** You decide

### Scope of Phase 1 Strings
| Option | Description | Selected |
|--------|-------------|----------|
| Convention only | Define naming pattern + example keys — each phase adds its own | ✓ |
| All placeholders | Pre-define keys for all 7 phases | |

**User's choice:** Convention only

---

## Claude's Discretion

- Data model: collection structure, reservation storage, guest user documents
- Security rules: private registry enforcement approach, public registry access policy
- String resources: key organization convention

## Deferred Ideas

None — discussion stayed within phase scope.
